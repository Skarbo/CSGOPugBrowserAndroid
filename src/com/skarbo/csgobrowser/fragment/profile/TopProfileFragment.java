package com.skarbo.csgobrowser.fragment.profile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import android.app.Activity;
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
import android.widget.PopupWindow;
import android.widget.TableRow;
import android.widget.TextView;

import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats.Stat;
import com.skarbo.csgobrowser.container.ProfilesContainer.Profile;
import com.skarbo.csgobrowser.handler.Handler;
import com.skarbo.csgobrowser.has.HasProfile;
import com.skarbo.csgobrowser.listener.HandlerListener;
import com.skarbo.csgobrowser.utils.Utils;
import com.skarbo.csgobrowser.utils.Utils.TitleAcronym;

public class TopProfileFragment extends Fragment implements HasProfile, HandlerListener {

	// ... ON

	private static final String TAG = TopProfileFragment.class.getSimpleName();

	private Profile profile;
	private Handler handler;
	private ServiceConfig serviceConfig;
	private Map<String, TitleAcronym> statsTitleAcronym;

	private View view;
	private LinearLayout profileTopMaps;
	private LayoutInflater inflater;
	private LinearLayout profileTopMapsHeaderStats;
	private LinearLayout profileTopWeaponsHeaderStats;
	private LinearLayout profileTopWeapons;
	private TextView profileTopNoneTextView;
	private LinearLayout profileTopMapsLayout;
	private LinearLayout profileTopWeaponsLayout;


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
		this.view = inflater.inflate(R.layout.fragment_profile_top, container, false);

		this.profileTopNoneTextView = (TextView) view.findViewById(R.id.profileTopNoneTextView);
		this.profileTopMapsLayout = (LinearLayout) view.findViewById(R.id.profileTopMapsLayout);
		this.profileTopWeaponsLayout = (LinearLayout) view.findViewById(R.id.profileTopWeaponsLayout);

		this.profileTopMapsHeaderStats = (LinearLayout) view.findViewById(R.id.profileTopMapsHeaderStats);
		this.profileTopMaps = (LinearLayout) view.findViewById(R.id.profileTopMaps);
		this.profileTopWeaponsHeaderStats = (LinearLayout) view.findViewById(R.id.profileTopWeaponsHeaderStats);
		this.profileTopWeapons = (LinearLayout) view.findViewById(R.id.profileTopWeapons);

		this.profileTopNoneTextView.setVisibility(View.VISIBLE);
		this.profileTopMapsLayout.setVisibility(View.GONE);
		this.profileTopWeaponsLayout.setVisibility(View.GONE);

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
		Profile profile = getProfile();

