package com.skarbo.csgobrowser.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.container.ProfilesContainer.Profile;
import com.skarbo.csgobrowser.fragment.profile.InfoProfileFragment;
import com.skarbo.csgobrowser.fragment.profile.MatchesProfileFragment;
import com.skarbo.csgobrowser.fragment.profile.StatsProfileFragment;
import com.skarbo.csgobrowser.fragment.profile.TopProfileFragment;
import com.skarbo.csgobrowser.handler.Handler;
import com.skarbo.csgobrowser.has.HasProfile;
import com.skarbo.csgobrowser.listener.HandlerListener;
import com.skarbo.csgobrowser.utils.DownloadImageTask;
import com.skarbo.csgobrowser.utils.Utils;

public class ProfileFragment extends Fragment implements HandlerListener, HasProfile {

	private static final String TAG = ProfileFragment.class.getSimpleName();
	private static final String ARG_SERVICE_ID = "service_id";
	private static final String ARG_PROFILE_ID = "profile_id";

	private static final int PROFILE_CHILDS = 4;
	private static final int PROFILE_CHILD_INFO = 0;
	private static final int PROFILE_CHILD_STATS = 1;
	private static final int PROFILE_CHILD_MATCHES = 2;
	private static final int PROFILE_CHILD_TOP = 3;

	private String serviceId;
	private String profileId;
	private Handler handler;
	private ServiceConfig serviceConfig;

	private View profileLayout;
	private ImageView profileImageImageView;
	private TextView profileNicknameTextView;
	private TextView profileNameTextView;
	private ImageView profileCountryImageImageView;
	private ImageView profileServiceImageView;
	private TextView profileLoadingTextView;
	private ViewPager profileViewPager;

	private ProfilePagerAdapter profilePagerAdapter;

	public static ProfileFragment createProfileFragment(Context context, String serviceId, String profileId) {
		ProfileFragment profileFragment = new ProfileFragment();
		Bundle bundle = new Bundle();
		bundle.putString(ARG_SERVICE_ID, serviceId);
		bundle.putString(ARG_PROFILE_ID, profileId);
		profileFragment.setArguments(bundle);
		return profileFragment;
	}

	// ... ON

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		this.serviceId = getArguments().getString(ARG_SERVICE_ID);
		this.profileId = getArguments().getString(ARG_PROFILE_ID);

		if (Utils.isEmpty(serviceId) || Utils.isEmpty(profileId)) {
			Toast.makeText(getActivity(), "Service or profile id not given", Toast.LENGTH_SHORT).show();
			return;
		}

		this.handler = ((HandlerListener) getActivity()).getHandler();
		this.serviceConfig = this.handler.getPreferenceHandler().createServiceConfig(this.serviceId);

		this.profilePagerAdapter = new ProfilePagerAdapter(getChildFragmentManager());

		this.profileViewPager.setAdapter(profilePagerAdapter);
		this.profileViewPager.setCurrentItem(PROFILE_CHILD_INFO);

