package com.vibehealth.android.ui.goals.dialogs

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vibehealth.android.R
import com.vibehealth.android.databinding.DialogGoalExplanationBinding
import com.vibehealth.android.domain.goals.GoalCalculationBreakdown
import com.vibehealth.android.ui.goals.CalculationMethodology
import com.vibehealth.android.ui.goals.WHOSourceInfo
import com.vibehealth.android.ui.goals.adapters.PersonalFactorsAdapter

/**
 * Dialog fragment for explaining WHO standards and goal calculation methodology.
 * 
 * This dialog provides transparency about how goals are calculated, shows the
 * WHO sources used, and explains the personal factors that influence the calculations.
 * Uses supportive, educational tone per Companion Principle.
 */
class GoalExplanationDialog : DialogFragment() {

    companion object {
        private const val ARG_BREAKDOWN = "breakdown"
        private const val ARG_WHO_SOURCES = "who_sources"
        private const val ARG_METHODOLOGY = "methodology"

        /**
         * Create a new instance of the dialog with calculation data.
         * 
         * @param breakdown Goal calculation breakdown
         * @param whoSources WHO source information
         * @param methodology Calculation methodology
         * @return New dialog instance
         */
        fun newInstance(
            breakdown: GoalCalculationBreakdown,
            whoSources: WHOSourceInfo,
            methodology: CalculationMethodology
        ): GoalExplanationDialog {
            return GoalExplanationDialog().apply {
                arguments = Bundle().apply {
                    // TODO: Implement proper serialization for these classes
                    // putSerializable(ARG_BREAKDOWN, breakdown)
                    // putSerializable(ARG_WHO_SOURCES, whoSources)
                    // putSerializable(ARG_METHODOLOGY, methodology)
                }
            }
        }
    }

    private var _binding: DialogGoalExplanationBinding? = null
    private val binding get() = _binding!!

    private lateinit var personalFactorsAdapter: PersonalFactorsAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogGoalExplanationBinding.inflate(layoutInflater)
        
        val breakdown = arguments?.getSerializable(ARG_BREAKDOWN) as? GoalCalculationBreakdown
        val whoSources = arguments?.getSerializable(ARG_WHO_SOURCES) as? WHOSourceInfo
        val methodology = arguments?.getSerializable(ARG_METHODOLOGY) as? CalculationMethodology

        setupDialog(breakdown, whoSources, methodology)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .setPositiveButton(R.string.got_it) { _, _ -> dismiss() }
            .setNeutralButton(R.string.learn_more) { _, _ -> openWHOWebsite(whoSources?.whoWebsite) }
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Setup the dialog content with calculation data.
     */
    private fun setupDialog(
        breakdown: GoalCalculationBreakdown?,
        whoSources: WHOSourceInfo?,
        methodology: CalculationMethodology?
    ) {
        with(binding) {
            // Set title and introduction
            textTitle.text = getString(R.string.how_your_goals_are_calculated)
            textIntroduction.text = getString(R.string.goal_explanation_introduction)

            if (breakdown != null && methodology != null) {
                setupGoalExplanations(breakdown, methodology)
                setupPersonalFactors(methodology.personalFactors)
            } else {
                showErrorState()
            }

            if (whoSources != null) {
                setupWHOSources(whoSources)
            }

            setupAccessibility()
        }
    }

    /**
     * Setup explanations for each goal type.
     */
    private fun setupGoalExplanations(
        breakdown: GoalCalculationBreakdown,
        methodology: CalculationMethodology
    ) {
        with(binding) {
            // Steps explanation
            textStepsTitle.text = getString(R.string.steps_goal_explanation_title)
            textStepsExplanation.text = methodology.stepsMethodology
            textStepsValue.text = getString(
                R.string.steps_goal_format, 
                breakdown.stepsBreakdown.finalGoal
            )

            // Calories explanation
            textCaloriesTitle.text = getString(R.string.calories_goal_explanation_title)
            textCaloriesExplanation.text = methodology.caloriesMethodology
            textCaloriesValue.text = getString(
                R.string.calories_goal_format, 
                breakdown.caloriesBreakdown.finalGoal
            )

            // Heart points explanation
            textHeartPointsTitle.text = getString(R.string.heart_points_goal_explanation_title)
            textHeartPointsExplanation.text = methodology.heartPointsMethodology
            textHeartPointsValue.text = getString(
                R.string.heart_points_goal_format, 
                breakdown.heartPointsBreakdown.finalGoal
            )
        }
    }

    /**
     * Setup personal factors that influence calculations.
     */
    private fun setupPersonalFactors(personalFactors: List<String>) {
        personalFactorsAdapter = PersonalFactorsAdapter(personalFactors)
        
        with(binding.recyclerPersonalFactors) {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = personalFactorsAdapter
        }

        binding.textPersonalFactorsTitle.text = getString(R.string.your_personal_factors)
        binding.textPersonalFactorsDescription.text = getString(R.string.personal_factors_description)
    }

    /**
     * Setup WHO source information.
     */
    private fun setupWHOSources(whoSources: WHOSourceInfo) {
        with(binding) {
            textWhoSourcesTitle.text = getString(R.string.who_sources_title)
            textWhoSourcesDescription.text = getString(R.string.who_sources_description)

            // Steps source
            textStepsSource.text = whoSources.stepsSource
            
            // Calories source
            textCaloriesSource.text = whoSources.caloriesSource
            
            // Heart points source
            textHeartPointsSource.text = whoSources.heartPointsSource

            // Last updated
            textWhoLastUpdated.text = getString(
                R.string.who_guidelines_last_updated, 
                whoSources.lastUpdated
            )

            // Setup WHO website link
            buttonWhoWebsite.setOnClickListener {
                openWHOWebsite(whoSources.whoWebsite)
            }
        }
    }

    /**
     * Show error state when data is not available.
     */
    private fun showErrorState() {
        with(binding) {
            textIntroduction.text = getString(R.string.goal_explanation_not_available)
            
            // Hide detailed sections
            layoutGoalExplanations.visibility = View.GONE
            layoutPersonalFactors.visibility = View.GONE
        }
    }

    /**
     * Setup accessibility features.
     */
    private fun setupAccessibility() {
        with(binding) {
            // Set content descriptions
            textTitle.contentDescription = getString(R.string.goal_explanation_dialog_title)
            
            // Make sections focusable for screen readers
            layoutStepsExplanation.isFocusable = true
            layoutCaloriesExplanation.isFocusable = true
            layoutHeartPointsExplanation.isFocusable = true
            
            // Set semantic headings
            androidx.core.view.ViewCompat.setAccessibilityHeading(textStepsTitle, true)
            androidx.core.view.ViewCompat.setAccessibilityHeading(textCaloriesTitle, true)
            androidx.core.view.ViewCompat.setAccessibilityHeading(textHeartPointsTitle, true)
            androidx.core.view.ViewCompat.setAccessibilityHeading(textPersonalFactorsTitle, true)
            androidx.core.view.ViewCompat.setAccessibilityHeading(textWhoSourcesTitle, true)
        }
    }

    /**
     * Open WHO website in browser.
     * 
     * @param url WHO website URL
     */
    private fun openWHOWebsite(url: String?) {
        if (url != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                // Handle case where no browser is available
                android.util.Log.w("GoalExplanationDialog", "Failed to open WHO website", e)
            }
        }
    }
}