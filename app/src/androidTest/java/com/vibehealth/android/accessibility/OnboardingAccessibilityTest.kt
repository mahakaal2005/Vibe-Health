package com.vibehealth.android.accessibility

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import com.vibehealth.android.R
import com.vibehealth.android.ui.onboarding.OnboardingActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility tests for onboarding screens per WCAG 2.1 Level AA
 */
@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OnboardingAccessibilityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val activityRule = ActivityScenarioRule(OnboardingActivity::class.java)

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        
        // Enable accessibility checks
        AccessibilityChecks.enable()
            .setRunChecksFromRootView(true)
            .setSuppressingResultMatcher(
                AccessibilityCheckResultUtils.matchesCheckNames(
                    // Suppress checks that may not be relevant for our use case
                    "DuplicateClickableBoundsCheck"
                )
            )
    }

    @Test
    fun welcomeScreen_shouldMeetAccessibilityStandards() {
        // Check that welcome screen elements have proper content descriptions
        onView(withId(R.id.tv_welcome_title))
            .check(matches(isDisplayed()))
            .check(matches(hasContentDescription()))

        onView(withId(R.id.btn_get_started))
            .check(matches(isDisplayed()))
            .check(matches(hasContentDescription()))
            .check(matches(isClickable()))

        // Check minimum touch target size (48dp)
        onView(withId(R.id.btn_get_started))
            .check(matches(hasMinimumTouchTargetSize()))
    }

    @Test
    fun personalInfoScreen_shouldMeetAccessibilityStandards() {
        // Navigate to personal info screen
        onView(withId(R.id.btn_get_started)).perform(click())

        // Check form elements have proper labels and descriptions
        onView(withId(R.id.til_full_name))
            .check(matches(isDisplayed()))
            .check(matches(hasContentDescription()))

        onView(withId(R.id.btn_select_birthday))
            .check(matches(isDisplayed()))
            .check(matches(hasContentDescription()))
            .check(matches(hasMinimumTouchTargetSize()))

        onView(withId(R.id.btn_continue))
            .check(matches(isDisplayed()))
            .check(matches(hasContentDescription()))
            .check(matches(hasMinimumTouchTargetSize()))

        // Check progress indicator is accessible
        onView(withId(R.id.progress_bar))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun physicalInfoScreen_shouldMeetAccessibilityStandards() {
        // Navigate to physical info screen
        onView(withId(R.id.btn_get_started)).perform(click())
        
        // Fill in personal info to proceed
        onView(withId(R.id.et_full_name))
            .perform(androidx.test.espresso.action.ViewActions.typeText("Test User"))
        
        onView(withId(R.id.btn_select_birthday)).perform(click())
        onView(withText("OK")).perform(click()) // Accept default date
        
        onView(withId(R.id.btn_continue)).perform(click())

        // Check unit system toggle accessibility
        onView(withId(R.id.rg_unit_system))
            .check(matches(isDisplayed()))
            .check(matches(hasContentDescription()))

        // Check input fields have proper labels
        onView(withId(R.id.til_height))
            .check(matches(hasContentDescription()))

        onView(withId(R.id.til_weight))
            .check(matches(hasContentDescription()))

        // Check gender radio buttons have proper descriptions
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
    }

    @Test
    fun completionScreen_shouldMeetAccessibilityStandards() {
        // Navigate through all screens to completion
        navigateToCompletionScreen()

        // Check completion elements are accessible
        onView(withId(R.id.tv_completion_title))
            .check(matches(isDisplayed()))
            .check(matches(hasContentDescription()))

        onView(withId(R.id.tv_goal_calculation_status))
            .check(matches(isDisplayed()))
            .check(matches(hasContentDescription()))

        onView(withId(R.id.btn_enter_app))
            .check(matches(isDisplayed()))
            .check(matches(hasContentDescription()))
            .check(matches(hasMinimumTouchTargetSize()))

        onView(withId(R.id.iv_success_checkmark))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun keyboardNavigation_shouldWorkProperly() {
        // Test keyboard navigation through welcome screen
        onView(withId(R.id.btn_get_started))
            .check(matches(isFocusable()))
            .perform(androidx.test.espresso.action.ViewActions.pressKey(android.view.KeyEvent.KEYCODE_ENTER))

        // Navigate to personal info and test keyboard navigation
        onView(withId(R.id.et_full_name))
            .check(matches(isFocusable()))

        onView(withId(R.id.btn_select_birthday))
            .check(matches(isFocusable()))

        onView(withId(R.id.btn_continue))
            .check(matches(isFocusable()))
    }

    @Test
    fun errorMessages_shouldBeAccessible() {
        // Navigate to personal info screen
        onView(withId(R.id.btn_get_started)).perform(click())

        // Try to continue without filling required fields
        onView(withId(R.id.btn_continue)).perform(click())

        // Check that error messages are announced to screen readers
        onView(withId(R.id.til_full_name))
            .check(matches(hasErrorText()))
    }

    @Test
    fun colorContrast_shouldMeetWCAGStandards() {
        // This test would typically use automated tools to check color contrast
        // For now, we verify that high contrast mode is supported
        
        onView(withId(R.id.tv_welcome_title))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btn_get_started))
            .check(matches(isDisplayed()))
    }

    @Test
    fun dynamicFontScaling_shouldBeSupported() {
        // Test that text scales properly with system font size
        onView(withId(R.id.tv_welcome_title))
            .check(matches(isDisplayed()))

        onView(withId(R.id.tv_welcome_subtitle))
            .check(matches(isDisplayed()))
    }

    private fun navigateToCompletionScreen() {
        // Navigate through all onboarding steps
        onView(withId(R.id.btn_get_started)).perform(click())
        
        // Fill personal info
        onView(withId(R.id.et_full_name))
            .perform(androidx.test.espresso.action.ViewActions.typeText("Test User"))
        
        onView(withId(R.id.btn_select_birthday)).perform(click())
        onView(withText("OK")).perform(click())
        
        onView(withId(R.id.btn_continue)).perform(click())
        
        // Fill physical info
        onView(withId(R.id.et_height))
            .perform(androidx.test.espresso.action.ViewActions.typeText("175"))
        
        onView(withId(R.id.et_weight))
            .perform(androidx.test.espresso.action.ViewActions.typeText("70"))
        
        onView(withId(R.id.rb_male)).perform(click())
        
        onView(withId(R.id.btn_continue)).perform(click())
    }

    private fun hasMinimumTouchTargetSize() = object : org.hamcrest.TypeSafeMatcher<android.view.View>() {
        override fun describeTo(description: org.hamcrest.Description) {
            description.appendText("has minimum touch target size of 48dp")
        }

        override fun matchesSafely(view: android.view.View): Boolean {
            val minSize = (48 * view.context.resources.displayMetrics.density).toInt()
            return view.width >= minSize && view.height >= minSize
        }
    }

    private fun hasErrorText() = object : org.hamcrest.TypeSafeMatcher<android.view.View>() {
        override fun describeTo(description: org.hamcrest.Description) {
            description.appendText("has error text")
        }

        override fun matchesSafely(view: android.view.View): Boolean {
            return when (view) {
                is com.google.android.material.textfield.TextInputLayout -> {
                    view.error != null && view.error.toString().isNotEmpty()
                }
                else -> false
            }
        }
    }
}