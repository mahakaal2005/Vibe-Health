package com.vibehealth.android.ui.discover.decorations

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * VideoCardDecoration - Specialized item decoration for video cards
 * Provides distinct spacing for video grid layout
 */
class VideoCardDecoration : RecyclerView.ItemDecoration() {
    
    private val spacing = 8 // Grid spacing for video thumbnails
    
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = spacing
        outRect.right = spacing
        outRect.bottom = spacing * 2
        
        // Add top margin only for first row
        if (parent.getChildAdapterPosition(view) < 2) {
            outRect.top = spacing
        }
    }
}