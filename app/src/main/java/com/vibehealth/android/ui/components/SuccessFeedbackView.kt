package com.vibehealth.android.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.vibehealth.android.databinding.ViewSuccessFeedbackBinding

/**
 * Custom success feedback view following design system specifications
 * Provides celebratory but subtle success animations and messaging
 */
class SuccessFeedbackView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private val binding: ViewSuccessFeedbackBinding
    
    init {
        binding = ViewSuccessFeedbackBinding.inflate(LayoutInflater.from(context), this, true)
        orientation = VERTICAL
        visibility = View.GONE
    }
    
    /**
     * Show success feedback with celebratory animation
     */
    fun showSuccess(
        message: String = "Success!",
        autoHide: Boolean = true,
        hideDelayMs: Long = 2000
    ) {
        binding.successMessage.text = message
        visibility = View.VISIBLE
        
        // Celebratory but subtle animation
        alpha = 0f
        scaleX = 0.8f
        scaleY = 0.8f
        
        animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(250)
            .withStartAction {
                // Animate checkmark icon
                binding.successIcon.alpha = 0f
                binding.successIcon.scaleX = 0.5f
                binding.successIcon.scaleY = 0.5f
                binding.successIcon.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setStartDelay(100)
                    .start()
            }
            .start()
        
        // Auto-hide if requested
        if (autoHide) {
            postDelayed({
                hideSuccess()
            }, hideDelayMs)
        }
    }
    
    /**
     * Hide success feedback
     */
    fun hideSuccess() {
        animate()
            .alpha(0f)
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(200)
            .withEndAction {
                visibility = View.GONE
                alpha = 1f
                scaleX = 1f
                scaleY = 1f
            }
            .start()
    }
    
    /**
     * Show success with custom icon
     */
    fun showSuccessWithIcon(
        message: String,
        iconResId: Int,
        autoHide: Boolean = true
    ) {
        binding.successIcon.setImageResource(iconResId)
        showSuccess(message, autoHide)
    }
}