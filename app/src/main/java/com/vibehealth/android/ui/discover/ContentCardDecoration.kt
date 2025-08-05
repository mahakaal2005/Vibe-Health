package com.vibehealth.android.ui.discover

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * ContentCardDecoration - Proper spacing for content cards
 * Following 8-point grid system and Material Design 3 patterns
 */
class ContentCardDecoration : RecyclerView.ItemDecoration() {
    
    private val verticalSpacing = 16 // 8-point grid: 2 * 8dp
    
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        
        // Add top margin for all items except the first
        if (position > 0) {
            outRect.top = verticalSpacing
        }
        
        // Add bottom margin for the last item
        if (position == (parent.adapter?.itemCount ?: 0) - 1) {
            outRect.bottom = verticalSpacing
        }
    }
}