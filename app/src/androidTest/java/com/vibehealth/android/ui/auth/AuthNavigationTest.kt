package com.vibehealth.android.ui.auth

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibehealth.android.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AuthNavigationTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun authActivity_startsWithLoginFragment() {
        // Given
        ActivityScenario.launch(AuthActivity::class.java)
        
        // Then - Should start with login fragment
        onView(withId(R.id.welcome_heading)).check(matches(isDisplayed()))
        onView(withId(R.id.welcome_heading)).check(matches(withText("Welcome Back")))
    }
    
    @Test
    fun authActivity_navigateFromLoginToRegister() {
        // Given
        ActivityScenario.launch(AuthActivity::class.java)
        
        // When - Click sign up link
        onView(withId(R.id.sign_up_link)).perform(click())
        
        // Then - Should navigate to register fragment
        onView(withId(R.id.create_account_heading)).check(matches(isDisplayed()))
        onView(withId(R.id.create_account_heading)).check(matches(withText("Create Account")))
    }
    
    @Test
    fun authActivity_navigateFromRegisterToLogin() {
        // Given
        ActivityScenario.launch(AuthActivity::class.java)
        
        // When - Navigate to register then back to login
        onView(withId(R.id.sign_up_link)).perform(click())
        onView(withId(R.id.sign_in_link)).perform(click())
        
        // Then - Should be back on login fragment
        onView(withId(R.id.welcome_heading)).check(matches(isDisplayed()))
        onView(withId(R.id.welcome_heading)).check(matches(withText("Welcome Back")))
    }
    
    @Test
    fun authActivity_handleDeepLinkToLogin() {
        // Given - Deep link intent to login
        val intent = Intent(ApplicationProvider.getApplicationContext(), AuthActivity::class.java).apply {
            data = android.net.Uri.parse("vibehealth://auth/login")
        }
        
        // When
        ActivityScenario.launch<AuthActivity>(intent)
        
        // Then - Should show login fragment
        onView(withId(R.id.welcome_heading)).check(matches(isDisplayed()))
        onView(withId(R.id.welcome_heading)).check(matches(withText("Welcome Back")))
    }
    
    @Test
    fun authActivity_handleDeepLinkToRegister() {
        // Given - Deep link intent to register
        val intent = Intent(ApplicationProvider.getApplicationContext(), AuthActivity::class.java).apply {
            data = android.net.Uri.parse("vibehealth://auth/register")
        }
        
        // When
        ActivityScenario.launch<AuthActivity>(intent)
        
        // Then - Should show register fragment
        onView(withId(R.id.create_account_heading)).check(matches(isDisplayed()))
        onView(withId(R.id.create_account_heading)).check(matches(withText("Create Account")))
    }
    
    @Test
    fun authActivity_backButtonBehavior() {
        // Given
        val scenario = ActivityScenario.launch(AuthActivity::class.java)
        
        // When - Navigate to register
        onView(withId(R.id.sign_up_link)).perform(click())
        
        // Then - Should be on register
        onView(withId(R.id.create_account_heading)).check(matches(isDisplayed()))
        
        // When - Press back button
        scenario.onActivity { activity ->
            activity.onBackPressed()
        }
        
        // Then - Should be back on login
        onView(withId(R.id.welcome_heading)).check(matches(isDisplayed()))
    }
    
    @Test
    fun authActivity_navigationAnimations() {
        // Given
        ActivityScenario.launch(AuthActivity::class.java)
        
        // When - Navigate between fragments
        onView(withId(R.id.sign_up_link)).perform(click())
        
        // Then - Should have smooth transition (animation testing would need more setup)
        onView(withId(R.id.create_account_heading)).check(matches(isDisplayed()))
        
        // When - Navigate back
        onView(withId(R.id.sign_in_link)).perform(click())
        
        // Then - Should have smooth transition back
        onView(withId(R.id.welcome_heading)).check(matches(isDisplayed()))
    }
}