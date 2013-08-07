package com.skarbo.csgobrowser.config.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.config.ServiceConfig.Page;
import com.skarbo.csgobrowser.config.ServiceConfig.Pages.SearchUsers;
import com.skarbo.csgobrowser.container.PlayersContainer;
import com.skarbo.csgobrowser.container.SearchUsersContainer;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats.Stat;
import com.skarbo.csgobrowser.container.SearchUsersContainer.SearchUser;
import com.skarbo.csgobrowser.container.ServersContainer;
import com.skarbo.csgobrowser.handler.control.asynctask.MatchControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.asynctask.match.LeetwayMatchControlHandlerAsyncTask;
import com.skarbo.csgobrowser.utils.Utils;
import com.skarbo.csgobrowser.utils.Utils.Country;

public class LeetwayServiceConfig extends ServiceConfig {

	public static final String TAG = LeetwayServiceConfig.class.getSimpleName();
	public static final String SERVICE_ID = "leetway";
	public static final String URL = "http://leetway.com/%s";

	public LeetwayServiceConfig() {
		this.id = SERVICE_ID;
		this.url = URL;

		this.pages = new LeetwayPages();
	}

	@Override
	public int getServiceImage() {
		return R.drawable.logo_leetway;
	}

	public static class LeetwayPages extends Pages {

		private LeetwayPages() {
			this.servers = new LeetwayServers();
			this.server = new LeetwayServer();
			this.match = new LeetwayMatch();
			this.profile = new LeetwayProfile();
			this.searchUsers = new LeetwaySearchUsers();
		}

		private static class LeetwayServers extends Servers {

			public static String PAGE = "ajax/match/servers.php";
			public static String REGEX_IS_PAGE = "<table cellspacing=\"0\" cellpadding=\"0\" class=\"table table-striped table-bordered table-condensed fxsmall\">";
			public static String REGEX_SERVER = "<tr>\\s+<td.*?>(\\d+)</td>\\s+<td><img.*?src=\".*?/(\\w+).png\".*?alt=\"(.*?)\".*?</td>\\s+<td.*?>(.*?)</td>.*?</td>.*?</td>.*?</td>.*?</td>.*?<td.*?>(\\d+)/(\\d+)</td>.*?<td.*?>(.*?)</td>.*?<td.*?><a\\shref=\"(.*?)\".*?>(.*?)</a></td>.*?</tr>";

			public static String POST_KEY_GAME = "game";
			public static String POST_KEY_REGION = "region";
			public static String POST_VALUE_ALL = "ALL";

			public LeetwayServers() {
				this.page = new Page(PAGE, REGEX_IS_PAGE);
				this.regexServer = REGEX_SERVER;

				this.page.post = new HashMap<String, String>();
				this.page.post.put(POST_KEY_GAME, POST_VALUE_ALL);
				this.page.post.put(POST_KEY_REGION, POST_VALUE_ALL);

				this.container = new LeetwayServersContainer();
			}

			private static class LeetwayServersContainer extends Container {

				public static String REGEX_NAME_REPLACE = ".*?,\\s";
				public static String REGEX_MAP = "<a.*?data-html=\"true\">(.*?)</a>";
				public static String REGEX_SCORE = "<span.*?>(\\d+)</span>.*?<span.*?>(\\d+)</span>";
				public static String REGEX_MATCHID = "match/lobby/(\\d+)";

				public static int REGEX_GROUP_ID = 1;
				public static int REGEX_GROUP_COUNTRY = 2;
				public static int REGEX_GROUP_NAME = 3;
				public static int REGEX_GROUP_MAP = 4;
				public static int REGEX_GROUP_PLAYERS_CURRENT = 5;
				public static int REGEX_GROUP_PLAYERS_MAX = 6;
				public static int REGEX_GROUP_SCORE = 7;
				public static int REGEX_GROUP_MATCHID = 8;
				public static int REGEX_GROUP_STATUS = 9;

