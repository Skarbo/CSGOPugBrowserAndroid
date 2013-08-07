package com.skarbo.csgobrowser.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.skarbo.csgobrowser.container.MatchesContainer.Match;
import com.skarbo.csgobrowser.utils.Utils;
import com.skarbo.csgobrowser.utils.Utils.Country;

public class ServersContainer implements Container<ServersContainer> {

	public List<ServersContainer.Server> servers = new ArrayList<ServersContainer.Server>();

	public static class Server implements Comparable<Server> {
		public enum Status {
			Available, Waiting, Live
		}

		public String serviceId = "";
		public String id = "";
		public String name;
		public Country country;
		public String location;
		public String ipAddress;
		public String map;
		public int playersCurrent = -1;
		public int playersMax = -1;
		public int scoreHome = -1;
		public int scoreAway = -1;
		public Status status;
		public String matchId = "";

		public PlayersContainer playersContainer = new PlayersContainer();

		@Override
		public String toString() {
			return String
					.format("Service id: %s, Id: %s, Name: %s, Country: %s, IpAddress: %s, Map: %s, Players current: %d, Players max: %d, Score home: %d, Score away: %d, Status: %s, Match id: %s",
							this.serviceId, this.id, this.name, this.country, this.ipAddress, this.map,
							this.playersCurrent, this.playersMax, this.scoreHome, this.scoreAway, this.status,
							this.matchId);
		}

		public void merge(Server server) {
			if (!Utils.isEmpty(server.id)) {
				this.id = server.id;
			}
			if (!Utils.isEmpty(server.name)) {
				this.name = server.name;
			}
			if (!Utils.isEmpty(server.ipAddress)) {
				this.ipAddress = server.ipAddress;
			}
			if (!Utils.isEmpty(server.map)) {
				this.map = server.map;
			}
			if (!Utils.isEmpty(server.matchId)) {
				this.matchId = server.matchId;
			}
			if (server.status != null) {
				this.status = server.status;
			}
			if (server.country != null) {
				this.country = server.country;
			}
			if (!Utils.isEmpty(server.location)) {
				this.location = server.location;
			}
			if (server.playersCurrent > -1)
				this.playersCurrent = server.playersCurrent;
			if (server.playersMax > -1)
				this.playersMax = server.playersMax;
			if (server.scoreHome > -1)
				this.scoreHome = server.scoreHome;
			if (server.scoreAway > -1)
				this.scoreAway = server.scoreAway;

			this.playersContainer.merge(server.playersContainer);
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Server))
				return super.equals(o);
			Server server = (Server) o;
			return isEqual(server.serviceId, server.id, server.matchId);
		}

		public boolean isEqual(String serviceId, String id, String matchId) {
			return this.serviceId.equalsIgnoreCase(serviceId)
					&& ((!Utils.isEmpty(id) && this.id.equalsIgnoreCase(id)) || (!Utils.isEmpty(matchId) && this.matchId
							.equalsIgnoreCase(matchId)));
		}

		@Override
		public int compareTo(Server another) {
			if (this.status != another.status) {
				if (this.status == Status.Live || (this.status == Status.Waiting && another.status == Status.Available))
					return -1;
				else
					return 1;
			}
			int comparePlayers = Integer.valueOf(another.playersCurrent)
					.compareTo(Integer.valueOf(this.playersCurrent));
			if (comparePlayers != 0)
				return comparePlayers;
			return this.name.compareTo(another.name);
		}
	}

	@Override
	public void merge(ServersContainer merge) {
		if (merge == null)
			return;
		for (Server server : merge.servers) {
			addServer(server);
		}
	}

	public void merge(MatchesContainer matchesContainer) {
		if (matchesContainer == null)
			return;
		for (Match match : matchesContainer.matches) {
			Server server = getServer(match.serviceId, null, match.id);
			if (server != null) {
				if (match.scoreHome.score > -1)
					server.scoreHome = match.scoreHome.score;
				if (match.scoreAway.score > -1)
					server.scoreAway = match.scoreAway.score;
				server.playersContainer.merge(match.playersContainer);
				match.playersContainer.merge(server.playersContainer);
			}
		}
	}

	public void addServer(ServersContainer.Server server) {
		int indexOf = this.servers.indexOf(server);
		if (indexOf > -1)
			this.servers.get(indexOf).merge(server);
		else
			this.servers.add(server);
	}

	public Server getServer(String serviceId, String serverId, String matchId) {
		for (Server server : this.servers) {
			if (server.isEqual(serviceId, serverId, matchId))
				return server;
		}
		return null;
	}

}
