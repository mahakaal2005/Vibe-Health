package com.vibehealth.android.domain.goals

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime

/**
 * Comprehensive unit tests for DailyGoals domain model.
 * 
 * Tests cover validation, data consistency, helper methods, and edge cases
 * as specified in Task 2.1 requirements.
 */
class DailyGoalsTest {

    private val validGoals = DailyGoals(
        userId = "test-user-123",
        stepsGoal = 10000,
        caloriesGoal = 2000,
        heartPointsGoal = 30,
        calculatedAt = LocalDateTime.now(),
        calculationSource = CalculationSource.WHO_STANDARD
    )

    @Nested
    @DisplayName("Data Class Properties")
    inner class DataClassProperties {

        @Test
        @DisplayName("Should create DailyGoals with all required properties")
        fun shouldCreateWithAllProperties() {
            val timestamp = LocalDateTime.of(2024, 1, 15, 10, 30)
            val goals = DailyGoals(
                userId = "user-456",
                stepsGoal = 8000,
                caloriesGoal = 1800,
                heartPointsGoal = 25,
                calculatedAt = timestamp,
                calculationSource = CalculationSource.FALLBACK_DEFAULT
            )

            assertEquals("user-456", goals.userId)
            assertEquals(8000, goals.stepsGoal)
            assertEquals(1800, goals.caloriesGoal)
            assertEquals(25, goals.heartPointsGoal)
            assertEquals(timestamp, goals.calculatedAt)
            assertEquals(CalculationSource.FALLBACK_DEFAULT, goals.calculationSource)
        }

        @Test
        @DisplayName("Should implement proper equals and hashCode")
        fun shouldImplementEqualsAndHashCode() {
            val timestamp = LocalDateTime.of(2024, 1, 15, 10, 30)
            val goals1 = DailyGoals(
                userId = "user-123",
                stepsGoal = 10000,
                caloriesGoal = 2000,
                heartPointsGoal = 30,
                calculatedAt = timestamp,
                calculationSource = CalculationSource.WHO_STANDARD
            )
            val goals2 = DailyGoals(
                userId = "user-123",
                stepsGoal = 10000,
                caloriesGoal = 2000,
                heartPointsGoal = 30,
                calculatedAt = timestamp,
                calculationSource = CalculationSource.WHO_STANDARD
            )
            val goals3 = DailyGoals(
                userId = "user-456",
                stepsGoal = 10000,
                caloriesGoal = 2000,
                heartPointsGoal = 30,
                calculatedAt = timestamp,
                calculationSource = CalculationSource.WHO_STANDARD
            )

            // Test equals
            assertEquals(goals1, goals2)
            assertNotEquals(goals1, goals3)

            // Test hashCode consistency
            assertEquals(goals1.hashCode(), goals2.hashCode())
            assertNotEquals(goals1.hashCode(), goals3.hashCode())
        }

        @Test
        @DisplayName("Should support copy functionality")
        fun shouldSupportCopy() {
            val original = validGoals
            val copied = original.copy(stepsGoal = 12000)

            assertEquals(original.userId, copied.userId)
            assertEquals(12000, copied.stepsGoal)
            assertEquals(original.caloriesGoal, copied.caloriesGoal)
            assertEquals(original.heartPointsGoal, copied.heartPointsGoal)
            assertEquals(original.calculatedAt, copied.calculatedAt)
            assertEquals(original.calculationSource, copied.calculationSource)
        }
    }

