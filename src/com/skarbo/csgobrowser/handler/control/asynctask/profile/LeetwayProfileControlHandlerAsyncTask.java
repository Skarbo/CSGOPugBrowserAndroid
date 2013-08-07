package com.skarbo.csgobrowser.handler.control.asynctask.profile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;

import android.util.Log;

import com.skarbo.csgobrowser.config.service.LeetwayServiceConfig;
import com.skarbo.csgobrowser.config.service.LeetwayServiceConfig.LeetwayPages.LeetwayProfile;
import com.skarbo.csgobrowser.container.MatchesContainer;
import com.skarbo.csgobrowser.container.PlayersContainer;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats.Stat;
import com.skarbo.csgobrowser.container.ProfilesContainer;
import com.skarbo.csgobrowser.container.ProfilesContainer.Profile;
import com.skarbo.csgobrowser.exception.InvalidPageException;
import com.skarbo.csgobrowser.exception.InvalidParseException;
import com.skarbo.csgobrowser.handler.ControlHandler;
import com.skarbo.csgobrowser.handler.control.ControlHandlerResult;
import com.skarbo.csgobrowser.handler.control.asynctask.ProfileControlHandlerAsyncTask;
import com.skarbo.csgobrowser.utils.RestClient;
import com.skarbo.csgobrowser.utils.RestClient.Response;
import com.skarbo.csgobrowser.utils.Utils;

public class LeetwayProfileControlHandlerAsyncTask extends ProfileControlHandlerAsyncTask<LeetwayServiceConfig> {

	public LeetwayProfileControlHandlerAsyncTask(String profileId, ControlHandler controlHandler,
			ControlHandlerResult<ProfilesContainer> handlerResult) {
		super(LeetwayServiceConfig.SERVICE_ID, profileId, controlHandler, handlerResult);
	}

	// ... GET

	private LeetwayProfile getLeetwayProfileConfig() {
		return (LeetwayProfile) getServiceConfig().pages.profile;
	}

	// ... /GET

	// ... DO

	@Override
	public ProfilesContainer doHandleProfile() throws Exception {

		ProfilesContainer profilesContainer = new ProfilesContainer();
		ProfilesContainer.Profile profile = new ProfilesContainer.Profile();
		profilesContainer.addProfile(profile);

		profile.serviceId = getServiceId();

		// INFO

		RestClient profileInfoRestClient = createRestClient(getServiceConfig(), createUrlProfileInfo());
		Response profileInfoResponse = profileInfoRestClient.execute(RestClient.RequestMethod.GET);

		if (!isPageProfileInfo(profileInfoResponse))
			throw new InvalidPageException();

		doParseProfileInfo(profileInfoResponse, profile);
		doParseProfileInfoStats(profileInfoResponse, profile);
		doParseProfileInfoMatches(profileInfoResponse, profile, profilesContainer);

		// /INFO

		publishProgress(profilesContainer);

		// WEAPONS

		RestClient profileWeaponsRestClient = createRestClient(getServiceConfig(), createUrlProfileWeapons());
		Response profileWeaponsResponse = profileWeaponsRestClient.execute(RestClient.RequestMethod.GET);

		if (!isPageProfileWeapons(profileWeaponsResponse))
			throw new InvalidPageException();

		doParseProfileWeapons(profileWeaponsResponse, profile);

		// /WEAPONS

		return profilesContainer;
	}

	public void doParseProfileInfo(Response response, ProfilesContainer.Profile profile) throws InvalidParseException {
		Matcher profileInfoMatcher = getLeetwayProfileConfig().regexProfileInfo.matcher(response.getResponse());
		if (profileInfoMatcher.find()) {
			profile.id = Utils.trimWhitespace(profileInfoMatcher.group(1));
			profile.image = Utils.trimWhitespace(profileInfoMatcher.group(2));
			profile.nickname = Utils.trimWhitespace(profileInfoMatcher.group(3));
			profile.registered = Utils.trimWhitespace(profileInfoMatcher.group(4));
			profile.signedInLast = Utils.trimWhitespace(profileInfoMatcher.group(5));
			profile.steamId = Utils.trimWhitespace(profileInfoMatcher.group(6));
			profile.steamUrl = Utils.trimWhitespace(profileInfoMatcher.group(7));

			// Group 1: 235 56 3
			// Group 2:
			// http://media.steampowered.com/steamcommunity/public/images/avatars/fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_medium.jpg
			// 80 121
			// Group 3: Skarbo 231 7
			// Group 4: March 16, 2013 656 14
			// Group 5:
			// 12 days ago 791 32
			// Group 6: STEAM_0:0:433023 1026 16
			// Group 7: http://steamcommunity.com/profiles/76561197961131774
		} else
			throw new InvalidParseException("Could not parse profile info");
	}