		((SherlockFragmentActivity) getActivity()).getSupportActionBar().setTitle(R.string.profile);
		doUpdateView();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_profile, container, false);

		this.profileLayout = (View) view.findViewById(R.id.profileLayout);
		this.profileImageImageView = (ImageView) view.findViewById(R.id.profileImageImageView);
		this.profileNicknameTextView = (TextView) view.findViewById(R.id.profileNicknameTextView);
		this.profileNameTextView = (TextView) view.findViewById(R.id.profileNameTextView);
		this.profileCountryImageImageView = (ImageView) view.findViewById(R.id.profileCountryImageImageView);
		this.profileServiceImageView = (ImageView) view.findViewById(R.id.profileServiceImageView);
		this.profileLoadingTextView = (TextView) view.findViewById(R.id.profileLoadingTextView);

		this.profileViewPager = (ViewPager) view.findViewById(R.id.profileChildViewPager);

		this.profileLoadingTextView.setVisibility(View.VISIBLE);
		this.profileLayout.setVisibility(View.GONE);
		this.profileViewPager.setVisibility(View.GONE);

		return view;
	}

	@Override
	public void onResume() {
		super.onPause();
		if (this.handler != null) {
			this.handler.addListener(TAG, this);
			this.handler.doRefresh();
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
		if (this.handler != null) {
			Log.d(TAG, "OnRefresh: Do profile: " + serviceId + ", " + profileId);
			this.handler.getControlHandler().doProfile(serviceId, profileId);
		}
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
		if (this.handler != null)
			return this.handler.getContainers().profilesContainer.getProfile(serviceId, profileId);
		return null;
	}

	// ... /GET

	// ... DO

	private void doUpdateView() {
		Profile profile = getProfile();

		if (profile == null) {
			this.profileLoadingTextView.setVisibility(View.VISIBLE);
			this.profileLayout.setVisibility(View.GONE);
			this.profileViewPager.setVisibility(View.GONE);
		} else {
			this.profileLoadingTextView.setVisibility(View.GONE);
			this.profileLayout.setVisibility(View.VISIBLE);
			this.profileViewPager.setVisibility(View.VISIBLE);

			((SherlockFragmentActivity) getActivity()).getSupportActionBar().setSubtitle(profile.nickname);
			this.profileNicknameTextView.setText(profile.nickname);

			// Service image
			this.profileServiceImageView.setImageDrawable(getResources().getDrawable(
					this.serviceConfig.getServiceImage()));

			// Name
			if (!Utils.isEmpty(profile.name)) {
				this.profileNameTextView.setText(profile.name);
				this.profileNameTextView.setVisibility(View.VISIBLE);
			} else
				this.profileNameTextView.setVisibility(View.GONE);

			// Image
			if (profile.image != null) {
				Bitmap bitmapFromCache = this.handler.getBitmapFromCache(profile.image);

				if (bitmapFromCache != null) {
					Log.d(TAG, "doUpdateView: Set bitmap from cache: " + profile.image);
					this.profileImageImageView.setImageBitmap(bitmapFromCache);
				} else {
					this.profileImageImageView.setTag(profile.image);
					(new DownloadImageTask(getHandler())).execute(this.profileImageImageView);
				}
			}

			// Country image
			if (profile.countryImage != null) {
				Bitmap bitmapFromCache = this.handler.getBitmapFromCache(profile.countryImage);

				if (bitmapFromCache != null) {
					this.profileCountryImageImageView.setImageBitmap(bitmapFromCache);
				} else {
					this.profileCountryImageImageView.setTag(profile.countryImage);
					(new DownloadImageTask(getHandler())).execute(this.profileCountryImageImageView);
				}
				this.profileImageImageView.setVisibility(View.VISIBLE);
			} else {
				this.profileImageImageView.setVisibility(View.GONE);
			}

		}
	}

	// ... /DO

	// ADAPTER

	public class ProfilePagerAdapter extends FragmentPagerAdapter {

		public ProfilePagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			Fragment fragment;
			switch (i) {
			case PROFILE_CHILD_TOP:
				fragment = new TopProfileFragment();
				break;
			case PROFILE_CHILD_MATCHES:
				fragment = new MatchesProfileFragment();
				break;
			case PROFILE_CHILD_STATS:
				fragment = new StatsProfileFragment();
				break;
			default:
				fragment = new InfoProfileFragment();
				break;
			}
			return fragment;
		}

		@Override
		public int getCount() {
			return PROFILE_CHILDS;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case PROFILE_CHILD_INFO:
				return "Info";
			case PROFILE_CHILD_TOP:
				return "Top";
			case PROFILE_CHILD_MATCHES:
				return "Matches";
			case PROFILE_CHILD_STATS:
				return "Statistics";
			}
			return null;
		}
	}

	// /ADAPTER
}
