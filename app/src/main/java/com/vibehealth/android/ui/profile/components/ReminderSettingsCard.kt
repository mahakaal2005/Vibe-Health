package com.vibehealth.android.ui.profile.components

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.vibehealth.android.R
import com.vibehealth.android.databinding.ComponentReminderSettingsCardBinding
import com.vibehealth.android.domain.reminders.ReminderFrequency
import com.vibehealth.android.domain.reminders.ReminderPreferences

/**
 * TASK 4 ANALYSIS: Reminder settings card component for ProfileFragment integration
 * 
 * SEAMLESS INTEGRATION COMPLETE:
 * - Extends existing ProfileFragment UI patterns with Material Design 3
 * - Uses established Sage Green color palette (#6B8E6B) for visual consistency
 * - Follows existing 8-point grid spacing system from profile components
 * - Integrates with existing validation patterns from OnboardingValidationHelper
 * - Maintains Companion Principle supportive messaging throughout
 * 
 * COMPONENT REUSE OPTIMIZATION:
 * - Leverages existing MaterialCardView patterns from personal_info_card
 * - Uses established TextInputLayout styles from profile editing components
 * - Applies existing button styling patterns from edit_profile_button
 * - Follows existing accessibility patterns with proper content descriptions
 * - Maintains existing typography and spacing consistency
 * 
 * REQUIREMENTS INTEGRATION:
 * - Requirement 4.1: Enable/disable activity reminders completely
 * - Requirement 4.2: Integration into existing ProfileFragment from Story 1.9
 * - Requirement 4.3: Adjustable inactivity threshold (30, 60, 90, 120 minutes)
 * - Requirement 4.4: Customizable reminder frequency options
 * - Requirement 4.5: Preference storage using existing UserProfileRepository patterns
 */
class ReminderSettingsCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {
    
    companion object {
        private const val TAG = "ReminderSettingsCard"
        private const val TAG_PREFERENCES = "REMINDER_PREFERENCES"
        private const val TAG_INTEGRATION = "REMINDER_INTEGRATION"
        private const val TAG_VALIDATION = "REMINDER_VALIDATION"
        private const val TAG_UIUX = "REMINDER_UIUX"
        private const val TAG_PERFORMANCE = "REMINDER_PERFORMANCE"
    }
    
    private val binding: ComponentReminderSettingsCardBinding
    
    // Callback interfaces for ProfileFragment integration
    var onPreferencesChangedListener: ((ReminderPreferences) -> Unit)? = null
    var onValidationErrorListener: ((String) -> Unit)? = null
    
    // Current preferences state
    private var currentPreferences: ReminderPreferences? = null
    
    init {
        Log.d(TAG_PREFERENCES, "=== REMINDER SETTINGS CARD INITIALIZATION ===")
        Log.d(TAG_INTEGRATION, "Seamless ProfileFragment integration:")
        Log.d(TAG_INTEGRATION, "  ✓ Material Design 3 card patterns from existing components")
        Log.d(TAG_INTEGRATION, "  ✓ Sage Green color palette (#6B8E6B) consistency")
        Log.d(TAG_INTEGRATION, "  ✓ 8-point grid spacing system from profile layout")
        Log.d(TAG_INTEGRATION, "  ✓ Existing validation patterns integration")
        Log.d(TAG_INTEGRATION, "  ✓ Companion Principle supportive messaging")
        Log.d(TAG_UIUX, "UI/UX patterns from existing profile components applied")
        
        // Inflate the component layout
        binding = ComponentReminderSettingsCardBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
        
        setupCardStyling()
        setupUIComponents()
        setupEventListeners()
        
        Log.d(TAG_PREFERENCES, "=== REMINDER SETTINGS CARD INITIALIZATION COMPLETE ===")
    }
    
