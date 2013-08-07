package com.skarbo.csgobrowser.handler.control.asynctask.server;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

import com.skarbo.csgobrowser.config.service.EseaServiceConfig;
import com.skarbo.csgobrowser.container.PlayersContainer;
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

public class EseaServerControlHandlerAsyncTask extends ServerControlHandlerAsyncTask<EseaServiceConfig> {

	private static Map<String, Country> COUNTRIES;
	{
		COUNTRIES = new HashMap<String, Country>();
		COUNTRIES.put("frankfurt", Country.DE);
		COUNTRIES.put("roubaix", Country.FR);
		COUNTRIES.put("london", Country.GB);
		COUNTRIES.put("stockholm", Country.SE);
		COUNTRIES.put("madrid", Country.ES);
	}

	public EseaServerControlHandlerAsyncTask(Server server, ControlHandler controlHandler,
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

		// SERVER

		RestClient serverRestClient = createRestClient(getServiceConfig(), createUrlServer());
		Response serverResponse = serverRestClient.execute(RestClient.RequestMethod.GET);

		if (!isPageServer(serverResponse))
			throw new InvalidPageException();

		doParseServerInfo(serverResponse, server, serverContainer);

		// /SERVER

		// PLAYER MATCH

		if (Utils.isEmpty(server.matchId)) {
			publishProgress(serverContainer);

			if (!server.playersContainer.players.isEmpty() && server.status == ServersContainer.Server.Status.Live) {
				String playerId = server.playersContainer.players.get(0).id;

				if (Utils.isEmpty(playerId))
					throw new Exception("Player id is empty");

				RestClient playerMatchRestClient = createRestClient(getServiceConfig(), createUrlPlayerMatch(playerId));
				Response playerMatchResponse = playerMatchRestClient.execute(RestClient.RequestMethod.GET);

				if (!isPagePlayerMatch(playerMatchResponse))
					throw new Exception();

				doParsePlayerMatch(playerMatchResponse, server, serverContainer);
			} else
				Log.w(TAG, "doHandleServer: No players or not live");

			// publishProgress(serverContainer);

		} else
			Log.d(TAG, "doHandleServer: Skipping player match");

		// /PLAYER MATCH

		// // MATCH
		//
		// if (!Utils.isEmpty(serverContainer.server.matchId)) {
		// EseaMatchControlHandlerAsyncTask eseaMatchControlHandlerAsyncTask =
		// new EseaMatchControlHandlerAsyncTask(
		// getServer().serviceId, serverContainer.server.matchId,
		// getControlHandler(), null);
		//
		// MatchesContainer matchesContainer =
		// eseaMatchControlHandlerAsyncTask.doHandleMatch();
		// if (matchesContainer != null && !matchesContainer.matches.isEmpty())
		// {
		// Match match = matchesContainer.matches.values().iterator().next();
		// if (match != null) {
		// if (match.scoreHome.score > -1)
		// serverContainer.server.scoreHome = match.scoreHome.score;
		// if (match.scoreAway.score > -1)
		// serverContainer.server.scoreAway = match.scoreAway.score;
		// if (!Utils.isEmpty(match.map))
		// serverContainer.server.map = match.map;
		// if (!Utils.isEmpty(match.id))
		// serverContainer.server.matchId = match.id;
		//
		// serverContainer.match = match;
		// } else
		// Log.w(TAG, "doHandleServer: Match is null");
		// } else
		// Log.w(TAG, "doHandleServer: MatchesContainer is null or empty");
		// } else
		// Log.w(TAG, "doHandleServer: Match id is empty");
		//
		// // /MATCH

		// Retrieve Match details
		if (!Utils.isEmpty(server.matchId))
			getControlHandler().doMatch(server.serviceId, server.matchId);

		return serverContainer;
	}