				public static Map<String, Country> COUNTRIES;
				{
					COUNTRIES = new HashMap<String, Country>();
					COUNTRIES.put("de", Country.DE);
					COUNTRIES.put("se", Country.SE);
					COUNTRIES.put("us", Country.US);
					COUNTRIES.put("gb", Country.GB);
				}

				public static Map<String, ServersContainer.Server.Status> STATUS;
				{
					STATUS = new HashMap<String, ServersContainer.Server.Status>();
					STATUS.put("Live", ServersContainer.Server.Status.Live);
					STATUS.put("Waiting", ServersContainer.Server.Status.Waiting);
					STATUS.put("Initializing", ServersContainer.Server.Status.Waiting);
				}

				private Pattern regexMap;
				private Pattern regexScore;
				private Pattern regexMatchid;

				public LeetwayServersContainer() {
					this.regexMap = Pattern.compile(REGEX_MAP);
					this.regexScore = Pattern.compile(REGEX_SCORE);
					this.regexMatchid = Pattern.compile(REGEX_MATCHID);
				}

				@Override
				public ServersContainer.Server createServer(Matcher serverMatcher) {
					ServersContainer.Server server = new ServersContainer.Server();

					server.serviceId = SERVICE_ID;
					server.id = Utils.trimWhitespace(serverMatcher.group(REGEX_GROUP_ID));
					server.name = Utils.trimWhitespace(serverMatcher.group(REGEX_GROUP_NAME));
					server.playersCurrent = Utils.parseInt(Utils.trimWhitespace(serverMatcher
							.group(REGEX_GROUP_PLAYERS_CURRENT)));
					server.playersMax = Utils.parseInt(Utils.trimWhitespace(serverMatcher
							.group(REGEX_GROUP_PLAYERS_MAX)));

					// Name
					if (server.name != null) {
						server.name = String.format("%s %s", server.name.replaceAll(REGEX_NAME_REPLACE, ""), server.id);
					}

					// Country
					if (COUNTRIES.containsKey(Utils.trimWhitespace(serverMatcher.group(REGEX_GROUP_COUNTRY)))) {
						server.country = COUNTRIES.get(Utils.trimWhitespace(serverMatcher.group(REGEX_GROUP_COUNTRY)));
					}

					// Location
					server.location = server.name.replace(server.id, "").trim();

					// Map
					Matcher mapMatcher = this.regexMap.matcher(Utils.trimWhitespace(serverMatcher
							.group(REGEX_GROUP_MAP)));
					if (mapMatcher.find()) {
						server.map = Utils.trimWhitespace(mapMatcher.group(1));
					}

					// Score
					Matcher scoreMatcher = this.regexScore.matcher(Utils.trimWhitespace(serverMatcher
							.group(REGEX_GROUP_SCORE)));
					if (scoreMatcher.find()) {
						server.scoreHome = Utils.parseInt(Utils.trimWhitespace(scoreMatcher.group(1)));
						server.scoreAway = Utils.parseInt(Utils.trimWhitespace(scoreMatcher.group(2)));
					}

					// Match id
					Matcher matchidMatcher = this.regexMatchid.matcher(Utils.trimWhitespace(serverMatcher
							.group(REGEX_GROUP_MATCHID)));
					if (matchidMatcher.find()) {
						server.matchId = Utils.trimWhitespace(matchidMatcher.group(1));
					}

					// Status
					server.status = ServersContainer.Server.Status.Available;
					if (STATUS.containsKey(Utils.trimWhitespace(serverMatcher.group(REGEX_GROUP_STATUS)))) {
						server.status = STATUS.get(Utils.trimWhitespace(serverMatcher.group(REGEX_GROUP_STATUS)));
					}

					return server;
				}
			}
		}

		public static class LeetwayServer extends Server {

