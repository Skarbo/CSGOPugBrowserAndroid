package com.skarbo.csgobrowser.fragment.profile;

import java.util.Map;
import java.util.Map.Entry;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.container.PlayersContainer;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats.Stat;
import com.skarbo.csgobrowser.container.ProfilesContainer;
import com.skarbo.csgobrowser.container.ProfilesContainer.Profile;
import com.skarbo.csgobrowser.handler.Handler;
import com.skarbo.csgobrowser.has.HasProfile;
import com.skarbo.csgobrowser.listener.HandlerListener;
import com.skarbo.csgobrowser.utils.Utils;
import com.skarbo.csgobrowser.utils.Utils.TitleAcronym;

public class StatsProfileFragment extends Fragment implements HasProfile, HandlerListener {

	// ... ON

	private static final String TAG = StatsProfileFragment.class.getSimpleName();

	private Profile profile;
	private Handler handler;
	private ServiceConfig serviceConfig;
	private Map<String, TitleAcronym> statsTitleAcronym;

	private LayoutInflater inflater;
	private View view;

	private LinearLayout profileStatsLayout;
	private LinearLayout profileStatsContainerLayout;
	private TextView profileStatsNoneTextView;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		this.handler = ((HandlerListener) getActivity()).getHandler();
		this.profile = ((HasProfile) getParentFragment()).getProfile();
		this.serviceConfig = this.handler.getPreferenceHandler().createServiceConfig(this.profile.serviceId);
		this.statsTitleAcronym = Utils.parseTitleAcronymMap(getResources().getStringArray(R.array.player_stats));

		doUpdateView();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.inflater = inflater;
		this.view = inflater.inflate(R.layout.fragment_profile_stats, container, false);

		this.profileStatsLayout = (LinearLayout) view.findViewById(R.id.profileStatsLayout);
		this.profileStatsContainerLayout = (LinearLayout) view.findViewById(R.id.profileStatsContainerLayout);
		this.profileStatsNoneTextView = (TextView) view.findViewById(R.id.profileStatsNoneTextView);

		this.profileStatsLayout.setVisibility(View.GONE);
		this.profileStatsNoneTextView.setVisibility(View.VISIBLE);

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

		if (profile == null) {
			this.profileStatsLayout.setVisibility(View.GONE);
			this.profileStatsNoneTextView.setVisibility(View.VISIBLE);
		} else {
			this.profileStatsLayout.setVisibility(View.VISIBLE);
			this.profileStatsNoneTextView.setVisibility(View.GONE);

			this.profileStatsContainerLayout.removeAllViews();

			for (Entry<Stat, String> entry : profile.stats.stats.entrySet()) {
				this.profileStatsContainerLayout.addView(createStatsLayout(entry.getKey(), entry.getValue()));
			}
		}
	}

	// ... /DO

	// ... CREATE

	private RelativeLayout createStatsLayout(Stat stat, String value) {
		RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.layout_profile_stat, null, false);

		TextView profileStatNameTextView = (TextView) layout.findViewById(R.id.profileStatNameTextView);
		TextView profileStatValueTextView = (TextView) layout.findViewById(R.id.profileStatValueTextView);

		// Name
		String statName = stat.toString();
		if (this.statsTitleAcronym.containsKey(statName))
			statName = this.statsTitleAcronym.get(statName).title;
		profileStatNameTextView.setText(statName);

		// Value
		profileStatValueTextView.setText(value);

		return layout;
	}

	// ... /CREATE

}
