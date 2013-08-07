package com.skarbo.csgobrowser.handler.control.asynctask.profile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;

import android.util.Log;

import com.skarbo.csgobrowser.config.service.EseaServiceConfig;
import com.skarbo.csgobrowser.config.service.EseaServiceConfig.EseaPages.EseaProfile;
import com.skarbo.csgobrowser.container.MatchesContainer;
import com.skarbo.csgobrowser.container.PlayersContainer;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats.Stat;
import com.skarbo.csgobrowser.container.ProfilesContainer;
import com.skarbo.csgobrowser.exception.InvalidPageException;
import com.skarbo.csgobrowser.exception.InvalidParseException;
import com.skarbo.csgobrowser.handler.ControlHandler;
import com.skarbo.csgobrowser.handler.control.ControlHandlerResult;
import com.skarbo.csgobrowser.handler.control.asynctask.ProfileControlHandlerAsyncTask;
import com.skarbo.csgobrowser.utils.RestClient;
import com.skarbo.csgobrowser.utils.RestClient.Response;
import com.skarbo.csgobrowser.utils.Utils;

public class EseaProfileControlHandlerAsyncTask extends ProfileControlHandlerAsyncTask<EseaServiceConfig> {

	public EseaProfileControlHandlerAsyncTask(String profileId, ControlHandler controlHandler,
			ControlHandlerResult<ProfilesContainer> handlerResult) {
		super(EseaServiceConfig.SERVICE_ID, profileId, controlHandler, handlerResult);
	}

	// ... GET

	private EseaProfile getEseaProfileConfig() {
		return (EseaProfile) getServiceConfig().pages.profile;
	}

	// ... /GET

	// ... DO

	@Override
	public ProfilesContainer doHandleProfile() throws Exception {
		RestClient profileInfoRestClient = createRestClient(getServiceConfig(), createUrlProfileInfo());
		Response profileInfoResponse = profileInfoRestClient.execute(RestClient.RequestMethod.GET);

		ProfilesContainer profilesContainer = new ProfilesContainer();
		ProfilesContainer.Profile profile = new ProfilesContainer.Profile();
		profilesContainer.addProfile(profile);

		profile.serviceId = getServiceId();

		// INFO

		if (!isPageProfileInfo(profileInfoResponse))
			throw new InvalidPageException();

		doParseProfileInfo(profileInfoResponse, profile);
		doParseProfileInfoFriends(profileInfoResponse, profile);
//		doParseProfileInfoMatches(profileInfoResponse, profile, profilesContainer);

		// /INFO

		publishProgress(profilesContainer);

		// STATS

		RestClient profileStatsRestClient = createRestClient(getServiceConfig(), createUrlProfileStats());
		Response profileStatsResponse = profileStatsRestClient.execute(RestClient.RequestMethod.GET);

		if (!isPageProfileInfo(profileStatsResponse))
			throw new InvalidPageException();

		doParseProfileStatsServer(profileStatsResponse, profile);
		doParseProfileStatsWeaponsMaps(profileStatsResponse, profile);
		doParseProfileStatsMatches(profileStatsResponse, profile, profilesContainer);

		// /STATS

		return profilesContainer;
	}

	public void doParseProfileInfo(Response response, ProfilesContainer.Profile profile) throws InvalidParseException {
		Matcher profileInfoMatcher = getEseaProfileConfig().regexProfileInfo.matcher(response.getResponse());
		if (profileInfoMatcher.find()) {
			profile.id = Utils.trimWhitespace(profileInfoMatcher.group(1));
			profile.nickname = Utils.trimWhitespace(profileInfoMatcher.group(2));
			profile.signedInLast = Utils.trimWhitespace(profileInfoMatcher.group(3));
			profile.image = Utils.trimWhitespace(profileInfoMatcher.group(4));
			profile.name = Utils.trimWhitespace(profileInfoMatcher.group(5));
			profile.age = Utils.parseInt(profileInfoMatcher.group(6));
			profile.gender = Utils.trimWhitespace(profileInfoMatcher.group(7));
			profile.countryImage = Utils.trimWhitespace(profileInfoMatcher.group(8));
			profile.country = Utils.trimWhitespace(profileInfoMatcher.group(9));
			profile.location = Utils.trimWhitespace(profileInfoMatcher.group(10));
			profile.karma = Utils.parseInt(profileInfoMatcher.group(11));
		} else
			throw new InvalidParseException("Could not parse profile info");

		Matcher profileInfoRWSRankMatcher = getEseaProfileConfig().regexProfileInfo_RWSRank.matcher(response
				.getResponse());
		if (profileInfoRWSRankMatcher.find()) {
			profile.stats.stats.put(Stats.Stat.roundWinSharesRank, profileInfoRWSRankMatcher.group(1));
		} else
			Log.w(TAG, "doParseProfileInfo: No profile RWS rank match");
	}