	public void doParseProfileInfoStats(Response response, ProfilesContainer.Profile profile) {
		Matcher profileInfoMatcher = getLeetwayProfileConfig().regexProfileInfo_Stats.matcher(response.getResponse());
		if (profileInfoMatcher.find()) {
			profile.stats.stats.put(Stats.Stat.frags, Utils.trimWhitespace(profileInfoMatcher.group(1)));
			profile.stats.stats.put(Stats.Stat.assists, Utils.trimWhitespace(profileInfoMatcher.group(2)));
			profile.stats.stats.put(Stats.Stat.deaths, Utils.trimWhitespace(profileInfoMatcher.group(3)));
			profile.stats.stats.put(Stats.Stat.suicides, Utils.trimWhitespace(profileInfoMatcher.group(4)));
			profile.stats.stats.put(Stats.Stat.headshots, Utils.trimWhitespace(profileInfoMatcher.group(5)));
			profile.stats.stats.put(Stats.Stat.headshotPercentage, Utils.trimWhitespace(profileInfoMatcher.group(6)));
			profile.stats.stats.put(Stats.Stat.teamKills, Utils.trimWhitespace(profileInfoMatcher.group(7)));
			profile.stats.stats.put(Stats.Stat.bombPlants, Utils.trimWhitespace(profileInfoMatcher.group(8)));
			profile.stats.stats.put(Stats.Stat.bombDefuse, Utils.trimWhitespace(profileInfoMatcher.group(9)));
			profile.stats.stats.put(Stats.Stat.killDeathRatio, Utils.trimWhitespace(profileInfoMatcher.group(10)));
			profile.stats.stats.put(Stats.Stat.damagePrRound, Utils.trimWhitespace(profileInfoMatcher.group(11)));
			profile.stats.stats.put(Stats.Stat.playerGamingRating, Utils.trimWhitespace(profileInfoMatcher.group(12)));
			profile.stats.stats.put(Stats.Stat.reputation, Utils.trimWhitespace(profileInfoMatcher.group(13)));

			profile.stats.stats.put(Stats.Stat.roundsPlayed, Utils.trimWhitespace(profileInfoMatcher.group(14)));
			profile.stats.stats.put(Stats.Stat.wonRounds, Utils.trimWhitespace(profileInfoMatcher.group(15)));
			profile.stats.stats.put(Stats.Stat.wonRoundsPercentage, Utils.trimWhitespace(profileInfoMatcher.group(16)));
			profile.stats.stats.put(Stats.Stat.lostRounds, Utils.trimWhitespace(profileInfoMatcher.group(17)));
			profile.stats.stats
					.put(Stats.Stat.lostRoundsPercentage, Utils.trimWhitespace(profileInfoMatcher.group(18)));
			profile.stats.stats.put(Stats.Stat.matchesPlayed, Utils.trimWhitespace(profileInfoMatcher.group(19)));
			profile.stats.stats.put(Stats.Stat.wonMatches, Utils.trimWhitespace(profileInfoMatcher.group(20)));
			profile.stats.stats
					.put(Stats.Stat.wonMatchesPercentage, Utils.trimWhitespace(profileInfoMatcher.group(21)));
			profile.stats.stats.put(Stats.Stat.tiedMatches, Utils.trimWhitespace(profileInfoMatcher.group(22)));
			profile.stats.stats.put(Stats.Stat.tiedMatchesPercentage,
					Utils.trimWhitespace(profileInfoMatcher.group(23)));
			profile.stats.stats.put(Stats.Stat.lostMatches, Utils.trimWhitespace(profileInfoMatcher.group(24)));
			profile.stats.stats.put(Stats.Stat.lostMatchesPercentage,
					Utils.trimWhitespace(profileInfoMatcher.group(25)));

			profile.stats.stats.put(Stats.Stat.kill2, Utils.trimWhitespace(profileInfoMatcher.group(26)));
			profile.stats.stats.put(Stats.Stat.kill3, Utils.trimWhitespace(profileInfoMatcher.group(27)));
			profile.stats.stats.put(Stats.Stat.kill4, Utils.trimWhitespace(profileInfoMatcher.group(28)));
			profile.stats.stats.put(Stats.Stat.kill5, Utils.trimWhitespace(profileInfoMatcher.group(29)));
			profile.stats.stats.put(Stats.Stat.oneV1, Utils.trimWhitespace(profileInfoMatcher.group(30)));
			profile.stats.stats.put(Stats.Stat.oneV2, Utils.trimWhitespace(profileInfoMatcher.group(31)));
			profile.stats.stats.put(Stats.Stat.oneV3, Utils.trimWhitespace(profileInfoMatcher.group(32)));
			profile.stats.stats.put(Stats.Stat.oneV4, Utils.trimWhitespace(profileInfoMatcher.group(33)));
			profile.stats.stats.put(Stats.Stat.oneV5, Utils.trimWhitespace(profileInfoMatcher.group(34)));

			profile.stats.stats.put(
					Stats.Stat.winLoseRatio,
					String.valueOf((double) Utils.parseDouble(profile.stats.stats.get(Stat.wonMatches))
							/ Utils.parseDouble(profile.stats.stats.get(Stat.lostMatches))));

			// Group 1: 303 1184 3
			// Group 2: 69 1221 2
			// Group 3: 273 1257 3
			// Group 4: 0 1294 1
			// Group 5: 152 1329 3
			// Group 6: 0.50 1366 4
			// Group 7: 0 1404 1
			// Group 8: 45 1439 2
			// Group 9: 7 1475 1
			// Group 10: 1.11 1510 4
			// Group 11: 92.05 1548 5
			// Group 12: 1,620.06 1587 8
			// Group 13: 10.67 1629 5
			// Group 14: 435 2381 3
			// Group 15: 230 2418 3
			// Group 16: 52.87% 2423 6
			// Group 17: 205 2464 3
			// Group 18: 47.13% 2469 6
			// Group 19: 20 2510 2
			// Group 20: 13 2546 2
			// Group 21: 65.00% 2550 6
			// Group 22: 0 2591 1
			// Group 23: 0.00% 2594 5
			// Group 24: 7 2634 1
			// Group 25: 35.00% 2637 6
			// Group 26: 49 3601 2
			// Group 27: 21 3687 2
			// Group 28: 3 3773 1
			// Group 29: 0 3858 1
			// Group 30: 19 3941 2
			// Group 31: 3 4025 1
			// Group 32: 3 4108 1
			// Group 33: 1 4191 1
			// Group 34: 0
		} else
			Log.w(TAG, "doParseProfileInfoStats: No profile info stats match");
	}

