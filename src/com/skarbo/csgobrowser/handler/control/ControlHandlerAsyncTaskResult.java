package com.skarbo.csgobrowser.handler.control;

public class ControlHandlerAsyncTaskResult<T> {
	private T result;
	private Exception error;

	public T getResult() {
		return result;
	}

	public boolean isError() {
		return getError() != null;
	}

	public Exception getError() {
		return error;
	}

	public ControlHandlerAsyncTaskResult(T result) {
		this.result = result;
	}

	public ControlHandlerAsyncTaskResult(Exception error) {
		this.error = error;
	}
}
