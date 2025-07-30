package com.vibehealth.android.ui.onboarding

import com.vibehealth.android.core.validation.OnboardingValidationHelper
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.UnitSystem
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.test.*

/**
 * Comprehensive unit tests for onboarding validation logic
 */
class OnboardingValidationComprehensiveTest {

    private lateinit var validationHelper: OnboardingValidationHelper

    @Before
    fun setup() {
        validationHelper = OnboardingValidationHelper(mockk())
    }

    @Test
    fun validateName_validNames_shouldPass() = runTest {
        val validNames = listOf(
            "John Doe",
            "María García",
            "李小明",
            "Jean-Pierre",
            "O'Connor",
            "van der Berg",
            "José María",
            "Anne-Marie",
            "Al-Rahman",
            "Müller"
        )

        validNames.forEach { name ->
            val result = validationHelper.validateCompleteOnboardingData(
                name = name,
                birthday = Date(System.currentTimeMillis() - (25L * 365 * 24 * 60 * 60 * 1000)),
                height = "175",
                weight = "70",
                gender = Gender.MALE,
                unitSystem = UnitSystem.METRIC
            )
            
            assertNull(result.errors.nameError, "Name '$name' should be valid")
        }
    }

    @Test
    fun validateName_invalidNames_shouldFail() = runTest {
        val invalidNames = mapOf(
            "" to "Name is required",
            " " to "Name is required",
            "A" to "Name must be at least 2 characters",
            "A".repeat(51) to "Name must be less than 50 characters",
            "123" to "Name should contain letters",
            "John@Doe" to "Name contains invalid characters",
            "John#Doe" to "Name contains invalid characters"
        )

        invalidNames.forEach { (name, expectedError) ->
            val result = validationHelper.validateCompleteOnboardingData(
                name = name,
                birthday = Date(System.currentTimeMillis() - (25L * 365 * 24 * 60 * 60 * 1000)),
                height = "175",
                weight = "70",
                gender = Gender.MALE,
                unitSystem = UnitSystem.METRIC
            )
            
            assertNotNull(result.errors.nameError, "Name '$name' should be invalid")
            assertTrue(
                result.errors.nameError!!.contains(expectedError.split(" ")[0], ignoreCase = true),
                "Error for name '$name' should contain relevant message"
            )
        }
    }

    @Test
    fun validateBirthday_validDates_shouldPass() = runTest {
        val validBirthdays = listOf(
            Date(System.currentTimeMillis() - (18L * 365 * 24 * 60 * 60 * 1000)), // 18 years ago
            Date(System.currentTimeMillis() - (25L * 365 * 24 * 60 * 60 * 1000)), // 25 years ago
            Date(System.currentTimeMillis() - (65L * 365 * 24 * 60 * 60 * 1000)), // 65 years ago
            Date(System.currentTimeMillis() - (100L * 365 * 24 * 60 * 60 * 1000)) // 100 years ago
        )

        validBirthdays.forEach { birthday ->
            val result = validationHelper.validateCompleteOnboardingData(
                name = "Test User",
                birthday = birthday,
                height = "175",
                weight = "70",
                gender = Gender.MALE,
                unitSystem = UnitSystem.METRIC
            )
            
            assertNull(result.errors.birthdayError, "Birthday should be valid")
        }
    }

