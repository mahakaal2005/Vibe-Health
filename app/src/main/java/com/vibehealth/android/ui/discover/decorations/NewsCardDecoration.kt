package com.vibehealth.android.ui.discover.decorations

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * NewsCardDecoration - Specialized item decoration for news cards
 * Provides distinct spacing for news-focused design
 */
class NewsCardDecoration : RecyclerView.ItemDecoration() {
    
    private val spacing = 12 // Tighter spacing for news items
    
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = spacing
        outRect.right = spacing
        outRect.bottom = spacing
        
        // Add top margin only for first item
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = spacing
        }
    }
}