	public void doParseProfileInfoFriends(Response response, ProfilesContainer.Profile profile) {
		Matcher profileInfoFriendsMatcher = getEseaProfileConfig().regexProfileInfo_Friends.matcher(response
				.getResponse());

		int friendsCount = 0;
		while (profileInfoFriendsMatcher.find()) {
			ProfilesContainer.Profile.Friend friend = new ProfilesContainer.Profile.Friend();

			friend.id = Utils.trimWhitespace(profileInfoFriendsMatcher.group(1));
			friend.countryImage = Utils.trimWhitespace(profileInfoFriendsMatcher.group(2));
			friend.country = Utils.trimWhitespace(profileInfoFriendsMatcher.group(3));
			friend.name = Utils.trimWhitespace(profileInfoFriendsMatcher.group(4));

			// Group 1: 51447 888 5
			// Group 2:
			// https://d1fj4sro0cm5nu.cloudfront.net/global/images/flags/NO.gif
			// 1231 64
			// Group 3: Norway 1304 6
			// Group 4: Antonbynisse

			Matcher profileInfoFriendsPlayingMatcher = getEseaProfileConfig().regexProfileInfo_FriendsPlaying
					.matcher(Utils.trimWhitespace(profileInfoFriendsMatcher.group(5)));
			if (profileInfoFriendsPlayingMatcher.find()) {
				friend.playingId = profileInfoFriendsPlayingMatcher.group(1);
				friend.playingServerName = profileInfoFriendsPlayingMatcher.group(2);
				// Group 1: 496 3543 3
				// Group 2: Stockholm 496
			}

			profile.addFriend(friend);
			friendsCount++;
		}
		if (friendsCount == 0)
			Log.w(TAG, "doParseProfileInfoFriends: No profile info friends match");
	}

	public void doParseProfileInfoMatches(Response response, ProfilesContainer.Profile profile,
			ProfilesContainer profilesContainer) {
		Matcher profileInfoMatchesMatcher = getEseaProfileConfig().regexProfileInfo_Matches.matcher(response
				.getResponse());

		int matchesCount = 0;
		while (profileInfoMatchesMatcher.find()) {
			MatchesContainer.Match match = new MatchesContainer.Match();

			match.serviceId = profile.serviceId;
			match.id = Utils.trimWhitespace(profileInfoMatchesMatcher.group(2));
			match.scoreHome.score = Utils.parseInt(profileInfoMatchesMatcher.group(3));
			match.scoreAway.score = Utils.parseInt(profileInfoMatchesMatcher.group(4));

			// Date
			String date = Utils.trimWhitespace(profileInfoMatchesMatcher.group(1));
			SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd");
			try {
				match.date = dateFormat.parse(date);
			} catch (ParseException e) {
				match.date = null;
			}

			// Jul 23
			//
			//
			// 268 36
			// Group 2: 3407390 359 7
			// Group 3: 15 370 2
			// Group 4: 15

			profilesContainer.matchesContainer.addMatch(match);
			if (!profile.matchIds.contains(match.id))
				profile.matchIds.add(match.id);
			matchesCount++;
		}
		if (matchesCount == 0)
			Log.w(TAG, "doParseProfileInfoMatches: No profile info matches match");
	}

