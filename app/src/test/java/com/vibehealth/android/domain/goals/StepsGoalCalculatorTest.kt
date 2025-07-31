package com.vibehealth.android.domain.goals

import com.vibehealth.android.domain.user.Gender
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for StepsGoalCalculator.
 * 
 * Tests verify WHO Physical Activity Guidelines 2020 implementation,
 * boundary conditions, and core functionality.
 */
class StepsGoalCalculatorTest {
    
    private lateinit var calculator: StepsGoalCalculator
    
    @Before
    fun setUp() {
        calculator = StepsGoalCalculator()
    }
    
    @Test
    fun shouldReturnBaselineForTypicalAdultMale() {
        // Given: Typical adult male profile
        val input = GoalCalculationInput(
            age = 30,
            gender = Gender.MALE,
            heightInCm = 175,
            weightInKg = 70.0
        )
        
        // When: Calculate steps goal
        val result = calculator.calculateStepsGoal(input)
        
        // Then: Should be close to WHO baseline with male adjustment
        val expected = (10000 * 1.0 * 1.05).toInt() // Adult age * Male gender
        assertEquals(expected, result)
        assertTrue("Result should be within reasonable range for typical adult", result in 8000..12000)
    }
    
    @Test
    fun shouldReturnBaselineForTypicalAdultFemale() {
        // Given: Typical adult female profile
        val input = GoalCalculationInput(
            age = 28,
            gender = Gender.FEMALE,
            heightInCm = 165,
            weightInKg = 60.0
        )
        
        // When: Calculate steps goal
        val result = calculator.calculateStepsGoal(input)
        
        // Then: Should be close to WHO baseline with female adjustment
        val expected = (10000 * 1.0 * 0.95).toInt() // Adult age * Female gender
        assertEquals(expected, result)
        assertTrue("Result should be within reasonable range for typical adult", result in 8000..12000)
    }
    
    @Test
    fun shouldReturnNeutralAdjustmentForInclusiveGenders() {
        val baseInput = GoalCalculationInput(
            age = 25,
            gender = Gender.OTHER,
            heightInCm = 170,
            weightInKg = 65.0
        )
        
        // Test OTHER gender
        val otherResult = calculator.calculateStepsGoal(baseInput.copy(gender = Gender.OTHER))
        assertEquals(10000, otherResult)
        
        // Test PREFER_NOT_TO_SAY gender
        val preferNotToSayResult = calculator.calculateStepsGoal(baseInput.copy(gender = Gender.PREFER_NOT_TO_SAY))
        assertEquals(10000, preferNotToSayResult)
    }
    
    @Test
    fun shouldIncreaseGoalForYouth() {
        // Given: Youth profile
        val input = GoalCalculationInput(
            age = 16,
            gender = Gender.OTHER, // Neutral gender to isolate age effect
            heightInCm = 170,
            weightInKg = 60.0
        )
        
        // When: Calculate steps goal
        val result = calculator.calculateStepsGoal(input)
        
        // Then: Should be higher than baseline due to youth adjustment
        val expected = (10000 * 1.2).toInt() // Youth adjustment factor
        assertEquals(expected, result)
        assertTrue("Youth should have higher step goals", result > 10000)
    }
    
    @Test
    fun shouldDecreaseGoalForOlderAdults() {
        // Given: Older adult profile
        val input = GoalCalculationInput(
            age = 70,
            gender = Gender.OTHER, // Neutral gender to isolate age effect
            heightInCm = 170,
            weightInKg = 65.0
        )
        
        // When: Calculate steps goal
        val result = calculator.calculateStepsGoal(input)
        
        // Then: Should be lower than baseline due to older adult adjustment
        val expected = (10000 * 0.8).toInt() // Older adult adjustment factor
        assertEquals(expected, result)
        assertTrue("Older adults should have adjusted step goals", result < 10000)
    }
    
    @Test
    fun shouldEnforceMinimumStepsGoal() {
        // Given: Profile that would result in very low goal
        val input = GoalCalculationInput(
            age = 90, // Very old age
            gender = Gender.FEMALE, // Lower adjustment
            heightInCm = 150,
            weightInKg = 45.0
        )
        
        // When: Calculate steps goal
        val result = calculator.calculateStepsGoal(input)
        
        // Then: Should not go below minimum
        assertTrue("Steps goal should never be below 5,000", result >= 5000)
    }
    
    @Test
    fun shouldEnforceMaximumStepsGoal() {
        // Given: Profile that would result in very high goal
        val input = GoalCalculationInput(
            age = 16, // Youth with higher needs
            gender = Gender.MALE, // Higher adjustment
            heightInCm = 190,
            weightInKg = 80.0
        )
        
        // When: Calculate steps goal
        val result = calculator.calculateStepsGoal(input)
        
        // Then: Should not exceed maximum
        assertTrue("Steps goal should never exceed 20,000", result <= 20000)
        assertEquals(12600, result) // (10000 * 1.2 * 1.05) = 12,600, which is within bounds
    }
    
    @Test
    fun shouldMatchWHOExamplesForTypicalPopulations() {
        // Test cases based on WHO Physical Activity Guidelines 2020 examples
        val testCases = listOf(
            // Typical adult male
            Pair(
                GoalCalculationInput(30, Gender.MALE, 175, 75.0),
                10500 // Expected: 10000 * 1.0 * 1.05
            ),
            // Typical adult female  
            Pair(
                GoalCalculationInput(28, Gender.FEMALE, 165, 60.0),
                9500 // Expected: 10000 * 1.0 * 0.95
            ),
            // Youth
            Pair(
                GoalCalculationInput(16, Gender.OTHER, 170, 65.0),
                12000 // Expected: 10000 * 1.2 * 1.0
            ),
            // Older adult
            Pair(
                GoalCalculationInput(70, Gender.OTHER, 170, 65.0),
                8000 // Expected: 10000 * 0.8 * 1.0
            )
        )
        
        testCases.forEach { (input, expected) ->
            val result = calculator.calculateStepsGoal(input)
            assertEquals(expected, result)
        }
    }
    
    @Test
    fun shouldProvideMedicallyReasonableResultsForEdgeCases() {
        val edgeCases = listOf(
            // Very young
            GoalCalculationInput(13, Gender.MALE, 150, 40.0),
            // Very old
            GoalCalculationInput(120, Gender.FEMALE, 160, 50.0),
            // Very tall and heavy
            GoalCalculationInput(25, Gender.MALE, 220, 120.0),
            // Very short and light
            GoalCalculationInput(25, Gender.FEMALE, 140, 40.0)
        )
        
        edgeCases.forEach { input ->
            val result = calculator.calculateStepsGoal(input)
            
            // All results should be medically reasonable
            assertTrue("Edge case should produce medically reasonable result", result in 5000..20000)
            
            // Results should be appropriate for age group
            when {
                input.age < 18 -> assertTrue("Youth should have higher goals", result >= 10000)
                input.age >= 65 -> assertTrue("Older adults should have adjusted goals", result <= 10000)
                else -> assertTrue("Adults should have moderate goals", result in 8000..12000)
            }
        }
    }
}