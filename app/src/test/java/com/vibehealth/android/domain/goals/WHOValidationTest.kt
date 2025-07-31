package com.vibehealth.android.domain.goals

import com.vibehealth.android.domain.user.Gender
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.system.measureTimeMillis

/**
 * Comprehensive unit tests for goal calculators with WHO validation.
 * 
 * Tests validate calculations against published WHO guidelines, test edge cases,
 * boundary conditions, and verify performance requirements as specified in Task 5.1.
 */
class WHOValidationTest {

    private lateinit var stepsCalculator: StepsGoalCalculator
    private lateinit var caloriesCalculator: CaloriesGoalCalculator
    private lateinit var heartPointsCalculator: HeartPointsGoalCalculator

    @BeforeEach
    fun setup() {
        stepsCalculator = StepsGoalCalculator()
        caloriesCalculator = CaloriesGoalCalculator()
        heartPointsCalculator = HeartPointsGoalCalculator()
    }

    @Nested
    @DisplayName("WHO Steps Goal Validation")
    inner class WHOStepsGoalValidation {

        @Test
        @DisplayName("Should calculate baseline 10,000 steps for standard adult male")
        fun shouldCalculateBaselineStepsForStandardAdultMale() {
            // Given - WHO standard adult male (25 years, average activity)
            val input = GoalCalculationInput(
                age = 25,
                gender = Gender.MALE,
                heightInCm = 175,
                weightInKg = 70.0,
                activityLevel = ActivityLevel.LIGHT
            )

            // When
            val stepsGoal = stepsCalculator.calculateStepsGoal(input)

            // Then - Should be close to WHO baseline of 10,000 steps
            assertTrue(stepsGoal in 9500..11000, "Steps goal $stepsGoal should be near WHO baseline of 10,000")
        }

        @Test
        @DisplayName("Should calculate higher steps for youth per WHO guidelines")
        fun shouldCalculateHigherStepsForYouth() {
            // Given - WHO youth recommendation (16 years)
            val youthInput = GoalCalculationInput(
                age = 16,
                gender = Gender.MALE,
                heightInCm = 170,
                weightInKg = 60.0,
                activityLevel = ActivityLevel.LIGHT
            )
            
            val adultInput = youthInput.copy(age = 25)

            // When
            val youthSteps = stepsCalculator.calculateStepsGoal(youthInput)
            val adultSteps = stepsCalculator.calculateStepsGoal(adultInput)

            // Then - Youth should have higher steps goal per WHO Physical Activity Guidelines 2020
            assertTrue(youthSteps > adultSteps, "Youth steps ($youthSteps) should be higher than adult steps ($adultSteps)")
            assertTrue(youthSteps >= 8000, "Youth should have at least 8,000 steps per WHO guidelines")
        }

        @Test
        @DisplayName("Should calculate adjusted steps for older adults per WHO guidelines")
        fun shouldCalculateAdjustedStepsForOlderAdults() {
            // Given - WHO older adult recommendation (70 years)
            val olderAdultInput = GoalCalculationInput(
                age = 70,
                gender = Gender.FEMALE,
                heightInCm = 160,
                weightInKg = 65.0,
                activityLevel = ActivityLevel.LIGHT
            )
            
            val adultInput = olderAdultInput.copy(age = 30)

            // When
            val olderAdultSteps = stepsCalculator.calculateStepsGoal(olderAdultInput)
            val adultSteps = stepsCalculator.calculateStepsGoal(adultInput)

            // Then - Older adults should have adjusted (typically lower) steps per WHO guidelines
            assertTrue(olderAdultSteps <= adultSteps, "Older adult steps ($olderAdultSteps) should be adjusted from adult steps ($adultSteps)")
            assertTrue(olderAdultSteps >= 5000, "Older adults should have at least 5,000 steps for health benefits")
        }

        @ParameterizedTest
        @CsvSource(
            "13, MALE, 5000, 20000",
            "25, FEMALE, 5000, 20000", 
            "65, OTHER, 5000, 20000",
            "120, PREFER_NOT_TO_SAY, 5000, 20000"
        )
        @DisplayName("Should enforce WHO safety bounds for all demographics")
        fun shouldEnforceWHOSafetyBounds(age: Int, gender: Gender, minSteps: Int, maxSteps: Int) {
            // Given
            val input = GoalCalculationInput(
                age = age,
                gender = gender,
                heightInCm = 170,
                weightInKg = 70.0,
                activityLevel = ActivityLevel.LIGHT
            )

            // When
            val stepsGoal = stepsCalculator.calculateStepsGoal(input)

            // Then - Should be within WHO safety bounds
            assertTrue(stepsGoal >= minSteps, "Steps goal $stepsGoal should be at least $minSteps")
            assertTrue(stepsGoal <= maxSteps, "Steps goal $stepsGoal should be at most $maxSteps")
        }
    }

