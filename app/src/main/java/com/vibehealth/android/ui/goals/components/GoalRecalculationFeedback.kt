package com.vibehealth.android.ui.goals.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.vibehealth.android.R
import com.vibehealth.android.databinding.ComponentGoalRecalculationFeedbackBinding
import com.vibehealth.android.domain.goals.DailyGoals

/**
 * Custom view component for providing UI feedback during goal recalculation.
 * 
 * This component handles loading indicators, success confirmations, error messages,
 * and subtle animations for goal value changes following the Companion Principle.
 */
class GoalRecalculationFeedback @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ComponentGoalRecalculationFeedbackBinding
    private var currentAnimator: Animator? = null
    
    // Callback for retry actions
    var onRetryClickListener: (() -> Unit)? = null

    init {
        binding = ComponentGoalRecalculationFeedbackBinding.inflate(
            LayoutInflater.from(context), this, true
        )
        
        orientation = VERTICAL
        setupDefaultState()
        setupClickListeners()
    }

    /**
     * Show loading state during goal recalculation.
     * 
     * @param message Loading message to display
     */
    fun showLoadingState(message: String = context.getString(R.string.recalculating_goals)) {
        with(binding) {
            // Show loading components
            progressRecalculation.visibility = View.VISIBLE
            textLoadingMessage.visibility = View.VISIBLE
            textLoadingMessage.text = message
            
            // Hide other states
            layoutSuccessState.visibility = View.GONE
            layoutErrorState.visibility = View.GONE
            
            // Start loading animation
            startLoadingAnimation()
        }
        
        visibility = View.VISIBLE
        announceForAccessibility(message)
    }

    /**
     * Show success confirmation when goals are updated.
     * 
     * @param oldGoals Previous goals for comparison
     * @param newGoals Updated goals
     * @param message Success message
     */
    fun showSuccessState(
        oldGoals: DailyGoals?,
        newGoals: DailyGoals,
        message: String = context.getString(R.string.goals_updated_successfully)
    ) {
        with(binding) {
            // Hide loading state
            progressRecalculation.visibility = View.GONE
            textLoadingMessage.visibility = View.GONE
            
            // Show success state
            layoutSuccessState.visibility = View.VISIBLE
            textSuccessMessage.text = message
            
            // Hide error state
            layoutErrorState.visibility = View.GONE
            
            // Show goal changes if available
            if (oldGoals != null) {
                showGoalChanges(oldGoals, newGoals)
            }
            
            // Start success animation
            startSuccessAnimation()
        }
        
        visibility = View.VISIBLE
        announceForAccessibility(message)
        
        // Auto-hide after delay
        postDelayed({ hideWithAnimation() }, 4000)
    }

    /**
     * Show error state when recalculation fails.
     * 
     * @param errorMessage Error message to display
     * @param canRetry Whether retry option should be shown
     */
    fun showErrorState(
        errorMessage: String,
        canRetry: Boolean = true
    ) {
        with(binding) {
            // Hide loading state
            progressRecalculation.visibility = View.GONE
            textLoadingMessage.visibility = View.GONE
            
            // Hide success state
            layoutSuccessState.visibility = View.GONE
            
            // Show error state
            layoutErrorState.visibility = View.VISIBLE
            textErrorMessage.text = errorMessage
            buttonRetry.visibility = if (canRetry) View.VISIBLE else View.GONE
            
            // Start error animation
            startErrorAnimation()
        }
        
        visibility = View.VISIBLE
        announceForAccessibility(errorMessage)
    }

    /**
     * Hide the feedback component with animation.
     */
    fun hideWithAnimation() {
        currentAnimator?.cancel()
        
        val fadeOut = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f)
        fadeOut.duration = 300
        fadeOut.interpolator = AccelerateDecelerateInterpolator()
        
        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                visibility = View.GONE
                alpha = 1f
            }
        })
        
        currentAnimator = fadeOut
        fadeOut.start()
    }

    /**
     * Show a snackbar with goal update information.
     * 
     * @param message Message to show in snackbar
     * @param actionText Optional action text
     * @param actionListener Optional action listener
     */
    fun showSnackbar(
        message: String,
        actionText: String? = null,
        actionListener: (() -> Unit)? = null
    ) {
        val snackbar = Snackbar.make(this, message, Snackbar.LENGTH_LONG)
        
        if (actionText != null && actionListener != null) {
            snackbar.setAction(actionText) { actionListener() }
        }
        
        snackbar.show()
    }

    // Private helper methods

    /**
     * Setup default state and styling.
     */
    private fun setupDefaultState() {
        visibility = View.GONE
        
        with(binding) {
            // Style loading components
            textLoadingMessage.setTextAppearance(R.style.TextAppearance_VibeHealth_Body1)
            textLoadingMessage.setTextColor(
                ContextCompat.getColor(context, R.color.primary_text)
            )
            
            // Style success components
            textSuccessMessage.setTextAppearance(R.style.TextAppearance_VibeHealth_Body1)
            textSuccessMessage.setTextColor(
                ContextCompat.getColor(context, R.color.success_color)
            )
            
            // Style error components
            textErrorMessage.setTextAppearance(R.style.TextAppearance_VibeHealth_Body1)
            textErrorMessage.setTextColor(
                ContextCompat.getColor(context, R.color.error_color)
            )
        }
    }

    /**
     * Setup click listeners.
     */
    private fun setupClickListeners() {
        binding.buttonRetry.setOnClickListener {
            onRetryClickListener?.invoke()
        }
        
        binding.buttonDismiss.setOnClickListener {
            hideWithAnimation()
        }
    }

    /**
     * Start loading animation.
     */
    private fun startLoadingAnimation() {
        currentAnimator?.cancel()
        
        val pulseAnimation = ObjectAnimator.ofFloat(binding.iconLoading, "alpha", 0.3f, 1f)
        pulseAnimation.duration = 1000
        pulseAnimation.repeatCount = ValueAnimator.INFINITE
        pulseAnimation.repeatMode = ValueAnimator.REVERSE
        pulseAnimation.interpolator = AccelerateDecelerateInterpolator()
        
        currentAnimator = pulseAnimation
        pulseAnimation.start()
    }

    /**
     * Start success animation.
     */
    private fun startSuccessAnimation() {
        currentAnimator?.cancel()
        
        val scaleX = ObjectAnimator.ofFloat(binding.iconSuccess, "scaleX", 0f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.iconSuccess, "scaleY", 0f, 1.2f, 1f)
        
        scaleX.duration = 600
        scaleY.duration = 600
        scaleX.interpolator = AccelerateDecelerateInterpolator()
        scaleY.interpolator = AccelerateDecelerateInterpolator()
        
        scaleX.start()
        scaleY.start()
    }

    /**
     * Start error animation.
     */
    private fun startErrorAnimation() {
        currentAnimator?.cancel()
        
        val shake = ObjectAnimator.ofFloat(binding.iconError, "translationX", 0f, -10f, 10f, -5f, 5f, 0f)
        shake.duration = 500
        shake.interpolator = AccelerateDecelerateInterpolator()
        
        currentAnimator = shake
        shake.start()
    }

    /**
     * Show goal changes with subtle animations.
     * 
     * @param oldGoals Previous goals
     * @param newGoals Updated goals
     */
    private fun showGoalChanges(oldGoals: DailyGoals, newGoals: DailyGoals) {
        with(binding) {
            layoutGoalChanges.visibility = View.VISIBLE
            
            // Show changes for each goal type
            showGoalChange(
                textStepsChange,
                "Steps",
                oldGoals.stepsGoal,
                newGoals.stepsGoal,
                R.drawable.ic_steps
            )
            
            showGoalChange(
                textCaloriesChange,
                "Calories",
                oldGoals.caloriesGoal,
                newGoals.caloriesGoal,
                R.drawable.ic_calories
            )
            
            showGoalChange(
                textHeartPointsChange,
                "Heart Points",
                oldGoals.heartPointsGoal,
                newGoals.heartPointsGoal,
                R.drawable.ic_heart_points
            )
        }
    }

    /**
     * Show individual goal change.
     * 
     * @param textView TextView to update
     * @param goalType Type of goal (Steps, Calories, etc.)
     * @param oldValue Previous value
     * @param newValue New value
     * @param iconRes Icon resource for the goal type
     */
    private fun showGoalChange(
        textView: android.widget.TextView,
        goalType: String,
        oldValue: Int,
        newValue: Int,
        iconRes: Int
    ) {
        if (oldValue != newValue) {
            val change = newValue - oldValue
            val changeText = if (change > 0) "+$change" else change.toString()
            val changeColor = if (change > 0) R.color.success_color else R.color.warning_color
            
            textView.text = context.getString(
                R.string.goal_change_format,
                goalType,
                oldValue,
                newValue,
                changeText
            )
            textView.setTextColor(ContextCompat.getColor(context, changeColor))
            textView.visibility = View.VISIBLE
            
            // Animate the change
            animateGoalValueChange(textView)
        } else {
            textView.visibility = View.GONE
        }
    }

    /**
     * Animate goal value change with subtle effect.
     * 
     * @param view View to animate
     */
    private fun animateGoalValueChange(view: View) {
        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val slideIn = ObjectAnimator.ofFloat(view, "translationY", 20f, 0f)
        
        fadeIn.duration = 400
        slideIn.duration = 400
        fadeIn.interpolator = AccelerateDecelerateInterpolator()
        slideIn.interpolator = AccelerateDecelerateInterpolator()
        
        fadeIn.start()
        slideIn.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        currentAnimator?.cancel()
    }
}