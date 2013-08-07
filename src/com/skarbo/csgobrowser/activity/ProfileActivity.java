package com.skarbo.csgobrowser.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.fragment.ProfileFragment;
import com.skarbo.csgobrowser.handler.Handler;
import com.skarbo.csgobrowser.listener.HandlerListener;
import com.skarbo.csgobrowser.utils.Utils;

public class ProfileActivity extends SherlockFragmentActivity implements HandlerListener {

	private static final String TAG = ProfileActivity.class.getSimpleName();
	private static final String ARG_SERVICE_ID = "service_id";
	private static final String ARG_PROFILE_ID = "profile_id";
	private static final int MENU_REFRESH = 0;

	private MenuItem menuRefresh;
	private Handler handler;

	public static Intent createProfileActivity(Context context, String serviceId, String profileId) {
		Intent intent = new Intent(context, ProfileActivity.class);
		intent.putExtra(ARG_SERVICE_ID, serviceId);
		intent.putExtra(ARG_PROFILE_ID, profileId);
		return intent;
	}

	// ... ON

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// HANDLER

		this.handler = new Handler(getApplicationContext());
		try {
			this.handler.doContainersCacheLoad();
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "Could not load cache", Toast.LENGTH_SHORT).show();
			Log.e(TAG, "doContainersCacheLoad: " + e.getMessage());
		}

		// /HANDLER

		// ACTIONBAR

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// /ACTIONBAR

		Bundle extras = getIntent().getExtras();

		String serviceId = extras.getString(ARG_SERVICE_ID);
		String profileId = extras.getString(ARG_PROFILE_ID);

		if (Utils.isEmpty(serviceId) || Utils.isEmpty(profileId)) {
			Toast.makeText(getApplicationContext(), "Service or profile id not given", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		// VIEW

		setContentView(R.layout.frame_content);
		getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.content_frame,
						ProfileFragment.createProfileFragment(getApplicationContext(), serviceId, profileId)).commit();

		// /VIEW
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (this.handler != null) {
			this.handler.addListener(TAG, this);
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
	public void onUpdating() {
		if (this.menuRefresh != null)
			this.menuRefresh.setEnabled(false);
		this.doRefreshAnimation(true);
	}

	@Override
	public void onUpdated() {
		if (this.menuRefresh != null)
			this.menuRefresh.setEnabled(true);
		this.doRefreshAnimation(false);
	}

	@Override
	public void onRefresh() {

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

	// ... /ON

	// ... DO

	private void doRefresh() {
		Log.d(TAG, "Do refresh");
		this.handler.doRefresh();
	}

	private void doRefreshAnimation(boolean active) {
		Utils.rotateMenuItem(getApplicationContext(), this.menuRefresh, active);
	}

	// ... /DO

	@Override
	public Handler getHandler() {
		return this.handler;
	}

}
