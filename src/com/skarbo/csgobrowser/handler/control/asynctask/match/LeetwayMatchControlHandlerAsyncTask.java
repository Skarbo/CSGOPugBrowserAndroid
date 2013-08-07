package com.skarbo.csgobrowser.handler.control.asynctask.match;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

import com.skarbo.csgobrowser.config.service.LeetwayServiceConfig;
import com.skarbo.csgobrowser.container.MatchesContainer;
import com.skarbo.csgobrowser.container.MatchesContainer.Match;
import com.skarbo.csgobrowser.container.PlayersContainer;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats.Stat;
import com.skarbo.csgobrowser.exception.InvalidPageException;
import com.skarbo.csgobrowser.handler.ControlHandler;
import com.skarbo.csgobrowser.handler.control.ControlHandlerResult;
import com.skarbo.csgobrowser.handler.control.asynctask.MatchControlHandlerAsyncTask;
import com.skarbo.csgobrowser.utils.RestClient;
import com.skarbo.csgobrowser.utils.RestClient.Response;
import com.skarbo.csgobrowser.utils.Utils;
import com.skarbo.csgobrowser.utils.Utils.Country;

public class LeetwayMatchControlHandlerAsyncTask extends MatchControlHandlerAsyncTask<LeetwayServiceConfig> {

	private static Map<String, MatchesContainer.Match.Status> STATUS;
	{
		STATUS = new HashMap<String, MatchesContainer.Match.Status>();
		STATUS.put("complete", MatchesContainer.Match.Status.Completed);
	}

	public static Map<String, Country> COUNTRIES;
	{
		COUNTRIES = new HashMap<String, Country>();
		COUNTRIES.put("de", Country.DE);
		COUNTRIES.put("se", Country.SE);
		COUNTRIES.put("us", Country.US);
		COUNTRIES.put("gb", Country.GB);
	}

	public LeetwayMatchControlHandlerAsyncTask(String matchId, ControlHandler controlHandler,
			ControlHandlerResult<MatchesContainer> handlerResult) {
		super(LeetwayServiceConfig.SERVICE_ID, matchId, controlHandler, handlerResult);
	}

	// ... GET

	public LeetwayServiceConfig.LeetwayPages.LeetwayMatch getLeetwayMatchConfig() {
		return (LeetwayServiceConfig.LeetwayPages.LeetwayMatch) getServiceConfig().pages.match;
	}

	// ... /GET

	// ... DO

	@Override
	public MatchesContainer doHandleMatch() throws Exception {
		MatchesContainer matchesContainer = new MatchesContainer();
		MatchesContainer.Match match = new MatchesContainer.Match();
		matchesContainer.addMatch(match);

		match.serviceId = getServiceId();

		// MATCH

		RestClient matchRestClient = createRestClient(getServiceConfig(), createUrlMatch());
		Response matchResponse = matchRestClient.execute(RestClient.RequestMethod.GET);

		if (!isPageMatch(matchResponse))
			throw new InvalidPageException();

		doParseMatchInfo(matchResponse, match);
		doParseMatchStats(matchResponse, match);

		// /MATCH

		publishProgress(matchesContainer);

		// ACHIEVEMENTS

		RestClient matchAchievementsRestClient = createRestClient(getServiceConfig(), createUrlMatchAchievements());
		Response matchAchievementsResponse = matchAchievementsRestClient.execute(RestClient.RequestMethod.GET);

		if (!isPageMatchAchievements(matchAchievementsResponse))
			throw new InvalidPageException();

		doParseMatchInfo(matchAchievementsResponse, match);
		doParseMatchStatsAchievementsPlayers(matchAchievementsResponse, match);

		// /ACHIEVEMENTS

		return matchesContainer;
	}

