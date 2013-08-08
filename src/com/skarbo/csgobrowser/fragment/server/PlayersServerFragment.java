package com.skarbo.csgobrowser.fragment.server;

import java.util.Map;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.activity.ProfileActivity;
import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.container.PlayersContainer;
import com.skarbo.csgobrowser.container.PlayersContainer.Player;
import com.skarbo.csgobrowser.container.ServersContainer.Server;
import com.skarbo.csgobrowser.handler.Handler;
import com.skarbo.csgobrowser.has.HasServer;
import com.skarbo.csgobrowser.listener.HandlerListener;
import com.skarbo.csgobrowser.utils.DownloadImageTask;
import com.skarbo.csgobrowser.utils.UiUtils;
import com.skarbo.csgobrowser.utils.Utils;
import com.skarbo.csgobrowser.utils.Utils.TitleAcronym;

public class PlayersServerFragment extends Fragment implements HandlerListener {

	public static final String TAG = PlayersServerFragment.class.getSimpleName();

	private TableLayout serverProfileTableLayout;
	private TextView serverProfileNoneTextView;
	private LayoutInflater inflater;
	private Handler handler;
	private Map<String, TitleAcronym> playerStatsTitleAcronym;
	private ServiceConfig serviceConfig;

	// ... ON

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		this.handler = ((HandlerListener) getActivity()).getHandler();
		Server server = ((HasServer) getActivity()).getServer();

		if (server == null) {
			Toast.makeText(getActivity(), "Server doest not exist", Toast.LENGTH_SHORT).show();
		}

		this.serviceConfig = this.handler.getPreferenceHandler().createServiceConfig(server.serviceId);
		this.playerStatsTitleAcronym = Utils.parseTitleAcronymMap(getResources().getStringArray(R.array.player_stats));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.inflater = inflater;

		View view = (View) inflater.inflate(R.layout.fragment_server_players, container, false);

