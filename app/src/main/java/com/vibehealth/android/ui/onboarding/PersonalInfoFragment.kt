package com.vibehealth.android.ui.onboarding

import android.app.DatePickerDialog
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
import com.vibehealth.android.databinding.FragmentOnboardingPersonalInfoBinding
import com.vibehealth.android.domain.onboarding.OnboardingStep
import com.vibehealth.android.core.accessibility.AccessibilityHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Personal information collection screen with validation
 */
@AndroidEntryPoint
class PersonalInfoFragment : Fragment() {

    private var _binding: FragmentOnboardingPersonalInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OnboardingViewModel by activityViewModels()
    
    @Inject
    lateinit var accessibilityHelper: AccessibilityHelper
    
    private var selectedBirthday: Date? = null
    private val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingPersonalInfoBinding.inflate(inflater, container, false)
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
            OnboardingStep.PERSONAL_INFO.stepNumber + 1,
            OnboardingStep.PERSONAL_INFO.totalSteps
        )

        // Set progress bar
        val progressPercentage = (OnboardingStep.PERSONAL_INFO.getProgressPercentage() * 100).toInt()
        binding.progressBar.progress = progressPercentage

        // Setup accessibility
        setupAccessibility()
        
        // Restore previous data if available
        restorePreviousData()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            viewModel.navigateToPreviousStep()
        }

        binding.btnBirthdayPicker.setOnClickListener {
            showDatePicker()
        }

        binding.btnContinue.setOnClickListener {
            if (validateForm()) {
                viewModel.navigateToNextStep()
            }
        }
    }

    private fun setupTextWatchers() {
        binding.etFullName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val name = s?.toString()?.trim() ?: ""
                updatePersonalInfo(name, selectedBirthday)
                validateNameField(name)
            }
        })
    }

    private fun observeViewModel() {
        // Observe validation errors
        viewModel.validationErrors.observe(viewLifecycleOwner) { errors ->
            // Update name field error
            if (errors.nameError != null) {
                binding.tilFullName.error = errors.nameError
                binding.tilFullName.isErrorEnabled = true
            } else {
                binding.tilFullName.error = null
                binding.tilFullName.isErrorEnabled = false
            }

            // Update birthday error
            if (errors.birthdayError != null) {
                binding.tvBirthdayError.text = errors.birthdayError
                binding.tvBirthdayError.visibility = View.VISIBLE
                // Change button appearance for error state
                binding.btnBirthdayPicker.setTextColor(ContextCompat.getColor(requireContext(), R.color.error))
            } else {
                binding.tvBirthdayError.visibility = View.GONE
                // Reset button appearance
                binding.btnBirthdayPicker.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }

            // Update continue button state
            updateContinueButtonState()
        }

        // Observe navigation events
        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
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

        // Observe user profile to restore data
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            if (profile != null) {
                // Restore name if not currently editing
                if (binding.etFullName.text.toString() != profile.displayName && !binding.etFullName.hasFocus()) {
                    binding.etFullName.setText(profile.displayName)
                }

                // Restore birthday
                if (profile.birthday != null && selectedBirthday != profile.birthday) {
                    selectedBirthday = profile.birthday
                    updateBirthdayDisplay(profile.birthday)
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        
        // Set initial date to selected birthday or current date minus 25 years
        if (selectedBirthday != null) {
            calendar.time = selectedBirthday!!
        } else {
            calendar.add(Calendar.YEAR, -25)
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.DatePickerTheme,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                selectedBirthday = selectedCalendar.time
                
                updateBirthdayDisplay(selectedBirthday!!)
                updatePersonalInfo(binding.etFullName.text.toString().trim(), selectedBirthday)
            },
            year,
            month,
            day
        )

        // Set max date to today
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        
        // Set min date to 120 years ago
        val minCalendar = Calendar.getInstance()
        minCalendar.add(Calendar.YEAR, -120)
        datePickerDialog.datePicker.minDate = minCalendar.timeInMillis

        datePickerDialog.show()
    }

    private fun updateBirthdayDisplay(date: Date) {
        binding.btnBirthdayPicker.text = dateFormatter.format(date)
        binding.btnBirthdayPicker.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.text_primary)
        )
    }

    private fun updatePersonalInfo(name: String, birthday: Date?) {
        viewModel.updatePersonalInfo(name, birthday)
    }

    private fun validateNameField(name: String) {
        when {
            name.isEmpty() -> {
                binding.tilFullName.error = getString(R.string.error_name_required)
                binding.tilFullName.isErrorEnabled = true
            }
            name.length < 2 -> {
                binding.tilFullName.error = getString(R.string.error_name_too_short)
                binding.tilFullName.isErrorEnabled = true
            }
            name.length > 50 -> {
                binding.tilFullName.error = getString(R.string.error_name_too_long)
                binding.tilFullName.isErrorEnabled = true
            }
            else -> {
                binding.tilFullName.error = null
                binding.tilFullName.isErrorEnabled = false
            }
        }
        updateContinueButtonState()
    }

    private fun validateForm(): Boolean {
        val name = binding.etFullName.text.toString().trim()
        val hasValidName = name.isNotEmpty() && name.length >= 2 && name.length <= 50
        val hasValidBirthday = selectedBirthday != null
        
        return hasValidName && hasValidBirthday
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
            // Restore name
            if (profile.displayName.isNotEmpty()) {
                binding.etFullName.setText(profile.displayName)
            }

            // Restore birthday
            if (profile.birthday != null) {
                selectedBirthday = profile.birthday
                updateBirthdayDisplay(profile.birthday)
            }
        }
        updateContinueButtonState()
    }

    /**
     * Set up accessibility features for WCAG 2.1 Level AA compliance
     */
    private fun setupAccessibility() {
        // Set content descriptions
        accessibilityHelper.setContentDescription(
            binding.etFullName,
            "Full name input field",
            "Enter your full name"
        )
        
        accessibilityHelper.setContentDescription(
            binding.btnBirthdayPicker,
            "Birthday picker button",
            "Tap to select your birthday"
        )
        
        accessibilityHelper.setContentDescription(
            binding.btnContinue,
            "Continue button",
            "Continue to next step"
        )
        
        // Ensure minimum touch targets
        accessibilityHelper.ensureMinimumTouchTarget(binding.btnContinue)
        accessibilityHelper.ensureMinimumTouchTarget(binding.btnBirthdayPicker)
        
        // Set up keyboard navigation
        accessibilityHelper.setupKeyboardNavigation(binding.root as ViewGroup)
        
        // Set up focus indicators
        accessibilityHelper.setupFocusIndicators(binding.etFullName)
        accessibilityHelper.setupFocusIndicators(binding.btnBirthdayPicker)
        accessibilityHelper.setupFocusIndicators(binding.btnContinue)
        
        // Apply dynamic font scaling
        accessibilityHelper.applyDynamicFontScaling(binding.tvHeading, 24f)
        accessibilityHelper.applyDynamicFontScaling(binding.tvHelperText, 16f)
        
        // Apply high contrast if needed
        accessibilityHelper.applyHighContrastIfNeeded(binding.tvHeading)
        accessibilityHelper.applyHighContrastIfNeeded(binding.tvHelperText)
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
        binding.etFullName.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                // Scroll to make the name input visible when keyboard appears
                view.post {
                    binding.root.smoothScrollTo(0, view.top - 100)
                }
            }
        }
        
        binding.btnBirthdayPicker.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                // Scroll to make the birthday picker visible when focused
                view.post {
                    binding.root.smoothScrollTo(0, view.top - 100)
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
        fun newInstance(): PersonalInfoFragment {
            return PersonalInfoFragment()
        }
    }
}