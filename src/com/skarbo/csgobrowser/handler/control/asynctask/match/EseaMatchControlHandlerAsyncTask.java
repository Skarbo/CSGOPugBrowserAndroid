package com.skarbo.csgobrowser.handler.control.asynctask.match;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

import com.skarbo.csgobrowser.config.service.EseaServiceConfig;
import com.skarbo.csgobrowser.container.MatchesContainer;
import com.skarbo.csgobrowser.container.MatchesContainer.Match;
import com.skarbo.csgobrowser.container.PlayersContainer;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats;
import com.skarbo.csgobrowser.exception.InvalidPageException;
import com.skarbo.csgobrowser.handler.ControlHandler;
import com.skarbo.csgobrowser.handler.control.ControlHandlerResult;
import com.skarbo.csgobrowser.handler.control.asynctask.MatchControlHandlerAsyncTask;
import com.skarbo.csgobrowser.utils.RestClient;
import com.skarbo.csgobrowser.utils.RestClient.Response;
import com.skarbo.csgobrowser.utils.Utils;

public class EseaMatchControlHandlerAsyncTask extends MatchControlHandlerAsyncTask<EseaServiceConfig> {

	private static Map<String, MatchesContainer.Match.Status> STATUS;
	{
		STATUS = new HashMap<String, MatchesContainer.Match.Status>();
		STATUS.put("live", MatchesContainer.Match.Status.Live);
		STATUS.put("completed", MatchesContainer.Match.Status.Completed);
	}

	public EseaMatchControlHandlerAsyncTask(String matchId, ControlHandler controlHandler,
			ControlHandlerResult<MatchesContainer> handlerResult) {
		super(EseaServiceConfig.SERVICE_ID, matchId, controlHandler, handlerResult);
	}

	// ... GET

	public EseaServiceConfig.EseaPages.EseaMatch getEseaMatchConfig() {
		return (EseaServiceConfig.EseaPages.EseaMatch) getServiceConfig().pages.match;
	}

	// ... /GET

	// ... DO

	@Override
	public MatchesContainer doHandleMatch() throws Exception {
		RestClient matchRestClient = createRestClient(getServiceConfig(), createUrlMatch());
		Response matchResponse = matchRestClient.execute(RestClient.RequestMethod.GET);

		MatchesContainer matchesContainer = new MatchesContainer();

		if (!isPageMatch(matchResponse))
			throw new InvalidPageException();

		doParseMatch(matchResponse, matchesContainer);

		return matchesContainer;
	}

	public void doParseMatch(Response response, MatchesContainer matchesContainer) {
		MatchesContainer.Match match = new MatchesContainer.Match();

		match.serviceId = getServiceId();

		// INFO

		doParseMatchInfo(response, match, matchesContainer);

		// /INFO

		// SCORES

		doParseMatchScores(response, match, matchesContainer);

		// /SCORES

		// STATS

		doParseMatchStats(response, match, matchesContainer);

		// /STATS

		matchesContainer.addMatch(match);
	}

