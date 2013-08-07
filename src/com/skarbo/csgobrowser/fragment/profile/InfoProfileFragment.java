package com.skarbo.csgobrowser.fragment.profile;

import java.util.Map;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats;
import com.skarbo.csgobrowser.container.ProfilesContainer.Profile;
import com.skarbo.csgobrowser.handler.Handler;
import com.skarbo.csgobrowser.has.HasProfile;
import com.skarbo.csgobrowser.listener.HandlerListener;
import com.skarbo.csgobrowser.utils.Utils;
import com.skarbo.csgobrowser.utils.Utils.TitleAcronym;
import com.skarbo.csgobrowser.view.AccuracyView;
import com.skarbo.csgobrowser.view.GaugeView;

public class InfoProfileFragment extends Fragment implements HasProfile, HandlerListener {

	// ... ON

	private static final String TAG = InfoProfileFragment.class.getSimpleName();

	private Profile profile;
	private Handler handler;
	private ServiceConfig serviceConfig;
	private Map<String, TitleAcronym> playerStatsTitleAcronym;

	private LayoutInflater inflater;
	private LinearLayout profileStatMainLayout;
	private GaugeView profileStatsWLRGauge;
	private TextView profileStatsWLR;
	private TextView profileStatsWinTieLose;
	private TextView profileStatsFrags;
	private TextView profileStatsDeaths;
	private GaugeView profileStatsADRGauge;
	private TextView profileStatsADR;
	private TextView profileStatsADRSubtitle;
	private TextView profileStatsKDR;
	private AccuracyView profileStatsHSPAccuracy;
	private TextView profileStatsHSPTextView;
	private TextView profileStatsHeadshotsTextView;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		this.handler = ((HandlerListener) getActivity()).getHandler();

		this.profile = ((HasProfile) getParentFragment()).getProfile();

		this.serviceConfig = this.handler.getPreferenceHandler().createServiceConfig(this.profile.serviceId);
		this.playerStatsTitleAcronym = Utils.parseTitleAcronymMap(getResources().getStringArray(R.array.player_stats));

		doUpdateView();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.inflater = inflater;
		View view = inflater.inflate(R.layout.fragment_profile_info, container, false);

		this.profileStatMainLayout = (LinearLayout) view.findViewById(R.id.profileStatMainLayout);

		this.profileStatsADRGauge = (GaugeView) view.findViewById(R.id.profileStatsADRGauge);
		this.profileStatsADR = (TextView) view.findViewById(R.id.profileStatsADR);
		this.profileStatsADRSubtitle = (TextView) view.findViewById(R.id.profileStatsADRSubtitle);
		this.profileStatsHSPAccuracy = (AccuracyView) view.findViewById(R.id.profileStatsHSPAccuracy);
		this.profileStatsHSPTextView = (TextView) view.findViewById(R.id.profileStatsHSPTextView);
		this.profileStatsHeadshotsTextView = (TextView) view.findViewById(R.id.profileStatsHeadshotsTextView);
		this.profileStatsWLRGauge = (GaugeView) view.findViewById(R.id.profileStatsWLRGauge);
		this.profileStatsWLR = (TextView) view.findViewById(R.id.profileStatsWLR);
		this.profileStatsWinTieLose = (TextView) view.findViewById(R.id.profileStatsWinTieLose);

		this.profileStatsFrags = (TextView) view.findViewById(R.id.profileStatsFrags);
		this.profileStatsDeaths = (TextView) view.findViewById(R.id.profileStatsDeaths);
		this.profileStatsKDR = (TextView) view.findViewById(R.id.profileStatsKDR);

		this.profileStatsADRGauge.setMinValue(0f);
		this.profileStatsADRGauge.setMaxValue(200f);
		this.profileStatsADRGauge.setSplitterValue(100f);

		this.profileStatsWLRGauge.setMinValue(0f);
		this.profileStatsWLRGauge.setMaxValue(2f);
		this.profileStatsWLRGauge.setSplitterValue(1f);

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
		Log.d(TAG, "DoUpdateView");
		Profile profile = getProfile();
		if (profile == null)
			return;

		// MAIN STATS

		this.profileStatMainLayout.removeAllViews();
		for (Stats.Stat mainStat : this.serviceConfig.pages.profile.getMainStats()) {
			View mainStatView = createMainStat(profile, mainStat);
			if (mainStatView != null)
				this.profileStatMainLayout.addView(mainStatView);
		}

		// /MAIN STATS

