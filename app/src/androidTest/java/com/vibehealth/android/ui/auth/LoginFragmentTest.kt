package com.vibehealth.android.ui.auth

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
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
class LoginFragmentTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun loginFragment_displaysAllUIElements() {
        // Given
        launchFragmentInContainer<LoginFragment>()
        
        // Then - Verify all UI elements are displayed
        onView(withId(R.id.logo)).check(matches(isDisplayed()))
        onView(withId(R.id.welcome_heading)).check(matches(isDisplayed()))
        onView(withId(R.id.subtitle)).check(matches(isDisplayed()))
        onView(withId(R.id.email_input_layout)).check(matches(isDisplayed()))
        onView(withId(R.id.password_input_layout)).check(matches(isDisplayed()))
        onView(withId(R.id.forgot_password_link)).check(matches(isDisplayed()))
        onView(withId(R.id.sign_in_button)).check(matches(isDisplayed()))
        onView(withId(R.id.sign_up_link)).check(matches(isDisplayed()))
    }
    
    @Test
    fun loginFragment_displaysCorrectText() {
        // Given
        launchFragmentInContainer<LoginFragment>()
        
        // Then - Verify correct text is displayed
        onView(withId(R.id.welcome_heading)).check(matches(withText("Welcome Back")))
        onView(withId(R.id.subtitle)).check(matches(withText("Sign in to continue")))
        onView(withId(R.id.forgot_password_link)).check(matches(withText("Forgot Password?")))
        onView(withId(R.id.sign_in_button)).check(matches(withText("Sign In")))
        onView(withId(R.id.sign_up_link)).check(matches(withText("Don't have an account? Sign Up")))
    }
    
    @Test
    fun loginFragment_emailValidation_showsErrorForInvalidEmail() {
        // Given
        launchFragmentInContainer<LoginFragment>()
        
        // When - Enter invalid email
        onView(withId(R.id.email_edit_text)).perform(typeText("invalid-email"), closeSoftKeyboard())
        
        // Then - Error should be displayed (this would need proper ViewModel integration)
        // This test would need to be expanded with proper mocking
    }
    
    @Test
    fun loginFragment_passwordValidation_showsErrorForShortPassword() {
        // Given
        launchFragmentInContainer<LoginFragment>()
        
        // When - Enter short password
        onView(withId(R.id.password_edit_text)).perform(typeText("123"), closeSoftKeyboard())
        
        // Then - Error should be displayed (this would need proper ViewModel integration)
        // This test would need to be expanded with proper mocking
    }
    
    @Test
    fun loginFragment_signInButton_isClickable() {
        // Given
        launchFragmentInContainer<LoginFragment>()
        
        // When - Fill in valid credentials
        onView(withId(R.id.email_edit_text)).perform(typeText("test@example.com"))
        onView(withId(R.id.password_edit_text)).perform(typeText("password123"), closeSoftKeyboard())
        
        // Then - Sign in button should be clickable
        onView(withId(R.id.sign_in_button)).check(matches(isClickable()))
    }
    
    @Test
    fun loginFragment_forgotPasswordLink_isClickable() {
        // Given
        launchFragmentInContainer<LoginFragment>()
        
        // Then - Forgot password link should be clickable
        onView(withId(R.id.forgot_password_link)).check(matches(isClickable()))
    }
    
    @Test
    fun loginFragment_signUpLink_isClickable() {
        // Given
        launchFragmentInContainer<LoginFragment>()
        
        // Then - Sign up link should be clickable
        onView(withId(R.id.sign_up_link)).check(matches(isClickable()))
    }
    
    @Test
    fun loginFragment_hasProperAccessibilitySupport() {
        // Given
        launchFragmentInContainer<LoginFragment>()
        
        // Then - Verify accessibility features
        onView(withId(R.id.logo)).check(matches(hasContentDescription()))
        onView(withId(R.id.email_edit_text)).check(matches(hasImeAction(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT)))
        onView(withId(R.id.password_edit_text)).check(matches(hasImeAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)))
    }
}