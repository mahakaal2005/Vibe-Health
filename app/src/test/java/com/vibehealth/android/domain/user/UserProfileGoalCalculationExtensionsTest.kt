package com.vibehealth.android.domain.user

import com.vibehealth.android.domain.common.UnitSystem
import com.vibehealth.android.domain.goals.ActivityLevel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.Calendar
import java.util.Date

/**
 * Comprehensive unit tests for UserProfile goal calculation extensions.
 * 
 * Tests cover validation methods, BMI calculations, age categories, and
 * conversion to GoalCalculationInput as specified in Task 2.1 requirements.
 */
class UserProfileGoalCalculationExtensionsTest {

    private fun createValidUserProfile(
        age: Int = 25,
        gender: Gender = Gender.MALE,
        heightInCm: Int = 175,
        weightInKg: Double = 70.0
    ): UserProfile {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -age)
        
        return UserProfile(
            userId = "test-user-123",
            email = "test@example.com",
            displayName = "Test User",
            firstName = "Test",
            lastName = "User",
            birthday = calendar.time,
            gender = gender,
            unitSystem = UnitSystem.METRIC,
            heightInCm = heightInCm,
            weightInKg = weightInKg,
            hasCompletedOnboarding = true
        )
    }

    @Nested
    @DisplayName("Goal Calculation Validation")
    inner class GoalCalculationValidation {

        @Test
        @DisplayName("Should validate complete profile as suitable for goal calculation")
        fun shouldValidateCompleteProfile() {
            val profile = createValidUserProfile()
            assertTrue(profile.isValidForGoalCalculation())
        }

        @Test
        @DisplayName("Should reject profile with missing birthday")
        fun shouldRejectProfileWithMissingBirthday() {
            val profile = createValidUserProfile().copy(birthday = null)
            assertFalse(profile.isValidForGoalCalculation())
        }

        @Test
        @DisplayName("Should reject profile with missing display name")
        fun shouldRejectProfileWithMissingDisplayName() {
            val profile = createValidUserProfile().copy(displayName = "")
            assertFalse(profile.isValidForGoalCalculation())
        }

        @Test
        @DisplayName("Should reject profile with zero height")
        fun shouldRejectProfileWithZeroHeight() {
            val profile = createValidUserProfile().copy(heightInCm = 0)
            assertFalse(profile.isValidForGoalCalculation())
        }

        @Test
        @DisplayName("Should reject profile with zero weight")
        fun shouldRejectProfileWithZeroWeight() {
            val profile = createValidUserProfile().copy(weightInKg = 0.0)
            assertFalse(profile.isValidForGoalCalculation())
        }

        @ParameterizedTest
        @ValueSource(ints = [12, 121, -5, 150])
        @DisplayName("Should reject profiles with invalid ages")
        fun shouldRejectInvalidAges(invalidAge: Int) {
            val profile = createValidUserProfile(age = invalidAge)
            assertFalse(profile.isValidForGoalCalculation())
        }

        @ParameterizedTest
        @ValueSource(ints = [13, 18, 25, 65, 120])
        @DisplayName("Should accept profiles with valid ages")
        fun shouldAcceptValidAges(validAge: Int) {
            val profile = createValidUserProfile(age = validAge)
            assertTrue(profile.isValidForGoalCalculation())
        }

        @ParameterizedTest
        @ValueSource(ints = [99, 251, -10, 0])
        @DisplayName("Should reject profiles with invalid heights")
        fun shouldRejectInvalidHeights(invalidHeight: Int) {
            val profile = createValidUserProfile(heightInCm = invalidHeight)
            assertFalse(profile.isValidForGoalCalculation())
        }

        @ParameterizedTest
        @ValueSource(ints = [100, 150, 175, 200, 250])
        @DisplayName("Should accept profiles with valid heights")
        fun shouldAcceptValidHeights(validHeight: Int) {
            val profile = createValidUserProfile(heightInCm = validHeight)
            assertTrue(profile.isValidForGoalCalculation())
        }

        @ParameterizedTest
        @ValueSource(doubles = [29.9, 300.1, -10.0, 0.0])
        @DisplayName("Should reject profiles with invalid weights")
        fun shouldRejectInvalidWeights(invalidWeight: Double) {
            val profile = createValidUserProfile(weightInKg = invalidWeight)
            assertFalse(profile.isValidForGoalCalculation())
        }

        @ParameterizedTest
        @ValueSource(doubles = [30.0, 50.0, 70.0, 100.0, 300.0])
        @DisplayName("Should accept profiles with valid weights")
        fun shouldAcceptValidWeights(validWeight: Double) {
            val profile = createValidUserProfile(weightInKg = validWeight)
            assertTrue(profile.isValidForGoalCalculation())
        }
    }

    @Nested
    @DisplayName("BMI Calculations")
    inner class BMICalculations {

        @Test
        @DisplayName("Should calculate BMI correctly for metric values")
        fun shouldCalculateBMICorrectly() {
            val profile = createValidUserProfile(heightInCm = 175, weightInKg = 70.0)
            val bmi = profile.getBMI()
            
            assertNotNull(bmi)
            assertEquals(22.86, bmi!!, 0.01) // 70 / (1.75^2) = 22.86
        }

        @Test
        @DisplayName("Should return null BMI for invalid height")
        fun shouldReturnNullBMIForInvalidHeight() {
            val profile = createValidUserProfile(heightInCm = 0)
            assertNull(profile.getBMI())
        }

        @Test
        @DisplayName("Should return null BMI for invalid weight")
        fun shouldReturnNullBMIForInvalidWeight() {
            val profile = createValidUserProfile(weightInKg = 0.0)
            assertNull(profile.getBMI())
        }

        @Test
        @DisplayName("Should categorize underweight BMI correctly")
        fun shouldCategorizeUnderweightBMI() {
            val profile = createValidUserProfile(heightInCm = 175, weightInKg = 50.0) // BMI ~16.3
            assertEquals("Underweight", profile.getBMICategory())
        }

        @Test
        @DisplayName("Should categorize normal weight BMI correctly")
        fun shouldCategorizeNormalWeightBMI() {
            val profile = createValidUserProfile(heightInCm = 175, weightInKg = 70.0) // BMI ~22.9
            assertEquals("Normal weight", profile.getBMICategory())
        }

        @Test
        @DisplayName("Should categorize overweight BMI correctly")
        fun shouldCategorizeOverweightBMI() {
            val profile = createValidUserProfile(heightInCm = 175, weightInKg = 85.0) // BMI ~27.8
            assertEquals("Overweight", profile.getBMICategory())
        }

        @Test
        @DisplayName("Should categorize obese BMI correctly")
        fun shouldCategorizeObeseBMI() {
            val profile = createValidUserProfile(heightInCm = 175, weightInKg = 100.0) // BMI ~32.7
            assertEquals("Obese", profile.getBMICategory())
        }

        @Test
        @DisplayName("Should return null BMI category for invalid data")
        fun shouldReturnNullBMICategoryForInvalidData() {
            val profile = createValidUserProfile(heightInCm = 0)
            assertNull(profile.getBMICategory())
        }
    }

    @Nested
    @DisplayName("Age Categories")
    inner class AgeCategories {

        @Test
        @DisplayName("Should categorize youth correctly")
        fun shouldCategorizeYouth() {
            val profile = createValidUserProfile(age = 16)
            assertEquals("Youth", profile.getAgeCategory())
        }

        @Test
        @DisplayName("Should categorize adult correctly")
        fun shouldCategorizeAdult() {
            val profile = createValidUserProfile(age = 30)
            assertEquals("Adult", profile.getAgeCategory())
        }

        @Test
        @DisplayName("Should categorize older adult correctly")
        fun shouldCategorizeOlderAdult() {
            val profile = createValidUserProfile(age = 70)
            assertEquals("Older Adult", profile.getAgeCategory())
        }

        @Test
        @DisplayName("Should handle edge case ages")
        fun shouldHandleEdgeCaseAges() {
            assertEquals("Youth", createValidUserProfile(age = 17).getAgeCategory())
            assertEquals("Adult", createValidUserProfile(age = 18).getAgeCategory())
            assertEquals("Adult", createValidUserProfile(age = 64).getAgeCategory())
            assertEquals("Older Adult", createValidUserProfile(age = 65).getAgeCategory())
        }
    }

    @Nested
    @DisplayName("Goal Calculation Input Conversion")
    inner class GoalCalculationInputConversion {

        @Test
        @DisplayName("Should convert valid profile to GoalCalculationInput")
        fun shouldConvertValidProfileToInput() {
            val profile = createValidUserProfile(
                age = 25,
                gender = Gender.FEMALE,
                heightInCm = 165,
                weightInKg = 60.0
            )
            
            val input = profile.toGoalCalculationInput()
            
            assertNotNull(input)
            assertEquals(25, input!!.age)
            assertEquals(Gender.FEMALE, input.gender)
            assertEquals(165, input.heightInCm)
            assertEquals(60.0, input.weightInKg)
            assertEquals(ActivityLevel.DEFAULT_FOR_URBAN_PROFESSIONALS, input.activityLevel)
        }

        @Test
        @DisplayName("Should return null for invalid profile")
        fun shouldReturnNullForInvalidProfile() {
            val profile = createValidUserProfile().copy(birthday = null)
            assertNull(profile.toGoalCalculationInput())
        }

        @Test
        @DisplayName("Should handle all gender types correctly")
        fun shouldHandleAllGenderTypes() {
            Gender.values().forEach { gender ->
                val profile = createValidUserProfile(gender = gender)
                val input = profile.toGoalCalculationInput()
                
                assertNotNull(input, "Should convert profile with gender $gender")
                assertEquals(gender, input!!.gender)
            }
        }

        @Test
        @DisplayName("Should use default activity level for urban professionals")
        fun shouldUseDefaultActivityLevel() {
            val profile = createValidUserProfile()
            val input = profile.toGoalCalculationInput()
            
            assertNotNull(input)
            assertEquals(ActivityLevel.LIGHT, input!!.activityLevel)
            assertEquals(ActivityLevel.DEFAULT_FOR_URBAN_PROFESSIONALS, input.activityLevel)
        }
    }

    @Nested
    @DisplayName("Profile Change Detection")
    inner class ProfileChangeDetection {

        @Test
        @DisplayName("Should detect birthday changes")
        fun shouldDetectBirthdayChanges() {
            val originalProfile = createValidUserProfile(age = 25)
            val changedProfile = createValidUserProfile(age = 26)
            
            assertTrue(changedProfile.hasGoalCalculationRelevantChanges(originalProfile))
        }

        @Test
        @DisplayName("Should detect gender changes")
        fun shouldDetectGenderChanges() {
            val originalProfile = createValidUserProfile(gender = Gender.MALE)
            val changedProfile = createValidUserProfile(gender = Gender.FEMALE)
            
            assertTrue(changedProfile.hasGoalCalculationRelevantChanges(originalProfile))
        }

        @Test
        @DisplayName("Should detect height changes")
        fun shouldDetectHeightChanges() {
            val originalProfile = createValidUserProfile(heightInCm = 175)
            val changedProfile = createValidUserProfile(heightInCm = 180)
            
            assertTrue(changedProfile.hasGoalCalculationRelevantChanges(originalProfile))
        }

        @Test
        @DisplayName("Should detect weight changes")
        fun shouldDetectWeightChanges() {
            val originalProfile = createValidUserProfile(weightInKg = 70.0)
            val changedProfile = createValidUserProfile(weightInKg = 75.0)
            
            assertTrue(changedProfile.hasGoalCalculationRelevantChanges(originalProfile))
        }

        @Test
        @DisplayName("Should not detect irrelevant changes")
        fun shouldNotDetectIrrelevantChanges() {
            val originalProfile = createValidUserProfile()
            val changedProfile = originalProfile.copy(
                displayName = "Different Name",
                email = "different@email.com",
                firstName = "Different",
                lastName = "Name"
            )
            
            assertFalse(changedProfile.hasGoalCalculationRelevantChanges(originalProfile))
        }

        @Test
        @DisplayName("Should not detect changes when profiles are identical")
        fun shouldNotDetectChangesWhenIdentical() {
            val profile1 = createValidUserProfile()
            val profile2 = createValidUserProfile()
            
            assertFalse(profile1.hasGoalCalculationRelevantChanges(profile2))
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    inner class EdgeCases {

        @Test
        @DisplayName("Should handle minimum valid values")
        fun shouldHandleMinimumValidValues() {
            val profile = createValidUserProfile(
                age = 13,
                heightInCm = 100,
                weightInKg = 30.0
            )
            
            assertTrue(profile.isValidForGoalCalculation())
            assertNotNull(profile.toGoalCalculationInput())
            assertNotNull(profile.getBMI())
            assertEquals("Youth", profile.getAgeCategory())
        }

        @Test
        @DisplayName("Should handle maximum valid values")
        fun shouldHandleMaximumValidValues() {
            val profile = createValidUserProfile(
                age = 120,
                heightInCm = 250,
                weightInKg = 300.0
            )
            
            assertTrue(profile.isValidForGoalCalculation())
            assertNotNull(profile.toGoalCalculationInput())
            assertNotNull(profile.getBMI())
            assertEquals("Older Adult", profile.getAgeCategory())
        }

        @Test
        @DisplayName("Should handle boundary age values correctly")
        fun shouldHandleBoundaryAgeValues() {
            // Test age boundaries for validation
            assertFalse(createValidUserProfile(age = 12).isValidForGoalCalculation())
            assertTrue(createValidUserProfile(age = 13).isValidForGoalCalculation())
            assertTrue(createValidUserProfile(age = 120).isValidForGoalCalculation())
            assertFalse(createValidUserProfile(age = 121).isValidForGoalCalculation())
        }

        @Test
        @DisplayName("Should handle boundary height values correctly")
        fun shouldHandleBoundaryHeightValues() {
            // Test height boundaries for validation
            assertFalse(createValidUserProfile(heightInCm = 99).isValidForGoalCalculation())
            assertTrue(createValidUserProfile(heightInCm = 100).isValidForGoalCalculation())
            assertTrue(createValidUserProfile(heightInCm = 250).isValidForGoalCalculation())
            assertFalse(createValidUserProfile(heightInCm = 251).isValidForGoalCalculation())
        }

        @Test
        @DisplayName("Should handle boundary weight values correctly")
        fun shouldHandleBoundaryWeightValues() {
            // Test weight boundaries for validation
            assertFalse(createValidUserProfile(weightInKg = 29.9).isValidForGoalCalculation())
            assertTrue(createValidUserProfile(weightInKg = 30.0).isValidForGoalCalculation())
            assertTrue(createValidUserProfile(weightInKg = 300.0).isValidForGoalCalculation())
            assertFalse(createValidUserProfile(weightInKg = 300.1).isValidForGoalCalculation())
        }

        @Test
        @DisplayName("Should handle extreme BMI calculations")
        fun shouldHandleExtremeBMICalculations() {
            // Very tall, light person
            val tallLightProfile = createValidUserProfile(heightInCm = 250, weightInKg = 50.0)
            val lowBMI = tallLightProfile.getBMI()
            assertNotNull(lowBMI)
            assertEquals("Underweight", tallLightProfile.getBMICategory())
            
            // Short, heavy person
            val shortHeavyProfile = createValidUserProfile(heightInCm = 100, weightInKg = 200.0)
            val highBMI = shortHeavyProfile.getBMI()
            assertNotNull(highBMI)
            assertEquals("Obese", shortHeavyProfile.getBMICategory())
        }

        @Test
        @DisplayName("Should handle future birthday dates gracefully")
        fun shouldHandleFutureBirthdayDates() {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.YEAR, 1) // Future date
            
            val profile = createValidUserProfile().copy(birthday = calendar.time)
            
            // Should handle gracefully - getAge() returns 0 for future dates
            assertFalse(profile.isValidForGoalCalculation()) // Age 0 is invalid
        }

        @Test
        @DisplayName("Should handle very old birthday dates")
        fun shouldHandleVeryOldBirthdayDates() {
            val calendar = Calendar.getInstance()
            calendar.set(1900, Calendar.JANUARY, 1) // Very old date
            
            val profile = createValidUserProfile().copy(birthday = calendar.time)
            
            // Should handle gracefully - very old age should be invalid
            assertFalse(profile.isValidForGoalCalculation())
        }
    }

    @Nested
    @DisplayName("Integration with Existing Methods")
    inner class IntegrationWithExistingMethods {

        @Test
        @DisplayName("Should work correctly with existing getAge method")
        fun shouldWorkWithExistingGetAgeMethod() {
            val profile = createValidUserProfile(age = 30)
            
            assertEquals(30, profile.getAge())
            assertEquals("Adult", profile.getAgeCategory())
            assertTrue(profile.isValidForGoalCalculation())
        }

        @Test
        @DisplayName("Should work correctly with existing isOnboardingDataComplete method")
        fun shouldWorkWithExistingOnboardingDataComplete() {
            val completeProfile = createValidUserProfile()
            val incompleteProfile = completeProfile.copy(displayName = "")
            
            assertTrue(completeProfile.isOnboardingDataComplete())
            assertTrue(completeProfile.isValidForGoalCalculation())
            
            assertFalse(incompleteProfile.isOnboardingDataComplete())
            assertFalse(incompleteProfile.isValidForGoalCalculation())
        }

        @Test
        @DisplayName("Should work correctly with unit system conversions")
        fun shouldWorkWithUnitSystemConversions() {
            val metricProfile = createValidUserProfile().copy(unitSystem = UnitSystem.METRIC)
            val imperialProfile = createValidUserProfile().copy(unitSystem = UnitSystem.IMPERIAL)
            
            // Both should be valid for goal calculation regardless of unit system
            assertTrue(metricProfile.isValidForGoalCalculation())
            assertTrue(imperialProfile.isValidForGoalCalculation())
            
            // Both should convert to GoalCalculationInput (which uses metric internally)
            assertNotNull(metricProfile.toGoalCalculationInput())
            assertNotNull(imperialProfile.toGoalCalculationInput())
        }
    }
}