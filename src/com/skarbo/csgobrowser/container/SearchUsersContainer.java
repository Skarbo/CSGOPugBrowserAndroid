package com.skarbo.csgobrowser.container;

import java.util.ArrayList;
import java.util.List;

public class SearchUsersContainer implements Container<SearchUsersContainer> {

	public String search;
	public List<SearchUsersContainer.SearchUser> searchUsers = new ArrayList<SearchUsersContainer.SearchUser>();

	public static class SearchUser {
		public String userid;
		public String username;
		public String name;
	}

	@Override
	public void merge(SearchUsersContainer merge) {

	}

}
