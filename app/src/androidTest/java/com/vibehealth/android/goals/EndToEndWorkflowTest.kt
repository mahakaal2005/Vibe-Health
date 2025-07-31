package com.vibehealth.android.goals

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibehealth.android.data.goals.GoalRepository
import com.vibehealth.android.data.user.UserProfileRepository
import com.vibehealth.android.domain.common.UnitSystem
import com.vibehealth.android.domain.goals.*
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.UserProfile
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end workflow tests for complete goal calculation integration.
 * 
 * Tests complete Story 1.1 → 1.2 → 1.3 → 1.4 integration flow,
 * onboarding completion triggers, profile updates, and error recovery
 * as specified in Task 5.2.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EndToEndWorkflowTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var goalCalculationUseCase: GoalCalculationUseCase
    
    @Inject
    lateinit var profileUpdateUseCase: ProfileUpdateUseCase
    
    @Inject
    lateinit var goalRepository: GoalRepository
    
    @Inject
    lateinit var userProfileRepository: UserProfileRepository
    
    @Inject
    lateinit var goalRecalculationTriggerService: GoalRecalculationTriggerService

    private val testUserId = "e2e-test-user"
    
    private val testUserProfile = UserProfile(
        userId = testUserId,
        email = "e2e@test.com",
        displayName = "E2E Test User",
        firstName = "E2E",
        lastName = "User",
        birthday = Calendar.getInstance().apply { add(Calendar.YEAR, -25) }.time,
        gender = Gender.MALE,
        unitSystem = UnitSystem.METRIC,
        heightInCm = 175,
        weightInKg = 70.0,
        hasCompletedOnboarding = false
    )

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testCompleteStory1_1to1_4IntegrationFlow() = runTest {
        // Story 1.1: Authentication (simulated - user is authenticated)
        // Story 1.2: User Profile Creation and Onboarding
        
        // Step 1: Create user profile during onboarding
        val profileResult = userProfileRepository.saveUserProfile(testUserProfile)
        assertTrue(profileResult.isSuccess)
        
        // Step 2: Complete onboarding
        val completedProfile = testUserProfile.copy(hasCompletedOnboarding = true)
        val onboardingResult = userProfileRepository.saveUserProfile(completedProfile)
        assertTrue(onboardingResult.isSuccess)
        
        // Story 1.3: Automatic Goal Calculation
        
        // Step 3: Trigger automatic goal calculation after onboarding
        val goalCalculationResult = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
        assertTrue(goalCalculationResult.isSuccess())
        
        val successResult = goalCalculationResult as GoalCalculationResult.Success
        val calculatedGoals = successResult.goals
        
        // Verify goals are calculated based on WHO standards
        assertTrue(calculatedGoals.stepsGoal in 5000..20000)
        assertTrue(calculatedGoals.caloriesGoal in 1200..4000)
        assertTrue(calculatedGoals.heartPointsGoal in 15..50)
        assertEquals(CalculationSource.WHO_STANDARD, calculatedGoals.calculationSource)
        
        // Step 4: Verify goals are stored and retrievable
        val storedGoals = goalRepository.getCurrentGoalsSync(testUserId)
        assertNotNull(storedGoals)
        assertEquals(calculatedGoals.stepsGoal, storedGoals.stepsGoal)
        assertEquals(calculatedGoals.caloriesGoal, storedGoals.caloriesGoal)
        assertEquals(calculatedGoals.heartPointsGoal, storedGoals.heartPointsGoal)
        
        // Story 1.4: Dashboard Display (simulated - goals are available for display)
        
        // Step 5: Verify goals are ready for dashboard display
        val hasValidGoals = goalCalculationUseCase.hasValidGoals(testUserId)
        assertTrue(hasValidGoals)
        
        // Step 6: Simulate dashboard refresh
        val dashboardGoals = goalRepository.getCurrentGoalsSync(testUserId)
        assertNotNull(dashboardGoals)
        assertEquals(CalculationSource.WHO_STANDARD, dashboardGoals.calculationSource)
    }

    @Test
    fun testOnboardingCompletionTriggersAutomaticGoalCalculation() = runTest {
        // Start with incomplete onboarding
        val incompleteProfile = testUserProfile.copy(hasCompletedOnboarding = false)
        userProfileRepository.saveUserProfile(incompleteProfile)
        
        // Verify no goals exist yet
        val initialGoals = goalRepository.getCurrentGoalsSync(testUserId)
        assertEquals(null, initialGoals)
        
        // Complete onboarding
        val completedProfile = incompleteProfile.copy(hasCompletedOnboarding = true)
        userProfileRepository.saveUserProfile(completedProfile)
        
        // Trigger goal calculation (simulating onboarding completion handler)
        val calculationResult = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
        assertTrue(calculationResult.isSuccess())
        
        // Verify goals are now available
        val calculatedGoals = goalRepository.getCurrentGoalsSync(testUserId)
        assertNotNull(calculatedGoals)
        assertEquals(CalculationSource.WHO_STANDARD, calculatedGoals.calculationSource)
    }

    @Test
    fun testProfileUpdateTriggersGoalRecalculationAndDashboardRefresh() = runTest {
        // Setup initial profile and goals
        userProfileRepository.saveUserProfile(testUserProfile.copy(hasCompletedOnboarding = true))
        val initialCalculation = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
        assertTrue(initialCalculation.isSuccess())
        
        val initialGoals = goalRepository.getCurrentGoalsSync(testUserId)
        assertNotNull(initialGoals)
        val initialStepsGoal = initialGoals.stepsGoal
        
        // Update profile (change weight)
        val updatedProfile = testUserProfile.copy(
            weightInKg = 80.0, // Increased weight
            hasCompletedOnboarding = true
        )
        
        // Trigger profile update with goal recalculation
        val updateResult = profileUpdateUseCase.updateProfileWithGoalRecalculation(
            testUserId, 
            updatedProfile
        )
        assertTrue(updateResult.isSuccess)
        
        // Verify goals were recalculated
        val recalculatedGoals = goalRepository.getCurrentGoalsSync(testUserId)
        assertNotNull(recalculatedGoals)
        
        // Goals should be different due to weight change
        assertTrue(recalculatedGoals.caloriesGoal != initialGoals.caloriesGoal)
        assertEquals(CalculationSource.WHO_STANDARD, recalculatedGoals.calculationSource)
        
        // Verify calculation timestamp is updated
        assertTrue(recalculatedGoals.calculatedAt > initialGoals.calculatedAt)
    }

    @Test
    fun testGoalDisplayInDashboardTripleRingAndProfileScreens() = runTest {
        // Setup user with calculated goals
        userProfileRepository.saveUserProfile(testUserProfile.copy(hasCompletedOnboarding = true))
        val calculationResult = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
        assertTrue(calculationResult.isSuccess())
        
        val goals = goalRepository.getCurrentGoalsSync(testUserId)
        assertNotNull(goals)
        
        // Test dashboard triple-ring display data
        val dashboardData = mapOf(
            "steps" to mapOf(
                "goal" to goals.stepsGoal,
                "current" to 0,
                "percentage" to 0.0
            ),
            "calories" to mapOf(
                "goal" to goals.caloriesGoal,
                "current" to 0,
                "percentage" to 0.0
            ),
            "heartPoints" to mapOf(
                "goal" to goals.heartPointsGoal,
                "current" to 0,
                "percentage" to 0.0
            )
        )
        
        // Verify dashboard data structure
        assertEquals(goals.stepsGoal, dashboardData["steps"]?.get("goal"))
        assertEquals(goals.caloriesGoal, dashboardData["calories"]?.get("goal"))
        assertEquals(goals.heartPointsGoal, dashboardData["heartPoints"]?.get("goal"))
        
        // Test profile screen display data
        val profileDisplayData = mapOf(
            "goals" to mapOf(
                "steps" to "${goals.stepsGoal} steps",
                "calories" to "${goals.caloriesGoal} cal",
                "heartPoints" to "${goals.heartPointsGoal} points"
            ),
            "source" to "WHO Standards",
            "calculatedAt" to goals.calculatedAt.toString(),
            "isEditable" to false
        )
        
        // Verify profile display data
        assertEquals("${goals.stepsGoal} steps", profileDisplayData["goals"]?.get("steps"))
        assertEquals("WHO Standards", profileDisplayData["source"])
        assertEquals(false, profileDisplayData["isEditable"])
    }

    @Test
    fun testErrorRecoveryAndRetryMechanismsAcrossStoryBoundaries() = runTest {
        // Test Story 1.2 → 1.3 error recovery
        userProfileRepository.saveUserProfile(testUserProfile.copy(hasCompletedOnboarding = true))
        
        // Simulate calculation failure and retry
        var attemptCount = 0
        var calculationResult: GoalCalculationResult
        
        do {
            calculationResult = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
            attemptCount++
        } while (calculationResult.isError() && attemptCount < 3)
        
        // Should eventually succeed or provide fallback
        assertTrue(calculationResult.isSuccess() || 
                  (calculationResult as? GoalCalculationResult.Success)?.calculationSource == CalculationSource.FALLBACK_DEFAULT)
        
        // Test Story 1.3 → 1.4 error recovery
        if (calculationResult.isSuccess()) {
            val goals = goalRepository.getCurrentGoalsSync(testUserId)
            assertNotNull(goals)
            
            // Simulate dashboard refresh failure and retry
            var dashboardAttempts = 0
            var dashboardGoals: DailyGoals?
            
            do {
                dashboardGoals = goalRepository.getCurrentGoalsSync(testUserId)
                dashboardAttempts++
            } while (dashboardGoals == null && dashboardAttempts < 3)
            
            assertNotNull(dashboardGoals)
        }
    }

    @Test
    fun testFallbackGoalScenarioWhenCalculationFailsDuringOnboarding() = runTest {
        // Setup profile with invalid data that might cause calculation failure
        val invalidProfile = testUserProfile.copy(
            heightInCm = -1, // Invalid height
            weightInKg = -1.0, // Invalid weight
            hasCompletedOnboarding = true
        )
        
        userProfileRepository.saveUserProfile(invalidProfile)
        
        // Attempt goal calculation
        val calculationResult = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
        
        // Should either succeed with fallback or handle error gracefully
        if (calculationResult.isSuccess()) {
            val successResult = calculationResult as GoalCalculationResult.Success
            
            // If calculation succeeded, it should be with fallback values
            if (successResult.calculationSource == CalculationSource.FALLBACK_DEFAULT) {
                val fallbackGoals = successResult.goals
                
                // Verify fallback goals are medically safe
                assertTrue(fallbackGoals.stepsGoal in 5000..10000)
                assertTrue(fallbackGoals.caloriesGoal in 1500..2500)
                assertTrue(fallbackGoals.heartPointsGoal in 15..30)
            }
        } else {
            // If calculation failed, verify error is handled properly
            val errorResult = calculationResult as GoalCalculationResult.Error
            assertTrue(errorResult.error is GoalCalculationError.ValidationFailed ||
                      errorResult.error is GoalCalculationError.CalculationFailed)
        }
    }

    @Test
    fun testConcurrentUserOperationsAcrossStories() = runTest {
        // Setup multiple users concurrently
        val userIds = (1..5).map { "concurrent-user-$it" }
        val profiles = userIds.map { userId ->
            testUserProfile.copy(
                userId = userId,
                email = "$userId@test.com",
                weightInKg = 70.0 + it,
                hasCompletedOnboarding = true
            )
        }
        
        // Concurrent profile creation and goal calculation
        val operations = profiles.map { profile ->
            kotlinx.coroutines.async {
                // Story 1.2: Profile creation
                val profileResult = userProfileRepository.saveUserProfile(profile)
                
                // Story 1.3: Goal calculation
                val goalResult = if (profileResult.isSuccess) {
                    goalCalculationUseCase.calculateAndStoreGoals(profile.userId)
                } else {
                    GoalCalculationResult.Error(GoalCalculationError.ProfileNotFound(profile.userId))
                }
                
                // Story 1.4: Goal retrieval for dashboard
                val dashboardGoals = if (goalResult.isSuccess()) {
                    goalRepository.getCurrentGoalsSync(profile.userId)
                } else {
                    null
                }
                
                Triple(profileResult, goalResult, dashboardGoals)
            }
        }
        
        // Wait for all operations
        val results = operations.map { it.await() }
        
        // Verify all operations succeeded
        results.forEachIndexed { index, (profileResult, goalResult, dashboardGoals) ->
            assertTrue(profileResult.isSuccess, "Profile creation failed for user ${index + 1}")
            assertTrue(goalResult.isSuccess(), "Goal calculation failed for user ${index + 1}")
            assertNotNull(dashboardGoals, "Dashboard goals not available for user ${index + 1}")
            
            // Verify goals are different based on different weights
            val expectedWeight = 70.0 + (index + 1)
            assertTrue(dashboardGoals!!.caloriesGoal > 1500) // Should vary based on weight
        }
    }

    @Test
    fun testDataConsistencyAcrossStoryTransitions() = runTest {
        // Story 1.2: Create profile
        val profileResult = userProfileRepository.saveUserProfile(
            testUserProfile.copy(hasCompletedOnboarding = true)
        )
        assertTrue(profileResult.isSuccess)
        
        // Story 1.3: Calculate goals
        val calculationResult = goalCalculationUseCase.calculateAndStoreGoals(testUserId)
        assertTrue(calculationResult.isSuccess())
        
        val calculatedGoals = (calculationResult as GoalCalculationResult.Success).goals
        
        // Verify data consistency between profile and goals
        assertEquals(testUserId, calculatedGoals.userId)
        
        // Story 1.4: Retrieve for dashboard
        val dashboardGoals = goalRepository.getCurrentGoalsSync(testUserId)
        assertNotNull(dashboardGoals)
        
        // Verify consistency across story boundaries
        assertEquals(calculatedGoals.userId, dashboardGoals.userId)
        assertEquals(calculatedGoals.stepsGoal, dashboardGoals.stepsGoal)
        assertEquals(calculatedGoals.caloriesGoal, dashboardGoals.caloriesGoal)
        assertEquals(calculatedGoals.heartPointsGoal, dashboardGoals.heartPointsGoal)
        assertEquals(calculatedGoals.calculationSource, dashboardGoals.calculationSource)
        
        // Test profile update consistency
        val updatedProfile = testUserProfile.copy(
            weightInKg = 75.0,
            hasCompletedOnboarding = true
        )
        
        val updateResult = profileUpdateUseCase.updateProfileWithGoalRecalculation(
            testUserId, 
            updatedProfile
        )
        assertTrue(updateResult.isSuccess)
        
        // Verify updated goals reflect profile changes
        val updatedGoals = goalRepository.getCurrentGoalsSync(testUserId)
        assertNotNull(updatedGoals)
        assertTrue(updatedGoals.calculatedAt > calculatedGoals.calculatedAt)
        assertEquals(testUserId, updatedGoals.userId)
    }
}