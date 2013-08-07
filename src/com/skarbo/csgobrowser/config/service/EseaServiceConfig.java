package com.skarbo.csgobrowser.config.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.container.PlayersContainer;
import com.skarbo.csgobrowser.container.SearchUsersContainer;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats.Stat;
import com.skarbo.csgobrowser.container.SearchUsersContainer.SearchUser;
import com.skarbo.csgobrowser.container.ServersContainer;
import com.skarbo.csgobrowser.handler.control.asynctask.MatchControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.asynctask.match.EseaMatchControlHandlerAsyncTask;
import com.skarbo.csgobrowser.utils.Utils;
import com.skarbo.csgobrowser.utils.Utils.Country;

public class EseaServiceConfig extends ServiceConfig {

	public static final String SERVICE_ID = "esea";
	public static final String URL = "http://play.esea.net/%s&region_id=2&locale=en_GB";

	public EseaServiceConfig() {
		this.id = SERVICE_ID;
		this.url = URL;

		this.cookies = new ServiceConfig.Cookie[2];
		this.cookies[0] = new ServiceConfig.Cookie("viewed_welcome_page", "1", "/", ".esea.net");
		this.cookies[1] = new ServiceConfig.Cookie("settings_game_ids", "25", "/", ".esea.net");

		this.headers = new ServiceConfig.Header[1];
		this.headers[0] = new ServiceConfig.Header("ESEA-Page-Format", "pagelet");

		this.pages = new EseaPages();
	}

	@Override
	public int getServiceImage() {
		return R.drawable.logo_esea;
	}

	public static class EseaPages extends Pages {

		private EseaPages() {
			this.servers = new EseaServers();
			this.server = new EseaServer();
			this.match = new EseaMatch();
			this.profile = new EseaProfile();
			this.searchUsers = new EseaSearchUsers();
		}

		public static class EseaServers extends Servers {

			private static String PAGE = "?s=servers&type=pug";
			private static String REGEX_IS_PAGE = "<div class=\"module-header\">Server Listing</div>";
			private static String REGEX_SERVER = "<tr\\sclass=\"row1\">\\s+<td><img\\ssrc=\"[\\w:/\\d.]+\"></td>\\s+<td><a href=\"[\\w/.=&?]+=(\\d+)\">([\\w\\s\\d]+)</a></td>\\s+<td>Pug</td>\\s+<td><a href=\".+?game=CSGO\">([\\d.:]+)</a></td>\\s+<td align=\"center\">(\\d+)/(\\d+)</td>\\s+</tr>";

			private EseaServers() {
				this.page = new Page(PAGE, REGEX_IS_PAGE);
				this.regexServer = REGEX_SERVER;
				this.container = new EseaServerContainer();
			}

			private static class EseaServerContainer extends Container {
				private static String REGEX_COUNTRY = "(.*?)\\s\\d+";
				private static int REGEX_GROUP_ID = 1;
				private static int REGEX_GROUP_NAME = 2;
				private static int REGEX_GROUP_IPADDRESS = 3;
				private static int REGEX_GROUP_PLAYER_CURRENT = 4;
				private static int REGEX_GROUP_PLAYER_MAX = 4;
				private static int PLAYERS_MAX = 10;

				private static Map<String, Country> COUNTRIES;
				{
					COUNTRIES = new HashMap<String, Country>();
					COUNTRIES.put("frankfurt", Country.DE);
					COUNTRIES.put("roubaix", Country.FR);
					COUNTRIES.put("london", Country.GB);
					COUNTRIES.put("stockholm", Country.SE);
					COUNTRIES.put("madrid", Country.ES);
				}

