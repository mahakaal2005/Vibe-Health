package com.vibehealth.android.ui.accessibility

import android.content.Context
import com.google.android.material.card.MaterialCardView
import com.vibehealth.android.ui.components.TripleRingView
import com.vibehealth.android.ui.dashboard.models.ProgressData
import com.vibehealth.android.ui.dashboard.models.RingType

/**
 * Enhanced accessibility manager for dashboard components
 */
class EnhancedAccessibilityManager(private val context: Context) {
    
    fun enhanceRingAccessibility(
        tripleRingView: TripleRingView, 
        progressDataList: List<ProgressData>
    ) {
        val description = buildString {
            append("Daily wellness progress rings. ")
            progressDataList.forEachIndexed { index, progressData ->
                val ringType = when (index) {
                    0 -> "Steps"
                    1 -> "Calories" 
                    2 -> "Heart Points"
                    else -> "Progress"
                }
                val percentage = (progressData.percentage * 100).toInt()
                append("$ringType: ${progressData.current.toInt()} of ${progressData.target.toInt()}, $percentage percent complete. ")
            }
        }
        tripleRingView.contentDescription = description
    }
    
    fun enhanceCardAccessibility(
        cardView: MaterialCardView,
        progressData: ProgressData,
        ringType: RingType
    ) {
        val percentage = (progressData.percentage * 100).toInt()
        val description = "${ringType.displayName}: ${progressData.current.toInt()} of ${progressData.target.toInt()}, $percentage percent complete"
        cardView.contentDescription = description
    }
}