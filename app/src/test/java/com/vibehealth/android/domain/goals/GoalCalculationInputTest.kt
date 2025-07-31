package com.vibehealth.android.domain.goals

import com.vibehealth.android.domain.user.Gender
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for GoalCalculationInput data model.
 * 
 * Tests verify validation logic, sanitization for logging,
 * and proper handling of edge cases.
 */
class GoalCalculationInputTest {
    
    @Test
    fun shouldValidateCorrectlyForValidInput() {
        // Given: Valid input within all acceptable ranges
        val validInput = GoalCalculationInput(
            age = 30,
            gender = Gender.MALE,
            heightInCm = 175,
            weightInKg = 70.0,
            activityLevel = ActivityLevel.MODERATE
        )
        
        // When: Validate input
        val isValid = validInput.isValid()
        
        // Then: Should be valid
        assertTrue("Valid input should pass validation", isValid)
    }
    
    @Test
    fun shouldRejectInvalidAge() {
        val baseInput = GoalCalculationInput(
            age = 30, // Will be overridden
            gender = Gender.MALE,
            heightInCm = 175,
            weightInKg = 70.0
        )
        
        // Test ages outside valid range
        val invalidAges = listOf(12, 121, -5, 0)
        
        invalidAges.forEach { age ->
            val input = baseInput.copy(age = age)
            assertFalse("Age $age should be invalid", input.isValid())
        }
        
        // Test ages within valid range
        val validAges = listOf(13, 18, 30, 65, 120)
        
        validAges.forEach { age ->
            val input = baseInput.copy(age = age)
            assertTrue("Age $age should be valid", input.isValid())
        }
    }
    
    @Test
    fun shouldRejectInvalidHeight() {
        val baseInput = GoalCalculationInput(
            age = 30,
            gender = Gender.MALE,
            heightInCm = 175, // Will be overridden
            weightInKg = 70.0
        )
        
        // Test heights outside valid range
        val invalidHeights = listOf(99, 251, -10, 0)
        
        invalidHeights.forEach { height ->
            val input = baseInput.copy(heightInCm = height)
            assertFalse("Height $height should be invalid", input.isValid())
        }
        
        // Test heights within valid range
        val validHeights = listOf(100, 150, 175, 200, 250)
        
        validHeights.forEach { height ->
            val input = baseInput.copy(heightInCm = height)
            assertTrue("Height $height should be valid", input.isValid())
        }
    }
    
    @Test
    fun shouldRejectInvalidWeight() {
        val baseInput = GoalCalculationInput(
            age = 30,
            gender = Gender.MALE,
            heightInCm = 175,
            weightInKg = 70.0 // Will be overridden
        )
        
        // Test weights outside valid range
        val invalidWeights = listOf(29.9, 300.1, -5.0, 0.0)
        
        invalidWeights.forEach { weight ->
            val input = baseInput.copy(weightInKg = weight)
            assertFalse("Weight $weight should be invalid", input.isValid())
        }
        
        // Test weights within valid range
        val validWeights = listOf(30.0, 50.0, 70.0, 100.0, 300.0)
        
        validWeights.forEach { weight ->
            val input = baseInput.copy(weightInKg = weight)
            assertTrue("Weight $weight should be valid", input.isValid())
        }
    }
    
    @Test
    fun shouldHandleAllGenderTypes() {
        val baseInput = GoalCalculationInput(
            age = 30,
            gender = Gender.MALE, // Will be overridden
            heightInCm = 175,
            weightInKg = 70.0
        )
        
        // Test all gender options
        val allGenders = listOf(Gender.MALE, Gender.FEMALE, Gender.OTHER, Gender.PREFER_NOT_TO_SAY)
        
        allGenders.forEach { gender ->
            val input = baseInput.copy(gender = gender)
            assertTrue("Gender $gender should be valid", input.isValid())
        }
    }
    
    @Test
    fun shouldHandleAllActivityLevels() {
        val baseInput = GoalCalculationInput(
            age = 30,
            gender = Gender.MALE,
            heightInCm = 175,
            weightInKg = 70.0,
            activityLevel = ActivityLevel.SEDENTARY // Will be overridden
        )
        
        // Test all activity levels
        val allActivityLevels = listOf(
            ActivityLevel.SEDENTARY,
            ActivityLevel.LIGHT,
            ActivityLevel.MODERATE,
            ActivityLevel.ACTIVE,
            ActivityLevel.VERY_ACTIVE
        )
        
        allActivityLevels.forEach { activityLevel ->
            val input = baseInput.copy(activityLevel = activityLevel)
            assertTrue("Activity level $activityLevel should be valid", input.isValid())
        }
    }
    
    @Test
    fun shouldUseDefaultActivityLevelForUrbanProfessionals() {
        // Given: Input without explicit activity level
        val input = GoalCalculationInput(
            age = 30,
            gender = Gender.MALE,
            heightInCm = 175,
            weightInKg = 70.0
            // activityLevel defaults to DEFAULT_FOR_URBAN_PROFESSIONALS
        )
        
        // Then: Should use LIGHT activity level as default
        assertEquals(ActivityLevel.LIGHT, input.activityLevel)
        assertEquals(ActivityLevel.DEFAULT_FOR_URBAN_PROFESSIONALS, input.activityLevel)
    }
    
