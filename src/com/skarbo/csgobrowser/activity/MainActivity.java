package com.skarbo.csgobrowser.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.fragment.MenuFragment;
import com.skarbo.csgobrowser.fragment.ServersFragment;
import com.skarbo.csgobrowser.handler.Handler;
import com.skarbo.csgobrowser.listener.HandlerListener;
import com.skarbo.csgobrowser.utils.Utils;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.app.SlidingFragmentActivity;

public class MainActivity extends SlidingFragmentActivity implements HandlerListener {

	private static final String TAG = MainActivity.class.getSimpleName();
	private static final String CONTENT_SAVE = "content";

	private static final int MENU_SETTINGS = 1;
	private static final int MENU_REFRESH = 2;
	private static final int MENU_CANCEL = 3;

	private Handler handler;

	private Fragment content;
	private MenuFragment menuFragment;
	private SlidingMenu slidingMenu;
	private MenuItem menuRefresh;
	private MenuItem menuSettings;
	private MenuItem menuCancel;

	// ... ON

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setSupportProgressBarIndeterminateVisibility(false);

		// ACTIONBAR

		getSupportActionBar().setTitle(R.string.app_name);

		// /ACTIONBAR

		// HANDLER

		this.handler = new Handler(this);
		try {
			this.handler.doContainersCacheLoad();
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "Could not load cache file", Toast.LENGTH_SHORT).show();
			Log.e(TAG, "onCreate: doContainersCacheLoad: " + e.getMessage());
		}

		// /HANDLER

		// CONTENT

		this.menuFragment = new MenuFragment();

		if (savedInstanceState != null)
			this.content = getSupportFragmentManager().getFragment(savedInstanceState, CONTENT_SAVE);

		// /CONTENT

		// VIEW

		// Set the Above View
		setContentView(R.layout.frame_content);
		if (this.content != null)
			getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, this.content).commit();
		else
			getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new ServersFragment()).commit();
		// getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,
		// ServerFragment.createServerFragment(LeetwayServiceConfig.SERVICE_ID,
		// "15")).commit();

		// Set the Behind View
		setBehindContentView(R.layout.frame_menu);
		getSupportFragmentManager().beginTransaction().replace(R.id.menu_frame, menuFragment).commit();

		// Customize the SlidingMenu
		SlidingMenu sm = getSlidingMenu();
		sm.setShadowWidthRes(R.dimen.shadow_width);
		sm.setShadowDrawable(R.drawable.shadow);
		sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
		sm.setFadeDegree(0.35f);
		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);

		// Set home as up
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// /VIEW

		// Intent matchIntent =
		// MatchActivity.createServerActivity(getApplicationContext(),
		// LeetwayServiceConfig.SERVICE_ID, "22468");
		// startActivity(matchIntent);
		// Intent profileIntent =
		// ProfileActivity.createProfileActivity(getApplicationContext(),
		// LeetwayServiceConfig.SERVICE_ID, "235");
		// startActivity(profileIntent);
		// Intent profileIntent =
		// ProfileActivity.createProfileActivity(getApplicationContext(),
		// EseaServiceConfig.SERVICE_ID, "523111");
		// startActivity(profileIntent);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");
		if (this.handler != null)
			this.handler.doContainersCacheDelete();
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "OnResume");

		if (this.handler != null)
			this.handler.addListener(TAG, this);

		doRefresh();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d(TAG, "OnActivityResult");

		if (this.handler != null) {
			try {
				this.handler.doContainersCacheLoad();
			} catch (Exception e) {
				Log.e(TAG, "onActivityResult: doContainersCacheLoad: " + e.getMessage());
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "OnPause");

		if (this.handler != null)
			this.handler.removeListener(TAG);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menuRefresh = menu.add(0, MENU_REFRESH, 1, "Refresh");
		menuRefresh.setIcon(R.drawable.navigation_refresh).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menuSettings = menu.add(0, MENU_SETTINGS, 0, "Settings");
		menuSettings.setIcon(R.drawable.action_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menuCancel = menu.add(0, MENU_CANCEL, 2, "Cancel");
		menuCancel.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		menuCancel.setEnabled(false);

		if (this.handler.isUpdating()) {
			this.menuRefresh.setEnabled(false);
			this.doRefreshAnimation(true);
			this.menuCancel.setEnabled(true);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_SETTINGS:
			doSettings();
			return true;
		case MENU_REFRESH:
			doRefresh();
			return true;
		case MENU_CANCEL:
			doCancel();
			return true;
		case android.R.id.home:
			toggle();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// ... /ON

	// ... GET

	public Handler getHandler() {
		return handler;
	}

	// ... /GET

	// ... ... HANDLER

	@Override
	public void onUpdating() {
		Log.d(TAG, "OnUpdating");
		if (this.menuRefresh != null)
			this.menuRefresh.setEnabled(false);
		if (this.menuCancel != null)
			this.menuCancel.setEnabled(true);
		this.doRefreshAnimation(true);
	}

	@Override
	public void onUpdated() {
		Log.d(TAG, "OnUpdated");
		if (this.menuRefresh != null)
			this.menuRefresh.setEnabled(true);
		if (this.menuCancel != null)
			this.menuCancel.setEnabled(false);
		this.doRefreshAnimation(false);
	}

	@Override
	public void onRefresh() {
		Log.d(TAG, "OnRefresh");
	}

	// ... ... /HANDLER

	// ... DO

	private void doRefresh() {
		if (this.handler != null) {
			this.handler.doReset();
			this.handler.doRefresh();
		}
	}

	private void doCancel() {
		if (this.handler != null) {
			Toast.makeText(getApplicationContext(), "Canceling", Toast.LENGTH_SHORT).show();
			this.handler.doReset();
		}
	}

	private void doRefreshAnimation(boolean active) {
		Utils.rotateMenuItem(getApplicationContext(), this.menuRefresh, active);
	}

	private void doSettings() {
		Intent settingsIntent = new Intent(this, PreferenceActivity.class);
		startActivity(settingsIntent);
	}

	public void doSwitchContent(Fragment fragment) {
		this.content = fragment;
		getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
		getSlidingMenu().showContent();
	}

	// ... /DO

	// INTERFACE

	public interface HomeHandler {
		public void onHome();
	}

	// /INTERFACE

}
