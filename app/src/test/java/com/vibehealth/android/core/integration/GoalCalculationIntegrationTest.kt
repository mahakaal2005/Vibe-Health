package com.vibehealth.android.core.integration

import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.UnitSystem
import com.vibehealth.android.domain.user.UserProfile
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for goal calculation integration
 */
class GoalCalculationIntegrationTest {

    private lateinit var goalCalculationIntegration: GoalCalculationIntegration

    @Before
    fun setup() {
        goalCalculationIntegration = DefaultGoalCalculationIntegration()
    }

    @Test
    fun calculateDailyGoals_maleProfile_shouldReturnValidGoals() = runTest {
        // Given
        val userProfile = UserProfile(
            userId = "test-user",
            email = "test@example.com",
            displayName = "Test User",
            birthday = Date(System.currentTimeMillis() - (25 * 365 * 24 * 60 * 60 * 1000L)), // 25 years ago
            gender = Gender.MALE,
            unitSystem = UnitSystem.METRIC,
            heightInCm = 175,
            weightInKg = 70.0,
            hasCompletedOnboarding = true
        )

        // When
        val result = goalCalculationIntegration.calculateDailyGoals(userProfile)

        // Then
        assertTrue(result is GoalCalculationResult.Success)
        val goals = (result as GoalCalculationResult.Success).goals
        assertEquals("test-user", goals.userId)
        assertTrue(goals.calorieGoal in 1200..3000)
        assertEquals(8, goals.waterGoal)
        assertEquals(30, goals.exerciseGoal)
    }

    @Test
    fun calculateDailyGoals_femaleProfile_shouldReturnValidGoals() = runTest {
        // Given
        val userProfile = UserProfile(
            userId = "test-user-female",
            email = "test@example.com",
            displayName = "Test User",
            birthday = Date(System.currentTimeMillis() - (30 * 365 * 24 * 60 * 60 * 1000L)), // 30 years ago
            gender = Gender.FEMALE,
            unitSystem = UnitSystem.METRIC,
            heightInCm = 165,
            weightInKg = 60.0,
            hasCompletedOnboarding = true
        )

        // When
        val result = goalCalculationIntegration.calculateDailyGoals(userProfile)

        // Then
        assertTrue(result is GoalCalculationResult.Success)
        val goals = (result as GoalCalculationResult.Success).goals
        assertEquals("test-user-female", goals.userId)
        assertTrue(goals.calorieGoal in 1200..3000)
        assertEquals(8, goals.waterGoal)
        assertEquals(30, goals.exerciseGoal)
    }

    @Test
    fun calculateDailyGoals_otherGender_shouldReturnValidGoals() = runTest {
        // Given
        val userProfile = UserProfile(
            userId = "test-user-other",
            email = "test@example.com",
            displayName = "Test User",
            birthday = Date(System.currentTimeMillis() - (28 * 365 * 24 * 60 * 60 * 1000L)), // 28 years ago
            gender = Gender.OTHER,
            unitSystem = UnitSystem.METRIC,
            heightInCm = 170,
            weightInKg = 65.0,
            hasCompletedOnboarding = true
        )

        // When
        val result = goalCalculationIntegration.calculateDailyGoals(userProfile)

        // Then
        assertTrue(result is GoalCalculationResult.Success)
        val goals = (result as GoalCalculationResult.Success).goals
        assertEquals("test-user-other", goals.userId)
        assertTrue(goals.calorieGoal in 1200..3000)
        assertEquals(8, goals.waterGoal)
        assertEquals(30, goals.exerciseGoal)
    }

    @Test
    fun calculateDailyGoals_extremeValues_shouldClampCalories() = runTest {
        // Given - Very small person (should clamp to minimum)
        val smallProfile = UserProfile(
            userId = "test-user-small",
            birthday = Date(System.currentTimeMillis() - (20 * 365 * 24 * 60 * 60 * 1000L)),
            gender = Gender.FEMALE,
            heightInCm = 140,
            weightInKg = 40.0,
            hasCompletedOnboarding = true
        )

        // When
        val smallResult = goalCalculationIntegration.calculateDailyGoals(smallProfile)

        // Then
        assertTrue(smallResult is GoalCalculationResult.Success)
        val smallGoals = (smallResult as GoalCalculationResult.Success).goals
        assertTrue(smallGoals.calorieGoal >= 1200)

        // Given - Very large person (should clamp to maximum)
        val largeProfile = UserProfile(
            userId = "test-user-large",
            birthday = Date(System.currentTimeMillis() - (25 * 365 * 24 * 60 * 60 * 1000L)),
            gender = Gender.MALE,
            heightInCm = 200,
            weightInKg = 120.0,
            hasCompletedOnboarding = true
        )

        // When
        val largeResult = goalCalculationIntegration.calculateDailyGoals(largeProfile)

        // Then
        assertTrue(largeResult is GoalCalculationResult.Success)
        val largeGoals = (largeResult as GoalCalculationResult.Success).goals
        assertTrue(largeGoals.calorieGoal <= 3000)
    }

    @Test
    fun calculateDailyGoals_profileWithoutBirthday_shouldUseDefaultAge() = runTest {
        // Given
        val userProfile = UserProfile(
            userId = "test-user-no-birthday",
            email = "test@example.com",
            displayName = "Test User",
            birthday = null, // No birthday
            gender = Gender.MALE,
            unitSystem = UnitSystem.METRIC,
            heightInCm = 175,
            weightInKg = 70.0,
            hasCompletedOnboarding = true
        )

        // When
        val result = goalCalculationIntegration.calculateDailyGoals(userProfile)

        // Then
        assertTrue(result is GoalCalculationResult.Success)
        val goals = (result as GoalCalculationResult.Success).goals
        assertTrue(goals.calorieGoal in 1200..3000)
    }
}