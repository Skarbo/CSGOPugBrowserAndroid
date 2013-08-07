package com.skarbo.csgobrowser.handler.control.asynctask.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

import com.skarbo.csgobrowser.config.service.LeetwayServiceConfig;
import com.skarbo.csgobrowser.container.PlayersContainer;
import com.skarbo.csgobrowser.container.PlayersContainer.Player;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats;
import com.skarbo.csgobrowser.container.ServersContainer;
import com.skarbo.csgobrowser.container.ServersContainer.Server;
import com.skarbo.csgobrowser.exception.InvalidPageException;
import com.skarbo.csgobrowser.handler.ControlHandler;
import com.skarbo.csgobrowser.handler.control.ControlHandlerResult;
import com.skarbo.csgobrowser.handler.control.asynctask.ServerControlHandlerAsyncTask;
import com.skarbo.csgobrowser.utils.RestClient;
import com.skarbo.csgobrowser.utils.RestClient.Response;
import com.skarbo.csgobrowser.utils.Utils;
import com.skarbo.csgobrowser.utils.Utils.Country;

public class LeetwayServerControlHandlerAsyncTask extends ServerControlHandlerAsyncTask<LeetwayServiceConfig> {

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
		STATUS.put("Waiting", ServersContainer.Server.Status.Waiting);
		STATUS.put("Live", ServersContainer.Server.Status.Live);
	}

	public LeetwayServerControlHandlerAsyncTask(Server server, ControlHandler controlHandler,
			ControlHandlerResult<ServersContainer> handlerResult) {
		super(server, controlHandler, handlerResult);
	}

	// ... DO

	@Override
	public ServersContainer doHandleServer() throws Exception {
		ServersContainer serverContainer = new ServersContainer();
		ServersContainer.Server server = new ServersContainer.Server();
		server.serviceId = getServiceConfig().id;
		server.id = getServer().id;
		serverContainer.addServer(server);

		if (Utils.isEmpty(getServer().matchId)) {
			throw new MatchidNotGivenException();
		}

		// SERVER

		RestClient serverRestClient = createRestClient(getServiceConfig(), createUrlServer());
		Response serverResponse = serverRestClient.execute(RestClient.RequestMethod.GET);

		if (!isPageServer(serverResponse))
			throw new InvalidPageException();

		doParseServer(serverResponse, server);

		publishProgress(serverContainer);

		// /SERVER

		// PLAYERS

		RestClient serverPlayersRestClient = createRestClient(getServiceConfig(), createUrlServerPlayers());
		Response serverPlayersResponse = serverPlayersRestClient.execute(RestClient.RequestMethod.GET);

		if (!isPageServerPlayers(serverPlayersResponse))
			throw new InvalidPageException();

		doParseServerPlayers(serverPlayersResponse, server);

		// /PLAYERS

		return serverContainer;
	}

	public void doParseServer(Response response, final Server server) {
		ServersContainer.Server serverTemp = new ServersContainer.Server();

		// Match id
		Matcher matchidMatcher = Pattern.compile(LeetwayServiceConfig.LeetwayPages.LeetwayServer.REGEX_SERVER_MATCHID,
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(response.getResponse());
		if (matchidMatcher.find()) {
			serverTemp.matchId = matchidMatcher.group(1);
		}

		// Info
		Matcher infoMatcher = Pattern.compile(LeetwayServiceConfig.LeetwayPages.LeetwayServer.REGEX_SERVER_INFO,
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(response.getResponse());
		if (infoMatcher.find()) {
			serverTemp.map = Utils.trimWhitespace(infoMatcher.group(1));
			serverTemp.country = Country.US;
			serverTemp.location = Utils.trimWhitespace(infoMatcher.group(3));
			serverTemp.playersCurrent = Utils.parseInt(Utils.trimWhitespace(infoMatcher.group(4)));
			serverTemp.playersMax = Utils.parseInt(Utils.trimWhitespace(infoMatcher.group(5)));
			serverTemp.status = ServersContainer.Server.Status.Available;
			serverTemp.scoreHome = Utils.parseInt(Utils.trimWhitespace(infoMatcher.group(7)), -1);
			serverTemp.scoreAway = Utils.parseInt(Utils.trimWhitespace(infoMatcher.group(8)), -1);

			// Name
			if (!Utils.isEmpty(serverTemp.id) && !Utils.isEmpty(serverTemp.location))
				serverTemp.name = String.format("%s %s", serverTemp.location, serverTemp.id);

			// Countries
			if (COUNTRIES.containsKey(Utils.trimWhitespace(infoMatcher.group(2)))) {
				serverTemp.country = COUNTRIES.get(Utils.trimWhitespace(infoMatcher.group(2)));
			}

			// Status
			if (STATUS.containsKey(Utils.trimWhitespace(infoMatcher.group(6)))) {
				serverTemp.status = STATUS.get(Utils.trimWhitespace(infoMatcher.group(6)));
			}
		}

		server.merge(serverTemp);
	}

	public void doParseServerPlayers(Response response, Server server) {
		List<PlayersContainer.Player> players = new ArrayList<PlayersContainer.Player>();

		Matcher playersMatcher = Pattern.compile(LeetwayServiceConfig.LeetwayPages.LeetwayServer.REGEX_SERVER_PLAYERS,
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(response.getResponse());
		int i = 0;
		while (playersMatcher.find()) {
			PlayersContainer.Player player = doParseServerPlayer(i++, playersMatcher);
			if (player != null)
				players.add(player);
		}

		for (Player player : players) {
			server.playersContainer.addPlayer(player);
		}
	}

	public PlayersContainer.Player doParseServerPlayer(int i, Matcher playerMatcher) {
		PlayersContainer.Player player = new PlayersContainer.Player();

		Matcher playerIdImageName = Pattern.compile(
				LeetwayServiceConfig.LeetwayPages.LeetwayServer.REGEX_SERVER_PLAYER_ID_IMAGE_NAME,
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(Utils.trimWhitespace(playerMatcher.group(1)));

		if (playerIdImageName.find()) {
			player.serviceId = getServiceConfig().id;
			player.id = Utils.trimWhitespace(playerIdImageName.group(1));
			player.image = Utils.unescapeJava(Utils.trimWhitespace(playerIdImageName.group(2)));
			player.name = Utils.unescapeJava(Utils.trimWhitespace(playerIdImageName.group(3)));
			player.stats.stats.put(Stats.Stat.playerGamingRating, Utils.trimWhitespace(playerMatcher.group(2)));
			player.stats.stats.put(Stats.Stat.reputation, Utils.trimWhitespace(playerMatcher.group(3)));

			if (!player.image.startsWith("http://")) {
				player.image = String.format("http://leetway.com/%s", player.image);
			}

			if (i < (LeetwayServiceConfig.LeetwayPages.LeetwayServer.PLAYERS_MAX / 2))
				player.team = PlayersContainer.Player.Team.Home;
			else
				player.team = PlayersContainer.Player.Team.Away;
			return player;
		} else
			Log.w(TAG, "doParseServerPlayer: " + i + ", Player matcher does not match");
		return null;
	}

	// ... /DO

	public String createUrlServer() {
		return LeetwayServiceConfig.LeetwayPages.LeetwayServer.PAGE_SERVER.page.replace("%matchid%",
				getServer().matchId);
	}

	public String createUrlServerPlayers() {
		return LeetwayServiceConfig.LeetwayPages.LeetwayServer.PAGE_SERVER_PLAYERS.page.replace("%matchid%",
				getServer().matchId);
	}

	public boolean isPageMatch(Response response) {
		return isPage(LeetwayServiceConfig.LeetwayPages.LeetwayServer.REGEX_ISPAGE_MATCH, response);
	}

	public boolean isPageServer(Response response) {
		return isPage(LeetwayServiceConfig.LeetwayPages.LeetwayServer.PAGE_SERVER.isPage, response);
	}

	public boolean isPageServerPlayers(Response response) {
		return isPage(LeetwayServiceConfig.LeetwayPages.LeetwayServer.PAGE_SERVER_PLAYERS.isPage, response);
	}

}