	private void doParseMatchStatsAchievementsPlayers(Response response, Match match) {
		Matcher matchAchievementsPlayersMatcher = Pattern.compile(
				getLeetwayMatchConfig().regexMatchStats_PlayerAchievment, Pattern.DOTALL | Pattern.CASE_INSENSITIVE)
				.matcher(response.getResponse());

		int playersCount = 0;
		while (matchAchievementsPlayersMatcher.find()) {
			PlayersContainer.Player player = new PlayersContainer.Player();

			player.serviceId = match.serviceId;
			player.id = Utils.trimWhitespace(matchAchievementsPlayersMatcher.group(1));
			player.name = Utils.trimWhitespace(matchAchievementsPlayersMatcher.group(2));
			player.stats.stats.put(Stat.kill2, Utils.trimWhitespace(matchAchievementsPlayersMatcher.group(3)));
			player.stats.stats.put(Stat.kill3, Utils.trimWhitespace(matchAchievementsPlayersMatcher.group(4)));
			player.stats.stats.put(Stat.kill4, Utils.trimWhitespace(matchAchievementsPlayersMatcher.group(5)));
			player.stats.stats.put(Stat.kill5, Utils.trimWhitespace(matchAchievementsPlayersMatcher.group(6)));
			player.stats.stats.put(Stat.oneV1, Utils.trimWhitespace(matchAchievementsPlayersMatcher.group(7)));
			player.stats.stats.put(Stat.oneV2, Utils.trimWhitespace(matchAchievementsPlayersMatcher.group(8)));
			player.stats.stats.put(Stat.oneV3, Utils.trimWhitespace(matchAchievementsPlayersMatcher.group(9)));
			player.stats.stats.put(Stat.oneV4, Utils.trimWhitespace(matchAchievementsPlayersMatcher.group(10)));
			player.stats.stats.put(Stat.oneV5, Utils.trimWhitespace(matchAchievementsPlayersMatcher.group(11)));

			// Log.d(TAG, "Adding player: " + player.toString());
			// Log.d(TAG, "Adding player stats: " + player.stats.toString());
			match.playersContainer.addPlayer(player);
			playersCount++;
		}

		if (playersCount == 0)
			Log.w(TAG, "doParseMatchStatsAchievementsPlayers: Match achievements players does not match");
	}

