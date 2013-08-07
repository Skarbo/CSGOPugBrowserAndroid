package com.skarbo.csgobrowser.handler.control.asynctask;

import com.skarbo.csgobrowser.config.ServiceConfig;
import com.skarbo.csgobrowser.container.ProfilesContainer;
import com.skarbo.csgobrowser.handler.ControlHandler;
import com.skarbo.csgobrowser.handler.control.ControlHandlerAsyncTask;
import com.skarbo.csgobrowser.handler.control.ControlHandlerResult;

public abstract class ProfileControlHandlerAsyncTask<T extends ServiceConfig> extends
		ControlHandlerAsyncTask<ProfilesContainer> {

	public static final String TAG = ProfileControlHandlerAsyncTask.class.getSimpleName();

	private T serviceConfig;
	private String serviceId;
	private String profileId;

	public ProfileControlHandlerAsyncTask(String serviceId, String profileId, ControlHandler controlHandler,
			ControlHandlerResult<ProfilesContainer> handlerResult) {
		super(controlHandler, handlerResult);

		this.serviceId = serviceId;
		this.profileId = profileId;
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

	public String getProfileId() {
		return profileId;
	}

	// ... /GET

	// ... DO

	@Override
	public ProfilesContainer doHandle(Void[] params) throws Exception {
		if (this.serviceId == null)
			throw new Exception("Service id not given");
		if (this.profileId == null)
			throw new Exception("Profile id not given");
		if (this.serviceConfig == null)
			throw new Exception("Service config not given");

		return doHandleProfile();
	}

	public abstract ProfilesContainer doHandleProfile() throws Exception;

	// ... /DO

}
