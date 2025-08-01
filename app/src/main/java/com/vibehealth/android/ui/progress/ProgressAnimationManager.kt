package com.vibehealth.android.ui.progress

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd
import com.vibehealth.android.ui.components.BasicProgressGraph

/**
 * ProgressAnimationManager - Handles supportive animations for progress views
 * 
 * This class manages all animations in the progress history view following the
 * Motion System Philosophy with 150-300ms timing, purposeful and celebratory
 * animations that feel supportive and encouraging rather than demanding.
 * 
 * Features:
 * - Motion System compliance (150-300ms timing)
 * - DecelerateInterpolator for calm, supportive easing
 * - Hardware-accelerated animations for 60fps performance
 * - Celebratory feedback for achievements
 * - Reduced motion support for accessibility
 */
class ProgressAnimationManager {
    
    companion object {
        private const val ANIMATION_DURATION_SHORT = 200L // Within 150-300ms guideline
        private const val ANIMATION_DURATION_MEDIUM = 250L // Within 150-300ms guideline
        private const val ANIMATION_DURATION_LONG = 300L // Maximum Motion System timing
        
        private const val CELEBRATION_SCALE_FACTOR = 1.05f
        private const val GENTLE_TRANSLATION_DISTANCE = 30f
    }
    
    private val activeAnimators = mutableListOf<ValueAnimator>()
    private var isReducedMotionEnabled = false
    
    /**
     * Sets reduced motion preference for accessibility
     */
    fun setReducedMotionEnabled(enabled: Boolean) {
        isReducedMotionEnabled = enabled
    }
    
    /**
     * Animates encouraging loading state with supportive feel
     */
    fun animateEncouragingLoading(loadingIndicator: View) {
        if (isReducedMotionEnabled) {
            // Provide alternative feedback for reduced motion users
            loadingIndicator.alpha = 0.7f
            return
        }
        
        // Gentle pulsing animation that feels reassuring
        val pulseAnimator = ValueAnimator.ofFloat(0.7f, 1f).apply {
            duration = ANIMATION_DURATION_MEDIUM
            interpolator = DecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            
            addUpdateListener { animator ->
                val alpha = animator.animatedValue as Float
                loadingIndicator.alpha = alpha
            }
        }
        
        activeAnimators.add(pulseAnimator)
        pulseAnimator.start()
    }
    
    /**
     * Animates encouraging empty state with gentle, welcoming feel
     */
    fun animateEncouragingEmptyState(emptyStateContainer: View) {
        if (isReducedMotionEnabled) {
            emptyStateContainer.alpha = 1f
            return
        }
        
        // Gentle fade-in with slight upward movement
        emptyStateContainer.alpha = 0f
        emptyStateContainer.translationY = GENTLE_TRANSLATION_DISTANCE
        
        emptyStateContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(ANIMATION_DURATION_MEDIUM)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    /**
     * Animates progress graph reveal with celebratory feel
     */
    fun animateProgressGraphReveal(graphs: List<BasicProgressGraph>) {
        if (isReducedMotionEnabled) {
            graphs.forEach { it.alpha = 1f }
            return
        }
        
        graphs.forEachIndexed { index, graph ->
            // Stagger the animations for a flowing, encouraging reveal
            val delay = index * 100L
            
            graph.alpha = 0f
            graph.scaleX = 0.95f
            graph.scaleY = 0.95f
            
            graph.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(delay)
                .setDuration(ANIMATION_DURATION_MEDIUM)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }
    
    /**
     * Celebrates progress achievements with encouraging animation
     */
    fun celebrateProgressAchievements(container: ViewGroup, celebratoryMessage: String) {
        if (isReducedMotionEnabled) {
            // Provide alternative celebration for reduced motion users
            container.announceForAccessibility(celebratoryMessage)
            return
        }
        
        // Gentle celebration animation that feels encouraging
        val celebrationAnimator = ValueAnimator.ofFloat(1f, CELEBRATION_SCALE_FACTOR, 1f).apply {
            duration = ANIMATION_DURATION_LONG
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                container.scaleX = scale
                container.scaleY = scale
            }
            
            doOnEnd {
                // Announce celebration for accessibility
                container.announceForAccessibility(celebratoryMessage)
            }
        }
        
        activeAnimators.add(celebrationAnimator)
        celebrationAnimator.start()
    }
    
    /**
     * Animates supportive insights with gentle, encouraging feel
     */
    fun animateSupportiveInsights(insightsContainer: View) {
        if (isReducedMotionEnabled) {
            insightsContainer.alpha = 1f
            return
        }
        
        // Gentle slide-in from the side with fade
        insightsContainer.alpha = 0f
        insightsContainer.translationX = -GENTLE_TRANSLATION_DISTANCE
        
        insightsContainer.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(ANIMATION_DURATION_SHORT)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    /**
     * Animates supportive error state with gentle, non-alarming feel
     */
    fun animateSupportiveError(errorContainer: View) {
        if (isReducedMotionEnabled) {
            errorContainer.alpha = 1f
            return
        }
        
        // Very gentle shake that doesn't feel harsh or judgmental
        val gentleShakeAnimator = ValueAnimator.ofFloat(0f, -5f, 5f, 0f).apply {
            duration = ANIMATION_DURATION_SHORT
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animator ->
                val translationX = animator.animatedValue as Float
                errorContainer.translationX = translationX
            }
        }
        
        activeAnimators.add(gentleShakeAnimator)
        gentleShakeAnimator.start()
    }
    
    /**
     * Animates metric card with supportive hover effect
     */
    fun animateMetricCardHover(card: View, isHovered: Boolean) {
        if (isReducedMotionEnabled) return
        
        val targetElevation = if (isHovered) 8f else 2f
        val targetScale = if (isHovered) 1.02f else 1f
        
        card.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .translationZ(targetElevation)
            .setDuration(ANIMATION_DURATION_SHORT)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    /**
     * Animates achievement badge with celebratory bounce
     */
    fun animateAchievementBadge(badge: View) {
        if (isReducedMotionEnabled) {
            badge.alpha = 1f
            return
        }
        
        // Celebratory bounce that feels rewarding
        badge.scaleX = 0f
        badge.scaleY = 0f
        
        val bounceAnimator = ValueAnimator.ofFloat(0f, 1.2f, 1f).apply {
            duration = ANIMATION_DURATION_LONG
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                badge.scaleX = scale
                badge.scaleY = scale
            }
        }
        
        activeAnimators.add(bounceAnimator)
        bounceAnimator.start()
    }
    
    /**
     * Animates progress bar fill with encouraging progression
     */
    fun animateProgressBarFill(progressBar: View, targetProgress: Float) {
        if (isReducedMotionEnabled) {
            progressBar.scaleX = targetProgress
            return
        }
        
        val currentProgress = progressBar.scaleX
        val progressAnimator = ValueAnimator.ofFloat(currentProgress, targetProgress).apply {
            duration = ANIMATION_DURATION_MEDIUM
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                progressBar.scaleX = progress
            }
        }
        
        activeAnimators.add(progressAnimator)
        progressAnimator.start()
    }
    