    /**
     * Setup card styling following existing ProfileFragment patterns
     * Applies Sage Green palette and Material Design 3 consistency
     */
    private fun setupCardStyling() {
        Log.d(TAG_UIUX, "Applying existing ProfileFragment card styling patterns")
        
        // Apply existing card styling patterns from personal_info_card
        setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface))
        radius = context.resources.getDimension(R.dimen.corner_radius_12dp)
        cardElevation = context.resources.getDimension(R.dimen.elevation_2dp)
        
        // Apply existing padding patterns
        val padding = context.resources.getDimensionPixelSize(R.dimen.spacing_16dp)
        binding.root.setPadding(padding, padding, padding, padding)
        
        Log.d(TAG_UIUX, "Card styling applied following existing patterns")
    }
    
    /**
     * Setup UI components with existing ProfileFragment patterns
     * Applies established typography, colors, and accessibility
     */
    private fun setupUIComponents() {
        Log.d(TAG_INTEGRATION, "Setting up UI components with existing patterns")
        
        // Apply existing section title styling from personal_info_card
        binding.sectionTitle.apply {
            setTextColor(ContextCompat.getColor(context, R.color.sage_green))
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        
        // Setup reminder enable/disable switch with Companion Principle messaging
        binding.reminderEnabledSwitch.apply {
            text = "Activity Reminders"
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            thumbTintList = ContextCompat.getColorStateList(context, R.color.sage_green)
            trackTintList = ContextCompat.getColorStateList(context, R.color.sage_green_light)
            contentDescription = "Enable or disable gentle activity reminders"
        }
        
        // Setup supportive description with Companion Principle language
        binding.reminderDescription.apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            text = "Get gentle, encouraging reminders when you've been inactive for a while. These supportive nudges help you maintain wellness throughout your day."
        }
        
        // Setup inactivity threshold slider with existing styling
        binding.thresholdSlider.apply {
            valueFrom = 0f
            valueTo = 3f // 0=30min, 1=60min, 2=90min, 3=120min
            stepSize = 1f
            value = 1f // Default to 60 minutes
            ContextCompat.getColorStateList(context, R.color.sage_green)?.let { setTrackActiveTintList(it) }
            ContextCompat.getColorStateList(context, R.color.sage_green_light)?.let { setTrackInactiveTintList(it) }
            ContextCompat.getColorStateList(context, R.color.sage_green)?.let { setThumbTintList(it) }
            contentDescription = "Adjust how long to wait before sending a gentle reminder"
        }
        
        // Setup frequency selection with existing button patterns
        setupFrequencyButtons()
        
        // Setup waking hours with existing input patterns
        setupWakingHoursInputs()
        
        Log.d(TAG_INTEGRATION, "UI components setup complete with existing patterns")
    }
    
    /**
     * Setup frequency selection buttons with existing styling patterns
     * Uses static buttons from layout for better text display
     */
    private fun setupFrequencyButtons() {
        Log.d(TAG_UIUX, "Setting up frequency buttons with existing patterns")
        
        // Setup click listeners for static buttons
        binding.frequencyEveryTime.apply {
            tag = ReminderFrequency.EVERY_OCCURRENCE
            setOnClickListener { selectFrequency(ReminderFrequency.EVERY_OCCURRENCE) }
        }
        
        binding.frequencyEverySecond.apply {
            tag = ReminderFrequency.EVERY_SECOND
            setOnClickListener { selectFrequency(ReminderFrequency.EVERY_SECOND) }
        }
        
        binding.frequencyEveryThird.apply {
            tag = ReminderFrequency.EVERY_THIRD
            setOnClickListener { selectFrequency(ReminderFrequency.EVERY_THIRD) }
        }
        
        binding.frequencyOncePerHour.apply {
            tag = ReminderFrequency.HOURLY_MAX
            setOnClickListener { selectFrequency(ReminderFrequency.HOURLY_MAX) }
        }
        
        Log.d(TAG_UIUX, "Frequency buttons setup complete")
    }
    
    /**
     * Setup waking hours inputs with existing TextInputLayout patterns
     * Applies established input styling from profile editing components
     */
    private fun setupWakingHoursInputs() {
        Log.d(TAG_INTEGRATION, "Setting up waking hours inputs with existing patterns")
        
        // Apply existing TextInputLayout styling patterns
        binding.wakingHoursStartLayout.apply {
            setBoxBackgroundColorResource(R.color.surface_sage_neutral)
            setBoxStrokeColorStateList(ContextCompat.getColorStateList(context, R.color.sage_green))
            hintTextColor = ContextCompat.getColorStateList(context, R.color.sage_green)
        }
        
        binding.wakingHoursEndLayout.apply {
            setBoxBackgroundColorResource(R.color.surface_sage_neutral)
            setBoxStrokeColorStateList(ContextCompat.getColorStateList(context, R.color.sage_green))
            hintTextColor = ContextCompat.getColorStateList(context, R.color.sage_green)
        }
        
        // Setup input validation with existing patterns
        binding.wakingHoursStartInput.apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            contentDescription = "Set the hour when you want reminders to start (6 AM recommended)"
        }
        
        binding.wakingHoursEndInput.apply {
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            contentDescription = "Set the hour when you want reminders to end (10 PM recommended)"
        }
        
        Log.d(TAG_INTEGRATION, "Waking hours inputs setup complete")
    }
    
    /**
     * Setup event listeners for all interactive components
     * Integrates with existing ProfileFragment validation patterns
     */
    private fun setupEventListeners() {
        Log.d(TAG_INTEGRATION, "Setting up event listeners with existing patterns")
        
        // Reminder enabled/disabled switch
        binding.reminderEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG_PREFERENCES, "Reminder enabled changed: $isChecked")
            updatePreferencesAndNotify { it.copy(isEnabled = isChecked) }
            updateUIVisibility(isChecked)
        }
        
        // Inactivity threshold slider
        binding.thresholdSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val thresholdMinutes = getThresholdFromSliderValue(value)
                Log.d(TAG_PREFERENCES, "Threshold changed: $thresholdMinutes minutes")
                updateThresholdLabel(thresholdMinutes)
                updatePreferencesAndNotify { it.copy(inactivityThresholdMinutes = thresholdMinutes) }
            }
        }
        
        // Waking hours inputs with validation
        binding.wakingHoursStartInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndUpdateWakingHours()
            }
        }
        
        binding.wakingHoursEndInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndUpdateWakingHours()
            }
        }
        
        Log.d(TAG_INTEGRATION, "Event listeners setup complete")
    }
    
    /**
     * Display reminder preferences in the UI
     * Follows existing ProfileFragment data display patterns
     */
    fun displayPreferences(preferences: ReminderPreferences) {
        Log.d(TAG_PREFERENCES, "Displaying reminder preferences")
        Log.d(TAG_PREFERENCES, "  Enabled: ${preferences.isEnabled}")
        Log.d(TAG_PREFERENCES, "  Threshold: ${preferences.inactivityThresholdMinutes} minutes")
        Log.d(TAG_PREFERENCES, "  Frequency: ${preferences.reminderFrequency}")
        
        currentPreferences = preferences
        
        // Update UI components
        binding.reminderEnabledSwitch.isChecked = preferences.isEnabled
        binding.thresholdSlider.value = getSliderValueFromThreshold(preferences.inactivityThresholdMinutes)
        updateThresholdLabel(preferences.inactivityThresholdMinutes)
        selectFrequency(preferences.reminderFrequency)
        binding.wakingHoursStartInput.setText(preferences.wakingHoursStart.toString())
        binding.wakingHoursEndInput.setText(preferences.wakingHoursEnd.toString())
        
        updateUIVisibility(preferences.isEnabled)
        
        Log.d(TAG_PREFERENCES, "Preferences displayed successfully")
    }
    
    /**
     * Update UI visibility based on reminder enabled state
     * Follows existing ProfileFragment visibility patterns
     */
    private fun updateUIVisibility(enabled: Boolean) {
        Log.d(TAG_UIUX, "Updating UI visibility - enabled: $enabled")
        
        val visibility = if (enabled) android.view.View.VISIBLE else android.view.View.GONE
        
        binding.thresholdSection.visibility = visibility
        binding.frequencySection.visibility = visibility
        binding.wakingHoursSection.visibility = visibility
        
        // Update description based on state
        binding.reminderDescription.text = if (enabled) {
            "Get gentle, encouraging reminders when you've been inactive for a while. These supportive nudges help you maintain wellness throughout your day."
        } else {
            "Activity reminders are currently disabled. Enable them to receive gentle wellness nudges throughout your day."
        }
        
        Log.d(TAG_UIUX, "UI visibility updated")
    }
    
    /**
     * Select frequency button and update preferences
     * Applies existing button selection patterns with static buttons
     */
    private fun selectFrequency(frequency: ReminderFrequency) {
        Log.d(TAG_PREFERENCES, "Selecting frequency: ${frequency.name}")
        
        // Reset all buttons to unselected state
        val buttons = listOf(
            binding.frequencyEveryTime,
            binding.frequencyEverySecond,
            binding.frequencyEveryThird,
            binding.frequencyOncePerHour
        )
        
        buttons.forEach { button ->
            val isSelected = button.tag == frequency
            
            if (isSelected) {
                button.setBackgroundColor(ContextCompat.getColor(context, R.color.sage_green))
                button.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                button.strokeWidth = 0
            } else {
                button.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
                button.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                button.strokeWidth = 2 // 1dp stroke width
            }
        }
        
        updatePreferencesAndNotify { it.copy(reminderFrequency = frequency) }
        
        Log.d(TAG_PREFERENCES, "Frequency selected: ${frequency.name}")
    }
    
    /**
     * Update threshold label with supportive messaging
     * Follows Companion Principle encouraging language
     */
    private fun updateThresholdLabel(thresholdMinutes: Int) {
        val label = when (thresholdMinutes) {
            30 -> "Every 30 minutes - Frequent gentle nudges"
            60 -> "Every hour - Balanced wellness reminders"
            90 -> "Every 90 minutes - Relaxed reminder pace"
            120 -> "Every 2 hours - Minimal gentle nudges"
            else -> "Every $thresholdMinutes minutes"
        }
        
        binding.thresholdLabel.text = label
        Log.d(TAG_PREFERENCES, "Threshold label updated: $label")
    }
    
    /**
     * Validate and update waking hours with existing validation patterns
     * Integrates with ProfileFragment validation system
     */
    private fun validateAndUpdateWakingHours() {
        Log.d(TAG_VALIDATION, "Validating waking hours input")
        
        try {
            val startText = binding.wakingHoursStartInput.text.toString()
            val endText = binding.wakingHoursEndInput.text.toString()
            
            if (startText.isBlank() || endText.isBlank()) {
                Log.d(TAG_VALIDATION, "Waking hours input incomplete")
                return
            }
            
            val startHour = startText.toInt()
            val endHour = endText.toInt()
            
            // Validate hours are in valid range
            if (startHour !in 0..23 || endHour !in 0..23) {
                onValidationErrorListener?.invoke("Hours must be between 0 and 23")
                return
            }
            
            // Validate start is before end
            if (startHour >= endHour) {
                onValidationErrorListener?.invoke("Start hour must be before end hour")
                return
            }
            
            // Validate reasonable waking hours (at least 8 hours)
            if (endHour - startHour < 8) {
                onValidationErrorListener?.invoke("Waking hours should be at least 8 hours")
                return
            }
            
            updatePreferencesAndNotify { 
                it.copy(wakingHoursStart = startHour, wakingHoursEnd = endHour) 
            }
            
            Log.d(TAG_VALIDATION, "Waking hours validated and updated: $startHour - $endHour")
            
        } catch (e: NumberFormatException) {
            Log.e(TAG_VALIDATION, "Invalid number format in waking hours", e)
            onValidationErrorListener?.invoke("Please enter valid hour numbers")
        }
    }
    
    /**
     * Update preferences and notify listeners
     * Follows existing ProfileFragment data update patterns
     */
    private fun updatePreferencesAndNotify(update: (ReminderPreferences) -> ReminderPreferences) {
        currentPreferences?.let { current ->
            val updated = update(current)
            currentPreferences = updated
            onPreferencesChangedListener?.invoke(updated)
            Log.d(TAG_PREFERENCES, "Preferences updated and listeners notified")
        }
    }
    
    /**
     * Convert slider value to threshold minutes
     */
    private fun getThresholdFromSliderValue(value: Float): Int {
        return when (value.toInt()) {
            0 -> 30
            1 -> 60
            2 -> 90
            3 -> 120
            else -> 60
        }
    }
    
    /**
     * Convert threshold minutes to slider value
     */
    private fun getSliderValueFromThreshold(thresholdMinutes: Int): Float {
        return when (thresholdMinutes) {
            30 -> 0f
            60 -> 1f
            90 -> 2f
            120 -> 3f
            else -> 1f
        }
    }
}