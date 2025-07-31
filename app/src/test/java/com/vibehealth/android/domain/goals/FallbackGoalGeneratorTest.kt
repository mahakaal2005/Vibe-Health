package com.vibehealth.android.domain.goals

import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.UserProfile
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for FallbackGoalGenerator.
 * 
 * Tests verify safe fallback goal generation, age/gender adjustments,
 * error handling, and medical safety validation.
 */
class FallbackGoalGeneratorTest {
    
    private lateinit var generator: FallbackGoalGenerator
    
    @Before
    fun setUp() {
        generator = FallbackGoalGenerator()
    }
    
    @Test
    fun shouldGenerateDefaultFallbackGoalsWithoutProfile() {
        // Given: No user profile information
        val userId = "test-user-123"
        
        // When: Generate fallback goals
        val result = generator.generateFallbackGoals(userId)
        
        // Then: Should return safe default goals
        assertEquals(userId, result.userId)
        assertEquals(7500, result.stepsGoal) // Default steps
        assertEquals(1800, result.caloriesGoal) // Default calories
        assertEquals(21, result.heartPointsGoal) // Default heart points
        assertEquals(CalculationSource.FALLBACK_DEFAULT, result.calculationSource)
        assertTrue("Fallback goals should be valid", result.isValid())
        assertTrue("Should be marked as fallback", result.isFallback())
    }
    
    @Test
    fun shouldAdjustGoalsForYouthWhenAgeKnown() {
        // Given: Youth profile (16 years old)
        val userId = "test-youth"
        val youthProfile = createTestProfile(16, Gender.OTHER, 170, 60.0)
        
        // When: Generate fallback goals
        val result = generator.generateFallbackGoals(userId, youthProfile)
        
        // Then: Should have higher goals for youth
        assertTrue("Youth should have higher steps goal", result.stepsGoal > 7500)
        assertTrue("Youth should have higher calories goal", result.caloriesGoal > 1800)
        assertTrue("Youth should have higher heart points goal", result.heartPointsGoal > 21)
        assertEquals(CalculationSource.FALLBACK_DEFAULT, result.calculationSource)
    }
    
    @Test
    fun shouldAdjustGoalsForOlderAdultsWhenAgeKnown() {
        // Given: Older adult profile (70 years old)
        val userId = "test-older-adult"
        val olderProfile = createTestProfile(70, Gender.OTHER, 170, 65.0)
        
        // When: Generate fallback goals
        val result = generator.generateFallbackGoals(userId, olderProfile)
        
        // Then: Should have more conservative goals for older adults
        assertTrue("Older adults should have lower steps goal", result.stepsGoal < 7500)
        assertTrue("Older adults should have lower calories goal", result.caloriesGoal < 1800)
        assertTrue("Older adults should have lower heart points goal", result.heartPointsGoal < 21)
        assertEquals(CalculationSource.FALLBACK_DEFAULT, result.calculationSource)
    }
    
    @Test
    fun shouldAdjustCaloriesForGenderWhenKnown() {
        val userId = "test-gender-adjustment"
        val baseAge = 30
        
        // Test male adjustment
        val maleProfile = createTestProfile(baseAge, Gender.MALE, 175, 75.0)
        val maleResult = generator.generateFallbackGoals(userId, maleProfile)
        
        // Test female adjustment
        val femaleProfile = createTestProfile(baseAge, Gender.FEMALE, 165, 60.0)
        val femaleResult = generator.generateFallbackGoals(userId, femaleProfile)
        
        // Test neutral gender
        val neutralProfile = createTestProfile(baseAge, Gender.OTHER, 170, 65.0)
        val neutralResult = generator.generateFallbackGoals(userId, neutralProfile)
        
        // Then: Males should have higher calorie goals than females
        assertTrue("Males should have higher calorie goals", maleResult.caloriesGoal > femaleResult.caloriesGoal)
        assertTrue("Neutral gender should be between male and female", 
            neutralResult.caloriesGoal >= femaleResult.caloriesGoal && 
            neutralResult.caloriesGoal <= maleResult.caloriesGoal)
    }
    
    @Test
    fun shouldGenerateFallbackGoalsForSpecificErrors() {
        // Given: Different types of calculation errors
        val userId = "test-error-handling"
        val profile = createTestProfile(30, Gender.MALE, 175, 75.0)
        
        val testErrors = listOf(
            IllegalArgumentException("Invalid input"),
            ArithmeticException("Division by zero"),
            NullPointerException("Missing data"),
            RuntimeException("Unexpected error")
        )
        
        testErrors.forEach { error ->
            // When: Generate fallback goals for specific error
            val result = generator.generateFallbackGoalsForError(userId, error, profile)
            
            // Then: Should return valid fallback goals
            assertTrue("Should generate valid goals for ${error.javaClass.simpleName}", result.isValid())
            assertEquals(CalculationSource.FALLBACK_DEFAULT, result.calculationSource)
            assertTrue("Should be within safe bounds", 
                result.stepsGoal in 6000..9000 &&
                result.caloriesGoal in 1400..2400 &&
                result.heartPointsGoal in 17..25)
        }
    }
    
