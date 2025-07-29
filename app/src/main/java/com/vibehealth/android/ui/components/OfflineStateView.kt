package com.vibehealth.android.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.vibehealth.android.databinding.ViewOfflineStateBinding

/**
 * Custom offline state view following design system specifications
 * Provides consistent offline messaging with retry functionality
 */
class OfflineStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private val binding: ViewOfflineStateBinding
    private var retryAction: (() -> Unit)? = null
    
    init {
        binding = ViewOfflineStateBinding.inflate(LayoutInflater.from(context), this, true)
        orientation = VERTICAL
        setupRetryButton()
    }
    
    /**
     * Set up retry button click listener
     */
    private fun setupRetryButton() {
        binding.retryButton.setOnClickListener {
            retryAction?.invoke()
            hideOfflineState()
        }
    }
    
    /**
     * Show offline state with retry action
     */
    fun showOfflineState(
        message: String = "You're offline. Please check your internet connection.",
        retryAction: (() -> Unit)? = null
    ) {
        this.retryAction = retryAction
        binding.offlineMessage.text = message
        binding.retryButton.visibility = if (retryAction != null) View.VISIBLE else View.GONE
        
        visibility = View.VISIBLE
        
        // Animate appearance
        alpha = 0f
        animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }
    
    /**
     * Hide offline state
     */
    fun hideOfflineState() {
        animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                visibility = View.GONE
                alpha = 1f
            }
            .start()
    }
    
    /**
     * Update retry action
     */
    fun setRetryAction(action: () -> Unit) {
        this.retryAction = action
        binding.retryButton.visibility = View.VISIBLE
    }
}