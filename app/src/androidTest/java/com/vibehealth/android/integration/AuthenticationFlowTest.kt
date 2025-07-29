package com.vibehealth.android.integration

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibehealth.android.R
import com.vibehealth.android.ui.splash.SplashActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AuthenticationFlowTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun completeAuthenticationFlow_newUser() {
        // Given - App starts from splash
        ActivityScenario.launch(SplashActivity::class.java)
        
        // Then - Should navigate to login (assuming no user is logged in)
        Thread.sleep(2000) // Wait for splash duration
        onView(withId(R.id.welcome_heading)).check(matches(isDisplayed()))
        
        // When - Navigate to register
        onView(withId(R.id.sign_up_link)).perform(click())
        
        // Then - Should be on register screen
        onView(withId(R.id.create_account_heading)).check(matches(isDisplayed()))
        
        // When - Fill registration form
        onView(withId(R.id.email_edit_text)).perform(typeText("test@example.com"))
        onView(withId(R.id.password_edit_text)).perform(typeText("password123"))
        onView(withId(R.id.confirm_password_edit_text)).perform(typeText("password123"))
        onView(withId(R.id.terms_checkbox)).perform(click())
        
        // When - Submit registration (this would need Firebase emulator for full test)
        onView(withId(R.id.create_account_button)).perform(click())
        
        // Then - Should show loading state
        onView(withId(R.id.progress_bar)).check(matches(isDisplayed()))
    }
    
    @Test
    fun completeAuthenticationFlow_existingUser() {
        // Given - App starts from splash
        ActivityScenario.launch(SplashActivity::class.java)
        
        // Then - Should navigate to login
        Thread.sleep(2000) // Wait for splash duration
        onView(withId(R.id.welcome_heading)).check(matches(isDisplayed()))
        
        // When - Fill login form
        onView(withId(R.id.email_edit_text)).perform(typeText("existing@example.com"))
        onView(withId(R.id.password_edit_text)).perform(typeText("password123"), closeSoftKeyboard())
        
        // When - Submit login (this would need Firebase emulator for full test)
        onView(withId(R.id.sign_in_button)).perform(click())
        
        // Then - Should show loading state
        onView(withId(R.id.progress_bar)).check(matches(isDisplayed()))
    }
    
    @Test
    fun authenticationFlow_formValidation() {
        // Given - App starts from splash
        ActivityScenario.launch(SplashActivity::class.java)
        
        // Navigate to login
        Thread.sleep(2000)
        onView(withId(R.id.welcome_heading)).check(matches(isDisplayed()))
        
        // When - Try to login with invalid email
        onView(withId(R.id.email_edit_text)).perform(typeText("invalid-email"))
        onView(withId(R.id.password_edit_text)).perform(typeText("short"), closeSoftKeyboard())
        onView(withId(R.id.sign_in_button)).perform(click())
        
        // Then - Should show validation errors
        // Note: This would need proper ViewModel integration to work fully
    }
    
    @Test
    fun authenticationFlow_forgotPassword() {
        // Given - App starts from splash
        ActivityScenario.launch(SplashActivity::class.java)
        
        // Navigate to login
        Thread.sleep(2000)
        onView(withId(R.id.welcome_heading)).check(matches(isDisplayed()))
        
        // When - Enter email and click forgot password
        onView(withId(R.id.email_edit_text)).perform(typeText("test@example.com"))
        onView(withId(R.id.forgot_password_link)).perform(click())
        
        // Then - Should show appropriate feedback
        // This would need Firebase integration to test fully
    }
    
    @Test
    fun authenticationFlow_navigationBetweenScreens() {
        // Given - App starts from splash
        ActivityScenario.launch(SplashActivity::class.java)
        
        // Navigate to login
        Thread.sleep(2000)
        onView(withId(R.id.welcome_heading)).check(matches(isDisplayed()))
        
        // When - Navigate to register
        onView(withId(R.id.sign_up_link)).perform(click())
        onView(withId(R.id.create_account_heading)).check(matches(isDisplayed()))
        
        // When - Navigate back to login
        onView(withId(R.id.sign_in_link)).perform(click())
        onView(withId(R.id.welcome_heading)).check(matches(isDisplayed()))
        
        // Then - Navigation should work smoothly with animations
    }
    
    @Test
    fun authenticationFlow_deepLinking() {
        // Given - Deep link to register
        val intent = Intent(ApplicationProvider.getApplicationContext(), 
            com.vibehealth.android.ui.auth.AuthActivity::class.java).apply {
            data = android.net.Uri.parse("vibehealth://auth/register")
        }
        
        // When - Launch with deep link
        ActivityScenario.launch<com.vibehealth.android.ui.auth.AuthActivity>(intent)
        
        // Then - Should navigate directly to register
        onView(withId(R.id.create_account_heading)).check(matches(isDisplayed()))
    }
    
    @Test
    fun authenticationFlow_sessionPersistence() {
        // This test would need to simulate app restart and check session persistence
        // Would require proper Firebase emulator setup and session mocking
    }
    
    @Test
    fun authenticationFlow_errorHandling() {
        // Given - App starts from splash
        ActivityScenario.launch(SplashActivity::class.java)
        
        // Navigate to login
        Thread.sleep(2000)
        onView(withId(R.id.welcome_heading)).check(matches(isDisplayed()))
        
        // When - Try to login with network error (would need network simulation)
        onView(withId(R.id.email_edit_text)).perform(typeText("test@example.com"))
        onView(withId(R.id.password_edit_text)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.sign_in_button)).perform(click())
        
        // Then - Should handle network errors gracefully
        // This would need proper error simulation
    }
    
    @Test
    fun authenticationFlow_loadingStates() {
        // Given - App starts from splash
        ActivityScenario.launch(SplashActivity::class.java)
        
        // Navigate to login
        Thread.sleep(2000)
        onView(withId(R.id.welcome_heading)).check(matches(isDisplayed()))
        
        // When - Submit login form
        onView(withId(R.id.email_edit_text)).perform(typeText("test@example.com"))
        onView(withId(R.id.password_edit_text)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.sign_in_button)).perform(click())
        
        // Then - Should show loading indicators
        onView(withId(R.id.progress_bar)).check(matches(isDisplayed()))
        onView(withId(R.id.sign_in_button)).check(matches(isDisplayed()))
    }
}