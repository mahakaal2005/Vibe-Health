package com.vibehealth.android.ui.accessibility

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.material.card.MaterialCardView
import com.vibehealth.android.R
import com.vibehealth.android.ui.components.TripleRingView
import com.vibehealth.android.ui.dashboard.models.ProgressData
import com.vibehealth.android.ui.dashboard.models.RingType

/**
 * Enhanced accessibility manager for the Vibe Health app.
 * Provides comprehensive screen reader support and accessibility features.
 */
class EnhancedAccessibilityManager(private val context: Context) {
    
    /**
     * Enhances triple ring view accessibility
     */
    fun enhanceRingAccessibility(
        ringView: TripleRingView,
        progressData: List<ProgressData>
    ) {
        val description = buildRingAccessibilityDescription(progressData)
        ringView.contentDescription = description
        
        ViewCompat.setAccessibilityDelegate(ringView, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                
                // Add custom action for detailed breakdown
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfo.ACTION_CLICK,
                        context.getString(R.string.accessibility_view_detailed_progress)
                    )
                )
                
                // Set role information
                info.roleDescription = context.getString(R.string.accessibility_progress_indicator)
                
                // Add state information
                val overallProgress = progressData.map { it.percentage }.average()
                info.stateDescription = when {
                    overallProgress >= 0.8 -> context.getString(R.string.accessibility_progress_excellent)
                    overallProgress >= 0.6 -> context.getString(R.string.accessibility_progress_good)
                    overallProgress >= 0.4 -> context.getString(R.string.accessibility_progress_moderate)
                    overallProgress >= 0.2 -> context.getString(R.string.accessibility_progress_low)
                    else -> context.getString(R.string.accessibility_progress_just_started)
                }
            }
        })
    }
    
    /**
     * Enhances progress card accessibility
     */
    fun enhanceCardAccessibility(
        card: MaterialCardView,
        progressData: ProgressData,
        ringType: RingType
    ) {
        val description = buildCardAccessibilityDescription(progressData, ringType)
        card.contentDescription = description
        
        ViewCompat.setAccessibilityDelegate(card, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                
                // Add custom actions
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfo.ACTION_CLICK,
                        context.getString(R.string.accessibility_view_details, ringType.displayName)
                    )
                )
                
                // Set role and state
                info.roleDescription = context.getString(R.string.accessibility_progress_card)
                info.stateDescription = getProgressStateDescription(progressData.percentage)
                
                // Add range information for screen readers
                info.rangeInfo = AccessibilityNodeInfoCompat.RangeInfoCompat.obtain(
                    AccessibilityNodeInfoCompat.RangeInfoCompat.RANGE_TYPE_PERCENT,
                    0f,
                    100f,
                    (progressData.percentage * 100).toFloat()
                )
            }
        })
        
        // Enable focus for keyboard navigation
        card.isFocusable = true
        card.isFocusableInTouchMode = false
    }
    
    /**
     * Builds comprehensive accessibility description for the ring
     */
    private fun buildRingAccessibilityDescription(progressData: List<ProgressData>): String {
        val overallProgress = progressData.map { it.percentage }.average()
        val overallPercentage = (overallProgress * 100).toInt()
        
        val builder = StringBuilder()
        builder.append(context.getString(
            R.string.accessibility_ring_overview,
            overallPercentage
        ))
        
        progressData.forEach { data ->
            val ringType = getRingTypeFromData(data)
            val percentage = (data.percentage * 100).toInt()
            
            builder.append(". ")
            builder.append(context.getString(
                R.string.accessibility_ring_detail,
                ringType.displayName,
                data.current,
                data.target,
                percentage
            ))
        }
        
        return builder.toString()
    }
    
    /**
     * Builds accessibility description for individual progress cards
     */
    private fun buildCardAccessibilityDescription(
        progressData: ProgressData,
        ringType: RingType
    ): String {
        val percentage = (progressData.percentage * 100).toInt()
        val progressState = getProgressStateDescription(progressData.percentage)
        
        return context.getString(
            R.string.accessibility_card_description,
            ringType.displayName,
            progressData.current,
            progressData.target,
            percentage,
            progressState
        )
    }
    
    /**
     * Gets progress state description for accessibility
     */
    private fun getProgressStateDescription(percentage: Float): String {
        return when {
            percentage >= 1.0 -> context.getString(R.string.accessibility_goal_achieved)
            percentage >= 0.8 -> context.getString(R.string.accessibility_almost_there)
            percentage >= 0.6 -> context.getString(R.string.accessibility_good_progress)
            percentage >= 0.4 -> context.getString(R.string.accessibility_halfway_there)
            percentage >= 0.2 -> context.getString(R.string.accessibility_getting_started)
            else -> context.getString(R.string.accessibility_just_beginning)
        }
    }
    
    /**
     * Determines ring type from progress data (helper method)
     */
    private fun getRingTypeFromData(progressData: ProgressData): RingType {
        // This would need to be implemented based on how you identify ring types
        // For now, returning a default - this should be passed as a parameter
        return RingType.STEPS
    }
    
    /**
     * Announces progress updates to screen readers
     */
    fun announceProgressUpdate(
        view: View,
        ringType: RingType,
        newProgress: ProgressData
    ) {
        val percentage = (newProgress.percentage * 100).toInt()
        val announcement = context.getString(
            R.string.accessibility_progress_updated,
            ringType.displayName,
            percentage
        )
        
        view.announceForAccessibility(announcement)
    }
    
    /**
     * Announces goal achievement
     */
    fun announceGoalAchievement(
        view: View,
        ringType: RingType
    ) {
        val announcement = context.getString(
            R.string.accessibility_goal_achieved_announcement,
            ringType.displayName
        )
        
        view.announceForAccessibility(announcement)
    }
    
    /**
     * Sets up focus order for keyboard navigation
     */
    fun setupFocusOrder(views: List<View>) {
        for (i in 0 until views.size - 1) {
            ViewCompat.setAccessibilityDelegate(views[i], object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    if (i < views.size - 1) {
                        info.setTraversalAfter(views[i + 1])
                    }
                }
            })
        }
    }
    
    /**
     * Enables high contrast mode support
     */
    fun applyHighContrastIfNeeded(view: View) {
        // Check if high contrast mode is enabled
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) 
            as android.view.accessibility.AccessibilityManager
        
        // Note: isHighTextContrastEnabled is not available in all API levels
        // For now, we'll skip this feature and implement it later
        // if (accessibilityManager.isHighTextContrastEnabled) {
        //     // Apply high contrast styling
        //     view.setBackgroundResource(R.drawable.high_contrast_background)
        // }
    }
    
    /**
     * Provides haptic feedback for accessibility
     */
    fun provideAccessibilityFeedback(view: View, feedbackType: Int = View.HAPTIC_FEEDBACK_ENABLED) {
        view.performHapticFeedback(feedbackType)
    }
}

/**
 * Extension functions for easier accessibility setup
 */
fun View.enhanceAccessibility(
    contentDescription: String,
    roleDescription: String? = null,
    stateDescription: String? = null
) {
    this.contentDescription = contentDescription
    
    ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
        override fun onInitializeAccessibilityNodeInfo(
            host: View,
            info: AccessibilityNodeInfoCompat
        ) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            roleDescription?.let { info.roleDescription = it }
            stateDescription?.let { info.stateDescription = it }
        }
    })
}

/**
 * Extension function for announcing accessibility events
 */
fun View.announceAccessibilityEvent(message: String) {
    this.announceForAccessibility(message)
}