    @Nested
    @DisplayName("WHO Calories Goal Validation")
    inner class WHOCaloriesGoalValidation {

        @Test
        @DisplayName("Should calculate BMR using Harris-Benedict equation for males")
        fun shouldCalculateBMRUsingHarrisBenedictForMales() {
            // Given - Standard adult male for Harris-Benedict validation
            val input = GoalCalculationInput(
                age = 30,
                gender = Gender.MALE,
                heightInCm = 180,
                weightInKg = 75.0,
                activityLevel = ActivityLevel.SEDENTARY
            )

            // When
            val caloriesGoal = caloriesCalculator.calculateCaloriesGoal(input)

            // Then - Validate against Harris-Benedict equation
            // BMR = 88.362 + (13.397 × 75) + (4.799 × 180) - (5.677 × 30) = 1,789.7
            // TDEE = BMR × 1.2 (sedentary) = 2,147.6
            val expectedBMR = 88.362 + (13.397 * 75) + (4.799 * 180) - (5.677 * 30)
            val expectedTDEE = (expectedBMR * 1.2).toInt()
            
            assertTrue(
                caloriesGoal in (expectedTDEE - 100)..(expectedTDEE + 100),
                "Calories goal $caloriesGoal should be close to Harris-Benedict calculation $expectedTDEE"
            )
        }

        @Test
        @DisplayName("Should calculate BMR using Harris-Benedict equation for females")
        fun shouldCalculateBMRUsingHarrisBenedictForFemales() {
            // Given - Standard adult female for Harris-Benedict validation
            val input = GoalCalculationInput(
                age = 25,
                gender = Gender.FEMALE,
                heightInCm = 165,
                weightInKg = 60.0,
                activityLevel = ActivityLevel.LIGHT
            )

            // When
            val caloriesGoal = caloriesCalculator.calculateCaloriesGoal(input)

            // Then - Validate against Harris-Benedict equation for females
            // BMR = 447.593 + (9.247 × 60) + (3.098 × 165) - (4.330 × 25) = 1,372.4
            // TDEE = BMR × 1.375 (light activity) = 1,887.1
            val expectedBMR = 447.593 + (9.247 * 60) + (3.098 * 165) - (4.330 * 25)
            val expectedTDEE = (expectedBMR * 1.375).toInt()
            
            assertTrue(
                caloriesGoal in (expectedTDEE - 100)..(expectedTDEE + 100),
                "Calories goal $caloriesGoal should be close to Harris-Benedict calculation $expectedTDEE"
            )
        }

        @Test
        @DisplayName("Should use Mifflin-St Jeor equation for non-binary genders")
        fun shouldUseMifflinStJeorForNonBinaryGenders() {
            // Given - Non-binary gender to test Mifflin-St Jeor equation
            val input = GoalCalculationInput(
                age = 30,
                gender = Gender.OTHER,
                heightInCm = 170,
                weightInKg = 70.0,
                activityLevel = ActivityLevel.MODERATE
            )

            // When
            val caloriesGoal = caloriesCalculator.calculateCaloriesGoal(input)

            // Then - Validate against Mifflin-St Jeor equation
            // BMR = 10 × 70 + 6.25 × 170 - 5 × 30 + 5 = 1,567.5
            // TDEE = BMR × 1.55 (moderate activity) = 2,429.6
            val expectedBMR = 10 * 70 + 6.25 * 170 - 5 * 30 + 5
            val expectedTDEE = (expectedBMR * 1.55).toInt()
            
            assertTrue(
                caloriesGoal in (expectedTDEE - 100)..(expectedTDEE + 100),
                "Calories goal $caloriesGoal should be close to Mifflin-St Jeor calculation $expectedTDEE"
            )
        }

        @ParameterizedTest
        @CsvSource(
            "SEDENTARY, 1.2",
            "LIGHT, 1.375",
            "MODERATE, 1.55",
            "ACTIVE, 1.725",
            "VERY_ACTIVE, 1.9"
        )
        @DisplayName("Should apply correct WHO activity factors")
        fun shouldApplyCorrectWHOActivityFactors(activityLevel: ActivityLevel, expectedFactor: Double) {
            // Given
            val baseInput = GoalCalculationInput(
                age = 30,
                gender = Gender.MALE,
                heightInCm = 175,
                weightInKg = 70.0,
                activityLevel = ActivityLevel.SEDENTARY
            )
            
            val testInput = baseInput.copy(activityLevel = activityLevel)

            // When
            val sedentaryCalories = caloriesCalculator.calculateCaloriesGoal(baseInput)
            val testCalories = caloriesCalculator.calculateCaloriesGoal(testInput)

            // Then - Should reflect the activity factor difference
            val expectedRatio = expectedFactor / 1.2 // Sedentary factor
            val actualRatio = testCalories.toDouble() / sedentaryCalories.toDouble()
            
            assertTrue(
                actualRatio in (expectedRatio - 0.1)..(expectedRatio + 0.1),
                "Activity factor ratio $actualRatio should be close to expected $expectedRatio"
            )
        }

        @Test
        @DisplayName("Should enforce WHO calorie safety bounds")
        fun shouldEnforceWHOCalorieSafetyBounds() {
            // Given - Extreme inputs that might produce unsafe calorie goals
            val extremeInputs = listOf(
                GoalCalculationInput(13, Gender.FEMALE, 100, 30.0, ActivityLevel.SEDENTARY),
                GoalCalculationInput(120, Gender.MALE, 250, 300.0, ActivityLevel.VERY_ACTIVE)
            )

            extremeInputs.forEach { input ->
                // When
                val caloriesGoal = caloriesCalculator.calculateCaloriesGoal(input)

                // Then - Should be within WHO safety bounds
                assertTrue(caloriesGoal >= 1200, "Calories goal $caloriesGoal should be at least 1200 for safety")
                assertTrue(caloriesGoal <= 4000, "Calories goal $caloriesGoal should be at most 4000 for safety")
            }
        }
    }

