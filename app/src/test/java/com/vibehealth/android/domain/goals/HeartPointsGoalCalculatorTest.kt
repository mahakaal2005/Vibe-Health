package com.vibehealth.android.domain.goals

import com.vibehealth.android.domain.user.Gender
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for HeartPointsGoalCalculator.
 * 
 * Tests verify WHO Physical Activity Guidelines implementation, METs conversion,
 * age and activity level adjustments, and Google Fit heart points standard compliance.
 */
class HeartPointsGoalCalculatorTest {
    
    private lateinit var calculator: HeartPointsGoalCalculator
    
    @Before
    fun setUp() {
        calculator = HeartPointsGoalCalculator()
    }
    
    @Test
    fun shouldCalculateCorrectHeartPointsForTypicalAdult() {
        // Given: Typical adult profile
        val input = GoalCalculationInput(
            age = 30,
            gender = Gender.OTHER, // Use neutral gender to isolate age/activity effects
            heightInCm = 170,
            weightInKg = 65.0,
            activityLevel = ActivityLevel.MODERATE
        )
        
        // When: Calculate heart points goal
        val result = calculator.calculateHeartPointsGoal(input)
        
        // Then: Should match WHO baseline calculation
        // WHO: 150 min/week ÷ 7 days = ~21.4 min/day
        // Base heart points: 21.4 × 1 point/min = 21.4 points
        // Age adjustment (adult): 1.0x
        // Activity adjustment (moderate): 1.0x
        // Final: 21.4 ≈ 21 points
        val expectedDailyMinutes = 150.0 / 7.0
        val expectedHeartPoints = (expectedDailyMinutes * 1.0 * 1.0).toInt()
        
        assertEquals(expectedHeartPoints, result)
        assertTrue("Result should be close to WHO baseline", result in 20..22)
    }
    
    @Test
    fun shouldIncreaseGoalForYouth() {
        // Given: Youth profile
        val input = GoalCalculationInput(
            age = 16,
            gender = Gender.OTHER,
            heightInCm = 170,
            weightInKg = 60.0,
            activityLevel = ActivityLevel.MODERATE
        )
        
        // When: Calculate heart points goal
        val result = calculator.calculateHeartPointsGoal(input)
        
        // Then: Should be higher than adult baseline due to youth adjustment
        // Base: ~21.4 points
        // Youth adjustment: 1.2x
        // Expected: 21.4 × 1.2 = 25.68 ≈ 26 points
        val expectedDailyMinutes = 150.0 / 7.0
        val expectedHeartPoints = (expectedDailyMinutes * 1.2 * 1.0).toInt()
        
        assertEquals(expectedHeartPoints, result)
        assertTrue("Youth should have higher heart points goals", result > 21)
    }
    
    @Test
    fun shouldDecreaseGoalForOlderAdults() {
        // Given: Older adult profile
        val input = GoalCalculationInput(
            age = 70,
            gender = Gender.OTHER,
            heightInCm = 170,
            weightInKg = 65.0,
            activityLevel = ActivityLevel.MODERATE
        )
        
        // When: Calculate heart points goal
        val result = calculator.calculateHeartPointsGoal(input)
        
        // Then: Should be lower than adult baseline due to older adult adjustment
        // Base: ~21.4 points
        // Older adult adjustment: 0.8x
        // Expected: 21.4 × 0.8 = 17.12 ≈ 17 points
        val expectedDailyMinutes = 150.0 / 7.0
        val expectedHeartPoints = (expectedDailyMinutes * 0.8 * 1.0).toInt()
        
        assertEquals(expectedHeartPoints, result)
        assertTrue("Older adults should have adjusted heart points goals", result < 21)
    }
    
