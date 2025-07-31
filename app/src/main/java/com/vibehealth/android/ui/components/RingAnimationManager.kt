package com.vibehealth.android.ui.components

import android.animation.*
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.BounceInterpolator
import com.vibehealth.android.ui.dashboard.models.RingType
import com.vibehealth.android.ui.dashboard.models.RingDisplayData

/**
 * Animation manager for TripleRingView following UI/UX motion guidelines.
 * Implements smooth progress animations with 150-300ms timing and celebratory animations.
 */
class RingAnimationManager {
    
    companion object {
        private const val PROGRESS_ANIMATION_DURATION = 250L // 150-300ms per UI/UX spec
        private const val CELEBRATION_ANIMATION_DURATION = 1000L
        private const val REDUCED_MOTION_DURATION = 100L // For accessibility
    }
    
    private val animationConfig = RingAnimationConfig()
    private var currentAnimator: Animator? = null
    private var isReducedMotionEnabled = false
    
    /**
     * Configuration for ring animations following UI/UX specifications.
     */
    data class RingAnimationConfig(
        val duration: Long = PROGRESS_ANIMATION_DURATION,
        val interpolator: android.view.animation.Interpolator = DecelerateInterpolator(),
        val celebrationDuration: Long = CELEBRATION_ANIMATION_DURATION
    )
    
    /**
     * Sets reduced motion preference for accessibility.
     */
    fun setReducedMotionEnabled(enabled: Boolean) {
        isReducedMotionEnabled = enabled
    }
    
    /**
     * Animates progress update with smooth transitions.
     * Uses DecelerateInterpolator for natural feel per UI/UX requirements.
     */
    fun animateProgressUpdate(
        tripleRingView: TripleRingView,
        fromData: List<RingDisplayData>,
        toData: List<RingDisplayData>,
        onComplete: () -> Unit = {}
    ) {
        if (!shouldAnimate()) {
            tripleRingView.updateProgress(toData, false)
            onComplete()
            return
        }
        
        // Cancel any existing animation
        currentAnimator?.cancel()
        
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = getOptimizedDuration(animationConfig.duration)
            interpolator = animationConfig.interpolator
            
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                val interpolatedData = interpolateRingData(fromData, toData, progress)
                tripleRingView.updateProgress(interpolatedData, true)
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentAnimator = null
                    onComplete()
                }
                
                override fun onAnimationCancel(animation: Animator) {
                    currentAnimator = null
                }
            })
        }
        
        currentAnimator = animator
        animator.start()
    }
    
    /**
     * Celebrates goal achievement with purposeful and meaningful animations.
     * Uses BounceInterpolator for celebratory feel per UI/UX motion design principles.
     */
    fun celebrateGoalAchievement(
        tripleRingView: TripleRingView,
        achievedRings: List<RingType>,
        onComplete: () -> Unit = {}
    ) {
        if (!shouldAnimate() || achievedRings.isEmpty()) {
            onComplete()
            return
        }
        
        // Cancel any existing animation
        currentAnimator?.cancel()
        
        // Create celebration animation set
        val animatorSet = AnimatorSet()
        val animations = mutableListOf<Animator>()
        
        // Scale animation for celebration
        val scaleUpX = ObjectAnimator.ofFloat(tripleRingView, "scaleX", 1f, 1.1f, 1f)
        val scaleUpY = ObjectAnimator.ofFloat(tripleRingView, "scaleY", 1f, 1.1f, 1f)
        
        scaleUpX.duration = getOptimizedDuration(animationConfig.celebrationDuration)
        scaleUpY.duration = getOptimizedDuration(animationConfig.celebrationDuration)
        scaleUpX.interpolator = BounceInterpolator()
        scaleUpY.interpolator = BounceInterpolator()
        
        animations.add(scaleUpX)
        animations.add(scaleUpY)
        
        // Pulse animation for achieved rings
        val pulseAnimator = createPulseAnimation(tripleRingView, achievedRings.size)
        animations.add(pulseAnimator)
        
        animatorSet.playTogether(animations)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                currentAnimator = null
                onComplete()
            }
            
            override fun onAnimationCancel(animation: Animator) {
                currentAnimator = null
            }
        })
        
        currentAnimator = animatorSet
        animatorSet.start()
        
        // Announce achievement for accessibility
        announceAchievement(tripleRingView, achievedRings)
    }
    
    /**
     * Creates a subtle pulse animation for goal achievement.
     */
    private fun createPulseAnimation(view: View, achievedCount: Int): Animator {
        val pulseCount = when (achievedCount) {
            3 -> 3 // All goals - triple pulse
            2 -> 2 // Two goals - double pulse
            else -> 1 // Single goal - single pulse
        }
        
        val pulseAnimator = ValueAnimator.ofFloat(1f, 1.05f, 1f)
        pulseAnimator.duration = getOptimizedDuration(300L)
        pulseAnimator.repeatCount = pulseCount - 1
        pulseAnimator.interpolator = DecelerateInterpolator()
        
        pulseAnimator.addUpdateListener { animation ->
            val scale = animation.animatedValue as Float
            view.scaleX = scale
            view.scaleY = scale
        }
        
        return pulseAnimator
    }
    
    /**
     * Interpolates between two sets of ring data for smooth animation.
     */
    private fun interpolateRingData(
        fromData: List<RingDisplayData>,
        toData: List<RingDisplayData>,
        progress: Float
    ): List<RingDisplayData> {
        if (fromData.size != toData.size) return toData
        
        return fromData.mapIndexed { index, from ->
            val to = toData[index]
            val interpolatedProgress = from.progress + (to.progress - from.progress) * progress
            
            to.copy(
                progress = interpolatedProgress,
                isAnimating = true
            )
        }
    }
    
    /**
     * Checks if animations should be played based on accessibility settings.
     */
    private fun shouldAnimate(): Boolean {
        return !isReducedMotionEnabled
    }
    
    /**
     * Gets optimized animation duration based on accessibility settings.
     */
    private fun getOptimizedDuration(baseDuration: Long): Long {
        return if (isReducedMotionEnabled) {
            REDUCED_MOTION_DURATION
        } else {
            baseDuration
        }
    }
    
    /**
     * Announces goal achievement for screen readers.
     */
    private fun announceAchievement(view: View, achievedRings: List<RingType>) {
        val announcement = when (achievedRings.size) {
            3 -> "Congratulations! All three wellness goals achieved today!"
            2 -> "Great progress! Two wellness goals achieved today!"
            1 -> {
                val ringName = achievedRings.first().displayName
                "Well done! $ringName goal achieved today!"
            }
            else -> return
        }
        
        view.announceForAccessibility(announcement)
    }
    
    /**
     * Cancels any currently running animations.
     */
    fun cancelAnimations() {
        currentAnimator?.cancel()
        currentAnimator = null
    }
    
    /**
     * Checks if animations are currently running.
     */
    fun isAnimating(): Boolean {
        return currentAnimator?.isRunning == true
    }
}