			public static String REGEX_ISPAGE_SERVER = "<title>Match\\s#\\d+\\sLobby</title>";
			public static String REGEX_ISPAGE_MATCH = "<title>Match\\s#\\d+:\\sOverview</title>";
			public static String REGEX_ISPAGE_SERVER_PLAYERS = "\\{\"status\":\"\\d+\",\"players\":\"\\d+\"";
			public static String REGEX_SERVER_MATCHID = "<title>Match\\s#(\\d+)\\sLobby</title>";
			public static String REGEX_SERVER_INFO = "<td\\sclass=\"center\"><a.*?alt='(.*?)'.*?</a></td>.*?<td\\sclass=\"center\"><img.*?src=\".*?/(\\w+)\\.png\".*?alt=\".*?,\\s(.*?)\".*?</td>.*?<td\\sclass=\"center\"><span.*?>(\\d+)</span>/(\\d+)</td>.*?<td\\sclass=\"center\"><span.*?>(.*?)</span></td>.*?<td\\sclass=\"center\"><span.*?>(\\d+)</td>.*?<td\\sclass=\"center\"><span.*?>(\\d+)</td>";
			// public static String REGEX_SERVER_PLAYERS =
			// "<tr.*?<td><a\\shref=\\\\\".*?/(\\d+)\\\\\".*?><img\\ssrc=\\\\\"(.*?)\\\\\".*?\\\\/>(.*?)<\\\\/a><\\\\/td>.*?<td\\sclass=\\\\\"center\\\\\">([\\d.\\-]+)<\\\\/td>.*?<td\\sclass=\\\\\"center\\\\\">([\\d.\\-]+)<\\\\/td>.*?<\\\\/tr>";
			public static String REGEX_SERVER_PLAYERS = "<tr.*?<td>(.*?)<\\\\/td>.*?<td\\sclass=\\\\\"center\\\\\">(.*?)<\\\\/td>.*?<td\\sclass=\\\\\"center\\\\\">(.*?)<\\\\/td>.*?<\\\\/tr>";
			public static String REGEX_SERVER_PLAYER_ID_IMAGE_NAME = "<a\\shref=\\\\\".*?/(\\d+)\\\\\".*?><img\\ssrc=\\\\\"(.*?)\\\\\".*?\\\\/>(.*?)<\\\\/a>";

			public static Page PAGE_SERVER = new Page("match/lobby/%matchid%", REGEX_ISPAGE_SERVER);
			public static Page PAGE_SERVER_PLAYERS = new Page("match/ajax_update_lobby/%matchid%",
					REGEX_ISPAGE_SERVER_PLAYERS);

			public static int PLAYERS_MAX = 10;

			public LeetwayServer() {
			}

			@Override
			public List<Stat> showPlayerStats() {
				ArrayList<Stat> types = new ArrayList<PlayersContainer.Player.Stats.Stat>();
				types.add(PlayersContainer.Player.Stats.Stat.playerGamingRating);
				types.add(PlayersContainer.Player.Stats.Stat.reputation);
				return types;
			}

		}

		public static class LeetwayMatch extends Match {

