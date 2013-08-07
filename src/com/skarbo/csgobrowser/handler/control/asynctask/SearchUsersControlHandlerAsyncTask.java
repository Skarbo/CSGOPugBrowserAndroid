package com.skarbo.csgobrowser.handler.control.asynctask;

import java.util.regex.Matcher;

import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.container.SearchUsersContainer;
import com.skarbo.csgobrowser.container.SearchUsersContainer.SearchUser;
import com.skarbo.csgobrowser.exception.InvalidPageException;
import com.skarbo.csgobrowser.handler.ControlHandler;
import com.skarbo.csgobrowser.handler.control.ControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.ControlHandlerResult;
import com.skarbo.csgobrowser.utils.RestClient;
import com.skarbo.csgobrowser.utils.RestClient.Response;

public class SearchUsersControlHandlerAsyncTask extends ControlHandlerAsyncTask<SearchUsersContainer> {

	public static final String TAG = SearchUsersControlHandlerAsyncTask.class.getSimpleName();

	private String serviceId;
	private String search;
	private ServiceConfig serviceConfig;

	public SearchUsersControlHandlerAsyncTask(String serviceId, String search, ControlHandler controlHandler,
			ControlHandlerResult<SearchUsersContainer> handlerResult) {
		super(controlHandler, handlerResult);

		this.search = search;
		this.serviceId = serviceId;
		for (ServiceConfig serviceConfig : getServiceConfigs()) {
			if (serviceConfig.id.equalsIgnoreCase(this.serviceId))
				this.serviceConfig = serviceConfig;
		}
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

	public boolean isPageSearchUsers(Response response) {
		return isPage(serviceConfig.pages.searchUsers.getPage().isPage, response);
	}

	// ... /IS

	// ... DO

	@Override
	public SearchUsersContainer doHandle(Void[] params) throws Exception {
		SearchUsersContainer searchUsersContainer = new SearchUsersContainer();
		searchUsersContainer.search = this.search;

		RestClient searchUsersRestClient = createRestClient(this.serviceConfig, createSearchUsersUrl());
		Response searchUsersResponse = searchUsersRestClient.execute(RestClient.RequestMethod.GET);

		if (!isPageSearchUsers(searchUsersResponse))
			throw new InvalidPageException();

		doParseSearchUsers(searchUsersResponse, searchUsersContainer);

		return searchUsersContainer;
	}

	public void doParseSearchUsers(Response searchUsersResponse, SearchUsersContainer searchUsersContainer) {
		Matcher searchUsersMatcher = serviceConfig.pages.searchUsers.getRegexSearchResult().matcher(
				searchUsersResponse.getResponse());

		while (searchUsersMatcher.find()) {
			SearchUser searchUser = serviceConfig.pages.searchUsers.createSearchUser(searchUsersMatcher);
			if (searchUser != null)
				searchUsersContainer.searchUsers.add(searchUser);
		}
	}

	// ... /DO

	// ... CREATE

	public String createSearchUsersUrl() {
		return String.format(serviceConfig.pages.searchUsers.getPage().page, this.search);
	}

	// ... /CREATE

	// /FUNCTIONS

}
