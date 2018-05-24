package com.petnbu.petnbu.views;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

public class RatioImageView extends AppCompatImageView {

    private float dx = 1.0f;
    private float dy = 1.0f;

    public RatioImageView(Context context) {
        super(context);
    }

    public RatioImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RatioImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        setMeasuredDimension((int) (width * dx), (int) (width * dy));
    }

    public void set(float dx, float dy) {
        this.dx = dx;
        this.dy = dy;
    }
}
