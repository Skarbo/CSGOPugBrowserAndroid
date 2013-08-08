package com.skarbo.csgobrowser.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.config.service.EseaServiceConfig;
import com.skarbo.csgobrowser.config.service.LeetwayServiceConfig;
import com.skarbo.csgobrowser.container.MatchesContainer.Match;
import com.skarbo.csgobrowser.container.ServersContainer;
import com.skarbo.csgobrowser.container.ServersContainer.Server;
import com.skarbo.csgobrowser.fragment.match.StatsMatchFragment;
import com.skarbo.csgobrowser.fragment.server.PlayersServerFragment;
import com.skarbo.csgobrowser.handler.Handler;
import com.skarbo.csgobrowser.has.HasMatch;
import com.skarbo.csgobrowser.has.HasServer;
import com.skarbo.csgobrowser.listener.HandlerListener;
import com.skarbo.csgobrowser.utils.UiUtils;

import java.io.IOException;
import java.io.InputStream;

public class ServerActivity extends AbstractActivity implements HasServer, HasMatch, HandlerListener {

    private static final String TAG = ServerActivity.class.getSimpleName();
    private static final String ARG_SERVICE_ID = "service_id";
    private static final String ARG_SERVER_ID = "server_id";
    private static final String ARG_MATCH_ID = "match_id";
    private static final int SERVER_CHILDS = 2;
    private static final int SERVER_CHILD_PLAYERS = 0;
    private static final int SERVER_CHILD_STATS = 1;
    private String serviceId;
    private String serverId;
    private String matchId;
    private MenuItem menuRefresh;
    private TextView serverNameTextView;
    private ImageView serverCountryImageView;
    private ImageView serverMapImageView;
    private ImageView serverServiceImageView;
    private TextView serverStatusTextView;
    private LinearLayout serverScoreLayout;
    private TextView serverScoreHomeTextView;
    private TextView serverScoreAwayTextView;
    private ViewPager serverViewPager;
    private ServerPagerAdapter serverPagerAdapter;

    public static Intent createServerActivity(Context context, String serviceId, String serverId, String matchId) {
        Intent serverActivityIntent = new Intent(context, ServerActivity.class);
        serverActivityIntent.putExtra(ARG_SERVICE_ID, serviceId);
        serverActivityIntent.putExtra(ARG_SERVER_ID, serverId);
        serverActivityIntent.putExtra(ARG_MATCH_ID, matchId);
        return serverActivityIntent;
    }

    // ... ON

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.serviceId = getIntent().getExtras().getString(ARG_SERVICE_ID);
        this.serverId = getIntent().getExtras().getString(ARG_SERVER_ID);
        this.matchId = getIntent().getExtras().getString(ARG_MATCH_ID);

        Server server = getHandler().getContainers().serversContainer.getServer(this.serviceId, this.serverId, this.matchId);