    @Test
    fun shouldAdjustForDifferentActivityLevels() {
        val baseInput = GoalCalculationInput(
            age = 30, // Adult baseline
            gender = Gender.OTHER,
            heightInCm = 170,
            weightInKg = 65.0,
            activityLevel = ActivityLevel.SEDENTARY // Will be overridden
        )
        
        // Test all activity levels
        val sedentaryResult = calculator.calculateHeartPointsGoal(baseInput.copy(activityLevel = ActivityLevel.SEDENTARY))
        val lightResult = calculator.calculateHeartPointsGoal(baseInput.copy(activityLevel = ActivityLevel.LIGHT))
        val moderateResult = calculator.calculateHeartPointsGoal(baseInput.copy(activityLevel = ActivityLevel.MODERATE))
        val activeResult = calculator.calculateHeartPointsGoal(baseInput.copy(activityLevel = ActivityLevel.ACTIVE))
        val veryActiveResult = calculator.calculateHeartPointsGoal(baseInput.copy(activityLevel = ActivityLevel.VERY_ACTIVE))
        
        // Results should increase with activity level
        assertTrue("Sedentary < Light", sedentaryResult < lightResult)
        assertTrue("Light < Moderate", lightResult < moderateResult)
        assertTrue("Moderate < Active", moderateResult < activeResult)
        assertTrue("Active < Very Active", activeResult < veryActiveResult)
        
        // Verify specific calculations
        val baseDailyMinutes = 150.0 / 7.0
        assertEquals((baseDailyMinutes * 1.0 * 0.9).toInt(), sedentaryResult)
        assertEquals((baseDailyMinutes * 1.0 * 0.95).toInt(), lightResult)
        assertEquals((baseDailyMinutes * 1.0 * 1.0).toInt(), moderateResult)
        assertEquals((baseDailyMinutes * 1.0 * 1.1).toInt(), activeResult)
        assertEquals((baseDailyMinutes * 1.0 * 1.15).toInt(), veryActiveResult)
    }
    
    @Test
    fun shouldEnforceMinimumHeartPointsGoal() {
        // Given: Profile that would result in very low heart points goal
        val input = GoalCalculationInput(
            age = 90,
            gender = Gender.OTHER,
            heightInCm = 150,
            weightInKg = 45.0,
            activityLevel = ActivityLevel.SEDENTARY
        )
        
        // When: Calculate heart points goal
        val result = calculator.calculateHeartPointsGoal(input)
        
        // Then: Should not go below minimum
        assertTrue("Heart points goal should never be below 15", result >= 15)
    }
    
    @Test
    fun shouldEnforceMaximumHeartPointsGoal() {
        // Given: Profile that would result in very high heart points goal
        val input = GoalCalculationInput(
            age = 16, // Youth with higher needs
            gender = Gender.OTHER,
            heightInCm = 180,
            weightInKg = 70.0,
            activityLevel = ActivityLevel.VERY_ACTIVE
        )
        
        // When: Calculate heart points goal
        val result = calculator.calculateHeartPointsGoal(input)
        
        // Then: Should not exceed maximum
        assertTrue("Heart points goal should never exceed 50", result <= 50)
        
        // Verify calculation would exceed maximum without bounds
        val baseDailyMinutes = 150.0 / 7.0
        val unboundedResult = (baseDailyMinutes * 1.2 * 1.15).toInt()
        assertEquals(29, unboundedResult) // Should be within bounds for this case
    }
    
    @Test
    fun shouldHandleAllAgeGroups() {
        val ageTestCases = listOf(
            Triple(13, "Youth", 25..35),
            Triple(25, "Young Adult", 18..25),
            Triple(45, "Middle-aged Adult", 18..25),
            Triple(65, "Older Adult", 15..20),
            Triple(85, "Elderly", 15..20)
        )
        
        ageTestCases.forEach { (age, description, expectedRange) ->
            val input = GoalCalculationInput(
                age = age,
                gender = Gender.OTHER,
                heightInCm = 170,
                weightInKg = 65.0,
                activityLevel = ActivityLevel.MODERATE
            )
            
            val result = calculator.calculateHeartPointsGoal(input)
            
            assertTrue("$description (age $age) should have appropriate heart points goal", result in expectedRange)
        }
    }
    