	public void doParseProfileInfoMatches(Response response, ProfilesContainer.Profile profile,
			ProfilesContainer profilesContainer) {
		Matcher profileInfoMatchesGroupMatcher = getLeetwayProfileConfig().regexProfileInfo_MatchesGroup
				.matcher(response.getResponse());

		if (profileInfoMatchesGroupMatcher.find()) {
			Matcher profileInfoMatchesMatcher = getLeetwayProfileConfig().regexProfileInfo_Matches
					.matcher(profileInfoMatchesGroupMatcher.group(1));

			int matchesCount = 0;
			SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
			while (profileInfoMatchesMatcher.find()) {
				MatchesContainer.Match match = new MatchesContainer.Match();
				PlayersContainer.Player player = new PlayersContainer.Player();

				match.serviceId = profile.serviceId;
				match.id = Utils.trimWhitespace(profileInfoMatchesMatcher.group(1));
				match.map = Utils.trimWhitespace(profileInfoMatchesMatcher.group(2));
				match.scoreHome.score = Utils.parseInt(profileInfoMatchesMatcher.group(4));
				match.scoreAway.score = Utils.parseInt(profileInfoMatchesMatcher.group(5));

				player.id = profile.id;
				player.serviceId = profile.serviceId;
				player.stats.stats.put(Stats.Stat.frags, Utils.trimWhitespace(profileInfoMatchesMatcher.group(6)));
				player.stats.stats.put(Stats.Stat.assists, Utils.trimWhitespace(profileInfoMatchesMatcher.group(7)));
				player.stats.stats.put(Stats.Stat.deaths, Utils.trimWhitespace(profileInfoMatchesMatcher.group(8)));
				player.stats.stats.put(Stats.Stat.killDeathRatio,
						Utils.trimWhitespace(profileInfoMatchesMatcher.group(9)));
				player.stats.stats.put(Stats.Stat.damagePrRound,
						Utils.trimWhitespace(profileInfoMatchesMatcher.group(10)));
				player.stats.stats.put(Stats.Stat.roundsPlayed,
						Utils.trimWhitespace(profileInfoMatchesMatcher.group(11)));
				player.stats.stats.put(Stats.Stat.points, Utils.trimWhitespace(profileInfoMatchesMatcher.group(12)));

				// Date
				String date = Utils.trimWhitespace(profileInfoMatchesMatcher.group(3));
				try {
					match.date = dateFormat.parse(date);
				} catch (ParseException e) {
					match.date = null;
				}

				// Group 1: 22468 952 5
				// Group 2: de_mirage_ce 1122 12
				// Group 3: 07/22/2013 1172 10
				// Group 4: 16 1252 2
				// Group 5: 4 1257 1
				// Group 6: 11 1299 2
				// Group 7: 2 1335 1
				// Group 8: 16 1370 2
				// Group 9: 0.69 1406 4
				// Group 10: 87.05 1444 5
				// Group 11: 20 1483 2
				// Group 12: -2.00

				profilesContainer.matchesContainer.addMatch(match);
				if (!profile.matchIds.contains(match.id))
					profile.matchIds.add(match.id);
				match.playersContainer.addPlayer(player);
				matchesCount++;
			}
			if (matchesCount == 0)
				Log.w(TAG, "doParseProfileInfoMatches: No profile info matches match");
		} else
			Log.w(TAG, "doParseProfileInfoMatches: No profile info matches group match");
	}