    /**
     * Animates supportive transition between states
     */
    fun animateSupportiveStateTransition(
        fromView: View,
        toView: View,
        onTransitionComplete: (() -> Unit)? = null
    ) {
        if (isReducedMotionEnabled) {
            fromView.visibility = View.GONE
            toView.visibility = View.VISIBLE
            onTransitionComplete?.invoke()
            return
        }
        
        // Cross-fade transition that feels smooth and supportive
        fromView.animate()
            .alpha(0f)
            .setDuration(ANIMATION_DURATION_SHORT)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                fromView.visibility = View.GONE
                
                toView.alpha = 0f
                toView.visibility = View.VISIBLE
                toView.animate()
                    .alpha(1f)
                    .setDuration(ANIMATION_DURATION_SHORT)
                    .setInterpolator(DecelerateInterpolator())
                    .withEndAction {
                        onTransitionComplete?.invoke()
                    }
                    .start()
            }
            .start()
    }
    
    /**
     * Creates a supportive pulse animation for highlighting elements
     */
    fun createSupportivePulse(view: View, pulseCount: Int = 2) {
        if (isReducedMotionEnabled) return
        
        val pulseAnimator = ValueAnimator.ofFloat(1f, 1.05f, 1f).apply {
            duration = ANIMATION_DURATION_MEDIUM
            interpolator = DecelerateInterpolator()
            repeatCount = pulseCount - 1
            
            addUpdateListener { animator ->
                val scale = animator.animatedValue as Float
                view.scaleX = scale
                view.scaleY = scale
            }
        }
        
        activeAnimators.add(pulseAnimator)
        pulseAnimator.start()
    }
    
    /**
     * Cancels all active animations to prevent memory leaks
     */
    fun cancelAnimations() {
        activeAnimators.forEach { animator ->
            if (animator.isRunning) {
                animator.cancel()
            }
        }
        activeAnimators.clear()
    }
    
    /**
     * Pauses all animations (for when app goes to background)
     */
    fun pauseAnimations() {
        activeAnimators.forEach { animator ->
            if (animator.isRunning) {
                animator.pause()
            }
        }
    }
    
    /**
     * Resumes all paused animations
     */
    fun resumeAnimations() {
        activeAnimators.forEach { animator ->
            if (animator.isPaused) {
                animator.resume()
            }
        }
    }
}
/**

 * Enum for celebration types with different animation intensities
 */
enum class CelebrationType {
    MAJOR,      // Major achievements with confetti-like effects
    MODERATE,   // Moderate achievements with star-like effects  
    GENTLE      // Gentle achievements with subtle glow
}

/**
 * Enum for transition directions
 */
enum class TransitionDirection {
    FORWARD,    // Left to right transition
    BACKWARD    // Right to left transition
}

/**
 * Enum for loading phases with encouraging progression
 */
enum class LoadingPhase {
    INITIAL,            // Initial loading state
    FETCHING_DATA,      // Fetching progress data
    PROCESSING_INSIGHTS, // Processing supportive insights
    FINALIZING          // Finalizing display
}