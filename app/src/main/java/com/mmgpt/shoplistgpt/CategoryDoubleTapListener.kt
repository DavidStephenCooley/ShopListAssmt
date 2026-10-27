package com.mmgpt.shoplistgpt

import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

/**
 * Reports the adapter position of a row that was double-tapped.
 *
 * Registered as an OnItemTouchListener so it sees every touch, including
 * those on child views that consume them (e.g. a row's checkbox). The callback
 * fires when the second tap lifts, not when it lands, so that changing the
 * ViewPager page doesn't happen while the finger is still down; otherwise the
 * pager receives the rest of the gesture, cancels its own scroll and can
 * settle on the wrong page.
 */
class CategoryDoubleTapListener(
    private val recyclerView: RecyclerView,
    private val onDoubleTap: (position: Int) -> Unit
) : RecyclerView.SimpleOnItemTouchListener() {

    private var pendingPosition = RecyclerView.NO_POSITION

    private val detector = GestureDetector(
        recyclerView.context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val child = recyclerView.findChildViewUnder(e.x, e.y)
                pendingPosition = child?.let { recyclerView.getChildAdapterPosition(it) }
                    ?: RecyclerView.NO_POSITION
                return true
            }

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_UP -> {
                        val position = pendingPosition
                        pendingPosition = RecyclerView.NO_POSITION
                        if (position != RecyclerView.NO_POSITION) onDoubleTap(position)
                    }
                    MotionEvent.ACTION_CANCEL -> pendingPosition = RecyclerView.NO_POSITION
                }
                return true
            }
        }
    )

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        detector.onTouchEvent(e)
        return false
    }
}