        if (server == null) {
            Toast.makeText(getApplicationContext(), "Server not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        // ACTIONBAR

        getSupportActionBar().setSubtitle(server.name);

        // /ACTIONBAR

        // VIEW

        setContentView(R.layout.activity_server);
        // getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,
        // new ServerFragment()).commit();

        serverNameTextView = (TextView) findViewById(R.id.serverNameTextView);
        serverCountryImageView = (ImageView) findViewById(R.id.serverCountryImageView);
        serverMapImageView = (ImageView) findViewById(R.id.serverMapImageView);
        serverServiceImageView = (ImageView) findViewById(R.id.serverServiceImageView);

        serverStatusTextView = (TextView) findViewById(R.id.serverStatusTextView);
        serverScoreLayout = (LinearLayout) findViewById(R.id.serverScoreLayout);
        serverScoreHomeTextView = (TextView) findViewById(R.id.serverScoreHomeTextView);
        serverScoreAwayTextView = (TextView) findViewById(R.id.serverScoreAwayTextView);

        serverViewPager = (ViewPager) findViewById(R.id.serverChildViewPager);

        // /VIEW

        serverPagerAdapter = new ServerPagerAdapter(getSupportFragmentManager());

        serverViewPager.setAdapter(serverPagerAdapter);
        serverViewPager.setCurrentItem(SERVER_CHILD_PLAYERS);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (getHandler() != null) {
            getHandler().doRefresh();
        }
    }

    // ... ... HANDLER

    @Override
    public void onUpdating() {
        super.onUpdating();
        doUpdateView();
    }

    @Override
    public void onUpdated() {
        super.onUpdated();
        doUpdateView();
    }

    @Override
    public void onRefresh() {
        Server server = getServer();
        if (getHandler() != null && server != null)
            getHandler().getControlHandler().doServer(server);
    }

    // ... ... /HANDLER

    // ... /ON

    // ... GET

    @Override
    protected String getActivityTitle() {
        return getResources().getString(R.string.server);
    }

    @Override
    public Server getServer() {
        return getHandler().getContainers().serversContainer.getServer(serviceId, serverId, matchId);
    }

    @Override
    public Match getMatch() {
        return getHandler().getContainers().matchesContainer.getMatch(getServer().serviceId, getServer().matchId);
    }

    // ... /GET

    // ... HAS

    @Override
    protected boolean hasRefreshActionbarItem() {
        return true;
    }

    @Override
    protected boolean hasCancelActionbarItem() {
        return true;
    }

    // ... /HAS

    // ... DO

    private void doUpdateView() {
        Server server = getServer();
        if (server != null) {
            serverNameTextView.setText(server.name);

            // Country
            switch (server.country) {
                case DE:
                    serverCountryImageView.setImageResource(R.drawable.flag_de);
                    break;
                case ES:
                    serverCountryImageView.setImageResource(R.drawable.flag_es);
                    break;
                case FR:
                    serverCountryImageView.setImageResource(R.drawable.flag_fr);
                    break;
                case GB:
                    serverCountryImageView.setImageResource(R.drawable.flag_gb);
                    break;
                case SE:
                    serverCountryImageView.setImageResource(R.drawable.flag_se);
                    break;
                default:
                    serverCountryImageView.setImageResource(R.drawable.flag_us);
                    break;
            }

            // Status
            serverScoreLayout.setVisibility(View.GONE);
            switch (server.status) {
                case Live:
                    serverStatusTextView.setText(R.string.server_status_live);
                    UiUtils.setViewBackground(serverStatusTextView, getResources().getDrawable(R.drawable.server_status_tile_live));
                    break;
                case Waiting:
                    serverStatusTextView.setText(R.string.server_status_waiting);
                    UiUtils.setViewBackground(serverStatusTextView, getResources().getDrawable(R.drawable.server_status_tile_waiting));
                    break;
                default:
                    serverStatusTextView.setText(R.string.server_status_available);
                    UiUtils.setViewBackground(serverStatusTextView, getResources().getDrawable(R.drawable.server_status_tile_available));
                    break;
            }

            // Score
            if (server.status == ServersContainer.Server.Status.Live && server.scoreHome > -1 && server.scoreAway > -1) {
                serverStatusTextView.setVisibility(View.GONE);
                serverScoreLayout.setVisibility(View.VISIBLE);
                serverScoreHomeTextView.setText(String.valueOf(server.scoreHome));
                serverScoreAwayTextView.setText(String.valueOf(server.scoreAway));

                if (server.scoreHome == server.scoreAway) {
                    UiUtils.setViewBackground(serverScoreHomeTextView, getResources().getDrawable(R.drawable.server_score_tile_draw));
                    UiUtils.setViewBackground(serverScoreAwayTextView, getResources().getDrawable(R.drawable.server_score_tile_draw));
                } else if (server.scoreHome > server.scoreAway) {
                    UiUtils.setViewBackground(serverScoreHomeTextView, getResources().getDrawable(R.drawable.server_score_tile_win));
                    UiUtils.setViewBackground(serverScoreAwayTextView, getResources().getDrawable(R.drawable.server_score_tile_lose));
                } else {
                    UiUtils.setViewBackground(serverScoreHomeTextView, getResources().getDrawable(R.drawable.server_score_tile_lose));
                    UiUtils.setViewBackground(serverScoreAwayTextView, getResources().getDrawable(R.drawable.server_score_tile_win));
                }
            }

            // Map
            if (server.map != null) {
                try {
                    InputStream inputStreamMapImage = getResources().getAssets().open(
                            String.format("maps/%s.png", server.map));
                    Drawable drawableMapImage = Drawable.createFromStream(inputStreamMapImage, null);
                    serverMapImageView.setImageDrawable(drawableMapImage);
                } catch (IOException ex) {
                    Log.w(TAG, "doUpdateView: Map image not found: " + ex.getMessage());
                }
            }

            // Service
            if (server.serviceId.equalsIgnoreCase(EseaServiceConfig.SERVICE_ID)) {
                serverServiceImageView.setImageResource(R.drawable.logo_esea);
            } else if (server.serviceId.equalsIgnoreCase(LeetwayServiceConfig.SERVICE_ID)) {
                serverServiceImageView.setImageResource(R.drawable.logo_leetway);
            }
        }
    }

    // ... /DO

    // ADAPTER

    public class ServerPagerAdapter extends FragmentPagerAdapter {

        public ServerPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment;
            switch (i) {
                case SERVER_CHILD_STATS:
                    Server server = getServer();
                    String matchId = server != null ? server.matchId : "";
                    fragment = StatsMatchFragment.createStatsMatchFragment(getApplicationContext(), serviceId, matchId);
                    break;
                default:
                    fragment = new PlayersServerFragment();
                    break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return SERVER_CHILDS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case SERVER_CHILD_PLAYERS:
                    return "Players";
                case SERVER_CHILD_STATS:
                    return "Statistics";
            }
            return null;
        }
    }

    // /ADAPTER

}