			private static String REGEX_ISPAGE_MATCH = "<h1>PUG Match Details #\\d+</h1>";
			private static String REGEX_ISPAGE_MATCH_ACHIEVEMENTS = "<h1>PUG Match Details #\\d+</h1>.*?<h2>Killstreaks and Clutch Situations</h2>";
			private static String PAGE_MATCH = "match/details/%s?tab=overview";
			private static String PAGE_MATCH_ACHIEVEMENTS = "match/details/22468?tab=achievements";
			private static String REGEX_MATCH_INFO = "<h1>PUG Match Details #(\\d+)</h1>.*?<tr>\\s+<td class=\"center\"><a class=\"bstip\" title=\"<img.*?alt='(.*?)'.*?</a></td>\\s+<td class=\"center\"><img src=\".*?/(\\w+).png\".*?>.*?,\\s(.*?)</td>\\s+<td class=\"center\">(.*?)</td>\\s+<td class=\"center\">.*?</td>\\s+<td class=\"center\"><span.*?>(.*?)</span></td>\\s+<td class=\"center\"><span.*?>(\\d+)</td>\\s+<td class=\"center\"><span.*?>(\\d+)</td>\\s+</tr>";
			private static String REGEX_MATCH_STATS_TEAM = "<table cellpadding=\"0\" cellspacing=\"0\" class=\"table fsmall table-striped table-bordered table-condensed\">(.*?)</table>";
			private static String REGEX_MATCH_STATS_PLAYER = "<tr.*?>\\s+<td><a href=\"/profile/user/(\\d+)\" class=\"\"><img src=\"(.*?)\" alt=\"User\" class=\"avatar\" />(.*?)</a></td>\\s+<td class=\"center\">(\\d+)</td>\\s+<td class=\"center\">(\\d+)</td>\\s+<td class=\"center\">(\\d+)</td>\\s+<td class=\"center\">(\\d+)</td>\\s+<td class=\"center\">(\\d+)</td>\\s+<td class=\"center\">([\\d.\\-+]+)</td>\\s+<td class=\"center\">(\\d+)</td>\\s+<td class=\"center\">(\\d+)</td>\\s+<td class=\"center\">(\\d+)</td>\\s+<td class=\"center\">([\\d.\\-+]+)</td>\\s+<td class=\"center\">([\\d.\\-+]+)</td>\\s+<td class=\"center\">(\\d+)</td>\\s+<td class=\"center\"><span.*?>([\\d.\\-+]+)</span></td>\\s+</tr>";
			private static String REGEX_MATCH_STATS_PLAYER_ACHIEVEMENT = "<tr>\\s+<td><a href=\"/profile/user/(\\d+)\" class=\"\"><img.*?> (.*?)</a></td>\\s+<td.*?><a.*?>(\\d+)</a></td>\\s+<td.*?><a.*?>(\\d+)</a></td>\\s+<td.*?><a.*?>(\\d+)</a></td>\\s+<td.*?><a.*?>(\\d+)</a></td>\\s+<td.*?><a.*?>(\\d+)</a></td>\\s+<td.*?><a.*?>(\\d+)</a></td>\\s+<td.*?><a.*?>(\\d+)</a></td>\\s+<td.*?><a.*?>(\\d+)</a></td>\\s+<td.*?><a.*?>(\\d+)</a></td>\\s+</tr>";

			public Page pageMatch = new Page(PAGE_MATCH, REGEX_ISPAGE_MATCH);
			public Page pageMatchAchievments = new Page(PAGE_MATCH_ACHIEVEMENTS, REGEX_ISPAGE_MATCH_ACHIEVEMENTS);
			public String regexMatchInfo = REGEX_MATCH_INFO;
			public String regexMatchStats_Team = REGEX_MATCH_STATS_TEAM;
			public String regexMatchStats_Player = REGEX_MATCH_STATS_PLAYER;
			public String regexMatchStats_PlayerAchievment = REGEX_MATCH_STATS_PLAYER_ACHIEVEMENT;

			@Override
			public Class<? extends MatchControlHandlerAsyncTask<?>> getMatchControlHandlerClass() {
				return LeetwayMatchControlHandlerAsyncTask.class;
			}

			@Override
			public List<List<Stat>> showPlayerStats() {
				List<List<Stat>> typesList = new ArrayList<List<Stat>>();

				ArrayList<Stat> types = new ArrayList<PlayersContainer.Player.Stats.Stat>();
				types.add(PlayersContainer.Player.Stats.Stat.points);
				types.add(PlayersContainer.Player.Stats.Stat.killDeathRatio);
				types.add(PlayersContainer.Player.Stats.Stat.damagePrRound);
				types.add(PlayersContainer.Player.Stats.Stat.roundsPlayed);
				typesList.add(types);

				types = new ArrayList<PlayersContainer.Player.Stats.Stat>();
				types.add(PlayersContainer.Player.Stats.Stat.frags);
				types.add(PlayersContainer.Player.Stats.Stat.assists);
				types.add(PlayersContainer.Player.Stats.Stat.deaths);
				typesList.add(types);

				types = new ArrayList<PlayersContainer.Player.Stats.Stat>();
				types.add(PlayersContainer.Player.Stats.Stat.headshots);
				types.add(PlayersContainer.Player.Stats.Stat.headshotPercentage);
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
				types.add(PlayersContainer.Player.Stats.Stat.oneV3);
				types.add(PlayersContainer.Player.Stats.Stat.oneV4);
				types.add(PlayersContainer.Player.Stats.Stat.oneV5);
				typesList.add(types);

				return typesList;
			}

		}