	public void doParseProfileStatsServer(Response response, ProfilesContainer.Profile profile) {
		Matcher profileInfoStatsServer = getEseaProfileConfig().regexProfileStats_Server
				.matcher(response.getResponse());

		if (profileInfoStatsServer.find()) {
			profile.stats.stats.put(Stat.wonMatches, profileInfoStatsServer.group(2));
			profile.stats.stats.put(Stat.lostMatches, profileInfoStatsServer.group(3));
			profile.stats.stats.put(Stat.tiedMatches, profileInfoStatsServer.group(4));
			profile.stats.stats.put(Stat.wonMatchesPercentage, profileInfoStatsServer.group(5));
			profile.stats.stats.put(Stat.frags, profileInfoStatsServer.group(6));
			profile.stats.stats.put(Stat.assists, profileInfoStatsServer.group(7));
			profile.stats.stats.put(Stat.deaths, profileInfoStatsServer.group(8));
			profile.stats.stats.put(Stat.bombPlants, profileInfoStatsServer.group(9));
			profile.stats.stats.put(Stat.bombDefuse, profileInfoStatsServer.group(10));
			profile.stats.stats.put(Stat.kill3, profileInfoStatsServer.group(11));
			profile.stats.stats.put(Stat.kill4, profileInfoStatsServer.group(12));
			profile.stats.stats.put(Stat.kill5, profileInfoStatsServer.group(13));
			profile.stats.stats.put(Stat.oneV2, profileInfoStatsServer.group(14));
			profile.stats.stats.put(Stat.oneV3, profileInfoStatsServer.group(15));
			profile.stats.stats.put(Stat.oneV4, profileInfoStatsServer.group(16));
			profile.stats.stats.put(Stat.headshotPercentage, profileInfoStatsServer.group(17));
			profile.stats.stats.put(Stat.damagePrRound, profileInfoStatsServer.group(18));
			profile.stats.stats.put(Stat.fragsPrRound, profileInfoStatsServer.group(19));
			profile.stats.stats.put(Stat.roundWinShares, profileInfoStatsServer.group(20));

			profile.stats.stats.put(
					Stats.Stat.killDeathRatio,
					String.valueOf((double) Utils.parseDouble(profile.stats.stats.get(Stat.frags))
							/ Utils.parseDouble(profile.stats.stats.get(Stat.deaths))));
			profile.stats.stats.put(
					Stats.Stat.winLoseRatio,
					String.valueOf((double) Utils.parseDouble(profile.stats.stats.get(Stat.wonMatches))
							/ Utils.parseDouble(profile.stats.stats.get(Stat.lostMatches))));

			// Time
			String time = profileInfoStatsServer.group(1);
			if (!Utils.isEmpty(time)) {
				String times[] = time.split("\\:");
				int seconds = 0, factor = 0;
				for (int i = times.length - 1; i >= 0; i--) {
					if (i == 1)
						factor = 60;
					else if (i == 2)
						factor = 60 * 60;
					else
						factor = 1;
					seconds += Utils.parseInt(times[0]) * factor;
				}
				profile.stats.stats.put(Stat.time, String.valueOf(seconds));
			}

			// Group 1: 116:02:08 390 9
			// Group 2: 127 429 3
			// Group 3: 101 433 3
			// Group 4: 3 437 1
			// Group 5: 4090 1776 4
			// Group 6: 626 1813 3
			// Group 7: 3834 1849 4
			// Group 8: 528 1886 3
			// Group 9: 75 1922 2
			// Group 10: 192 1957 3
			// Group 11: 39 1993 2
			// Group 12: 4 2028 1
			// Group 13: 44 2062 2
			// Group 14: 14 2097 2
			// Group 15: 4 2132 1
			// Group 16: .192 2166 4
			// Group 17: 74.3 2203 4
			// Group 18: .686 2240 4
			// Group 19: 9.25
		} else
			Log.w(TAG, "doParseProfileStatsServer: No profile stats server match");
	}

	public void doParseProfileStatsWeaponsMaps(Response response, ProfilesContainer.Profile profile) {

		// WEAPONS

		Matcher profileInfoStatsWeaponsGroup = getEseaProfileConfig().regexProfileStats_WeaponsGroup.matcher(response
				.getResponse());

		if (profileInfoStatsWeaponsGroup.find()) {
			Matcher profileInfoStatsWeapons = getEseaProfileConfig().regexProfileStats_Weapons
					.matcher(profileInfoStatsWeaponsGroup.group(1));

			int weaponCount = 0;
			while (profileInfoStatsWeapons.find()) {
				Stats.Weapon weapon = new Stats.Weapon();

				weapon.name = Utils.trimWhitespace(profileInfoStatsWeapons.group(1));
				weapon.stats.put(Stats.Stat.frags, Utils.trimWhitespace(profileInfoStatsWeapons.group(2)));
				weapon.stats.put(Stats.Stat.deaths, Utils.trimWhitespace(profileInfoStatsWeapons.group(3)));
				weapon.stats.put(Stats.Stat.headshotPercentage, Utils.trimWhitespace(profileInfoStatsWeapons.group(4)));

				// Group 1: AK-47 538 5
				// Group 2: 1372 583 4
				// Group 3: 1436 962 4
				// Group 4: .192

				profile.stats.addWeapon(weapon);
				weaponCount++;
			}
			if (weaponCount == 0)
				Log.w(TAG, "doParseProfileStatsWeapons: No profile stats weapons match");
		} else
			Log.w(TAG, "doParseProfileStatsWeapons: No profile stats weapons group match");

		// /WEAPONS

		// MAPS

		Matcher profileInfoStatsMapsGroup = getEseaProfileConfig().regexProfileStats_MapsGroup.matcher(response
				.getResponse());

		if (profileInfoStatsMapsGroup.find()) {
			Matcher profileInfoStatsMaps = getEseaProfileConfig().regexProfileStats_Maps
					.matcher(profileInfoStatsMapsGroup.group(1));

			int mapCount = 0;
			while (profileInfoStatsMaps.find()) {
				Stats.Map map = new Stats.Map();

				map.map = Utils.trimWhitespace(profileInfoStatsMaps.group(1));
				map.stats.put(Stats.Stat.frags, Utils.trimWhitespace(profileInfoStatsMaps.group(2)));
				map.stats.put(Stats.Stat.deaths, Utils.trimWhitespace(profileInfoStatsMaps.group(3)));
				map.stats.put(Stats.Stat.fragsPrRound, Utils.trimWhitespace(profileInfoStatsMaps.group(4)));
				map.stats.put(Stats.Stat.wonMatchesPercentage, Utils.trimWhitespace(profileInfoStatsMaps.group(5)));

				profile.stats.addMap(map);
				mapCount++;
			}
			if (mapCount == 0)
				Log.w(TAG, "doParseProfileStatsWeapons: No profile stats maps match");
		} else
			Log.w(TAG, "doParseProfileStatsWeapons: No profile stats maps group match");

		// /WEAPONS
	}

