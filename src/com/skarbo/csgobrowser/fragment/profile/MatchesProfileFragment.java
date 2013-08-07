package com.skarbo.csgobrowser.fragment.profile;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Map;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.activity.MatchActivity;
import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.container.MatchesContainer.Match;
import com.skarbo.csgobrowser.container.MatchesContainer;
import com.skarbo.csgobrowser.container.PlayersContainer.Player;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats.Stat;
import com.skarbo.csgobrowser.container.ProfilesContainer;
import com.skarbo.csgobrowser.container.ProfilesContainer.Profile;
import com.skarbo.csgobrowser.handler.Handler;
import com.skarbo.csgobrowser.has.HasProfile;
import com.skarbo.csgobrowser.listener.HandlerListener;
import com.skarbo.csgobrowser.utils.Utils;
import com.skarbo.csgobrowser.utils.Utils.TitleAcronym;

public class MatchesProfileFragment extends Fragment implements HasProfile, HandlerListener {

	// ... ON

	private static final String TAG = MatchesProfileFragment.class.getSimpleName();

	private Profile profile;
	private Handler handler;
	private ServiceConfig serviceConfig;
	private Map<String, TitleAcronym> statsTitleAcronym;

	private LayoutInflater inflater;
	private View view;

	private LinearLayout profileMatchesLayout;
	private LinearLayout profileMatchesHeaderStats;
	private LinearLayout profileMatchesContainerLayout;
	private TextView profileMatchesNoneTextView;

	private SimpleDateFormat matchSimpleDateFormat;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		this.handler = ((HandlerListener) getActivity()).getHandler();
		this.profile = ((HasProfile) getParentFragment()).getProfile();
		this.serviceConfig = this.handler.getPreferenceHandler().createServiceConfig(this.profile.serviceId);
		this.statsTitleAcronym = Utils.parseTitleAcronymMap(getResources().getStringArray(R.array.player_stats));

		this.matchSimpleDateFormat = new SimpleDateFormat("dd. MMM yy");
		doUpdateView();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.inflater = inflater;
		this.view = inflater.inflate(R.layout.fragment_profile_matches, container, false);

		this.profileMatchesLayout = (LinearLayout) view.findViewById(R.id.profileMatchesLayout);
		this.profileMatchesHeaderStats = (LinearLayout) view.findViewById(R.id.profileMatchesHeaderStats);
		this.profileMatchesContainerLayout = (LinearLayout) view.findViewById(R.id.profileMatchesContainerLayout);
		this.profileMatchesNoneTextView = (TextView) view.findViewById(R.id.profileMatchesNoneTextView);

		this.profileMatchesLayout.setVisibility(View.GONE);
		this.profileMatchesNoneTextView.setVisibility(View.VISIBLE);