		// OVERALL

		// ... ADR

		double damagePrRound = Utils.parseDouble(profile.stats.stats.get(Stats.Stat.damagePrRound));
		this.profileStatsADRGauge.doSetValue((float) damagePrRound);
		this.profileStatsADR.setText(String.format("%,.2f", damagePrRound));

		// Rounds played
		if (profile.stats.stats.containsKey(Stats.Stat.roundsPlayed)) {
			this.profileStatsADRSubtitle.setText(String.format(getResources().getString(R.string.stats_rounds_played,
					String.format("%,d", Utils.parseInt(profile.stats.stats.get(Stats.Stat.roundsPlayed))))));
		}
		// Time
		else if (profile.stats.stats.containsKey(Stats.Stat.time)) {
			int[] secondsParsed = Utils.parseSeconds(Utils.parseInt(profile.stats.stats.get(Stats.Stat.time)));
			String timeString = "", timePostfix = "";
			for (int i = 0; i < secondsParsed.length; i++) {
				if (i == 1)
					timePostfix = "m";
				else if (i == 2)
					timePostfix = "h";
				else if (i == 3)
					timePostfix = "d";
				else
					timePostfix = "s";
				timeString = String.format("%d%s %s", secondsParsed[i], timePostfix, timeString);
			}
			this.profileStatsADRSubtitle.setText(timeString.trim());
		}

		// ... /ADR

		// ... HEADSHOT

		double hsp = Utils.parseDouble(profile.stats.stats.get(Stats.Stat.headshotPercentage));
		this.profileStatsHSPAccuracy.doSetAccuracy(hsp);
		this.profileStatsHSPTextView.setText(String.format("%.1f%%", hsp * 100));
		if (profile.stats.stats.containsKey(Stats.Stat.headshots)) {
			int headshots = Utils.parseInt(profile.stats.stats.get(Stats.Stat.headshots));
			this.profileStatsHeadshotsTextView.setText(String.format(getResources().getString(R.string.stats_headshots,
					String.format("%,d", headshots))));
			this.profileStatsHeadshotsTextView.setVisibility(View.VISIBLE);
		} else
			this.profileStatsHeadshotsTextView.setVisibility(View.GONE);

		// ... /HEADSHOT

		double winLoseRatio = Utils.parseDouble(profile.stats.stats.get(Stats.Stat.winLoseRatio));
		this.profileStatsWLRGauge.doSetValue((float) winLoseRatio);
		this.profileStatsWLR.setText(String.format("%,.2f", winLoseRatio));
		this.profileStatsWinTieLose.setText(String.format("W%s T%s L%s",
				profile.stats.stats.get(Stats.Stat.wonMatches), profile.stats.stats.get(Stats.Stat.tiedMatches),
				profile.stats.stats.get(Stats.Stat.lostMatches)));

		this.profileStatsFrags.setText(String.format("%,d", Utils.parseInt(profile.stats.stats.get(Stats.Stat.frags))));
		this.profileStatsDeaths
				.setText(String.format("%,d", Utils.parseInt(profile.stats.stats.get(Stats.Stat.deaths))));
		this.profileStatsKDR.setText(String.format("%,.2f",
				Utils.parseDouble(profile.stats.stats.get(Stats.Stat.killDeathRatio))));

		// /OVERALL
	}

	// ... /DO

	// ... CREATE

	private View createMainStat(Profile profile, Stats.Stat stat) {
		if (!profile.stats.stats.containsKey(stat))
			return null;

		View view = inflater.inflate(R.layout.layout_profile_info_mainstat, null, false);

		TextView profileStatMainDescTextView = ((TextView) view.findViewById(R.id.profileStatMainDescTextView));
		TextView profileStatMainTextView = ((TextView) view.findViewById(R.id.profileStatMainTextView));

		if (this.playerStatsTitleAcronym.containsKey(stat.toString()))
			profileStatMainDescTextView.setText(this.playerStatsTitleAcronym.get(stat.toString()).title);
		else
			profileStatMainDescTextView.setText(stat.toString());

		String mainStat = profile.stats.stats.get(stat);
		if (!Utils.isEmpty(mainStat)) {
			profileStatMainTextView.setText(String.format("%,.2f", Utils.parseDouble(mainStat.replace(",", ""))));
			profileStatMainTextView.setVisibility(View.VISIBLE);
		} else {
			profileStatMainTextView.setVisibility(View.GONE);
		}

		return view;
	}

	// ... /CREATE

}
