package com.udofy.ui.view.graph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.arasthel.asyncjob.AsyncJob;
import com.gs.apputil.ui.view.TextViewProximaNovaRegular;
import com.gs.apputil.util.AppUtils;
import com.udofy.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by abhayalekal on 17/03/17.
 */
public class BarGraph extends HorizontalScrollView {
    private static final float HEIGHT_REDUCE = 2.5f;
    private int bgHeight;
    private int bgWidth;
    private int bgRows;
    private int bgPadding4;
    private int bgPadding16;
    private int bgPadding16x2;
    private int deviceWidth;
    private float bgRowHeight;
    private float bgYMultiplier;
    private float bgScaleY;
    private float blurAlpha;
    private int focusAlpha;
    private int scrollFromRight;
    private int activeIndex = -1;
    private boolean pagingActive;
    private boolean activeInteraction;
    private boolean scrollListenerEnabled;
    private ArrayList<AntiAliasingPaint> bgSplitPaints;
    private ArrayList<BarGraphData> bgData;
    private RelativeLayout bgContainer;
    private BarClickHandler barClickHandler;
    private Bar[] bars;
    private View[] activeDots;
    private View[] markers;
    private PaginationListener paginationListener;
    private int newBarsAdded;
    private Timer focusChangeDetectTimer;
    private AntiAliasingPaint noAttemptsPaint;

    public BarGraph(Context context) {
        super(context);
        defaults();
    }