    @Nested
    @DisplayName("Goal Validation")
    inner class GoalValidation {

        @Test
        @DisplayName("Should validate goals within acceptable ranges")
        fun shouldValidateGoalsWithinRanges() {
            val validGoals = DailyGoals(
                userId = "user-123",
                stepsGoal = 10000,
                caloriesGoal = 2000,
                heartPointsGoal = 30,
                calculatedAt = LocalDateTime.now(),
                calculationSource = CalculationSource.WHO_STANDARD
            )

            assertTrue(validGoals.isValid())
        }

        @ParameterizedTest
        @ValueSource(ints = [4999, 20001, -100, 0])
        @DisplayName("Should invalidate steps goals outside range (5000-20000)")
        fun shouldInvalidateStepsOutsideRange(invalidSteps: Int) {
            val goals = validGoals.copy(stepsGoal = invalidSteps)
            assertFalse(goals.isValid())
        }

        @ParameterizedTest
        @ValueSource(ints = [5000, 10000, 15000, 20000])
        @DisplayName("Should validate steps goals within range (5000-20000)")
        fun shouldValidateStepsWithinRange(validSteps: Int) {
            val goals = validGoals.copy(stepsGoal = validSteps)
            assertTrue(goals.isValid())
        }

        @ParameterizedTest
        @ValueSource(ints = [1199, 4001, -500, 0])
        @DisplayName("Should invalidate calories goals outside range (1200-4000)")
        fun shouldInvalidateCaloriesOutsideRange(invalidCalories: Int) {
            val goals = validGoals.copy(caloriesGoal = invalidCalories)
            assertFalse(goals.isValid())
        }

        @ParameterizedTest
        @ValueSource(ints = [1200, 2000, 3000, 4000])
        @DisplayName("Should validate calories goals within range (1200-4000)")
        fun shouldValidateCaloriesWithinRange(validCalories: Int) {
            val goals = validGoals.copy(caloriesGoal = validCalories)
            assertTrue(goals.isValid())
        }

        @ParameterizedTest
        @ValueSource(ints = [14, 51, -10, 0])
        @DisplayName("Should invalidate heart points goals outside range (15-50)")
        fun shouldInvalidateHeartPointsOutsideRange(invalidHeartPoints: Int) {
            val goals = validGoals.copy(heartPointsGoal = invalidHeartPoints)
            assertFalse(goals.isValid())
        }

        @ParameterizedTest
        @ValueSource(ints = [15, 25, 35, 50])
        @DisplayName("Should validate heart points goals within range (15-50)")
        fun shouldValidateHeartPointsWithinRange(validHeartPoints: Int) {
            val goals = validGoals.copy(heartPointsGoal = validHeartPoints)
            assertTrue(goals.isValid())
        }

        @Test
        @DisplayName("Should invalidate when multiple goals are out of range")
        fun shouldInvalidateMultipleOutOfRange() {
            val invalidGoals = DailyGoals(
                userId = "user-123",
                stepsGoal = 3000, // Too low
                caloriesGoal = 5000, // Too high
                heartPointsGoal = 60, // Too high
                calculatedAt = LocalDateTime.now(),
                calculationSource = CalculationSource.WHO_STANDARD
            )

            assertFalse(invalidGoals.isValid())
        }
    }

    @Nested
    @DisplayName("Helper Methods")
    inner class HelperMethods {

        @Test
        @DisplayName("Should generate proper summary string")
        fun shouldGenerateSummary() {
            val goals = DailyGoals(
                userId = "user-123",
                stepsGoal = 8500,
                caloriesGoal = 1900,
                heartPointsGoal = 28,
                calculatedAt = LocalDateTime.now(),
                calculationSource = CalculationSource.WHO_STANDARD
            )

            val summary = goals.getSummary()
            assertEquals("Daily Goals: 8500 steps, 1900 calories, 28 heart points", summary)
        }

        @Test
        @DisplayName("Should detect fresh goals (less than 24 hours old)")
        fun shouldDetectFreshGoals() {
            val freshGoals = validGoals.copy(calculatedAt = LocalDateTime.now().minusHours(12))
            assertTrue(freshGoals.isFresh())
        }

        @Test
        @DisplayName("Should detect stale goals (more than 24 hours old)")
        fun shouldDetectStaleGoals() {
            val staleGoals = validGoals.copy(calculatedAt = LocalDateTime.now().minusHours(25))
            assertFalse(staleGoals.isFresh())
        }

        @Test
        @DisplayName("Should detect goals exactly 24 hours old as stale")
        fun shouldDetectExactly24HoursAsStale() {
            val exactlyOldGoals = validGoals.copy(calculatedAt = LocalDateTime.now().minusHours(24))
            assertFalse(exactlyOldGoals.isFresh())
        }

        @Test
        @DisplayName("Should update timestamp while preserving other data")
        fun shouldUpdateTimestamp() {
            val originalTimestamp = LocalDateTime.of(2024, 1, 1, 10, 0)
            val originalGoals = validGoals.copy(calculatedAt = originalTimestamp)
            
            val updatedGoals = originalGoals.withUpdatedTimestamp()
            
            // All other properties should remain the same
            assertEquals(originalGoals.userId, updatedGoals.userId)
            assertEquals(originalGoals.stepsGoal, updatedGoals.stepsGoal)
            assertEquals(originalGoals.caloriesGoal, updatedGoals.caloriesGoal)
            assertEquals(originalGoals.heartPointsGoal, updatedGoals.heartPointsGoal)
            assertEquals(originalGoals.calculationSource, updatedGoals.calculationSource)
            
            // Timestamp should be updated to current time
            assertNotEquals(originalTimestamp, updatedGoals.calculatedAt)
            assertTrue(updatedGoals.calculatedAt.isAfter(originalTimestamp))
        }

        @Test
        @DisplayName("Should sanitize user ID for logging")
        fun shouldSanitizeForLogging() {
            val originalGoals = validGoals.copy(userId = "sensitive-user-id-123")
            val sanitizedGoals = originalGoals.sanitizeForLogging()
            
            assertEquals("[USER_ID_REDACTED]", sanitizedGoals.userId)
            
            // All other properties should remain the same
            assertEquals(originalGoals.stepsGoal, sanitizedGoals.stepsGoal)
            assertEquals(originalGoals.caloriesGoal, sanitizedGoals.caloriesGoal)
            assertEquals(originalGoals.heartPointsGoal, sanitizedGoals.heartPointsGoal)
            assertEquals(originalGoals.calculatedAt, sanitizedGoals.calculatedAt)
            assertEquals(originalGoals.calculationSource, sanitizedGoals.calculationSource)
        }
    }

