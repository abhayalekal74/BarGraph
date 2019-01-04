package com.udofy.ui.view.graph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.gs.apputil.util.AppUtils;

/**
 * Created by abhayalekal on 15/03/17.
 */
public class Graph extends View {
    protected int maxRows;
    protected int maxCols;
    protected float dataScaleX;
    protected float dataScaleY;
    protected float preciseScaleX;
    protected float pixelScaleX;
    protected int threshold;
    protected int graphHeight;
    protected int graphWidth;
    protected int bottomSpace;
    protected int leftSpace;
    protected int padding4;
    protected int padding16;
    protected int padding16x2;
    protected int padding16x3;
    protected int horizAxesPadding;
    protected float rowHeight;
    protected Paint axesPaint;
    protected Paint thresholdPaint;
    protected Paint markerPaint;
    protected Paint gridPaint;
    protected Paint errPaint;
    protected Paint inactivePaint;

    public Graph(Context context) {
        super(context);
        setDefaults();
    }

    private void setDefaults() {
        padding4 = AppUtils.pxFromDp(getContext(), 4);
        padding16 = AppUtils.pxFromDp(getContext(), 16);
        padding16x2 = padding16 * 2;
        padding16x3 = padding16 * 3;
        bottomSpace = padding16;
        leftSpace = padding16x2;
        horizAxesPadding = padding16x3;
        axesPaint = new AntiAliasingPaint(getContext(), Color.GRAY);
        markerPaint = new AntiAliasingPaint(getContext(), Color.BLACK);
        errPaint = new AntiAliasingPaint(getContext(), Color.BLACK);
        gridPaint = new AntiAliasingPaint(getContext(), Color.LTGRAY);
        inactivePaint = new AntiAliasingPaint(getContext(), Color.LTGRAY);
        thresholdPaint = new AntiAliasingPaint(getContext(), Color.RED);
    }

    public Graph(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDefaults();
    }

    protected void drawMarkersX(Canvas canvas) {
        for (int xCounter = 1; xCounter <= maxCols; xCounter++) {
            canvas.drawText(String.valueOf(xCounter * dataScaleX), horizX(xCounter * pixelScaleX), horizY(0), markerPaint);
        }
    }

    protected float horizX(float x) {
        return x + padding16;
    }

    protected float horizY(float y) {
        return Math.abs(graphHeight - y - padding4);
    }

    protected void drawMarkersY(Canvas canvas) {
        for (int yCounter = 1; yCounter <= maxRows; yCounter++) {
            canvas.drawText(String.valueOf(yCounter * dataScaleY), vertX(0), vertY(yCounter * rowHeight), markerPaint);
        }
    }

    protected float vertX(float x) {
        return x + padding4;
    }

    protected float vertY(float y) {
        return Math.abs(graphHeight - y - padding16);
    }

    protected void drawAxisX(Canvas canvas) {
        canvas.drawLine(pointX(0), pointY(0), pointX(graphWidth - horizAxesPadding), pointY(0), axesPaint);
    }

    protected float pointX(float x) {
        return x + leftSpace;
    }

    protected float pointY(float y) {
        return Math.abs(graphHeight - y - bottomSpace);
    }

    protected void drawAxisY(Canvas canvas) {
        canvas.drawLine(pointX(0), pointY(0), pointX(0), pointY(graphHeight), axesPaint);
    }

    protected void drawGrid(Canvas canvas) {
        for (int counter = 1; counter <= maxRows; counter++) {
            canvas.drawLine(pointX(0), pointY(counter * rowHeight), pointX(graphWidth - horizAxesPadding), pointY(counter * rowHeight), gridPaint);
        }
    }

    protected void drawThreshold(Canvas canvas) {
        if (threshold > 0) {
            canvas.drawLine(pointX(0), pointY(threshold), pointX(graphWidth - horizAxesPadding), pointY(threshold), thresholdPaint);
        }
    }

    protected void drawError(Canvas canvas) {
        canvas.drawText("Error, insufficient data", pointX(padding16), pointY(graphHeight / 2), errPaint);
    }

    protected void clearCanvas(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
    }
}
