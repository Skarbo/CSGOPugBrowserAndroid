package com.skarbo.csgobrowser.container;

import java.util.ArrayList;
import java.util.List;

import com.skarbo.csgobrowser.utils.Utils;

public class ProfilesContainer implements Container<ProfilesContainer> {

	public List<ProfilesContainer.Profile> profiles = new ArrayList<ProfilesContainer.Profile>();
	public MatchesContainer matchesContainer = new MatchesContainer();

	@Override
	public void merge(ProfilesContainer merge) {
		if (merge == null)
			return;
		for (Profile profile : merge.profiles)
			addProfile(profile);
		this.matchesContainer.merge(merge.matchesContainer);
	}

	public void addProfile(Profile profile) {
		int indexOf = this.profiles.indexOf(profile);
		if (indexOf > -1)
			this.profiles.get(indexOf).merge(profile);
		else
			this.profiles.add(profile);
	}

	public Profile getProfile(String serviceId, String profileId) {
		for (Profile profile : this.profiles) {
			if (profile.isEqual(serviceId, profileId))
				return profile;
		}
		return null;
	}

	// CLASS

	public static class Profile implements Container<Profile> {
		public String serviceId = "";
		public String id = "";
		public String image;
		public String nickname;
		public String name;
		public int age;
		public String gender;
		public String country;
		public String location;
		public String countryImage;
		public int karma;
		public String playingId;
		public String registered;
		public String signedInLast;
		public String steamId;
		public String steamUrl;
		public PlayersContainer.Player.Stats stats = new PlayersContainer.Player.Stats();

		public List<String> matchIds = new ArrayList<String>();
		public List<Friend> friends = new ArrayList<Friend>();

		@Override
		public String toString() {
			return String
					.format("Service id: %s, Id: %s, Image: %s, Nickname: %s, Name: %s, Age: %d, Gender: %s, Country: %s, Location: %s, Country image: %s, Karma: %s, Playing id: %s, Signed in last: %s, Friends: %d, Matches: %d",
							this.serviceId, this.id, this.image, this.nickname, this.name, this.age, this.gender,
							this.country, this.location, this.countryImage, this.karma, this.playingId,
							this.signedInLast, this.friends.size(), this.matchIds.size());
		}

		@Override
		public void merge(Profile profile) {
			if (!Utils.isEmpty(profile.serviceId))
				this.serviceId = profile.serviceId;
			if (!Utils.isEmpty(profile.id))
				this.id = profile.id;
			if (!Utils.isEmpty(profile.image))
				this.image = profile.image;
			if (!Utils.isEmpty(profile.nickname))
				this.nickname = profile.nickname;
			if (!Utils.isEmpty(profile.name))
				this.name = profile.name;
			if (profile.age > 0)
				this.age = profile.age;
			if (!Utils.isEmpty(profile.gender))
				this.gender = profile.gender;
			if (!Utils.isEmpty(profile.country))
				this.country = profile.country;
			if (!Utils.isEmpty(profile.location))
				this.location = profile.location;
			if (!Utils.isEmpty(profile.countryImage))
				this.countryImage = profile.countryImage;
			if (profile.karma > 0)
				this.karma = profile.karma;
			if (!Utils.isEmpty(profile.playingId))
				this.playingId = profile.playingId;
			if (!Utils.isEmpty(profile.signedInLast))
				this.signedInLast = profile.signedInLast;

			this.stats.merge(profile.stats);

			for (String matchId : profile.matchIds) {
				if (!this.matchIds.contains(matchId))
					this.matchIds.add(matchId);
			}
			for (Friend friend : profile.friends) {
				int indexOf = profile.friends.indexOf(friend);
				if (indexOf > -1)
					profile.friends.get(indexOf).merge(friend);
				else
					profile.friends.add(friend);
			}
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Profile))
				return super.equals(o);
			Profile profile = (Profile) o;
			return this.serviceId.equalsIgnoreCase(profile.serviceId) && this.id.equalsIgnoreCase(profile.id);
		}

		public boolean isEqual(String serviceId, String profileId) {
			return this.serviceId.equalsIgnoreCase(serviceId) && this.id.equalsIgnoreCase(profileId);
		}

		public void addFriend(Friend friend) {
			int indexOf = this.friends.indexOf(friend);
			if (indexOf > -1)
				this.friends.get(indexOf).merge(friend);
			else
				this.friends.add(friend);
		}

		public Friend getFriend(String friendId) {
			for (Friend friend : this.friends) {
				if (friend.isEqual(friendId))
					return friend;
			}
			return null;
		}

		// CLASS

		public static class Friend implements Container<Friend> {

			public String id = "";
			public String country;
			public String countryImage;
			public String name;
			public String playingId;
			public String playingServerName;

			@Override
			public void merge(Friend friend) {
				if (!Utils.isEmpty(friend.id)) {
					this.id = friend.id;
				}
				if (!Utils.isEmpty(friend.country)) {
					this.country = friend.country;
				}
				if (!Utils.isEmpty(friend.countryImage)) {
					this.countryImage = friend.countryImage;
				}
				if (!Utils.isEmpty(friend.name)) {
					this.name = friend.name;
				}
				this.playingId = friend.playingId;
				this.playingServerName = friend.playingServerName;
			}

			@Override
			public boolean equals(Object o) {
				if (!(o instanceof Friend))
					return super.equals(o);
				return isEqual(((Friend) o).id);
			}

			public boolean isEqual(String friendId) {
				return this.id.equalsIgnoreCase(friendId);
			}

		}

		// /CLASS
	}

	// /CLASS

}
