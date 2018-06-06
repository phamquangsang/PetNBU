package com.petnbu.petnbu.views

import android.content.Context
import android.support.v4.widget.NestedScrollView
import android.util.AttributeSet
import android.view.MotionEvent

class CustomNestedScrollView : NestedScrollView {

    private var xDistance: Float = 0f
    private var yDistance: Float = 0f
    private var lastX: Float = 0f
    private var lastY: Float = 0f

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                yDistance = 0f
                xDistance = yDistance
                lastX = ev.x
                lastY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val curX = ev.x
                val curY = ev.y
                xDistance += Math.abs(curX - lastX)
                yDistance += Math.abs(curY - lastY)
                lastX = curX
                lastY = curY
                if (xDistance > yDistance)
                    return false
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
}
