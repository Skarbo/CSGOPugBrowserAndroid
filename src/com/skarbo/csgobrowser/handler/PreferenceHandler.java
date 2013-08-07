package com.skarbo.csgobrowser.handler;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.container.ServersContainer;
import com.skarbo.csgobrowser.container.ServersContainer.Server.Status;
import com.skarbo.csgobrowser.utils.Utils.Country;

public class PreferenceHandler {

	private static final String TAG = PreferenceHandler.class.getSimpleName();
	private static final String PREF_KEY_SERVICE_ENABLED = "service_enabled_%s";
	private static final String PREF_KEY_SERVICE_USERID = "service_userid_%s";
	private static final String PREF_KEY_SERVICE_USERNAME = "service_username_%s";
	private static final String PREF_KEY_SERVERS_FILTER = "servers_filter";

	private Context context;
	private SharedPreferences sharedPreferences;
	private ServersFilter serversFilter;

	public PreferenceHandler(Context context) {
		this.context = context;
		this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		this.serversFilter = createServersFilter();
	}

	// FUNCTIONS

	// ... GET

	public SharedPreferences getSharedPreferences() {
		return sharedPreferences;
	}

	public static String getPrefKeyServiceEnabled(String serviceId) {
		return String.format(PREF_KEY_SERVICE_ENABLED, serviceId);
	}

	public static String getPrefKeyServiceUserid(String serviceId) {
		return String.format(PREF_KEY_SERVICE_USERID, serviceId);
	}

	public static String getPrefKeyServiceUsername(String serviceId) {
		return String.format(PREF_KEY_SERVICE_USERNAME, serviceId);
	}

	public String getPrefServiceUserid(String serviceId) {
		return this.sharedPreferences.getString(getPrefKeyServiceUserid(serviceId), "");
	}

	public String getPrefServiceUsername(String serviceId) {
		return this.sharedPreferences.getString(getPrefKeyServiceUsername(serviceId), "");
	}

	public PreferenceHandler.ServersFilter getPrefServersFilter() {
		return this.serversFilter;
	}

	// ... /GET

	// ... IS

	public boolean isPrefServiceEnabled(String serviceId) {
		return this.sharedPreferences.getBoolean(getPrefKeyServiceEnabled(serviceId), true);
	}

	// ... /IS

	// ... DO

	public void doPrefSaveServersFilter() {
		String preferenceJson = (new Gson()).toJson(this.serversFilter);
		Editor editor = getSharedPreferences().edit();
		editor.putString(PREF_KEY_SERVERS_FILTER, preferenceJson);
		editor.commit();
	}

	public void doPrefServiceEnabled(String serviceId, boolean enabled) {
		Editor editor = getSharedPreferences().edit();
		editor.putBoolean(PreferenceHandler.getPrefKeyServiceEnabled(serviceId), enabled);
		editor.commit();
	}

	public void doPrefServiceUserid(String serviceId, String userid) {
		Editor editor = getSharedPreferences().edit();
		String key = PreferenceHandler.getPrefKeyServiceUserid(serviceId);
		if (userid != null)
			editor.putString(key, userid);
		else
			editor.remove(key);
		editor.commit();
	}

	public void doPrefServiceUsername(String serviceId, String username) {
		Editor editor = getSharedPreferences().edit();
		String key = PreferenceHandler.getPrefKeyServiceUsername(serviceId);
		if (username != null)
			editor.putString(key, username);
		else
			editor.remove(key);
		editor.commit();
	}

	// ... /DO

	// ... CREATE

	public ServiceConfig createServiceConfig(String serviceId) {
		return ServiceConfig.createServiceConfig(serviceId);
	}

	public List<ServiceConfig> createServiceConfigsEnabled() {
		List<ServiceConfig> serviceConfigs = new ArrayList<ServiceConfig>();
		for (String serviceId : ServiceConfig.SERVICE_IDS) {
			if (isPrefServiceEnabled(serviceId))
				serviceConfigs.add(ServiceConfig.createServiceConfig(serviceId));
		}
		return serviceConfigs;
	}

	private ServersFilter createServersFilter() {
		String preferenceJson = getSharedPreferences().getString(PREF_KEY_SERVERS_FILTER, null);
		if (preferenceJson == null || preferenceJson.equalsIgnoreCase("")) {
			ServersFilter serversFilterTemp = new ServersFilter();

			for (String serviceId : ServiceConfig.SERVICE_IDS)
				serversFilterTemp.serviceIds.add(serviceId);

			for (Status status : ServersContainer.Server.Status.values())
				serversFilterTemp.status.add(status);

			for (Country country : Country.values())
				serversFilterTemp.countries.add(country);

			return serversFilterTemp;
		}
		return (new Gson()).fromJson(preferenceJson, ServersFilter.class);
	}

	// ... /CREATE

	// /FUNCTIONS

	// CLASS

	public class ServersFilter {
		public List<String> serviceIds = new ArrayList<String>();
		public List<ServersContainer.Server.Status> status = new ArrayList<ServersContainer.Server.Status>();
		public List<Country> countries = new ArrayList<Country>();

		/**
		 * @return True if Server matches filter
		 */
		public boolean isServerFiltered(ServersContainer.Server server) {
			if (!this.serviceIds.contains(server.serviceId))
				return false;
			if (!this.status.contains(server.status))
				return false;
			if (!this.countries.contains(server.country))
				return false;
			return true;
		}
	}

	// /CLASS

}
