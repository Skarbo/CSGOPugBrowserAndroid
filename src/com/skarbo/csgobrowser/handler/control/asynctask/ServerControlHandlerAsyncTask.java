package com.skarbo.csgobrowser.handler.control.asynctask;

import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.container.ServersContainer;
import com.skarbo.csgobrowser.container.ServersContainer.Server;
import com.skarbo.csgobrowser.handler.ControlHandler;
import com.skarbo.csgobrowser.handler.control.ControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.ControlHandlerResult;

public abstract class ServerControlHandlerAsyncTask<T extends ServiceConfig> extends
		ControlHandlerAsyncTask<ServersContainer> {

	public static final String TAG = ServerControlHandlerAsyncTask.class.getSimpleName();

	private Server server;
	private T serviceConfig;

	public ServerControlHandlerAsyncTask(ServersContainer.Server server, ControlHandler controlHandler,
			ControlHandlerResult<ServersContainer> handlerResult) {
		super(controlHandler, handlerResult);

		this.server = server;
		for (ServiceConfig serviceConfig : getServiceConfigs()) {
			if (serviceConfig.id.equalsIgnoreCase(this.server.serviceId))
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

	public Server getServer() {
		return server;
	}

	// ... /GET

	// ... DO

	@Override
	public ServersContainer doHandle(Void[] params) throws Exception {
		if (this.server == null)
			throw new Exception("Server not given");
		if (this.serviceConfig == null)
			throw new Exception("Service config not given");

		return doHandleServer();
	}

	public abstract ServersContainer doHandleServer() throws Exception;

	// ... /DO

	// ... CLASS

	public static class MatchidNotGivenException extends Exception {

	}

	// ... /CLASS

}
