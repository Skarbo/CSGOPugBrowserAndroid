package com.skarbo.csgobrowser.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import android.util.Log;

import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats.Map;
import com.skarbo.csgobrowser.container.PlayersContainer.Player.Stats.Weapon;
import com.skarbo.csgobrowser.utils.Utils;

public class PlayersContainer implements Container<PlayersContainer> {

	private static final String TAG = PlayersContainer.class.getSimpleName();

	public List<PlayersContainer.Player> players = new ArrayList<PlayersContainer.Player>();

	public static class Player implements Comparable<Player> {

		public enum Team {
			Home, Away
		}

		public String serviceId = "";
		public Team team;
		public String id = "";
		public String image = "";
		public String name = "";
		public Stats stats = new Stats();

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Player))
				return super.equals(o);
			Player player = (Player) o;
			return isEqual(player.id, player.serviceId);
		}

		private boolean isEqual(String playerId, String serviceId) {
			return playerId.equalsIgnoreCase(this.id) && serviceId.equalsIgnoreCase(this.serviceId);
		}

		public void merge(Player player) {
			if (!Utils.isEmpty(player.id))
				this.id = player.id;
			if (!Utils.isEmpty(player.image))
				this.image = player.image;
			if (!Utils.isEmpty(player.name))
				this.name = player.name;
			if (player.team != null)
				this.team = player.team;
			this.stats.merge(player.stats);
		}

		@Override
		public int compareTo(Player another) {
			return 0;
		}

		@Override
		public String toString() {
			return String.format("Service id: %s, Team: %s, Id: %s, Image: %s, Name: %s", this.serviceId, this.team,
					this.id, this.image, this.name);
		}

		// CLASS

		public static class Stats implements Container<Stats> {
			public enum Stat {
				playerGamingRating, reputation, time, roundWinShares, roundWinSharesRank, frags, assists, deaths, bombPlants, bombDefuse, kill2, kill3, kill4, kill5, oneV1, oneV2, headshotPercentage, roundsPlayed, damagePrRound, fragsPrRound, suicides, headshots, teamKills, killDeathRatio, points, oneV3, oneV4, oneV5, wonMatches, lostMatches, tiedMatches, wonMatchesPercentage, winLoseRatio, lostMatchesPercentage, matchesPlayed, tiedMatchesPercentage, wonRounds, wonRoundsPercentage, lostRounds, lostRoundsPercentage
			}

			public java.util.Map<Stats.Stat, String> stats = new HashMap<Stats.Stat, String>();
			public List<PlayersContainer.Player.Stats.Weapon> weapons = new ArrayList<PlayersContainer.Player.Stats.Weapon>();
			public List<PlayersContainer.Player.Stats.Map> maps = new ArrayList<PlayersContainer.Player.Stats.Map>();

			@Override
			public void merge(Stats stats) {
				for (Entry<Stats.Stat, String> entry : stats.stats.entrySet()) {
					this.stats.put(entry.getKey(), entry.getValue());
				}
				for (Weapon weapon : stats.weapons) {
					int indexOf = this.weapons.indexOf(weapon);
					if (indexOf > -1)
						this.weapons.get(indexOf).merge(weapon);
					else
						this.weapons.add(weapon);
				}
				for (Map map : stats.maps) {
					int indexOf = this.maps.indexOf(map);
					if (indexOf > -1)
						this.maps.get(indexOf).merge(map);
					else
						this.maps.add(map);
				}
			}

			public void addWeapon(Weapon weapon) {
				int indexOf = this.weapons.indexOf(weapon);
				if (indexOf > -1)
					this.weapons.get(indexOf).merge(weapon);
				else
					this.weapons.add(weapon);
			}

			public void addMap(Map map) {
				int indexOf = this.maps.indexOf(map);
				if (indexOf > -1)
					this.maps.get(indexOf).merge(map);
				else
					this.maps.add(map);
			}

			@Override
			public String toString() {
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append("Stats: ");
				for (Entry<Stats.Stat, String> entry : this.stats.entrySet())
					stringBuilder.append(entry.getKey() + ": " + entry.getValue() + ", ");
				stringBuilder.append("\nWeapons: ");
				for (Weapon weapon : this.weapons)
					stringBuilder.append(weapon.toString() + ", ");
				stringBuilder.append("\nMaps: ");
				for (Map map : this.maps)
					stringBuilder.append(map.toString() + ", ");
				return stringBuilder.toString();
			}

			// CLASS

			public static class Weapon implements Container<Weapon> {
				public String name = "";
				public java.util.Map<PlayersContainer.Player.Stats.Stat, String> stats = new HashMap<PlayersContainer.Player.Stats.Stat, String>();

				@Override
				public void merge(Weapon weapon) {
					for (Entry<Stats.Stat, String> entry : weapon.stats.entrySet())
						this.stats.put(entry.getKey(), entry.getValue());
				}

				@Override
				public boolean equals(Object o) {
					if (!(o instanceof Weapon))
						return super.equals(o);
					return isEqual(((Weapon) o).name);
				}

				public boolean isEqual(String weapon) {
					return this.name.equalsIgnoreCase(weapon);
				}
			}

			public static class Map implements Container<Map> {
				public String map = "";
				public java.util.Map<Stats.Stat, String> stats = new HashMap<PlayersContainer.Player.Stats.Stat, String>();

				@Override
				public void merge(Map map) {
					for (Entry<Stats.Stat, String> entry : map.stats.entrySet())
						this.stats.put(entry.getKey(), entry.getValue());
				}

				@Override
				public boolean equals(Object o) {
					if (!(o instanceof Weapon))
						return super.equals(o);
					return isEqual(((Weapon) o).name);
				}

				public boolean isEqual(String weapon) {
					return this.map.equalsIgnoreCase(weapon);
				}
			}

		}

		// /CLASS

	}

	@Override
	public void merge(PlayersContainer merge) {
		if (merge == null)
			return;
		for (Player player : merge.players) {
			addPlayer(player);
		}
	}

	public void addPlayer(PlayersContainer.Player player) {
		int indexOf = this.players.indexOf(player);
		if (indexOf > -1)
			this.players.get(indexOf).merge(player);
		else
			this.players.add(player);
	}

	public PlayersContainer.Player getPlayer(String serviceId, String playerId) {
		if (Utils.isEmpty(serviceId) || Utils.isEmpty(playerId))
			return null;
		for (Player player : this.players) {
			if (player.isEqual(playerId, serviceId))
				return player;
		}
		return null;
	}
}
