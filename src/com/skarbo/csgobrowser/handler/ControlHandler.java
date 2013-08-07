package com.skarbo.csgobrowser.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.config.service.EseaServiceConfig;
import com.skarbo.csgobrowser.config.service.LeetwayServiceConfig;
import com.skarbo.csgobrowser.container.MatchesContainer;
import com.skarbo.csgobrowser.container.ProfilesContainer;
import com.skarbo.csgobrowser.container.SearchUsersContainer;
import com.skarbo.csgobrowser.container.ServersContainer;
import com.skarbo.csgobrowser.container.ServersContainer.Server;
import com.skarbo.csgobrowser.handler.control.ControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.ControlHandlerResult;
import com.skarbo.csgobrowser.handler.control.asynctask.MatchControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.asynctask.ProfileControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.asynctask.SearchUsersControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.asynctask.ServerControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.asynctask.ServerControlHandlerAsyncTask.MatchidNotGivenException;
import com.skarbo.csgobrowser.handler.control.asynctask.ServersControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.asynctask.match.EseaMatchControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.asynctask.match.LeetwayMatchControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.asynctask.profile.EseaProfileControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.asynctask.profile.LeetwayProfileControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.asynctask.server.EseaServerControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.asynctask.server.LeetwayServerControlHandlerAsyncTask;
import com.skarbo.csgobrowser.listener.HandlerListener;
import com.skarbo.csgobrowser.listener.SearchUserListener;

public class ControlHandler {

	// VARIABLES

	private static final String TAG = ControlHandler.class.getSimpleName();

	private Handler handler;
	private List<ControlHandlerAsyncTask<?>> handleQueue;
	private ControlHandlerAsyncTask<?> handlingQueue;

	private final Comparator<ControlHandlerAsyncTask<?>> handleQueueComperator = new Comparator<ControlHandlerAsyncTask<?>>() {
		@Override
		public int compare(ControlHandlerAsyncTask<?> lhs, ControlHandlerAsyncTask<?> rhs) {
			return (Integer.valueOf(lhs.getOrder())).compareTo(Integer.valueOf(rhs.getOrder())) * -1;
		}
	};

	// /VARIABLES

	public ControlHandler(Handler handler) {
		this.handleQueue = new ArrayList<ControlHandlerAsyncTask<?>>();
		this.handler = handler;
	}

	// FUNCTIONS

	// ... GET

	public Handler getHandler() {
		return handler;
	}

	public List<ServiceConfig> getServiceConfigs() {
		return getHandler().getServiceConfigs();
	}

	public Handler.Containers getContainers() {
		return getHandler().getContainers();
	}

	public Context getContext() {
		return getHandler().getContext();
	}

	// ... /GET

	// ... IS

	public boolean isUpdating() {
		return this.handlingQueue != null || !this.handleQueue.isEmpty();
	}

	// ... /IS

	// ... HANDLE

	public void handleQueue(ControlHandlerAsyncTask<?> handleAsyncTask) {
		// Add handle to queue
		if (handleAsyncTask != null) {
			if (!this.handleQueue.contains(handleAsyncTask)) {
				this.handleQueue.add(handleAsyncTask);
				Collections.sort(this.handleQueue, this.handleQueueComperator);
			} else {
				int indexOf = this.handleQueue.indexOf(handleAsyncTask);
				if (indexOf > -1)
					this.handleQueue.set(indexOf, handleAsyncTask);
			}
		}

		if (this.handlingQueue != null) {
			return;
		}

		if (!this.handleQueue.isEmpty()) {
			doNotifyUpdating();

			// Handle first in queue
			ControlHandlerAsyncTask<?> handle = this.handleQueue.remove(0);
			this.handlingQueue = handle;
			handle.getHandlerResult().handleExecute();
			handle.execute();
		} else {
			// Notify updated
			this.handler.doNotifyListeners(HandlerListener.class, new Handler.NotifyListener<HandlerListener>() {
				@Override
				public void doNotify(HandlerListener listener) {
					listener.onUpdated();
				}
			});
			this.handlingQueue = null;
		}
	}

	public void handledQueue() {
		this.handlingQueue = null;
		handleQueue(null);
	}

	// ... /HANDLE

	// ... DO

	// ... ... HANDLE

	public void doReset() {
		if (this.handlingQueue != null)
			this.handlingQueue.cancel(true);
		this.handlingQueue = null;
		this.handleQueue.clear();
		doNotifyUpdated();
	}

	public void doServers() {
		handleQueue(new ServersControlHandlerAsyncTask(this, new ControlHandlerResult<ServersContainer>() {
			@Override
			public boolean handleResult(ServersContainer result) {
				getContainers().serversContainer.merge(result);
				return true;
			}

			@Override
			public void handleProgress(ServersContainer result) {
				handleResult(result);
				doNotifyUpdating();
			}

			@Override
			public void doResubHandle() {
				doServers();
			}
		}));
	}

