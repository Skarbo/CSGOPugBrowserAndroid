package com.skarbo.csgobrowser.handler.control;

public abstract class ControlHandlerResult<T> {

	/**
	 * @param result
	 * @return True if handle is to be removed from queue
	 */
	public abstract boolean handleResult(T result);

	/**
	 * Handle progress result
	 * 
	 * @param result
	 */
	public void handleProgress(T result) {
		handleResult(result);
	}

	/**
	 * Handle execute
	 */
	public void handleExecute() {

	}

	/**
	 * @param exception
	 * @return True if handled, false if not handled
	 */
	public boolean handleError(Exception exception) {
		return false;
	}

	/**
	 * Called when control handler is to be recalled because of an error or
	 * recued
	 */
	public void doResubHandle() {

	}
}
