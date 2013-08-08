package com.skarbo.csgobrowser.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.fragment.ProfileFragment;
import com.skarbo.csgobrowser.handler.Handler;
import com.skarbo.csgobrowser.listener.HandlerListener;
import com.skarbo.csgobrowser.utils.Utils;

public class ProfileActivity extends AbstractActivity implements HandlerListener {

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


    // ... /ON

    @Override
    protected String getActivityTitle() {
        return getResources().getString(R.string.profile);
    }

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
}
