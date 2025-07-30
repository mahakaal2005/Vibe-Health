package com.vibehealth.android.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.vibehealth.android.R
import com.vibehealth.android.databinding.FragmentOnboardingWelcomeBinding
import com.vibehealth.android.domain.onboarding.OnboardingStep
import com.vibehealth.android.core.accessibility.AccessibilityHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Welcome screen fragment following design system specifications
 */
@AndroidEntryPoint
class WelcomeFragment : Fragment() {

    private var _binding: FragmentOnboardingWelcomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()
    
    @Inject
    lateinit var accessibilityHelper: AccessibilityHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupAccessibility()
        setupResponsiveLayout()
        setupUI()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupUI() {
        // Set progress indicator using proper string formatting
        binding.tvProgressIndicator.text = getString(
            R.string.step_indicator,
            OnboardingStep.WELCOME.stepNumber + 1,
            OnboardingStep.WELCOME.totalSteps
        )
        
        // Set step indicator
        binding.tvStepIndicator.text = getString(R.string.step_indicator)
    }

    private fun setupClickListeners() {
        binding.btnGetStarted.setOnClickListener {
            viewModel.navigateToNextStep()
        }
    }

    private fun observeViewModel() {
        // Observe navigation events
        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OnboardingNavigationEvent.NavigateForward -> {
                    // Navigation will be handled by the parent activity
                }
                else -> {
                    // Handle other navigation events if needed
                }
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnGetStarted.isEnabled = !isLoading
        }
    }

    /**
     * Set up accessibility features for WCAG 2.1 Level AA compliance
     */
    private fun setupAccessibility() {
        // Set content descriptions
        accessibilityHelper.setContentDescription(
            binding.logo,
            "Vibe Health logo",
            "Welcome to Vibe Health"
        )
        
        accessibilityHelper.setContentDescription(
            binding.welcomeHeading,
            "Welcome heading",
            "Welcome to Vibe Health"
        )
        
        accessibilityHelper.setContentDescription(
            binding.subtitle,
            "Welcome subtitle",
            "Let's personalize your wellness journey"
        )
        
        accessibilityHelper.setContentDescription(
            binding.btnGetStarted,
            "Get started button",
            "Tap to begin onboarding"
        )
        
        // Ensure minimum touch targets
        accessibilityHelper.ensureMinimumTouchTarget(binding.btnGetStarted)
        
        // Set up keyboard navigation
        accessibilityHelper.setupKeyboardNavigation(binding.root as ViewGroup)
        
        // Set up focus indicators
        accessibilityHelper.setupFocusIndicators(binding.btnGetStarted)
        
        // Apply dynamic font scaling
        accessibilityHelper.applyDynamicFontScaling(binding.welcomeHeading, 24f)
        accessibilityHelper.applyDynamicFontScaling(binding.subtitle, 16f)
        
        // Apply high contrast if needed
        accessibilityHelper.applyHighContrastIfNeeded(binding.welcomeHeading)
        accessibilityHelper.applyHighContrastIfNeeded(binding.subtitle)
    }
    
    /**
     * Set up responsive layout for different screen sizes
     */
    private fun setupResponsiveLayout() {
        // Standard Android phone layout - no responsive adjustments needed
        // Welcome screen doesn't have input fields, so no special keyboard handling needed
    }

    override fun onDestroyView() {
        // Cancel any pending operations to prevent crashes
        _binding?.root?.removeCallbacks(null)
        
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): WelcomeFragment {
            return WelcomeFragment()
        }
    }
}