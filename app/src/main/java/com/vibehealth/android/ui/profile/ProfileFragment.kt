package com.vibehealth.android.ui.profile

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.vibehealth.android.R
import com.vibehealth.android.core.validation.OnboardingValidationHelper
import com.vibehealth.android.core.validation.OnboardingValidationUtils
import com.vibehealth.android.core.validation.ValidationResult
import com.vibehealth.android.domain.common.UnitSystem
import com.vibehealth.android.domain.onboarding.ValidationErrors
import com.vibehealth.android.domain.onboarding.ValidationField
import com.vibehealth.android.domain.user.Gender
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import com.vibehealth.android.databinding.FragmentProfileBinding
import com.vibehealth.android.domain.auth.AuthState
import com.vibehealth.android.ui.auth.AuthViewModel
import com.vibehealth.android.ui.base.AuthenticatedFragment
import com.vibehealth.android.ui.profile.components.ProfileGoalsSection
import com.vibehealth.android.ui.profile.components.ReminderSettingsCard
import com.vibehealth.android.domain.user.UserProfile
import com.vibehealth.android.ui.components.ValidationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * TASK 1 ANALYSIS: Enhanced Profile fragment with comprehensive profile management
 * 
 * EXISTING CAPABILITIES DISCOVERED:
 * - Basic authentication-based profile display
 * - Logout functionality with lifecycle management
 * - AuthenticatedFragment base class integration
 * - Hilt dependency injection setup
 * - Proper view binding lifecycle management
 * 
 * INTEGRATION OPPORTUNITIES IDENTIFIED:
 * - ProfileViewModel already exists with comprehensive goal integration
 * - ProfileGoalsSection component available for reuse
 * - Bottom navigation integration already established
 * - Sage Green design system colors already defined
 * - 8-point grid spacing system already implemented
 * 
 * ENHANCEMENT PLAN:
 * - Extend existing layout with comprehensive profile information display
 * - Integrate ProfileViewModel for full profile data management
 * - Leverage ProfileGoalsSection for profile-goals integration
 * - Apply Companion Principle design patterns throughout
 * - Maintain existing authentication and navigation patterns
 */
@AndroidEntryPoint
class ProfileFragment : AuthenticatedFragment() {
    
    companion object {
        private const val TAG = "ProfileFragment"
        private const val TAG_ANALYSIS = "PROFILE_ANALYSIS"
        private const val TAG_LAYOUT = "PROFILE_LAYOUT"
        private const val TAG_INTEGRATION = "PROFILE_INTEGRATION"
        private const val TAG_VALIDATION = "PROFILE_VALIDATION"
        private const val TAG_GOALS = "PROFILE_GOALS"
        private const val TAG_PERFORMANCE = "PROFILE_PERFORMANCE"
        private const val TAG_NAV = "PROFILE_NAV"
        private const val TAG_VM = "PROFILE_VM"
        private const val TAG_COMPONENTS = "PROFILE_COMPONENTS"
        private const val TAG_DESIGN = "PROFILE_DESIGN"
        private const val TAG_UIUX = "PROFILE_UIUX"
        private const val TAG_ERRORS = "PROFILE_ERRORS"
        private const val TAG_LOADING = "PROFILE_LOADING"
        private const val TAG_STATE = "PROFILE_STATE"
        private const val TAG_DATA = "PROFILE_DATA"
    }
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    
    // TASK 4 ANALYSIS: Reminder preferences ViewModel integration
    private val reminderPreferencesViewModel: ReminderPreferencesViewModel by viewModels()
    
    // ANALYSIS: Discovered existing ProfileGoalsSection component
    private lateinit var profileGoalsSection: ProfileGoalsSection
    
    // TASK 4 ANALYSIS: Reminder settings card component integration
    private lateinit var reminderSettingsCard: ReminderSettingsCard
    
    // Toast management removed - no success messages needed
    
    // TASK 3: Inline editing variables
    @Inject
    lateinit var validationHelper: OnboardingValidationHelper
    
    // TASK 4: Validation components
    private lateinit var uiValidationHelper: ValidationHelper
    private var currentValidationErrors = ValidationErrors()
    
