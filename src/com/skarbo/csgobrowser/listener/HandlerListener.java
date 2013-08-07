package com.skarbo.csgobrowser.listener;

import com.skarbo.csgobrowser.handler.Handler;

public interface HandlerListener {

	public Handler getHandler();

	public void onUpdating();

	public void onUpdated();

	public void onRefresh();

}
