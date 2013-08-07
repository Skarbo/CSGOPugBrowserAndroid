package com.skarbo.csgobrowser.handler.control.asynctask;

import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.container.MatchesContainer;
import com.skarbo.csgobrowser.handler.ControlHandler;
import com.skarbo.csgobrowser.handler.control.ControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.ControlHandlerResult;

public abstract class MatchControlHandlerAsyncTask<T extends ServiceConfig> extends
		ControlHandlerAsyncTask<MatchesContainer> {

	public static final String TAG = MatchControlHandlerAsyncTask.class.getSimpleName();

	private T serviceConfig;
	private String serviceId;
	private String matchId;

	public MatchControlHandlerAsyncTask(String serviceId, String matchId, ControlHandler controlHandler,
			ControlHandlerResult<MatchesContainer> handlerResult) {
		super(controlHandler, handlerResult);

		this.serviceId = serviceId;
		this.matchId = matchId;
		for (ServiceConfig serviceConfig : getServiceConfigs()) {
			if (serviceConfig.id.equalsIgnoreCase(this.serviceId))
				this.serviceConfig = (T) serviceConfig;
		}
	}

	// ... GET

	@Override
	public String getKey() {
		return TAG;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	public T getServiceConfig() {
		return serviceConfig;
	}

	public String getServiceId() {
		return serviceId;
	}

	public String getMatchId() {
		return matchId;
	}

	// ... /GET

	// ... DO

	@Override
	public MatchesContainer doHandle(Void[] params) throws Exception {
		if (this.serviceId == null)
			throw new Exception("Service id not given");
		if (this.matchId == null)
			throw new Exception("Match id not given");
		if (this.serviceConfig == null)
			throw new Exception("Service config not given");

		return doHandleMatch();
	}

	public abstract MatchesContainer doHandleMatch() throws Exception;

	// ... /DO

}
