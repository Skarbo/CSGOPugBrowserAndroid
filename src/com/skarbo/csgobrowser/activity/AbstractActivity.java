package com.skarbo.csgobrowser.activity;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.handler.Handler;
import com.skarbo.csgobrowser.listener.HandlerListener;
import com.skarbo.csgobrowser.utils.Utils;

public abstract class AbstractActivity extends ActionBarActivity implements HandlerListener {

    public static final int MENU_REFRESH = 1;
    public static final int MENU_CANCEL = 2;
    public static final int ORDER_MENU_REFRESH = 100;
    public static final int ORDER_MENU_CANCEL = 200;
    private static final String TAG = AbstractActivity.class.getSimpleName();
    protected MenuItem menuRefresh;
    protected MenuItem menuCancel;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ACTIONBAR

        getSupportActionBar().setTitle(getActivityTitle());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

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
    }

    // ... GET

    protected abstract String getActivityTitle();

    // ... /GET

    // ... HAS

    protected boolean hasRefreshActionbarItem() {
        return false;
    }

    protected boolean hasCancelActionbarItem() {
        return false;
    }

    // ... /HAS

    // ... ON

    @Override
    protected void onResume() {
        super.onResume();

        if (this.handler != null) {
            this.handler.addListener(TAG, this);
            try {
                this.handler.doContainersCacheLoad();
            } catch (Exception e) {
                Log.e(TAG, "onActivityResult: doContainersCacheLoad: " + e.getMessage());
            }
        }

        doRefresh();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menuRefresh = menu.add(0, MENU_REFRESH, ORDER_MENU_REFRESH, "Refresh");
        this.menuRefresh.setIcon(R.drawable.navigation_refresh);
        if (hasRefreshActionbarItem())
            MenuItemCompat.setShowAsAction(menuRefresh, MenuItem.SHOW_AS_ACTION_IF_ROOM);

        this.menuCancel = menu.add(0, MENU_CANCEL, ORDER_MENU_CANCEL, "Cancel");
        if (hasCancelActionbarItem())
            MenuItemCompat.setShowAsAction(menuCancel, MenuItem.SHOW_AS_ACTION_NEVER);
        this.menuCancel.setEnabled(false);

        if (this.handler.isUpdating()) {
            this.menuRefresh.setEnabled(false);
            this.menuCancel.setEnabled(true);
            this.doRefreshAnimation(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_REFRESH:
                doRefresh();
                return true;
            case MENU_CANCEL:
                doCancel();
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ... ... HANDLER

    @Override
    public void onUpdating() {
        if (this.menuRefresh != null)
            this.menuRefresh.setEnabled(false);
        if (this.menuCancel != null)
            this.menuCancel.setEnabled(true);
        this.doRefreshAnimation(true);
    }

    @Override
    public void onUpdated() {
        if (this.menuRefresh != null)
            this.menuRefresh.setEnabled(true);
        if (this.menuCancel != null)
            this.menuCancel.setEnabled(false);
        this.doRefreshAnimation(false);
    }

    @Override
    public void onRefresh() {

    }

    // ... ... /HANDLER

    // ... /ON

    // ... GET

    public Handler getHandler() {
        return handler;
    }


    // ... /GET

    // ... DO

    protected void doRefresh() {
        if (this.handler != null) {
            this.handler.doReset();
            this.handler.doRefresh();
        }
    }

    protected void doRefreshAnimation(boolean active) {
        if (this.menuRefresh != null)
            Utils.rotateMenuItem(this, this.menuRefresh, active);
    }

    protected void doCancel() {
        if (this.handler != null) {
            Toast.makeText(this, "Canceling", Toast.LENGTH_SHORT).show();
            this.handler.doReset();
        }
    }

    // ... /DO

}