    @Nested
    @DisplayName("CalculationSource Enum")
    inner class CalculationSourceEnum {

        @Test
        @DisplayName("Should provide correct display names")
        fun shouldProvideDisplayNames() {
            assertEquals("Calculated based on WHO standards", CalculationSource.WHO_STANDARD.getDisplayName())
            assertEquals("Default goals for health benefits", CalculationSource.FALLBACK_DEFAULT.getDisplayName())
            assertEquals("Manually adjusted goals", CalculationSource.USER_ADJUSTED.getDisplayName())
        }

        @Test
        @DisplayName("Should identify calculated sources correctly")
        fun shouldIdentifyCalculatedSources() {
            assertTrue(CalculationSource.WHO_STANDARD.isCalculated())
            assertTrue(CalculationSource.USER_ADJUSTED.isCalculated())
            assertFalse(CalculationSource.FALLBACK_DEFAULT.isCalculated())
        }

        @Test
        @DisplayName("Should have all required enum values")
        fun shouldHaveRequiredEnumValues() {
            val values = CalculationSource.values()
            assertEquals(3, values.size)
            assertTrue(values.contains(CalculationSource.WHO_STANDARD))
            assertTrue(values.contains(CalculationSource.FALLBACK_DEFAULT))
            assertTrue(values.contains(CalculationSource.USER_ADJUSTED))
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    inner class EdgeCases {

        @Test
        @DisplayName("Should handle minimum valid goal values")
        fun shouldHandleMinimumValidValues() {
            val minGoals = DailyGoals(
                userId = "user-123",
                stepsGoal = 5000,
                caloriesGoal = 1200,
                heartPointsGoal = 15,
                calculatedAt = LocalDateTime.now(),
                calculationSource = CalculationSource.WHO_STANDARD
            )

            assertTrue(minGoals.isValid())
        }

        @Test
        @DisplayName("Should handle maximum valid goal values")
        fun shouldHandleMaximumValidValues() {
            val maxGoals = DailyGoals(
                userId = "user-123",
                stepsGoal = 20000,
                caloriesGoal = 4000,
                heartPointsGoal = 50,
                calculatedAt = LocalDateTime.now(),
                calculationSource = CalculationSource.WHO_STANDARD
            )

            assertTrue(maxGoals.isValid())
        }

        @Test
        @DisplayName("Should handle empty user ID")
        fun shouldHandleEmptyUserId() {
            val goalsWithEmptyUserId = validGoals.copy(userId = "")
            
            // Model should still be valid (userId validation is business logic, not domain model concern)
            assertTrue(goalsWithEmptyUserId.isValid())
            assertEquals("", goalsWithEmptyUserId.userId)
        }

        @Test
        @DisplayName("Should handle very old timestamps")
        fun shouldHandleVeryOldTimestamps() {
            val veryOldGoals = validGoals.copy(calculatedAt = LocalDateTime.of(2020, 1, 1, 0, 0))
            assertFalse(veryOldGoals.isFresh())
        }

        @Test
        @DisplayName("Should handle future timestamps")
        fun shouldHandleFutureTimestamps() {
            val futureGoals = validGoals.copy(calculatedAt = LocalDateTime.now().plusDays(1))
            // Future timestamps should be considered fresh
            assertTrue(futureGoals.isFresh())
        }
    }
}