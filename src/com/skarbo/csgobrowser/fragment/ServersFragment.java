package com.skarbo.csgobrowser.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.activity.MainActivity;
import com.skarbo.csgobrowser.activity.ServerActivity;
import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.config.service.LeetwayServiceConfig;
import com.skarbo.csgobrowser.container.ServersContainer;
import com.skarbo.csgobrowser.handler.Handler;
import com.skarbo.csgobrowser.handler.PreferenceHandler;
import com.skarbo.csgobrowser.listener.HandlerListener;
import com.skarbo.csgobrowser.utils.Utils.Country;
import com.skarbo.csgobrowser.view.SlidingUpPanelLayout;
import com.skarbo.csgobrowser.view.SlidingUpPanelLayout.PanelSlideListener;
import com.skarbo.csgobrowser.view.StatusFilterCheckBox;

public class ServersFragment extends Fragment implements HandlerListener, OnItemClickListener {

	private static final String TAG = ServersFragment.class.getSimpleName();

	private Handler handler;

	private ListView serversListView;
	private TextView emptyServersTextView;
	private LinearLayout serversFilterServicesLayout;
	private LinearLayout serversFilterLocationsLayout;

	private ServersAdapter serversAdapter;

	private Map<ServersContainer.Server.Status, StatusFilterCheckBox> serversFilterStatusCheckboxs = new HashMap<ServersContainer.Server.Status, StatusFilterCheckBox>();

	// ... ON

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		this.handler = ((MainActivity) getActivity()).getHandler();
		((MainActivity) getActivity()).getSupportActionBar().setTitle(R.string.servers);

		serversAdapter = new ServersAdapter(getActivity());
		if (serversListView != null)
			serversListView.setAdapter(serversAdapter);