	public void doParseServerInfo(Response response, final Server server, ServersContainer serverContainer) {
		ServersContainer.Server serverTemp = new ServersContainer.Server();

		// Info
		Matcher infoMatcher = Pattern.compile(EseaServiceConfig.EseaPages.EseaServer.REGEX_INFO,
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(response.getResponse());
		if (infoMatcher.find()) {
			serverTemp.name = Utils.trimWhitespace(infoMatcher.group(1));
			serverTemp.ipAddress = Utils.trimWhitespace(infoMatcher.group(2));
			serverTemp.playersCurrent = Utils.parseInt(Utils.trimWhitespace(infoMatcher.group(3)));
			serverTemp.playersMax = EseaServiceConfig.EseaPages.EseaServer.PLAYERS_MAX;

			// Country
			Matcher countryMatcher = Pattern.compile(EseaServiceConfig.EseaPages.EseaServer.REGEX_INFO_COUNTRY)
					.matcher(serverTemp.name);
			if (countryMatcher.find()) {
				String country = countryMatcher.group(1).toLowerCase();
				if (COUNTRIES.containsKey(country)) {
					serverTemp.country = COUNTRIES.get(country);
				}
			}

			// Location
			serverTemp.location = serverTemp.name.replaceAll("(?si)\\s\\d+", "").trim();

			// Status
			if (serverTemp.playersCurrent >= EseaServiceConfig.EseaPages.EseaServer.PLAYERS_MAX) {
				serverTemp.status = ServersContainer.Server.Status.Live;
			} else if (serverTemp.playersCurrent > 0) {
				serverTemp.status = ServersContainer.Server.Status.Waiting;
			} else {
				serverTemp.status = ServersContainer.Server.Status.Available;
			}
		} else
			Log.w(TAG, "doParseServerInfo: No match for info");

		// Players
		Matcher playerMatcher = Pattern.compile(EseaServiceConfig.EseaPages.EseaServer.REGEX_INFO_PLAYERS,
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(response.getResponse());
		int count = 0;
		while (playerMatcher.find()) {
			PlayersContainer.Player player = new PlayersContainer.Player();

			player.serviceId = getServiceConfig().id;
			player.image = Utils.trimWhitespace(playerMatcher.group(1));
			player.id = Utils.trimWhitespace(playerMatcher.group(2));
			player.name = Utils.trimWhitespace(playerMatcher.group(3));
			player.stats.stats.put(Stats.Stat.time, Utils.trimWhitespace(playerMatcher.group(4)));

			if (count++ < EseaServiceConfig.EseaPages.EseaServer.PLAYERS_MAX / 2)
				player.team = PlayersContainer.Player.Team.Home;
			else
				player.team = PlayersContainer.Player.Team.Away;

			serverTemp.playersContainer.addPlayer(player);
		}
		if (count == 0)
			Log.w(TAG, "doParseServerInfo: No player matches");

		server.merge(serverTemp);
	}

	public void doParsePlayerMatch(Response response, final Server server, ServersContainer serverContainer) {
		ServersContainer.Server serverTemp = new ServersContainer.Server();

		Matcher playerMatchMatcher = Pattern.compile(EseaServiceConfig.EseaPages.EseaServer.REGEX_PLAYER_MATCH,
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(response.getResponse());
		if (playerMatchMatcher.find()) {
			serverTemp.matchId = Utils.trimWhitespace(playerMatchMatcher.group(1));
			serverTemp.map = Utils.trimWhitespace(playerMatchMatcher.group(2));
		} else
			Log.w(TAG, "doParsePlayerMatch: No match for player");

		server.merge(serverTemp);
	}

	// ... /DO

	public String createUrlServer() {
		return String.format(EseaServiceConfig.EseaPages.EseaServer.PAGE_SERVER.page, getServer().id);
	}

	public String createUrlPlayerMatch(String playerId) {
		return String.format(EseaServiceConfig.EseaPages.EseaServer.PAGE_PLAYER_MATCH.page, playerId);
	}

	public boolean isPageServer(Response response) {
		return isPage(EseaServiceConfig.EseaPages.EseaServer.REGEX_ISPAGE_SERVER, response);
	}

	public boolean isPagePlayerMatch(Response response) {
		return isPage(EseaServiceConfig.EseaPages.EseaServer.REGEX_ISPAGE_PLAYER_MATCH, response);
	}

}
