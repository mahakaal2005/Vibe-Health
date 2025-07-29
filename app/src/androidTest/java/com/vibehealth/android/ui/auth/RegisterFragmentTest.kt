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
class RegisterFragmentTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun registerFragment_displaysAllUIElements() {
        // Given
        launchFragmentInContainer<RegisterFragment>()
        
        // Then - Verify all UI elements are displayed
        onView(withId(R.id.logo)).check(matches(isDisplayed()))
        onView(withId(R.id.create_account_heading)).check(matches(isDisplayed()))
        onView(withId(R.id.subtitle)).check(matches(isDisplayed()))
        onView(withId(R.id.email_input_layout)).check(matches(isDisplayed()))
        onView(withId(R.id.password_input_layout)).check(matches(isDisplayed()))
        onView(withId(R.id.confirm_password_input_layout)).check(matches(isDisplayed()))
        onView(withId(R.id.terms_checkbox)).check(matches(isDisplayed()))
        onView(withId(R.id.create_account_button)).check(matches(isDisplayed()))
        onView(withId(R.id.sign_in_link)).check(matches(isDisplayed()))
    }
    
    @Test
    fun registerFragment_displaysCorrectText() {
        // Given
        launchFragmentInContainer<RegisterFragment>()
        
        // Then - Verify correct text is displayed
        onView(withId(R.id.create_account_heading)).check(matches(withText("Create Account")))
        onView(withId(R.id.subtitle)).check(matches(withText("Join your wellness journey")))
        onView(withId(R.id.terms_checkbox)).check(matches(withText("I agree to Terms & Conditions")))
        onView(withId(R.id.create_account_button)).check(matches(withText("Create Account")))
        onView(withId(R.id.sign_in_link)).check(matches(withText("Already have an account? Sign In")))
    }
    
    @Test
    fun registerFragment_termsCheckbox_isClickable() {
        // Given
        launchFragmentInContainer<RegisterFragment>()
        
        // Then - Terms checkbox should be clickable
        onView(withId(R.id.terms_checkbox)).check(matches(isClickable()))
    }
    
    @Test
    fun registerFragment_createAccountButton_isClickable() {
        // Given
        launchFragmentInContainer<RegisterFragment>()
        
        // When - Fill in valid credentials and accept terms
        onView(withId(R.id.email_edit_text)).perform(typeText("test@example.com"))
        onView(withId(R.id.password_edit_text)).perform(typeText("password123"))
        onView(withId(R.id.confirm_password_edit_text)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.terms_checkbox)).perform(click())
        
        // Then - Create account button should be clickable
        onView(withId(R.id.create_account_button)).check(matches(isClickable()))
    }
    
    @Test
    fun registerFragment_signInLink_isClickable() {
        // Given
        launchFragmentInContainer<RegisterFragment>()
        
        // Then - Sign in link should be clickable
        onView(withId(R.id.sign_in_link)).check(matches(isClickable()))
    }
    
    @Test
    fun registerFragment_hasProperAccessibilitySupport() {
        // Given
        launchFragmentInContainer<RegisterFragment>()
        
        // Then - Verify accessibility features
        onView(withId(R.id.logo)).check(matches(hasContentDescription()))
        onView(withId(R.id.email_edit_text)).check(matches(hasImeAction(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT)))
        onView(withId(R.id.password_edit_text)).check(matches(hasImeAction(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT)))
        onView(withId(R.id.confirm_password_edit_text)).check(matches(hasImeAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)))
    }
    
    @Test
    fun registerFragment_passwordFields_haveToggleVisibility() {
        // Given
        launchFragmentInContainer<RegisterFragment>()
        
        // Then - Password fields should have toggle visibility
        onView(withId(R.id.password_input_layout)).check(matches(isDisplayed()))
        onView(withId(R.id.confirm_password_input_layout)).check(matches(isDisplayed()))
        
        // The password toggle icons should be present (this would need more specific testing)
    }
    
    @Test
    fun registerFragment_formValidation_showsErrorsForEmptyFields() {
        // Given
        launchFragmentInContainer<RegisterFragment>()
        
        // When - Try to submit with empty fields
        onView(withId(R.id.create_account_button)).perform(click())
        
        // Then - Appropriate errors should be shown (this would need proper ViewModel integration)
        // This test would need to be expanded with proper mocking
    }
    
    @Test
    fun registerFragment_termsValidation_showsErrorWhenNotAccepted() {
        // Given
        launchFragmentInContainer<RegisterFragment>()
        
        // When - Fill form but don't accept terms
        onView(withId(R.id.email_edit_text)).perform(typeText("test@example.com"))
        onView(withId(R.id.password_edit_text)).perform(typeText("password123"))
        onView(withId(R.id.confirm_password_edit_text)).perform(typeText("password123"), closeSoftKeyboard())
        onView(withId(R.id.create_account_button)).perform(click())
        
        // Then - Terms error should be displayed
        onView(withId(R.id.terms_error)).check(matches(isDisplayed()))
    }
}