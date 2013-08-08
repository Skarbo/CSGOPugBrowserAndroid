package com.skarbo.csgobrowser.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.fragment.ServersFragment;

public class MainActivity extends AbstractActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String CONTENT_SAVE = "content";
    private static final int MENU_SETTINGS = 3;
    private static final int ORDER_MENU_SETTINGS = 50;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private Fragment content;
    private MenuItem menuSettings;

    // ... ON

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // CONTENT

        // this.menuFragment = new MenuFragment();
        //
        // if (savedInstanceState != null)
        // this.content =
        // getSupportFragmentManager().getFragment(savedInstanceState,
        // CONTENT_SAVE);

        // /CONTENT

        // VIEW

        // // Set the Above View
        // setContentView(R.layout.frame_content);
        // if (this.content != null)
        // getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,
        // this.content).commit();
        // else
        // getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,
        // new ServersFragment()).commit();
        // //
        // getSupportFragmentManager().beginTransaction().replace(R.id.content_frame,
        // //
        // ServerFragment.createServerFragment(LeetwayServiceConfig.SERVICE_ID,
        // // "15")).commit();
        //
        // // Set the Behind View
        // setBehindContentView(R.layout.frame_menu);
        // getSupportFragmentManager().beginTransaction().replace(R.id.menu_frame,
        // menuFragment).commit();
        //
        // // Customize the SlidingMenu
        // SlidingMenu sm = getSlidingMenu();
        // sm.setShadowWidthRes(R.dimen.shadow_width);
        // sm.setShadowDrawable(R.drawable.shadow);
        // sm.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        // sm.setFadeDegree(0.35f);
        // sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
        //
        // // Set home as up
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_main);

        this.drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        this.drawerList = (ListView) findViewById(R.id.left_drawer);

        this.drawerToggle = new ActionBarDrawerToggle(this, this.drawerLayout, R.drawable.ic_drawer,
                R.string.drawer_open, R.string.drawer_close);
        this.drawerLayout.setDrawerListener(this.drawerToggle);

        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new ServersFragment()).commit();

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
        if (getHandler() != null)
            getHandler().doContainersCacheDelete();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menuSettings = menu.add(0, MENU_SETTINGS, 0, "Settings");
        menuSettings.setIcon(R.drawable.action_settings);
        MenuItemCompat.setShowAsAction(menuSettings, MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (this.drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case MENU_SETTINGS:
                doSettings();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ... /ON

    // ... GET

    @Override
    protected String getActivityTitle() {
        return getResources().getString(R.string.app_name);
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

    private void doSettings() {
        Intent settingsIntent = new Intent(this, PreferenceActivity.class);
        startActivity(settingsIntent);
    }

    public void doSwitchContent(Fragment fragment) {
        this.content = fragment;
        getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
        // getSlidingMenu().showContent();
    }

    // ... /DO

}