    @Test
    fun shouldEnforceSafeBoundsForAllFallbackGoals() {
        // Given: Various user profiles that might produce extreme adjustments
        val extremeProfiles = listOf(
            createTestProfile(13, Gender.MALE, 200, 120.0), // Very young, tall, heavy
            createTestProfile(90, Gender.FEMALE, 140, 40.0), // Very old, short, light
            createTestProfile(25, Gender.OTHER, 170, 70.0), // Normal adult
            null // No profile
        )
        
        extremeProfiles.forEach { profile ->
            // When: Generate fallback goals
            val result = generator.generateFallbackGoals("test-user", profile)
            
            // Then: Should always be within safe medical bounds
            assertTrue("Steps should be within safe bounds", result.stepsGoal in 6000..9000)
            assertTrue("Calories should be within safe bounds", result.caloriesGoal in 1400..2400)
            assertTrue("Heart points should be within safe bounds", result.heartPointsGoal in 17..25)
            assertTrue("Should pass validation", generator.validateFallbackGoals(result))
        }
    }
    
    @Test
    fun shouldValidateFallbackGoalsCorrectly() {
        // Given: Valid fallback goals
        val validGoals = DailyGoals(
            userId = "test-user",
            stepsGoal = 7500,
            caloriesGoal = 1800,
            heartPointsGoal = 21,
            calculatedAt = java.time.LocalDateTime.now(),
            calculationSource = CalculationSource.FALLBACK_DEFAULT
        )
        
        // When: Validate goals
        val isValid = generator.validateFallbackGoals(validGoals)
        
        // Then: Should be valid
        assertTrue("Valid fallback goals should pass validation", isValid)
    }
    
    @Test
    fun shouldRejectInvalidFallbackGoals() {
        // Given: Invalid fallback goals (outside safe bounds)
        val invalidGoals = listOf(
            // Steps too low
            DailyGoals("test", 4000, 1800, 21, java.time.LocalDateTime.now(), CalculationSource.FALLBACK_DEFAULT),
            // Steps too high
            DailyGoals("test", 12000, 1800, 21, java.time.LocalDateTime.now(), CalculationSource.FALLBACK_DEFAULT),
            // Calories too low
            DailyGoals("test", 7500, 1000, 21, java.time.LocalDateTime.now(), CalculationSource.FALLBACK_DEFAULT),
            // Calories too high
            DailyGoals("test", 7500, 3000, 21, java.time.LocalDateTime.now(), CalculationSource.FALLBACK_DEFAULT),
            // Heart points too low
            DailyGoals("test", 7500, 1800, 10, java.time.LocalDateTime.now(), CalculationSource.FALLBACK_DEFAULT),
            // Heart points too high
            DailyGoals("test", 7500, 1800, 35, java.time.LocalDateTime.now(), CalculationSource.FALLBACK_DEFAULT),
            // Wrong source
            DailyGoals("test", 7500, 1800, 21, java.time.LocalDateTime.now(), CalculationSource.WHO_STANDARD)
        )
        
        invalidGoals.forEach { goals ->
            // When: Validate invalid goals
            val isValid = generator.validateFallbackGoals(goals)
            
            // Then: Should be invalid
            assertFalse("Invalid goals should fail validation: ${goals.sanitizeForLogging()}", isValid)
        }
    }
    
    @Test
    fun shouldCreateEmergencyFallbackGoals() {
        // Given: Need for emergency fallback
        val userId = "emergency-user"
        
        // When: Create emergency fallback goals
        val result = generator.createEmergencyFallbackGoals(userId)
        
        // Then: Should return ultra-conservative goals
        assertEquals(userId, result.userId)
        assertEquals(6000, result.stepsGoal) // More conservative than default
        assertEquals(1600, result.caloriesGoal) // More conservative than default
        assertEquals(18, result.heartPointsGoal) // More conservative than default
        assertEquals(CalculationSource.FALLBACK_DEFAULT, result.calculationSource)
        assertTrue("Emergency goals should be valid", result.isValid())
        assertTrue("Emergency goals should pass validation", generator.validateFallbackGoals(result))
    }
    
