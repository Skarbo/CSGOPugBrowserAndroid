package com.skarbo.csgobrowser.listener;

import com.skarbo.csgobrowser.container.SearchUsersContainer;

public interface SearchUserListener extends HandlerListener {

	public void onSearchUsers(String search);
	
	public void onSearchUsersResult(SearchUsersContainer searchUsersContainer);

}