		if (profile == null) {
			this.profileTopNoneTextView.setVisibility(View.VISIBLE);
			this.profileTopMapsLayout.setVisibility(View.GONE);
			this.profileTopWeaponsLayout.setVisibility(View.GONE);
		} else {
			boolean topMaps = false;
			boolean topWeapons = false;

			// Maps
			Stat[] topMapsStats = this.serviceConfig.pages.profile.getTopMapsStats();
			if (!profile.stats.maps.isEmpty() && topMapsStats != null) {
				this.profileTopMapsLayout.setVisibility(View.VISIBLE);
				topMaps = true;

				// Header
				this.profileTopMapsHeaderStats.removeAllViews();
				this.profileTopMapsHeaderStats.setWeightSum(topMapsStats.length);
				for (Stat stat : topMapsStats) {
					String statHeader = stat.toString();
					if (this.statsTitleAcronym.containsKey(statHeader)) {
						TitleAcronym titleAcronym = this.statsTitleAcronym.get(statHeader);
						statHeader = titleAcronym.acronym != "" ? titleAcronym.acronym : titleAcronym.title;
					}
					this.profileTopMapsHeaderStats.addView(createTopHeaderStatsText(statHeader));
				}

				// Stat
				this.profileTopMaps.removeAllViews();
				int number = 1;
				for (Stats.Map map : profile.stats.maps) {
					this.profileTopMaps.addView(createTopMap(number++, map));
				}
			} else
				this.profileTopMapsLayout.setVisibility(View.GONE);

			// Weapons
			Stat[] topWeaponsStats = this.serviceConfig.pages.profile.getTopWeaponsStats();
			if (!profile.stats.weapons.isEmpty() && topWeaponsStats != null) {
				topWeapons = true;
				this.profileTopWeaponsLayout.setVisibility(View.VISIBLE);

				// Header
				this.profileTopWeaponsHeaderStats.removeAllViews();
				this.profileTopWeaponsHeaderStats.setWeightSum(topWeaponsStats.length);
				for (Stat stat : topWeaponsStats) {
					String statHeader = stat.toString();
					if (this.statsTitleAcronym.containsKey(statHeader)) {
						TitleAcronym titleAcronym = this.statsTitleAcronym.get(statHeader);
						statHeader = titleAcronym.acronym != "" ? titleAcronym.acronym : titleAcronym.title;
					}
					this.profileTopWeaponsHeaderStats.addView(createTopHeaderStatsText(statHeader));
				}

				// Stat
				this.profileTopWeapons.removeAllViews();
				int number = 1;
				for (Stats.Weapon weapon : profile.stats.weapons) {
					this.profileTopWeapons.addView(createTopWeapon(number++, weapon));
				}
			} else
				this.profileTopWeaponsLayout.setVisibility(View.GONE);

			if (topWeapons || topMaps)
				this.profileTopNoneTextView.setVisibility(View.GONE);
			else
				this.profileTopNoneTextView.setVisibility(View.VISIBLE);
		}

	}

	// ... /DO

	private LinearLayout createTopMap(int number, Stats.Map map) {
		LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.layout_profile_top_map, null, false);

		((TextView) linearLayout.findViewById(R.id.topMapNumber)).setText(String.valueOf(number));
		((TextView) linearLayout.findViewById(R.id.topMapName)).setText(map.map);

		try {
			InputStream inputStreamMapImage = getResources().getAssets().open(String.format("maps/%s.png", map.map));
			Drawable drawableMapImage = Drawable.createFromStream(inputStreamMapImage, null);
			((ImageView) linearLayout.findViewById(R.id.topMapImage)).setImageDrawable(drawableMapImage);
		} catch (IOException ex) {
			Log.w(TAG, "createMap: Map (" + map.map + ") image not found: " + ex.getMessage());
		}

		// STATS

		LinearLayout topMapStats = (LinearLayout) linearLayout.findViewById(R.id.topMapStats);
		topMapStats.removeAllViews();

		Stat[] topMapsStats = this.serviceConfig.pages.profile.getTopMapsStats();
		topMapStats.setWeightSum(topMapsStats.length);
		for (Stat stat : topMapsStats) {
			String statString = "";
			if (map.stats.containsKey(stat)) {
				statString = map.stats.get(stat);
				try {
					statString = String.format("%,d", Integer.parseInt(statString));
				} catch (Exception e) {
				}
			}
			topMapStats.addView(createTopStatsText(statString));
		}

		// /STATS

		return linearLayout;
	}

	private LinearLayout createTopWeapon(int number, final Stats.Weapon weapon) {
		LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.layout_profile_top_weapon, null, false);

		((TextView) linearLayout.findViewById(R.id.topWeaponNumber)).setText(String.valueOf(number));
		((TextView) linearLayout.findViewById(R.id.topWeaponName)).setText(weapon.name);

		try {
			String weaponFile = weapon.name.toLowerCase().replaceAll("[^\\w\\d]+", "");
			InputStream inputStreamMapImage = getResources().getAssets().open(
					String.format("weapons/%s.png", weaponFile));
			final Drawable drawableMapImage = Drawable.createFromStream(inputStreamMapImage, null);
			((ImageView) linearLayout.findViewById(R.id.topWeaponImage)).setImageDrawable(drawableMapImage);
			((ImageView) linearLayout.findViewById(R.id.topWeaponImage)).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.d(TAG, "Popupimage");
					createPopupImage(drawableMapImage, weapon.name);
				}
			});
		} catch (IOException ex) {
			Log.w(TAG, "createWeapon: Weapon (" + weapon.name + ") image not found: " + ex.getMessage());
		}

		// STATS

		LinearLayout topWeaponStats = (LinearLayout) linearLayout.findViewById(R.id.topWeaponStats);
		topWeaponStats.removeAllViews();

		Stat[] topWeaponsStats = this.serviceConfig.pages.profile.getTopWeaponsStats();
		topWeaponStats.setWeightSum(topWeaponsStats.length);
		for (Stat stat : topWeaponsStats) {
			String statString = "";
			if (weapon.stats.containsKey(stat)) {
				statString = weapon.stats.get(stat);
				try {
					statString = String.format("%,d", Integer.parseInt(statString));
				} catch (NumberFormatException e) {
				}
			}
			topWeaponStats.addView(createTopStatsText(statString));
		}

		// /STATS

		return linearLayout;
	}

	private TextView createTopStatsText(String text) {
		TextView textView = new TextView(getActivity());
		textView.setLayoutParams(new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f));
		textView.setTextAppearance(getActivity(), R.style.Profile_Top_Stat);
		textView.setText(text);
		textView.setGravity(Gravity.CENTER);
		return textView;
	}

	private TextView createTopHeaderStatsText(String text) {
		TextView textView = createTopStatsText(text);
		textView.setTextAppearance(getActivity(), R.style.Profile_Top_Header);
		return textView;
	}

	private void createPopupImage(Drawable drawableMapImage, String text) {
		LayoutInflater layoutInflater = (LayoutInflater) getActivity().getBaseContext().getSystemService(
				Activity.LAYOUT_INFLATER_SERVICE);
		View popupView = layoutInflater.inflate(R.layout.layout_popup_image, null);
		final PopupWindow popupWindow = new PopupWindow(popupView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		((ImageView) popupView.findViewById(R.id.popupImageImageView)).setImageDrawable(drawableMapImage);
		((TextView) popupView.findViewById(R.id.popupImageTextView)).setText(text);

		OnClickListener onClickListener = new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				popupWindow.dismiss();
			}
		};
		popupView.setOnClickListener(onClickListener);
		((ImageView) popupView.findViewById(R.id.popupImageImageView)).setOnClickListener(onClickListener);

		popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
	}

}
