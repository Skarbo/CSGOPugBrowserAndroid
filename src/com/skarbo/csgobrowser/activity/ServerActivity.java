package com.skarbo.csgobrowser.activity;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
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
import com.skarbo.csgobrowser.utils.Utils;

public class ServerActivity extends SherlockFragmentActivity implements HasServer, HasMatch, HandlerListener {

	private static final String TAG = ServerActivity.class.getSimpleName();
	private static final String ARG_SERVICE_ID = "service_id";
	private static final String ARG_SERVER_ID = "server_id";
	private static final String ARG_MATCH_ID = "match_id";
	private static final int MENU_REFRESH = 0;
	private static final int SERVER_CHILDS = 2;
	private static final int SERVER_CHILD_PLAYERS = 0;
	private static final int SERVER_CHILD_STATS = 1;

	private Handler handler;

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

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setSupportProgressBarIndeterminateVisibility(false);

		// HANDLER

		this.handler = new Handler(this);
		try {
			this.handler.doContainersCacheLoad();
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "Could not load cache file", Toast.LENGTH_SHORT).show();
			Log.e(TAG, "onCreate: doContainersCacheLoad: " + e.getMessage());
		}

		this.serviceId = getIntent().getExtras().getString(ARG_SERVICE_ID, "");
		this.serverId = getIntent().getExtras().getString(ARG_SERVER_ID, "");
		this.matchId = getIntent().getExtras().getString(ARG_MATCH_ID, "");

		Server server = this.handler.getContainers().serversContainer.getServer(this.serviceId, serverId, matchId);

		if (server == null) {
			Toast.makeText(getApplicationContext(), "Server not found", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		// /HANDLER

		// ACTIONBAR

		getSupportActionBar().setTitle(R.string.server);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

		if (this.handler != null) {
			this.handler.addListener(TAG, this);
			this.handler.doRefresh();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (this.handler != null) {
			this.handler.removeListener(TAG);
			this.handler.doReset();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_REFRESH:
			doRefresh();
			return true;
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.menuRefresh = menu.add(0, MENU_REFRESH, 1, "Refresh");
		this.menuRefresh.setIcon(R.drawable.navigation_refresh).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

		if (this.handler.isUpdating()) {
			this.menuRefresh.setEnabled(false);
			this.doRefreshAnimation(true);
		}

		return true;
	}

	// ... ... HANDLER

	@Override
	public void onUpdating() {
		if (this.menuRefresh != null)
			this.menuRefresh.setEnabled(false);
		this.doRefreshAnimation(true);
		doUpdateView();
	}

	@Override
	public void onUpdated() {
		if (this.menuRefresh != null)
			this.menuRefresh.setEnabled(true);
		this.doRefreshAnimation(false);
		doUpdateView();
	}

	@Override
	public void onRefresh() {
		Server server = getServer();
		if (this.handler != null && server != null)
			this.handler.getControlHandler().doServer(server);
	}

	// ... ... /HANDLER

	// ... /ON

	// ... GET

	@Override
	public Handler getHandler() {
		return handler;
	}

	@Override
	public Server getServer() {
		return getHandler().getContainers().serversContainer.getServer(serviceId, serverId, matchId);
	}

	@Override
	public Match getMatch() {
		return this.handler.getContainers().matchesContainer.getMatch(getServer().serviceId, getServer().matchId);
	}

	// ... /GET

	// ... DO

	private void doRefresh() {
		this.handler.doReset();
	}

	private void doRefreshAnimation(boolean active) {
		Utils.rotateMenuItem(getApplicationContext(), this.menuRefresh, active);
	}

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
				serverStatusTextView.setBackground(getResources().getDrawable(R.drawable.server_status_tile_live));
				break;
			case Waiting:
				serverStatusTextView.setText(R.string.server_status_waiting);
				serverStatusTextView.setBackground(getResources().getDrawable(R.drawable.server_status_tile_waiting));
				break;
			default:
				serverStatusTextView.setText(R.string.server_status_available);
				serverStatusTextView.setBackground(getResources().getDrawable(R.drawable.server_status_tile_available));
				break;
			}

			// Score
			if (server.status == ServersContainer.Server.Status.Live && server.scoreHome > -1 && server.scoreAway > -1) {
				serverStatusTextView.setVisibility(View.GONE);
				serverScoreLayout.setVisibility(View.VISIBLE);
				serverScoreHomeTextView.setText(String.valueOf(server.scoreHome));
				serverScoreAwayTextView.setText(String.valueOf(server.scoreAway));

				if (server.scoreHome == server.scoreAway) {
					serverScoreHomeTextView
							.setBackground(getResources().getDrawable(R.drawable.server_score_tile_draw));
					serverScoreAwayTextView
							.setBackground(getResources().getDrawable(R.drawable.server_score_tile_draw));
				} else if (server.scoreHome > server.scoreAway) {
					serverScoreHomeTextView.setBackground(getResources().getDrawable(R.drawable.server_score_tile_win));
					serverScoreAwayTextView
							.setBackground(getResources().getDrawable(R.drawable.server_score_tile_lose));
				} else {
					serverScoreHomeTextView
							.setBackground(getResources().getDrawable(R.drawable.server_score_tile_lose));
					serverScoreAwayTextView.setBackground(getResources().getDrawable(R.drawable.server_score_tile_win));
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