    @Test
    fun shouldProvideAccurateCalculationBreakdown() {
        // Given: Typical user profile
        val input = GoalCalculationInput(
            age = 30,
            gender = Gender.OTHER,
            heightInCm = 170,
            weightInKg = 65.0,
            activityLevel = ActivityLevel.LIGHT
        )
        
        // When: Get calculation breakdown
        val breakdown = calculator.getCalculationBreakdown(input)
        
        // Then: Should provide accurate breakdown
        val expectedDailyMinutes = 150.0 / 7.0
        val expectedBaseHeartPoints = expectedDailyMinutes * 1.0
        val expectedAdjustedGoal = expectedBaseHeartPoints * 1.0 * 0.95 // Adult age × Light activity
        
        assertEquals(150, breakdown.whoWeeklyMinutes)
        assertEquals(expectedDailyMinutes, breakdown.dailyModerateMinutes, 0.01)
        assertEquals(expectedBaseHeartPoints, breakdown.baseHeartPoints, 0.01)
        assertEquals(1.0, breakdown.ageAdjustment)
        assertEquals(0.95, breakdown.activityAdjustment)
        assertEquals(expectedAdjustedGoal, breakdown.adjustedGoal, 0.01)
        assertEquals(expectedAdjustedGoal.toInt(), breakdown.finalGoal)
        assertEquals(false, breakdown.boundsApplied) // No bounds needed for typical case
    }
    
    @Test
    fun shouldIndicateBoundsApplicationInBreakdown() {
        // Given: Profile that requires bounds adjustment (minimum)
        val input = GoalCalculationInput(
            age = 90,
            gender = Gender.OTHER,
            heightInCm = 150,
            weightInKg = 45.0,
            activityLevel = ActivityLevel.SEDENTARY
        )
        
        // When: Get calculation breakdown
        val breakdown = calculator.getCalculationBreakdown(input)
        
        // Then: Should indicate bounds were applied
        assertTrue("Bounds should be applied for extreme case", breakdown.boundsApplied)
        assertEquals(15, breakdown.finalGoal) // Minimum enforced
        assertTrue("Adjusted goal should be less than final goal", breakdown.adjustedGoal < breakdown.finalGoal)
    }
    
    @Test
    fun shouldConvertHeartPointsToMinutesCorrectly() {
        // Test conversion from heart points to activity minutes
        val testCases = listOf(
            Pair(15, 15), // 15 heart points = 15 minutes moderate activity
            Pair(21, 21), // WHO baseline
            Pair(30, 30), // Higher goal
            Pair(50, 50)  // Maximum goal
        )
        
        testCases.forEach { (heartPoints, expectedMinutes) ->
            val result = calculator.convertHeartPointsToMinutes(heartPoints)
            assertEquals(expectedMinutes, result)
        }
    }
    
    @Test
    fun shouldCalculateWeeklyEquivalentCorrectly() {
        // Test conversion from daily heart points to weekly minutes
        val testCases = listOf(
            Pair(15, 105), // 15 points/day × 7 days = 105 minutes/week
            Pair(21, 147), // Close to WHO 150 min/week
            Pair(30, 210), // Higher weekly equivalent
            Pair(50, 350)  // Maximum weekly equivalent
        )
        
        testCases.forEach { (dailyHeartPoints, expectedWeeklyMinutes) ->
            val result = calculator.getWeeklyEquivalent(dailyHeartPoints)
            assertEquals(expectedWeeklyMinutes, result)
        }
    }
    
    @Test
    fun shouldProvideReadableExplanation() {
        // Given: Typical user profile
        val input = GoalCalculationInput(
            age = 25,
            gender = Gender.OTHER,
            heightInCm = 170,
            weightInKg = 65.0,
            activityLevel = ActivityLevel.ACTIVE
        )
        
        // When: Get explanation
        val breakdown = calculator.getCalculationBreakdown(input)
        val explanation = breakdown.getExplanation()
        
        // Then: Should contain key information
        assertTrue("Should mention WHO baseline", explanation.contains("WHO Baseline"))
        assertTrue("Should mention daily equivalent", explanation.contains("Daily Equivalent"))
        assertTrue("Should mention heart points", explanation.contains("Heart Points"))
        assertTrue("Should mention age adjustment", explanation.contains("Age Adjustment"))
        assertTrue("Should mention activity adjustment", explanation.contains("Activity Adjustment"))
        assertTrue("Should mention final goal", explanation.contains("Final Goal"))
        assertTrue("Should mention WHO guidelines", explanation.contains("WHO Physical Activity Guidelines"))
        assertTrue("Should contain actual numbers", explanation.contains(breakdown.finalGoal.toString()))
    }
    