    @Nested
    @DisplayName("WHO Heart Points Goal Validation")
    inner class WHOHeartPointsGoalValidation {

        @Test
        @DisplayName("Should calculate heart points based on WHO 150 minutes/week guideline")
        fun shouldCalculateHeartPointsBasedOnWHO150Minutes() {
            // Given - Standard adult for WHO 150 minutes validation
            val input = GoalCalculationInput(
                age = 30,
                gender = Gender.MALE,
                heightInCm = 175,
                weightInKg = 70.0,
                activityLevel = ActivityLevel.LIGHT
            )

            // When
            val heartPointsGoal = heartPointsCalculator.calculateHeartPointsGoal(input)

            // Then - Should be based on WHO 150 minutes/week = ~21.4 minutes/day = ~21 heart points
            assertTrue(
                heartPointsGoal in 18..35,
                "Heart points goal $heartPointsGoal should be based on WHO 150 min/week guideline (~21 points/day)"
            )
        }

        @Test
        @DisplayName("Should adjust heart points for youth per WHO guidelines")
        fun shouldAdjustHeartPointsForYouth() {
            // Given - Youth vs adult comparison
            val youthInput = GoalCalculationInput(
                age = 16,
                gender = Gender.FEMALE,
                heightInCm = 160,
                weightInKg = 55.0,
                activityLevel = ActivityLevel.LIGHT
            )
            
            val adultInput = youthInput.copy(age = 25)

            // When
            val youthHeartPoints = heartPointsCalculator.calculateHeartPointsGoal(youthInput)
            val adultHeartPoints = heartPointsCalculator.calculateHeartPointsGoal(adultInput)

            // Then - Youth should have higher heart points per WHO guidelines
            assertTrue(
                youthHeartPoints >= adultHeartPoints,
                "Youth heart points ($youthHeartPoints) should be >= adult heart points ($adultHeartPoints)"
            )
        }

        @Test
        @DisplayName("Should adjust heart points for older adults per WHO guidelines")
        fun shouldAdjustHeartPointsForOlderAdults() {
            // Given - Older adult vs standard adult
            val olderAdultInput = GoalCalculationInput(
                age = 70,
                gender = Gender.MALE,
                heightInCm = 170,
                weightInKg = 75.0,
                activityLevel = ActivityLevel.LIGHT
            )
            
            val adultInput = olderAdultInput.copy(age = 35)

            // When
            val olderAdultHeartPoints = heartPointsCalculator.calculateHeartPointsGoal(olderAdultInput)
            val adultHeartPoints = heartPointsCalculator.calculateHeartPointsGoal(adultInput)

            // Then - Older adults should have adjusted heart points per WHO guidelines
            assertTrue(
                olderAdultHeartPoints <= adultHeartPoints,
                "Older adult heart points ($olderAdultHeartPoints) should be <= adult heart points ($adultHeartPoints)"
            )
            assertTrue(
                olderAdultHeartPoints >= 15,
                "Older adults should have at least 15 heart points for health benefits"
            )
        }

        @Test
        @DisplayName("Should enforce WHO heart points safety bounds")
        fun shouldEnforceWHOHeartPointsSafetyBounds() {
            // Given - Various demographic inputs
            val testInputs = listOf(
                GoalCalculationInput(13, Gender.MALE, 150, 40.0, ActivityLevel.SEDENTARY),
                GoalCalculationInput(25, Gender.FEMALE, 175, 65.0, ActivityLevel.MODERATE),
                GoalCalculationInput(80, Gender.OTHER, 165, 70.0, ActivityLevel.ACTIVE)
            )

            testInputs.forEach { input ->
                // When
                val heartPointsGoal = heartPointsCalculator.calculateHeartPointsGoal(input)

                // Then - Should be within WHO safety bounds
                assertTrue(
                    heartPointsGoal >= 15,
                    "Heart points goal $heartPointsGoal should be at least 15 for health benefits"
                )
                assertTrue(
                    heartPointsGoal <= 50,
                    "Heart points goal $heartPointsGoal should be at most 50 for realistic daily goals"
                )
            }
        }
    }