		this.serverProfileTableLayout = (TableLayout) view.findViewById(R.id.fragmentServerProfilePlayersTableLayout);
		this.serverProfileNoneTextView = (TextView) view.findViewById(R.id.fragmentServerProfilePlayersNoneTextView);

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (this.handler != null)
			this.handler.addListener(TAG, this);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.handler != null)
			this.handler.removeListener(TAG);
	}

	// ... ... HANDLER

	@Override
	public void onUpdating() {
		doUpdateView();
	}

	@Override
	public void onUpdated() {
		doUpdateView();
	}

	@Override
	public void onRefresh() {

	}

	// ... ... /HANDLER

	// ... /ON

	@Override
	public Handler getHandler() {
		return this.handler;
	}

	// ... DO

	public void doUpdateView() {
		Server server = ((HasServer) getActivity()).getServer();
		if (server.playersContainer.players.isEmpty()) {
			this.serverProfileNoneTextView.setVisibility(View.VISIBLE);
			this.serverProfileTableLayout.setVisibility(View.GONE);
		} else {
			this.serverProfileNoneTextView.setVisibility(View.GONE);
			this.serverProfileTableLayout.setVisibility(View.VISIBLE);

			this.serverProfileTableLayout.removeAllViews();
			this.serverProfileTableLayout.addView(createTableRowHeader(server));

			this.serverProfileTableLayout.addView(createTableRowTeam(server, true));

			int count = 0;
			for (Player player : server.playersContainer.players) {
				if (player != null) {
					if (player.team == PlayersContainer.Player.Team.Home) {
						this.serverProfileTableLayout.addView(createTableRowPlayer(server, player));
						count++;
					}
				}
			}
			if (count == 0)
				this.serverProfileTableLayout.addView(createTableRowPlayerNone());

			this.serverProfileTableLayout.addView(createTableRowTeam(server, false));

			count = 0;
			for (Player player : server.playersContainer.players) {
				if (player != null) {
					if (player.team == PlayersContainer.Player.Team.Away) {
						this.serverProfileTableLayout.addView(createTableRowPlayer(server, player));
						count++;
					}
				}
			}
			if (count == 0)
				this.serverProfileTableLayout.addView(createTableRowPlayerNone());
		}
	}

	// ... /DO

	// ... CREATE

	private TableRow createTableRowHeader(Server server) {
		TableRow tableRow = (TableRow) this.inflater.inflate(R.layout.table_row_server_player_header, null, false);

		for (PlayersContainer.Player.Stats.Stat statsType : this.serviceConfig.pages.server.showPlayerStats()) {
			tableRow.addView(createTableRowHeaderStat(statsType));
		}

		return tableRow;
	}

	private TextView createTableRowHeaderStat(PlayersContainer.Player.Stats.Stat statsType) {
		TextView textView = new TextView(getActivity());
		textView.setTextAppearance(getActivity(), R.style.TableRowPlayer_Header_Stats);

		if (this.playerStatsTitleAcronym.containsKey(statsType.toString())) {
			TitleAcronym titleAcronym = this.playerStatsTitleAcronym.get(statsType.toString());
			textView.setText(titleAcronym.acronym != "" ? titleAcronym.acronym : titleAcronym.title);
		} else {
			textView.setText(statsType.toString());
		}

		return textView;
	}

	private TableRow createTableRowTeam(Server server, boolean home) {
		TableRow tableRowServerPlayerTeam = (TableRow) this.inflater.inflate(R.layout.table_row_server_player_team,
				null, false);

		if (home) {
			((TextView) tableRowServerPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeam))
					.setText(R.string.team_home);
			((TextView) tableRowServerPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore)).setText(String
					.valueOf(server.scoreHome));
			if (server.scoreHome > server.scoreAway) {
                UiUtils.setViewBackground(tableRowServerPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore), getResources().getDrawable(R.drawable.server_score_tile_win));
			} else if (server.scoreHome < server.scoreAway) {
                UiUtils.setViewBackground(tableRowServerPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore), getResources().getDrawable(R.drawable.server_score_tile_lose));
			} else {
                UiUtils.setViewBackground(tableRowServerPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore), getResources().getDrawable(R.drawable.server_score_tile_draw));
			}

			if (server.scoreHome == -1)
				tableRowServerPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore).setVisibility(
						View.INVISIBLE);
		} else {
			((TextView) tableRowServerPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeam))
					.setText(R.string.team_away);
			((TextView) tableRowServerPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore)).setText(String
					.valueOf(server.scoreAway));
			if (server.scoreHome < server.scoreAway) {
                UiUtils.setViewBackground(tableRowServerPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore), getResources().getDrawable(R.drawable.server_score_tile_win));
			} else if (server.scoreHome > server.scoreAway) {
                UiUtils.setViewBackground(tableRowServerPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore), getResources().getDrawable(R.drawable.server_score_tile_lose));
			} else {
                UiUtils.setViewBackground(tableRowServerPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore), getResources().getDrawable(R.drawable.server_score_tile_draw));
			}

			if (server.scoreAway == -1)
				tableRowServerPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore).setVisibility(
						View.INVISIBLE);

		}

		int count = 0;
		for (Player player : server.playersContainer.players) {
			if (player != null) {
				if ((home && player.team == PlayersContainer.Player.Team.Home)
						|| (!home && player.team == PlayersContainer.Player.Team.Away)) {
					count++;
				}
			}
		}
		((TextView) tableRowServerPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerCount)).setText(String
				.valueOf(count));

		return tableRowServerPlayerTeam;
	}

	private TableRow createTableRowPlayer(Server server, final PlayersContainer.Player player) {
		TableRow tableRowServerPlayer = (TableRow) this.inflater.inflate(R.layout.table_row_server_player_player, null,
				false);

		tableRowServerPlayer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(ProfileActivity.createProfileActivity(getActivity(), player.serviceId, player.id));
			}
		});

		((TextView) tableRowServerPlayer.findViewById(R.id.tableRowServerPlayerPlayerTextView)).setText(Html
				.fromHtml(player.name));

		for (PlayersContainer.Player.Stats.Stat statsType : this.serviceConfig.pages.server.showPlayerStats()) {
			tableRowServerPlayer.addView(createTableRowPlayerStat(player, statsType));
		}

		// Image
		if (player.image != null) {
			ImageView playerImageView = (ImageView) tableRowServerPlayer
					.findViewById(R.id.tableRowServerPlayerImageView);
			Bitmap bitmapFromCache = this.handler.getBitmapFromCache(player.image);

			if (bitmapFromCache != null) {
				playerImageView.setImageBitmap(bitmapFromCache);
			} else {
				String URL = player.image;
				playerImageView.setTag(URL);
				(new DownloadImageTask(handler)).execute(playerImageView);
			}
		}

		return tableRowServerPlayer;
	}

	private TextView createTableRowPlayerStat(PlayersContainer.Player player,
			PlayersContainer.Player.Stats.Stat statsType) {
		TextView textView = new TextView(getActivity());
		textView.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		textView.setGravity(Gravity.CENTER);
		textView.setTextAppearance(getActivity(), R.style.TableRowPlayer_Player_Stats);

		if (player.stats.stats.containsKey(statsType)) {
			textView.setText(player.stats.stats.get(statsType));
		} else {
			textView.setText("");
		}

		return textView;
	}

	private TableRow createTableRowPlayerNone() {
		return (TableRow) this.inflater.inflate(R.layout.table_row_server_player_none, null, false);
	}

	// ... /CREATE

}
