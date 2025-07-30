package com.vibehealth.android.ui.onboarding

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.BounceInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.vibehealth.android.R
import com.vibehealth.android.databinding.FragmentOnboardingCompletionBinding
import com.vibehealth.android.domain.onboarding.OnboardingState
import com.vibehealth.android.core.accessibility.AccessibilityHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Completion screen with success feedback and goal calculation integration
 */
@AndroidEntryPoint
class CompletionFragment : Fragment() {

    private var _binding: FragmentOnboardingCompletionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()
    
    @Inject
    lateinit var accessibilityHelper: AccessibilityHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingCompletionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupAccessibility()
        setupResponsiveLayout()
        setupUI()
        setupClickListeners()
        observeViewModel()
        startCompletionFlow()
    }

    private fun setupUI() {
        // Set progress indicator using proper string formatting
        binding.tvProgressIndicator.text = getString(
            R.string.step_indicator,
            com.vibehealth.android.domain.onboarding.OnboardingStep.COMPLETION.stepNumber + 1,
            com.vibehealth.android.domain.onboarding.OnboardingStep.COMPLETION.totalSteps
        )
        
        // Set button text and make it visible immediately
        binding.btnEnterApp.text = getString(R.string.enter_vibe_health)
        binding.btnEnterApp.visibility = View.VISIBLE
        
        // Initially disable button while goals are being calculated
        binding.btnEnterApp.isEnabled = false
        binding.btnEnterApp.alpha = 0.6f
    }

    private fun setupClickListeners() {
        binding.btnEnterApp.setOnClickListener {
            // Prevent multiple clicks
            binding.btnEnterApp.isEnabled = false
            
            // Navigate to main app
            viewModel.completeOnboarding()
        }
    }

    private fun observeViewModel() {
        // Observe onboarding state
        viewModel.onboardingState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is OnboardingState.Completing -> {
                    showLoadingState()
                }
                is OnboardingState.Completed -> {
                    showCompletedState()
                }
                is OnboardingState.Error -> {
                    showErrorState(state.message)
                }
                else -> {
                    // Handle other states if needed
                }
            }
        }

        // Observe navigation events
        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OnboardingNavigationEvent.OnboardingComplete -> {
                    // Navigation to main app will be handled by parent activity
                }
                else -> {
                    // Handle other navigation events if needed
                }
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                showLoadingState()
            }
        }
    }

    private fun startCompletionFlow() {
        // Start with success checkmark animation
        animateSuccessCheckmark()
        
        // Simulate goal calculation process with shorter delay
        lifecycleScope.launch {
            delay(1500) // Reduced simulation time
            showGoalCalculationComplete()
            delay(500) // Brief pause before enabling button
            enableEnterButton()
        }
    }

    private fun animateSuccessCheckmark() {
        // Scale animation with bounce
        val scaleX = ObjectAnimator.ofFloat(binding.ivSuccessIcon, "scaleX", 0f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.ivSuccessIcon, "scaleY", 0f, 1f)
        
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 500
        animatorSet.interpolator = BounceInterpolator()
        animatorSet.start()
    }

    private fun showLoadingState() {
        binding.pbCalculating.visibility = View.VISIBLE
        binding.tvCalculating.text = getString(R.string.calculating_goals)
        // Keep button visible but disabled during loading
        binding.btnEnterApp.isEnabled = false
        binding.btnEnterApp.alpha = 0.6f
    }

    private fun showGoalCalculationComplete() {
        // Hide loading
        binding.pbCalculating.visibility = View.GONE
        
        // Update status text
        binding.tvCalculating.text = "Your personalized goals are ready!"
        
        // Ensure button text is set (button should already be visible)
        binding.btnEnterApp.text = getString(R.string.enter_vibe_health)
    }

    private fun showGoalSummary() {
        // Goals are calculated in the background
        // In the simplified layout, we just show the completion message
        binding.tvCalculating.text = "Your personalized goals are ready!"
    }

    private fun calculateCalorieGoal(userProfile: com.vibehealth.android.domain.user.UserProfile): Int {
        // Simple BMR calculation (this would be more sophisticated in real implementation)
        val age = userProfile.getAge()
        val weight = userProfile.weightInKg
        val height = userProfile.heightInCm
        
        return when (userProfile.gender) {
            com.vibehealth.android.domain.user.Gender.MALE -> {
                (88.362 + (13.397 * weight) + (4.799 * height) - (5.677 * age)).toInt()
            }
            com.vibehealth.android.domain.user.Gender.FEMALE -> {
                (447.593 + (9.247 * weight) + (3.098 * height) - (4.330 * age)).toInt()
            }
            else -> {
                // Average for other genders
                ((88.362 + 447.593) / 2 + (13.397 + 9.247) / 2 * weight + 
                 (4.799 + 3.098) / 2 * height - (5.677 + 4.330) / 2 * age).toInt()
            }
        }.coerceIn(1200, 3000) // Reasonable bounds
    }



    private fun enableEnterButton() {
        // Check if fragment is still active to prevent crashes
        if (_binding == null || !isAdded) return
        
        // Ensure button text is set
        binding.btnEnterApp.text = getString(R.string.enter_vibe_health)
        binding.btnEnterApp.isEnabled = true
        binding.btnEnterApp.alpha = 1f
        
        // Animate button appearance with null checks
        binding.btnEnterApp.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(200)
            .withEndAction {
                // Check again before second animation
                if (_binding != null && isAdded) {
                    binding.btnEnterApp.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start()
                }
            }
            .start()
    }

    private fun showCompletedState() {
        // Update UI to show completion
        binding.tvCalculating.text = "Ready to start your wellness journey!"
        
        // Ensure button text is set (button should already be visible)
        binding.btnEnterApp.text = getString(R.string.enter_vibe_health)
        enableEnterButton()
    }

    private fun showErrorState(message: String) {
        // Show error state
        binding.pbCalculating.visibility = View.GONE
        binding.tvCalculating.text = "Something went wrong. Please try again."
        binding.tvCalculating.setTextColor(
            resources.getColor(R.color.error, requireContext().theme)
        )
        
        // Enable button to allow retry
        enableEnterButton()
    }

    /**
     * Set up accessibility features for WCAG 2.1 Level AA compliance
     */
    private fun setupAccessibility() {
        // Set content descriptions
        accessibilityHelper.setContentDescription(
            binding.tvCompletionTitle,
            "Completion title",
            "You're all set!"
        )
        
        accessibilityHelper.setContentDescription(
            binding.tvCalculating,
            "Status message",
            "Calculating your personalized goals"
        )
        
        accessibilityHelper.setContentDescription(
            binding.btnEnterApp,
            "Enter app button",
            "Enter Vibe Health app"
        )
        
        // Ensure minimum touch targets
        accessibilityHelper.ensureMinimumTouchTarget(binding.btnEnterApp)
        
        // Set up keyboard navigation
        accessibilityHelper.setupKeyboardNavigation(binding.root as ViewGroup)
        
        // Set up focus indicators
        accessibilityHelper.setupFocusIndicators(binding.btnEnterApp)
        
        // Apply dynamic font scaling
        accessibilityHelper.applyDynamicFontScaling(binding.tvCompletionTitle, 24f)
        accessibilityHelper.applyDynamicFontScaling(binding.tvCalculating, 16f)
        
        // Apply high contrast if needed
        accessibilityHelper.applyHighContrastIfNeeded(binding.tvCompletionTitle)
        accessibilityHelper.applyHighContrastIfNeeded(binding.tvCalculating)
    }
    
    /**
     * Set up responsive layout for different screen sizes
     */
    private fun setupResponsiveLayout() {
        // Standard Android phone layout - no responsive adjustments needed
        // Completion screen doesn't have input fields, so no special keyboard handling needed
    }

    override fun onDestroyView() {
        // Cancel any pending operations to prevent crashes
        _binding?.root?.removeCallbacks(null)
        
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): CompletionFragment {
            return CompletionFragment()
        }
    }
}