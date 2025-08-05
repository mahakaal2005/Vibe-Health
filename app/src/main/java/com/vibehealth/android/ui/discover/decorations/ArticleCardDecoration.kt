package com.vibehealth.android.ui.discover.decorations

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * ArticleCardDecoration - Specialized item decoration for article cards
 * Provides distinct spacing for article-focused design
 */
class ArticleCardDecoration : RecyclerView.ItemDecoration() {
    
    private val spacing = 16 // 16dp spacing following 8-point grid
    
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