	public void doServer(final Server server) {
		ControlHandlerResult<ServersContainer> controlHandlerResult = new ControlHandlerResult<ServersContainer>() {
			private void doHandleServerContainer(ServersContainer serversContainer) {
				getHandler().getContainers().serversContainer.merge(serversContainer);
				// getHandler().getContainers().playersContainer.merge(serversContainer.playersContainer);
				// TODO fix?
			}

			@Override
			public boolean handleResult(ServersContainer result) {
				doHandleServerContainer(result);
				// if (!Utils.isEmpty(result.server.matchId)) {
				// doMatch(server.serviceId, result.server.matchId);
				// }
				return true;
			}

			@Override
			public void handleProgress(ServersContainer result) {
				doHandleServerContainer(result);
				doNotifyUpdating();
			}

			@Override
			public boolean handleError(Exception exception) {
				if (exception instanceof MatchidNotGivenException) {
					Toast.makeText(getContext(), "Could not retrieve server info", Toast.LENGTH_SHORT).show();
					return true;
				}
				return false;
			}

			@Override
			public void doResubHandle() {
				doServer(server);
			}
		};

		ServerControlHandlerAsyncTask<?> serverControlHandlerAsyncTask = null;
		if (server.serviceId.equalsIgnoreCase(LeetwayServiceConfig.SERVICE_ID)) {
			serverControlHandlerAsyncTask = new LeetwayServerControlHandlerAsyncTask(server, this, controlHandlerResult);
		} else if (server.serviceId.equalsIgnoreCase(EseaServiceConfig.SERVICE_ID)) {
			serverControlHandlerAsyncTask = new EseaServerControlHandlerAsyncTask(server, this, controlHandlerResult);
		}

		if (serverControlHandlerAsyncTask != null) {
			handleQueue(serverControlHandlerAsyncTask);
		}
	}

	public void doMatch(final String serviceId, final String matchId) {
		ControlHandlerResult<MatchesContainer> controlHandlerResult = new ControlHandlerResult<MatchesContainer>() {
			@Override
			public boolean handleResult(MatchesContainer result) {
				getHandler().getContainers().matchesContainer.merge(result);
				getHandler().getContainers().serversContainer.merge(result);
				return true;
			}

			@Override
			public void doResubHandle() {
				doMatch(serviceId, matchId);
			}
		};

		MatchControlHandlerAsyncTask<?> matchControlHandlerAsyncTask = null;
		if (serviceId.equalsIgnoreCase(LeetwayServiceConfig.SERVICE_ID)) {
			matchControlHandlerAsyncTask = new LeetwayMatchControlHandlerAsyncTask(matchId, this, controlHandlerResult);
		} else if (serviceId.equalsIgnoreCase(EseaServiceConfig.SERVICE_ID)) {
			matchControlHandlerAsyncTask = new EseaMatchControlHandlerAsyncTask(matchId, this, controlHandlerResult);
		}

		if (matchControlHandlerAsyncTask != null) {
			handleQueue(matchControlHandlerAsyncTask);
		}
	}

	public void doProfile(final String serviceId, final String profileId) {
		ControlHandlerResult<ProfilesContainer> controlHandlerResult = new ControlHandlerResult<ProfilesContainer>() {
			@Override
			public boolean handleResult(ProfilesContainer result) {
				getHandler().getContainers().profilesContainer.merge(result);
				getHandler().getContainers().matchesContainer.merge(result.matchesContainer);
				return true;
			}

			@Override
			public void doResubHandle() {
				doProfile(serviceId, profileId);
			}
		};

		ProfileControlHandlerAsyncTask<?> profileControlHandlerAsyncTask = null;
		if (serviceId.equalsIgnoreCase(EseaServiceConfig.SERVICE_ID))
			profileControlHandlerAsyncTask = new EseaProfileControlHandlerAsyncTask(profileId, this,
					controlHandlerResult);
		else if (serviceId.equalsIgnoreCase(LeetwayServiceConfig.SERVICE_ID))
			profileControlHandlerAsyncTask = new LeetwayProfileControlHandlerAsyncTask(profileId, this,
					controlHandlerResult);

		if (profileControlHandlerAsyncTask != null) {
			handleQueue(profileControlHandlerAsyncTask);
		}
	}

	public void doSearchUsers(final String serviceId, final String search) {
		handleQueue(new SearchUsersControlHandlerAsyncTask(serviceId, search, this,
				new ControlHandlerResult<SearchUsersContainer>() {
					@Override
					public boolean handleResult(final SearchUsersContainer result) {
						handler.doNotifyListeners(SearchUserListener.class,
								new Handler.NotifyListener<SearchUserListener>() {
									@Override
									public void doNotify(SearchUserListener listener) {
										listener.onSearchUsersResult(result);
									}
								});
						return true;
					}

					@Override
					public void handleExecute() {
						handler.doNotifyListeners(SearchUserListener.class,
								new Handler.NotifyListener<SearchUserListener>() {
									@Override
									public void doNotify(SearchUserListener listener) {
										listener.onSearchUsers(search);
									}
								});
					}
				}));
	}

	// ... ... /HANDLE

	private void doNotifyUpdated() {
		this.handler.doNotifyListeners(HandlerListener.class, new Handler.NotifyListener<HandlerListener>() {
			@Override
			public void doNotify(HandlerListener listener) {
				listener.onUpdated();
			}
		});
	}

	private void doNotifyUpdating() {
		this.handler.doNotifyListeners(HandlerListener.class, new Handler.NotifyListener<HandlerListener>() {
			@Override
			public void doNotify(HandlerListener listener) {
				listener.onUpdating();
			}
		});
	}

	// ... /DO

	// /FUNCTIONS

	// CLASS

	// /CLASS

}
