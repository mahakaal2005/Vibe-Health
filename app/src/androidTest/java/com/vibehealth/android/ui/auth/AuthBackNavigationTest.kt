package com.vibehealth.android.ui.auth

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vibehealth.android.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test back navigation behavior in authentication flow
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class AuthBackNavigationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var activityScenarioRule = ActivityScenarioRule(AuthActivity::class.java)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testBackNavigationFromLoginExitsApp() {
        // Verify we're on login fragment
        onView(withId(R.id.sign_in_button))
            .check(matches(isDisplayed()))
        
        // Press back button - should exit app (activity should finish)
        pressBack()
        
        // Activity should be finished
        activityScenarioRule.scenario.onActivity { activity ->
            assert(activity.isFinishing || activity.isDestroyed)
        }
    }

    @Test
    fun testBackNavigationFromRegisterGoesToLogin() {
        // Navigate to register fragment
        onView(withId(R.id.sign_up_link))
            .perform(click())
        
        // Verify we're on register fragment
        onView(withId(R.id.create_account_button))
            .check(matches(isDisplayed()))
        
        // Press back button - should go to login
        pressBack()
        
        // Verify we're back on login fragment
        onView(withId(R.id.sign_in_button))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testBackNavigationFromLoginAfterRegisterExitsApp() {
        // Navigate to register fragment
        onView(withId(R.id.sign_up_link))
            .perform(click())
        
        // Verify we're on register fragment
        onView(withId(R.id.create_account_button))
            .check(matches(isDisplayed()))
        
        // Press back button - should go to login
        pressBack()
        
        // Verify we're back on login fragment
        onView(withId(R.id.sign_in_button))
            .check(matches(isDisplayed()))
        
        // Press back button again - should exit app
        pressBack()
        
        // Activity should be finished
        activityScenarioRule.scenario.onActivity { activity ->
            assert(activity.isFinishing || activity.isDestroyed)
        }
    }
}