				@Override
				public ServersContainer.Server createServer(Matcher serverMatcher) {
					ServersContainer.Server server = new ServersContainer.Server();

					server.serviceId = SERVICE_ID;
					server.id = Utils.trimWhitespace(serverMatcher.group(REGEX_GROUP_ID));
					server.name = Utils.trimWhitespace(serverMatcher.group(REGEX_GROUP_NAME));
					server.ipAddress = Utils.trimWhitespace(serverMatcher.group(REGEX_GROUP_IPADDRESS));
					server.playersCurrent = Utils.parseInt(Utils.trimWhitespace(serverMatcher
							.group(REGEX_GROUP_PLAYER_CURRENT)));
					server.playersMax = PLAYERS_MAX; // Utils.parseInt(Utils.trimWhitespace(serverMatcher.group(5)));

					// Country
					Matcher countryMatcher = Pattern.compile(REGEX_COUNTRY).matcher(server.name);
					if (countryMatcher.find()) {
						String country = countryMatcher.group(1).toLowerCase();
						if (COUNTRIES.containsKey(country)) {
							server.country = COUNTRIES.get(country);
						}
					}

					// Location
					server.location = server.name.replace(server.id, "").trim();

					// Status
					if (server.playersCurrent >= PLAYERS_MAX) {
						server.status = ServersContainer.Server.Status.Live;
					} else if (server.playersCurrent > 0) {
						server.status = ServersContainer.Server.Status.Waiting;
					} else {
						server.status = ServersContainer.Server.Status.Available;
					}

					return server;
				}
			}
		}

		public static class EseaServer extends Server {

			public static String REGEX_ISPAGE_SERVER = "<div class=\"module-header\">Server Details</div>";
			public static String REGEX_ISPAGE_PLAYER_MATCH = "<div class=\"module-header\">.*?-\\sUser Profile</div>";
			public static String REGEX_INFO = "<label>Name:</label>.*?<div class=\"data\">(.*?)</div>.*?<label>Address:</label>.*?<div class=\"data\">(.*?)</div>.*?<label>Players:</label>.*?<div class=\"data\">.*?(\\d+)/(\\d+).*?</div>";
			public static String REGEX_INFO_PLAYERS = "<tr class=\"row1\">\\s+<th align=\"left\">.*?</th>\\s+<td><a href=\"/premium\">.*?<img.*?<a.*?<img src=\"(.*?)\".*?></a>\\s<a href=\"/users/(\\d+)\">(.*?)</a></td>\\s+<td align=\"center\">(.*?)</td>\\s+</tr>";
			public static String REGEX_INFO_COUNTRY = "(.*?)\\s\\d+";
			public static String REGEX_PLAYER_MATCH = "<div class=\"sub-header bold margin-top\">Recent Matches</div>.*?<tr class=\"row1\">\\s+<td>.*?<td><a href=\".*?&id=(\\d+)\">Pug</a></td>.*?</td>.*?</td>.*?<td align=\"right\">(.*?)</td>.*?<td align=\"right\"><a.*?>Live</a></td>.*?<td align=\"right\"><a.*?>(\\d+)-(\\d+)</a></td>.*?</tr>";

			public static Page PAGE_SERVER = new Page("?s=servers&id=%s", REGEX_ISPAGE_SERVER);
			public static Page PAGE_PLAYER_MATCH = new Page("users/%s?tab=stats", REGEX_ISPAGE_PLAYER_MATCH);
			public static int PLAYERS_MAX = 10;

			public EseaServer() {
			}

			@Override
			public List<PlayersContainer.Player.Stats.Stat> showPlayerStats() {
				ArrayList<Stat> types = new ArrayList<PlayersContainer.Player.Stats.Stat>();
				types.add(PlayersContainer.Player.Stats.Stat.time);
				return types;
			}

		}

		public static class EseaMatch extends Match {

