package com.skarbo.csgobrowser.fragment.match;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.skarbo.csgobrowser.R;
import com.skarbo.csgobrowser.activity.ProfileActivity;
import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.container.MatchesContainer.Match;
import com.skarbo.csgobrowser.container.PlayersContainer;
import com.skarbo.csgobrowser.container.PlayersContainer.Player;
import com.skarbo.csgobrowser.handler.Handler;
import com.skarbo.csgobrowser.has.HasMatch;
import com.skarbo.csgobrowser.listener.HandlerListener;
import com.skarbo.csgobrowser.utils.DownloadImageTask;
import com.skarbo.csgobrowser.utils.Utils;
import com.skarbo.csgobrowser.utils.Utils.TitleAcronym;

public class StatsMatchFragment extends Fragment implements HandlerListener {

	public static final String TAG = StatsMatchFragment.class.getSimpleName();
	private static final String ARG_SERVICE_ID = "service_id";
	private static final String ARG_MATCH_ID = "match_id";

	private Handler handler;

	private LayoutInflater inflater;
	private TextView matchStatsNoneTextView;
	private LinearLayout matchStatsContentLayout;
	private LinearLayout matchStatsPlayersLayout;
	private LinearLayout matchStatsHeaderContainerLayout;
	private RelativeLayout matchStatsButtonsLayout;
	private ImageButton matchStatsButtonsNextImageButton;
	private ImageButton matchStatsButtonsPrevImageButton;
	private Map<String, TitleAcronym> playerStatsTitleAcronym;
	private ServiceConfig serviceConfig;
	private String serviceId;
	private String matchId;
	private int statsGroup = 0;
	private List<StatsGroupLinearLayout> statsGroupLinearLayouts;
	private Animation animationSlideInLeft;
	private Animation animationSlideInRight;
	private StatsButtonsGestureListener statsButtonsGestureDetector;

	public static StatsMatchFragment createStatsMatchFragment(Context context, String serviceId, String matchId) {
		StatsMatchFragment statsMatchFragment = new StatsMatchFragment();
		Bundle bundle = new Bundle();
		bundle.putString(ARG_SERVICE_ID, serviceId);
		bundle.putString(ARG_MATCH_ID, matchId);
		statsMatchFragment.setArguments(bundle);
		return statsMatchFragment;
	}

	// ... ON

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		this.handler = ((HandlerListener) getActivity()).getHandler();

		this.serviceId = getArguments().getString(ARG_SERVICE_ID);
		this.matchId = getArguments().getString(ARG_MATCH_ID);

		this.serviceConfig = this.handler.getPreferenceHandler().createServiceConfig(this.serviceId);
		this.playerStatsTitleAcronym = Utils.parseTitleAcronymMap(getResources().getStringArray(R.array.player_stats));

		this.statsGroupLinearLayouts = new ArrayList<StatsMatchFragment.StatsGroupLinearLayout>();

