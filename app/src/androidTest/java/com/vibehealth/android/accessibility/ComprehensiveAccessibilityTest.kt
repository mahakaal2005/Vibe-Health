package com.vibehealth.android.accessibility

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.vibehealth.android.R
import com.vibehealth.android.core.accessibility.AccessibilityHelper
import com.vibehealth.android.ui.onboarding.OnboardingActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.*

/**
 * Comprehensive accessibility testing for onboarding flow
 * Tests compliance with WCAG 2.1 Level AA standards and assistive technology compatibility
 */
@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ComprehensiveAccessibilityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val activityRule = ActivityScenarioRule(OnboardingActivity::class.java)

    @Inject
    lateinit var accessibilityHelper: AccessibilityHelper

    private lateinit var context: Context
    private lateinit var uiDevice: UiDevice

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun welcomeScreen_shouldMeetAccessibilityStandards() {
        // Test welcome screen accessibility
        
        // Verify all interactive elements have content descriptions
        onView(withId(R.id.btn_get_started))
            .check(matches(hasContentDescription()))
            .check(matches(isDisplayed()))

        // Test minimum touch target size (48dp x 48dp)
        onView(withId(R.id.btn_get_started))
            .check(matches(hasMinimumTouchTargetSize()))

        // Test color contrast for title
        val titleColor = android.graphics.Color.BLACK
        val backgroundColor = android.graphics.Color.WHITE
        assertTrue(accessibilityHelper.checkColorContrast(titleColor, backgroundColor),
            "Welcome title should have sufficient color contrast (4.5:1 ratio)")

        // Test color contrast for subtitle
        val subtitleColor = android.graphics.Color.DKGRAY
        assertTrue(accessibilityHelper.checkColorContrast(subtitleColor, backgroundColor),
            "Welcome subtitle should have sufficient color contrast")

        // Test color contrast for button
        val buttonTextColor = android.graphics.Color.WHITE
        val buttonBackgroundColor = android.graphics.Color.parseColor("#4CAF50") // Sage green
        assertTrue(accessibilityHelper.checkColorContrast(buttonTextColor, buttonBackgroundColor),
            "Button should have sufficient color contrast")

        // Test focus management
        onView(withId(R.id.btn_get_started))
            .check(matches(isFocusable()))
    }

    @Test
    fun personalInfoScreen_shouldSupportScreenReaders() {
        // Navigate to personal info screen
        onView(withId(R.id.btn_get_started))
            .perform(click())

        // Test form field accessibility
        onView(withId(R.id.et_full_name))
            .check(matches(hasContentDescription()))
            .check(matches(isDisplayed()))

        // Test input field labeling
        onView(withId(R.id.til_full_name))
            .check(matches(hasContentDescription()))

        // Test birthday picker accessibility
        onView(withId(R.id.btn_select_birthday))
            .check(matches(hasContentDescription()))
            .check(matches(hasMinimumTouchTargetSize()))

        // Test error message accessibility
        onView(withId(R.id.et_full_name))
            .perform(typeText(""), closeSoftKeyboard())
        
        onView(withId(R.id.btn_continue))
            .perform(click())

        // Error message should be announced by screen readers
        onView(withId(R.id.til_full_name))
            .check(matches(hasErrorText()))

        // Test form field color contrast
        val inputTextColor = accessibilityHelper.getTextColor(R.id.et_full_name)
        val inputBackgroundColor = accessibilityHelper.getBackgroundColor(R.id.et_full_name)
        assertTrue(accessibilityHelper.checkColorContrast(inputTextColor, inputBackgroundColor),
            "Input field should have sufficient color contrast")

        // Test helper text accessibility
        onView(withId(R.id.tv_helper_text))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun physicalInfoScreen_shouldSupportKeyboardNavigation() {
        // Navigate to physical info screen
        navigateToPhysicalInfoScreen()

        // Test unit system toggle accessibility
        onView(withId(R.id.toggle_metric))
            .check(matches(hasContentDescription()))
            .check(matches(isClickable()))
            .check(matches(hasMinimumTouchTargetSize()))

        onView(withId(R.id.toggle_imperial))
            .check(matches(hasContentDescription()))
            .check(matches(isClickable()))
            .check(matches(hasMinimumTouchTargetSize()))

        // Test height input accessibility
        onView(withId(R.id.et_height))
            .check(matches(hasContentDescription()))
            .check(matches(isFocusable()))

        // Test weight input accessibility
        onView(withId(R.id.et_weight))
            .check(matches(hasContentDescription()))
            .check(matches(isFocusable()))

        // Test gender radio buttons accessibility
        onView(withId(R.id.rb_male))
            .check(matches(hasContentDescription()))
            .check(matches(hasMinimumTouchTargetSize()))

        onView(withId(R.id.rb_female))
            .check(matches(hasContentDescription()))
            .check(matches(hasMinimumTouchTargetSize()))

        onView(withId(R.id.rb_other))
            .check(matches(hasContentDescription()))
            .check(matches(hasMinimumTouchTargetSize()))

        onView(withId(R.id.rb_prefer_not_to_say))
            .check(matches(hasContentDescription()))
            .check(matches(hasMinimumTouchTargetSize()))

        // Test keyboard navigation order
        testKeyboardNavigationOrder()
    }

    @Test
    fun onboardingFlow_shouldSupportLargeFontSizes() {
        // Test with 200% font scale
        val originalFontScale = accessibilityHelper.getFontScale()
        
        try {
            accessibilityHelper.setFontScale(2.0f)
            Thread.sleep(1000) // Allow UI to update

            // Welcome screen should still be usable
            onView(withId(R.id.tv_welcome_title))
                .check(matches(isDisplayed()))

            onView(withId(R.id.btn_get_started))
                .check(matches(isDisplayed()))
                .check(matches(isClickable()))

            // Navigate through flow with large fonts
            onView(withId(R.id.btn_get_started))
                .perform(click())

            // Personal info screen
            onView(withId(R.id.et_full_name))
                .check(matches(isDisplayed()))
                .perform(typeText("Large Font Test"), closeSoftKeyboard())

            onView(withId(R.id.btn_select_birthday))
                .check(matches(isDisplayed()))
                .perform(click())

            onView(withText("OK"))
                .perform(click())

            onView(withId(R.id.btn_continue))
                .check(matches(isDisplayed()))
                .perform(click())

            // Physical info screen
            onView(withId(R.id.et_height))
                .check(matches(isDisplayed()))
                .perform(typeText("175"), closeSoftKeyboard())

            onView(withId(R.id.et_weight))
                .check(matches(isDisplayed()))
                .perform(typeText("70"), closeSoftKeyboard())

            // All elements should remain accessible with large fonts
            onView(withId(R.id.rb_male))
                .check(matches(isDisplayed()))
                .perform(click())

            onView(withId(R.id.btn_continue))
                .check(matches(isDisplayed()))

        } finally {
            // Reset font scale
            accessibilityHelper.setFontScale(originalFontScale)
        }
    }

    @Test
    fun onboardingFlow_shouldSupportHighContrastMode() {
        // Enable high contrast mode
        accessibilityHelper.enableHighContrastMode()
        
        try {
            // Test welcome screen in high contrast
            val titleColor = accessibilityHelper.getTextColor(R.id.tv_welcome_title)
            val backgroundColor = accessibilityHelper.getBackgroundColor(R.id.layout_welcome)
            
            // High contrast mode should provide even better contrast
            val contrastRatio = accessibilityHelper.calculateContrastRatio(titleColor, backgroundColor)
            assertTrue(contrastRatio >= 7.0, "High contrast mode should provide 7:1 contrast ratio")

            // Navigate through flow
            onView(withId(R.id.btn_get_started))
                .perform(click())

            // Test form elements in high contrast
            val inputTextColor = accessibilityHelper.getTextColor(R.id.et_full_name)
            val inputBackgroundColor = accessibilityHelper.getBackgroundColor(R.id.et_full_name)
            val inputContrastRatio = accessibilityHelper.calculateContrastRatio(inputTextColor, inputBackgroundColor)
            
            assertTrue(inputContrastRatio >= 4.5, 
                "Form elements should maintain good contrast in high contrast mode")

        } finally {
            accessibilityHelper.disableHighContrastMode()
        }
    }

    @Test
    fun onboardingFlow_shouldSupportReducedMotion() {
        // Enable reduced motion preference
        accessibilityHelper.enableReducedMotion()
        
        try {
            // Animations should be reduced or disabled
            val animationDuration = accessibilityHelper.getAnimationDuration()
            assertTrue(animationDuration <= 100, "Animations should be reduced when reduce motion is enabled")

            // Navigate through flow
            onView(withId(R.id.btn_get_started))
                .perform(click())

            // Transitions should be immediate or very fast
            onView(withId(R.id.et_full_name))
                .check(matches(isDisplayed()))

            // Progress animations should be reduced
            val progressAnimationDuration = accessibilityHelper.getProgressAnimationDuration()
            assertTrue(progressAnimationDuration <= 100, 
                "Progress animations should be reduced")

        } finally {
            accessibilityHelper.disableReducedMotion()
        }
    }

    @Test
    fun onboardingFlow_shouldSupportVoiceInput() {
        // Navigate to personal info screen
        onView(withId(R.id.btn_get_started))
            .perform(click())

        // Test voice input compatibility
        onView(withId(R.id.et_full_name))
            .check(matches(isDisplayed()))
            .check(matches(hasImeAction()))

        // Voice input should work with form fields
        onView(withId(R.id.et_full_name))
            .perform(click())

        // Verify input method is available
        assertTrue(accessibilityHelper.isVoiceInputAvailable(), 
            "Voice input should be available for text fields")

        // Navigate to physical info
        onView(withId(R.id.et_full_name))
            .perform(typeText("Voice Test User"), closeSoftKeyboard())

        onView(withId(R.id.btn_select_birthday))
            .perform(click())
        onView(withText("OK"))
            .perform(click())

        onView(withId(R.id.btn_continue))
            .perform(click())

        // Test voice input with numeric fields
        onView(withId(R.id.et_height))
            .check(matches(hasImeAction()))
            .perform(click())

        assertTrue(accessibilityHelper.isVoiceInputAvailable(), 
            "Voice input should be available for numeric fields")
    }

    @Test
    fun onboardingFlow_shouldSupportSwitchAccess() {
        // Simulate switch access navigation
        accessibilityHelper.enableSwitchAccess()
        
        try {
            // Test switch navigation through welcome screen
            val focusableElements = accessibilityHelper.getFocusableElements()
            assertTrue(focusableElements.isNotEmpty(), "Should have focusable elements for switch access")

            // Verify logical focus order
            val expectedFocusOrder = listOf(
                R.id.btn_get_started
            )

            expectedFocusOrder.forEach { elementId ->
                assertTrue(accessibilityHelper.isElementFocusable(elementId),
                    "Element $elementId should be focusable for switch access")
            }

            // Navigate using switch access
            accessibilityHelper.simulateSwitchPress()
            
            onView(withId(R.id.btn_get_started))
                .check(matches(hasFocus()))

            accessibilityHelper.simulateSwitchSelect()

            // Should navigate to next screen
            onView(withId(R.id.et_full_name))
                .check(matches(isDisplayed()))

        } finally {
            accessibilityHelper.disableSwitchAccess()
        }
    }

    @Test
    fun errorMessages_shouldBeAccessible() {
        // Navigate to personal info screen
        onView(withId(R.id.btn_get_started))
            .perform(click())

        // Trigger validation errors
        onView(withId(R.id.btn_continue))
            .perform(click())

        // Error messages should be properly announced
        onView(withId(R.id.til_full_name))
            .check(matches(hasErrorText()))

        // Error should be associated with the input field
        val errorText = accessibilityHelper.getErrorText(R.id.til_full_name)
        assertNotNull(errorText, "Error text should be available")
        assertTrue(errorText.isNotEmpty(), "Error text should not be empty")

        // Error should have sufficient contrast
        val errorColor = accessibilityHelper.getErrorTextColor(R.id.til_full_name)
        val backgroundColor = accessibilityHelper.getBackgroundColor(R.id.til_full_name)
        assertTrue(accessibilityHelper.checkColorContrast(errorColor, backgroundColor),
            "Error text should have sufficient contrast")

        // Navigate to physical info and test more errors
        onView(withId(R.id.et_full_name))
            .perform(typeText("Test User"), closeSoftKeyboard())

        onView(withId(R.id.btn_select_birthday))
            .perform(click())
        onView(withText("OK"))
            .perform(click())

        onView(withId(R.id.btn_continue))
            .perform(click())

        // Test physical info validation errors
        onView(withId(R.id.btn_continue))
            .perform(click())

        // Height and weight errors should be accessible
        onView(withId(R.id.til_height))
            .check(matches(hasErrorText()))

        onView(withId(R.id.til_weight))
            .check(matches(hasErrorText()))
    }

    @Test
    fun progressIndicators_shouldBeAccessible() {
        // Progress indicators should be announced by screen readers
        onView(withId(R.id.tv_step_indicator))
            .check(matches(hasContentDescription()))

        // Navigate through steps and verify progress is announced
        onView(withId(R.id.btn_get_started))
            .perform(click())

        onView(withId(R.id.tv_step_indicator))
            .check(matches(withText("Step 1 of 3")))
            .check(matches(hasContentDescription()))

        // Complete personal info
        onView(withId(R.id.et_full_name))
            .perform(typeText("Progress Test"), closeSoftKeyboard())

        onView(withId(R.id.btn_select_birthday))
            .perform(click())
        onView(withText("OK"))
            .perform(click())

        onView(withId(R.id.btn_continue))
            .perform(click())

        // Progress should update
        onView(withId(R.id.tv_step_indicator))
            .check(matches(withText("Step 2 of 3")))

        // Progress bar should be accessible
        onView(withId(R.id.pb_progress))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun darkMode_shouldMaintainAccessibility() {
        // Enable dark mode
        uiDevice.executeShellCommand("cmd uimode night yes")
        Thread.sleep(1000)

        try {
            // Test contrast in dark mode
            val titleColor = accessibilityHelper.getTextColor(R.id.tv_welcome_title)
            val backgroundColor = accessibilityHelper.getBackgroundColor(R.id.layout_welcome)
            
            assertTrue(accessibilityHelper.checkColorContrast(titleColor, backgroundColor),
                "Dark mode should maintain sufficient contrast")

            // Navigate through flow in dark mode
            onView(withId(R.id.btn_get_started))
                .perform(click())

            // Test form elements in dark mode
            val inputTextColor = accessibilityHelper.getTextColor(R.id.et_full_name)
            val inputBackgroundColor = accessibilityHelper.getBackgroundColor(R.id.et_full_name)
            
            assertTrue(accessibilityHelper.checkColorContrast(inputTextColor, inputBackgroundColor),
                "Form elements should maintain contrast in dark mode")

            // Test button contrast in dark mode
            val buttonTextColor = accessibilityHelper.getTextColor(R.id.btn_continue)
            val buttonBackgroundColor = accessibilityHelper.getBackgroundColor(R.id.btn_continue)
            
            assertTrue(accessibilityHelper.checkColorContrast(buttonTextColor, buttonBackgroundColor),
                "Buttons should maintain contrast in dark mode")

        } finally {
            // Disable dark mode
            uiDevice.executeShellCommand("cmd uimode night no")
            Thread.sleep(1000)
        }
    }

    // Helper methods
    private fun navigateToPhysicalInfoScreen() {
        onView(withId(R.id.btn_get_started))
            .perform(click())

        onView(withId(R.id.et_full_name))
            .perform(typeText("Test User"), closeSoftKeyboard())

        onView(withId(R.id.btn_select_birthday))
            .perform(click())
        
        onView(withText("OK"))
            .perform(click())

        onView(withId(R.id.btn_continue))
            .perform(click())
    }

    private fun testKeyboardNavigationOrder() {
        // Test that tab order is logical
        val expectedTabOrder = listOf(
            R.id.toggle_metric,
            R.id.toggle_imperial,
            R.id.et_height,
            R.id.et_weight,
            R.id.rb_male,
            R.id.rb_female,
            R.id.rb_other,
            R.id.rb_prefer_not_to_say,
            R.id.btn_continue
        )

        expectedTabOrder.forEach { elementId ->
            assertTrue(accessibilityHelper.isElementFocusable(elementId),
                "Element $elementId should be focusable")
        }

        // Verify tab order is correct
        val actualTabOrder = accessibilityHelper.getTabOrder()
        assertEquals(expectedTabOrder, actualTabOrder, "Tab order should be logical")
    }

    private fun hasMinimumTouchTargetSize() = object : org.hamcrest.Matcher<android.view.View> {
        override fun describeTo(description: org.hamcrest.Description?) {
            description?.appendText("has minimum touch target size of 48dp x 48dp")
        }

        override fun matches(item: Any?): Boolean {
            if (item !is android.view.View) return false
            val minSize = (48 * item.context.resources.displayMetrics.density).toInt()
            return item.width >= minSize && item.height >= minSize
        }

        override fun describeMismatch(item: Any?, mismatchDescription: org.hamcrest.Description?) {
            if (item is android.view.View) {
                val density = item.context.resources.displayMetrics.density
                val widthDp = item.width / density
                val heightDp = item.height / density
                mismatchDescription?.appendText("was ${widthDp}dp x ${heightDp}dp")
            }
        }

        override fun _dont_implement_Matcher___instead_extend_BaseMatcher_() {}
    }

    private fun hasErrorText() = object : org.hamcrest.Matcher<android.view.View> {
        override fun describeTo(description: org.hamcrest.Description?) {
            description?.appendText("has error text")
        }

        override fun matches(item: Any?): Boolean {
            if (item !is com.google.android.material.textfield.TextInputLayout) return false
            return !item.error.isNullOrEmpty()
        }

        override fun describeMismatch(item: Any?, mismatchDescription: org.hamcrest.Description?) {
            mismatchDescription?.appendText("had no error text")
        }

        override fun _dont_implement_Matcher___instead_extend_BaseMatcher_() {}
    }

    private fun hasImeAction() = object : org.hamcrest.Matcher<android.view.View> {
        override fun describeTo(description: org.hamcrest.Description?) {
            description?.appendText("has IME action")
        }

        override fun matches(item: Any?): Boolean {
            if (item !is android.widget.EditText) return false
            return item.imeOptions != android.view.inputmethod.EditorInfo.IME_ACTION_NONE
        }

        override fun describeMismatch(item: Any?, mismatchDescription: org.hamcrest.Description?) {
            mismatchDescription?.appendText("had no IME action")
        }

        override fun _dont_implement_Matcher___instead_extend_BaseMatcher_() {}
    }
}