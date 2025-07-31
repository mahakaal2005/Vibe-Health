package com.vibehealth.android.domain.goals

import com.vibehealth.android.domain.user.Gender
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for CaloriesGoalCalculator.
 * 
 * Tests verify Harris-Benedict Revised (1984) and Mifflin-St Jeor equation implementations,
 * activity level calculations, boundary conditions, and medical safety requirements.
 */
class CaloriesGoalCalculatorTest {
    
    private lateinit var calculator: CaloriesGoalCalculator
    
    @Before
    fun setUp() {
        calculator = CaloriesGoalCalculator()
    }
    
    @Test
    fun shouldCalculateCorrectCaloriesForTypicalAdultMale() {
        // Given: Typical adult male profile
        val input = GoalCalculationInput(
            age = 30,
            gender = Gender.MALE,
            heightInCm = 175,
            weightInKg = 75.0,
            activityLevel = ActivityLevel.LIGHT
        )
        
        // When: Calculate calories goal
        val result = calculator.calculateCaloriesGoal(input)
        
        // Then: Should match Harris-Benedict calculation
        // BMR = 88.362 + (13.397 × 75) + (4.799 × 175) - (5.677 × 30)
        // BMR = 88.362 + 1004.775 + 839.825 - 170.31 = 1762.652
        // TDEE = 1762.652 × 1.375 = 2423.647 ≈ 2424
        val expectedBMR = 88.362 + (13.397 * 75.0) + (4.799 * 175) - (5.677 * 30)
        val expectedTDEE = (expectedBMR * 1.375).toInt()
        
        assertEquals(expectedTDEE, result)
        assertTrue("Result should be within reasonable range for adult male", result in 2000..2800)
    }
    
    @Test
    fun shouldCalculateCorrectCaloriesForTypicalAdultFemale() {
        // Given: Typical adult female profile
        val input = GoalCalculationInput(
            age = 28,
            gender = Gender.FEMALE,
            heightInCm = 165,
            weightInKg = 60.0,
            activityLevel = ActivityLevel.LIGHT
        )
        
        // When: Calculate calories goal
        val result = calculator.calculateCaloriesGoal(input)
        
        // Then: Should match Harris-Benedict calculation for females
        // BMR = 447.593 + (9.247 × 60) + (3.098 × 165) - (4.330 × 28)
        // BMR = 447.593 + 554.82 + 511.17 - 121.24 = 1392.343
        // TDEE = 1392.343 × 1.375 = 1914.472 ≈ 1914
        val expectedBMR = 447.593 + (9.247 * 60.0) + (3.098 * 165) - (4.330 * 28)
        val expectedTDEE = (expectedBMR * 1.375).toInt()
        
        assertEquals(expectedTDEE, result)
        assertTrue("Result should be within reasonable range for adult female", result in 1600..2200)
    }
    
    @Test
    fun shouldUseMifflinStJeorForInclusiveGenders() {
        val baseInput = GoalCalculationInput(
            age = 25,
            gender = Gender.OTHER,
            heightInCm = 170,
            weightInKg = 65.0,
            activityLevel = ActivityLevel.LIGHT
        )
        
        // Test OTHER gender
        val otherResult = calculator.calculateCaloriesGoal(baseInput.copy(gender = Gender.OTHER))
        
        // Test PREFER_NOT_TO_SAY gender
        val preferNotToSayResult = calculator.calculateCaloriesGoal(baseInput.copy(gender = Gender.PREFER_NOT_TO_SAY))
        
        // Both should use Mifflin-St Jeor equation and produce same result
        assertEquals(otherResult, preferNotToSayResult)
        
        // Verify Mifflin-St Jeor calculation
        // BMR = (10 × 65) + (6.25 × 170) - (5 × 25) + 5
        // BMR = 650 + 1062.5 - 125 + 5 = 1592.5
        // TDEE = 1592.5 × 1.375 = 2189.9375 ≈ 2190
        val expectedBMR = (10.0 * 65.0) + (6.25 * 170) - (5.0 * 25) + 5.0
        val expectedTDEE = (expectedBMR * 1.375).toInt()
        
        assertEquals(expectedTDEE, otherResult)
        assertTrue("Result should be within reasonable range", otherResult in 1800..2400)
    }
    