	public void doParseProfileStatsMatches(Response response, ProfilesContainer.Profile profile,
			ProfilesContainer profilesContainer) {
		Matcher profileStatsMatchesMatcher = getEseaProfileConfig().regexProfileStats_Matches.matcher(response
				.getResponse());

		int matchesCount = 0;
		SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yy");
		while (profileStatsMatchesMatcher.find()) {
			MatchesContainer.Match match = new MatchesContainer.Match();
			PlayersContainer.Player player = new PlayersContainer.Player();

			match.serviceId = profile.serviceId;
			match.id = Utils.trimWhitespace(profileStatsMatchesMatcher.group(1));
			match.map = Utils.trimWhitespace(profileStatsMatchesMatcher.group(2));
			match.scoreHome.score = Utils.parseInt(profileStatsMatchesMatcher.group(3));
			match.scoreAway.score = Utils.parseInt(profileStatsMatchesMatcher.group(4));

			player.id = profile.id;
			player.serviceId = profile.serviceId;
			player.stats.stats.put(Stats.Stat.frags, Utils.trimWhitespace(profileStatsMatchesMatcher.group(5)));
			player.stats.stats.put(Stats.Stat.deaths, Utils.trimWhitespace(profileStatsMatchesMatcher.group(6)));
			player.stats.stats.put(Stats.Stat.kill5, Utils.trimWhitespace(profileStatsMatchesMatcher.group(7)));
			player.stats.stats
					.put(Stats.Stat.roundWinShares, Utils.trimWhitespace(profileStatsMatchesMatcher.group(8)));

			// Date
			String date = Utils.trimWhitespace(profileStatsMatchesMatcher.group(9));
			try {
				match.date = dateFormat.parse(date);
			} catch (ParseException e) {
				match.date = null;
			}

			// Group 1: 3407390 1049 7
			// Group 2: de_inferno_se 1137 13
			// Group 3: 15 1320 2
			// Group 4: 15 1323 2
			// Group 5: 11 1372 2
			// Group 6: 20 1406 2
			// Group 7: 0 1440 1
			// Group 8: 3.64 1473 4
			// Group 9: Jul 23 13

			if (!profile.matchIds.contains(match.id))
				profile.matchIds.add(match.id);

			match.playersContainer.addPlayer(player);
			profilesContainer.matchesContainer.addMatch(match);
			matchesCount++;
		}
		if (matchesCount == 0)
			Log.w(TAG, "doParseProfileStatsMatches: No profile stats matches match");
	}

	// ... /DO

	// ... IS

	public boolean isPageProfileInfo(Response response) {
		return isPage(getEseaProfileConfig().pageProfile.isPage, response);
	}

	public boolean isPageProfileStats(Response response) {
		return isPage(getEseaProfileConfig().pageProfileStats.isPage, response);
	}

	// ... /IS

	// ... CREATE

	private String createUrlProfileInfo() {
		return String.format(getEseaProfileConfig().pageProfile.page, getProfileId());
	}

	private String createUrlProfileStats() {
		return String.format(getEseaProfileConfig().pageProfileStats.page, getProfileId());
	}

	// ... /CREATE

}