			private static String REGEX_ISPAGE_MATCH = "<div class=\"module-header\">.*?Pug Match\\s#.*?</div>";
			private static String PAGE_MATCH = "index.php?s=stats&d=match&id=%s";
			private static String REGEX_MATCH_INFO_DATE_TIME_MAP = "<div class=\"match-header\">\\s+<div class=\"spacer\">.*?<h1>.*?<img.*?>\\s(.*?)\\s/\\s(.*?)\\s/\\s(.*?)\\s/.*?</div>";
			private static String REGEX_MATCH_INFO_ID_STATUS = "<div class=\"module-header\">\\s+Pug Match\\s#(\\d+)\\s-\\s(\\w+)\\s+</div>";
			private static String REGEX_MATCH_SCORES = "<table class=\"box\" cellspacing=\"0\">\\s+<thead>\\s+<tr>\\s+<th colspan=\"100\" align=\"left\">Period Scores</th>\\s+</tr>\\s+</thead>\\s+<tr>.*?</tr>\\s+<tr>\\s+<th align=\"left\">Team A</th>\\s+<td class=\"t stat\">(\\d+)</td>\\s+<td.*?>(.*?)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">\\d+</td>\\s+</tr>\\s+<tr>\\s+<th align=\"left\">Team B</th>\\s+<td class=\"ct stat\">(\\d+)</td>\\s+<td.*?>(.*?)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">\\d+</td>\\s+</tr>\\s+</table>";
			private static String REGEX_MATCH_STATS_TEAM = "<tbody id=\"body-match-total\\d+\">(.*?)</tbody>";
			private static String REGEX_MATCH_STATS_PLAYER = "<tr>\\s+<td><a.*?><img src=\"(.*?)\".*?></a> <a href=\"/users/(\\d+)\">(.*?)</a></td>\\s+<td class=\"stat\">([\\d.\\-]+)</td>\\s+<td class=\"stat\">([\\d.\\-]+)</td>\\s+<td class=\"stat\">([\\d.\\-]+)</td>\\s+<td class=\"stat\">([\\d.\\-]+)</td>\\s+<td class=\"stat\">([\\d.\\-]+)</td>\\s+<td class=\"stat\">([\\d.\\-]+)</td>\\s+<td class=\"stat\">([\\d.\\-]+)</td>\\s+<td class=\"stat\">([\\d.\\-]+)</td>\\s+<td class=\"stat\">([\\d.\\-]+)</td>\\s+<td class=\"stat\">([\\d.\\-]+)</td>\\s+<td class=\"stat\">([\\d.\\-]+)</td>\\s+<td class=\"stat\">([\\d.\\-]+)</td>\\s+<td class=\"stat\">([\\d.\\-]+)</td>\\s+<td class=\"stat\">([\\d.\\-]+)</td>.*?<td class=\"stat\">([\\d.\\-]+)</td>\\s+<td class=\"stat\">([\\d.\\-]+)</td>.*?</tr>";

			public Page pageMatch = new Page(PAGE_MATCH, REGEX_ISPAGE_MATCH);
			public String regexMatchInfo_DateTimeMap = REGEX_MATCH_INFO_DATE_TIME_MAP;
			public String regexMatchInfo_IdStatus = REGEX_MATCH_INFO_ID_STATUS;
			public String regexMatchScores = REGEX_MATCH_SCORES;
			public String regexMatchStatsTeam = REGEX_MATCH_STATS_TEAM;
			public String regexMatchStatsPlayer = REGEX_MATCH_STATS_PLAYER;

