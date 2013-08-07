package com.skarbo.csgobrowser.activity;

import java.util.ArrayList;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.container.SearchUsersContainer;
import com.skarbo.csgobrowser.container.SearchUsersContainer.SearchUser;
import com.skarbo.csgobrowser.handler.Handler;
import com.skarbo.csgobrowser.listener.SearchUserListener;
import com.skarbo.csgobrowser.utils.Utils;

public class PreferenceActivity extends SherlockFragmentActivity implements SearchUserListener {

	public static final String TAG = PreferenceActivity.class.getSimpleName();

	private Map<String, String> serviceNames;
	private Handler handler;
	private ServiceUserSearchDialogFragment serviceUserSearchDialog;

	private LinearLayout preferenceServiceContainerLayout;
	private LayoutInflater inflater;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		this.serviceNames = Utils.parseStringMap(getResources().getStringArray(R.array.services));

		// HANDLER

		this.handler = new Handler(getApplicationContext());

		// /HANDLER

		// VIEW

		setContentView(R.layout.activity_preference);

		this.inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		this.preferenceServiceContainerLayout = (LinearLayout) findViewById(R.id.preferenceServiceContainerLayout);

		this.serviceUserSearchDialog = new ServiceUserSearchDialogFragment();

		// /VIEW
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (this.handler != null)
			this.handler.addListener(TAG, this);
		doUpdateView();
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (this.handler != null)
			this.handler.removeListener(TAG);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// ... ... HANDLER

	@Override
	public void onUpdating() {
	}

	@Override
	public void onUpdated() {
	}

	@Override
	public void onRefresh() {

	}

	@Override
	public void onSearchUsers(String search) {
		Log.d(TAG, "onSearchUsers: " + search);
		this.serviceUserSearchDialog.onSearching(true);
	}

	@Override
	public void onSearchUsersResult(SearchUsersContainer searchUsersContainer) {
		Log.d(TAG,
				"onSearchUsersResult: " + searchUsersContainer.search + ", " + searchUsersContainer.searchUsers.size());
		this.serviceUserSearchDialog.onSearching(false);
		this.serviceUserSearchDialog.doUpdateSearchResult(searchUsersContainer);
	}

	// ... ... /HANDLER

	// ... /ON

	// ... GET

	@Override
	public Handler getHandler() {
		return this.handler;
	}

	// ... /GET

	// ... CREATE