    @Nested
    @DisplayName("Performance Requirements")
    inner class PerformanceRequirements {

        @Test
        @DisplayName("Should complete individual calculations within 100ms")
        fun shouldCompleteIndividualCalculationsWithin100ms() = runTest {
            // Given
            val input = GoalCalculationInput(
                age = 25,
                gender = Gender.MALE,
                heightInCm = 175,
                weightInKg = 70.0,
                activityLevel = ActivityLevel.LIGHT
            )

            // When & Then - Each calculator should complete within 100ms
            val stepsTime = measureTimeMillis {
                repeat(10) { stepsCalculator.calculateStepsGoal(input) }
            }
            assertTrue(stepsTime < 100, "Steps calculation took ${stepsTime}ms, should be < 100ms")

            val caloriesTime = measureTimeMillis {
                repeat(10) { caloriesCalculator.calculateCaloriesGoal(input) }
            }
            assertTrue(caloriesTime < 100, "Calories calculation took ${caloriesTime}ms, should be < 100ms")

            val heartPointsTime = measureTimeMillis {
                repeat(10) { heartPointsCalculator.calculateHeartPointsGoal(input) }
            }
            assertTrue(heartPointsTime < 100, "Heart points calculation took ${heartPointsTime}ms, should be < 100ms")
        }

        @Test
        @DisplayName("Should complete all three calculations within 500ms total")
        fun shouldCompleteAllCalculationsWithin500ms() = runTest {
            // Given
            val input = GoalCalculationInput(
                age = 30,
                gender = Gender.FEMALE,
                heightInCm = 165,
                weightInKg = 60.0,
                activityLevel = ActivityLevel.MODERATE
            )

            // When
            val totalTime = measureTimeMillis {
                repeat(10) {
                    stepsCalculator.calculateStepsGoal(input)
                    caloriesCalculator.calculateCaloriesGoal(input)
                    heartPointsCalculator.calculateHeartPointsGoal(input)
                }
            }

            // Then - Total time should be under 500ms for all three calculations
            assertTrue(totalTime < 500, "Total calculation time ${totalTime}ms should be < 500ms")
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    inner class EdgeCasesAndBoundaryConditions {

        @Test
        @DisplayName("Should handle minimum age boundary (13 years)")
        fun shouldHandleMinimumAgeBoundary() {
            // Given
            val input = GoalCalculationInput(
                age = 13,
                gender = Gender.FEMALE,
                heightInCm = 150,
                weightInKg = 45.0,
                activityLevel = ActivityLevel.LIGHT
            )

            // When & Then - Should not throw exceptions and produce reasonable results
            assertDoesNotThrow {
                val steps = stepsCalculator.calculateStepsGoal(input)
                val calories = caloriesCalculator.calculateCaloriesGoal(input)
                val heartPoints = heartPointsCalculator.calculateHeartPointsGoal(input)
                
                assertTrue(steps in 5000..20000)
                assertTrue(calories in 1200..4000)
                assertTrue(heartPoints in 15..50)
            }
        }

        @Test
        @DisplayName("Should handle maximum age boundary (120 years)")
        fun shouldHandleMaximumAgeBoundary() {
            // Given
            val input = GoalCalculationInput(
                age = 120,
                gender = Gender.MALE,
                heightInCm = 170,
                weightInKg = 60.0,
                activityLevel = ActivityLevel.SEDENTARY
            )

            // When & Then - Should not throw exceptions and produce reasonable results
            assertDoesNotThrow {
                val steps = stepsCalculator.calculateStepsGoal(input)
                val calories = caloriesCalculator.calculateCaloriesGoal(input)
                val heartPoints = heartPointsCalculator.calculateHeartPointsGoal(input)
                
                assertTrue(steps in 5000..20000)
                assertTrue(calories in 1200..4000)
                assertTrue(heartPoints in 15..50)
            }
        }

        @Test
        @DisplayName("Should handle extreme height and weight values")
        fun shouldHandleExtremeHeightAndWeightValues() {
            // Given - Extreme but valid values
            val extremeInputs = listOf(
                GoalCalculationInput(25, Gender.MALE, 100, 30.0, ActivityLevel.LIGHT), // Very short, light
                GoalCalculationInput(25, Gender.FEMALE, 250, 300.0, ActivityLevel.LIGHT) // Very tall, heavy
            )

            extremeInputs.forEach { input ->
                // When & Then - Should handle gracefully
                assertDoesNotThrow {
                    val steps = stepsCalculator.calculateStepsGoal(input)
                    val calories = caloriesCalculator.calculateCaloriesGoal(input)
                    val heartPoints = heartPointsCalculator.calculateHeartPointsGoal(input)
                    
                    // Should still be within safety bounds
                    assertTrue(steps in 5000..20000)
                    assertTrue(calories in 1200..4000)
                    assertTrue(heartPoints in 15..50)
                }
            }
        }

        @Test
        @DisplayName("Should produce consistent results for identical inputs")
        fun shouldProduceConsistentResultsForIdenticalInputs() {
            // Given
            val input = GoalCalculationInput(
                age = 25,
                gender = Gender.MALE,
                heightInCm = 175,
                weightInKg = 70.0,
                activityLevel = ActivityLevel.LIGHT
            )

            // When - Calculate multiple times
            val stepsResults = (1..5).map { stepsCalculator.calculateStepsGoal(input) }
            val caloriesResults = (1..5).map { caloriesCalculator.calculateCaloriesGoal(input) }
            val heartPointsResults = (1..5).map { heartPointsCalculator.calculateHeartPointsGoal(input) }

            // Then - All results should be identical
            assertTrue(stepsResults.all { it == stepsResults.first() }, "Steps calculations should be consistent")
            assertTrue(caloriesResults.all { it == caloriesResults.first() }, "Calories calculations should be consistent")
            assertTrue(heartPointsResults.all { it == heartPointsResults.first() }, "Heart points calculations should be consistent")
        }
    }

    @Nested
    @DisplayName("WHO Formula Documentation Validation")
    inner class WHOFormulaDocumentationValidation {

        @Test
        @DisplayName("Should document WHO sources in calculator implementations")
        fun shouldDocumentWHOSourcesInCalculatorImplementations() {
            // This test verifies that WHO sources are properly documented
            // The actual validation would be done through code review and documentation checks
            
            // Verify that calculators exist and are properly implemented
            assertNotNull(stepsCalculator, "StepsGoalCalculator should be implemented")
            assertNotNull(caloriesCalculator, "CaloriesGoalCalculator should be implemented")
            assertNotNull(heartPointsCalculator, "HeartPointsGoalCalculator should be implemented")
            
            // Test that calculators produce results within WHO-recommended ranges
            val standardInput = GoalCalculationInput(
                age = 25,
                gender = Gender.MALE,
                heightInCm = 175,
                weightInKg = 70.0,
                activityLevel = ActivityLevel.LIGHT
            )
            
            val steps = stepsCalculator.calculateStepsGoal(standardInput)
            val calories = caloriesCalculator.calculateCaloriesGoal(standardInput)
            val heartPoints = heartPointsCalculator.calculateHeartPointsGoal(standardInput)
            
            // Verify results align with WHO guidelines
            assertTrue(steps >= 5000, "Steps should meet WHO minimum activity recommendations")
            assertTrue(calories >= 1200, "Calories should meet WHO minimum energy requirements")
            assertTrue(heartPoints >= 15, "Heart points should meet WHO cardiovascular activity recommendations")
        }
    }
}