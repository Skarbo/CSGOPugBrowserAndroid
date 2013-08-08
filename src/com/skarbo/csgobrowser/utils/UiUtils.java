package com.skarbo.csgobrowser.utils;

import android.graphics.drawable.Drawable;
import android.view.View;

public class UiUtils {

    public static void setViewBackground(View view, Drawable drawable) {
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable);
        }
    }
}