	private View createServicePreference(final String serviceId) {
		boolean serviceIsEnabled = this.getHandler().getPreferenceHandler().isPrefServiceEnabled(serviceId);
		String userid = this.getHandler().getPreferenceHandler().getPrefServiceUserid(serviceId);
		String username = this.getHandler().getPreferenceHandler().getPrefServiceUsername(serviceId);

		LinearLayout view = (LinearLayout) inflater.inflate(R.layout.layout_preference_service, null, false);
		((TextView) view.findViewById(R.id.preferenceServiceNameTextView)).setText(this.serviceNames.get(serviceId));

		TextView preferenceServiceUsernameTextView = (TextView) view
				.findViewById(R.id.preferenceServiceUsernameTextView);
		if (Utils.isEmpty(userid) && Utils.isEmpty(username))
			preferenceServiceUsernameTextView.setText(R.string.service_user_name_none);
		else
			preferenceServiceUsernameTextView.setText(String.format("%s (%s)", username, userid));

		// USERNAME SEARCH

		final ImageButton preferenceServiceUsernameSearchImageButton = (ImageButton) view
				.findViewById(R.id.preferenceServiceUsernameSearchImageButton);
		preferenceServiceUsernameSearchImageButton.setEnabled(serviceIsEnabled);
		preferenceServiceUsernameSearchImageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doServiceSearchUser(serviceId);
			}
		});

		// /USERNAME SEARCH

		// TOGGLE LAYOUT

		LinearLayout preferenceServiceToggleLayout = (LinearLayout) view
				.findViewById(R.id.preferenceServiceToggleLayout);

		CompoundButton serviceToggleButton = createServiceToggleButton();
		serviceToggleButton.setGravity(Gravity.CENTER);
		serviceToggleButton.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
				TableLayout.LayoutParams.MATCH_PARENT));
		serviceToggleButton.setChecked(serviceIsEnabled);
		serviceToggleButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				getHandler().getPreferenceHandler().doPrefServiceEnabled(serviceId, isChecked);
				preferenceServiceUsernameSearchImageButton.setEnabled(isChecked);
			}

		});

		preferenceServiceToggleLayout.removeAllViews();
		preferenceServiceToggleLayout.addView(serviceToggleButton);

		// /TOGGLE LAYOUT

		return view;
	}

	private CompoundButton createServiceToggleButton() {
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return createServiceToggleButtonSwitch();
		} else {
			return new ToggleButton(getApplicationContext());
		}
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public Switch createServiceToggleButtonSwitch() {
		Switch switchButton = new Switch(getApplicationContext());
		return switchButton;
	}

	// ... /CREATE

	// ... DO

	private void doUpdateView() {
		// SERVICES

		this.preferenceServiceContainerLayout.removeAllViews();

		for (String serviceId : ServiceConfig.SERVICE_IDS) {
			this.preferenceServiceContainerLayout.addView(createServicePreference(serviceId));
		}

		// /SERVICES
	}

	protected void doServiceSearchUser(String serviceId) {
		doShowUserSearchDialog(serviceId);
	}

	protected void doServiceUser(String serviceId, SearchUser searchUser) {
		if (!Utils.isEmpty(serviceId) && searchUser != null) {
			getHandler().getPreferenceHandler().doPrefServiceUserid(serviceId, searchUser.userid);
			getHandler().getPreferenceHandler().doPrefServiceUsername(serviceId, searchUser.username);
		} else if (!Utils.isEmpty(serviceId) && searchUser == null) {
			getHandler().getPreferenceHandler().doPrefServiceUserid(serviceId, null);
			getHandler().getPreferenceHandler().doPrefServiceUsername(serviceId, null);
		} else
			Log.w(TAG, "doServiceUser: Service id or search user is not given");
		doUpdateView();
	}

	public void doShowUserSearchDialog(String serviceId) {
		FragmentManager fragmentManager = getSupportFragmentManager();

		this.serviceUserSearchDialog.preferenceActivity = this;
		this.serviceUserSearchDialog.serviceId = serviceId;
		this.serviceUserSearchDialog.serviceName = this.serviceNames.get(serviceId);

		serviceUserSearchDialog.show(fragmentManager, "dialog");
		// if (true) {
		// // The device is using a large layout, so show the fragment as a
		// // dialog
		// serviceUserSearchDialog.show(fragmentManager, "dialog");
		// } else {
		// // The device is smaller, so show the fragment fullscreen
		// FragmentTransaction transaction = fragmentManager.beginTransaction();
		// // For a little polish, specify a transition animation
		// transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		// // To make it fullscreen, use the 'content' root view as the
		// // container
		// // for the fragment, which is always the root view for the activity
		// transaction.add(android.R.id.content,
		// serviceUserSearchDialog).addToBackStack(null).commit();
		// }
	}

	// ... /DO

	// CLASS

	public static class ServiceUserSearchDialogFragment extends DialogFragment {

		public PreferenceActivity preferenceActivity;
		public String serviceId;
		public String serviceName;
		private TextView dialogServiceResultsNoneTextView;
		private ListView dialogServiceResultsListView;
		private SearchUserArrayAdapter searchUserArrayAdapter;
		private ImageButton dialogServiceSearchImageButton;
		private EditText dialogServiceSearchEditText;
		private ProgressBar dialogServiceSearchProgressBar;
		private ImageButton dialogServiceRemoveImageButton;

		@Override
		public void onActivityCreated(Bundle arg0) {
			super.onActivityCreated(arg0);

			this.searchUserArrayAdapter = new SearchUserArrayAdapter(getActivity());
			this.dialogServiceResultsListView.setAdapter(this.searchUserArrayAdapter);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.dialog_service_user, container, false);

			((TextView) view.findViewById(R.id.dialogServiceNameTextView)).setText(String.format(getResources()
					.getString(R.string.service_user_search), this.serviceName));

			this.dialogServiceRemoveImageButton = (ImageButton) view.findViewById(R.id.dialogServiceRemoveImageButton);
			this.dialogServiceResultsNoneTextView = (TextView) view.findViewById(R.id.dialogServiceResultsNoneTextView);
			this.dialogServiceResultsListView = (ListView) view.findViewById(R.id.dialogServiceResultsListView);
			this.dialogServiceSearchEditText = (EditText) view.findViewById(R.id.dialogServiceSearchEditText);
			this.dialogServiceSearchProgressBar = (ProgressBar) view.findViewById(R.id.dialogServiceSearchProgressBar);
			this.dialogServiceSearchImageButton = (ImageButton) view.findViewById(R.id.dialogServiceSearchImageButton);

			this.dialogServiceRemoveImageButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (preferenceActivity != null) {
						preferenceActivity.doServiceUser(serviceId, null);
						dismiss();
					}
				}
			});

			this.dialogServiceSearchImageButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					doSearch();
				}
			});

			this.dialogServiceResultsListView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					SearchUser searchUser = searchUserArrayAdapter.getItem(arg2);
					if (searchUser != null && preferenceActivity != null) {
						preferenceActivity.doServiceUser(serviceId, searchUser);
						dismiss();
					}
				}
			});

			this.dialogServiceSearchEditText.setText("");

			this.dialogServiceResultsNoneTextView.setVisibility(View.VISIBLE);
			this.dialogServiceResultsListView.setVisibility(View.GONE);
			this.dialogServiceSearchProgressBar.setVisibility(View.GONE);

			return view;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			Dialog dialog = super.onCreateDialog(savedInstanceState);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			return dialog;
		}

		protected void doSearch() {
			if (this.dialogServiceSearchEditText != null && this.preferenceActivity != null) {
				String search = this.dialogServiceSearchEditText.getText().toString();
				this.preferenceActivity.getHandler().getControlHandler().doSearchUsers(this.serviceId, search);
			}
		}

		public void onSearching(boolean searching) {
			if (this.dialogServiceSearchImageButton != null)
				this.dialogServiceSearchImageButton.setEnabled(!searching);
			if (this.dialogServiceSearchProgressBar != null)
				this.dialogServiceSearchProgressBar.setVisibility(searching ? View.VISIBLE : View.GONE);
		}

		public void doUpdateSearchResult(SearchUsersContainer searchUsersContainer) {
			if (this.searchUserArrayAdapter != null) {
				this.searchUserArrayAdapter.clear();

				for (SearchUsersContainer.SearchUser searchUser : searchUsersContainer.searchUsers) {
					this.searchUserArrayAdapter.add(searchUser);
				}

				this.searchUserArrayAdapter.notifyDataSetChanged();

				if (this.searchUserArrayAdapter.isEmpty()) {
					this.dialogServiceResultsNoneTextView.setVisibility(View.VISIBLE);
					this.dialogServiceResultsListView.setVisibility(View.GONE);
				} else {
					this.dialogServiceResultsNoneTextView.setVisibility(View.GONE);
					this.dialogServiceResultsListView.setVisibility(View.VISIBLE);
				}
			}
		}

		public static class SearchUserArrayAdapter extends ArrayAdapter<SearchUsersContainer.SearchUser> {

			public static final int LIST_ITEM_DIALOG_SERVICE_USER = R.layout.list_item_dialog_service_user;
			private LayoutInflater layoutInflater;

			public SearchUserArrayAdapter(Context context) {
				super(context, LIST_ITEM_DIALOG_SERVICE_USER, new ArrayList<SearchUsersContainer.SearchUser>());
				this.layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				final ViewHolder viewHolder;

				if (convertView == null) {
					convertView = layoutInflater.inflate(LIST_ITEM_DIALOG_SERVICE_USER, null);

					viewHolder = new ViewHolder();
					viewHolder.usernameTextView = (TextView) convertView
							.findViewById(R.id.dialogServiceListItemUsernameTextView);
					viewHolder.nameTextView = (TextView) convertView
							.findViewById(R.id.dialogServiceListItemUserNameTextView);
					viewHolder.useridTextView = (TextView) convertView
							.findViewById(R.id.dialogServiceListItemUseridTextView);

					convertView.setTag(viewHolder);
				} else
					viewHolder = (ViewHolder) convertView.getTag();

				SearchUsersContainer.SearchUser searchUserContainer = getItem(position);
				if (searchUserContainer != null) {
					viewHolder.usernameTextView.setText(searchUserContainer.username);
					viewHolder.useridTextView.setText(String.format("(%s)", searchUserContainer.userid));

					if (!Utils.isEmpty(searchUserContainer.name)) {
						viewHolder.nameTextView.setText(searchUserContainer.name);
						viewHolder.nameTextView.setVisibility(View.VISIBLE);
					} else
						viewHolder.nameTextView.setVisibility(View.GONE);
				}

				return convertView;
			}

			private class ViewHolder {

				public TextView nameTextView;
				public TextView useridTextView;
				public TextView usernameTextView;

			}

		}
	}

	// /CLASS

}
