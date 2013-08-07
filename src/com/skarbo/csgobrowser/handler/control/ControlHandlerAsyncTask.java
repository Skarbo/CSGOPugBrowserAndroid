package com.skarbo.csgobrowser.handler.control;

import java.util.List;
import java.util.regex.Pattern;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.config.ServiceConfig.Cookie;
import com.skarbo.csgobrowser.config.ServiceConfig.Header;
import com.skarbo.csgobrowser.container.Container;
import com.skarbo.csgobrowser.exception.InvalidPageException;
import com.skarbo.csgobrowser.handler.ControlHandler;
import com.skarbo.csgobrowser.utils.RestClient;
import com.skarbo.csgobrowser.utils.RestClient.Response;

public abstract class ControlHandlerAsyncTask<T extends Container> extends
		AsyncTask<Void, T, ControlHandlerAsyncTaskResult<T>> {

	private static final String TAG = ControlHandlerAsyncTask.class.getSimpleName();

	private ControlHandler controlHandler;
	private ControlHandlerResult<T> handlerResult;

	public ControlHandlerAsyncTask(ControlHandler controlHandler, ControlHandlerResult<T> handlerResult) {
		this.controlHandler = controlHandler;
		this.handlerResult = handlerResult;
	}

	// FUNCTIONS

	// ... GET

	public abstract String getKey();

	public abstract int getOrder();

	public ControlHandler getControlHandler() {
		return controlHandler;
	}

	public ControlHandlerResult<T> getHandlerResult() {
		return handlerResult;
	}

	public List<ServiceConfig> getServiceConfigs() {
		return getControlHandler().getServiceConfigs();
	}

	// ... /GET

	// ... IS

	/**
	 * @param response
	 * @param regex
	 * @return True if the correct page
	 */
	public boolean isPage(String response, Pattern regex) {
		return regex.matcher(response).find();
	}

	// ... /IS

	// ... ON

	@Override
	protected void onProgressUpdate(T... values) {
		if (values.length > 0)
			this.handlerResult.handleProgress(values[0]);
	}

	@Override
	protected void onPostExecute(ControlHandlerAsyncTaskResult<T> result) {
		if (result == null)
			return;
		if (result.isError()) {
			if (!this.handlerResult.handleError(result.getError())) {
				if (doHandleError(result.getError()))
					this.controlHandler.handledQueue();
			} else
				this.controlHandler.handledQueue();
		} else {
			if (this.handlerResult.handleResult(result.getResult()))
				this.controlHandler.handledQueue();
			try {
				this.controlHandler.getHandler().doContainersCacheSave();
			} catch (Exception e) {
				Toast.makeText(this.controlHandler.getContext(), "Could not save cache", Toast.LENGTH_SHORT).show();
				Log.e(TAG, "onPostExecute: doContainersCacheSave: " + e.getMessage());
			}
		}
	}

	// ... /ON

	// ... DO

	public abstract T doHandle(Void[] params) throws Exception;

	@Override
	protected ControlHandlerAsyncTaskResult<T> doInBackground(Void... params) {
		try {
			return new ControlHandlerAsyncTaskResult<T>(doHandle(params));
		} catch (Exception e) {
			return new ControlHandlerAsyncTaskResult<T>(e);
		}
	}

	protected boolean doHandleError(Exception exception) {
		Log.e(TAG, "Handle Async error: " + exception.getMessage(), exception);
		if (exception instanceof InvalidPageException) {
			Toast.makeText(getControlHandler().getContext(), "Invalid page", Toast.LENGTH_SHORT).show();
			return true;
		} else {
			return false;
		}
	}

	// ... /DO

	public RestClient createRestClient(ServiceConfig serviceConfig, String query) {
		String url = serviceConfig.url;

		if (url.contains("%s")) {
			url = String.format(url, query);
		}

		RestClient restClient = new RestClient(url);

		if (serviceConfig.cookies != null) {
			for (Cookie cookie : serviceConfig.cookies) {
				restClient.addCookie(cookie.name, cookie.value, cookie.domain, cookie.path);
			}
		}

		if (serviceConfig.headers != null) {
			for (Header header : serviceConfig.headers) {
				restClient.addHeader(header.name, header.value);
			}
		}

		return restClient;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ControlHandlerAsyncTask)
			return ((ControlHandlerAsyncTask<?>) o).getKey() == getKey();
		return super.equals(o);
	}

	protected boolean isPage(String isPageRegex, Response response) {
		if (isPageRegex == null || isPageRegex.equalsIgnoreCase(""))
			return false;
		if (response.getResponseCode() != 200)
			return false;
		Pattern patternIsPage = Pattern.compile(isPageRegex, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
		return patternIsPage.matcher(response.getResponse()).find();
	}

}
