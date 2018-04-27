package com.petnbu.petnbu.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class InterceptTouchEventsLayout extends FrameLayout {

    private boolean shouldInterceptEvents = false;

    public InterceptTouchEventsLayout(@NonNull Context context) {
        super(context);
    }

    public InterceptTouchEventsLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public InterceptTouchEventsLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setShouldInterceptEvents(boolean shouldInterceptEvents) {
        this.shouldInterceptEvents = shouldInterceptEvents;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(shouldInterceptEvents) {
            return true;
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }
}