    @Test
    fun shouldSanitizeDataForLogging() {
        // Given: Input with specific values
        val input = GoalCalculationInput(
            age = 25, // Adult category
            gender = Gender.FEMALE,
            heightInCm = 165, // Average height
            weightInKg = 60.0, // Below average weight
            activityLevel = ActivityLevel.MODERATE
        )
        
        // When: Sanitize for logging
        val sanitized = input.sanitizeForLogging()
        
        // Then: Should anonymize sensitive data while preserving categories
        assertEquals(-2, sanitized.age) // Adult category
        assertEquals(-2, sanitized.heightInCm) // Average height category
        assertEquals(-1.0, sanitized.weightInKg) // Below average weight category
        assertEquals(Gender.FEMALE, sanitized.gender) // Gender preserved for calculation debugging
        assertEquals(ActivityLevel.MODERATE, sanitized.activityLevel) // Activity level preserved
    }
    
    @Test
    fun shouldSanitizeYouthAgeCategory() {
        // Given: Youth input
        val input = GoalCalculationInput(
            age = 16, // Youth category
            gender = Gender.OTHER,
            heightInCm = 170,
            weightInKg = 55.0
        )
        
        // When: Sanitize for logging
        val sanitized = input.sanitizeForLogging()
        
        // Then: Should use youth category code
        assertEquals(-1, sanitized.age) // Youth category
    }
    
    @Test
    fun shouldSanitizeOlderAdultAgeCategory() {
        // Given: Older adult input
        val input = GoalCalculationInput(
            age = 70, // Older adult category
            gender = Gender.OTHER,
            heightInCm = 170,
            weightInKg = 65.0
        )
        
        // When: Sanitize for logging
        val sanitized = input.sanitizeForLogging()
        
        // Then: Should use older adult category code
        assertEquals(-3, sanitized.age) // Older adult category
    }
    
    @Test
    fun shouldSanitizeHeightCategories() {
        val baseInput = GoalCalculationInput(
            age = 30,
            gender = Gender.OTHER,
            heightInCm = 170, // Will be overridden
            weightInKg = 70.0
        )
        
        // Test height categories
        val heightTests = listOf(
            Triple(150, -1, "Below average"), // < 160
            Triple(170, -2, "Average"), // 160-179
            Triple(190, -3, "Above average") // >= 180
        )
        
        heightTests.forEach { (height, expectedCode, description) ->
            val input = baseInput.copy(heightInCm = height)
            val sanitized = input.sanitizeForLogging()
            assertEquals(expectedCode, sanitized.heightInCm, "Height $height should be categorized as $description")
        }
    }
    
    @Test
    fun shouldSanitizeWeightCategories() {
        val baseInput = GoalCalculationInput(
            age = 30,
            gender = Gender.OTHER,
            heightInCm = 170,
            weightInKg = 70.0 // Will be overridden
        )
        
        // Test weight categories
        val weightTests = listOf(
            Triple(50.0, -1.0, "Below average"), // < 60
            Triple(70.0, -2.0, "Average"), // 60-79
            Triple(90.0, -3.0, "Above average") // >= 80
        )
        
        weightTests.forEach { (weight, expectedCode, description) ->
            val input = baseInput.copy(weightInKg = weight)
            val sanitized = input.sanitizeForLogging()
            assertEquals(expectedCode, sanitized.weightInKg, "Weight $weight should be categorized as $description")
        }
    }
    
    @Test
    fun shouldPreserveNonSensitiveDataInSanitization() {
        // Given: Input with all fields
        val input = GoalCalculationInput(
            age = 30,
            gender = Gender.PREFER_NOT_TO_SAY,
            heightInCm = 175,
            weightInKg = 70.0,
            activityLevel = ActivityLevel.ACTIVE
        )
        
        // When: Sanitize for logging
        val sanitized = input.sanitizeForLogging()
        
        // Then: Should preserve gender and activity level for debugging
        assertEquals(Gender.PREFER_NOT_TO_SAY, sanitized.gender)
        assertEquals(ActivityLevel.ACTIVE, sanitized.activityLevel)
    }
    
    @Test
    fun shouldHandleBoundaryValues() {
        // Test exact boundary values
        val boundaryTests = listOf(
            // Age boundaries
            Triple(13, 100, 30.0), // Minimum age
            Triple(120, 250, 300.0), // Maximum age
            // Height boundaries
            Triple(30, 100, 70.0), // Minimum height
            Triple(30, 250, 70.0), // Maximum height
            // Weight boundaries
            Triple(30, 175, 30.0), // Minimum weight
            Triple(30, 175, 300.0) // Maximum weight
        )
        
        boundaryTests.forEach { (age, height, weight) ->
            val input = GoalCalculationInput(
                age = age,
                gender = Gender.OTHER,
                heightInCm = height,
                weightInKg = weight
            )
            
            assertTrue("Boundary values (age=$age, height=$height, weight=$weight) should be valid", input.isValid())
        }
    }
}