    @Test
    fun shouldAlignWithWHOGuidelines() {
        // Test that our calculations align with WHO recommendations
        val whoTestCases = listOf(
            // Standard adult should get close to WHO baseline
            Triple(
                GoalCalculationInput(30, Gender.OTHER, 170, 65.0, ActivityLevel.MODERATE),
                21, // Expected heart points (150 min/week ÷ 7 ≈ 21.4)
                "Standard adult with moderate activity"
            ),
            // Youth should exceed adult baseline
            Triple(
                GoalCalculationInput(16, Gender.OTHER, 170, 60.0, ActivityLevel.MODERATE),
                25, // Expected higher goal for youth
                "Youth with moderate activity"
            ),
            // Older adult should be below baseline but still meaningful
            Triple(
                GoalCalculationInput(70, Gender.OTHER, 170, 65.0, ActivityLevel.LIGHT),
                16, // Expected lower but achievable goal
                "Older adult with light activity"
            )
        )
        
        whoTestCases.forEach { (input, expectedApprox, description) ->
            val result = calculator.calculateHeartPointsGoal(input)
            val weeklyEquivalent = calculator.getWeeklyEquivalent(result)
            
            // Result should be close to expected
            assertTrue("$description should have appropriate goal", kotlin.math.abs(result - expectedApprox) <= 2)
            
            // Weekly equivalent should be reasonable compared to WHO guidelines
            when {
                input.age < 18 -> assertTrue("Youth weekly equivalent should exceed WHO baseline", weeklyEquivalent >= 150)
                input.age >= 65 -> assertTrue("Older adult weekly equivalent should be achievable", weeklyEquivalent in 100..180)
                else -> assertTrue("Adult weekly equivalent should align with WHO", weeklyEquivalent in 130..200)
            }
        }
    }
    
    @Test
    fun shouldHandleExtremeButValidInputs() {
        val extremeCases = listOf(
            // Very young, very active
            GoalCalculationInput(13, Gender.OTHER, 160, 50.0, ActivityLevel.VERY_ACTIVE),
            // Very old, sedentary
            GoalCalculationInput(120, Gender.OTHER, 150, 40.0, ActivityLevel.SEDENTARY),
            // Average age, extreme activity levels
            GoalCalculationInput(30, Gender.OTHER, 170, 70.0, ActivityLevel.VERY_ACTIVE),
            GoalCalculationInput(30, Gender.OTHER, 170, 70.0, ActivityLevel.SEDENTARY)
        )
        
        extremeCases.forEach { input ->
            val result = calculator.calculateHeartPointsGoal(input)
            
            // All results should be within safety bounds
            assertTrue("Extreme case should produce safe heart points goal", result in 15..50)
            
            // Results should be medically reasonable for the profile
            when {
                input.age < 18 && input.activityLevel == ActivityLevel.VERY_ACTIVE -> {
                    assertTrue("Very active youth should have high heart points goals", result >= 25)
                }
                input.age >= 80 && input.activityLevel == ActivityLevel.SEDENTARY -> {
                    assertTrue("Sedentary elderly should have conservative goals", result <= 20)
                }
                input.activityLevel == ActivityLevel.VERY_ACTIVE -> {
                    assertTrue("Very active individuals should have elevated goals", result >= 22)
                }
                input.activityLevel == ActivityLevel.SEDENTARY -> {
                    assertTrue("Sedentary individuals should have conservative goals", result <= 25)
                }
            }
        }
    }
}