    @Test
    fun shouldAdjustForDifferentActivityLevels() {
        val baseInput = GoalCalculationInput(
            age = 30,
            gender = Gender.MALE,
            heightInCm = 175,
            weightInKg = 75.0,
            activityLevel = ActivityLevel.SEDENTARY // Will be overridden
        )
        
        // Test all activity levels
        val sedentaryResult = calculator.calculateCaloriesGoal(baseInput.copy(activityLevel = ActivityLevel.SEDENTARY))
        val lightResult = calculator.calculateCaloriesGoal(baseInput.copy(activityLevel = ActivityLevel.LIGHT))
        val moderateResult = calculator.calculateCaloriesGoal(baseInput.copy(activityLevel = ActivityLevel.MODERATE))
        val activeResult = calculator.calculateCaloriesGoal(baseInput.copy(activityLevel = ActivityLevel.ACTIVE))
        val veryActiveResult = calculator.calculateCaloriesGoal(baseInput.copy(activityLevel = ActivityLevel.VERY_ACTIVE))
        
        // Results should increase with activity level
        assertTrue("Sedentary < Light", sedentaryResult < lightResult)
        assertTrue("Light < Moderate", lightResult < moderateResult)
        assertTrue("Moderate < Active", moderateResult < activeResult)
        assertTrue("Active < Very Active", activeResult < veryActiveResult)
        
        // Verify specific calculations
        val baseBMR = 88.362 + (13.397 * 75.0) + (4.799 * 175) - (5.677 * 30)
        assertEquals((baseBMR * 1.2).toInt(), sedentaryResult)
        assertEquals((baseBMR * 1.375).toInt(), lightResult)
        assertEquals((baseBMR * 1.55).toInt(), moderateResult)
        assertEquals((baseBMR * 1.725).toInt(), activeResult)
        assertEquals((baseBMR * 1.9).toInt(), veryActiveResult)
    }
    
    @Test
    fun shouldEnforceMinimumCaloriesGoal() {
        // Given: Profile that would result in very low calorie goal
        val input = GoalCalculationInput(
            age = 80,
            gender = Gender.FEMALE,
            heightInCm = 140,
            weightInKg = 35.0,
            activityLevel = ActivityLevel.SEDENTARY
        )
        
        // When: Calculate calories goal
        val result = calculator.calculateCaloriesGoal(input)
        
        // Then: Should not go below minimum
        assertTrue("Calories goal should never be below 1,200", result >= 1200)
    }
    
    @Test
    fun shouldEnforceMaximumCaloriesGoal() {
        // Given: Profile that would result in very high calorie goal
        val input = GoalCalculationInput(
            age = 20,
            gender = Gender.MALE,
            heightInCm = 200,
            weightInKg = 120.0,
            activityLevel = ActivityLevel.VERY_ACTIVE
        )
        
        // When: Calculate calories goal
        val result = calculator.calculateCaloriesGoal(input)
        
        // Then: Should not exceed maximum
        assertTrue("Calories goal should never exceed 4,000", result <= 4000)
    }
    
    @Test
    fun shouldHandleEdgeCaseAges() {
        val edgeCases = listOf(13, 25, 45, 65, 85, 120)
        
        edgeCases.forEach { age ->
            val input = GoalCalculationInput(
                age = age,
                gender = Gender.OTHER,
                heightInCm = 170,
                weightInKg = 65.0,
                activityLevel = ActivityLevel.LIGHT
            )
            
            val result = calculator.calculateCaloriesGoal(input)
            
            assertTrue("Age $age should produce valid calorie goal", result in 1200..4000)
            
            // Older adults should generally have lower calorie needs
            when {
                age >= 65 -> assertTrue("Older adults should have moderate calorie needs", result in 1400..2200)
                age < 25 -> assertTrue("Younger adults should have higher calorie needs", result in 1800..3000)
                else -> assertTrue("Middle-aged adults should have moderate calorie needs", result in 1600..2800)
            }
        }
    }
    
    @Test
    fun shouldProvideAccurateCalculationBreakdown() {
        // Given: Typical user profile
        val input = GoalCalculationInput(
            age = 30,
            gender = Gender.MALE,
            heightInCm = 175,
            weightInKg = 75.0,
            activityLevel = ActivityLevel.LIGHT
        )
        
        // When: Get calculation breakdown
        val breakdown = calculator.getCalculationBreakdown(input)
        
        // Then: Should provide accurate breakdown
        val expectedBMR = 88.362 + (13.397 * 75.0) + (4.799 * 175) - (5.677 * 30)
        val expectedTDEE = expectedBMR * 1.375
        
        assertEquals(expectedBMR, breakdown.bmr, 0.01)
        assertEquals(ActivityLevel.LIGHT, breakdown.activityLevel)
        assertEquals(1.375, breakdown.activityFactor)
        assertEquals(expectedTDEE, breakdown.tdee, 0.01)
        assertEquals(expectedTDEE.toInt(), breakdown.finalGoal)
        assertEquals("Harris-Benedict Revised (1984)", breakdown.equation)
        assertEquals(false, breakdown.boundsApplied) // No bounds needed for typical case
    }
    
