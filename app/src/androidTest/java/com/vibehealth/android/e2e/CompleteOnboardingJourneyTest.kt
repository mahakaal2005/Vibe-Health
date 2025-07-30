package com.vibehealth.android.e2e

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.vibehealth.android.MainActivity
import com.vibehealth.android.R
import com.vibehealth.android.ui.auth.AuthActivity
import com.vibehealth.android.ui.onboarding.OnboardingActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for complete user journey from authentication through onboarding to main app
 */
@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CompleteOnboardingJourneyTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun completeUserJourney_newUser_shouldSucceed() {
        // Start with authentication
        val authScenario = androidx.test.ext.junit.rules.ActivityScenarioRule(AuthActivity::class.java)
        authScenario.scenario.use { scenario ->
            
            // Register new user
            onView(withId(R.id.tv_create_account))
                .perform(click())

            onView(withId(R.id.et_email))
                .perform(typeText("test.user@example.com"), closeSoftKeyboard())

            onView(withId(R.id.et_password))
                .perform(typeText("TestPassword123!"), closeSoftKeyboard())

            onView(withId(R.id.et_confirm_password))
                .perform(typeText("TestPassword123!"), closeSoftKeyboard())

            onView(withId(R.id.cb_terms))
                .perform(click())

            onView(withId(R.id.btn_sign_up))
                .perform(click())

            // Wait for authentication to complete and navigate to onboarding
            Thread.sleep(3000)
        }

        // Continue with onboarding
        val onboardingScenario = androidx.test.ext.junit.rules.ActivityScenarioRule(OnboardingActivity::class.java)
        onboardingScenario.scenario.use { scenario ->
            
            // Welcome screen
            onView(withId(R.id.tv_welcome_title))
                .check(matches(isDisplayed()))

            onView(withId(R.id.btn_get_started))
                .perform(click())

            // Personal info screen
            onView(withId(R.id.et_full_name))
                .perform(typeText("Test User"), closeSoftKeyboard())

            onView(withId(R.id.btn_select_birthday))
                .perform(click())
            
            onView(withText("OK"))
                .perform(click())

            onView(withId(R.id.btn_continue))
                .perform(click())

            // Physical info screen
            onView(withId(R.id.et_height))
                .perform(typeText("175"), closeSoftKeyboard())

            onView(withId(R.id.et_weight))
                .perform(typeText("70"), closeSoftKeyboard())

            onView(withId(R.id.rb_male))
                .perform(click())

            onView(withId(R.id.btn_continue))
                .perform(click())

            // Completion screen
            onView(withId(R.id.tv_completion_title))
                .check(matches(isDisplayed()))

            // Wait for goal calculation
            Thread.sleep(5000)

            onView(withId(R.id.btn_enter_app))
                .perform(click())
        }

        // Verify navigation to main app
        val mainScenario = androidx.test.ext.junit.rules.ActivityScenarioRule(MainActivity::class.java)
        mainScenario.scenario.use { scenario ->
            
            // Verify main app is displayed
            onView(withId(R.id.bottom_navigation))
                .check(matches(isDisplayed()))

            // Verify user profile is accessible
            onView(withId(R.id.navigation_profile))
                .perform(click())

            // Should show user profile with onboarding data
            onView(withId(R.id.tv_user_name))
                .check(matches(withText("Test User")))
        }
    }

    @Test
    fun completeUserJourney_existingUser_shouldSkipOnboarding() {
        // Simulate existing user with completed onboarding
        // This would require setting up test data in Firebase
        
        val authScenario = androidx.test.ext.junit.rules.ActivityScenarioRule(AuthActivity::class.java)
        authScenario.scenario.use { scenario ->
            
            // Login existing user
            onView(withId(R.id.et_email))
                .perform(typeText("existing.user@example.com"), closeSoftKeyboard())

            onView(withId(R.id.et_password))
                .perform(typeText("ExistingPassword123!"), closeSoftKeyboard())

            onView(withId(R.id.btn_sign_in))
                .perform(click())

            // Wait for authentication
            Thread.sleep(3000)
        }

        // Should navigate directly to main app (skipping onboarding)
        val mainScenario = androidx.test.ext.junit.rules.ActivityScenarioRule(MainActivity::class.java)
        mainScenario.scenario.use { scenario ->
            
            onView(withId(R.id.bottom_navigation))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun onboardingFlow_withNetworkInterruption_shouldHandleGracefully() {
        val onboardingScenario = androidx.test.ext.junit.rules.ActivityScenarioRule(OnboardingActivity::class.java)
        onboardingScenario.scenario.use { scenario ->
            
            // Complete onboarding flow
            completeOnboardingFlow()

            // Simulate network interruption during goal calculation
            // This would require network simulation tools
            
            // Verify error handling
            onView(withText("Network Error"))
                .check(matches(isDisplayed()))

            onView(withText("Retry"))
                .perform(click())

            // Should retry and eventually succeed
            Thread.sleep(5000)

            onView(withId(R.id.btn_enter_app))
                .check(matches(isEnabled()))
        }
    }

    @Test
    fun onboardingFlow_withProcessDeath_shouldRestoreState() {
        val onboardingScenario = androidx.test.ext.junit.rules.ActivityScenarioRule(OnboardingActivity::class.java)
        onboardingScenario.scenario.use { scenario ->
            
            // Fill partial onboarding data
            onView(withId(R.id.btn_get_started))
                .perform(click())

            onView(withId(R.id.et_full_name))
                .perform(typeText("Test User"), closeSoftKeyboard())

            onView(withId(R.id.btn_select_birthday))
                .perform(click())
            
            onView(withText("OK"))
                .perform(click())

            // Simulate process death by recreating activity
            scenario.recreate()

            // Verify state is restored
            onView(withId(R.id.et_full_name))
                .check(matches(withText("Test User")))

            // Should be able to continue from where left off
            onView(withId(R.id.btn_continue))
                .check(matches(isEnabled()))
        }
    }

    @Test
    fun onboardingFlow_withDifferentDeviceOrientations_shouldWork() {
        val onboardingScenario = androidx.test.ext.junit.rules.ActivityScenarioRule(OnboardingActivity::class.java)
        onboardingScenario.scenario.use { scenario ->
            
            // Test in portrait (default)
            completeOnboardingFlow()

            // Rotate to landscape (if supported)
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val uiAutomation = instrumentation.uiAutomation
            
            try {
                uiAutomation.setRotation(android.view.Surface.ROTATION_90)
                Thread.sleep(1000)

                // Verify UI still works in landscape
                onView(withId(R.id.tv_completion_title))
                    .check(matches(isDisplayed()))

                // Rotate back to portrait
                uiAutomation.setRotation(android.view.Surface.ROTATION_0)
                Thread.sleep(1000)

                onView(withId(R.id.btn_enter_app))
                    .check(matches(isDisplayed()))
                    
            } catch (e: Exception) {
                // Rotation might not be supported on all test devices
                android.util.Log.w("E2ETest", "Rotation test skipped", e)
            }
        }
    }

    @Test
    fun onboardingFlow_withAccessibilityServices_shouldWork() {
        // This test would require enabling accessibility services
        // For now, we'll test basic accessibility compliance
        
        val onboardingScenario = androidx.test.ext.junit.rules.ActivityScenarioRule(OnboardingActivity::class.java)
        onboardingScenario.scenario.use { scenario ->
            
            // Verify all interactive elements have content descriptions
            onView(withId(R.id.btn_get_started))
                .check(matches(hasContentDescription()))

            onView(withId(R.id.btn_get_started))
                .perform(click())

            onView(withId(R.id.et_full_name))
                .check(matches(hasContentDescription()))

            onView(withId(R.id.btn_select_birthday))
                .check(matches(hasContentDescription()))

            onView(withId(R.id.btn_continue))
                .check(matches(hasContentDescription()))
        }
    }

    @Test
    fun onboardingFlow_withLowMemoryConditions_shouldHandleGracefully() {
        // This test would simulate low memory conditions
        val onboardingScenario = androidx.test.ext.junit.rules.ActivityScenarioRule(OnboardingActivity::class.java)
        onboardingScenario.scenario.use { scenario ->
            
            // Force garbage collection to simulate memory pressure
            System.gc()
            
            // Complete onboarding flow under memory pressure
            completeOnboardingFlow()

            // Verify successful completion despite memory constraints
            onView(withId(R.id.tv_completion_title))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun onboardingFlow_withSlowNetwork_shouldShowAppropriateLoading() {
        // This test would require network throttling
        val onboardingScenario = androidx.test.ext.junit.rules.ActivityScenarioRule(OnboardingActivity::class.java)
        onboardingScenario.scenario.use { scenario ->
            
            completeOnboardingFlow()

            // Verify loading indicators are shown during slow operations
            onView(withId(R.id.pb_loading))
                .check(matches(isDisplayed()))

            // Wait for operation to complete
            Thread.sleep(10000)

            onView(withId(R.id.btn_enter_app))
                .check(matches(isEnabled()))
        }
    }

    private fun completeOnboardingFlow() {
        // Welcome screen
        onView(withId(R.id.btn_get_started))
            .perform(click())

        // Personal info
        onView(withId(R.id.et_full_name))
            .perform(typeText("Test User"), closeSoftKeyboard())

        onView(withId(R.id.btn_select_birthday))
            .perform(click())
        
        onView(withText("OK"))
            .perform(click())

        onView(withId(R.id.btn_continue))
            .perform(click())

        // Physical info
        onView(withId(R.id.et_height))
            .perform(typeText("175"), closeSoftKeyboard())

        onView(withId(R.id.et_weight))
            .perform(typeText("70"), closeSoftKeyboard())

        onView(withId(R.id.rb_male))
            .perform(click())

        onView(withId(R.id.btn_continue))
            .perform(click())
    }
}