		this.animationSlideInLeft = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_left);
		this.animationSlideInRight = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_right);

		// fadeOut.setAnimationListener(new StatsAnimationListener());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.inflater = inflater;

		View view = (View) inflater.inflate(R.layout.fragment_match_stats, container, false);

		this.matchStatsNoneTextView = (TextView) view.findViewById(R.id.matchStatsNone_TextView);
		this.matchStatsContentLayout = (LinearLayout) view.findViewById(R.id.matchStatsContentLayout);
		this.matchStatsPlayersLayout = (LinearLayout) view.findViewById(R.id.matchStatsPlayersLayout);
		this.matchStatsHeaderContainerLayout = (LinearLayout) view.findViewById(R.id.matchStatsHeaderContainerLayout);
		this.matchStatsButtonsLayout = (RelativeLayout) view.findViewById(R.id.matchStatsButtonsLayout);
		this.matchStatsButtonsNextImageButton = (ImageButton) view.findViewById(R.id.matchStatsButtonsNextImageButton);
		this.matchStatsButtonsPrevImageButton = (ImageButton) view.findViewById(R.id.matchStatsButtonsPrevImageButton);

		this.matchStatsButtonsNextImageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doStatsSwitch(true);
			}
		});
		this.matchStatsButtonsPrevImageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doStatsSwitch(false);
			}
		});

		this.matchStatsNoneTextView.setVisibility(View.VISIBLE);
		this.matchStatsContentLayout.setVisibility(View.GONE);
		this.matchStatsButtonsLayout.setVisibility(View.GONE);

		this.statsButtonsGestureDetector = new StatsButtonsGestureListener(getActivity());
		this.matchStatsButtonsLayout.setOnTouchListener(this.statsButtonsGestureDetector);

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
		Match match = ((HasMatch) getActivity()).getMatch();

		if (match == null) {
			this.matchStatsNoneTextView.setVisibility(View.VISIBLE);
			this.matchStatsContentLayout.setVisibility(View.GONE);
			this.matchStatsButtonsLayout.setVisibility(View.GONE);
		} else {
			this.matchStatsNoneTextView.setVisibility(View.GONE);
			this.matchStatsContentLayout.setVisibility(View.VISIBLE);
			this.matchStatsButtonsLayout.setVisibility(View.VISIBLE);

			this.statsGroupLinearLayouts.clear();

			// HEADER

			this.matchStatsHeaderContainerLayout.removeAllViews();

			int i = 0;
			for (List<PlayersContainer.Player.Stats.Stat> statsTypes : this.serviceConfig.pages.match.showPlayerStats()) {
				StatsGroupLinearLayout linearLayout = createHeaderStat(i, statsTypes);
				this.matchStatsHeaderContainerLayout.addView(linearLayout);
				this.statsGroupLinearLayouts.add(linearLayout);
				i++;
			}

			// /HEADER

			// PLAYERS

			this.matchStatsPlayersLayout.removeAllViews();

			// ... HOME

			this.matchStatsPlayersLayout.addView(createLayoutTeam(match, true));

			int count = 0;
			for (Player player : match.playersContainer.players) {
				if (player != null && player.team == PlayersContainer.Player.Team.Home) {
					this.matchStatsPlayersLayout.addView(createLayoutPlayer(match, player));
					count++;
				}
			}
			if (count == 0)
				this.matchStatsPlayersLayout.addView(createLayoutPlayerNone());

			// ... /HOME

			View viewBorder = this.inflater.inflate(R.layout.layout_border, null, false);
			viewBorder.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, 2));
			this.matchStatsPlayersLayout.addView(viewBorder);

			// ... AWAY

			this.matchStatsPlayersLayout.addView(createLayoutTeam(match, false));

			count = 0;
			for (Player player : match.playersContainer.players) {
				if (player != null && player.team == PlayersContainer.Player.Team.Away) {
					this.matchStatsPlayersLayout.addView(createLayoutPlayer(match, player));
					count++;
				}
			}
			if (count == 0)
				this.matchStatsPlayersLayout.addView(createLayoutPlayerNone());

			// ... /AWAY

			// /PLAYERS
		}

		doStatsShow(this.statsGroup, false);
	}

	protected void doStatsSwitch(boolean next) {
		int statsGroupSize = this.serviceConfig.pages.match.showPlayerStats().size();
		int statsGroup = Utils.mod(this.statsGroup + (next ? 1 : -1), statsGroupSize);
		doStatsShow(statsGroup, next);
	}

	protected void doStatsShow(int count, boolean left) {
		for (StatsGroupLinearLayout statsGroupLinearLayout : this.statsGroupLinearLayouts) {
			if (statsGroupLinearLayout.statsGroupNumber == count) {
				statsGroupLinearLayout.setVisibility(View.VISIBLE);
				statsGroupLinearLayout.startAnimation(left ? this.animationSlideInLeft : this.animationSlideInRight);
			} else {
				statsGroupLinearLayout.setVisibility(View.GONE);
			}
		}

		TextView matchStatsButtonsCountTextView = (TextView) this.matchStatsButtonsLayout
				.findViewById(R.id.matchStatsButtonsCountTextView);
		matchStatsButtonsCountTextView.setText(String.format("%d/%d", count + 1, this.serviceConfig.pages.match
				.showPlayerStats().size()));

		// for (int i = 0; i <
		// this.matchStatsHeaderContainerLayout.getChildCount(); i++) {
		// StatsGroupLinearLayout statsGroupLinearLayout =
		// (StatsGroupLinearLayout) this.matchStatsHeaderContainerLayout
		// .getChildAt(i);
		// if (statsGroupLinearLayout.statsGroupNumber == count) {
		// statsGroupLinearLayout.setVisibility(View.VISIBLE);
		// } else {
		// statsGroupLinearLayout.setVisibility(View.GONE);
		// }
		// }
		//
		// for (int i = 0; i < this.matchStatsPlayersLayout.getChildCount();
		// i++) {
		// RelativeLayout relativeLayout = (RelativeLayout)
		// this.matchStatsPlayersLayout.getChildAt(i);
		// LinearLayout linearLayout = (LinearLayout) relativeLayout
		// .findViewById(R.id.matchStatsPlayerContainerLayout);
		// if (linearLayout != null) {
		// for (int j = 0; j < linearLayout.getChildCount(); j++) {
		// StatsGroupLinearLayout statsGroupLinearLayout =
		// (StatsGroupLinearLayout) linearLayout.getChildAt(j);
		// if (statsGroupLinearLayout.statsGroupNumber == count) {
		// statsGroupLinearLayout.setVisibility(View.VISIBLE);
		// } else {
		// statsGroupLinearLayout.setVisibility(View.GONE);
		// }
		// }
		// }
		// }

		this.statsGroup = count;
	}

	// ... /DO

	// ... CREATE

	private StatsGroupLinearLayout createHeaderStat(int i, List<PlayersContainer.Player.Stats.Stat> statsTypes) {
		StatsGroupLinearLayout linearLayout = new StatsGroupLinearLayout(getActivity(), i);
		linearLayout.setWeightSum(statsTypes.size());
		linearLayout
				.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
		for (PlayersContainer.Player.Stats.Stat type : statsTypes) {
			TextView textView = new TextView(getActivity());
			textView.setLayoutParams(new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f));
			textView.setGravity(Gravity.CENTER);
			textView.setTextAppearance(getActivity(), R.style.PlayerStats_Header_Item);
			textView.setEllipsize(TruncateAt.MARQUEE);
			if (this.playerStatsTitleAcronym.containsKey(type.toString())) {
				TitleAcronym titleAcronym = this.playerStatsTitleAcronym.get(type.toString());
				textView.setText(titleAcronym.acronym != "" ? titleAcronym.acronym : titleAcronym.title);
			} else {
				textView.setText(type.toString());
			}
			linearLayout.addView(textView);
		}
		return linearLayout;
	}

	private View createLayoutPlayerNone() {
		TextView textView = new TextView(getActivity());
		textView.setTextAppearance(getActivity(), R.style.PlayerStats_None);
		textView.setText("No players");
		return textView;
	}

	private View createLayoutPlayer(Match match, final Player player) {
		LinearLayout layoutMatchStatsPlayer = (LinearLayout) this.inflater.inflate(R.layout.layout_match_stats_player,
				null, false);

		layoutMatchStatsPlayer.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(ProfileActivity.createProfileActivity(getActivity(), player.serviceId, player.id));
			}
		});

		// Name
		((TextView) layoutMatchStatsPlayer.findViewById(R.id.matchStatsPlayerTextView)).setText(Html
				.fromHtml(player.name));

		LinearLayout matchStatsPlayerContainerLayout = (LinearLayout) layoutMatchStatsPlayer
				.findViewById(R.id.matchStatsPlayerContainerLayout);

		// Stats
		int count = 0;
		for (List<PlayersContainer.Player.Stats.Stat> statsTypes : this.serviceConfig.pages.match.showPlayerStats()) {
			StatsGroupLinearLayout layoutPlayerStats = createLayoutPlayerStats(player, statsTypes, count++);
			matchStatsPlayerContainerLayout.addView(layoutPlayerStats);
			this.statsGroupLinearLayouts.add(layoutPlayerStats);
		}

		// Image
		if (player.image != null) {
			ImageView playerImageView = (ImageView) layoutMatchStatsPlayer.findViewById(R.id.matchStatsPlayerImageView);
			Bitmap bitmapFromCache = this.handler.getBitmapFromCache(player.image);

			if (bitmapFromCache != null) {
				playerImageView.setImageBitmap(bitmapFromCache);
			} else {
				String URL = player.image;
				playerImageView.setTag(URL);
				(new DownloadImageTask(handler)).execute(playerImageView);
			}
		}

		return layoutMatchStatsPlayer;
	}

	private StatsGroupLinearLayout createLayoutPlayerStats(final PlayersContainer.Player player,
			List<PlayersContainer.Player.Stats.Stat> statsTypes, int count) {
		StatsGroupLinearLayout linearLayout = new StatsGroupLinearLayout(getActivity(), count);
		linearLayout.setWeightSum(statsTypes.size());
		linearLayout
				.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		linearLayout.setGravity(Gravity.CENTER);

		for (PlayersContainer.Player.Stats.Stat statsType : statsTypes) {
			linearLayout.addView(createLayoutPlayerStat(player, statsType));
		}
		return linearLayout;
	}

	private View createLayoutPlayerStat(final PlayersContainer.Player player,
			PlayersContainer.Player.Stats.Stat statsType) {
		LinearLayout linearLayout = new LinearLayout(getActivity());
		linearLayout
				.setLayoutParams(new TableRow.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f));
		linearLayout.setGravity(Gravity.CENTER);

		TextView textView = new TextView(getActivity());
		textView.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		textView.setGravity(Gravity.CENTER);
		textView.setTextAppearance(getActivity(), R.style.PlayerStats_Player_Stat);
		// textView.setBackground(getResources().getDrawable(R.drawable.border_bottom));
		if (player.stats.stats.containsKey(statsType)) {
			textView.setText(player.stats.stats.get(statsType));
		} else {
			textView.setText("");
		}
		linearLayout.addView(textView);

		return linearLayout;
	}

	private RelativeLayout createLayoutTeam(Match match, boolean home) {
		RelativeLayout layoutPlayerTeam = (RelativeLayout) this.inflater.inflate(R.layout.layout_player_team, null,
				false);

		if (home) {
			((TextView) layoutPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeam)).setText(R.string.team_home);
			((TextView) layoutPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore)).setText(String
					.valueOf(match.scoreHome.score));
			if (match.scoreHome.score > match.scoreAway.score) {
				((TextView) layoutPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore))
						.setBackground(getResources().getDrawable(R.drawable.server_score_tile_win));
			} else if (match.scoreHome.score < match.scoreAway.score) {
				((TextView) layoutPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore))
						.setBackground(getResources().getDrawable(R.drawable.server_score_tile_lose));
			} else {
				((TextView) layoutPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore))
						.setBackground(getResources().getDrawable(R.drawable.server_score_tile_draw));
			}

			if (match.scoreHome.score == -1)
				layoutPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore).setVisibility(View.INVISIBLE);
		} else {
			((TextView) layoutPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeam)).setText(R.string.team_away);
			((TextView) layoutPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore)).setText(String
					.valueOf(match.scoreAway.score));
			if (match.scoreHome.score < match.scoreAway.score) {
				((TextView) layoutPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore))
						.setBackground(getResources().getDrawable(R.drawable.server_score_tile_win));
			} else if (match.scoreHome.score > match.scoreAway.score) {
				((TextView) layoutPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore))
						.setBackground(getResources().getDrawable(R.drawable.server_score_tile_lose));
			} else {
				((TextView) layoutPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore))
						.setBackground(getResources().getDrawable(R.drawable.server_score_tile_draw));
			}

			if (match.scoreAway.score == -1)
				layoutPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerTeamScore).setVisibility(View.INVISIBLE);

		}

		int count = 0;
		for (Player player : match.playersContainer.players) {
			if (player != null) {
				if ((home && player.team == PlayersContainer.Player.Team.Home)
						|| (!home && player.team == PlayersContainer.Player.Team.Away)) {
					count++;
				}
			}
		}
		((TextView) layoutPlayerTeam.findViewById(R.id.layoutPlayerTeamPlayerCount)).setText(String.valueOf(count));

		return layoutPlayerTeam;
	}

	// ... /CREATE

	// CLASS

	public static class StatsGroupLinearLayout extends LinearLayout {

		public int statsGroupNumber;

		public StatsGroupLinearLayout(Context context, int statsGroupNumber) {
			super(context);
			this.statsGroupNumber = statsGroupNumber;
			setVisibility(View.GONE);
		}

	}

	public static class StatsAnimationListener implements AnimationListener {
		public View viewHide;

		@Override
		public void onAnimationEnd(Animation animation) {

		}

		@Override
		public void onAnimationRepeat(Animation animation) {

		}

		@Override
		public void onAnimationStart(Animation animation) {
			if (viewHide != null)
				viewHide.setVisibility(View.GONE);
		}
	}

	private final class StatsButtonsGestureListener extends SimpleOnGestureListener implements OnTouchListener {

		private static final int SWIPE_THRESHOLD = 100;
		private static final int SWIPE_VELOCITY_THRESHOLD = 100;

		private GestureDetector gestureDetector;
		private Context context;

		public StatsButtonsGestureListener(Context context) {
			this.context = context;
			this.gestureDetector = new GestureDetector(context, this);
		}

		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			Log.d(TAG, "onFling");
			boolean result = false;
			try {
				float diffY = e2.getY() - e1.getY();
				float diffX = e2.getX() - e1.getX();
				if (Math.abs(diffX) > Math.abs(diffY)) {
					if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
						if (diffX > 0) {
							doStatsSwitch(true);
						} else {
							doStatsSwitch(false);
						}
					}
				}
			} catch (Exception exception) {
				exception.printStackTrace();
			}
			return result;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			this.gestureDetector.onTouchEvent(event);
			return true;
		}
	}

	// /CLASS

}
