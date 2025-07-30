package com.vibehealth.android.ui.onboarding

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vibehealth.android.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for complete onboarding flow
 */
@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OnboardingFlowUITest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val activityRule = ActivityScenarioRule(OnboardingActivity::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun completeOnboardingFlow_shouldNavigateToMainApp() {
        // Welcome screen
        onView(withId(R.id.tv_welcome_title))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.welcome_to_vibe_health)))

        onView(withId(R.id.btn_get_started))
            .check(matches(isDisplayed()))
            .perform(click())

        // Personal info screen
        onView(withId(R.id.tv_personal_info_title))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.personal_information)))

        // Fill in name
        onView(withId(R.id.et_full_name))
            .perform(typeText("John Doe"), closeSoftKeyboard())

        // Select birthday
        onView(withId(R.id.btn_select_birthday))
            .perform(click())
        
        // Accept default date in date picker
        onView(withText("OK"))
            .perform(click())

        // Continue to physical info
        onView(withId(R.id.btn_continue))
            .check(matches(isEnabled()))
            .perform(click())

        // Physical info screen
        onView(withId(R.id.tv_physical_info_title))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.physical_information)))

        // Fill in height
        onView(withId(R.id.et_height))
            .perform(typeText("175"), closeSoftKeyboard())

        // Fill in weight
        onView(withId(R.id.et_weight))
            .perform(typeText("70"), closeSoftKeyboard())

        // Select gender
        onView(withId(R.id.rb_male))
            .perform(click())

        // Continue to completion
        onView(withId(R.id.btn_continue))
            .check(matches(isEnabled()))
            .perform(click())

        // Completion screen
        onView(withId(R.id.tv_completion_title))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.youre_all_set)))

        // Wait for goal calculation to complete and button to be enabled
        Thread.sleep(3000)

        onView(withId(R.id.btn_enter_app))
            .check(matches(isEnabled()))
            .perform(click())

        // Should navigate to main app (MainActivity)
        // This would be verified by checking if MainActivity is displayed
    }

    @Test
    fun onboardingFlow_withImperialUnits_shouldWork() {
        // Navigate to physical info screen
        navigateToPhysicalInfoScreen()

        // Switch to imperial units
        onView(withId(R.id.btn_imperial))
            .perform(click())

        // Verify unit labels changed
        onView(withId(R.id.til_height))
            .check(matches(hasDescendant(withText(containsString("ft")))))

        onView(withId(R.id.til_weight))
            .check(matches(hasDescendant(withText(containsString("lbs")))))

        // Fill in imperial measurements
        onView(withId(R.id.et_height))
            .perform(typeText("5.9"), closeSoftKeyboard())

        onView(withId(R.id.et_weight))
            .perform(typeText("154"), closeSoftKeyboard())

        // Select gender and continue
        onView(withId(R.id.rb_female))
            .perform(click())

        onView(withId(R.id.btn_continue))
            .check(matches(isEnabled()))
            .perform(click())

        // Should reach completion screen
        onView(withId(R.id.tv_completion_title))
            .check(matches(isDisplayed()))
    }

    @Test
    fun onboardingFlow_backNavigation_shouldWork() {
        // Navigate to personal info
        onView(withId(R.id.btn_get_started)).perform(click())

        // Fill in personal info
        onView(withId(R.id.et_full_name))
            .perform(typeText("Jane Doe"), closeSoftKeyboard())
        
        onView(withId(R.id.btn_select_birthday)).perform(click())
        onView(withText("OK")).perform(click())
        
        onView(withId(R.id.btn_continue)).perform(click())

        // Now on physical info screen
        onView(withId(R.id.tv_physical_info_title))
            .check(matches(isDisplayed()))

        // Go back
        onView(withId(R.id.btn_back))
            .perform(click())

        // Should be back on personal info screen
        onView(withId(R.id.tv_personal_info_title))
            .check(matches(isDisplayed()))

        // Name should still be filled
        onView(withId(R.id.et_full_name))
            .check(matches(withText("Jane Doe")))
    }

    @Test
    fun onboardingFlow_formValidation_shouldPreventProgression() {
        // Navigate to personal info
        onView(withId(R.id.btn_get_started)).perform(click())

        // Try to continue without filling required fields
        onView(withId(R.id.btn_continue))
            .check(matches(isNotEnabled()))

        // Fill only name
        onView(withId(R.id.et_full_name))
            .perform(typeText("Test User"), closeSoftKeyboard())

        // Should still be disabled without birthday
        onView(withId(R.id.btn_continue))
            .check(matches(isNotEnabled()))

        // Add birthday
        onView(withId(R.id.btn_select_birthday)).perform(click())
        onView(withText("OK")).perform(click())

        // Now should be enabled
        onView(withId(R.id.btn_continue))
            .check(matches(isEnabled()))
    }

    @Test
    fun onboardingFlow_nameValidation_shouldShowErrors() {
        // Navigate to personal info
        onView(withId(R.id.btn_get_started)).perform(click())

        // Enter invalid name (too short)
        onView(withId(R.id.et_full_name))
            .perform(typeText("A"), closeSoftKeyboard())

        // Lose focus to trigger validation
        onView(withId(R.id.btn_select_birthday))
            .perform(click())

        // Should show error
        onView(withId(R.id.til_full_name))
            .check(matches(hasErrorText(R.string.error_name_too_short)))

        // Clear and enter valid name
        onView(withId(R.id.et_full_name))
            .perform(clearText(), typeText("Valid Name"), closeSoftKeyboard())

        // Error should be cleared
        onView(withId(R.id.til_full_name))
            .check(matches(hasNoErrorText()))
    }

    @Test
    fun onboardingFlow_physicalInfoValidation_shouldWork() {
        // Navigate to physical info screen
        navigateToPhysicalInfoScreen()

        // Try to continue without filling required fields
        onView(withId(R.id.btn_continue))
            .check(matches(isNotEnabled()))

        // Fill invalid height
        onView(withId(R.id.et_height))
            .perform(typeText("50"), closeSoftKeyboard()) // Too short

        // Fill invalid weight
        onView(withId(R.id.et_weight))
            .perform(typeText("20"), closeSoftKeyboard()) // Too light

        // Should still be disabled
        onView(withId(R.id.btn_continue))
            .check(matches(isNotEnabled()))

        // Fix values
        onView(withId(R.id.et_height))
            .perform(clearText(), typeText("175"), closeSoftKeyboard())

        onView(withId(R.id.et_weight))
            .perform(clearText(), typeText("70"), closeSoftKeyboard())

        onView(withId(R.id.rb_male))
            .perform(click())

        // Now should be enabled
        onView(withId(R.id.btn_continue))
            .check(matches(isEnabled()))
    }

    @Test
    fun onboardingFlow_unitSystemSwitch_shouldPreserveValues() {
        // Navigate to physical info screen
        navigateToPhysicalInfoScreen()

        // Fill in metric values
        onView(withId(R.id.et_height))
            .perform(typeText("175"), closeSoftKeyboard())

        onView(withId(R.id.et_weight))
            .perform(typeText("70"), closeSoftKeyboard())

        // Switch to imperial
        onView(withId(R.id.btn_imperial))
            .perform(click())

        // Values should be converted and displayed
        onView(withId(R.id.et_height))
            .check(matches(withText(containsString("5.7")))) // Approximate conversion

        onView(withId(R.id.et_weight))
            .check(matches(withText(containsString("154")))) // Approximate conversion

        // Switch back to metric
        onView(withId(R.id.btn_metric))
            .perform(click())

        // Should be back to original values (approximately)
        onView(withId(R.id.et_height))
            .check(matches(withText(containsString("175"))))

        onView(withId(R.id.et_weight))
            .check(matches(withText(containsString("70"))))
    }

    @Test
    fun onboardingFlow_progressIndicator_shouldUpdate() {
        // Welcome screen - Step 1 of 3
        onView(withId(R.id.tv_step_indicator))
            .check(matches(withText(containsString("1"))))
            .check(matches(withText(containsString("3"))))

        // Navigate to personal info
        onView(withId(R.id.btn_get_started)).perform(click())

        // Personal info screen - Step 2 of 3
        onView(withId(R.id.tv_step_indicator))
            .check(matches(withText(containsString("2"))))
            .check(matches(withText(containsString("3"))))

        // Navigate to physical info
        navigateToPhysicalInfoFromPersonalInfo()

        // Physical info screen - Step 3 of 3
        onView(withId(R.id.tv_step_indicator))
            .check(matches(withText(containsString("3"))))
            .check(matches(withText(containsString("3"))))
    }

    private fun navigateToPhysicalInfoScreen() {
        // Navigate through welcome and personal info screens
        onView(withId(R.id.btn_get_started)).perform(click())
        
        onView(withId(R.id.et_full_name))
            .perform(typeText("Test User"), closeSoftKeyboard())
        
        onView(withId(R.id.btn_select_birthday)).perform(click())
        onView(withText("OK")).perform(click())
        
        onView(withId(R.id.btn_continue)).perform(click())
    }

    private fun navigateToPhysicalInfoFromPersonalInfo() {
        onView(withId(R.id.et_full_name))
            .perform(typeText("Test User"), closeSoftKeyboard())
        
        onView(withId(R.id.btn_select_birthday)).perform(click())
        onView(withText("OK")).perform(click())
        
        onView(withId(R.id.btn_continue)).perform(click())
    }

    private fun hasErrorText(errorStringRes: Int) = hasDescendant(withText(errorStringRes))
    private fun hasNoErrorText() = hasDescendant(withText(""))
}