    @Test
    fun shouldIndicateBoundsApplicationInBreakdown() {
        // Given: Profile that requires bounds adjustment
        val input = GoalCalculationInput(
            age = 80,
            gender = Gender.FEMALE,
            heightInCm = 140,
            weightInKg = 35.0,
            activityLevel = ActivityLevel.SEDENTARY
        )
        
        // When: Get calculation breakdown
        val breakdown = calculator.getCalculationBreakdown(input)
        
        // Then: Should indicate bounds were applied
        assertTrue("Bounds should be applied for extreme case", breakdown.boundsApplied)
        assertEquals(1200, breakdown.finalGoal) // Minimum enforced
        assertTrue("TDEE should be less than final goal", breakdown.tdee < breakdown.finalGoal)
    }
    
    @Test
    fun shouldUseCorrectEquationForEachGender() {
        val testCases = listOf(
            Triple(Gender.MALE, "Harris-Benedict Revised (1984)", true),
            Triple(Gender.FEMALE, "Harris-Benedict Revised (1984)", true),
            Triple(Gender.OTHER, "Mifflin-St Jeor (1990)", false),
            Triple(Gender.PREFER_NOT_TO_SAY, "Mifflin-St Jeor (1990)", false)
        )
        
        testCases.forEach { (gender, expectedEquation, isHarrisBenedict) ->
            val input = GoalCalculationInput(
                age = 30,
                gender = gender,
                heightInCm = 170,
                weightInKg = 65.0,
                activityLevel = ActivityLevel.LIGHT
            )
            
            val breakdown = calculator.getCalculationBreakdown(input)
            
            assertEquals(expectedEquation, breakdown.equation)
            
            // Verify calculation method by checking BMR ranges
            if (isHarrisBenedict) {
                // Harris-Benedict typically produces different results than Mifflin-St Jeor
                when (gender) {
                    Gender.MALE -> assertTrue("Male BMR should be in expected range", breakdown.bmr in 1600..1900)
                    Gender.FEMALE -> assertTrue("Female BMR should be in expected range", breakdown.bmr in 1300..1600)
                    else -> { /* Not applicable */ }
                }
            } else {
                // Mifflin-St Jeor for gender-neutral calculation
                assertTrue("Gender-neutral BMR should be in expected range", breakdown.bmr in 1400..1700)
            }
        }
    }
    
    @Test
    fun shouldProvideReadableExplanation() {
        // Given: Typical user profile
        val input = GoalCalculationInput(
            age = 25,
            gender = Gender.FEMALE,
            heightInCm = 165,
            weightInKg = 60.0,
            activityLevel = ActivityLevel.MODERATE
        )
        
        // When: Get explanation
        val breakdown = calculator.getCalculationBreakdown(input)
        val explanation = breakdown.getExplanation()
        
        // Then: Should contain key information
        assertTrue("Should mention equation used", explanation.contains("Harris-Benedict Revised (1984)"))
        assertTrue("Should mention activity level", explanation.contains("Moderate exercise"))
        assertTrue("Should mention BMR", explanation.contains("BMR"))
        assertTrue("Should mention TDEE", explanation.contains("TDEE"))
        assertTrue("Should mention final goal", explanation.contains("Final Goal"))
        assertTrue("Should contain actual numbers", explanation.contains(breakdown.finalGoal.toString()))
    }
    
    @Test
    fun shouldHandleExtremeButValidInputs() {
        val extremeCases = listOf(
            // Very tall, heavy, young, active person
            GoalCalculationInput(13, Gender.MALE, 220, 120.0, ActivityLevel.VERY_ACTIVE),
            // Very short, light, old, sedentary person
            GoalCalculationInput(120, Gender.FEMALE, 140, 35.0, ActivityLevel.SEDENTARY),
            // Average person with extreme activity
            GoalCalculationInput(30, Gender.OTHER, 170, 70.0, ActivityLevel.VERY_ACTIVE),
            // Average person with minimal activity
            GoalCalculationInput(30, Gender.OTHER, 170, 70.0, ActivityLevel.SEDENTARY)
        )
        
        extremeCases.forEach { input ->
            val result = calculator.calculateCaloriesGoal(input)
            
            // All results should be within medical safety bounds
            assertTrue("Extreme case should produce safe calorie goal", result in 1200..4000)
            
            // Results should be medically reasonable for the profile
            when {
                input.age < 18 && input.activityLevel == ActivityLevel.VERY_ACTIVE -> {
                    assertTrue("Active youth should have high calorie needs", result >= 2000)
                }
                input.age >= 80 && input.activityLevel == ActivityLevel.SEDENTARY -> {
                    assertTrue("Sedentary elderly should have lower calorie needs", result <= 2000)
                }
                input.activityLevel == ActivityLevel.VERY_ACTIVE -> {
                    assertTrue("Very active individuals should have high calorie needs", result >= 2200)
                }
                input.activityLevel == ActivityLevel.SEDENTARY -> {
                    assertTrue("Sedentary individuals should have moderate calorie needs", result <= 2500)
                }
            }
        }
    }
}