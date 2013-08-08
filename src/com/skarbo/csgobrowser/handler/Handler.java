package com.skarbo.csgobrowser.handler;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.google.gson.Gson;
import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.container.MatchesContainer;
import com.skarbo.csgobrowser.container.PlayersContainer;
import com.skarbo.csgobrowser.container.ProfilesContainer;
import com.skarbo.csgobrowser.container.ServersContainer;
import com.skarbo.csgobrowser.listener.HandlerListener;
import com.skarbo.csgobrowser.utils.Utils;

public class Handler {

	private static final String CACHE_CONTAINERS = "containers.json";
	private static final String TAG = Handler.class.getSimpleName();

	private Context context;
	private ControlHandler controlHandler;
	private PreferenceHandler preferenceHandler;
	private List<ServiceConfig> serviceConfigs;
	private HashMap<String, HandlerListener> listeners;
	private Handler.Containers containers;
	private LruCache<String, Bitmap> bitmapCache;

	public Handler(Context context) {
		this.context = context;
		this.controlHandler = new ControlHandler(this);
		this.serviceConfigs = new ArrayList<ServiceConfig>();
		this.preferenceHandler = new PreferenceHandler(context);
		this.listeners = new HashMap<String, HandlerListener>();
		this.containers = new Handler.Containers();

		this.serviceConfigs = this.preferenceHandler.createServiceConfigsEnabled();

		// BITMAP CACHE

		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		final int cacheSize = maxMemory / 8;

		this.bitmapCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return (int) (Utils.getBitmapSizeInBytes(bitmap) / 1024);
			}
		};

		// /BITMAP CACHE
	}

	// FUNCTIONS

	// ... GET

	public ControlHandler getControlHandler() {
		return controlHandler;
	}

	public Handler.Containers getContainers() {
		return this.containers;
	}

	public PreferenceHandler getPreferenceHandler() {
		return preferenceHandler;
	}

	public List<ServiceConfig> getServiceConfigs() {
		return serviceConfigs;
	}

	public Context getContext() {
		return this.context;
	}

	// ... /GET

	// ... ADD/GET

	public void addBitmapToCache(String key, Bitmap bitmap) {
		if (getBitmapFromCache(key) == null) {
			Log.d(TAG, "addBitmapToCache: " + key);
			this.bitmapCache.put(key, bitmap);
		}
	}

	public Bitmap getBitmapFromCache(String key) {
		return this.bitmapCache.get(key);
	}

	// ... /ADD/GET

	// ... IS

	public boolean isUpdating() {
		return this.controlHandler.isUpdating();
	}

	// ... /IS

	// ... DO

	public void doRefresh() {
		List<HandlerListener> listenerList = Collections.list(Collections.enumeration(this.listeners.values()));
		for (HandlerListener listener : listenerList) {
			listener.onRefresh();
		}
	}

	public void doReset() {
		this.serviceConfigs = this.preferenceHandler.createServiceConfigsEnabled();
		// this.containers = new Containers();
		this.controlHandler.doReset();
	}

	@SuppressWarnings("unchecked")
	public <E extends HandlerListener> void doNotifyListeners(Class<E> notifyClass, NotifyListener<E> notifyListener) {
		List<HandlerListener> listenerList = Collections.list(Collections.enumeration(this.listeners.values()));
		for (HandlerListener listener : listenerList) {
			if (notifyClass.isAssignableFrom(listener.getClass())) {
				notifyListener.doNotify((E) listener);
			}
		}
	}

	public void doContainersCacheSave() throws Exception {
		FileOutputStream outputStream;
		Gson gson = new Gson();
		File file = new File(context.getFilesDir(), CACHE_CONTAINERS);

		String containersJson = gson.toJson(getContainers());
		outputStream = new FileOutputStream(file);
		outputStream.write(containersJson.getBytes());
		outputStream.close();
	}

	public void doContainersCacheLoad() throws Exception {
		File file = new File(context.getFilesDir(), CACHE_CONTAINERS);
		Gson gson = new Gson();

		if (file.exists()) {
			String containersJson = Utils.retrieveContent(file);
			this.containers = gson.fromJson(containersJson, Handler.Containers.class);
		} else
			Log.w(TAG, "doContainersCacheLoad: Cache file dose not exist");
	}

	public void doContainersCacheDelete() {
		Log.d(TAG, "doContainersCacheDelete");
		File file = new File(context.getFilesDir(), CACHE_CONTAINERS);
		file.delete();
	}

	// ... /DO

	// ... ADD/REMOVE

	public void addListener(String key, HandlerListener listener) {
		listeners.put(key, listener);
	}

	public void removeListener(String key) {
		listeners.remove(key);
	}

	// ... /ADD/REMOVE

	// /FUNCTIONS

	// INTERFACE

	public static interface NotifyListener<E extends HandlerListener> {
		public void doNotify(E listener);
	}

	// /INTERFACE

	// CLASS

	public static class Containers {

		public ServersContainer serversContainer = new ServersContainer();
		public MatchesContainer matchesContainer = new MatchesContainer();
		public ProfilesContainer profilesContainer = new ProfilesContainer();

	}

	// /CLASS

}
