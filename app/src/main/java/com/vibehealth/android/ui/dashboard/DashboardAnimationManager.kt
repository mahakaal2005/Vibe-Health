package com.vibehealth.android.ui.dashboard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.card.MaterialCardView
import com.vibehealth.android.ui.components.TripleRingView
import com.vibehealth.android.ui.dashboard.models.RingDisplayData

/**
 * Centralized animation state manager for the dashboard.
 * Handles entrance animations, state transitions, and lifecycle management.
 */
class DashboardAnimationManager : DefaultLifecycleObserver {
    
    // Animation states
    enum class AnimationState {
        INITIAL,
        ENTRANCE_STARTED,
        ENTRANCE_COMPLETE,
        UPDATE_IN_PROGRESS,
        IDLE
    }
    
    private var animationState = AnimationState.INITIAL
    private val activeAnimations = mutableSetOf<Animator>()
    private val animatorPool = mutableListOf<ValueAnimator>()
    
    // Animation configuration
    companion object {
        private const val ENTRANCE_DURATION = 400L
        private const val CARD_STAGGER_DELAY = 50L
        private const val RING_FILL_DURATION = 600L
        private const val CARD_ENTRANCE_DURATION = 250L
        private const val SCALE_ANIMATION_DURATION = 200L
    }
    
    /**
     * Determines if entrance animations should be played
     */
    fun shouldAnimateEntrance(): Boolean = animationState == AnimationState.INITIAL
    
    /**
     * Marks entrance animation as started
     */
    fun markEntranceStarted() {
        animationState = AnimationState.ENTRANCE_STARTED
    }
    
    /**
     * Marks entrance animation as complete
     */
    fun markEntranceComplete() {
        animationState = AnimationState.ENTRANCE_COMPLETE
    }
    
    /**
     * Animates the triple ring entrance with spring effect
     */
    fun animateRingEntrance(
        ringView: TripleRingView,
        onComplete: () -> Unit = {}
    ) {
        if (!shouldAnimateEntrance()) {
            onComplete()
            return
        }
        
        markEntranceStarted()
        
        // Initial state
        ringView.alpha = 0f
        ringView.scaleX = 0.7f
        ringView.scaleY = 0.7f
        
        // Entrance animation
        val animator = ringView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ENTRANCE_DURATION)
            .setInterpolator(OvershootInterpolator(1.2f))
            .withEndAction {
                markEntranceComplete()
                onComplete()
            }
        
