package com.skarbo.csgobrowser.handler;

import com.skarbo.csgobrowser.utils.RestClient;

public abstract class ServiceHandler {

	// FUNCTIONS

	// ... GET

	public abstract String getId();

	// ... /GET

	// ... HANDLE

	public abstract RestClient.Response handleServers();

	// ... /HANDLE

	// /FUNCTIONS

}
