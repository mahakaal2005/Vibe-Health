package com.vibehealth.android.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vibehealth.android.core.integration.OnboardingIntegrationManager
import com.vibehealth.android.core.integration.NavigationDecision
import com.vibehealth.android.core.integration.OnboardingRequirement
import com.vibehealth.android.domain.user.UserProfile
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.UnitSystem
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for onboarding flow with authentication system
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OnboardingIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var integrationManager: OnboardingIntegrationManager

    private val testUserId = "test-user-integration"

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun checkOnboardingRequirement_newUser_shouldRequireOnboarding() = runTest {
        // When
        val requirement = integrationManager.checkOnboardingRequirement("new-user-id")

        // Then
        assertEquals(OnboardingRequirement.REQUIRED, requirement)
    }

    @Test
    fun handleExistingUserOnboardingCheck_newUser_shouldNavigateToOnboarding() = runTest {
        // When
        val decision = integrationManager.handleExistingUserOnboardingCheck("new-user-id")

        // Then
        assertEquals(NavigationDecision.NAVIGATE_TO_ONBOARDING, decision)
    }

    @Test
    fun completeOnboardingIntegration_validProfile_shouldSucceed() = runTest {
        // Given
        val userProfile = UserProfile(
            userId = testUserId,
            email = "test@example.com",
            displayName = "Test User",
            birthday = Date(System.currentTimeMillis() - (25 * 365 * 24 * 60 * 60 * 1000L)), // 25 years ago
            gender = Gender.MALE,
            unitSystem = UnitSystem.METRIC,
            heightInCm = 175,
            weightInKg = 70.0,
            hasCompletedOnboarding = true,
            createdAt = Date(),
            updatedAt = Date()
        )

        // When
        val result = integrationManager.completeOnboardingIntegration(userProfile)

        // Then
        assertTrue(result.isSuccess())
    }

    @Test
    fun integrationContract_shouldProvideValidCallbacks() {
        // When
        val contract = integrationManager.createIntegrationContract()

        // Then
        assertTrue(contract.onboardingCompleteCallback != null)
        assertTrue(contract.goalCalculationTrigger != null)
        assertTrue(contract.sessionUpdateCallback != null)
    }

    @Test
    fun navigateToMainApp_shouldNotThrowException() {
        // Given
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val userProfile = UserProfile(
            userId = testUserId,
            email = "test@example.com",
            displayName = "Test User",
            hasCompletedOnboarding = true
        )

        // When/Then - Should not throw exception
        integrationManager.navigateToMainApp(context, userProfile)
    }
}