    @Test
    fun shouldProvideFallbackExplanations() {
        // Test different failure reasons
        val explanationTests = listOf(
            Pair(null, "update your profile anytime"),
            Pair("Invalid input data", "complete your profile with accurate"),
            Pair("Missing required data", "Complete your profile to receive"),
            Pair("Unknown error", "update your profile anytime")
        )
        
        explanationTests.forEach { (reason, expectedContent) ->
            // When: Get explanation for reason
            val explanation = generator.getFallbackExplanation(reason)
            
            // Then: Should contain appropriate guidance
            assertTrue("Explanation should contain expected content", 
                explanation.contains(expectedContent, ignoreCase = true))
            assertTrue("Should mention WHO guidelines", 
                explanation.contains("WHO", ignoreCase = true))
            assertTrue("Should mention health benefits", 
                explanation.contains("health", ignoreCase = true))
        }
    }
    
    @Test
    fun shouldHandleIncompleteProfilesGracefully() {
        // Given: Profiles with missing information
        val incompleteProfiles = listOf(
            UserProfile(userId = "test1", email = "test@example.com", displayName = "Test"), // No physical data
            UserProfile(userId = "test2", email = "test@example.com", displayName = "Test", 
                birthday = null, gender = Gender.MALE), // No birthday
            UserProfile(userId = "test3", email = "test@example.com", displayName = "Test", 
                birthday = Date(), gender = null) // No gender
        )
        
        incompleteProfiles.forEach { profile ->
            // When: Generate fallback goals
            val result = generator.generateFallbackGoals("test-user", profile)
            
            // Then: Should handle gracefully and return valid goals
            assertTrue("Should handle incomplete profile gracefully", result.isValid())
            assertEquals(CalculationSource.FALLBACK_DEFAULT, result.calculationSource)
            assertTrue("Should pass validation", generator.validateFallbackGoals(result))
        }
    }
    
    @Test
    fun shouldProvideConsistentResultsForSameInput() {
        // Given: Same input parameters
        val userId = "consistency-test"
        val profile = createTestProfile(30, Gender.MALE, 175, 75.0)
        
        // When: Generate fallback goals multiple times
        val results = (1..5).map { 
            generator.generateFallbackGoals(userId, profile)
        }
        
        // Then: Should produce consistent results (excluding timestamp)
        val firstResult = results.first()
        results.forEach { result ->
            assertEquals("Steps should be consistent", firstResult.stepsGoal, result.stepsGoal)
            assertEquals("Calories should be consistent", firstResult.caloriesGoal, result.caloriesGoal)
            assertEquals("Heart points should be consistent", firstResult.heartPointsGoal, result.heartPointsGoal)
            assertEquals("Source should be consistent", firstResult.calculationSource, result.calculationSource)
        }
    }
    
    @Test
    fun shouldHandleExtensionFunctions() {
        // Given: Fallback and non-fallback goals
        val fallbackGoals = generator.generateFallbackGoals("test-user")
        val whoGoals = DailyGoals(
            userId = "test-user",
            stepsGoal = 10000,
            caloriesGoal = 2200,
            heartPointsGoal = 22,
            calculatedAt = java.time.LocalDateTime.now(),
            calculationSource = CalculationSource.WHO_STANDARD
        )
        
        // When: Use extension functions
        val fallbackCheck1 = fallbackGoals.isFallback()
        val fallbackCheck2 = whoGoals.isFallback()
        val fallbackMessage1 = fallbackGoals.getFallbackMessage()
        val fallbackMessage2 = whoGoals.getFallbackMessage()
        
        // Then: Should work correctly
        assertTrue("Fallback goals should be identified as fallback", fallbackCheck1)
        assertFalse("WHO goals should not be identified as fallback", fallbackCheck2)
        assertTrue("Fallback message should mention default goals", 
            fallbackMessage1.contains("default goals", ignoreCase = true))
        assertTrue("WHO message should mention calculated specifically", 
            fallbackMessage2.contains("calculated specifically", ignoreCase = true))
    }
    
    private fun createTestProfile(age: Int, gender: Gender, heightInCm: Int, weightInKg: Double): UserProfile {
        val birthYear = java.time.LocalDate.now().year - age
        val birthday = Date.from(
            java.time.LocalDate.of(birthYear, 1, 1)
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
        )
        
        return UserProfile(
            userId = "test-user-${age}-${gender.name}",
            email = "test@example.com",
            displayName = "Test User $age",
            birthday = birthday,
            gender = gender,
            heightInCm = heightInCm,
            weightInKg = weightInKg,
            hasCompletedOnboarding = true
        )
    }
}