package com.vibehealth.android.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.vibehealth.android.R
import com.vibehealth.android.databinding.ViewLoadingStateBinding

/**
 * Custom loading state view following design system specifications
 * Provides consistent loading indicators with sage green colors and proper animations
 */
class LoadingStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val binding: ViewLoadingStateBinding
    
    init {
        binding = ViewLoadingStateBinding.inflate(LayoutInflater.from(context), this, true)
        setupLoadingIndicator()
    }
    
    /**
     * Set up loading indicator with design system colors
     */
    private fun setupLoadingIndicator() {
        binding.progressIndicator.indeterminateTintList = 
            ContextCompat.getColorStateList(context, R.color.sage_green)
    }
    
    /**
     * Show loading state with message
     */
    fun showLoading(message: String = "Loading...") {
        binding.loadingMessage.text = message
        visibility = View.VISIBLE
        
        // Animate appearance
        alpha = 0f
        animate()
            .alpha(1f)
            .setDuration(150)
            .start()
    }
    
    /**
     * Hide loading state
     */
    fun hideLoading() {
        animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction {
                visibility = View.GONE
                alpha = 1f
            }
            .start()
    }
    
    /**
     * Show loading with custom progress size
     */
    fun showLoadingWithSize(message: String = "Loading...", size: LoadingSize) {
        binding.loadingMessage.text = message
        
        val progressSize = when (size) {
            LoadingSize.SMALL -> resources.getDimensionPixelSize(R.dimen.progress_small)
            LoadingSize.MEDIUM -> resources.getDimensionPixelSize(R.dimen.progress_medium)
            LoadingSize.LARGE -> resources.getDimensionPixelSize(R.dimen.progress_large)
        }
        
        binding.progressIndicator.layoutParams = binding.progressIndicator.layoutParams.apply {
            width = progressSize
            height = progressSize
        }
        
        showLoading(message)
    }
    
    enum class LoadingSize {
        SMALL, MEDIUM, LARGE
    }
}