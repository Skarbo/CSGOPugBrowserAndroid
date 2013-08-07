package com.skarbo.csgobrowser.handler.control.asynctask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.container.ServersContainer;
import com.skarbo.csgobrowser.container.ServersContainer.Server;
import com.skarbo.csgobrowser.handler.ControlHandler;
import com.skarbo.csgobrowser.handler.control.ControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.ControlHandlerResult;
import com.skarbo.csgobrowser.utils.RestClient;
import com.skarbo.csgobrowser.utils.RestClient.Response;

public class ServersControlHandlerAsyncTask extends ControlHandlerAsyncTask<ServersContainer> {

	public static final String TAG = ServersControlHandlerAsyncTask.class.getSimpleName();

	public ServersControlHandlerAsyncTask(ControlHandler controlHandler,
			ControlHandlerResult<ServersContainer> handlerResult) {
		super(controlHandler, handlerResult);
	}

	// ... GET

	@Override
	public String getKey() {
		return TAG;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	// ... /GET

	// ... IS

	public boolean isPageServers(ServiceConfig serviceConfig, Response serversResponse) {
		if (serviceConfig.pages.servers.page.isPage == null || serviceConfig.pages.servers.page.isPage == "")
			return false;
		if (serversResponse.getResponseCode() != 200)
			return false;
		Pattern patternIsPage = Pattern.compile(serviceConfig.pages.servers.page.isPage);
		return patternIsPage.matcher(serversResponse.getResponse()).find();
	}

	// ... /IS

	// ... ON

	// ... /ON

	// ... DO

	@Override
	public ServersContainer doHandle(Void[] params) throws Exception {
		ServersContainer serversContainer = new ServersContainer();
		int count = 0;
		for (ServiceConfig serviceConfig : getServiceConfigs()) {
			Response serversResponse = doRetrieveServersResponse(serviceConfig);

			if (isPageServers(serviceConfig, serversResponse)) {

				ServersContainer serversContainerTemp = new ServersContainer();
				serversContainerTemp.servers = doParseServers(serviceConfig, serversResponse.getResponse());

				if (serversContainerTemp != null)
					serversContainer.merge(serversContainerTemp);
			}
			if (++count < getServiceConfigs().size())
				publishProgress(serversContainer);
		}
		return serversContainer;
	}

	public RestClient.Response doRetrieveServersResponse(ServiceConfig serviceConfig) throws Exception {
		RestClient serversRestClient = createRestClient(serviceConfig, serviceConfig.pages.servers.page.page);
		Response serversResponse = null;

		if (serviceConfig.pages.servers.page.post != null) {
			for (Entry<String, String> entry : serviceConfig.pages.servers.page.post.entrySet()) {
				serversRestClient.addData(entry.getKey(), entry.getValue());
			}
			serversResponse = serversRestClient.execute(RestClient.RequestMethod.POST);
		} else {
			serversResponse = serversRestClient.execute(RestClient.RequestMethod.GET);
		}

		return serversResponse;
	}

	public List<ServersContainer.Server> doParseServers(ServiceConfig serviceConfig, String response) {
		if (serviceConfig.pages.servers.regexServer == null || serviceConfig.pages.servers.regexServer == "")
			return null;
		List<ServersContainer.Server> servers = new ArrayList<ServersContainer.Server>();

		Pattern patternServer = Pattern.compile(serviceConfig.pages.servers.regexServer, Pattern.DOTALL
				| Pattern.MULTILINE);
		Matcher patternMatcher = patternServer.matcher(response);

		while (patternMatcher.find()) {
			// if (patternMatcher.find()) {
			Server server = serviceConfig.pages.servers.container.createServer(patternMatcher);
			if (server != null)
				servers.add(server);
		}

		return servers;
	}

	// ... /DO

	// /FUNCTIONS

}
