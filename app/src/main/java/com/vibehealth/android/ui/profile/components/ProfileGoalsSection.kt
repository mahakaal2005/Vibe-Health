package com.vibehealth.android.ui.profile.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.vibehealth.android.R
import com.vibehealth.android.databinding.ComponentProfileGoalsSectionBinding
import com.vibehealth.android.domain.goals.DailyGoals
import com.vibehealth.android.ui.goals.FormattedGoals
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Custom view component for displaying goals in the profile screen.
 * 
 * This component shows goals as read-only information with calculation timestamp,
 * source attribution, and recalculation trigger following design system consistency.
 */
class ProfileGoalsSection @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ComponentProfileGoalsSectionBinding
    
    // Track the current content state for proper toggle behavior
    private enum class ContentState { GOALS, EMPTY, LOADING, ERROR }
    private var currentContentState = ContentState.EMPTY
    
    // Callback for recalculation trigger
    var onRecalculateClickListener: (() -> Unit)? = null
    var onLearnMoreClickListener: (() -> Unit)? = null

    init {
        binding = ComponentProfileGoalsSectionBinding.inflate(
            LayoutInflater.from(context), this, true
        )
        
        orientation = VERTICAL
        setupDefaultStyling()
        setupClickListeners()
        setupAccessibility()
    }

    /**
     * Display formatted goals in the profile section.
     * 
     * @param formattedGoals Formatted goals data for display
     */
    fun displayGoals(formattedGoals: FormattedGoals) {
        currentContentState = ContentState.GOALS
        
        with(binding) {
            // Hide loading indicator and restore normal appearance
            progressCalculation.visibility = View.GONE
            layoutGoalValues.alpha = 1.0f
            
            // Display goal values
            textStepsValue.text = formattedGoals.stepsGoal
            textCaloriesValue.text = formattedGoals.caloriesGoal
            textHeartPointsValue.text = formattedGoals.heartPointsGoal
            
            // Display calculation information
            textCalculationSource.text = formattedGoals.calculationSource
            textLastCalculated.text = formattedGoals.lastUpdated
            
            // Update status indicators
            updateGoalStatus(formattedGoals.isValid, formattedGoals.isFresh)
            
            // Show/hide recalculation button based on freshness
            buttonRecalculate.visibility = if (formattedGoals.isFresh) View.GONE else View.VISIBLE
            
            // Update accessibility descriptions
            updateAccessibilityDescriptions(formattedGoals)
            
            // Start in expanded state so goals are visible
            setExpanded(true)
        }
    }

    /**
     * Display raw goals data (fallback method).
     * 
     * @param goals Raw daily goals data
     */
    fun displayGoals(goals: DailyGoals) {
        currentContentState = ContentState.GOALS
        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        
        with(binding) {
            // Hide loading indicator and restore normal appearance
            progressCalculation.visibility = View.GONE
            layoutGoalValues.alpha = 1.0f
            
            textStepsValue.text = context.getString(R.string.steps_goal_format, goals.stepsGoal)
            textCaloriesValue.text = context.getString(R.string.calories_goal_format, goals.caloriesGoal)
            textHeartPointsValue.text = context.getString(R.string.heart_points_goal_format, goals.heartPointsGoal)
            
            textCalculationSource.text = goals.calculationSource.getDisplayName()
            textLastCalculated.text = context.getString(
                R.string.last_calculated_format, 
                goals.calculatedAt.format(formatter)
            )
            
            updateGoalStatus(goals.isValid, goals.isFresh)
            buttonRecalculate.visibility = if (goals.isFresh) View.GONE else View.VISIBLE
            
            // Start in expanded state so goals are visible
            setExpanded(true)
        }
    }

    /**
     * Show empty state when no goals are available.
     */
    fun showEmptyState() {
        currentContentState = ContentState.EMPTY
        
        with(binding) {
            textEmptyTitle.text = context.getString(R.string.no_goals_calculated)
            textEmptyDescription.text = context.getString(R.string.complete_profile_to_calculate_goals)
            buttonCalculateGoals.text = context.getString(R.string.calculate_my_goals)
            
            // Start in expanded state so empty state is visible, but make it collapsible
            setExpanded(true)
        }
    }

    /**
     * Show loading state while goals are being calculated.
     */
    fun showLoadingState() {
        currentContentState = ContentState.LOADING
        
        with(binding) {
            textStepsValue.text = context.getString(R.string.calculating)
            textCaloriesValue.text = context.getString(R.string.calculating)
            textHeartPointsValue.text = context.getString(R.string.calculating)
            
            textCalculationSource.text = context.getString(R.string.calculating_goals)
            textLastCalculated.text = ""
            
            // Show loading indicator
            progressCalculation.visibility = View.VISIBLE
            buttonRecalculate.visibility = View.GONE
            
            // Dim the content
            layoutGoalValues.alpha = 0.6f
            
            // Show loading state in expanded form
            setExpanded(true)
        }
    }

    /**
     * Show error state when goals cannot be loaded.
     * 
     * @param errorMessage Error message to display
     */
    fun showErrorState(errorMessage: String) {
        currentContentState = ContentState.ERROR
        
        with(binding) {
            textEmptyTitle.text = context.getString(R.string.goals_unavailable)
            textEmptyDescription.text = errorMessage
            buttonCalculateGoals.text = context.getString(R.string.try_again)
            
            // Show error styling
            iconEmptyState.setImageResource(R.drawable.ic_error)
            iconEmptyState.setColorFilter(
                ContextCompat.getColor(context, R.color.error_color)
            )
            
            // Show error state in expanded form
            setExpanded(true)
        }
    }

    /**
     * Set whether the goals section is expanded or collapsed.
     * 
     * @param expanded Whether to show expanded view
     */
    fun setExpanded(expanded: Boolean) {
        with(binding) {
            if (expanded) {
                // Show content based on current state
                when (currentContentState) {
                    ContentState.GOALS, ContentState.LOADING -> {
                        layoutGoalsContent.visibility = View.VISIBLE
                        layoutGoalDetails.visibility = View.VISIBLE
                        layoutEmptyState.visibility = View.GONE
                    }
                    ContentState.EMPTY, ContentState.ERROR -> {
                        layoutEmptyState.visibility = View.VISIBLE
                        layoutGoalsContent.visibility = View.GONE
                        layoutGoalDetails.visibility = View.GONE
                    }
                }
            } else {
                // Hide everything when collapsed
                layoutGoalsContent.visibility = View.GONE
                layoutGoalDetails.visibility = View.GONE
                layoutEmptyState.visibility = View.GONE
            }
            
            iconExpand.rotation = if (expanded) 180f else 0f
        }
    }

    // Private helper methods

    /**
     * Setup default styling following design system.
     */
    private fun setupDefaultStyling() {
        with(binding) {
            // Section header styling
            textSectionTitle.setTextAppearance(R.style.TextAppearance_VibeHealth_Headline6)
            textSectionTitle.setTextColor(
                ContextCompat.getColor(context, R.color.primary_text)
            )
            
            // Goal value styling
            textStepsValue.setTextAppearance(R.style.TextAppearance_VibeHealth_Subtitle1)
            textCaloriesValue.setTextAppearance(R.style.TextAppearance_VibeHealth_Subtitle1)
            textHeartPointsValue.setTextAppearance(R.style.TextAppearance_VibeHealth_Subtitle1)
            
            // Label styling
            textStepsLabel.setTextAppearance(R.style.TextAppearance_VibeHealth_Body2)
            textCaloriesLabel.setTextAppearance(R.style.TextAppearance_VibeHealth_Body2)
            textHeartPointsLabel.setTextAppearance(R.style.TextAppearance_VibeHealth_Body2)
            
            // Info text styling
            textCalculationSource.setTextAppearance(R.style.TextAppearance_VibeHealth_Caption)
            textLastCalculated.setTextAppearance(R.style.TextAppearance_VibeHealth_Caption)
            
            // Set colors
            textCalculationSource.setTextColor(
                ContextCompat.getColor(context, R.color.primary_color)
            )
            textLastCalculated.setTextColor(
                ContextCompat.getColor(context, R.color.secondary_text)
            )
        }
    }

    /**
     * Setup click listeners.
     */
    private fun setupClickListeners() {
        with(binding) {
            // Header click to expand/collapse
            layoutSectionHeader.setOnClickListener {
                // Check if any content is currently visible to determine expanded state
                val isExpanded = when (currentContentState) {
                    ContentState.GOALS, ContentState.LOADING -> layoutGoalsContent.visibility == View.VISIBLE
                    ContentState.EMPTY, ContentState.ERROR -> layoutEmptyState.visibility == View.VISIBLE
                }
                setExpanded(!isExpanded)
            }
            
            // Recalculate button
            buttonRecalculate.setOnClickListener {
                onRecalculateClickListener?.invoke()
            }
            
            // Calculate goals button (empty state)
            buttonCalculateGoals.setOnClickListener {
                onRecalculateClickListener?.invoke()
            }
            
            // Learn more button
            buttonLearnMore.setOnClickListener {
                onLearnMoreClickListener?.invoke()
            }
            
            // Source info click
            layoutSourceInfo.setOnClickListener {
                onLearnMoreClickListener?.invoke()
            }
        }
    }

    /**
     * Setup accessibility features.
     */
    private fun setupAccessibility() {
        with(binding) {
            // Set content descriptions
            layoutSectionHeader.contentDescription = context.getString(
                R.string.goals_section_header_description
            )
            
            textStepsValue.contentDescription = context.getString(R.string.steps_goal_description)
            textCaloriesValue.contentDescription = context.getString(R.string.calories_goal_description)
            textHeartPointsValue.contentDescription = context.getString(R.string.heart_points_goal_description)
            
            // Make expandable section accessible
            layoutSectionHeader.isFocusable = true
            layoutSectionHeader.isClickable = true
            
            // Set semantic headings
            androidx.core.view.ViewCompat.setAccessibilityHeading(textSectionTitle, true)
        }
    }

    /**
     * Update goal status indicators.
     * 
     * @param isValid Whether goals are valid
     * @param isFresh Whether goals are fresh
     */
    private fun updateGoalStatus(isValid: Boolean, isFresh: Boolean) {
        with(binding) {
            // Update validity indicator
            if (isValid) {
                indicatorStatus.setImageResource(R.drawable.ic_check_circle)
                indicatorStatus.setColorFilter(
                    ContextCompat.getColor(context, R.color.success_color)
                )
                indicatorStatus.contentDescription = context.getString(R.string.goals_valid)
            } else {
                indicatorStatus.setImageResource(R.drawable.ic_warning)
                indicatorStatus.setColorFilter(
                    ContextCompat.getColor(context, R.color.warning_color)
                )
                indicatorStatus.contentDescription = context.getString(R.string.goals_need_review)
            }
            
            // Update freshness styling
            if (!isFresh) {
                textLastCalculated.setTextColor(
                    ContextCompat.getColor(context, R.color.warning_color)
                )
                textLastCalculated.append(" • ${context.getString(R.string.needs_update)}")
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
            layoutGoalsContent.contentDescription = context.getString(
                R.string.profile_goals_accessibility_description,
                formattedGoals.stepsGoal,
                formattedGoals.caloriesGoal,
                formattedGoals.heartPointsGoal,
                formattedGoals.calculationSource
            )
        }
    }
}