	private void doParseMatchStats(Response response, Match match, MatchesContainer matchesContainer) {
		Matcher matchStatsTeamMatcher = Pattern.compile(getEseaMatchConfig().regexMatchStatsTeam,
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(response.getResponse());

		int teamCount = 0;
		while (matchStatsTeamMatcher.find()) {
			PlayersContainer.Player.Team team = teamCount == 0 ? PlayersContainer.Player.Team.Home
					: PlayersContainer.Player.Team.Away;

			Matcher matchStatsTeamPlayer = Pattern.compile(getEseaMatchConfig().regexMatchStatsPlayer,
					Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(matchStatsTeamMatcher.group(1));
			while (matchStatsTeamPlayer.find()) {
				PlayersContainer.Player player = new PlayersContainer.Player();

				player.serviceId = match.serviceId;
				player.team = team;
				player.image = Utils.trimWhitespace(matchStatsTeamPlayer.group(1));
				player.id = Utils.trimWhitespace(matchStatsTeamPlayer.group(2));
				player.name = Utils.trimWhitespace(matchStatsTeamPlayer.group(3));
				player.stats.stats.put(Stats.Stat.roundWinShares, Utils.trimWhitespace(matchStatsTeamPlayer.group(4)));
				player.stats.stats.put(Stats.Stat.frags, Utils.trimWhitespace(matchStatsTeamPlayer.group(5)));
				player.stats.stats.put(Stats.Stat.assists, Utils.trimWhitespace(matchStatsTeamPlayer.group(6)));
				player.stats.stats.put(Stats.Stat.deaths, Utils.trimWhitespace(matchStatsTeamPlayer.group(7)));
				player.stats.stats.put(Stats.Stat.bombPlants, Utils.trimWhitespace(matchStatsTeamPlayer.group(8)));
				player.stats.stats.put(Stats.Stat.bombDefuse, Utils.trimWhitespace(matchStatsTeamPlayer.group(9)));
				player.stats.stats.put(Stats.Stat.kill2, Utils.trimWhitespace(matchStatsTeamPlayer.group(10)));
				player.stats.stats.put(Stats.Stat.kill3, Utils.trimWhitespace(matchStatsTeamPlayer.group(11)));
				player.stats.stats.put(Stats.Stat.kill4, Utils.trimWhitespace(matchStatsTeamPlayer.group(12)));
				player.stats.stats.put(Stats.Stat.kill5, Utils.trimWhitespace(matchStatsTeamPlayer.group(13)));
				player.stats.stats.put(Stats.Stat.oneV1, Utils.trimWhitespace(matchStatsTeamPlayer.group(14)));
				player.stats.stats.put(Stats.Stat.oneV2, Utils.trimWhitespace(matchStatsTeamPlayer.group(15)));
				player.stats.stats.put(Stats.Stat.headshotPercentage,
						Utils.trimWhitespace(matchStatsTeamPlayer.group(16)));
				player.stats.stats.put(Stats.Stat.roundsPlayed, Utils.trimWhitespace(matchStatsTeamPlayer.group(17)));
				player.stats.stats.put(Stats.Stat.damagePrRound, Utils.trimWhitespace(matchStatsTeamPlayer.group(18)));
				player.stats.stats.put(Stats.Stat.fragsPrRound, Utils.trimWhitespace(matchStatsTeamPlayer.group(19)));

				match.playersContainer.addPlayer(player);
			}

			teamCount++;
		}
		if (teamCount == 0)
			Log.w(TAG, "doParseMatchStats: No team matches");
	}

	private void doParseMatchScores(Response response, final MatchesContainer.Match match,
			MatchesContainer matchesContainer) {
		Matcher matchScoresMatcher = Pattern.compile(getEseaMatchConfig().regexMatchScores,
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(response.getResponse());
		if (matchScoresMatcher.find()) {
			match.scoreHome.halfT = Utils.parseInt(matchScoresMatcher.group(1), -1);
			match.scoreHome.score = Utils.parseInt(matchScoresMatcher.group(3), -1);
			match.scoreHome.frags = Utils.parseInt(matchScoresMatcher.group(4), -1);
			match.scoreHome.deaths = Utils.parseInt(matchScoresMatcher.group(5), -1);

			match.scoreAway.halfCT = Utils.parseInt(matchScoresMatcher.group(6), -1);
			match.scoreAway.score = Utils.parseInt(matchScoresMatcher.group(8), -1);
			match.scoreAway.frags = Utils.parseInt(matchScoresMatcher.group(9), -1);
			match.scoreAway.deaths = Utils.parseInt(matchScoresMatcher.group(10), -1);

			match.scoreHome.halfCT = Utils.parseInt(matchScoresMatcher.group(2).replaceAll("(?i)[^\\d]+", ""), -1);
			match.scoreAway.halfT = Utils.parseInt(matchScoresMatcher.group(7).replaceAll("(?i)[^\\d]+", ""), -1);
		} else
			Log.w(TAG, "doParseMatch: Score does not match");
	}

	private void doParseMatchInfo(Response response, final MatchesContainer.Match match,
			MatchesContainer matchesContainer) {
		// Id/status
		Matcher matchInfoIdStatusMatcher = Pattern.compile(getEseaMatchConfig().regexMatchInfo_IdStatus,
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(response.getResponse());
		if (matchInfoIdStatusMatcher.find()) {
			match.id = Utils.trimWhitespace(matchInfoIdStatusMatcher.group(1));

			// Status
			String status = Utils.trimWhitespace(matchInfoIdStatusMatcher.group(2)).toLowerCase();
			if (STATUS.containsKey(status)) {
				match.status = STATUS.get(status);
			}
		} else
			Log.w(TAG, "doParseMatch: Id and status does not match");

		// Date/time/map
		Matcher matchInfoDateTimeMapMatcher = Pattern.compile(getEseaMatchConfig().regexMatchInfo_DateTimeMap,
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(response.getResponse());
		if (matchInfoDateTimeMapMatcher.find()) {
			match.time = Utils.trimWhitespace(matchInfoDateTimeMapMatcher.group(2));
			match.map = Utils.trimWhitespace(matchInfoDateTimeMapMatcher.group(3));

			// Date
			String date = Utils.trimWhitespace(matchInfoDateTimeMapMatcher.group(1));
			SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yy h:mma");
			try {
				match.date = dateFormat.parse(date);
			} catch (ParseException e) {
				match.date = null;
			}
		} else
			Log.w(TAG, "doParseMatch: Date, time and map does not match");
	}

	// ... /DO

	// ... IS

	public boolean isPageMatch(Response response) {
		return isPage(getEseaMatchConfig().pageMatch.isPage, response);
	}

	// ... /IS

	// ... CREATE

	public String createUrlMatch() {
		return String.format(getEseaMatchConfig().pageMatch.page, getMatchId());
	}

	// ... /CREATE

}
