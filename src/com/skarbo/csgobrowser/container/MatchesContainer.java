package com.skarbo.csgobrowser.container;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.util.Log;

import com.skarbo.csgobrowser.utils.Utils;
import com.skarbo.csgobrowser.utils.Utils.Country;

public class MatchesContainer implements Container<MatchesContainer> {

	private static final String TAG = MatchesContainer.class.getSimpleName();
	public List<MatchesContainer.Match> matches = new ArrayList<MatchesContainer.Match>();

	public static class Match {

		public enum Status {
			Live, Completed
		}

		public String serviceId = "";
		public String id = "";
		public Status status;
		public String time;
		public String map;
		public Date date;
		public Country country;
		public String location;
		public Score scoreHome = new Score();
		public Score scoreAway = new Score();

		public PlayersContainer playersContainer = new PlayersContainer();

		public void merge(Match match) {
			if (match.status != null)
				this.status = match.status;
			if (!Utils.isEmpty(match.time))
				this.time = match.time;
			if (!Utils.isEmpty(match.map))
				this.map = match.map;
			if (match.date != null)
				this.date = match.date;
			if (match.country != null)
				this.country = match.country;
			if (!Utils.isEmpty(location))
				this.location = match.location;
			this.scoreHome.merge(match.scoreHome);
			this.scoreAway.merge(match.scoreAway);

			this.playersContainer.merge(match.playersContainer);
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Match))
				return super.equals(o);
			Match match = (Match) o;
			return isEqual(match.serviceId, match.id);
		}

		public boolean isEqual(String serviceId, String matchId) {
			return serviceId.equalsIgnoreCase(this.serviceId) && matchId.equalsIgnoreCase(this.id);
		}

		@Override
		public String toString() {
			return String
					.format("Service id: %s, Id: %s, Status: %s, Time: %s, Map: %s, Date: %s, Score Home: %s, Score Away: %s, Players: %d",
							this.serviceId, this.id, this.status, this.time, this.map, this.date,
							this.scoreHome.toString(), this.scoreAway.toString(), this.playersContainer.players.size());
		}

		// CLASS

		public static class Score {
			public int halfT = -1;
			public int halfCT = -1;
			public int score = -1;
			public int frags = -1;
			public int deaths = -1;
			public String playerGamingRating;

			public void merge(Score scoreMerge) {
				if (scoreMerge.halfT > -1)
					this.halfT = scoreMerge.halfT;
				if (scoreMerge.halfCT > -1)
					this.halfCT = scoreMerge.halfCT;
				if (scoreMerge.score > -1)
					this.score = scoreMerge.score;
				if (scoreMerge.frags > -1)
					this.frags = scoreMerge.frags;
				if (scoreMerge.deaths > -1)
					this.deaths = scoreMerge.deaths;
			}

			@Override
			public String toString() {
				return String.format("%d/%d/%d/%d/%d", this.score, this.halfT, this.halfCT, this.frags, this.deaths);
			}
		}

		// /CLASS

	}

	public void addMatch(MatchesContainer.Match match) {
		int indexOf = this.matches.indexOf(match);
		if (indexOf > -1)
			this.matches.get(indexOf).merge(match);
		else
			this.matches.add(match);
	}

	@Override
	public void merge(MatchesContainer merge) {
		if (merge == null)
			return;
		for (Match match : merge.matches) {
			addMatch(match);
		}
	}

	public Match getMatch(String serviceId, String matchId) {
		if (serviceId == null || matchId == null)
			return null;
		for (Match match : this.matches) {
			if (match.isEqual(serviceId, matchId))
				return match;
		}
		return null;
	}

}