		public static class LeetwayProfile extends Profile {

			private static final String PAGE_INFO = "profile/user/%s";
			private static final String PAGE_WEAPONS = "profile/user/%s?tab=weaponstats";

			private static final String REGEX_ISPAGE_INFO = "<ul class=\"breadcrumb\">.*?<li><a href=\"/profile/user/\\d+\".*?><img.*?/>.*?</a>.*?</li>.*?<li class=\"active\">Player Profile</li>.*?</ul>";
			private static final String REGEX_ISPAGE_WEAPONS = "<th class=\"center\"><a title=\"Weapon Name\" class=\"bstip\">Weapon</a></th>.*?<th class=\"center\"><a title=\"Kills with weapon\" class=\"bstip\">Kills</a></th>.*?<th class=\"center\"><a title=\"Headshots with weapon\" class=\"bstip\">Headshots</a></th>";

			private static Pattern REGEX_PROFILE_INFO = Pattern
					.compile(
							"<ul class=\"breadcrumb\">.*?<li><a href=\"/profile/user/(\\d+)\" class=\"\"><img src=\"(.*?)\".*?/>(.*?)</a>.*?</li>.*?<li.*?</ul>.*?<td>Member Since: (.*?)</td>.*? <td class=\"midrow\">Last Activity:\\s(.*?)</td>.*?<td>SteamID: <a.*?>(.*?)</a></td>.*?<td class=\"right\"><a href=\"(.*?)\" target=\"_blank\">View Steam Profile</a></td>.*?</tr>.*?</table>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			private static Pattern REGEX_PROFILE_INFO_MATCHES = Pattern
					.compile(
							"<tr>.*?<td class=\"center\"><a.*?>(\\d+)</a></td>.*?<td class=\"center\"><a.*?/>\".*?>(.*?)</a></td>.*?<td class=\"center\">(.*?)</td>.*?<td class=\"center\"><span.*?>(\\d+)\\s-\\s(\\d+)</span></td>.*?<td class=\"center\">(\\d+)</td>.*?<td class=\"center\">(\\d+)</td>.*?<td class=\"center\">(\\d+)</td>.*?<td class=\"center\">([\\d.+\\-]+)</td>.*?<td class=\"center\">([\\d.+\\-]+)</td>.*?<td class=\"center\">(\\d+)</td>.*?<td class=\"center\"><span.*?>([\\d.+\\-]+)</span></td>.*?</tr>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			private static Pattern REGEX_PROFILE_INFO_MATCHES_GROUP = Pattern
					.compile(
							"<h2>Recent PUG Matches.*?<table.*?>.*?<tr>.*?<th class=\"center\">Match ID</th>.*?</tr>(.*?)</table>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			private static Pattern REGEX_PROFILE_INFO_STATS = Pattern
					.compile(
							"<th class=\"center\"><a title=\"Kills\" class=\"bstip\">K</a></th>.*?</tr>.*?<tr>.*?<td class=\"center\">(\\d+)</td>.*?<td class=\"center\">(\\d+)</td>.*?<td class=\"center\">(\\d+)</td>.*?<td class=\"center\">(\\d+)</td>.*?<td class=\"center\">(\\d+)</td>.*?<td class=\"center\">([\\d.\\-+]+)</td>.*?<td class=\"center\">(\\d+)</td>.*?<td class=\"center\">(\\d+)</td>.*?<td class=\"center\">(\\d+)</td>.*?<td class=\"center\">([\\d.\\-+,]+)</td>.*?<td class=\"center\">([\\d.\\-+,]+)</td>.*?<td class=\"center\">([\\d.\\-+,]+)</td>.*?<td class=\"center\">([\\d.\\-+,]+)</td>.*?</tr>.*?<th class=\"center\"><a title=\"Rounds Played\" class=\"bstip\">RDS</a></th>.*?<tr>.*?<td class=\"center\">(\\d+)</td>.*?<td class=\"center\">(\\d+)\\s\\(([\\d.\\-+]+)%\\)</td>.*?<td class=\"center\">(\\d+)\\s\\(([\\d.\\-+]+)%\\)</td>.*?<td class=\"center\">(\\d+)</td>.*?<td class=\"center\">(\\d+)\\s\\(([\\d.\\-+]+)%\\)</td>.*?<td class=\"center\">(\\d+)\\s\\(([\\d.\\-+]+)%\\)</td>.*?<td class=\"center\">(\\d+)\\s\\(([\\d.\\-+]+)%\\)</td>.*?</tr>.*?<th class=\"center\"><a title=\"2 Kill Streak\" class=\"bstip\">2K</a></th>.*?<tr>.*?<td class=\"center\"><a.*?>(\\d+)</a></td>.*?<td class=\"center\"><a.*?>(\\d+)</a></td>.*?<td class=\"center\"><a.*?>(\\d+)</a></td>.*?<td class=\"center\"><a.*?>(\\d+)</a></td>.*?<td class=\"center\"><a.*?>(\\d+)</a></td>.*?<td class=\"center\"><a.*?>(\\d+)</a></td>.*?<td class=\"center\"><a.*?>(\\d+)</a></td>.*?<td class=\"center\"><a.*?>(\\d+)</a></td>.*?<td class=\"center\"><a.*?>(\\d+)</a></td>.*?</tr>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			private static Pattern REGEX_PROFILE_WEAPONS = Pattern
					.compile(
							"<tr>.*?<td class=\"center\">(.*?)</td>.*?<td class=\"center\">(.*?)</td>.*?<td class=\"center\">(.*?)</td>.*?</tr>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

			public Page pageProfile = new Page(PAGE_INFO, REGEX_ISPAGE_INFO);
			public Page pageProfileWeapons = new Page(PAGE_WEAPONS, REGEX_ISPAGE_WEAPONS);
			public Pattern regexProfileInfo = REGEX_PROFILE_INFO;
			public Pattern regexProfileInfo_Matches = REGEX_PROFILE_INFO_MATCHES;
			public Pattern regexProfileInfo_MatchesGroup = REGEX_PROFILE_INFO_MATCHES_GROUP;
			public Pattern regexProfileInfo_Stats = REGEX_PROFILE_INFO_STATS;
			public Pattern regexProfileWeapons = REGEX_PROFILE_WEAPONS;

			@Override
			public Stats.Stat[] getMainStats() {
				return new Stats.Stat[] { Stats.Stat.playerGamingRating, Stats.Stat.reputation };
			}

			@Override
			public Stat[] getTopMapsStats() {
				return null;
			}

			@Override
			public Stat[] getTopWeaponsStats() {
				return new Stats.Stat[] { Stats.Stat.frags, Stats.Stat.headshots };
			}

			@Override
			public Stat[] getMatchesStats() {
				return new Stats.Stat[] { Stats.Stat.frags, Stats.Stat.deaths, Stats.Stat.damagePrRound };
			}

		}

		public static class LeetwaySearchUsers extends SearchUsers {

			private static final String PAGE_SEARCH_USERS = "search?q=%s&action=Search";
			private static final String REGEX_IS_PAGE_SEARCH_USERS = "<h1>Search Results</h1>";
			private static final Pattern REGEX_SEARCH_RESULT = Pattern
					.compile(
							"<div class=\"search-result\">.*?<strong><a href=\"/profile/user/(\\d+)\">User\\s-\\s(.*?)</a></strong>.*?<p>.*?</p>.*?</div>",
							Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

			private static final Page PAGE = new Page(PAGE_SEARCH_USERS, REGEX_IS_PAGE_SEARCH_USERS);

			private static final int MATCHER_GROUP_SEARCH_RESULT_USERID = 1;
			private static final int MATCHER_GROUP_SEARCH_RESULT_USERNAME = 2;

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
				searchUser.username = Utils.trimWhitespace(searchUserMatcher
						.group(MATCHER_GROUP_SEARCH_RESULT_USERNAME));

				return searchUser;
			}

		}
	}

}
