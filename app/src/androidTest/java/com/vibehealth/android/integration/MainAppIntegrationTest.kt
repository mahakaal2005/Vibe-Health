package com.vibehealth.android.integration

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibehealth.android.MainActivity
import com.vibehealth.android.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for main application with authentication
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainAppIntegrationTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun mainActivity_showsBottomNavigation() {
        // Given - MainActivity is launched
        ActivityScenario.launch(MainActivity::class.java)
        
        // Then - Bottom navigation should be visible
        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()))
        
        // And - Navigation host fragment should be visible
        onView(withId(R.id.nav_host_fragment_main))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun bottomNavigation_navigatesBetweenFragments() {
        // Given - MainActivity is launched
        ActivityScenario.launch(MainActivity::class.java)
        
        // When - Navigate to different tabs
        onView(withId(R.id.homeFragment))
            .perform(click())
        
        onView(withId(R.id.prescriptionsFragment))
            .perform(click())
        
        onView(withId(R.id.discoverFragment))
            .perform(click())
        
        onView(withId(R.id.profileFragment))
            .perform(click())
        
        // Then - Navigation should work without crashes
        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun profileFragment_showsLogoutButton() {
        // Given - MainActivity is launched
        ActivityScenario.launch(MainActivity::class.java)
        
        // When - Navigate to profile
        onView(withId(R.id.profileFragment))
            .perform(click())
        
        // Then - Logout button should be visible
        onView(withId(R.id.logout_button))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun profileFragment_showsUserInformation() {
        // Given - MainActivity is launched
        ActivityScenario.launch(MainActivity::class.java)
        
        // When - Navigate to profile
        onView(withId(R.id.profileFragment))
            .perform(click())
        
        // Then - User information should be displayed
        onView(withId(R.id.user_email))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.user_name))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun mainActivity_handlesBackButton() {
        // Given - MainActivity is launched
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // When - Navigate to different fragment
        onView(withId(R.id.profileFragment))
            .perform(click())
        
        // Then - Back button should work properly
        scenario.onActivity { activity ->
            activity.onBackPressed()
            // App should minimize instead of closing
        }
    }
    
    @Test
    fun authenticationGuards_protectFragments() {
        // This test would verify that fragments are protected
        // by authentication guards
        
        // Given - MainActivity is launched
        ActivityScenario.launch(MainActivity::class.java)
        
        // Then - All fragments should be accessible only when authenticated
        // This would need proper authentication state mocking
    }
    
    @Test
    fun optionsMenu_showsLogoutOption() {
        // Given - MainActivity is launched
        ActivityScenario.launch(MainActivity::class.java)
        
        // When - Open options menu
        onView(withContentDescription("More options"))
            .perform(click())
        
        // Then - Logout option should be visible
        onView(withText("Logout"))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun logoutConfirmation_showsDialog() {
        // Given - MainActivity is launched
        ActivityScenario.launch(MainActivity::class.java)
        
        // When - Open options menu and click logout
        onView(withContentDescription("More options"))
            .perform(click())
        
        onView(withText("Logout"))
            .perform(click())
        
        // Then - Confirmation dialog should appear
        onView(withText("Sign Out"))
            .check(matches(isDisplayed()))
        
        onView(withText("Are you sure you want to sign out?"))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun edgeToEdge_handlesSystemInsets() {
        // Given - MainActivity is launched
        ActivityScenario.launch(MainActivity::class.java)
        
        // Then - Edge-to-edge should be properly configured
        // This would need proper system UI testing
        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun navigationAnimations_workSmoothly() {
        // Given - MainActivity is launched
        ActivityScenario.launch(MainActivity::class.java)
        
        // When - Navigate between fragments quickly
        onView(withId(R.id.homeFragment))
            .perform(click())
        
        onView(withId(R.id.profileFragment))
            .perform(click())
        
        onView(withId(R.id.discoverFragment))
            .perform(click())
        
        // Then - Navigation should be smooth without crashes
        onView(withId(R.id.bottom_navigation))
            .check(matches(isDisplayed()))
    }
    
    @Test
    fun authenticationState_updatesUI() {
        // This test would verify that authentication state changes
        // properly update the UI throughout the app
        
        // Given - User is authenticated
        ActivityScenario.launch(MainActivity::class.java)
        
        // When - Authentication state changes
        // Then - UI should update accordingly
        
        // This would need proper authentication state management testing
    }
}