			@Override
			public List<List<PlayersContainer.Player.Stats.Stat>> showPlayerStats() {
				List<List<Stat>> typesList = new ArrayList<List<Stat>>();

				ArrayList<Stat> types = new ArrayList<PlayersContainer.Player.Stats.Stat>();
				types.add(PlayersContainer.Player.Stats.Stat.roundWinShares);
				types.add(PlayersContainer.Player.Stats.Stat.damagePrRound);
				types.add(PlayersContainer.Player.Stats.Stat.fragsPrRound);
				types.add(PlayersContainer.Player.Stats.Stat.roundsPlayed);
				typesList.add(types);

				types = new ArrayList<PlayersContainer.Player.Stats.Stat>();
				types.add(PlayersContainer.Player.Stats.Stat.frags);
				types.add(PlayersContainer.Player.Stats.Stat.assists);
				types.add(PlayersContainer.Player.Stats.Stat.deaths);
				typesList.add(types);

				types = new ArrayList<PlayersContainer.Player.Stats.Stat>();
				types.add(PlayersContainer.Player.Stats.Stat.kill2);
				types.add(PlayersContainer.Player.Stats.Stat.kill3);
				types.add(PlayersContainer.Player.Stats.Stat.kill4);
				types.add(PlayersContainer.Player.Stats.Stat.kill5);
				typesList.add(types);

				types = new ArrayList<PlayersContainer.Player.Stats.Stat>();
				types.add(PlayersContainer.Player.Stats.Stat.oneV1);
				types.add(PlayersContainer.Player.Stats.Stat.oneV2);
				types.add(PlayersContainer.Player.Stats.Stat.headshotPercentage);
				typesList.add(types);

				return typesList;
			}

			@Override
			public Class<? extends MatchControlHandlerAsyncTask<?>> getMatchControlHandlerClass() {
				return EseaMatchControlHandlerAsyncTask.class;
			}

		}

		public static class EseaProfile extends Profile {
			private static String REGEX_ISPAGE_PROFILE = "<div class=\"module-header\">.*?\\s-\\sUser Profile</div>";
			private static String REGEX_ISPAGE_PROFILE_STATS = "<div class=\"module-header\">.*?User Profile</div>.*?<div class=\"tabArea\".*?>.*?<a class=\"tab activeTab\".*?>Statistics</a>";