    @Test
    fun validateBirthday_invalidDates_shouldFail() = runTest {
        val invalidBirthdays = listOf(
            null, // No birthday
            Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000)), // Future date
            Date(System.currentTimeMillis() - (10L * 365 * 24 * 60 * 60 * 1000)), // Too young (10 years)
            Date(System.currentTimeMillis() - (130L * 365 * 24 * 60 * 60 * 1000)) // Too old (130 years)
        )

        invalidBirthdays.forEach { birthday ->
            val result = validationHelper.validateCompleteOnboardingData(
                name = "Test User",
                birthday = birthday,
                height = "175",
                weight = "70",
                gender = Gender.MALE,
                unitSystem = UnitSystem.METRIC
            )
            
            assertNotNull(result.errors.birthdayError, "Birthday should be invalid: $birthday")
        }
    }

    @Test
    fun validateHeight_metricSystem_shouldValidateCorrectly() = runTest {
        val testCases = mapOf(
            "100" to true,  // Minimum valid
            "175" to true,  // Normal
            "250" to true,  // Maximum valid
            "99" to false,  // Too short
            "251" to false, // Too tall
            "" to false,    // Empty
            "abc" to false, // Non-numeric
            "175.5" to true, // Decimal
            "-175" to false // Negative
        )

        testCases.forEach { (height, shouldBeValid) ->
            val result = validationHelper.validateCompleteOnboardingData(
                name = "Test User",
                birthday = Date(System.currentTimeMillis() - (25L * 365 * 24 * 60 * 60 * 1000)),
                height = height,
                weight = "70",
                gender = Gender.MALE,
                unitSystem = UnitSystem.METRIC
            )
            
            if (shouldBeValid) {
                assertNull(result.errors.heightError, "Height '$height' should be valid in metric")
            } else {
                assertNotNull(result.errors.heightError, "Height '$height' should be invalid in metric")
            }
        }
    }

    @Test
    fun validateHeight_imperialSystem_shouldValidateCorrectly() = runTest {
        val testCases = mapOf(
            "3.0" to true,   // 3'0" - minimum valid
            "5.9" to true,   // 5'9" - normal
            "8.0" to true,   // 8'0" - maximum valid
            "2.11" to false, // Too short
            "8.1" to false,  // Too tall
            "" to false,     // Empty
            "abc" to false,  // Non-numeric
            "-5.9" to false  // Negative
        )

        testCases.forEach { (height, shouldBeValid) ->
            val result = validationHelper.validateCompleteOnboardingData(
                name = "Test User",
                birthday = Date(System.currentTimeMillis() - (25L * 365 * 24 * 60 * 60 * 1000)),
                height = height,
                weight = "150",
                gender = Gender.MALE,
                unitSystem = UnitSystem.IMPERIAL
            )
            
            if (shouldBeValid) {
                assertNull(result.errors.heightError, "Height '$height' should be valid in imperial")
            } else {
                assertNotNull(result.errors.heightError, "Height '$height' should be invalid in imperial")
            }
        }
    }

    @Test
    fun validateWeight_metricSystem_shouldValidateCorrectly() = runTest {
        val testCases = mapOf(
            "30" to true,    // Minimum valid
            "70" to true,    // Normal
            "300" to true,   // Maximum valid
            "29" to false,   // Too light
            "301" to false,  // Too heavy
            "" to false,     // Empty
            "abc" to false,  // Non-numeric
            "70.5" to true,  // Decimal
            "-70" to false   // Negative
        )

        testCases.forEach { (weight, shouldBeValid) ->
            val result = validationHelper.validateCompleteOnboardingData(
                name = "Test User",
                birthday = Date(System.currentTimeMillis() - (25L * 365 * 24 * 60 * 60 * 1000)),
                height = "175",
                weight = weight,
                gender = Gender.MALE,
                unitSystem = UnitSystem.METRIC
            )
            
            if (shouldBeValid) {
                assertNull(result.errors.weightError, "Weight '$weight' should be valid in metric")
            } else {
                assertNotNull(result.errors.weightError, "Weight '$weight' should be invalid in metric")
            }
        }
    }

    @Test
    fun validateWeight_imperialSystem_shouldValidateCorrectly() = runTest {
        val testCases = mapOf(
            "66" to true,    // Minimum valid (30kg)
            "154" to true,   // Normal (70kg)
            "660" to true,   // Maximum valid (300kg)
            "65" to false,   // Too light
            "661" to false,  // Too heavy
            "" to false,     // Empty
            "abc" to false,  // Non-numeric
            "154.5" to true, // Decimal
            "-154" to false  // Negative
        )

        testCases.forEach { (weight, shouldBeValid) ->
            val result = validationHelper.validateCompleteOnboardingData(
                name = "Test User",
                birthday = Date(System.currentTimeMillis() - (25L * 365 * 24 * 60 * 60 * 1000)),
                height = "5.9",
                weight = weight,
                gender = Gender.MALE,
                unitSystem = UnitSystem.IMPERIAL
            )
            
            if (shouldBeValid) {
                assertNull(result.errors.weightError, "Weight '$weight' should be valid in imperial")
            } else {
                assertNotNull(result.errors.weightError, "Weight '$weight' should be invalid in imperial")
            }
        }
    }

    @Test
    fun validateGender_allOptions_shouldBeValid() = runTest {
        val genderOptions = listOf(
            Gender.MALE,
            Gender.FEMALE,
            Gender.OTHER,
            Gender.PREFER_NOT_TO_SAY
        )

        genderOptions.forEach { gender ->
            val result = validationHelper.validateCompleteOnboardingData(
                name = "Test User",
                birthday = Date(System.currentTimeMillis() - (25L * 365 * 24 * 60 * 60 * 1000)),
                height = "175",
                weight = "70",
                gender = gender,
                unitSystem = UnitSystem.METRIC
            )
            
            assertNull(result.errors.genderError, "Gender $gender should be valid")
        }
    }

    @Test
    fun validateCompleteData_allValid_shouldPass() = runTest {
        val result = validationHelper.validateCompleteOnboardingData(
            name = "John Doe",
            birthday = Date(System.currentTimeMillis() - (25L * 365 * 24 * 60 * 60 * 1000)),
            height = "175",
            weight = "70",
            gender = Gender.MALE,
            unitSystem = UnitSystem.METRIC
        )
        
        assertTrue(result.isValid, "Complete valid data should pass validation")
        assertNull(result.errors.nameError)
        assertNull(result.errors.birthdayError)
        assertNull(result.errors.heightError)
        assertNull(result.errors.weightError)
        assertNull(result.errors.genderError)
        assertNotNull(result.parsedHeight)
        assertNotNull(result.parsedWeight)
    }

    @Test
    fun validateCompleteData_multipleErrors_shouldReturnAllErrors() = runTest {
        val result = validationHelper.validateCompleteOnboardingData(
            name = "", // Invalid
            birthday = null, // Invalid
            height = "50", // Invalid
            weight = "10", // Invalid
            gender = Gender.MALE,
            unitSystem = UnitSystem.METRIC
        )
        
        assertFalse(result.isValid, "Invalid data should fail validation")
        assertNotNull(result.errors.nameError, "Should have name error")
        assertNotNull(result.errors.birthdayError, "Should have birthday error")
        assertNotNull(result.errors.heightError, "Should have height error")
        assertNotNull(result.errors.weightError, "Should have weight error")
    }

    @Test
    fun validateCompleteData_edgeCases_shouldHandleCorrectly() = runTest {
        // Test with minimum valid values
        val minResult = validationHelper.validateCompleteOnboardingData(
            name = "Jo", // Minimum length
            birthday = Date(System.currentTimeMillis() - (13L * 365 * 24 * 60 * 60 * 1000)), // 13 years old
            height = "100", // Minimum height
            weight = "30", // Minimum weight
            gender = Gender.PREFER_NOT_TO_SAY,
            unitSystem = UnitSystem.METRIC
        )
        
        assertTrue(minResult.isValid, "Minimum valid values should pass")
        
        // Test with maximum valid values
        val maxResult = validationHelper.validateCompleteOnboardingData(
            name = "A".repeat(50), // Maximum length
            birthday = Date(System.currentTimeMillis() - (120L * 365 * 24 * 60 * 60 * 1000)), // 120 years old
            height = "250", // Maximum height
            weight = "300", // Maximum weight
            gender = Gender.OTHER,
            unitSystem = UnitSystem.METRIC
        )
        
        assertTrue(maxResult.isValid, "Maximum valid values should pass")
    }

    @Test
    fun validateCompleteData_unicodeNames_shouldHandleCorrectly() = runTest {
        val unicodeNames = listOf(
            "José María",
            "李小明",
            "محمد علي",
            "Владимир",
            "Ñoño",
            "François",
            "Björk",
            "Müller"
        )

        unicodeNames.forEach { name ->
            val result = validationHelper.validateCompleteOnboardingData(
                name = name,
                birthday = Date(System.currentTimeMillis() - (25L * 365 * 24 * 60 * 60 * 1000)),
                height = "175",
                weight = "70",
                gender = Gender.MALE,
                unitSystem = UnitSystem.METRIC
            )
            
            assertNull(result.errors.nameError, "Unicode name '$name' should be valid")
        }
    }

    @Test
    fun validateCompleteData_precisionHandling_shouldWork() = runTest {
        val precisionTestCases = listOf(
            "175.0" to "70.0",
            "175.5" to "70.5",
            "175.99" to "70.99",
            "175.123" to "70.456" // Should handle multiple decimal places
        )

        precisionTestCases.forEach { (height, weight) ->
            val result = validationHelper.validateCompleteOnboardingData(
                name = "Test User",
                birthday = Date(System.currentTimeMillis() - (25L * 365 * 24 * 60 * 60 * 1000)),
                height = height,
                weight = weight,
                gender = Gender.MALE,
                unitSystem = UnitSystem.METRIC
            )
            
            assertTrue(result.isValid, "Decimal values should be handled correctly")
            assertNotNull(result.parsedHeight, "Height should be parsed")
            assertNotNull(result.parsedWeight, "Weight should be parsed")
        }
    }
}