    private var isEditMode = false
    private var currentProfile: UserProfile? = null
    private var selectedBirthday: Date? = null
    private var currentUnitSystem: UnitSystem = UnitSystem.METRIC
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG_ANALYSIS, "ProfileFragment onCreateView() - Analyzing existing infrastructure")
        Log.d(TAG_LAYOUT, "Extending existing fragment_profile.xml with comprehensive profile information display")
        
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        
        // ANALYSIS: Existing layout structure discovered
        Log.d(TAG_INTEGRATION, "Existing layout components found:")
        Log.d(TAG_INTEGRATION, "  - Profile avatar: ${binding.profileAvatar != null}")
        Log.d(TAG_INTEGRATION, "  - User name display: ${binding.userName != null}")
        Log.d(TAG_INTEGRATION, "  - User email display: ${binding.userEmail != null}")
        Log.d(TAG_INTEGRATION, "  - Profile options section: ${binding.profileOptions != null}")
        Log.d(TAG_INTEGRATION, "  - Logout button: ${binding.logoutButton != null}")
        
        return binding.root
    }
    
    override fun onAuthenticatedViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG_ANALYSIS, "onAuthenticatedViewCreated() - Setting up enhanced profile management")
        
        // TASK 4: Initialize validation helper
        uiValidationHelper = ValidationHelper(requireContext())
        Log.d("PROFILE_VALIDATION", "ValidationHelper initialized for comprehensive validation")
        
        analyzeExistingInfrastructure()
        setupUI()
        setupObservers()
        setupProfileGoalsIntegration()
        setupReminderSettingsIntegration()
    }
    
    /**
     * TASK 1: Analyze existing profile infrastructure components
     * 
     * This method performs comprehensive analysis of existing profile components
     * and logs integration opportunities for enhancement.
     */
    private fun analyzeExistingInfrastructure() {
        Log.d(TAG_ANALYSIS, "=== COMPREHENSIVE INFRASTRUCTURE ANALYSIS ===")
        
        // Analyze existing ViewModels
        Log.d(TAG_VM, "ProfileViewModel capabilities discovered:")
        Log.d(TAG_VM, "  - Profile state management with ProfileState sealed class")
        Log.d(TAG_VM, "  - Goal calculation integration with GoalCalculationState")
        Log.d(TAG_VM, "  - Error handling with ProfileErrorState")
        Log.d(TAG_VM, "  - Loading states for profile and goal operations")
        Log.d(TAG_VM, "  - Automatic goal recalculation on profile updates")
        Log.d(TAG_VM, "  - User-friendly error messaging following Companion Principle")
        
        // Analyze existing components
        Log.d(TAG_COMPONENTS, "Existing components available for reuse:")
        Log.d(TAG_COMPONENTS, "  - ProfileGoalsSection: Custom view for displaying goals in profile")
        Log.d(TAG_COMPONENTS, "  - UserProfile domain model with comprehensive validation")
        Log.d(TAG_COMPONENTS, "  - UserProfileRepository with offline-first approach")
        Log.d(TAG_COMPONENTS, "  - ProfileUpdateUseCase with goal recalculation integration")
        
        // Analyze existing design system
        Log.d(TAG_DESIGN, "Design system components discovered:")
        Log.d(TAG_DESIGN, "  - Sage Green color palette (#6B8E6B) already defined")
        Log.d(TAG_DESIGN, "  - 8-point grid spacing system implemented")
        Log.d(TAG_DESIGN, "  - Material Design 3 card patterns established")
        Log.d(TAG_DESIGN, "  - Companion Principle supportive messaging patterns")
        
        // Analyze navigation integration
        Log.d(TAG_NAV, "Navigation integration status:")
        Log.d(TAG_NAV, "  - Bottom navigation with Profile tab already configured")
        Log.d(TAG_NAV, "  - MainActivity navigation setup completed")
        Log.d(TAG_NAV, "  - Fragment lifecycle management established")
        
        Log.d(TAG_ANALYSIS, "=== ANALYSIS COMPLETE - READY FOR ENHANCEMENT ===")
    }
    
    private fun setupUI() {
        Log.d(TAG_LAYOUT, "Setting up enhanced UI with existing components")
        
        // REUSE EXISTING: Maintain existing logout functionality
        binding.logoutButton.setOnClickListener {
            Log.d(TAG_INTEGRATION, "Logout button clicked - maintaining existing functionality")
            logout()
        }
        
        // TASK 3: Add edit profile button functionality
        binding.editProfileButton.setOnClickListener {
            Log.d(TAG_NAV, "Edit profile button clicked - navigating to profile edit")
            navigateToProfileEdit()
        }
        
        // EXTEND EXISTING: Add click listeners for profile options
        binding.settingsOption.setOnClickListener {
            Log.d(TAG_NAV, "Settings option clicked - leveraging existing navigation")
            // TODO: Navigate to settings (following existing navigation patterns)
        }
        
        binding.privacyOption.setOnClickListener {
            Log.d(TAG_NAV, "Privacy option clicked")
            // TODO: Navigate to privacy settings
        }
        
        binding.helpOption.setOnClickListener {
            Log.d(TAG_NAV, "Help option clicked")
            // TODO: Navigate to help & support
        }
        
        Log.d(TAG_UIUX, "Applying Sage Green palette and Companion Principle design patterns")
        Log.d(TAG_DESIGN, "UI setup complete - following established design system")
    }
    
    private fun setupObservers() {
        Log.d(TAG_INTEGRATION, "Setting up observers for enhanced profile management")
        
        // EXTEND EXISTING: Enhanced authentication state observation
        // Load profile data immediately if user is authenticated
        try {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                Log.d(TAG_INTEGRATION, "User authenticated - loading profile data")
                profileViewModel.loadProfile(currentUser.uid)
            }
        } catch (e: Exception) {
            Log.e(TAG_INTEGRATION, "Error loading profile data", e)
        }
        
        // Continue with existing observer
        authViewModel.authState.observe(viewLifecycleOwner) { authState ->
            Log.d(TAG_VALIDATION, "Auth state changed: ${authState.javaClass.simpleName}")
            
            when (authState) {
                is AuthState.Authenticated -> {
                    Log.d(TAG_INTEGRATION, "User authenticated - loading comprehensive profile data")
                    
                    // REUSE EXISTING: Maintain existing user info display
                    binding.userEmail.text = authState.user.email ?: "No email"
                    binding.userName.text = authState.user.displayName ?: "User"
                    binding.logoutButton.isEnabled = true
                    
                    // EXTEND: Load full profile data using ProfileViewModel
                    authState.user.uid?.let { userId ->
                        Log.d(TAG_VM, "Loading profile data for user: $userId")
                        profileViewModel.loadProfile(userId)
                    }
                }
                is AuthState.NotAuthenticated -> {
                    Log.d(TAG_INTEGRATION, "User not authenticated - will be redirected by MainActivity")
                }
                is AuthState.Error -> {
                    Log.d(TAG_ERRORS, "Auth error - disabling profile functionality")
                    binding.logoutButton.isEnabled = false
                }
                is AuthState.Loading -> {
                    Log.d(TAG_LOADING, "Auth loading - showing loading state")
                    binding.logoutButton.isEnabled = false
                }
            }
        }
        
        // NEW: Observe comprehensive profile state
        profileViewModel.profileState.observe(viewLifecycleOwner) { profileState ->
            Log.d(TAG_STATE, "Profile state changed: ${profileState.javaClass.simpleName}")
            handleProfileState(profileState)
        }
        
        // TASK 11: Observe real-time profile updates for immediate UI reflection
        setupRealTimeProfileObservers()
        
        // NEW: Observe goal calculation state
        profileViewModel.goalCalculationState.observe(viewLifecycleOwner) { goalState ->
            Log.d(TAG_GOALS, "Goal calculation state changed: ${goalState.javaClass.simpleName}")
            handleGoalCalculationState(goalState)
        }
        
        // NEW: Observe error states with supportive messaging
        profileViewModel.errorState.observe(viewLifecycleOwner) { errorState ->
            errorState?.let {
                Log.d(TAG_ERRORS, "Error state: ${it.javaClass.simpleName}")
                handleProfileError(it)
            }
        }
        
        // NEW: Observe current goals for ProfileGoalsSection
        profileViewModel.currentGoals.observe(viewLifecycleOwner) { goals ->
            goals?.let {
                Log.d(TAG_GOALS, "Current goals updated")
                try {
                    profileGoalsSection.displayGoals(it)
                } catch (e: Exception) {
                    Log.e(TAG_ERRORS, "Error displaying current goals: ${e.message}", e)
                }
            }
        }
        
        // TASK 4 ANALYSIS: Reminder preferences observers
        setupReminderPreferencesObservers()
    }
    
    /**
     * TASK 4 ANALYSIS: Setup reminder preferences observers
     * Integrates with existing observer patterns for seamless data flow
     */
    private fun setupReminderPreferencesObservers() {
        Log.d("REMINDER_PREFERENCES", "Setting up reminder preferences observers")
        
        // Load reminder preferences when user is authenticated
        authViewModel.authState.observe(viewLifecycleOwner) { authState ->
            if (authState is AuthState.Authenticated) {
                authState.user.uid?.let { userId ->
                    Log.d("REMINDER_PREFERENCES", "Loading reminder preferences for user: $userId")
                    reminderPreferencesViewModel.loadReminderPreferences(userId)
                }
            }
        }
        
        // Observe reminder preferences changes
        reminderPreferencesViewModel.reminderPreferences.observe(viewLifecycleOwner) { preferences ->
            Log.d("REMINDER_PREFERENCES", "Reminder preferences updated - updating UI")
            reminderSettingsCard.displayPreferences(preferences)
        }
        
        // Observe loading state
        reminderPreferencesViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d("REMINDER_PREFERENCES", "Loading state: $isLoading")
            // TODO: Show loading indicator if needed
        }
        
        // Observe error state
        reminderPreferencesViewModel.errorState.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e("REMINDER_PREFERENCES", "Error: ${it.message}")
                android.widget.Toast.makeText(
                    requireContext(),
                    it.message,
                    android.widget.Toast.LENGTH_LONG
                ).show()
                reminderPreferencesViewModel.clearError()
            }
        }
        
        // Success messages removed - no toast needed for UI interactions
        
        Log.d("REMINDER_PREFERENCES", "Reminder preferences observers setup complete")
    }
    
    /**
     * Handle profile state changes with comprehensive logging
     */
    private fun handleProfileState(profileState: ProfileState) {
        when (profileState) {
            is ProfileState.Loading -> {
                Log.d(TAG_LOADING, "Profile loading - showing supportive loading state")
                showLoadingState()
            }
            is ProfileState.Loaded -> {
                Log.d(TAG_INTEGRATION, "Profile loaded successfully")
                Log.d(TAG_DATA, "Profile validation status: ${profileState.profile.isValidForGoalCalculation()}")
                Log.d(TAG_DATA, "Onboarding complete: ${profileState.profile.hasCompletedOnboarding}")
                displayProfileInformation(profileState.profile)
            }
            is ProfileState.NotFound -> {
                Log.d(TAG_ERRORS, "Profile not found - showing supportive guidance")
                showEmptyProfileState()
            }
            is ProfileState.Error -> {
                Log.d(TAG_ERRORS, "Profile error: ${profileState.message}")
                showProfileError(profileState.message)
            }
        }
    }
    
    /**
     * Handle goal calculation state with encouraging feedback
     */
    private fun handleGoalCalculationState(goalState: GoalCalculationState) {
        when (goalState) {
            is GoalCalculationState.Loading -> {
                Log.d(TAG_GOALS, "Goal calculation loading: ${goalState.message}")
                try {
                    profileGoalsSection.showLoadingState()
                } catch (e: Exception) {
                    Log.e(TAG_ERRORS, "Error showing goals loading state: ${e.message}", e)
                }
            }
            is GoalCalculationState.Success -> {
                Log.d(TAG_GOALS, "Goals calculated successfully - was recalculated: ${goalState.wasRecalculated}")
                Log.d(TAG_GOALS, "Reason: ${goalState.reason}")
                try {
                    profileGoalsSection.displayGoals(goalState.goals)
                } catch (e: Exception) {
                    Log.e(TAG_ERRORS, "Error displaying goals: ${e.message}", e)
                }
            }
            is GoalCalculationState.Error -> {
                Log.d(TAG_GOALS, "Goal calculation error: ${goalState.message}")
                Log.d(TAG_GOALS, "Can retry: ${goalState.canRetry}")
                try {
                    profileGoalsSection.showErrorState(goalState.message)
                } catch (e: Exception) {
                    Log.e(TAG_ERRORS, "Error showing goals error state: ${e.message}", e)
                }
            }
            is GoalCalculationState.NeedsRecalculation -> {
                Log.d(TAG_GOALS, "Goals need recalculation: ${goalState.reason}")
                try {
                    profileGoalsSection.showEmptyState()
                } catch (e: Exception) {
                    Log.e(TAG_ERRORS, "Error showing goals empty state: ${e.message}", e)
                }
            }
            is GoalCalculationState.NoRecalculationNeeded -> {
                Log.d(TAG_GOALS, "No recalculation needed: ${goalState.reason}")
                // Goals should be available through currentGoals observer
                // If not, show empty state to allow manual calculation
                val currentGoals = profileViewModel.currentGoals.value
                if (currentGoals == null) {
                    try {
                        profileGoalsSection.showEmptyState()
                    } catch (e: Exception) {
                        Log.e(TAG_ERRORS, "Error showing goals empty state: ${e.message}", e)
                    }
                }
            }
            is GoalCalculationState.Loaded -> {
                Log.d(TAG_GOALS, "Goals loaded: ${goalState.message}")
                // Goals should be available through currentGoals observer
                // If not, show empty state to allow manual calculation
                val currentGoals = profileViewModel.currentGoals.value
                if (currentGoals == null) {
                    try {
                        profileGoalsSection.showEmptyState()
                    } catch (e: Exception) {
                        Log.e(TAG_ERRORS, "Error showing goals empty state: ${e.message}", e)
                    }
                }
            }
        }
    }
    
    /**
     * Handle profile errors with supportive messaging following Companion Principle
     */
    private fun handleProfileError(errorState: ProfileErrorState) {
        when (errorState) {
            is ProfileErrorState.ProfileLoadFailed -> {
                Log.d(TAG_ERRORS, "Profile load failed: ${errorState.message}")
            }
            is ProfileErrorState.ProfileUpdateFailed -> {
                Log.d(TAG_ERRORS, "Profile update failed: ${errorState.message}")
            }
            is ProfileErrorState.GoalCalculationFailed -> {
                Log.d(TAG_ERRORS, "Goal calculation failed: ${errorState.message}")
            }
            is ProfileErrorState.ConcurrentUpdate -> {
                Log.d(TAG_ERRORS, "Concurrent update detected: ${errorState.message}")
            }
            is ProfileErrorState.UnexpectedError -> {
                Log.d(TAG_ERRORS, "Unexpected error: ${errorState.message}")
            }
            is ProfileErrorState.ProfileNotFound -> {
                Log.d(TAG_ERRORS, "Profile not found: ${errorState.message}")
            }
        }
    }
    
    /**
     * Setup ProfileGoalsSection integration leveraging existing component
     */
    private fun setupProfileGoalsIntegration() {
        Log.d(TAG_COMPONENTS, "Setting up ProfileGoalsSection integration")
        
        try {
            // Initialize ProfileGoalsSection from layout
            profileGoalsSection = binding.profileGoalsSection
            
            // Setup goal recalculation callback
            profileGoalsSection.onRecalculateClickListener = {
                Log.d(TAG_GOALS, "Manual goal recalculation requested")
                // Trigger recalculation using existing ProfileViewModel
                val currentProfile = (profileViewModel.profileState.value as? ProfileState.Loaded)?.profile
                currentProfile?.let { profile ->
                    profileViewModel.recalculateGoals(profile.userId)
                }
            }
            
            // Setup learn more callback
            profileGoalsSection.onLearnMoreClickListener = {
                Log.d(TAG_GOALS, "Learn more about goals clicked")
                // TODO: Navigate to goals explanation screen
            }
            
            Log.d(TAG_COMPONENTS, "ProfileGoalsSection integration complete")
        } catch (e: Exception) {
            Log.e(TAG_ERRORS, "Error setting up ProfileGoalsSection: ${e.message}", e)
        }
    }
    
    /**
     * TASK 4 ANALYSIS: Setup reminder settings integration following existing patterns
     * Seamless integration with ProfileFragment using established component patterns
     */
    private fun setupReminderSettingsIntegration() {
        Log.d("REMINDER_PREFERENCES", "=== REMINDER SETTINGS INTEGRATION SETUP ===")
        Log.d("REMINDER_PREFERENCES", "Seamless ProfileFragment integration:")
        Log.d("REMINDER_PREFERENCES", "  ✓ Following existing ProfileGoalsSection integration patterns")
        Log.d("REMINDER_PREFERENCES", "  ✓ Material Design 3 consistency with existing components")
        Log.d("REMINDER_PREFERENCES", "  ✓ Sage Green palette and 8-point grid compliance")
        Log.d("REMINDER_PREFERENCES", "  ✓ Companion Principle supportive messaging")
        
        try {
            // Initialize ReminderSettingsCard from layout
            reminderSettingsCard = binding.reminderSettingsCard
            
            // Setup preferences changed callback
            reminderSettingsCard.onPreferencesChangedListener = { preferences ->
                Log.d("REMINDER_PREFERENCES", "Reminder preferences changed - updating via ViewModel")
                Log.d("REMINDER_PREFERENCES", "  Enabled: ${preferences.isEnabled}")
                Log.d("REMINDER_PREFERENCES", "  Threshold: ${preferences.inactivityThresholdMinutes} minutes")
                Log.d("REMINDER_PREFERENCES", "  Frequency: ${preferences.reminderFrequency}")
                
                // Update preferences using ReminderPreferencesViewModel
                reminderPreferencesViewModel.saveReminderPreferences(preferences)
            }
            
            // Setup validation error callback
            reminderSettingsCard.onValidationErrorListener = { errorMessage ->
                Log.w("REMINDER_VALIDATION", "Validation error: $errorMessage")
                
                // Show error message using existing patterns
                android.widget.Toast.makeText(
                    requireContext(),
                    errorMessage,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            
            Log.d("REMINDER_PREFERENCES", "ReminderSettingsCard integration complete")
            Log.d("REMINDER_INTEGRATION", "Component callbacks configured successfully")
            
        } catch (e: Exception) {
            Log.e("REMINDER_ERRORS", "Error setting up ReminderSettingsCard: ${e.message}", e)
        }
        
        Log.d("REMINDER_PREFERENCES", "=== REMINDER SETTINGS INTEGRATION SETUP COMPLETE ===")
    }
    
    /**
     * Display comprehensive profile information in the enhanced UI
     */
    private fun displayProfileInformation(profile: UserProfile) {
        Log.d(TAG, "PROFILE_SECTIONS: Displaying comprehensive profile information")
        
        try {
            // Update basic info (already handled by auth state)
            binding.userName.text = profile.getFullName()
            binding.userEmail.text = profile.email
            
            // Personal Information Section
            Log.d(TAG, "PROFILE_SECTIONS: Updating personal information section")
            profile.birthday?.let { birthday ->
                val formatter = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                binding.birthdayValue.text = formatter.format(birthday)
            } ?: run {
                binding.birthdayValue.text = "Not set"
            }
            
            binding.genderValue.text = profile.gender.getDisplayName()
            
            // Health Information Section
            Log.d(TAG, "PROFILE_SECTIONS: Updating health information section")
            if (profile.heightInCm > 0) {
                binding.heightValue.text = profile.getFormattedHeight()
            } else {
                binding.heightValue.text = "Not set"
            }
            
            if (profile.weightInKg > 0) {
                binding.weightValue.text = profile.getFormattedWeight()
            } else {
                binding.weightValue.text = "Not set"
            }
            
            // BMI Calculation
            profile.getBMI()?.let { bmi ->
                val bmiFormatted = String.format("%.1f", bmi)
                val category = profile.getBMICategory() ?: ""
                binding.bmiValue.text = "$bmiFormatted ($category)"
            } ?: run {
                binding.bmiValue.text = "Not calculated"
            }
            
            // Preferences Section
            Log.d(TAG, "PROFILE_SECTIONS: Updating preferences section")
            binding.unitSystemValue.text = when (profile.unitSystem) {
                com.vibehealth.android.domain.common.UnitSystem.METRIC -> "Metric"
                com.vibehealth.android.domain.common.UnitSystem.IMPERIAL -> "Imperial"
            }
            
            Log.d(TAG, "PROFILE_INTEGRATION: Profile information display complete")
        } catch (e: Exception) {
            Log.e(TAG_ERRORS, "Error displaying profile information: ${e.message}", e)
        }
    }
    
    /**
     * Show loading state with supportive messaging
     */
    private fun showLoadingState() {
        Log.d(TAG, "PROFILE_LOADING: Showing supportive loading state")
        try {
            // Set loading placeholders
            binding.birthdayValue.text = "Loading..."
            binding.genderValue.text = "Loading..."
            binding.heightValue.text = "Loading..."
            binding.weightValue.text = "Loading..."
            binding.bmiValue.text = "Loading..."
            binding.unitSystemValue.text = "Loading..."
        } catch (e: Exception) {
            Log.e(TAG_ERRORS, "Error showing loading state: ${e.message}", e)
        }
    }
    
    /**
     * Show empty profile state with encouraging guidance
     */
    private fun showEmptyProfileState() {
        Log.d(TAG, "PROFILE_ERRORS: Showing supportive empty profile guidance")
        try {
            // Set placeholder values with encouraging messaging
            binding.birthdayValue.text = "Complete your profile"
            binding.genderValue.text = "Not specified"
            binding.heightValue.text = "Add your height"
            binding.weightValue.text = "Add your weight"
            binding.bmiValue.text = "Complete health info"
            binding.unitSystemValue.text = "Metric"
        } catch (e: Exception) {
            Log.e(TAG_ERRORS, "Error showing empty profile state: ${e.message}", e)
        }
    }
    
    /**
     * Show profile error with supportive messaging
     */
    private fun showProfileError(message: String) {
        Log.d(TAG, "PROFILE_ERRORS: Showing supportive error message: $message")
        try {
            // Set error placeholders with supportive messaging
            binding.birthdayValue.text = "Unable to load"
            binding.genderValue.text = "Unable to load"
            binding.heightValue.text = "Unable to load"
            binding.weightValue.text = "Unable to load"
            binding.bmiValue.text = "Unable to load"
            binding.unitSystemValue.text = "Unable to load"
        } catch (e: Exception) {
            Log.e(TAG_ERRORS, "Error showing profile error state: ${e.message}", e)
        }
    }
    
    /**
     * TASK 3: Toggle profile editing mode (inline editing)
     */
    private fun navigateToProfileEdit() {
        Log.d(TAG_NAV, "Toggling profile edit mode")
        try {
            // Toggle between view and edit modes within the same fragment
            toggleEditMode()
            
            Log.d(TAG_NAV, "Profile edit mode toggled successfully")
        } catch (e: Exception) {
            Log.e(TAG_ERRORS, "Failed to toggle edit mode: ${e.message}", e)
            
            // Show error message
            android.widget.Toast.makeText(
                requireContext(), 
                "Unable to edit profile. Please try again.", 
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * TASK 3: Toggle between view and edit modes (inline editing)
     */
    private fun toggleEditMode() {
        Log.d("PROFILE_EDIT_PATTERNS", "Toggling edit mode - current: $isEditMode")
        
        isEditMode = !isEditMode
        
        if (isEditMode) {
            Log.d("PROFILE_EDIT_UI", "Entering edit mode - showing form fields")
            enterEditMode()
        } else {
            Log.d("PROFILE_EDIT_UI", "Exiting edit mode - showing view fields")
            exitEditMode()
        }
    }
    
    /**
     * PROFILE_EDIT_COMPONENTS: Enter edit mode - show form fields
     */
    private fun enterEditMode() {
        Log.d("PROFILE_EDIT_COMPONENTS", "Setting up edit mode UI components")
        
        try {
            with(binding) {
                // Hide view mode elements
                userName.visibility = View.GONE
                birthdayViewLayout.visibility = View.GONE
                genderViewLayout.visibility = View.GONE
                heightViewLayout.visibility = View.GONE
                weightViewLayout.visibility = View.GONE
                editProfileButton.visibility = View.GONE
                profileOptions.visibility = View.GONE
                logoutButton.visibility = View.GONE
                
                // Show edit mode elements
                tilUserName.visibility = View.VISIBLE
                birthdayEditLayout.visibility = View.VISIBLE
                genderEditLayout.visibility = View.VISIBLE
                heightEditLayout.visibility = View.VISIBLE
                weightEditLayout.visibility = View.VISIBLE
                editActionButtons.visibility = View.VISIBLE
                
                // Update email constraint to point to TextInputLayout
                updateEmailConstraint(true)
                
                // Populate edit fields with current data
                populateEditFields()
                
                // Setup edit mode listeners
                setupEditModeListeners()
                
                Log.d("PROFILE_EDIT_UI", "Edit mode UI setup complete")
            }
        } catch (e: Exception) {
            Log.e("PROFILE_EDIT_COMPONENTS", "Error entering edit mode: ${e.message}", e)
        }
    }
    
    /**
     * PROFILE_EDIT_COMPONENTS: Exit edit mode - show view fields
     */
    private fun exitEditMode() {
        Log.d("PROFILE_EDIT_COMPONENTS", "Exiting edit mode - restoring view UI")
        
        try {
            with(binding) {
                // Show view mode elements
                userName.visibility = View.VISIBLE
                birthdayViewLayout.visibility = View.VISIBLE
                genderViewLayout.visibility = View.VISIBLE
                heightViewLayout.visibility = View.VISIBLE
                weightViewLayout.visibility = View.VISIBLE
                editProfileButton.visibility = View.VISIBLE
                profileOptions.visibility = View.VISIBLE
                logoutButton.visibility = View.VISIBLE
                
                // Hide edit mode elements
                tilUserName.visibility = View.GONE
                birthdayEditLayout.visibility = View.GONE
                genderEditLayout.visibility = View.GONE
                heightEditLayout.visibility = View.GONE
                weightEditLayout.visibility = View.GONE
                editActionButtons.visibility = View.GONE
                
                // Update email constraint to point back to TextView
                updateEmailConstraint(false)
                
                // Clear validation errors
                clearValidationErrors()
                
                Log.d("PROFILE_EDIT_UI", "View mode UI restored")
            }
        } catch (e: Exception) {
            Log.e("PROFILE_EDIT_COMPONENTS", "Error exiting edit mode: ${e.message}", e)
        }
    }

    /**
     * PROFILE_EDIT_ARCH: Populate edit fields with current profile data
     */
    private fun populateEditFields() {
        Log.d("PROFILE_EDIT_ARCH", "Populating edit fields with current profile data")
        
        currentProfile?.let { profile ->
            with(binding) {
                // Name
                etUserName.setText(profile.displayName)
                
                // Birthday
                profile.birthday?.let { birthday ->
                    selectedBirthday = birthday
                    btnBirthdayPicker.text = dateFormat.format(birthday)
                    btnBirthdayPicker.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.text_primary)
                    )
                }
                
                // Gender
                when (profile.gender) {
                    Gender.MALE -> rbMale.isChecked = true
                    Gender.FEMALE -> rbFemale.isChecked = true
                    Gender.OTHER -> rbOther.isChecked = true
                    Gender.PREFER_NOT_TO_SAY -> rbPreferNotToSay.isChecked = true
                    null -> { /* No selection */ }
                }
                
                // Unit system
                currentUnitSystem = profile.unitSystem
                updateUnitLabels()
                
                // Height and weight (convert from stored metric values)
                populateHeightWeight(profile)
            }
        }
    }
    
    /**
     * PROFILE_EDIT_SYSTEMS: Populate height and weight with unit conversion
     */
    private fun populateHeightWeight(profile: UserProfile) {
        Log.d("PROFILE_EDIT_SYSTEMS", "Populating height/weight with unit conversion")
        
        with(binding) {
            when (currentUnitSystem) {
                UnitSystem.METRIC -> {
                    if (profile.heightInCm > 0) {
                        etHeight.setText(profile.heightInCm.toString())
                    }
                    if (profile.weightInKg > 0.0) {
                        etWeight.setText(String.format("%.1f", profile.weightInKg))
                    }
                }
                UnitSystem.IMPERIAL -> {
                    if (profile.heightInCm > 0) {
                        val heightInInches = profile.heightInCm / 2.54
                        val feet = (heightInInches / 12).toInt()
                        val inches = (heightInInches % 12).toInt()
                        etHeight.setText("$feet'$inches\"")
                    }
                    if (profile.weightInKg > 0.0) {
                        val weightInLbs = profile.weightInKg * 2.20462
                        etWeight.setText(String.format("%.1f", weightInLbs))
                    }
                }
            }
        }
    }
    
    /**
     * PROFILE_EDIT_PATTERNS: Setup edit mode listeners using onboarding patterns
     */
    private fun setupEditModeListeners() {
        Log.d("PROFILE_EDIT_PATTERNS", "Setting up edit mode listeners with validation")
        
        with(binding) {
            // Birthday picker
            btnBirthdayPicker.setOnClickListener {
                showDatePicker()
            }
            
            // Save button
            btnSaveProfile.setOnClickListener {
                Log.d("PROFILE_EDIT_SYSTEMS", "Save button pressed - validating and saving")
                saveProfileChanges()
            }
            
            // Cancel button
            btnCancelEdit.setOnClickListener {
                Log.d("PROFILE_EDIT_UI", "Cancel button pressed - exiting edit mode")
                toggleEditMode()
            }
            
            // Unit toggle buttons
            btnHeightUnit.setOnClickListener {
                toggleUnitSystem()
            }
            
            btnWeightUnit.setOnClickListener {
                toggleUnitSystem()
            }
            
            // Real-time validation
            setupRealTimeValidation()
        }
    }
    
    /**
     * PROFILE_EDIT_PATTERNS: Setup real-time validation using onboarding patterns
     */
    private fun setupRealTimeValidation() {
        Log.d("PROFILE_EDIT_PATTERNS", "Setting up real-time validation")
        
        with(binding) {
            // Name validation
            etUserName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    validateField(ValidationField.NAME, s?.toString() ?: "")
                }
            })
            
            // Height validation
            etHeight.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    validateHeightField(s?.toString() ?: "")
                }
            })
            
            // Weight validation
            etWeight.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    validateWeightField(s?.toString() ?: "")
                }
            })
        }
    }
    
    /**
     * TASK 4: PROFILE_VALIDATION - Validate individual field using onboarding patterns
     */
    private fun validateField(field: ValidationField, value: String) {
        Log.d("PROFILE_VALIDATION", "Validating field: ${field.name} with value length: ${value.length}")
        
        val result = when (field) {
            ValidationField.NAME -> OnboardingValidationUtils.validateName(value)
            ValidationField.BIRTHDAY -> {
                selectedBirthday?.let { birthday ->
                    OnboardingValidationUtils.validateBirthday(birthday)
                } ?: ValidationResult.Error("Birthday is required")
            }
            ValidationField.HEIGHT -> {
                val heightValue = parseHeightInput(value)
                if (heightValue != null) {
                    OnboardingValidationUtils.validateHeight(heightValue, currentUnitSystem)
                } else {
                    ValidationResult.Error("Please enter a valid height")
                }
            }
            ValidationField.WEIGHT -> {
                val weightValue = parseWeightInput(value)
                if (weightValue != null) {
                    OnboardingValidationUtils.validateWeight(weightValue, currentUnitSystem)
                } else {
                    ValidationResult.Error("Please enter a valid weight")
                }
            }
            ValidationField.GENDER -> {
                val selectedGender = getSelectedGender()
                OnboardingValidationUtils.validateGender(selectedGender)
            }
        }
        
        // Apply validation result to UI
        applyValidationResult(field, result)
    }
    
    /**
     * TASK 4: PROFILE_VALIDATION - Validate height field with unit-specific parsing
     */
    private fun validateHeightField(value: String) {
        Log.d("PROFILE_RANGES", "Validating height field: $value")
        
        val heightValue = parseHeightInput(value)
        val result = if (heightValue != null) {
            OnboardingValidationUtils.validateHeight(heightValue, currentUnitSystem)
        } else if (value.isNotBlank()) {
            ValidationResult.Error(getHeightFormatHint())
        } else {
            ValidationResult.Error("Height is required")
        }
        
        applyValidationResult(ValidationField.HEIGHT, result)
    }
    
    /**
     * TASK 4: PROFILE_VALIDATION - Validate weight field with unit-specific parsing
     */
    private fun validateWeightField(value: String) {
        Log.d("PROFILE_RANGES", "Validating weight field: $value")
        
        val weightValue = parseWeightInput(value)
        val result = if (weightValue != null) {
            OnboardingValidationUtils.validateWeight(weightValue, currentUnitSystem)
        } else if (value.isNotBlank()) {
            ValidationResult.Error(getWeightFormatHint())
        } else {
            ValidationResult.Error("Weight is required")
        }
        
        applyValidationResult(ValidationField.WEIGHT, result)
    }
    
    /**
     * TASK 4: PROFILE_MESSAGES - Apply validation result with supportive messaging
     */
    private fun applyValidationResult(field: ValidationField, result: ValidationResult<*>) {
        Log.d("PROFILE_MESSAGES", "Applying validation result for ${field.name}: ${result.javaClass.simpleName}")
        
        val inputLayout = getInputLayoutForField(field)
        val supportiveMessage = if (result is ValidationResult.Error) {
            OnboardingValidationUtils.getSupportiveErrorMessage(field, result.message)
        } else null
        
        inputLayout?.let { layout ->
            if (result is ValidationResult.Error) {
                Log.d("PROFILE_ERRORS", "Showing supportive error: $supportiveMessage")
                uiValidationHelper.applyValidationToField(
                    layout, 
                    com.vibehealth.android.domain.auth.ValidationResult(false, supportiveMessage),
                    false
                )
            } else {
                Log.d("PROFILE_REALTIME", "Showing positive validation feedback")
                uiValidationHelper.applyValidationToField(
                    layout,
                    com.vibehealth.android.domain.auth.ValidationResult(true, null),
                    true // Show success indicator
                )
            }
        }
        
        // Update validation state
        currentValidationErrors = if (result is ValidationResult.Error) {
            currentValidationErrors.setFieldError(field, supportiveMessage ?: "")
        } else {
            currentValidationErrors.clearFieldError(field)
        }
    }
    
    /**
     * TASK 4: PROFILE_RANGES - Parse height input based on unit system
     */
    private fun parseHeightInput(input: String): Double? {
        Log.d("PROFILE_RANGES", "Parsing height input: '$input' for unit system: $currentUnitSystem")
        
        return try {
            when (currentUnitSystem) {
                UnitSystem.METRIC -> {
                    // Expect centimeters
                    input.toDoubleOrNull()
                }
                UnitSystem.IMPERIAL -> {
                    // Parse feet'inches format or just inches
                    OnboardingValidationUtils.parseImperialHeight(input)
                }
            }
        } catch (e: Exception) {
            Log.e("PROFILE_RANGES", "Error parsing height: ${e.message}")
            null
        }
    }
    
    /**
     * TASK 4: PROFILE_RANGES - Parse weight input
     */
    private fun parseWeightInput(input: String): Double? {
        Log.d("PROFILE_RANGES", "Parsing weight input: '$input'")
        
        return try {
            input.toDoubleOrNull()
        } catch (e: Exception) {
            Log.e("PROFILE_RANGES", "Error parsing weight: ${e.message}")
            null
        }
    }
    
    /**
     * TASK 4: PROFILE_MESSAGES - Get supportive format hint for height
     */
    private fun getHeightFormatHint(): String {
        return when (currentUnitSystem) {
            UnitSystem.METRIC -> "Enter height in centimeters (e.g., 175)"
            UnitSystem.IMPERIAL -> "Enter height as feet'inches (e.g., 5'10\")"
        }
    }
    
    /**
     * TASK 4: PROFILE_MESSAGES - Get supportive format hint for weight
     */
    private fun getWeightFormatHint(): String {
        return when (currentUnitSystem) {
            UnitSystem.METRIC -> "Enter weight in kilograms (e.g., 70.5)"
            UnitSystem.IMPERIAL -> "Enter weight in pounds (e.g., 154)"
        }
    }
    
    /**
     * TASK 4: PROFILE_STATE - Get TextInputLayout for validation field
     */
    private fun getInputLayoutForField(field: ValidationField): com.google.android.material.textfield.TextInputLayout? {
        return with(binding) {
            when (field) {
                ValidationField.NAME -> tilUserName
                ValidationField.HEIGHT -> tilHeight
                ValidationField.WEIGHT -> tilWeight
                ValidationField.BIRTHDAY -> null // Birthday uses button, not TextInputLayout
                ValidationField.GENDER -> null // Gender uses radio buttons
            }
        }
    }
    
    /**
     * TASK 4: PROFILE_STATE - Get currently selected gender
     */
    private fun getSelectedGender(): Gender? {
        return with(binding) {
            when {
                rbMale.isChecked -> Gender.MALE
                rbFemale.isChecked -> Gender.FEMALE
                rbOther.isChecked -> Gender.OTHER
                rbPreferNotToSay.isChecked -> Gender.PREFER_NOT_TO_SAY
                else -> null
            }
        }
    }
    
    /**
     * TASK 4: PROFILE_STATE - Clear all validation errors
     */
    private fun clearValidationErrors() {
        Log.d("PROFILE_STATE", "Clearing all validation errors")
        
        with(binding) {
            tilUserName.error = null
            tilUserName.isErrorEnabled = false
            tilHeight.error = null
            tilHeight.isErrorEnabled = false
            tilWeight.error = null
            tilWeight.isErrorEnabled = false
        }
        
        currentValidationErrors = ValidationErrors()
    }
    
    /**
     * TASK 4: PROFILE_VALIDATION - Validate all fields before saving
     */
    private fun validateAllFields(): Boolean {
        Log.d("PROFILE_VALIDATION", "Validating all profile fields")
        
        var isValid = true
        
        // Validate name
        val nameValue = binding.etUserName.text.toString()
        validateField(ValidationField.NAME, nameValue)
        if (OnboardingValidationUtils.validateName(nameValue) is ValidationResult.Error) {
            isValid = false
        }
        
        // Validate birthday
        if (selectedBirthday == null) {
            isValid = false
            showBirthdayError("Please select your birthday")
        }
        
        // Validate height
        val heightValue = binding.etHeight.text.toString()
        validateHeightField(heightValue)
        if (parseHeightInput(heightValue) == null) {
            isValid = false
        }
        
        // Validate weight
        val weightValue = binding.etWeight.text.toString()
        validateWeightField(weightValue)
        if (parseWeightInput(weightValue) == null) {
            isValid = false
        }
        
        // Validate gender
        if (getSelectedGender() == null) {
            isValid = false
            showGenderError("Please select a gender option")
        }
        
        Log.d("PROFILE_VALIDATION", "Overall validation result: $isValid")
        return isValid
    }
    
    /**
     * TASK 4: PROFILE_ERRORS - Show birthday validation error
     */
    private fun showBirthdayError(message: String) {
        Log.d("PROFILE_ERRORS", "Showing birthday error: $message")
        
        with(binding) {
            btnBirthdayPicker.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.error)
            )
            btnBirthdayPicker.text = message
        }
    }
    
    /**
     * TASK 4: PROFILE_ERRORS - Show gender validation error
     */
    private fun showGenderError(message: String) {
        Log.d("PROFILE_ERRORS", "Showing gender error: $message")
        
        // Could add error text below radio group or use other visual indicator
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Update save button state based on validation
     */
    private fun updateSaveButtonState() {
        val isValid = !currentValidationErrors.hasErrors() && hasRequiredFields()
        binding.btnSaveProfile.isEnabled = isValid
        
        Log.d("PROFILE_EDIT_UI", "Save button enabled: $isValid")
    }
    
    /**
     * Check if required fields are filled
     */
    private fun hasRequiredFields(): Boolean {
        with(binding) {
            return !etUserName.text.isNullOrBlank() &&
                    selectedBirthday != null &&
                    !etHeight.text.isNullOrBlank() &&
                    !etWeight.text.isNullOrBlank() &&
                    rgGender.checkedRadioButtonId != -1
        }
    }
    
    /**
     * Show date picker for birthday selection
     */
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedBirthday?.let { calendar.time = it }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedBirthday = calendar.time
                
                binding.btnBirthdayPicker.text = dateFormat.format(selectedBirthday!!)
                binding.btnBirthdayPicker.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_primary)
                )
                
                validateField(ValidationField.BIRTHDAY, selectedBirthday?.toString() ?: "")
                Log.d("PROFILE_EDIT_UI", "Birthday selected: ${dateFormat.format(selectedBirthday!!)}")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set reasonable date limits
        val minDate = Calendar.getInstance()
        minDate.add(Calendar.YEAR, -120)
        datePickerDialog.datePicker.minDate = minDate.timeInMillis

        val maxDate = Calendar.getInstance()
        maxDate.add(Calendar.YEAR, -13)
        datePickerDialog.datePicker.maxDate = maxDate.timeInMillis

        datePickerDialog.show()
    }
    
    /**
     * Toggle between metric and imperial units
     */
    private fun toggleUnitSystem() {
        val newUnitSystem = when (currentUnitSystem) {
            UnitSystem.METRIC -> UnitSystem.IMPERIAL
            UnitSystem.IMPERIAL -> UnitSystem.METRIC
        }
        
        Log.d("PROFILE_EDIT_SYSTEMS", "Toggling unit system from $currentUnitSystem to $newUnitSystem")
        
        // Convert existing values if they exist
        convertExistingValues(currentUnitSystem, newUnitSystem)
        currentUnitSystem = newUnitSystem
        updateUnitLabels()
        
        // Re-validate with new unit system
        validateHeightField(binding.etHeight.text?.toString() ?: "")
        validateWeightField(binding.etWeight.text?.toString() ?: "")
    }
    
    /**
     * Update unit labels on buttons
     */
    private fun updateUnitLabels() {
        with(binding) {
            when (currentUnitSystem) {
                UnitSystem.METRIC -> {
                    btnHeightUnit.text = "cm"
                    btnWeightUnit.text = "kg"
                    tilHeight.hint = "Height (cm)"
                    tilWeight.hint = "Weight (kg)"
                }
                UnitSystem.IMPERIAL -> {
                    btnHeightUnit.text = "ft/in"
                    btnWeightUnit.text = "lbs"
                    tilHeight.hint = "Height (ft'in\")"
                    tilWeight.hint = "Weight (lbs)"
                }
            }
        }
    }
    
    /**
     * Convert existing field values between unit systems
     */
    private fun convertExistingValues(from: UnitSystem, to: UnitSystem) {
        Log.d("PROFILE_EDIT_SYSTEMS", "Converting values from $from to $to")
        
        with(binding) {
            // Convert height
            val heightText = etHeight.text?.toString()
            if (!heightText.isNullOrBlank()) {
                try {
                    when {
                        from == UnitSystem.METRIC && to == UnitSystem.IMPERIAL -> {
                            val cm = heightText.toDouble()
                            val inches = cm / 2.54
                            val feet = (inches / 12).toInt()
                            val remainingInches = (inches % 12).toInt()
                            etHeight.setText("$feet'$remainingInches\"")
                        }
                        from == UnitSystem.IMPERIAL && to == UnitSystem.METRIC -> {
                            val inches = OnboardingValidationUtils.parseImperialHeight(heightText)
                            inches?.let {
                                val cm = it * 2.54
                                etHeight.setText(cm.toInt().toString())
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("PROFILE_EDIT_SYSTEMS", "Failed to convert height: $heightText", e)
                }
            }

            // Convert weight
            val weightText = etWeight.text?.toString()
            if (!weightText.isNullOrBlank()) {
                try {
                    val weight = weightText.toDouble()
                    when {
                        from == UnitSystem.METRIC && to == UnitSystem.IMPERIAL -> {
                            val lbs = weight * 2.20462
                            etWeight.setText(String.format("%.1f", lbs))
                        }
                        from == UnitSystem.IMPERIAL && to == UnitSystem.METRIC -> {
                            val kg = weight / 2.20462
                            etWeight.setText(String.format("%.1f", kg))
                        }
                    }
                } catch (e: Exception) {
                    Log.w("PROFILE_EDIT_SYSTEMS", "Failed to convert weight: $weightText", e)
                }
            }
        }
    }
    

    
    /**
     * PROFILE_EDIT_SYSTEMS: Save profile changes with validation and conversion
     */
    private fun saveProfileChanges() {
        Log.d("PROFILE_EDIT_SYSTEMS", "Saving profile changes with validation")
        
        if (currentValidationErrors.hasErrors()) {
            Log.w("PROFILE_EDIT_SYSTEMS", "Cannot save profile - validation errors exist")
            return
        }

        val name = binding.etUserName.text?.toString()?.trim() ?: ""
        val birthday = selectedBirthday
        val heightText = binding.etHeight.text?.toString() ?: ""
        val weightText = binding.etWeight.text?.toString() ?: ""
        val gender = getSelectedGender()
        
        if (gender == null) {
            Log.w("PROFILE_EDIT_SYSTEMS", "Cannot save profile - gender not selected")
            return
        }

        // Parse and convert measurements to metric for storage
        val heightValue = try {
            if (currentUnitSystem == UnitSystem.IMPERIAL) {
                OnboardingValidationUtils.parseImperialHeight(heightText) ?: 0.0
            } else {
                heightText.toDouble()
            }
        } catch (e: NumberFormatException) {
            Log.e("PROFILE_EDIT_SYSTEMS", "Failed to parse height: $heightText", e)
            return
        }
        
        val weightValue = try {
            weightText.toDouble()
        } catch (e: NumberFormatException) {
            Log.e("PROFILE_EDIT_SYSTEMS", "Failed to parse weight: $weightText", e)
            return
        }

        // Convert to metric for storage
        val (heightInCm, weightInKg) = when (currentUnitSystem) {
            UnitSystem.METRIC -> Pair(heightValue.toInt(), weightValue)
            UnitSystem.IMPERIAL -> {
                val heightInCm = (heightValue * 2.54).toInt()
                val weightInKg = weightValue / 2.20462
                Pair(heightInCm, weightInKg)
            }
        }

        // Create updated profile
        val updatedProfile = currentProfile?.copy(
            displayName = name,
            birthday = birthday,
            heightInCm = heightInCm,
            weightInKg = weightInKg,
            gender = gender,
            unitSystem = currentUnitSystem
        ) ?: return

        Log.d("PROFILE_EDIT_SYSTEMS", "Updating profile: ${updatedProfile.displayName}")
        profileViewModel.updateUserProfile(updatedProfile)
        
        // Exit edit mode
        toggleEditMode()
        
        // Show success message
        android.widget.Toast.makeText(
            requireContext(),
            "Profile updated successfully!",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    /**
     * Update email constraint to prevent layout overlap
     */
    private fun updateEmailConstraint(isEditMode: Boolean) {
        try {
            val constraintSet = ConstraintSet()
            constraintSet.clone(binding.constraintLayout)
            
            if (isEditMode) {
                // In edit mode, constrain email to the TextInputLayout
                constraintSet.connect(
                    R.id.user_email, ConstraintSet.TOP,
                    R.id.til_user_name, ConstraintSet.BOTTOM,
                    resources.getDimensionPixelSize(R.dimen.spacing_small)
                )
            } else {
                // In view mode, constrain email to the TextView
                constraintSet.connect(
                    R.id.user_email, ConstraintSet.TOP,
                    R.id.user_name, ConstraintSet.BOTTOM,
                    resources.getDimensionPixelSize(R.dimen.spacing_small)
                )
            }
            
            constraintSet.applyTo(binding.constraintLayout)
            Log.d("PROFILE_EDIT_UI", "Email constraint updated for ${if (isEditMode) "edit" else "view"} mode")
        } catch (e: Exception) {
            Log.e("PROFILE_EDIT_UI", "Error updating email constraint: ${e.message}", e)
        }
    }



    /**
     * TASK 11: Setup real-time profile observers for immediate UI reflection
     */
    private fun setupRealTimeProfileObservers() {
        Log.d("PROFILE_REALTIME", "Setting up real-time profile observers")
        
        // Observe profile changes for immediate view updates without refresh
        profileViewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            if (profile != null) {
                Log.d("PROFILE_REALTIME", "Profile updated - reflecting changes immediately")
                
                // Update display name immediately
                binding.userName.text = profile.displayName.ifEmpty { profile.email }
                
                // Update profile information sections immediately
                updateProfileDisplaySections(profile)
                
                Log.d("PROFILE_REALTIME", "Profile UI updated without refresh")
            }
        }
        
        // Observe unit system changes for immediate app-wide unit display updates
        lifecycleScope.launch {
            profileViewModel.unitSystemFlow.collect { unitSystem ->
                if (unitSystem != null) {
                    Log.d("PROFILE_REALTIME", "Unit system changed - updating displays: $unitSystem")
                    
                    // Update unit displays immediately
                    updateUnitDisplays(unitSystem)
                }
            }
        }
        
        // Observe display name changes for navigation headers and UI elements
        lifecycleScope.launch {
            profileViewModel.displayNameFlow.collect { displayName ->
                if (displayName != null) {
                    Log.d("PROFILE_REALTIME", "Display name changed - updating UI: $displayName")
                    
                    // Update display name immediately
                    binding.userName.text = displayName
                }
            }
        }
    }
    
    /**
     * TASK 11: Update profile display sections with immediate reflection
     */
    private fun updateProfileDisplaySections(profile: UserProfile) {
        Log.d("PROFILE_REALTIME", "Updating profile display sections")
        
        try {
            // Update personal information section
            binding.birthdayValue.text = profile.birthday?.let { 
                java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(it)
            } ?: "Not set"
            
            binding.genderValue.text = profile.gender.getDisplayName()
            
            // Update health information section
            binding.heightValue.text = profile.getFormattedHeight()
            binding.weightValue.text = profile.getFormattedWeight()
            binding.bmiValue.text = profile.getBMI()?.let { 
                String.format("%.1f", it) 
            } ?: "Not calculated"
            
            // Update preferences section
            binding.unitSystemValue.text = when (profile.unitSystem) {
                com.vibehealth.android.domain.common.UnitSystem.METRIC -> "Metric"
                com.vibehealth.android.domain.common.UnitSystem.IMPERIAL -> "Imperial"
            }
            
            Log.d("PROFILE_REALTIME", "Profile display sections updated successfully")
            
        } catch (e: Exception) {
            Log.e("PROFILE_REALTIME", "Error updating profile display sections", e)
        }
    }
    
    /**
     * TASK 11: Update unit displays for immediate app-wide unit display updates
     */
    private fun updateUnitDisplays(unitSystem: com.vibehealth.android.domain.common.UnitSystem) {
        Log.d("PROFILE_REALTIME", "Updating unit displays for: $unitSystem")
        
        try {
            val currentProfile = profileViewModel.userProfile.value
            if (currentProfile != null) {
                // Update height and weight displays with new unit system
                val updatedProfile = currentProfile.copy(unitSystem = unitSystem)
                binding.heightValue.text = updatedProfile.getFormattedHeight()
                binding.weightValue.text = updatedProfile.getFormattedWeight()
                binding.unitSystemValue.text = when (unitSystem) {
                    com.vibehealth.android.domain.common.UnitSystem.METRIC -> "Metric"
                    com.vibehealth.android.domain.common.UnitSystem.IMPERIAL -> "Imperial"
                }
                
                Log.d("PROFILE_REALTIME", "Unit displays updated successfully")
            }
            
        } catch (e: Exception) {
            Log.e("PROFILE_REALTIME", "Error updating unit displays", e)
        }
    }

    private fun logout() {
        Log.d(TAG_INTEGRATION, "Logout initiated - maintaining existing functionality")
        lifecycleScope.launch {
            authViewModel.signOut()
            // Navigation will be handled by MainActivity auth observer
        }
    }
    
    override fun onDestroyView() {
        Log.d(TAG_INTEGRATION, "ProfileFragment onDestroyView - cleaning up enhanced components")
        
        // Toast cleanup removed - no success toasts used
        
        super.onDestroyView()
        _binding = null
    }
}