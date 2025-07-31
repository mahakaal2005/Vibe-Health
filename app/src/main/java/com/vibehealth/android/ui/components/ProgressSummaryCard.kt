package com.vibehealth.android.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.vibehealth.android.R
import com.vibehealth.android.databinding.ComponentProgressSummaryCardBinding
import com.vibehealth.android.ui.dashboard.models.ProgressData
import com.vibehealth.android.ui.dashboard.models.RingType

/**
 * MetricSummaryCard component from UI/UX specification.
 * Displays detailed progress information for a single wellness metric.
 * 
 * Supports all system states: loading, empty, partial, ideal, and error.
 */
class ProgressSummaryCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private val binding: ComponentProgressSummaryCardBinding
    
    init {
        binding = ComponentProgressSummaryCardBinding.inflate(
            LayoutInflater.from(context), this, true
        )
        
        orientation = VERTICAL
        setupAccessibility()
    }
    
    /**
     * Updates the card with progress data.
     */
    fun updateProgress(progressData: ProgressData) {
        showLoadedState()
        
        // Set ring type icon and label
        val iconRes = when (progressData.ringType) {
            RingType.STEPS -> R.drawable.ic_directions_walk
            RingType.CALORIES -> R.drawable.ic_local_fire_department
            RingType.HEART_POINTS -> R.drawable.ic_favorite
        }
        
        binding.ringTypeIcon.setImageResource(iconRes)
        binding.ringTypeIcon.setColorFilter(progressData.progressColor)
        binding.ringTypeLabel.text = progressData.ringType.displayName.uppercase()
        
        // Set progress values
        binding.currentValue.text = progressData.getCurrentValueString()
        binding.targetValue.text = progressData.getTargetValueString()
        binding.unitLabel.text = progressData.ringType.unit
        binding.progressPercentage.text = "${progressData.getPercentageInt()}%"
        binding.progressPercentage.setTextColor(progressData.progressColor)
        
        // Set progress bar
        binding.progressBar.progress = progressData.getPercentageInt()
        binding.progressBar.progressTintList = 
            ContextCompat.getColorStateList(context, android.R.color.transparent)?.apply {
                binding.progressBar.progressDrawable.setTint(progressData.progressColor)
            }
        
        // Show achievement indicator if goal is achieved
        if (progressData.isGoalAchieved) {
            binding.achievementIndicator.visibility = VISIBLE
            binding.achievementText.text = "Goal achieved!"
        } else {
            binding.achievementIndicator.visibility = GONE
        }
        
        // Update accessibility
        updateAccessibilityDescription(progressData)
    }
    
    /**
     * Shows loading state.
     */
    fun showLoadingState() {
        binding.loadingState.visibility = VISIBLE
        binding.errorState.visibility = GONE
        hideMainContent()
    }
    
    /**
     * Shows error state.
     */
    fun showErrorState(errorMessage: String = "Data unavailable") {
        binding.errorState.visibility = VISIBLE
        binding.errorText.text = errorMessage
        binding.loadingState.visibility = GONE
        hideMainContent()
    }
    
    /**
     * Shows loaded state with data.
     */
    private fun showLoadedState() {
        binding.loadingState.visibility = GONE
        binding.errorState.visibility = GONE
        showMainContent()
    }
    
    /**
     * Hides main content during loading/error states.
     */
    private fun hideMainContent() {
        binding.currentValue.visibility = GONE
        binding.progressBar.visibility = GONE
        binding.achievementIndicator.visibility = GONE
    }
    
    /**
     * Shows main content when data is loaded.
     */
    private fun showMainContent() {
        binding.currentValue.visibility = VISIBLE
        binding.progressBar.visibility = VISIBLE
    }
    
    /**
     * Sets up accessibility support.
     */
    private fun setupAccessibility() {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
    }
    
    /**
     * Updates accessibility description with current progress data.
     */
    private fun updateAccessibilityDescription(progressData: ProgressData) {
        contentDescription = progressData.getAccessibilityDescription()
    }
}