	public void doParseProfileWeapons(Response response, Profile profile) {
		Matcher profileWeaponsMatcher = getLeetwayProfileConfig().regexProfileWeapons.matcher(response.getResponse());

		int weaponsCount = 0;
		while (profileWeaponsMatcher.find()) {
			Stats.Weapon weapon = new Stats.Weapon();

			weapon.name = Utils.trimWhitespace(profileWeaponsMatcher.group(1));
			weapon.stats.put(Stats.Stat.frags, Utils.trimWhitespace(profileWeaponsMatcher.group(2)));
			weapon.stats.put(Stats.Stat.headshots, Utils.trimWhitespace(profileWeaponsMatcher.group(3)));

			profile.stats.weapons.add(weapon);
			weaponsCount++;
		}
		if (weaponsCount == 0)
			Log.w(TAG, "doParseProfileWeapons: No profile weapons match");
	}

	// ... /DO

	// ... IS

	public boolean isPageProfileInfo(Response response) {
		return isPage(getLeetwayProfileConfig().pageProfile.isPage, response);
	}

	public boolean isPageProfileWeapons(Response response) {
		return isPage(getLeetwayProfileConfig().pageProfileWeapons.isPage, response);
	}

	// ... /IS

	// ... CREATE

	private String createUrlProfileInfo() {
		return String.format(getLeetwayProfileConfig().pageProfile.page, getProfileId());
	}

	private String createUrlProfileWeapons() {
		return String.format(getLeetwayProfileConfig().pageProfileWeapons.page, getProfileId());
	}

	// ... /CREATE

}
