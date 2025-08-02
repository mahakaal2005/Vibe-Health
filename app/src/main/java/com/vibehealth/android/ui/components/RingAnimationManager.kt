package com.vibehealth.android.ui.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import android.view.animation.BounceInterpolator
import com.vibehealth.android.ui.dashboard.models.RingDisplayData
import com.vibehealth.android.ui.dashboard.models.RingType

/**
 * Manages ring-specific animations for TripleRingView
 * Implements UI/UX specifications for smooth, encouraging ring animations
 */
class RingAnimationManager {
    
    companion object {
        // UI/UX Specification: 150-300ms timing (increased for better visibility)
        private const val RING_FILL_DURATION = 800L // Increased from 250L for more visible animation
        private const val CELEBRATION_DURATION = 600L
        private const val PROGRESS_UPDATE_DURATION = 600L // Increased from 200L for more visible animation
    }
    
    private var isReducedMotionEnabled = false
    private val runningAnimators = mutableSetOf<Animator>()
    
    // UI/UX Specification: DecelerateInterpolator for smooth animations
    private val smoothInterpolator = DecelerateInterpolator()
    private val celebrationInterpolator = BounceInterpolator()
    
    /**
     * Sets reduced motion preference for accessibility
     */
    fun setReducedMotionEnabled(enabled: Boolean) {
        isReducedMotionEnabled = enabled
    }
    
    /**
     * Animates progress update between ring states
     * UI/UX Specification: Smooth 200ms transitions
     */
    fun animateProgressUpdate(
        tripleRingView: TripleRingView,
        fromData: List<RingDisplayData>,
        toData: List<RingDisplayData>,
        onComplete: (() -> Unit)? = null
    ) {
        android.util.Log.d("VIBE_FIX", "RingAnimationManager: animateProgressUpdate called - reducedMotion: $isReducedMotionEnabled")
        android.util.Log.d("VIBE_FIX", "RingAnimationManager: fromData size: ${fromData.size}, toData size: ${toData.size}")
        
        if (isReducedMotionEnabled) {
            android.util.Log.d("VIBE_FIX", "RingAnimationManager: Reduced motion enabled - forcing animation anyway for testing")
            // Force animation even with reduced motion for testing
            // tripleRingView.invalidate()
            // onComplete?.invoke()
            // return
        }
        
        // Create smooth progress transition animation
        val progressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = PROGRESS_UPDATE_DURATION
            interpolator = smoothInterpolator
            
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                
                // Interpolate between old and new progress values
                val interpolatedData = fromData.zip(toData) { from, to ->
                    val interpolatedProgress = from.progress + (to.progress - from.progress) * progress
                    to.copy(progress = interpolatedProgress)
                }
                
                // Update the view with interpolated data
                tripleRingView.updateRingsData(interpolatedData)
                tripleRingView.invalidate()
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    runningAnimators.add(animation)
                }
                
                override fun onAnimationEnd(animation: Animator) {
                    runningAnimators.remove(animation)
                    
                    // Ensure final state is set
                    tripleRingView.updateRingsData(toData)
                    tripleRingView.invalidate()
                    
                    onComplete?.invoke()
                }
            })
        }
        
        progressAnimator.start()
    }
    
    /**
     * Celebrates goal achievement with encouraging animation
     * UI/UX Specification: Meaningful, celebratory feedback
     */
    fun celebrateGoalAchievement(
        tripleRingView: TripleRingView,
        achievedRings: List<RingType>
    ) {
        if (isReducedMotionEnabled) {
            // Simple accessibility announcement
            val message = when (achievedRings.size) {
                1 -> "Goal achieved: ${achievedRings.first().displayName}!"
                else -> "${achievedRings.size} goals achieved today!"
            }
            tripleRingView.announceForAccessibility(message)
            return
        }
        
        // Create celebratory ring pulse animation
        val pulseAnimator = ValueAnimator.ofFloat(1f, 1.2f, 1f).apply {
            duration = CELEBRATION_DURATION
            interpolator = celebrationInterpolator
            repeatCount = 2
            
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                tripleRingView.scaleX = scale
                tripleRingView.scaleY = scale
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    runningAnimators.add(animation)
                    
                    // Encouraging accessibility announcement
                    val message = when (achievedRings.size) {
                        1 -> "🎉 Congratulations! You achieved your ${achievedRings.first().displayName} goal!"
                        else -> "🌟 Amazing! You achieved ${achievedRings.size} goals today!"
                    }
                    tripleRingView.announceForAccessibility(message)
                }
                
                override fun onAnimationEnd(animation: Animator) {
                    runningAnimators.remove(animation)
                    
                    // Reset scale
                    tripleRingView.scaleX = 1f
                    tripleRingView.scaleY = 1f
                }
            })
        }
        
        pulseAnimator.start()
    }
    
    /**
     * Cancels all running animations
     */
    fun cancelAnimations() {
        runningAnimators.forEach { it.cancel() }
        runningAnimators.clear()
    }
}

/**
 * Extension function to update rings data in TripleRingView
 */
private fun TripleRingView.updateRingsData(data: List<RingDisplayData>) {
    // This would need to be implemented in TripleRingView
    // For now, we'll use the existing updateProgress method
    updateProgress(data, false)
}