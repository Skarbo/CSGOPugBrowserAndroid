package com.skarbo.csgobrowser.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;

import com.skarbo.csgobrowser.container.ServersContainer;

public class StatusFilterCheckBox extends CheckBox {
	public ServersContainer.Server.Status status;

	public StatusFilterCheckBox(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
}