	private void doParseMatchInfo(Response response, Match match) {
		Matcher matchInfoMatcher = Pattern.compile(getLeetwayMatchConfig().regexMatchInfo,
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(response.getResponse());
		if (matchInfoMatcher.find()) {
			match.id = Utils.trimWhitespace(matchInfoMatcher.group(1));
			match.map = Utils.trimWhitespace(matchInfoMatcher.group(2));
			match.location = Utils.trimWhitespace(matchInfoMatcher.group(4));
			match.scoreHome.score = Utils.parseInt(matchInfoMatcher.group(7));
			match.scoreAway.score = Utils.parseInt(matchInfoMatcher.group(8));

			// Status
			String status = Utils.trimWhitespace(matchInfoMatcher.group(6)).toLowerCase();
			if (STATUS.containsKey(status)) {
				match.status = STATUS.get(status);
			}

			// Country
			String country = Utils.trimWhitespace(matchInfoMatcher.group(3)).toLowerCase();
			if (COUNTRIES.containsKey(country)) {
				match.country = COUNTRIES.get(country);
			}

			// Date
			String date = Utils.trimWhitespace(matchInfoMatcher.group(5));
			SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyy");
			try {
				match.date = dateFormat.parse(date);
			} catch (ParseException e) {
				match.date = null;
			}
		} else
			Log.w(TAG, "doParseMatchInfo: Match info does not match");
	}

	private void doParseMatchStats(Response response, Match match) {
		Matcher matchStatsTeamMatcher = Pattern.compile(getLeetwayMatchConfig().regexMatchStats_Team,
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(response.getResponse());

		int teamCount = 0;
		while (matchStatsTeamMatcher.find()) {
			PlayersContainer.Player.Team team = teamCount == 0 ? PlayersContainer.Player.Team.Home
					: PlayersContainer.Player.Team.Away;

			// if (teamCount == 0)
			// match.scoreHome.playerGamingRating =
			// Utils.trimWhitespace(matchStatsTeamMatcher.group(1));
			// else
			// match.scoreAway.playerGamingRating =
			// Utils.trimWhitespace(matchStatsTeamMatcher.group(1));

			Matcher matchStatsTeamPlayer = Pattern.compile(getLeetwayMatchConfig().regexMatchStats_Player,
					Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(matchStatsTeamMatcher.group(1));
			int playerCount = 0;
			while (matchStatsTeamPlayer.find()) {
				PlayersContainer.Player player = new PlayersContainer.Player();

				player.serviceId = match.serviceId;
				player.team = team;
				player.id = Utils.trimWhitespace(matchStatsTeamPlayer.group(1));
				player.image = Utils.trimWhitespace(matchStatsTeamPlayer.group(2));
				player.name = Utils.trimWhitespace(matchStatsTeamPlayer.group(3));
				player.stats.stats.put(Stats.Stat.frags, Utils.trimWhitespace(matchStatsTeamPlayer.group(4)));
				player.stats.stats.put(Stats.Stat.assists, Utils.trimWhitespace(matchStatsTeamPlayer.group(5)));
				player.stats.stats.put(Stats.Stat.deaths, Utils.trimWhitespace(matchStatsTeamPlayer.group(6)));
				player.stats.stats.put(Stats.Stat.suicides, Utils.trimWhitespace(matchStatsTeamPlayer.group(7)));
				player.stats.stats.put(Stats.Stat.headshots, Utils.trimWhitespace(matchStatsTeamPlayer.group(8)));
				player.stats.stats.put(Stats.Stat.headshotPercentage,
						Utils.trimWhitespace(matchStatsTeamPlayer.group(9)));
				player.stats.stats.put(Stats.Stat.teamKills, Utils.trimWhitespace(matchStatsTeamPlayer.group(10)));
				player.stats.stats.put(Stats.Stat.bombPlants, Utils.trimWhitespace(matchStatsTeamPlayer.group(11)));
				player.stats.stats.put(Stats.Stat.bombDefuse, Utils.trimWhitespace(matchStatsTeamPlayer.group(12)));
				player.stats.stats.put(Stats.Stat.killDeathRatio, Utils.trimWhitespace(matchStatsTeamPlayer.group(13)));
				player.stats.stats.put(Stats.Stat.damagePrRound, Utils.trimWhitespace(matchStatsTeamPlayer.group(14)));
				player.stats.stats.put(Stats.Stat.roundsPlayed, Utils.trimWhitespace(matchStatsTeamPlayer.group(15)));
				player.stats.stats.put(Stats.Stat.points, Utils.trimWhitespace(matchStatsTeamPlayer.group(16)));
				if (!player.image.startsWith("http://")) {
					player.image = String.format("http://leetway.com/%s", player.image);
				}

				match.playersContainer.addPlayer(player);
				playerCount++;
			}
			if (playerCount == 0)
				Log.w(TAG, "doParseMatchStats: No player matches");
			teamCount++;
		}
		if (teamCount == 0)
			Log.w(TAG, "doParseMatchStats: No team matches");
	}

	// ... /DO

	// ... IS

	public boolean isPageMatch(Response response) {
		return isPage(getLeetwayMatchConfig().pageMatch.isPage, response);
	}

	public boolean isPageMatchAchievements(Response response) {
		return isPage(getLeetwayMatchConfig().pageMatchAchievments.isPage, response);
	}

	// ... /IS

	// ... CREATE

	private String createUrlMatch() {
		return String.format(getLeetwayMatchConfig().pageMatch.page, getMatchId());
	}

	private String createUrlMatchAchievements() {
		return String.format(getLeetwayMatchConfig().pageMatchAchievments.page, getMatchId());
	}

	// ... /CREATE

}
