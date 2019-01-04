package com.udofy.ui.view.graph;

import android.content.Context;
import android.graphics.Paint;

import com.gs.apputil.util.AppUtils;

/**
 * Created by abhayalekal on 17/03/17.
 */
class AntiAliasingPaint extends Paint {
    AntiAliasingPaint(Context context, int color) {
        super();
        setAntiAlias(true);
        if (context != null) {
            setTextSize(AppUtils.pxFromDp(context, 8));
            setStrokeWidth(AppUtils.pxFromDp(context, 1));
        }
        setColor(color);
    }
}
