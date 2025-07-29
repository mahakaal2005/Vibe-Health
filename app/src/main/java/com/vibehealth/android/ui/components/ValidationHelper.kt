package com.vibehealth.android.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.animation.AnimationUtils
import com.google.android.material.textfield.TextInputLayout
import com.vibehealth.android.R
import com.vibehealth.android.domain.auth.ValidationResult

/**
 * Helper class for applying validation feedback to UI components
 * Follows design system specifications for error handling and animations
 */
class ValidationHelper(private val context: Context) {
    
    /**
     * Apply validation result to TextInputLayout with animations
     */
    fun applyValidationToField(
        inputLayout: TextInputLayout,
        validationResult: ValidationResult,
        showSuccessIndicator: Boolean = false
    ) {
        if (!validationResult.isValid) {
            // Show error with animation
            inputLayout.error = validationResult.errorMessage
            inputLayout.isErrorEnabled = true
            
            // Apply error shake animation
            inputLayout.startAnimation(
                AnimationUtils.loadAnimation(context, R.anim.field_error_shake)
            )
            
            // Change box stroke error color to error color
            inputLayout.boxStrokeErrorColor = ColorStateList.valueOf(context.getColor(R.color.error))
            
        } else {
            // Clear error
            inputLayout.error = null
            inputLayout.isErrorEnabled = false
            
            // Show success indicator if requested
            if (showSuccessIndicator) {
                // You could add a success icon or green border here
                inputLayout.boxStrokeColor = context.getColor(R.color.success)
            } else {
                // Reset to default color
                inputLayout.boxStrokeColor = context.getColor(R.color.sage_green)
            }
        }
    }
    
    /**
     * Show error message with fade-in animation
     */
    fun showErrorMessage(errorView: View, message: String, textSetter: (String) -> Unit) {
        textSetter(message)
        
        if (errorView.visibility != View.VISIBLE) {
            errorView.visibility = View.VISIBLE
            errorView.startAnimation(
                AnimationUtils.loadAnimation(context, R.anim.error_fade_in)
            )
        }
    }
    
    /**
     * Hide error message with fade-out animation
     */
    fun hideErrorMessage(errorView: View) {
        if (errorView.visibility == View.VISIBLE) {
            errorView.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction {
                    errorView.visibility = View.GONE
                    errorView.alpha = 1f
                }
                .start()
        }
    }
    
    /**
     * Apply field focus transition animation
     */
    fun applyFocusTransition(inputLayout: TextInputLayout, hasFocus: Boolean) {
        val duration = 100L
        
        if (hasFocus) {
            // Focus animation - subtle elevation increase
            inputLayout.animate()
                .translationZ(2f)
                .setDuration(duration)
                .start()
        } else {
            // Unfocus animation - return to normal elevation
            inputLayout.animate()
                .translationZ(0f)
                .setDuration(duration)
                .start()
        }
    }
    
    /**
     * Show success feedback with checkmark animation
     */
    fun showSuccessFeedback(view: View) {
        view.visibility = View.VISIBLE
        view.startAnimation(
            AnimationUtils.loadAnimation(context, R.anim.success_checkmark)
        )
        
        // Auto-hide after 2 seconds
        view.postDelayed({
            hideErrorMessage(view)
        }, 2000)
    }
    
    /**
     * Apply network error styling and retry mechanism
     */
    fun showNetworkError(
        errorView: View,
        textSetter: (String) -> Unit,
        retryAction: () -> Unit
    ) {
        val message = "Please check your internet connection and try again"
        showErrorMessage(errorView, message, textSetter)
        
        // Add retry functionality (this could be enhanced with a retry button)
        errorView.setOnClickListener {
            hideErrorMessage(errorView)
            retryAction()
        }
    }
    
    /**
     * Create validation debouncer for real-time validation
     */
    fun createValidationDebouncer(
        delayMs: Long = 300,
        validationAction: () -> Unit
    ): () -> Unit {
        var validationRunnable: Runnable? = null
        
        return {
            validationRunnable?.let { 
                // Remove previous validation if exists
            }
            
            validationRunnable = Runnable {
                validationAction()
            }
            
            // Post delayed validation
            android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(validationRunnable!!, delayMs)
        }
    }
}