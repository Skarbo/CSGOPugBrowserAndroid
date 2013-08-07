package com.skarbo.csgobrowser.activity;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;

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
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.config.service.EseaServiceConfig;
import com.skarbo.csgobrowser.config.service.LeetwayServiceConfig;
import com.skarbo.csgobrowser.container.MatchesContainer.Match;
import com.skarbo.csgobrowser.fragment.match.StatsMatchFragment;
import com.skarbo.csgobrowser.handler.Handler;
import com.skarbo.csgobrowser.has.HasMatch;
import com.skarbo.csgobrowser.listener.HandlerListener;
import com.skarbo.csgobrowser.utils.Utils;

public class MatchActivity extends SherlockFragmentActivity implements HasMatch, HandlerListener {

	private static final String TAG = MatchActivity.class.getSimpleName();
	private static final String ARG_SERVICE_ID = "service_id";
	private static final String ARG_MATCH_ID = "match_id";
	private static final int MENU_REFRESH = 0;
	private static final int MATCH_CHILDS = 1;
	private static final int MATCH_CHILD_STATS = 0;

	private Handler handler;

	private String serviceId;
	private String matchId;

	private MenuItem menuRefresh;
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
		this.matchId = getIntent().getExtras().getString(ARG_MATCH_ID, "");

		// /HANDLER

		// ACTIONBAR

		getSupportActionBar().setTitle(R.string.match);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// /ACTIONBAR

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
		if (this.handler != null)
			this.handler.getControlHandler().doMatch(this.serviceId, this.matchId);
	}

	// ... ... /HANDLER

	// ... /ON

	// ... GET

	@Override
	public Handler getHandler() {
		return handler;
	}

	@Override
	public Match getMatch() {
		return this.handler.getContainers().matchesContainer.getMatch(this.serviceId, this.matchId);
	}

	// ... /GET

	// ... DO

	private void doRefresh() {
		this.handler.doRefresh();
	}

	private void doRefreshAnimation(boolean active) {
		Utils.rotateMenuItem(getApplicationContext(), this.menuRefresh, active);
	}

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
