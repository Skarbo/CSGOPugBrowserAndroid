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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.config.service.EseaServiceConfig;
import com.skarbo.csgobrowser.config.service.LeetwayServiceConfig;
import com.skarbo.csgobrowser.container.MatchesContainer.Match;
import com.skarbo.csgobrowser.fragment.match.StatsMatchFragment;
import com.skarbo.csgobrowser.has.HasMatch;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;

public class MatchActivity extends AbstractActivity implements HasMatch {

    private static final String TAG = MatchActivity.class.getSimpleName();
    private static final String ARG_SERVICE_ID = "service_id";
    private static final String ARG_MATCH_ID = "match_id";
    private static final int MENU_REFRESH = 0;
    private static final int MATCH_CHILDS = 1;
    private static final int MATCH_CHILD_STATS = 0;
    private String serviceId;
    private String matchId;
    private MatchPagerAdapter matchPagerAdapter;
    private ViewPager matchViewPager;
    private ImageView matchCountryImageView;
    private TextView matchDateTextView;
    private TextView matchScoreHomeTextView;
    private TextView matchScoreAwayTextView;
    private ImageView matchServiceImageView;
    private ImageView serverMapImageView;

    public static Intent createServerActivity(Context context, String serviceId, String matchId) {
        Intent serverActivityIntent = new Intent(context, MatchActivity.class);
        serverActivityIntent.putExtra(ARG_SERVICE_ID, serviceId);
        serverActivityIntent.putExtra(ARG_MATCH_ID, matchId);
        return serverActivityIntent;
    }

    // ... ON

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        this.serviceId = getIntent().getExtras().getString(ARG_SERVICE_ID, "");
        this.matchId = getIntent().getExtras().getString(ARG_MATCH_ID, "");


        // VIEW

        setContentView(R.layout.activity_match);

        this.serverMapImageView = (ImageView) findViewById(R.id.serverMapImageView);
        this.matchCountryImageView = (ImageView) findViewById(R.id.matchCountryImageView);
        this.matchDateTextView = (TextView) findViewById(R.id.matchDateTextView);
        this.matchScoreHomeTextView = (TextView) findViewById(R.id.matchScoreHomeTextView);
        this.matchScoreAwayTextView = (TextView) findViewById(R.id.matchScoreAwayTextView);
        this.matchServiceImageView = (ImageView) findViewById(R.id.matchServiceImageView);

        this.matchViewPager = (ViewPager) findViewById(R.id.matchChildViewPager);

        // /VIEW

        this.matchPagerAdapter = new MatchPagerAdapter(getSupportFragmentManager());

        this.matchViewPager.setAdapter(matchPagerAdapter);
        this.matchViewPager.setCurrentItem(MATCH_CHILD_STATS);

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
        super.onRefresh();
        if (getHandler() != null)
            getHandler().getControlHandler().doMatch(this.serviceId, this.matchId);
    }

    // ... ... /HANDLER

    // ... /ON

    // ... GET

    @Override
    protected String getActivityTitle() {
        return getResources().getString(R.string.match);
    }

    @Override
    public Match getMatch() {
        return getHandler().getContainers().matchesContainer.getMatch(this.serviceId, this.matchId);
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
        Match match = getMatch();

        if (match != null) {
            getSupportActionBar().setSubtitle(String.format("#%s", match.id));

            // Country
            if (match.country != null) {
                switch (match.country) {
                    case DE:
                        matchCountryImageView.setImageResource(R.drawable.flag_de);
                        break;
                    case ES:
                        matchCountryImageView.setImageResource(R.drawable.flag_es);
                        break;
                    case FR:
                        matchCountryImageView.setImageResource(R.drawable.flag_fr);
                        break;
                    case GB:
                        matchCountryImageView.setImageResource(R.drawable.flag_gb);
                        break;
                    case SE:
                        matchCountryImageView.setImageResource(R.drawable.flag_se);
                        break;
                    default:
                        matchCountryImageView.setImageResource(R.drawable.flag_us);
                        break;
                }
            } else {
                matchCountryImageView.setVisibility(View.GONE);
            }

            // Score
            if (match.scoreHome.score > -1 && match.scoreAway.score > -1) {
                matchScoreHomeTextView.setText(String.valueOf(match.scoreHome.score));
                matchScoreAwayTextView.setText(String.valueOf(match.scoreAway.score));

                if (match.scoreHome.score == match.scoreAway.score) {
                    matchScoreHomeTextView.setBackground(getResources().getDrawable(R.drawable.server_score_tile_draw));
                    matchScoreAwayTextView.setBackground(getResources().getDrawable(R.drawable.server_score_tile_draw));
                } else if (match.scoreHome.score > match.scoreAway.score) {
                    matchScoreHomeTextView.setBackground(getResources().getDrawable(R.drawable.server_score_tile_win));
                    matchScoreAwayTextView.setBackground(getResources().getDrawable(R.drawable.server_score_tile_lose));
                } else {
                    matchScoreHomeTextView.setBackground(getResources().getDrawable(R.drawable.server_score_tile_lose));
                    matchScoreAwayTextView.setBackground(getResources().getDrawable(R.drawable.server_score_tile_win));
                }
            }

            // Map
            if (match.map != null) {
                try {
                    InputStream inputStreamMapImage = getResources().getAssets().open(
                            String.format("maps/%s.png", match.map));
                    Drawable drawableMapImage = Drawable.createFromStream(inputStreamMapImage, null);
                    serverMapImageView.setImageDrawable(drawableMapImage);
                } catch (IOException ex) {
                    Log.w(TAG, "doUpdateView: Map image not found: " + ex.getMessage());
                }
            }

            // Date
            if (match.date != null) {
                this.matchDateTextView.setVisibility(View.VISIBLE);
                SimpleDateFormat sdf = new SimpleDateFormat("dd. MMM yy");
                this.matchDateTextView.setText(sdf.format(match.date));
            } else {
                this.matchDateTextView.setVisibility(View.GONE);
            }

            // Service
            if (match.serviceId.equalsIgnoreCase(EseaServiceConfig.SERVICE_ID)) {
                matchServiceImageView.setImageResource(R.drawable.logo_esea);
            } else if (match.serviceId.equalsIgnoreCase(LeetwayServiceConfig.SERVICE_ID)) {
                matchServiceImageView.setImageResource(R.drawable.logo_leetway);
            }
        }
    }

    // ... /DO

    // ADAPTER

    public class MatchPagerAdapter extends FragmentPagerAdapter {

        public MatchPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment;
            switch (i) {
                default:
                    fragment = StatsMatchFragment.createStatsMatchFragment(getApplicationContext(), serviceId, matchId);
                    break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            return MATCH_CHILDS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case MATCH_CHILD_STATS:
                    return "Statistics";
            }
            return null;
        }
    }

    // /ADAPTER

}