    private void defaults() {
        bgRows = 6;
        noAttemptsPaint = new AntiAliasingPaint(getContext(), Color.LTGRAY);
        bgContainer = new RelativeLayout(getContext());
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        deviceWidth = displayMetrics.widthPixels;
        bgRowHeight = (float) (displayMetrics.ydpi / (2.54 * HEIGHT_REDUCE));
        bgPadding4 = AppUtils.pxFromDp(getContext(), 4);
        bgPadding16 = bgPadding4 * 4;
        bgPadding16x2 = bgPadding16 * 2;
        focusAlpha = 1;
        blurAlpha = 0.6f;
        bgHeight = (int) (bgRowHeight * (bgRows + 1)) + bgPadding16x2 + bgPadding4;
        setSplitPaints(new int[]{Color.RED, Color.GREEN, Color.YELLOW});
        setHorizontalScrollBarEnabled(false);
        bgContainer.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, bgHeight));
        addView(bgContainer);
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    activeInteraction = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    activeInteraction = false;
                }
                return false;
            }
        });

        getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            private static final long INTERVAL = 5;
            ScrollWatcher scrollWatcher;

            @Override
            public void onScrollChanged() {
                if (scrollListenerEnabled) {
                    if (pagingActive && paginationListener != null && getScrollX() < deviceWidth) {
                        paginationListener.fetchPrevious();
                    }
                    if (focusChangeDetectTimer == null) {
                        focusChangeDetectTimer = new Timer();
                        scrollWatcher = new ScrollWatcher(getScrollX());
                        focusChangeDetectTimer.schedule(scrollWatcher, INTERVAL, INTERVAL);
                    } else {
                        if (scrollWatcher != null) {
                            try {
                                scrollWatcher.setX(getScrollX());
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            private void focusBar() {
                final int barWidth = getResources().getDimensionPixelOffset(R.dimen.bar_width) + getResources().getDimensionPixelOffset(R.dimen.bar_margin) * 2;
                int xPos = getScrollX() - bgPadding16x2;
                final int barFocusIndex = xPos / barWidth;
                AsyncJob.doOnMainThread(new AsyncJob.OnMainThreadJob() {
                    @Override
                    public void doInUIThread() {
                        new Handler().post(new Runnable() {
                            @Override
                            public void run() {
                                if (barFocusIndex == 0) {
                                    smoothScrollTo(0, 0);
                                } else {
                                    smoothScrollTo(barFocusIndex * barWidth + bgPadding16x2, 0);
                                }
                                activeInteraction = false;
                            }
                        });
                    }
                });
            }

            class ScrollWatcher extends TimerTask {
                int x;
                long lastModified;

                ScrollWatcher(int scrollX) {
                    x = scrollX;
                    lastModified = System.currentTimeMillis();
                }

                void setX(int x) {
                    this.x = x;
                    lastModified = System.currentTimeMillis();
                }

                @Override
                public void run() {
                    if (x == getScrollX() && System.currentTimeMillis() - lastModified > INTERVAL && !activeInteraction) {
                        focusBar();
                        if (focusChangeDetectTimer != null) {
                            focusChangeDetectTimer.cancel();
                            focusChangeDetectTimer = null;
                        }
                        if (scrollWatcher != null) {
                            scrollWatcher.cancel();
                            scrollWatcher = null;
                        }
                    }
                }
            }
        });
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public BarGraph setSplitPaints(int[] colors) {
        bgSplitPaints = new ArrayList<>();
        for (int color : colors) {
            bgSplitPaints.add(new AntiAliasingPaint(getContext(), color));
        }
        return this;
    }

    public BarGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
        defaults();
    }

    public BarGraph setPagingActive(boolean pagingActive) {
        this.pagingActive = pagingActive;
        return this;
    }

    public BarGraph setPaginationListener(PaginationListener paginationListener) {
        this.paginationListener = paginationListener;
        return this;
    }

    public BarGraph setBarClickHandler(BarClickHandler barClickHandler) {
        this.barClickHandler = barClickHandler;
        return this;
    }

    public BarGraph setRows(int bgRows) {
        this.bgRows = bgRows;
        return this;
    }

    public BarGraph setData(List<BarGraphData> data) {
        if (bgData == null) {
            bgData = new ArrayList<>();
        }
        float yRange = 0;
        if (data != null) {
            newBarsAdded = 0;
            for (int index = 0; index < data.size(); index++) {
                BarGraphData barGraphData = data.get(index);
                if (bgData.contains(barGraphData)) {
                    continue;
                }
                newBarsAdded++;
                String[] dateSplits = barGraphData.date.split("-");
                String year = dateSplits[0];
                String month = dateSplits[1];
                String day = dateSplits[2];
                barGraphData.rowHeader = day + " " + AppUtils.months[Integer.parseInt(month) - 1];
                if (index == data.size() - 1) {
                    String format = AppUtils.getTodaysDate();
                    if (format.equals(barGraphData.date)) {
                        barGraphData.bottomMarker = getContext().getString(R.string.Today);
                        barGraphData.rowHeader = getContext().getString(R.string.Today);
                    } else {
                        sundayOrDate(barGraphData, year, month, day);
                    }
                } else {
                    sundayOrDate(barGraphData, year, month, day);
                }
                barGraphData.ranges = new float[]{barGraphData.correct, barGraphData.total - barGraphData.correct};
                for (int counter = 1; counter < barGraphData.ranges.length; counter++) {
                    barGraphData.ranges[counter] += barGraphData.ranges[counter - 1];
                }
                if (barGraphData.ranges[barGraphData.ranges.length - 1] > yRange) {
                    yRange = barGraphData.ranges[barGraphData.ranges.length - 1];
                }
                bgData.add(index, barGraphData);
            }
            barScaleY(yRange);
            barGraphWidth();
        }
        return this;
    }

    private void sundayOrDate(BarGraphData barGraphData, String year, String month, String day) {
        Calendar cal = new GregorianCalendar(Integer.parseInt(year), Integer.parseInt(month) - 1, Integer.parseInt(day));
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek == Calendar.SUNDAY) {
            barGraphData.bottomMarker = "Sun";
        } else {
            barGraphData.bottomMarker = day + "/" + month;
        }
    }

    private void barScaleY(float yRange) {
//        yRange *= 1.5f;
        float scale = barScale(yRange / bgRows, bgRows);
        if (scale > bgScaleY) {
            bgScaleY = scale;
            multipliers();
        }
    }

    private void barGraphWidth() {
        int barWidth = getResources().getDimensionPixelOffset(R.dimen.bar_margin) * 2 + getResources().getDimensionPixelOffset(R.dimen.bar_width);
        bgWidth = Math.max(bgData.size() * barWidth + bgPadding16x2 + spaceWidth(), deviceWidth);
    }

    private float barScale(float scale, int count) {
        return ((scale / count) + 1) * count;
    }

    private void multipliers() {
        bgYMultiplier = bgRowHeight / bgScaleY;
    }

    private int spaceWidth() {
        int barWidth = getResources().getDimensionPixelOffset(R.dimen.bar_margin) * 2 + getResources().getDimensionPixelOffset(R.dimen.bar_width);
        if (barWidth * bgData.size() < deviceWidth / 2) {
            return deviceWidth / 2 - getResources().getDimensionPixelOffset(R.dimen.bar_margin) - getResources().getDimensionPixelOffset(R.dimen.bar_width) / 2;
        } else {
            return deviceWidth / 2 - getResources().getDimensionPixelOffset(R.dimen.bar_margin) * 2 - getResources().getDimensionPixelOffset(R.dimen.bar_width) / 2;
        }
    }

    public void build(boolean addingPage) {
        if (bgData != null && bgData.size() > 0) {
            draw(addingPage);
        } else {
            AppUtils.showToastAtTheBottom(getContext(), "Insufficient data to draw graph!");
        }
    }

    private void draw(boolean addingPage) {
        if (addingPage && getScrollX() > 0) {
            scrollFromRight = bgWidth - getScrollX();
        }
        if (bgContainer != null) {
            bgContainer.removeAllViews();
        }
        bgContainer.addView(new Grid(getContext()));
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setLayoutParams(new RelativeLayout.LayoutParams(bgWidth, bgHeight));
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setPadding(bgPadding16, 0, 0, 0);
        linearLayout.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
        bars = new Bar[bgData.size()];
        activeDots = new View[bgData.size()];
        markers = new View[bgData.size()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(bgPadding4, bgPadding4);
        for (int counter = 0; counter < bgData.size(); counter++) {
            BarGraphData barGraphData = bgData.get(counter);
            LinearLayout barLayout = new LinearLayout(getContext());
            barLayout.setGravity(Gravity.CENTER_HORIZONTAL);
            barLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            barLayout.setOrientation(LinearLayout.VERTICAL);
            Bar bar = new Bar(getContext(), barGraphData, counter, counter < newBarsAdded);
            bars[counter] = bar;
            barLayout.addView(bar);
            TextView marker = new TextViewProximaNovaRegular(getContext());
            marker.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, bgPadding16));
            marker.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
            marker.setPadding(0, bgPadding4, 0, 0);
            marker.setTextColor(getResources().getColor(R.color.post_text_color));
            marker.setText(barGraphData.bottomMarker);
            marker.setAlpha(blurAlpha);
            barLayout.addView(marker);
            markers[counter] = marker;
            View activeDot = new View(getContext());
            activeDot.setLayoutParams(layoutParams);
            barLayout.addView(activeDot);
            activeDots[counter] = activeDot;
            linearLayout.addView(barLayout);
        }
        barSelected(bgData.size() - 1);
        View spaceView = new View(getContext());
        spaceView.setLayoutParams(new ViewGroup.LayoutParams(spaceWidth(), bgHeight));
        linearLayout.addView(spaceView);
        bgContainer.addView(linearLayout);
        invalidate();
        requestLayout();
        scrollListenerEnabled = false;
        if (newBarsAdded == bgData.size()) {
            // Passes only the first time data is added. Paging data will not trigger this.
            setVisibility(INVISIBLE);
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    fullScroll(FOCUS_RIGHT);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            setVisibility(View.VISIBLE);
                            scrollListenerEnabled = true;
                        }
                    }, 200);
                }
            });
        } else {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    scrollTo(bgWidth - scrollFromRight, 0);
                    scrollFromRight = 0;
                    scrollListenerEnabled = true;
                }
            });
        }
        newBarsAdded = 0;
    }

    private void barSelected(int index) {
        if (index < bgData.size()) {
            if (activeIndex > -1 && activeIndex < bars.length) {
                bars[activeIndex].selected(false);
                activeDots[activeIndex].setBackgroundDrawable(null);
                markers[activeIndex].setAlpha(blurAlpha);
                bars[activeIndex].invalidate();
                bars[activeIndex].requestLayout();
            }
            activeIndex = index;
            bars[activeIndex].selected(true);
            markers[activeIndex].setAlpha(focusAlpha);
            activeDots[activeIndex].setBackgroundDrawable(getResources().getDrawable(R.drawable.selected_bar));
            bars[activeIndex].invalidate();
            bars[activeIndex].requestLayout();
            if (barClickHandler != null) {
                barClickHandler.onClick(bgData.get(index));
            }
            centerActiveBar();
        }
    }

    private void centerActiveBar() {
        final int barWidth = getResources().getDimensionPixelOffset(R.dimen.bar_width) + getResources().getDimensionPixelOffset(R.dimen.bar_margin) * 2;
        int curScrollX = getScrollX();
        float touchX = (activeIndex + 1) * barWidth + bgPadding16x2;
        if (touchX < deviceWidth - spaceWidth() + bgPadding16x2) {
            smoothScrollTo(0, 0);
        } else {
            int targetX = curScrollX + deviceWidth - spaceWidth();
            if (touchX > targetX) {
                setScrollX((int) (curScrollX + (touchX - targetX)));
            } else {
                setScrollX((int) (curScrollX - (targetX - touchX)));
            }
        }
    }

    private Paint getSplitPaint(int index) {
        if (index < bgSplitPaints.size()) {
            return bgSplitPaints.get(index);
        } else {
            AntiAliasingPaint antiAliasingPaint = new AntiAliasingPaint(getContext(), getRandColor());
            bgSplitPaints.add(antiAliasingPaint);
            return antiAliasingPaint;
        }
    }

    protected static int getRandColor() {
        Random random = new Random();
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return Color.rgb(r, g, b);
    }

    public void clearData() {
        if (bgData != null) {
            bgData.clear();
        }
    }

    public void build() {
        build(false);
    }

    public interface BarClickHandler {
        void onClick(BarGraphData barGraphData);
    }

    public interface PaginationListener {
        void fetchPrevious();
    }

    private class Bar extends Graph {
        private float[] ranges;
        private RectCoordinates rectCoordinates;
        private float barWidth;
        private boolean animate;
        private int index;
        private boolean noAttempts;

        public Bar(Context context, BarGraphData barGraphData, int index, boolean animate) {
            super(context);
            this.ranges = barGraphData.ranges;
            defaults();
            this.animate = animate;
            this.index = index;
            selected(index == bgData.size() - 1);
        }

        private void defaults() {
            barWidth = getResources().getDimensionPixelOffset(R.dimen.bar_width);
            graphHeight = bgHeight;
            rectCoordinates = new RectCoordinates(ranges[ranges.length - 1]);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams((int) barWidth, (int) (bgRows * bgRowHeight));
            int margin = getResources().getDimensionPixelOffset(R.dimen.bar_margin);
            layoutParams.setMargins(margin, 0, margin, 0);
            setLayoutParams(layoutParams);
            setRotation(180);
            if (ranges[ranges.length - 1] == 0) {
                noAttempts = true;
            }
            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    barSelected(index);
                }
            });
        }

        public void selected(boolean selected) {
            if (selected) {
                setAlpha(focusAlpha);
            } else {
                setAlpha(blurAlpha);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (noAttempts) {
                drawEmptyRect(canvas);
            } else {
                if (animate) {
                    drawRect(canvas);
                } else {
                    drawRectWithoutAnimation(canvas);
                }
            }
        }

        private void drawEmptyRect(Canvas canvas) {
            int left = getResources().getDimensionPixelOffset(R.dimen.bar_draw_start);
            int right = getResources().getDimensionPixelOffset(R.dimen.bar_draw_end);
            canvas.drawRect(leftX(left), 0, rightX(right), bgHeight, noAttemptsPaint);
        }

        private float leftX(float x) {
            return x;
        }

        private float rightX(float x) {
            return x;
        }

        private void drawRect(Canvas canvas) {
            if (rectCoordinates.splitCounter < ranges.length) {
                float rangeDelta = rectCoordinates.delta;
                if (rectCoordinates.top > rectCoordinates.medRange) {
                    rangeDelta *= 0.2;
                } else if (rectCoordinates.top > rectCoordinates.fastRange) {
                    rangeDelta *= 0.5;
                }
                rectCoordinates.top += rangeDelta;
                if (rectCoordinates.top < ranges[rectCoordinates.splitCounter]) {
                    float top = rectY(rectCoordinates.top);
                    float bottom = rectY(rectCoordinates.bottom);
                    canvas.drawRect(leftX(rectCoordinates.left), Math.min(top, bottom), rightX(rectCoordinates.right), Math.max(top, bottom), getSplitPaint(rectCoordinates.splitCounter));
                    if (rectCoordinates.splitCounter > 0) {
                        drawRectWithoutAnimation(canvas);
                    }
                } else {
                    rectCoordinates.splitCounter++;
                    drawRectWithoutAnimation(canvas);
                    rectCoordinates.top = ranges[rectCoordinates.splitCounter - 1];
                    rectCoordinates.bottom = rectCoordinates.top;
                }
                postInvalidateDelayed(20);
            } else {
                drawRectWithoutAnimation(canvas);
            }
        }

        private float rectY(float y) {
            return y * bgYMultiplier;
        }

        private void drawRectWithoutAnimation(Canvas canvas) {
            int size = animate ? rectCoordinates.splitCounter : ranges.length;
            for (int counter = 0; counter < size; counter++) {
                float top, bottom;
                if (counter == 0) {
                    top = ranges[0];
                    bottom = 0;
                } else {
                    top = ranges[counter];
                    bottom = ranges[counter - 1];
                }
                float topRectY = rectY(top);
                float bottomRectY = rectY(bottom);
                canvas.drawRect(leftX(rectCoordinates.left), Math.min(topRectY, bottomRectY), rightX(rectCoordinates.right), Math.max(topRectY, bottomRectY), getSplitPaint(counter));
            }
        }

        private class RectCoordinates {
            float left, top, right, bottom, delta, fastRange, medRange;
            int splitCounter;

            RectCoordinates(float range) {
                left = getResources().getDimensionPixelOffset(R.dimen.bar_draw_start);
                right = getResources().getDimensionPixelOffset(R.dimen.bar_draw_end);
                delta = 30;
                fastRange = 0.4f * range;
                medRange = fastRange + 0.25f * range;
            }
        }

    }

    private class Grid extends Graph {
        public Grid(Context context) {
            super(context);
            defaults();
        }

        private void defaults() {
            graphHeight = bgHeight;
            graphWidth = bgWidth;
            maxRows = bgRows;
            rowHeight = bgRowHeight;
            int color = getResources().getColor(R.color.addImageColor);
            axesPaint = new AntiAliasingPaint(getContext(), color);
            gridPaint = new AntiAliasingPaint(getContext(), color);
            bottomSpace = padding16 + bgPadding4;
            leftSpace = padding16;
            horizAxesPadding = padding16x2;
            setLayoutParams(new ViewGroup.LayoutParams(bgWidth, bgHeight));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            drawAxisX(canvas);
            drawGrid(canvas);
        }
    }
}
