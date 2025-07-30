package com.vibehealth.android.ui.onboarding.accessibility

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.material.textfield.TextInputLayout
import com.vibehealth.android.R
import com.vibehealth.android.core.accessibility.AccessibilityHelper

import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Accessibility manager specifically for onboarding screens
 * Ensures WCAG 2.1 Level AA compliance for onboarding flow
 */
@Singleton
class OnboardingAccessibilityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accessibilityHelper: AccessibilityHelper
) {

    /**
     * Set up accessibility for welcome screen
     */
    fun setupWelcomeScreenAccessibility(rootView: ViewGroup) {
        // Set up content descriptions
        rootView.findViewById<TextView>(R.id.welcome_heading)?.let { titleView ->
            accessibilityHelper.setContentDescription(
                titleView,
                context.getString(R.string.welcome_to_vibe_health),
                "Main heading for onboarding"
            )
            accessibilityHelper.setAccessibilityRole(titleView, "heading")
        }

        rootView.findViewById<TextView>(R.id.subtitle)?.let { subtitleView ->
            accessibilityHelper.setContentDescription(
                subtitleView,
                context.getString(R.string.personalize_wellness_journey),
                "Description of onboarding purpose"
            )
        }

        rootView.findViewById<Button>(R.id.btn_get_started)?.let { button ->
            accessibilityHelper.setContentDescription(
                button,
                context.getString(R.string.get_started),
                "Button to begin onboarding process"
            )
            accessibilityHelper.ensureMinimumTouchTarget(button)
        }

        // Set up keyboard navigation
        accessibilityHelper.setupKeyboardNavigation(rootView)
        
        // Apply responsive adjustments
        applyResponsiveAccessibility(rootView)
    }

    /**
     * Set up accessibility for personal info screen
     */
    fun setupPersonalInfoScreenAccessibility(rootView: ViewGroup) {
        // Name input field
        rootView.findViewById<TextInputLayout>(R.id.til_full_name)?.let { nameLayout ->
            val editText = nameLayout.editText
            editText?.let { input ->
                accessibilityHelper.setContentDescription(
                    input,
                    context.getString(R.string.full_name),
                    "Required field for your full name"
                )
                
                // Set up input validation announcements
                setupInputValidationAccessibility(input, nameLayout)
            }
        }

        // Birthday picker
        rootView.findViewById<View>(R.id.btn_birthday_picker)?.let { birthdayButton ->
            accessibilityHelper.setContentDescription(
                birthdayButton,
                context.getString(R.string.select_date),
                "Button to open date picker for birthday"
            )
            accessibilityHelper.setAccessibilityRole(birthdayButton, "button")
            accessibilityHelper.ensureMinimumTouchTarget(birthdayButton)
        }

        // Continue button
        rootView.findViewById<Button>(R.id.btn_continue)?.let { button ->
            accessibilityHelper.setContentDescription(
                button,
                context.getString(R.string.continue_button),
                "Continue to next step of onboarding"
            )
            accessibilityHelper.ensureMinimumTouchTarget(button)
        }

        // Helper text
        rootView.findViewById<TextView>(R.id.tv_helper_text)?.let { helperText ->
            accessibilityHelper.setContentDescription(
                helperText,
                context.getString(R.string.helps_calculate_goals),
                "Explanation of why this information is needed"
            )
        }

        setupFormAccessibility(rootView)
    }

    /**
     * Set up accessibility for physical info screen
     */
    fun setupPhysicalInfoScreenAccessibility(rootView: ViewGroup) {
        // Unit system buttons
        rootView.findViewById<View>(R.id.btn_height_unit)?.let { heightUnitButton ->
            accessibilityHelper.setContentDescription(
                heightUnitButton,
                "Height unit toggle",
                "Tap to switch between centimeters and feet"
            )
            accessibilityHelper.ensureMinimumTouchTarget(heightUnitButton)
        }
        
        rootView.findViewById<View>(R.id.btn_weight_unit)?.let { weightUnitButton ->
            accessibilityHelper.setContentDescription(
                weightUnitButton,
                "Weight unit toggle", 
                "Tap to switch between kilograms and pounds"
            )
            accessibilityHelper.ensureMinimumTouchTarget(weightUnitButton)
        }

        // Height input
        rootView.findViewById<TextInputLayout>(R.id.til_height)?.let { heightLayout ->
            val editText = heightLayout.editText
            editText?.let { input ->
                accessibilityHelper.setContentDescription(
                    input,
                    "Height input",
                    "Enter your height in the selected unit system"
                )
                setupInputValidationAccessibility(input, heightLayout)
            }
        }

        // Weight input
        rootView.findViewById<TextInputLayout>(R.id.til_weight)?.let { weightLayout ->
            val editText = weightLayout.editText
            editText?.let { input ->
                accessibilityHelper.setContentDescription(
                    input,
                    "Weight input",
                    "Enter your weight in the selected unit system"
                )
                setupInputValidationAccessibility(input, weightLayout)
            }
        }

        // Gender selection
        setupGenderSelectionAccessibility(rootView)
        
        setupFormAccessibility(rootView)
    }

    /**
     * Set up accessibility for completion screen
     */
    fun setupCompletionScreenAccessibility(rootView: ViewGroup) {
        // Success message
        rootView.findViewById<TextView>(R.id.tv_completion_title)?.let { titleView ->
            accessibilityHelper.setContentDescription(
                titleView,
                context.getString(R.string.youre_all_set),
                "Onboarding completed successfully"
            )
            accessibilityHelper.setAccessibilityRole(titleView, "heading")
        }

        // Status message
        rootView.findViewById<TextView>(R.id.tv_calculating)?.let { statusView ->
            accessibilityHelper.setContentDescription(
                statusView,
                "Goal calculation status",
                "Current status of goal calculation process"
            )
            
            // Set up live region for status updates
            ViewCompat.setAccessibilityLiveRegion(statusView, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE)
        }

        // Enter app button
        rootView.findViewById<Button>(R.id.btn_enter_app)?.let { button ->
            accessibilityHelper.setContentDescription(
                button,
                context.getString(R.string.enter_vibe_health),
                "Enter the main application"
            )
            accessibilityHelper.ensureMinimumTouchTarget(button)
        }

        // Success icon
        rootView.findViewById<View>(R.id.iv_success_icon)?.let { icon ->
            accessibilityHelper.setContentDescription(
                icon,
                "Success checkmark",
                "Visual indicator of successful completion"
            )
        }

        applyResponsiveAccessibility(rootView)
    }

    /**
     * Set up gender selection accessibility with inclusive language
     */
    private fun setupGenderSelectionAccessibility(rootView: ViewGroup) {
        val genderOptions = listOf(
            R.id.rb_male to context.getString(R.string.male),
            R.id.rb_female to context.getString(R.string.female),
            R.id.rb_other to context.getString(R.string.other),
            R.id.rb_prefer_not_to_say to context.getString(R.string.prefer_not_to_say)
        )

        genderOptions.forEach { (id, label) ->
            rootView.findViewById<RadioButton>(id)?.let { radioButton ->
                accessibilityHelper.setContentDescription(
                    radioButton,
                    label,
                    "Gender option"
                )
                accessibilityHelper.ensureMinimumTouchTarget(radioButton)
                
                // Set up state change announcements
                radioButton.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        accessibilityHelper.announceForAccessibility(
                            radioButton,
                            "$label selected"
                        )
                    }
                }
            }
        }
    }

    /**
     * Set up input validation accessibility
     */
    private fun setupInputValidationAccessibility(editText: EditText, textInputLayout: TextInputLayout) {
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // Announce validation errors when focus leaves the field
                textInputLayout.error?.let { errorMessage ->
                    accessibilityHelper.announceForAccessibility(
                        editText,
                        "Error: $errorMessage"
                    )
                }
            }
        }

        // Set up error announcements
        textInputLayout.setErrorTextAppearance(R.style.VibeHealth_Text_Error)
    }

    /**
     * Set up general form accessibility
     */
    private fun setupFormAccessibility(rootView: ViewGroup) {
        // Set up keyboard navigation
        accessibilityHelper.setupKeyboardNavigation(rootView)
        
        // Apply responsive adjustments
        applyResponsiveAccessibility(rootView)
        
        // Set up focus indicators
        setupFocusIndicators(rootView)
        
        // Handle high contrast mode
        applyHighContrastIfNeeded(rootView)
    }

    /**
     * Apply responsive accessibility adjustments
     */
    private fun applyResponsiveAccessibility(rootView: ViewGroup) {
        // Apply standard accessibility features for Android phones
        
        for (i in 0 until rootView.childCount) {
            val child = rootView.getChildAt(i)
            
            when (child) {
                is Button -> {
                    accessibilityHelper.ensureMinimumTouchTarget(child)
                }
                is TextView -> {
                    // Apply dynamic font scaling
                    val baseTextSize = child.textSize
                    accessibilityHelper.applyDynamicFontScaling(child, baseTextSize)
                }
                is ViewGroup -> {
                    applyResponsiveAccessibility(child)
                }
            }
        }
    }

    /**
     * Set up focus indicators for keyboard navigation
     */
    private fun setupFocusIndicators(rootView: ViewGroup) {
        val focusableViews = mutableListOf<View>()
        
        // Find all focusable views
        findFocusableViews(rootView, focusableViews)
        
        // Set up focus indicators
        focusableViews.forEach { view ->
            accessibilityHelper.setupFocusIndicators(view)
        }
    }

    /**
     * Recursively find focusable views
     */
    private fun findFocusableViews(viewGroup: ViewGroup, focusableViews: MutableList<View>) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            
            if (child.isFocusable) {
                focusableViews.add(child)
            }
            
            if (child is ViewGroup) {
                findFocusableViews(child, focusableViews)
            }
        }
    }

    /**
     * Apply high contrast colors if needed
     */
    private fun applyHighContrastIfNeeded(rootView: ViewGroup) {
        if (accessibilityHelper.isHighContrastEnabled()) {
            for (i in 0 until rootView.childCount) {
                val child = rootView.getChildAt(i)
                accessibilityHelper.applyHighContrastIfNeeded(child)
                
                if (child is ViewGroup) {
                    applyHighContrastIfNeeded(child)
                }
            }
        }
    }

    /**
     * Set up voice input compatibility
     */
    fun setupVoiceInputCompatibility(editText: EditText) {
        editText.setOnLongClickListener {
            // Enable voice input hint
            accessibilityHelper.announceForAccessibility(
                editText,
                "Long press to use voice input"
            )
            false // Allow default long press behavior
        }
    }

    /**
     * Handle reduced motion preferences
     */
    fun getAccessibleAnimationDuration(defaultDuration: Long): Long {
        return accessibilityHelper.getAccessibleAnimationDuration(defaultDuration)
    }

    /**
     * Set up progress announcements for screen readers
     */
    fun announceProgressUpdate(view: View, currentStep: Int, totalSteps: Int) {
        val progressMessage = "Step $currentStep of $totalSteps"
        accessibilityHelper.announceForAccessibility(view, progressMessage)
    }

    /**
     * Set up error recovery announcements
     */
    fun announceErrorRecovery(view: View, errorMessage: String, recoveryAction: String) {
        val fullMessage = "$errorMessage. $recoveryAction"
        accessibilityHelper.announceForAccessibility(view, fullMessage)
    }
}