		doUpdateView();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_servers, container, false);

		serversListView = (ListView) view.findViewById(R.id.serversListView);
		emptyServersTextView = (TextView) view.findViewById(R.id.emptyServersTextView);

		serversListView.setOnItemClickListener(this);

		// SLIDING UP PANEL

		TextView serversFilterTextView = (TextView) view.findViewById(R.id.serversFilterTextView);
		SlidingUpPanelLayout serversFilterSlidingPanelLayout = (SlidingUpPanelLayout) view
				.findViewById(R.id.sliding_layout);
		serversFilterSlidingPanelLayout.setShadowDrawable(getResources().getDrawable(R.drawable.above_shadow));
		serversFilterSlidingPanelLayout.setPanelHeightDp(20);
		serversFilterSlidingPanelLayout.setDragView(serversFilterTextView);
		serversFilterSlidingPanelLayout.setPanelSlideListener(new PanelSlideListener() {
			@Override
			public void onPanelSlide(View panel, float slideOffset) {

			}

			@Override
			public void onPanelExpanded(View panel) {

			}

			@Override
			public void onPanelCollapsed(View panel) {

			}
		});
		// LinearLayout t = (LinearLayout)
		// view.findViewById(R.id.serversLayout);
		// t.setMovementMethod(LinkMovementMethod.getInstance());

		// /SLIDING UP PANEL

		// FILTER PANEL

		this.serversFilterServicesLayout = (LinearLayout) view.findViewById(R.id.serversFilterServicesLayout);

		this.serversFilterStatusCheckboxs.put(ServersContainer.Server.Status.Waiting,
				(StatusFilterCheckBox) view.findViewById(R.id.serversFilterStatusWaitingCheckBox));
		this.serversFilterStatusCheckboxs.get(ServersContainer.Server.Status.Waiting).status = ServersContainer.Server.Status.Waiting;
		this.serversFilterStatusCheckboxs.put(ServersContainer.Server.Status.Available,
				(StatusFilterCheckBox) view.findViewById(R.id.serversFilterStatusAvilableCheckBox));
		this.serversFilterStatusCheckboxs.get(ServersContainer.Server.Status.Available).status = ServersContainer.Server.Status.Available;
		this.serversFilterStatusCheckboxs.put(ServersContainer.Server.Status.Live,
				(StatusFilterCheckBox) view.findViewById(R.id.serversFilterStatusLiveCheckBox));
		this.serversFilterStatusCheckboxs.get(ServersContainer.Server.Status.Live).status = ServersContainer.Server.Status.Live;

		this.serversFilterLocationsLayout = (LinearLayout) view.findViewById(R.id.serversFilterLocationsLayout);

		// /FILTER PANEL

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
		if (this.handler != null)
			this.handler.getControlHandler().doServers();
	}

	// ... ... /HANDLER

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		ServersContainer.Server server = this.serversAdapter.getItem(arg2);
		if (server != null) {
			doServerActivity(server);
		}
	}

	// ... /ON

	@Override
	public Handler getHandler() {
		return this.handler;
	}

	// ... DO

	private void doUpdateView() {
		ServersContainer serversContainer = this.handler.getControlHandler().getContainers().serversContainer;
		final PreferenceHandler.ServersFilter serversFilter = this.handler.getPreferenceHandler()
				.getPrefServersFilter();

		// SERVERS

		Collections.sort(serversContainer.servers);

		this.serversAdapter.clear();
		for (ServersContainer.Server server : serversContainer.servers) {
			if (serversFilter.isServerFiltered(server))
				this.serversAdapter.add(server);
		}
		this.serversAdapter.notifyDataSetChanged();

		if (this.serversAdapter.getCount() == 0) {
			emptyServersTextView.setVisibility(View.VISIBLE);
			serversListView.setVisibility(View.GONE);
		} else {
			emptyServersTextView.setVisibility(View.GONE);
			serversListView.setVisibility(View.VISIBLE);
		}

		// /SERVERS

		// FILTER PANEL

		OnCheckedChangeListener serviceFilterListener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				ServiceFilterCheckBox serviceCheckBox = (ServiceFilterCheckBox) buttonView;
				serversFilter.serviceIds.remove(serviceCheckBox.serviceId);
				if (isChecked)
					serversFilter.serviceIds.add(serviceCheckBox.serviceId);
				handler.getPreferenceHandler().doPrefSaveServersFilter();
				doUpdateView();
			}
		};
		OnCheckedChangeListener statusFilterListener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				StatusFilterCheckBox statusFilterCheckBox = (StatusFilterCheckBox) buttonView;
				serversFilter.status.remove(statusFilterCheckBox.status);
				if (isChecked)
					serversFilter.status.add(statusFilterCheckBox.status);
				handler.getPreferenceHandler().doPrefSaveServersFilter();
				doUpdateView();
			}
		};
		OnCheckedChangeListener countryFilterListener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				CountryFilterCheckBox countryFilterCheckBox = (CountryFilterCheckBox) buttonView;
				serversFilter.countries.remove(countryFilterCheckBox.country);
				if (isChecked)
					serversFilter.countries.add(countryFilterCheckBox.country);
				handler.getPreferenceHandler().doPrefSaveServersFilter();
				doUpdateView();
			}
		};

		List<ServiceConfig> serviceConfigs = this.handler.getServiceConfigs();
		this.serversFilterServicesLayout.removeAllViews();
		for (ServiceConfig serviceConfig : serviceConfigs) {
			ServiceFilterCheckBox serviceCheckBox = new ServiceFilterCheckBox(serviceConfig.id, getActivity());
			serviceCheckBox.setText(serviceConfig.id);
			if (serversFilter.serviceIds.contains(serviceConfig.id))
				serviceCheckBox.setChecked(true);
			serviceCheckBox.setOnCheckedChangeListener(serviceFilterListener);
			this.serversFilterServicesLayout.addView(serviceCheckBox);
		}

		for (ServersContainer.Server.Status status : ServersContainer.Server.Status.values()) {
			if (serversFilter.status.contains(status))
				this.serversFilterStatusCheckboxs.get(status).setChecked(true);
			this.serversFilterStatusCheckboxs.get(status).setOnCheckedChangeListener(statusFilterListener);
		}

		this.serversFilterLocationsLayout.removeAllViews();
		for (Country country : Country.values()) {
			CountryFilterCheckBox countryFilterCheckBox = new CountryFilterCheckBox(country, getActivity());
			countryFilterCheckBox.setText(country.toString());
			if (serversFilter.countries.contains(country))
				countryFilterCheckBox.setChecked(true);
			countryFilterCheckBox.setOnCheckedChangeListener(countryFilterListener);
			this.serversFilterLocationsLayout.addView(countryFilterCheckBox);
		}

		// /FILTER PANEL
	}

	private void doServerActivity(ServersContainer.Server server) {
		Intent serverActivityIntent = ServerActivity.createServerActivity(getActivity(), server.serviceId, server.id,
				server.matchId);
		startActivityForResult(serverActivityIntent, 0);
		// getActivity().overridePendingTransition(R.anim.slide_out_left,
		// R.anim.slide_in_right);
	}

	// ... /DO

	// CLASS

	public class ServersAdapter extends ArrayAdapter<ServersContainer.Server> {

		private static final int LIST_ITEM_SERVER = R.layout.list_item_server;
		private LayoutInflater layoutInflater;

		public ServersAdapter(Context context) {
			super(context, LIST_ITEM_SERVER, new ArrayList<ServersContainer.Server>());
			layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder viewHolder;

			if (convertView == null) {
				convertView = layoutInflater.inflate(LIST_ITEM_SERVER, null);

				viewHolder = new ViewHolder();
				viewHolder.serverInfoTextView = (TextView) convertView.findViewById(R.id.serverInfoTextView);
				viewHolder.serverNameTextView = (TextView) convertView.findViewById(R.id.serverName_TextView);
				viewHolder.serverCurrentPlayersTextView = (TextView) convertView
						.findViewById(R.id.serverCurrentPlayersTextView);
				viewHolder.serverMaxPlayersTextView = (TextView) convertView
						.findViewById(R.id.serverMaxPlayersTextView);
				viewHolder.serverStatusAvailableTextView = (TextView) convertView
						.findViewById(R.id.serverStatusAvailableTextView);
				viewHolder.serverStatusWaitingTextView = (TextView) convertView
						.findViewById(R.id.serverStatusWaitingTextView);
				viewHolder.serverStatusLiveTextView = (TextView) convertView
						.findViewById(R.id.serverStatusLiveTextView);
				viewHolder.serverStatusLiveScoreHomeTextView = (TextView) convertView
						.findViewById(R.id.serverStatusLiveScoreHomeTextView);
				viewHolder.serverStatusLiveScoreLinearLayout = (LinearLayout) convertView
						.findViewById(R.id.serverStatusLiveScoreLayout);
				viewHolder.serverStatusLiveScoreAwayTextView = (TextView) convertView
						.findViewById(R.id.serverStatusLiveScoreAwayTextView);
				viewHolder.serverCountryImageView = (ImageView) convertView.findViewById(R.id.serverCountryImageView);
				viewHolder.serverServiceImageView = (ImageView) convertView.findViewById(R.id.serverServiceImageView);

				convertView.setTag(viewHolder);
			} else
				viewHolder = (ViewHolder) convertView.getTag();

			ServersContainer.Server server = getItem(position);
			if (server != null) {
				viewHolder.serverNameTextView.setText(server.name);
				viewHolder.serverCurrentPlayersTextView.setText(String.valueOf(server.playersCurrent));
				viewHolder.serverMaxPlayersTextView.setText(String.valueOf(server.playersMax));

				// Info
				viewHolder.serverInfoTextView.setTypeface(null, Typeface.NORMAL);
				if (server.map != null) {
					viewHolder.serverInfoTextView.setText(server.map);
				} else if (server.ipAddress != null) {
					viewHolder.serverInfoTextView.setText(server.ipAddress);
				} else if (server.status != null) {
					switch (server.status) {
					case Live:
						viewHolder.serverInfoTextView.setText(R.string.server_status_live);
						break;
					case Waiting:
						viewHolder.serverInfoTextView.setText(R.string.server_status_waiting);
						break;
					default:
						viewHolder.serverInfoTextView.setText(R.string.server_status_available);
						break;
					}
					viewHolder.serverInfoTextView.setTypeface(null, Typeface.ITALIC);
				}

				// Service
				if (server.serviceId != null) {
					if (server.serviceId.equalsIgnoreCase(LeetwayServiceConfig.SERVICE_ID))
						viewHolder.serverServiceImageView.setImageResource(R.drawable.logo_leetway);
					else
						viewHolder.serverServiceImageView.setImageResource(R.drawable.logo_esea);
				}

				// Status
				viewHolder.serverStatusAvailableTextView.setVisibility(View.GONE);
				viewHolder.serverStatusWaitingTextView.setVisibility(View.GONE);
				viewHolder.serverStatusLiveTextView.setVisibility(View.GONE);
				viewHolder.serverStatusLiveScoreLinearLayout.setVisibility(View.GONE);
				if (server.status != null) {
					switch (server.status) {
					case Live:
						if (server.scoreAway > 0 && server.scoreHome > 0) {
							viewHolder.serverStatusLiveScoreLinearLayout.setVisibility(View.VISIBLE);
							viewHolder.serverStatusLiveScoreHomeTextView.setText(String.valueOf(server.scoreHome));
							viewHolder.serverStatusLiveScoreAwayTextView.setText(String.valueOf(server.scoreAway));
						} else {
							viewHolder.serverStatusLiveTextView.setVisibility(View.VISIBLE);
						}
						break;
					case Waiting:
						viewHolder.serverStatusWaitingTextView.setVisibility(View.VISIBLE);
						break;
					default:
						viewHolder.serverStatusAvailableTextView.setVisibility(View.VISIBLE);
						break;
					}
				}

				// Country
				if (server.country != null) {
					switch (server.country) {
					case DE:
						viewHolder.serverCountryImageView.setImageResource(R.drawable.flag_de);
						break;
					case ES:
						viewHolder.serverCountryImageView.setImageResource(R.drawable.flag_es);
						break;
					case FR:
						viewHolder.serverCountryImageView.setImageResource(R.drawable.flag_fr);
						break;
					case GB:
						viewHolder.serverCountryImageView.setImageResource(R.drawable.flag_gb);
						break;
					case SE:
						viewHolder.serverCountryImageView.setImageResource(R.drawable.flag_se);
						break;
					default:
						viewHolder.serverCountryImageView.setImageResource(R.drawable.flag_us);
						break;
					}
				}
			}

			return convertView;
		}

		private class ViewHolder {
			public TextView serverStatusLiveTextView;
			public TextView serverStatusWaitingTextView;
			public LinearLayout serverStatusLiveScoreLinearLayout;
			public TextView serverStatusLiveScoreAwayTextView;
			public TextView serverStatusLiveScoreHomeTextView;
			public TextView serverStatusAvailableTextView;
			public ImageView serverServiceImageView;
			public ImageView serverCountryImageView;
			public TextView serverInfoTextView;
			public TextView serverNameTextView;
			public TextView serverCurrentPlayersTextView;
			public TextView serverMaxPlayersTextView;
		}

	}

	private class ServiceFilterCheckBox extends CheckBox {
		public String serviceId;

		public ServiceFilterCheckBox(String serviceId, Context context) {
			super(context);
			this.serviceId = serviceId;
		}
	}

	private class CountryFilterCheckBox extends CheckBox {
		public Country country;

		public CountryFilterCheckBox(Country country, Context context) {
			super(context);
			this.country = country;
		}
	}

	// /CLASS

}
