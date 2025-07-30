package com.vibehealth.android.ui.onboarding

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.vibehealth.android.R
import com.vibehealth.android.databinding.FragmentOnboardingPhysicalInfoBinding
import com.vibehealth.android.domain.onboarding.OnboardingStep
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.common.UnitSystem
import com.vibehealth.android.core.accessibility.AccessibilityHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat
import javax.inject.Inject

/**
 * Physical information collection screen with unit system support
 */
@AndroidEntryPoint
class PhysicalInfoFragment : Fragment() {

    private var _binding: FragmentOnboardingPhysicalInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()
    
    @Inject
    lateinit var accessibilityHelper: AccessibilityHelper
    
    private var currentUnitSystem = UnitSystem.METRIC
    private var selectedGender: Gender? = null
    private val decimalFormat = DecimalFormat("#.#")
    private var isUpdatingText = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingPhysicalInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupAccessibility()
        setupResponsiveLayout()
        setupUI()
        setupClickListeners()
        setupTextWatchers()
        observeViewModel()
    }

    private fun setupUI() {
        // Set progress indicator using proper string formatting
        binding.tvProgressIndicator.text = getString(
            R.string.step_indicator,
            OnboardingStep.PHYSICAL_INFO.stepNumber + 1,
            OnboardingStep.PHYSICAL_INFO.totalSteps
        )

        // Set progress bar
        val progressPercentage = (OnboardingStep.PHYSICAL_INFO.getProgressPercentage() * 100).toInt()
        binding.progressBar.progress = progressPercentage

        // Ensure no gender is selected by default
        binding.rgGender.clearCheck()
        selectedGender = null
        
        // Restore previous data if available
        restorePreviousData()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            viewModel.navigateToPreviousStep()
        }

        binding.btnContinue.setOnClickListener {
            if (validateForm()) {
                // Update ViewModel only when user explicitly continues
                updatePhysicalInfo()
                viewModel.navigateToNextStep()
            }
        }

        // Unit system buttons
        binding.btnHeightUnit.setOnClickListener {
            // Toggle between metric and imperial for height
            val currentText = binding.btnHeightUnit.text.toString()
            if (currentText == "cm") {
                binding.btnHeightUnit.text = "ft"
                binding.btnWeightUnit.text = "lbs"
                viewModel.switchUnitSystem(UnitSystem.IMPERIAL)
            } else {
                binding.btnHeightUnit.text = "cm"
                binding.btnWeightUnit.text = "kg"
                viewModel.switchUnitSystem(UnitSystem.METRIC)
            }
        }
        
        binding.btnWeightUnit.setOnClickListener {
            // Toggle between metric and imperial for weight
            val currentText = binding.btnWeightUnit.text.toString()
            if (currentText == "kg") {
                binding.btnHeightUnit.text = "ft"
                binding.btnWeightUnit.text = "lbs"
                viewModel.switchUnitSystem(UnitSystem.IMPERIAL)
            } else {
                binding.btnHeightUnit.text = "cm"
                binding.btnWeightUnit.text = "kg"
                viewModel.switchUnitSystem(UnitSystem.METRIC)
            }
        }

        // Gender radio buttons - ensure single selection
        binding.rgGender.setOnCheckedChangeListener { radioGroup, checkedId ->
            // Clear any previous selection to ensure single selection
            if (checkedId != -1) {
                selectedGender = when (checkedId) {
                    R.id.rb_male -> Gender.MALE
                    R.id.rb_female -> Gender.FEMALE
                    R.id.rb_other -> Gender.OTHER
                    R.id.rb_prefer_not_to_say -> Gender.PREFER_NOT_TO_SAY
                    else -> null
                }
            } else {
                selectedGender = null
            }
            // Use post to avoid immediate recursive calls
            binding.rgGender.post {
                if (_binding != null && isAdded) {
                    updateContinueButtonState()
                }
            }
        }
    }

    private fun setupTextWatchers() {
        binding.etHeight.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                // Skip if we're programmatically updating text
                if (isUpdatingText) return
                
                // Simple validation without ViewModel updates to prevent loops
                validateHeightField(s?.toString() ?: "")
                updateContinueButtonState()
            }
        })

        binding.etWeight.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                // Skip if we're programmatically updating text
                if (isUpdatingText) return
                
                // Simple validation without ViewModel updates to prevent loops
                validateWeightField(s?.toString() ?: "")
                updateContinueButtonState()
            }
        })
    }

    private fun observeViewModel() {
        // Observe unit system changes
        viewModel.unitSystem.observe(viewLifecycleOwner) { unitSystem ->
            if (unitSystem != currentUnitSystem) {
                currentUnitSystem = unitSystem
                updateUnitSystemUI(unitSystem)
            }
        }

        // Observe validation errors
        viewModel.validationErrors.observe(viewLifecycleOwner) { errors ->
            // Update height field error
            if (errors.heightError != null) {
                binding.tilHeight.error = errors.heightError
                binding.tilHeight.isErrorEnabled = true
            } else {
                binding.tilHeight.error = null
                binding.tilHeight.isErrorEnabled = false
            }

            // Update weight field error
            if (errors.weightError != null) {
                binding.tilWeight.error = errors.weightError
                binding.tilWeight.isErrorEnabled = true
            } else {
                binding.tilWeight.error = null
                binding.tilWeight.isErrorEnabled = false
            }

            // Gender error is handled through radio button validation
            // No separate error view in current layout

            // Update continue button state
            updateContinueButtonState()
        }

        // Observe navigation events
        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OnboardingNavigationEvent.UnitSystemChanged -> {
                    handleUnitSystemChange(event)
                }
                is OnboardingNavigationEvent.NavigateForward,
                is OnboardingNavigationEvent.NavigateBackward -> {
                    // Navigation will be handled by the parent activity
                }
                else -> {
                    // Handle other navigation events if needed
                }
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnContinue.isEnabled = !isLoading && validateForm()
        }

        // Observe user profile to restore data - but avoid infinite loops
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            if (profile != null && !isUpdatingText) {
                restoreProfileData(profile)
            }
        }
    }

    private fun switchUnitSystem(newUnitSystem: UnitSystem) {
        currentUnitSystem = newUnitSystem
        viewModel.switchUnitSystem(newUnitSystem)
        updateUnitSystemUI(newUnitSystem)
    }

    private fun updateUnitSystemUI(unitSystem: UnitSystem) {
        // Update unit buttons
        when (unitSystem) {
            UnitSystem.METRIC -> {
                binding.btnHeightUnit.text = "cm"
                binding.btnWeightUnit.text = "kg"
            }
            UnitSystem.IMPERIAL -> {
                binding.btnHeightUnit.text = "ft"
                binding.btnWeightUnit.text = "lbs"
            }
        }

        // Update input field hints
        binding.tilHeight.hint = when (unitSystem) {
            UnitSystem.METRIC -> getString(R.string.height_cm)
            UnitSystem.IMPERIAL -> getString(R.string.height_ft)
        }

        binding.tilWeight.hint = when (unitSystem) {
            UnitSystem.METRIC -> getString(R.string.weight_kg)
            UnitSystem.IMPERIAL -> getString(R.string.weight_lbs)
        }

        // Update input types
        binding.etHeight.inputType = when (unitSystem) {
            UnitSystem.METRIC -> android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            UnitSystem.IMPERIAL -> android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        
        // Re-setup focus listeners after unit system change
        setupFocusListener(binding.etHeight)
        setupFocusListener(binding.etWeight)
    }

    private fun handleUnitSystemChange(event: OnboardingNavigationEvent.UnitSystemChanged) {
        // Prevent recursive updates
        isUpdatingText = true
        
        // Convert and display current values in new unit system
        if (event.convertedHeight != null) {
            val heightText = when (event.newSystem) {
                UnitSystem.METRIC -> decimalFormat.format(event.convertedHeight)
                UnitSystem.IMPERIAL -> formatImperialHeight(event.convertedHeight)
            }
            binding.etHeight.setText(heightText)
        }

        if (event.convertedWeight != null) {
            val weightText = decimalFormat.format(event.convertedWeight)
            binding.etWeight.setText(weightText)
        }
        
        // Re-enable updates after a brief delay
        binding.root.postDelayed({
            isUpdatingText = false
        }, 100)
    }

    private fun formatImperialHeight(totalInches: Double): String {
        val feet = (totalInches / 12).toInt()
        val inches = (totalInches % 12).toInt()
        return "${feet}'${inches}\""
    }

    private fun updatePhysicalInfo() {
        // Only update if fragment is still active to prevent crashes
        if (_binding != null && isAdded) {
            val height = binding.etHeight.text.toString().trim()
            val weight = binding.etWeight.text.toString().trim()
            
            viewModel.updatePhysicalInfo(height, weight, selectedGender, currentUnitSystem)
        }
    }

    private fun validateHeightField(height: String) {
        when {
            height.isEmpty() -> {
                binding.tilHeight.error = getString(R.string.error_height_required)
                binding.tilHeight.isErrorEnabled = true
            }
            else -> {
                // Let ViewModel handle detailed validation
                binding.tilHeight.error = null
                binding.tilHeight.isErrorEnabled = false
            }
        }
        updateContinueButtonState()
    }

    private fun validateWeightField(weight: String) {
        when {
            weight.isEmpty() -> {
                binding.tilWeight.error = getString(R.string.error_weight_required)
                binding.tilWeight.isErrorEnabled = true
            }
            else -> {
                // Let ViewModel handle detailed validation
                binding.tilWeight.error = null
                binding.tilWeight.isErrorEnabled = false
            }
        }
        updateContinueButtonState()
    }

    private fun validateGenderField() {
        // Gender validation is handled through radio button selection
        // Continue button state is updated based on selection
        updateContinueButtonState()
    }

    private fun validateForm(): Boolean {
        val height = binding.etHeight.text.toString().trim()
        val weight = binding.etWeight.text.toString().trim()
        val hasValidHeight = height.isNotEmpty()
        val hasValidWeight = weight.isNotEmpty()
        val hasValidGender = selectedGender != null
        
        return hasValidHeight && hasValidWeight && hasValidGender
    }

    private fun updateContinueButtonState() {
        val isValid = validateForm()
        binding.btnContinue.isEnabled = isValid
        
        // Update button appearance based on state
        if (isValid) {
            binding.btnContinue.alpha = 1.0f
        } else {
            binding.btnContinue.alpha = 0.6f
        }
    }

    private fun restorePreviousData() {
        val profile = viewModel.userProfile.value
        if (profile != null) {
            restoreProfileData(profile)
        }
        updateContinueButtonState()
    }

    private fun restoreProfileData(profile: com.vibehealth.android.domain.user.UserProfile) {
        // Prevent text watchers from triggering during restore
        isUpdatingText = true
        
        // Restore unit system
        currentUnitSystem = profile.unitSystem
        updateUnitSystemUI(currentUnitSystem)

        // Restore gender
        selectedGender = profile.gender
        val genderRadioId = when (profile.gender) {
            Gender.MALE -> R.id.rb_male
            Gender.FEMALE -> R.id.rb_female
            Gender.OTHER -> R.id.rb_other
            Gender.PREFER_NOT_TO_SAY -> R.id.rb_prefer_not_to_say
        }
        binding.rgGender.check(genderRadioId)

        // Restore height and weight if available
        if (profile.heightInCm > 0) {
            val heightText = when (currentUnitSystem) {
                UnitSystem.METRIC -> profile.heightInCm.toString()
                UnitSystem.IMPERIAL -> {
                    val totalInches = profile.heightInCm * 0.393701
                    formatImperialHeight(totalInches)
                }
            }
            binding.etHeight.setText(heightText)
        }

        if (profile.weightInKg > 0.0) {
            val weightText = when (currentUnitSystem) {
                UnitSystem.METRIC -> decimalFormat.format(profile.weightInKg)
                UnitSystem.IMPERIAL -> decimalFormat.format(profile.weightInKg * 2.20462)
            }
            binding.etWeight.setText(weightText)
        }
        
        // Re-enable text watchers after restore
        binding.root.postDelayed({
            isUpdatingText = false
        }, 200)
    }

    /**
     * Set up accessibility features for WCAG 2.1 Level AA compliance
     */
    private fun setupAccessibility() {
        // Set content descriptions
        accessibilityHelper.setContentDescription(
            binding.etHeight,
            "Height input field",
            "Enter your height"
        )
        
        accessibilityHelper.setContentDescription(
            binding.etWeight,
            "Weight input field",
            "Enter your weight"
        )
        
        accessibilityHelper.setContentDescription(
            binding.btnContinue,
            "Continue button",
            "Continue to next step"
        )
        
        // Ensure minimum touch targets
        accessibilityHelper.ensureMinimumTouchTarget(binding.btnContinue)
        accessibilityHelper.ensureMinimumTouchTarget(binding.btnHeightUnit)
        accessibilityHelper.ensureMinimumTouchTarget(binding.btnWeightUnit)
        
        // Set up keyboard navigation
        accessibilityHelper.setupKeyboardNavigation(binding.root as ViewGroup)
        
        // Set up focus indicators
        accessibilityHelper.setupFocusIndicators(binding.etHeight)
        accessibilityHelper.setupFocusIndicators(binding.etWeight)
        accessibilityHelper.setupFocusIndicators(binding.btnContinue)
        
        // Apply dynamic font scaling
        accessibilityHelper.applyDynamicFontScaling(binding.tvHeading, 24f)
        accessibilityHelper.applyDynamicFontScaling(binding.tvGenderLabel, 16f)
        
        // Apply high contrast if needed
        accessibilityHelper.applyHighContrastIfNeeded(binding.tvHeading)
        accessibilityHelper.applyHighContrastIfNeeded(binding.tvGenderLabel)
    }
    
    /**
     * Set up responsive layout for different screen sizes
     */
    private fun setupResponsiveLayout() {
        // Handle keyboard visibility for better UX
        setupKeyboardHandling()
    }
    
    private fun setupKeyboardHandling() {
        // Set focus change listeners to handle keyboard visibility
        setupFocusListener(binding.etHeight)
        setupFocusListener(binding.etWeight)
    }
    
    private fun setupFocusListener(editText: android.widget.EditText) {
        editText.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus && _binding != null && isAdded) {
                // Scroll to make the input visible when keyboard appears
                view.post {
                    if (_binding != null && isAdded) {
                        val scrollY = maxOf(0, view.top - 200)
                        binding.root.smoothScrollTo(0, scrollY)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        // Cancel any pending operations to prevent crashes
        _binding?.root?.removeCallbacks(null)
        
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): PhysicalInfoFragment {
            return PhysicalInfoFragment()
        }
    }
}