			private static Pattern REGEX_PROFILE_INFO = Pattern
					.compile(
							"<div class=\"tabArea\" id=\"profile-(\\d+)\">.*?<div id=\"profile-header\">\\s+<h1>(.*?)<span>\\s+/(.*?)</span>\\s+</h1>\\s+</div>\\s+<div id=\"profile-info\">\\s+<a.*?><img.*?src=\"(.*?)\".*?></a>\\s+<div class=\"content\">\\s+<div class=\"sub-header bold\">User Information</div>\\s+<label>Name:</label>\\s+<div class=\"data\">(.*?)</div>\\s+<label>Age:</label>\\s+<div class=\"data\">(\\d+)</div>\\s+<label>Gender:</label>\\s+<div class=\"data\">(.*?)</div>\\s+<label>Country:</label>\\s+<div class=\"data\">\\s+<span><a.*?><img.*?src=\"(.*?)\"\\stitle=\"(.*?)\".*?></a>.*?</div>\\s+<label>Location:</label>\\s+<div class=\"data\">(.*?)</div>.*?<label>Karma:</label>\\s+<div class=\"data\">\\s+<a.*?/a>\\s<span.*?>(\\d+)</span>\\s+</div>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			private static Pattern REGEX_PROFILE_INFO_RWSRANK = Pattern.compile(
					"<div id=\"rankGraph\">.*?<a.*?>.*?<h1><small>#</small>(\\d+)</h1>", Pattern.DOTALL
							| Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			private static Pattern REGEX_PROFILE_INFO_FRIENDS = Pattern
					.compile(
							"<tr class=\"row1.*?\" id=\"buddy-(\\d+)\">\\s+<td><img.*?>\\s<a.*?<img\\ssrc=\"(.*?)\"\\stitle=\"(.*?)\".*?<a.*?>(.*?)</a></td>\\s+<td class=\"online-stream\">.*?</td>\\s+<td class=\"online-game\">(.*?)</td>\\s+</tr>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			private static Pattern REGEX_PROFILE_INFO_FRIENDS_PLAYING = Pattern
					.compile(
							"<a href=\"/index\\.php\\?s=servers&id=(\\d+)\"><img src=\".*?\" title=\"User is currently playing CSGO in (.*?)\"></a>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			private static Pattern REGEX_PROFILE_INFO_MATCHES = Pattern
					.compile(
							"<tr class=\"row1\">\\s+<td><img.*?></td>\\s+<td>(.*?)</td>\\s+<td><a href=\"/index\\.php\\?s=stats&d=match&id=(\\d+)\">\\w\\s(\\d+)-(\\d+)</a></td>\\s+<td align=\"right\"><a.*?>Pug</a></td>\\s+</tr>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

			private static Pattern REGEX_PROFILE_STATS_SERVER = Pattern
					.compile(
							"<th colspan=\"20\">\\s+<span class=\"right\"><b>Time Played:</b>\\s(.*?)</span>\\s+<b>Record:</b>\\s(\\d+)-(\\d+)-(\\d+)\\s\\(([\\d.]+)\\)\\s+</th>\\s+</tr>\\s+<tr>.*?</tr>\\s+<tr>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td class=\"stat\">([\\d.]+)</td>\\s+<td class=\"stat\">([\\d.]+)</td>\\s+<td class=\"stat\">([\\d.]+)</td>\\s+<td class=\"stat\">([\\d.]+)</td>\\s+</tr>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			private static Pattern REGEX_PROFILE_STATS_WEAPONS_GROUP = Pattern
					.compile(
							"<div class=\"sub-header bold margin-top\">Top Weapons</div>\\s+<table width=\"100%\" cellspacing=\"0\" cellpadding=\"1\" border=\"0\">(.*?)</table>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			private static Pattern REGEX_PROFILE_STATS_WEAPONS = Pattern
					.compile(
							"<tr class=\"row1\">\\s+<td>(.*?)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td.*?<td class=\"stat\">(\\d+)</td>\\s+<td.*?<td class=\"stat\">([\\d.]+)</td>\\s+<td.*?</tr>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			private static Pattern REGEX_PROFILE_STATS_MAPS_GROUP = Pattern
					.compile(
							"<div class=\"sub-header bold margin-top\">Top Maps</div>\\s+<table width=\"100%\" cellspacing=\"0\" cellpadding=\"1\" border=\"0\">(.*?)</table>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			private static Pattern REGEX_PROFILE_STATS_MAPS = Pattern
					.compile(
							"<tr class=\"row1\">\\s+<td>(.*?)</td>\\s+<td class=\"stat\">(\\d+)</td>\\s+<td.*?<td class=\"stat\">(\\d+)</td>\\s+<td.*?<td class=\"stat\">([\\d.]+)</td>\\s+<td.*?<td class=\"stat\">([\\d.]+)</td>\\s+<td.*?</tr>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			private static Pattern REGEX_PROFILE_STATS_MATCHES = Pattern
					.compile(
							"<tr class=\"row1\">\\s+<td>.*?</td>\\s+<td><a href=\"/index\\.php\\?s=stats&d=match&id=(\\d+)\">Pug</a></td>\\s+<td.*?<td.*?<td align=\"right\">(.*?)</td>\\s+<td.*?<td.*?>(\\d+)-(\\d+)</a></td>\\s+<td align=\"center\">(\\d+)</td>\\s+<td align=\"center\">(\\d+)</td>\\s+<td align=\"center\">(\\d+)</td>\\s+<td align=\"center\">([.\\d]+)</td>\\s+<td align=\"right\"><a.*?>(.*?)</a></td>\\s+</tr>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

			private static String PAGE_PROFILE = "users/%s";
			private static String PAGE_PROFILE_STATS = "users/%s?tab=stats&last_type_scope=pug&game_id=25&type_scope=pug&period[type]=career";

			public Page pageProfile = new Page(PAGE_PROFILE, REGEX_ISPAGE_PROFILE);
			public Page pageProfileStats = new Page(PAGE_PROFILE_STATS, REGEX_ISPAGE_PROFILE_STATS);

			public Pattern regexProfileInfo = REGEX_PROFILE_INFO;
			public Pattern regexProfileInfo_RWSRank = REGEX_PROFILE_INFO_RWSRANK;
			public Pattern regexProfileInfo_Friends = REGEX_PROFILE_INFO_FRIENDS;
			public Pattern regexProfileInfo_FriendsPlaying = REGEX_PROFILE_INFO_FRIENDS_PLAYING;
			public Pattern regexProfileInfo_Matches = REGEX_PROFILE_INFO_MATCHES;
			public Pattern regexProfileStats_Server = REGEX_PROFILE_STATS_SERVER;
			public Pattern regexProfileStats_WeaponsGroup = REGEX_PROFILE_STATS_WEAPONS_GROUP;
			public Pattern regexProfileStats_Weapons = REGEX_PROFILE_STATS_WEAPONS;
			public Pattern regexProfileStats_MapsGroup = REGEX_PROFILE_STATS_MAPS_GROUP;
			public Pattern regexProfileStats_Maps = REGEX_PROFILE_STATS_MAPS;
			public Pattern regexProfileStats_Matches = REGEX_PROFILE_STATS_MATCHES;

			@Override
			public Stats.Stat[] getMainStats() {
				return new Stats.Stat[] { Stats.Stat.roundWinShares, Stats.Stat.roundWinSharesRank };
			}

			@Override
			public Stat[] getTopMapsStats() {
				return new Stats.Stat[] { Stats.Stat.frags, Stats.Stat.deaths, Stats.Stat.fragsPrRound,
						Stats.Stat.wonMatchesPercentage };
			}

			@Override
			public Stat[] getTopWeaponsStats() {
				return new Stats.Stat[] { Stats.Stat.frags, Stats.Stat.deaths, Stats.Stat.headshotPercentage };
			}

			@Override
			public Stat[] getMatchesStats() {
				return new Stats.Stat[] { Stats.Stat.frags, Stats.Stat.deaths, Stats.Stat.roundWinShares };
			}
		}

		public static class EseaSearchUsers extends SearchUsers {

			private static final String PAGE_SEARCH_USERS = "index.php?s=search&source=users&query=%s";
			private static final String REGEX_IS_PAGE_SEARCH_USERS = "<div class=\"module-header\">Search</div>";
			private static final Pattern REGEX_SEARCH_RESULT = Pattern
					.compile(
							"<div class=\"result-container\">.*?<a class=\"result\" href=\"/users/(\\d+)\">(.*?)</a>.*?<h2>.*?</h2>.*?<a class=\"more\" href=\"/users/\\d+\\?tab=stats\">Statistics</a>.*?</div>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			private static final String REGEX_REPLACE_NAME = "(?si)\".*?\"";
			private static final Pattern REGEX_USERNAME = Pattern.compile("\"(.*?)\"", Pattern.DOTALL
					| Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

			private static final Page PAGE = new Page(PAGE_SEARCH_USERS, REGEX_IS_PAGE_SEARCH_USERS);

			private static final int MATCHER_GROUP_SEARCH_RESULT_USERID = 1;
			private static final int MATCHER_GROUP_SEARCH_RESULT_USERNAME_NAME = 2;

			@Override
			public Page getPage() {
				return PAGE;
			}

			@Override
			public Pattern getRegexSearchResult() {
				return REGEX_SEARCH_RESULT;
			}

			@Override
			public SearchUser createSearchUser(Matcher searchUserMatcher) {
				SearchUsersContainer.SearchUser searchUser = new SearchUsersContainer.SearchUser();

				searchUser.userid = Utils.trimWhitespace(searchUserMatcher.group(MATCHER_GROUP_SEARCH_RESULT_USERID));

				String usernameName = Utils.trimWhitespace(searchUserMatcher
						.group(MATCHER_GROUP_SEARCH_RESULT_USERNAME_NAME));

				searchUser.name = Utils.trimWhitespace(usernameName.replaceAll(REGEX_REPLACE_NAME, ""));

				Matcher usernameMatcher = REGEX_USERNAME.matcher(usernameName);
				if (usernameMatcher.find()) {
					searchUser.username = Utils.trimWhitespace(usernameMatcher.group(1));
				}

				return searchUser;
			}

		}
	}

}
