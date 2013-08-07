package com.skarbo.csgobrowser.config;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.skarbo.csgobrowser.config.service.EseaServiceConfig;
import com.skarbo.csgobrowser.config.service.LeetwayServiceConfig;
import com.skarbo.csgobrowser.container.PlayersContainer;
import com.skarbo.csgobrowser.container.SearchUsersContainer;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats;
import com.skarbo.csgobrowser.container.ServersContainer;
import com.skarbo.csgobrowser.handler.control.asynctask.MatchControlHandlerAsyncTask;

public abstract class ServiceConfig {

	public static final String[] SERVICE_IDS = { EseaServiceConfig.SERVICE_ID, LeetwayServiceConfig.SERVICE_ID };

	public static ServiceConfig createServiceConfig(String serviceId) {
		if (EseaServiceConfig.SERVICE_ID.equalsIgnoreCase(serviceId))
			return new EseaServiceConfig();
		if (LeetwayServiceConfig.SERVICE_ID.equalsIgnoreCase(serviceId))
			return new LeetwayServiceConfig();
		return null;
	}

	public String id;
	public String url;
	public Cookie[] cookies;
	public Header[] headers;
	public Pages pages;

	public abstract int getServiceImage();

	public static abstract class Pages {
		public Servers servers;
		public Server server;
		public Match match;
		public Profile profile;
		public SearchUsers searchUsers;

		public static abstract class Servers {
			public Page page;
			public String regexServer;
			public Container container;

			public static abstract class Container {
				public abstract ServersContainer.Server createServer(Matcher serverMatcher);
			}
		}

		public static abstract class Server {
			public abstract List<PlayersContainer.Player.Stats.Stat> showPlayerStats();
		}

		public static abstract class Match {
			public abstract Class<? extends MatchControlHandlerAsyncTask<?>> getMatchControlHandlerClass();

			public abstract List<List<PlayersContainer.Player.Stats.Stat>> showPlayerStats();
		}

		public static abstract class Profile {

			public abstract Stats.Stat[] getMainStats();

			public abstract Stats.Stat[] getTopMapsStats();

			public abstract Stats.Stat[] getTopWeaponsStats();

			public abstract Stats.Stat[] getMatchesStats();

		}

		public static abstract class SearchUsers {
			public abstract Page getPage();

			public abstract Pattern getRegexSearchResult();

			public abstract SearchUsersContainer.SearchUser createSearchUser(Matcher searchUserMatcher);
		}
	}

	public static class Cookie {
		public String name;
		public String value;
		public String domain;
		public String path;

		public Cookie(String name, String value, String path, String domain) {
			this.name = name;
			this.value = value;
			this.domain = domain;
			this.path = path;
		}
	}

	public static class Header {
		public String name;
		public String value;

		public Header(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}

	public static class Page {
		public String page;
		public String isPage;
		public Map<String, String> post;

		public Page(String page, String isPage) {
			this.page = page;
			this.isPage = isPage;
		}

		public Page(String page, String isPage, Map<String, String> post) {
			this(page, isPage);
			this.post = post;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ServiceConfig))
			return super.equals(o);
		return ((ServiceConfig) o).id == this.id;
	}

}