		return view;
	}

	@Override
	public void onResume() {
		super.onPause();
		if (this.handler != null) {
			this.handler.addListener(TAG, this);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.handler != null)
			this.handler.removeListener(TAG);
	}

	// ... ... HANDLER

	@Override
	public void onUpdating() {
		doUpdateView();
	}

	@Override
	public void onUpdated() {
		doUpdateView();
	}

	@Override
	public void onRefresh() {

	}

	// ... ... /HANDLER

	// ... /ON

	// ... GET

	@Override
	public Handler getHandler() {
		return this.handler;
	}

	@Override
	public Profile getProfile() {
		return this.profile;
	}

	// ... /GET

	// ... DO

	private void doUpdateView() {
		ProfilesContainer.Profile profile = getProfile();

		if (profile == null || profile.matchIds.isEmpty()) {
			this.profileMatchesLayout.setVisibility(View.GONE);
			this.profileMatchesNoneTextView.setVisibility(View.VISIBLE);
		} else {
			this.profileMatchesLayout.setVisibility(View.VISIBLE);
			this.profileMatchesNoneTextView.setVisibility(View.GONE);

			// Header
			this.profileMatchesHeaderStats.setWeightSum(this.serviceConfig.pages.profile.getMatchesStats().length);
			this.profileMatchesHeaderStats.removeAllViews();
			for (Stats.Stat stat : this.serviceConfig.pages.profile.getMatchesStats()) {
				String statString = stat.toString();
				if (this.statsTitleAcronym.containsKey(statString)) {
					TitleAcronym titleAcronym = this.statsTitleAcronym.get(statString);
					statString = titleAcronym.acronym != "" ? titleAcronym.acronym : titleAcronym.title;
				}
				this.profileMatchesHeaderStats.addView(createMatchesHeaderStatsText(statString));
			}

			// Matches
			this.profileMatchesContainerLayout.removeAllViews();
			for (String matchId : profile.matchIds) {
				Match match = this.handler.getContainers().matchesContainer.getMatch(profile.serviceId, matchId);
				if (match != null)
					this.profileMatchesContainerLayout.addView(createMatch(profile, match));
				else
					Log.w(TAG, "doUpdateView: Match is null: " + profile.serviceId + ", " + matchId);
			}
		}
	}

	// ... /DO

	// ... CREATE

	private LinearLayout createMatch(ProfilesContainer.Profile profile, final MatchesContainer.Match match) {
		LinearLayout view = (LinearLayout) inflater.inflate(R.layout.layout_profile_match, null, false);

		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(MatchActivity.createServerActivity(getActivity(), match.serviceId, match.id));
			}
		});

		// Score
		TextView matchScoreHomeTextView = (TextView) view.findViewById(R.id.profileMatchScoreHomeTextView);
		TextView matchScoreAwayTextView = (TextView) view.findViewById(R.id.profileMatchScoreAwayTextView);

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

		// Map
		ImageView profileMatchMapImageView = (ImageView) view.findViewById(R.id.profileMatchMapImageView);
		if (match.map != null) {
			try {
				InputStream inputStreamMapImage = getResources().getAssets().open(
						String.format("maps/%s.png", match.map));
				Drawable drawableMapImage = Drawable.createFromStream(inputStreamMapImage, null);
				profileMatchMapImageView.setImageDrawable(drawableMapImage);
			} catch (IOException ex) {
				Log.w(TAG, "createMatch: Map image not found: " + ex.getMessage());
			}
		}

		// Date
		String matchDate = "";
		TextView profileMatchDateTextView = (TextView) view.findViewById(R.id.profileMatchDateTextView);
		if (match.date != null)
			matchDate = matchSimpleDateFormat.format(match.date);
		profileMatchDateTextView.setText(matchDate);

		// Stats
		LinearLayout profileMatchStatsLayout = (LinearLayout) view.findViewById(R.id.profileMatchStatsLayout);
		profileMatchStatsLayout.removeAllViews();
		profileMatchStatsLayout.setWeightSum(this.serviceConfig.pages.profile.getMatchesStats().length);
		if (!match.playersContainer.players.isEmpty()) {
			Player player = match.playersContainer.getPlayer(match.serviceId, profile.id);
			if (player != null) {
				for (Stat stat : this.serviceConfig.pages.profile.getMatchesStats()) {
					String statString = player.stats.stats.get(stat);
					profileMatchStatsLayout.addView(createMatchStatsText(statString));
				}
			} else
				Log.w(TAG, "createMatch: Could not retrieve match player: " + match.serviceId + ", " + profile.id);
		}

		return view;
	}

	private TextView createMatchStatsText(String text) {
		TextView textView = new TextView(getActivity());
		textView.setLayoutParams(new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f));
		textView.setTextAppearance(getActivity(), R.style.Profile_Top_Stat);
		textView.setText(text);
		textView.setGravity(Gravity.CENTER);
		return textView;
	}

	private TextView createMatchesHeaderStatsText(String text) {
		TextView textView = createMatchStatsText(text);
		textView.setTextAppearance(getActivity(), R.style.Profile_Top_Header);
		return textView;
	}

	// ... /CREATE

}
