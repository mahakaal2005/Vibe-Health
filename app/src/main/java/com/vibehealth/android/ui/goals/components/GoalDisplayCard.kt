package com.vibehealth.android.ui.goals.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.vibehealth.android.R
import com.vibehealth.android.databinding.ComponentGoalDisplayCardBinding
import com.vibehealth.android.domain.goals.DailyGoals
import com.vibehealth.android.ui.goals.FormattedGoals
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Custom view component for displaying daily goals with WHO source attribution.
 * 
 * This component follows the design system guidelines and provides a consistent
 * way to display calculated goals across the app with proper accessibility support.
 */
class GoalDisplayCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ComponentGoalDisplayCardBinding
    
    init {
        binding = ComponentGoalDisplayCardBinding.inflate(
            LayoutInflater.from(context), this, true
        )
        
        orientation = VERTICAL
        setupDefaultStyling()
        setupAccessibility()
    }

    /**
     * Display formatted goals with WHO source attribution.
     * 
     * @param formattedGoals Formatted goals data for display
     */
    fun displayGoals(formattedGoals: FormattedGoals) {
        with(binding) {
            // Display goal values
            textStepsGoal.text = formattedGoals.stepsGoal
            textCaloriesGoal.text = formattedGoals.caloriesGoal
            textHeartPointsGoal.text = formattedGoals.heartPointsGoal
            
            // Display calculation source and timestamp
            textCalculationSource.text = formattedGoals.calculationSource
            textSourceDescription.text = formattedGoals.sourceDescription
            textLastUpdated.text = formattedGoals.lastUpdated
            
            // Update visual indicators
            updateValidityIndicator(formattedGoals.isValid)
            updateFreshnessIndicator(formattedGoals.isFresh)
            
            // Update accessibility descriptions
            updateAccessibilityDescriptions(formattedGoals)
        }
    }

    /**
     * Display raw goals data (fallback method).
     * 
     * @param goals Raw daily goals data
     */
    fun displayGoals(goals: DailyGoals) {
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        
        with(binding) {
            textStepsGoal.text = context.getString(R.string.steps_goal_format, goals.stepsGoal)
            textCaloriesGoal.text = context.getString(R.string.calories_goal_format, goals.caloriesGoal)
            textHeartPointsGoal.text = context.getString(R.string.heart_points_goal_format, goals.heartPointsGoal)
            
            textCalculationSource.text = goals.calculationSource.getDisplayName()
            textSourceDescription.text = getSourceDescription(goals.calculationSource)
            textLastUpdated.text = context.getString(
                R.string.last_updated_format, 
                goals.calculatedAt.format(formatter)
            )
            
            updateValidityIndicator(goals.isValid())
            updateFreshnessIndicator(goals.isFresh())
        }
    }

    /**
     * Show loading state while goals are being calculated.
     */
    fun showLoadingState() {
        with(binding) {
            textStepsGoal.text = context.getString(R.string.calculating_goals)
            textCaloriesGoal.text = "..."
            textHeartPointsGoal.text = "..."
            textCalculationSource.text = context.getString(R.string.calculating)
            textSourceDescription.text = context.getString(R.string.setting_up_your_goals)
            textLastUpdated.text = ""
            
            // Show loading indicator
            progressIndicator.visibility = VISIBLE
            layoutGoalValues.alpha = 0.6f
        }
    }

    /**
     * Show error state when goals cannot be loaded.
     * 
     * @param errorMessage Error message to display
     */
    fun showErrorState(errorMessage: String) {
        with(binding) {
            textStepsGoal.text = context.getString(R.string.goals_unavailable)
            textCaloriesGoal.text = "---"
            textHeartPointsGoal.text = "---"
            textCalculationSource.text = context.getString(R.string.error)
            textSourceDescription.text = errorMessage
            textLastUpdated.text = ""
            
            // Hide loading indicator and show error styling
            progressIndicator.visibility = GONE
            layoutGoalValues.alpha = 1.0f
            cardContainer.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.error_background)
            )
        }
    }

    /**
     * Show empty state when no goals are available.
     */
    fun showEmptyState() {
        with(binding) {
            textStepsGoal.text = context.getString(R.string.no_goals_set)
            textCaloriesGoal.text = "---"
            textHeartPointsGoal.text = "---"
            textCalculationSource.text = context.getString(R.string.not_calculated)
            textSourceDescription.text = context.getString(R.string.complete_profile_for_goals)
            textLastUpdated.text = ""
            
            progressIndicator.visibility = GONE
            layoutGoalValues.alpha = 0.7f
        }
    }

    /**
     * Set click listener for the entire card.
     * 
     * @param listener Click listener
     */
    fun setOnCardClickListener(listener: OnClickListener?) {
        binding.cardContainer.setOnClickListener(listener)
    }

    /**
     * Set click listener for the WHO source information.
     * 
     * @param listener Click listener for source info
     */
    fun setOnSourceInfoClickListener(listener: OnClickListener?) {
        binding.layoutSourceInfo.setOnClickListener(listener)
    }

    // Private helper methods

    /**
     * Setup default styling following design system.
     */
    private fun setupDefaultStyling() {
        with(binding) {
            // Card styling
            cardContainer.cardElevation = resources.getDimension(R.dimen.card_elevation_default)
            cardContainer.radius = resources.getDimension(R.dimen.card_corner_radius)
            
            // Typography following design system
            textStepsGoal.setTextAppearance(R.style.TextAppearance_VibeHealth_Headline6)
            textCaloriesGoal.setTextAppearance(R.style.TextAppearance_VibeHealth_Headline6)
            textHeartPointsGoal.setTextAppearance(R.style.TextAppearance_VibeHealth_Headline6)
            
            textCalculationSource.setTextAppearance(R.style.TextAppearance_VibeHealth_Subtitle2)
            textSourceDescription.setTextAppearance(R.style.TextAppearance_VibeHealth_Body2)
            textLastUpdated.setTextAppearance(R.style.TextAppearance_VibeHealth_Caption)
            
            // Colors following design system
            textCalculationSource.setTextColor(
                ContextCompat.getColor(context, R.color.primary_text)
            )
            textSourceDescription.setTextColor(
                ContextCompat.getColor(context, R.color.secondary_text)
            )
            textLastUpdated.setTextColor(
                ContextCompat.getColor(context, R.color.tertiary_text)
            )
        }
    }

    /**
     * Setup accessibility support.
     */
    private fun setupAccessibility() {
        with(binding) {
            // Make the card focusable for accessibility
            cardContainer.isFocusable = true
            cardContainer.isClickable = true
            
            // Set content descriptions
            textStepsGoal.contentDescription = context.getString(R.string.steps_goal_description)
            textCaloriesGoal.contentDescription = context.getString(R.string.calories_goal_description)
            textHeartPointsGoal.contentDescription = context.getString(R.string.heart_points_goal_description)
            
            layoutSourceInfo.contentDescription = context.getString(R.string.goal_source_info_description)
        }
    }

    /**
     * Update validity indicator based on goal validation.
     * 
     * @param isValid Whether goals are valid
     */
    private fun updateValidityIndicator(isValid: Boolean) {
        with(binding) {
            if (isValid) {
                indicatorValidity.setImageResource(R.drawable.ic_check_circle)
                indicatorValidity.setColorFilter(
                    ContextCompat.getColor(context, R.color.success_color)
                )
                indicatorValidity.contentDescription = context.getString(R.string.goals_valid)
            } else {
                indicatorValidity.setImageResource(R.drawable.ic_warning)
                indicatorValidity.setColorFilter(
                    ContextCompat.getColor(context, R.color.warning_color)
                )
                indicatorValidity.contentDescription = context.getString(R.string.goals_need_review)
            }
        }
    }

    /**
     * Update freshness indicator based on calculation age.
     * 
     * @param isFresh Whether goals are fresh (calculated recently)
     */
    private fun updateFreshnessIndicator(isFresh: Boolean) {
        with(binding) {
            if (isFresh) {
                indicatorFreshness.setImageResource(R.drawable.ic_refresh)
                indicatorFreshness.setColorFilter(
                    ContextCompat.getColor(context, R.color.success_color)
                )
                indicatorFreshness.contentDescription = context.getString(R.string.goals_up_to_date)
            } else {
                indicatorFreshness.setImageResource(R.drawable.ic_refresh_needed)
                indicatorFreshness.setColorFilter(
                    ContextCompat.getColor(context, R.color.warning_color)
                )
                indicatorFreshness.contentDescription = context.getString(R.string.goals_need_update)
            }
        }
    }

    /**
     * Update accessibility descriptions with current goal values.
     * 
     * @param formattedGoals Current formatted goals
     */
    private fun updateAccessibilityDescriptions(formattedGoals: FormattedGoals) {
        with(binding) {
            cardContainer.contentDescription = context.getString(
                R.string.goal_card_accessibility_description,
                formattedGoals.stepsGoal,
                formattedGoals.caloriesGoal,
                formattedGoals.heartPointsGoal,
                formattedGoals.calculationSource
            )
        }
    }

    /**
     * Get source description for calculation source.
     * 
     * @param source Calculation source
     * @return Human-readable description
     */
    private fun getSourceDescription(source: com.vibehealth.android.domain.goals.CalculationSource): String {
        return when (source) {
            com.vibehealth.android.domain.goals.CalculationSource.WHO_STANDARD -> 
                context.getString(R.string.who_standard_description)
            com.vibehealth.android.domain.goals.CalculationSource.FALLBACK_DEFAULT -> 
                context.getString(R.string.fallback_default_description)
            com.vibehealth.android.domain.goals.CalculationSource.USER_ADJUSTED -> 
                context.getString(R.string.user_adjusted_description)
        }
    }
}