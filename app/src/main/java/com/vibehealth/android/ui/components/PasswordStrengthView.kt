package com.vibehealth.android.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.vibehealth.android.R
import com.vibehealth.android.core.validation.PasswordStrength
import com.vibehealth.android.databinding.ViewPasswordStrengthBinding

/**
 * Custom view for displaying password strength with visual indicators
 * Follows design system specifications for colors and animations
 */
class PasswordStrengthView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private val binding: ViewPasswordStrengthBinding
    
    init {
        binding = ViewPasswordStrengthBinding.inflate(LayoutInflater.from(context), this, true)
        orientation = VERTICAL
        visibility = View.GONE
    }
    
    /**
     * Update password strength display with smooth animations
     */
    fun updatePasswordStrength(strength: PasswordStrength) {
        when (strength.level) {
            PasswordStrength.Level.NONE -> {
                visibility = View.GONE
            }
            else -> {
                visibility = View.VISIBLE
                updateStrengthIndicator(strength.level)
                updateStrengthText(strength.level)
                updateFeedback(strength.feedback)
                
                // Animate the appearance
                alpha = 0f
                animate()
                    .alpha(1f)
                    .setDuration(150)
                    .start()
            }
        }
    }
    
    /**
     * Update the visual strength indicator bars
     */
    private fun updateStrengthIndicator(level: PasswordStrength.Level) {
        val strengthBars = listOf(
            binding.strengthBar1,
            binding.strengthBar2,
            binding.strengthBar3,
            binding.strengthBar4
        )
        
        val (activeCount, color) = when (level) {
            PasswordStrength.Level.VERY_WEAK -> 1 to R.color.error
            PasswordStrength.Level.WEAK -> 2 to R.color.warning
            PasswordStrength.Level.MEDIUM -> 3 to R.color.warning
            PasswordStrength.Level.STRONG -> 4 to R.color.sage_green
            PasswordStrength.Level.VERY_STRONG -> 4 to R.color.success
            else -> 0 to R.color.text_secondary
        }
        
        strengthBars.forEachIndexed { index, bar ->
            val isActive = index < activeCount
            bar.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    if (isActive) color else R.color.text_secondary
                )
            )
            
            // Animate bar activation
            bar.animate()
                .scaleY(if (isActive) 1f else 0.3f)
                .setDuration(100)
                .setStartDelay((index * 50).toLong())
                .start()
        }
    }
    
    /**
     * Update the strength text label
     */
    private fun updateStrengthText(level: PasswordStrength.Level) {
        val (text, color) = when (level) {
            PasswordStrength.Level.VERY_WEAK -> "Very Weak" to R.color.error
            PasswordStrength.Level.WEAK -> "Weak" to R.color.warning
            PasswordStrength.Level.MEDIUM -> "Medium" to R.color.warning
            PasswordStrength.Level.STRONG -> "Strong" to R.color.sage_green
            PasswordStrength.Level.VERY_STRONG -> "Very Strong" to R.color.success
            else -> "" to R.color.text_secondary
        }
        
        binding.strengthText.text = text
        binding.strengthText.setTextColor(ContextCompat.getColor(context, color))
    }
    
    /**
     * Update feedback suggestions
     */
    private fun updateFeedback(feedback: List<String>) {
        if (feedback.isEmpty()) {
            binding.feedbackText.visibility = View.GONE
        } else {
            binding.feedbackText.visibility = View.VISIBLE
            binding.feedbackText.text = feedback.joinToString(" • ")
            
            // Fade in feedback
            binding.feedbackText.alpha = 0f
            binding.feedbackText.animate()
                .alpha(1f)
                .setDuration(200)
                .setStartDelay(100)
                .start()
        }
    }
}