        trackAnimation(animator)
    }
    
    /**
     * Animates ring progress fill-up with staggered delays
     */
    fun animateRingFillUp(
        ringView: TripleRingView,
        targetData: List<RingDisplayData>
    ) {
        val staggerDelays = listOf(0L, 200L, 400L) // Steps: 0ms, Heart: 200ms, Cal: 400ms
        
        targetData.forEachIndexed { index, ringData ->
            val delay = staggerDelays.getOrElse(index) { 0L }
            
            val animator = getRecycledAnimator().apply {
                setFloatValues(0f, 1f)
                duration = RING_FILL_DURATION
                startDelay = delay
                interpolator = android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f)
                
                addUpdateListener { animation ->
                    val progress = animation.animatedValue as Float
                    val currentData = ringView.getRingsData().toMutableList()
                    
                    if (index < currentData.size) {
                        currentData[index] = currentData[index].copy(
                            progress = ringData.progress * progress
                        )
                        ringView.updateProgress(currentData, false)
                    }
                }
                
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (index == targetData.size - 1) {
                            ringView.updateProgress(targetData, false)
                        }
                        recycleAnimator(this@apply)
                    }
                })
            }
            
            trackAnimation(animator)
            animator.start()
        }
    }
    
    /**
     * Animates card entrance with staggered delays
     */
    fun animateCardEntrance(
        cards: List<View>,
        onComplete: () -> Unit = {}
    ) {
        if (!shouldAnimateEntrance()) {
            // Ensure cards are visible for subsequent updates
            cards.forEach { card ->
                card.alpha = 1f
                card.translationX = 0f
            }
            onComplete()
            return
        }
        
        var completedAnimations = 0
        
        cards.forEachIndexed { index, card ->
            // Initial state
            card.alpha = 0f
            card.translationX = 100f
            
            val animator = card.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(CARD_ENTRANCE_DURATION)
                .setStartDelay(index * CARD_STAGGER_DELAY)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    completedAnimations++
                    if (completedAnimations == cards.size) {
                        onComplete()
                    }
                }
            
            trackAnimation(animator)
        }
    }
    
    /**
     * Animates card press interaction
     */
    fun animateCardPress(card: MaterialCardView) {
        card.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }
    
    /**
     * Animates card release interaction
     */
    fun animateCardRelease(card: MaterialCardView) {
        card.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(150)
            .setInterpolator(OvershootInterpolator(0.5f))
            .start()
    }
    
    /**
     * Animates card hierarchy emphasis
     */
    fun animateCardEmphasis(
        card: MaterialCardView,
        isHighestProgress: Boolean
    ) {
        val targetScale = if (isHighestProgress) 1.02f else 1f
        val targetElevation = if (isHighestProgress) 12f else 6f
        
        card.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(SCALE_ANIMATION_DURATION)
            .start()
        
        card.cardElevation = targetElevation * card.context.resources.displayMetrics.density
    }
    
    /**
     * Gets a recycled animator from the pool or creates a new one
     */
    private fun getRecycledAnimator(): ValueAnimator {
        return animatorPool.removeFirstOrNull() ?: ValueAnimator()
    }
    
    /**
     * Recycles an animator back to the pool
     */
    private fun recycleAnimator(animator: ValueAnimator) {
        animator.removeAllListeners()
        animator.removeAllUpdateListeners()
        animator.cancel()
        animatorPool.add(animator)
    }
    
    /**
     * Tracks an active animation for lifecycle management
     */
    private fun trackAnimation(animator: Any) {
        when (animator) {
            is Animator -> activeAnimations.add(animator)
            is android.view.ViewPropertyAnimator -> {
                // ViewPropertyAnimator doesn't extend Animator, handle separately
                // We'll track it through the view's tag for cleanup
            }
        }
    }
    
    /**
     * Resets animation state (useful for testing or manual resets)
     */
    fun resetAnimationState() {
        animationState = AnimationState.INITIAL
    }
    
    /**
     * Checks if any animations are currently running
     */
    fun hasActiveAnimations(): Boolean = activeAnimations.isNotEmpty()
    
    // Lifecycle management
    override fun onPause(owner: LifecycleOwner) {
        activeAnimations.forEach { animator ->
            if (animator.isRunning) {
                animator.pause()
            }
        }
    }
    
    override fun onResume(owner: LifecycleOwner) {
        activeAnimations.forEach { animator ->
            if (animator.isPaused) {
                animator.resume()
            }
        }
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        // Cancel all active animations
        activeAnimations.forEach { animator ->
            animator.cancel()
        }
        activeAnimations.clear()
        
        // Clear animator pool
        animatorPool.forEach { animator ->
            animator.cancel()
        }
        animatorPool.clear()
        
        // Reset state
        animationState = AnimationState.INITIAL
    }
}

/**
 * Card emphasis levels for visual hierarchy
 */
sealed class CardEmphasis {
    object Primary : CardEmphasis()    // Highest progress - 12dp elevation
    object Secondary : CardEmphasis()  // Medium progress - 8dp elevation  
    object Tertiary : CardEmphasis()   // Lower progress - 4dp elevation
    
    fun getElevation(): Float = when (this) {
        is Primary -> 12f
        is Secondary -> 8f
        is Tertiary -> 4f
    }
    
    fun getScale(): Float = when (this) {
        is Primary -> 1.02f
        is Secondary -> 1.01f
        is Tertiary -> 1f
    }
}