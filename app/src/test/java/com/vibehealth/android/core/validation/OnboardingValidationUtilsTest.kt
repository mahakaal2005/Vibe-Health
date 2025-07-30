package com.vibehealth.android.core.validation

import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.UnitSystem
import com.vibehealth.android.domain.onboarding.ValidationField
import com.vibehealth.android.domain.onboarding.ValidationErrors
import org.junit.Test
import java.util.Calendar
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for OnboardingValidationUtils
 */
class OnboardingValidationUtilsTest {

    @Test
    fun validateName_withValidName_shouldReturnSuccess() {
        // Given
        val validNames = listOf(
            "John Doe",
            "María García",
            "李小明",
            "O'Connor",
            "Jean-Pierre",
            "Al-Rahman",
            "Smith Jr.",
            "Anne-Marie"
        )

        // When & Then
        validNames.forEach { name ->
            val result = OnboardingValidationUtils.validateName(name)
            assertTrue(result is ValidationResult.Success, "Failed for name: $name")
            assertEquals(name.trim(), (result as ValidationResult.Success).data)
        }
    }

    @Test
    fun validateName_withInvalidName_shouldReturnError() {
        // Given
        val invalidNames = mapOf(
            "" to "Name is required",
            "   " to "Name is required",
            "A" to "Name must be at least 2 characters",
            "A".repeat(51) to "Name must be less than 50 characters",
            "John<script>" to "Name contains invalid characters",
            "User@123" to "Name contains invalid characters",
            "Test#Name" to "Name contains invalid characters"
        )

        // When & Then
        invalidNames.forEach { (name, expectedError) ->
            val result = OnboardingValidationUtils.validateName(name)
            assertTrue(result is ValidationResult.Error, "Should fail for name: $name")
            assertTrue((result as ValidationResult.Error).message.contains(expectedError.split(" ")[0]))
        }
    }

    @Test
    fun validateName_shouldSanitizeInput() {
        // Given
        val unsafeName = "John<>Doe&amp;"

        // When
        val result = OnboardingValidationUtils.validateName(unsafeName)

        // Then
        assertTrue(result is ValidationResult.Success)
        val sanitizedName = (result as ValidationResult.Success).data
        assertFalse(sanitizedName.contains("<>"))
        assertFalse(sanitizedName.contains("&amp;"))
        assertEquals("JohnDoe", sanitizedName)
    }

    @Test
    fun validateBirthday_withValidDate_shouldReturnSuccess() {
        // Given
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -25) // 25 years ago
        val validBirthday = calendar.time

        // When
        val result = OnboardingValidationUtils.validateBirthday(validBirthday)

        // Then
        assertTrue(result is ValidationResult.Success)
        assertEquals(validBirthday, (result as ValidationResult.Success).data)
    }

    @Test
    fun validateBirthday_withNullDate_shouldReturnError() {
        // When
        val result = OnboardingValidationUtils.validateBirthday(null)

        // Then
        assertTrue(result is ValidationResult.Error)
        assertEquals("Birthday is required", (result as ValidationResult.Error).message)
    }

    @Test
    fun validateBirthday_withFutureDate_shouldReturnError() {
        // Given
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1) // Tomorrow
        val futureDate = calendar.time

        // When
        val result = OnboardingValidationUtils.validateBirthday(futureDate)

        // Then
        assertTrue(result is ValidationResult.Error)
        assertEquals("Birthday cannot be in the future", (result as ValidationResult.Error).message)
    }

    @Test
    fun validateBirthday_withTooYoungAge_shouldReturnError() {
        // Given
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -10) // 10 years ago (too young)
        val tooYoungDate = calendar.time

        // When
        val result = OnboardingValidationUtils.validateBirthday(tooYoungDate)

        // Then
        assertTrue(result is ValidationResult.Error)
        assertTrue((result as ValidationResult.Error).message.contains("13 years old"))
    }

    @Test
    fun validateBirthday_withTooOldAge_shouldReturnError() {
        // Given
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -130) // 130 years ago (too old)
        val tooOldDate = calendar.time

        // When
        val result = OnboardingValidationUtils.validateBirthday(tooOldDate)

        // Then
        assertTrue(result is ValidationResult.Error)
        assertEquals("Please enter a valid birth date", (result as ValidationResult.Error).message)
    }

    @Test
    fun validateBirthday_withBoundaryAges_shouldWork() {
        // Given
        val calendar = Calendar.getInstance()
        
        // Test minimum age (13 years)
        calendar.add(Calendar.YEAR, -13)
        calendar.add(Calendar.DAY_OF_MONTH, -1) // Just over 13
        val minAgeDate = calendar.time
        
        // Test maximum age (120 years)
        calendar.time = Date()
        calendar.add(Calendar.YEAR, -120)
        calendar.add(Calendar.DAY_OF_MONTH, 1) // Just under 120
        val maxAgeDate = calendar.time

        // When
        val minResult = OnboardingValidationUtils.validateBirthday(minAgeDate)
        val maxResult = OnboardingValidationUtils.validateBirthday(maxAgeDate)

        // Then
        assertTrue(minResult is ValidationResult.Success)
        assertTrue(maxResult is ValidationResult.Success)
    }

    @Test
    fun validateHeight_withValidMetricHeight_shouldReturnSuccess() {
        // Given
        val validHeights = listOf(100.0, 150.0, 175.5, 200.0, 250.0)

        // When & Then
        validHeights.forEach { height ->
            val result = OnboardingValidationUtils.validateHeight(height, UnitSystem.METRIC)
            assertTrue(result is ValidationResult.Success, "Failed for height: $height cm")
            assertEquals(height, (result as ValidationResult.Success).data)
        }
    }

    @Test
    fun validateHeight_withValidImperialHeight_shouldReturnSuccess() {
        // Given
        val validHeights = listOf(36.0, 60.0, 72.0, 84.0, 96.0) // inches

        // When & Then
        validHeights.forEach { height ->
            val result = OnboardingValidationUtils.validateHeight(height, UnitSystem.IMPERIAL)
            assertTrue(result is ValidationResult.Success, "Failed for height: $height inches")
            assertEquals(height, (result as ValidationResult.Success).data)
        }
    }

    @Test
    fun validateHeight_withInvalidHeight_shouldReturnError() {
        // Given
        val invalidMetricHeights = listOf(0.0, -10.0, 50.0, 300.0)
        val invalidImperialHeights = listOf(0.0, -5.0, 30.0, 120.0)

        // When & Then
        invalidMetricHeights.forEach { height ->
            val result = OnboardingValidationUtils.validateHeight(height, UnitSystem.METRIC)
            assertTrue(result is ValidationResult.Error, "Should fail for metric height: $height")
        }

        invalidImperialHeights.forEach { height ->
            val result = OnboardingValidationUtils.validateHeight(height, UnitSystem.IMPERIAL)
            assertTrue(result is ValidationResult.Error, "Should fail for imperial height: $height")
        }
    }

    @Test
    fun validateWeight_withValidMetricWeight_shouldReturnSuccess() {
        // Given
        val validWeights = listOf(30.0, 50.5, 70.0, 100.5, 300.0)

        // When & Then
        validWeights.forEach { weight ->
            val result = OnboardingValidationUtils.validateWeight(weight, UnitSystem.METRIC)
            assertTrue(result is ValidationResult.Success, "Failed for weight: $weight kg")
            assertEquals(weight, (result as ValidationResult.Success).data)
        }
    }

    @Test
    fun validateWeight_withValidImperialWeight_shouldReturnSuccess() {
        // Given
        val validWeights = listOf(66.0, 120.5, 180.0, 250.5, 660.0)

        // When & Then
        validWeights.forEach { weight ->
            val result = OnboardingValidationUtils.validateWeight(weight, UnitSystem.IMPERIAL)
            assertTrue(result is ValidationResult.Success, "Failed for weight: $weight lbs")
            assertEquals(weight, (result as ValidationResult.Success).data)
        }
    }

    @Test
    fun validateWeight_withInvalidWeight_shouldReturnError() {
        // Given
        val invalidMetricWeights = listOf(0.0, -10.0, 20.0, 350.0)
        val invalidImperialWeights = listOf(0.0, -5.0, 50.0, 700.0)

        // When & Then
        invalidMetricWeights.forEach { weight ->
            val result = OnboardingValidationUtils.validateWeight(weight, UnitSystem.METRIC)
            assertTrue(result is ValidationResult.Error, "Should fail for metric weight: $weight")
        }

        invalidImperialWeights.forEach { weight ->
            val result = OnboardingValidationUtils.validateWeight(weight, UnitSystem.IMPERIAL)
            assertTrue(result is ValidationResult.Error, "Should fail for imperial weight: $weight")
        }
    }

    @Test
    fun validateGender_withValidGender_shouldReturnSuccess() {
        // Given
        val validGenders = Gender.values()

        // When & Then
        validGenders.forEach { gender ->
            val result = OnboardingValidationUtils.validateGender(gender)
            assertTrue(result is ValidationResult.Success, "Failed for gender: $gender")
            assertEquals(gender, (result as ValidationResult.Success).data)
        }
    }

    @Test
    fun validateGender_withNullGender_shouldReturnError() {
        // When
        val result = OnboardingValidationUtils.validateGender(null)

        // Then
        assertTrue(result is ValidationResult.Error)
        assertEquals("Please select a gender option", (result as ValidationResult.Error).message)
    }

    @Test
    fun validateOnboardingData_withAllValidData_shouldReturnNoErrors() {
        // Given
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -25)
        val validBirthday = calendar.time

        // When
        val result = OnboardingValidationUtils.validateOnboardingData(
            name = "John Doe",
            birthday = validBirthday,
            height = 175.0,
            weight = 70.0,
            gender = Gender.MALE,
            unitSystem = UnitSystem.METRIC
        )

        // Then
        assertFalse(result.hasErrors())
    }

    @Test
    fun validateOnboardingData_withAllInvalidData_shouldReturnAllErrors() {
        // When
        val result = OnboardingValidationUtils.validateOnboardingData(
            name = "",
            birthday = null,
            height = 0.0,
            weight = 0.0,
            gender = null,
            unitSystem = UnitSystem.METRIC
        )

        // Then
        assertTrue(result.hasErrors())
        assertNotNull(result.nameError)
        assertNotNull(result.birthdayError)
        assertNotNull(result.heightError)
        assertNotNull(result.weightError)
        assertNotNull(result.genderError)
        assertEquals(5, result.getAllErrors().size)
    }

    @Test
    fun validateField_shouldUpdateSpecificFieldError() {
        // Given
        val initialErrors = ValidationErrors(nameError = "Old error")

        // When
        val updatedErrors = OnboardingValidationUtils.validateField(
            field = ValidationField.NAME,
            value = "John Doe",
            unitSystem = UnitSystem.METRIC,
            currentErrors = initialErrors
        )

        // Then
        assertEquals(null, updatedErrors.nameError) // Should be cleared
        assertEquals(initialErrors.birthdayError, updatedErrors.birthdayError) // Should remain unchanged
    }

    @Test
    fun sanitizeName_shouldRemoveUnsafeCharacters() {
        // Given
        val unsafeNames = mapOf(
            "John<script>alert('xss')</script>Doe" to "JohnalertxssDoe",
            "User@#$%Name" to "UserName",
            "Test   Multiple   Spaces" to "Test Multiple Spaces",
            "  Leading and trailing  " to "Leading and trailing"
        )

        // When & Then
        unsafeNames.forEach { (input, expected) ->
            val result = OnboardingValidationUtils.sanitizeName(input)
            assertEquals(expected, result, "Failed for input: $input")
        }
    }

    @Test
    fun parseImperialHeight_withValidFormats_shouldReturnCorrectInches() {
        // Given
        val validFormats = mapOf(
            "5'10\"" to 70.0,
            "5'10" to 70.0,
            "5 10" to 70.0,
            "6'0\"" to 72.0,
            "6'0" to 72.0,
            "6 0" to 72.0,
            "72" to 72.0, // Just inches
            "6" to 72.0   // Just feet (converted to inches)
        )

        // When & Then
        validFormats.forEach { (input, expected) ->
            val result = OnboardingValidationUtils.parseImperialHeight(input)
            assertNotNull(result, "Failed to parse: $input")
            assertEquals(expected, result, "Incorrect result for: $input")
        }
    }

    @Test
    fun parseImperialHeight_withInvalidFormats_shouldReturnNull() {
        // Given
        val invalidFormats = listOf(
            "",
            "abc",
            "5'",
            "'10",
            "5'10'5",
            "invalid"
        )

        // When & Then
        invalidFormats.forEach { input ->
            val result = OnboardingValidationUtils.parseImperialHeight(input)
            assertEquals(null, result, "Should return null for: $input")
        }
    }

    @Test
    fun isLeapYear_shouldCorrectlyIdentifyLeapYears() {
        // Given
        val leapYears = listOf(2000, 2004, 2008, 2012, 2016, 2020, 2024)
        val nonLeapYears = listOf(1900, 2001, 2002, 2003, 2100, 2200, 2300)

        // When & Then
        leapYears.forEach { year ->
            assertTrue(OnboardingValidationUtils.isLeapYear(year), "$year should be a leap year")
        }

        nonLeapYears.forEach { year ->
            assertFalse(OnboardingValidationUtils.isLeapYear(year), "$year should not be a leap year")
        }
    }

    @Test
    fun getSupportiveErrorMessage_shouldProvideHelpfulMessages() {
        // Given
        val testCases = mapOf(
            ValidationField.NAME to "Invalid name",
            ValidationField.BIRTHDAY to "Invalid date",
            ValidationField.HEIGHT to "Invalid height",
            ValidationField.WEIGHT to "Invalid weight",
            ValidationField.GENDER to "Invalid gender"
        )

        // When & Then
        testCases.forEach { (field, error) ->
            val result = OnboardingValidationUtils.getSupportiveErrorMessage(field, error)
            assertTrue(result.contains(error), "Should contain original error")
            assertTrue(result.length > error.length, "Should add supportive prefix")
        }
    }

    @Test
    fun validateName_withUnicodeCharacters_shouldWork() {
        // Given
        val unicodeNames = listOf(
            "José María",
            "李小明",
            "محمد علي",
            "Владимир",
            "Ñoño",
            "François",
            "Björk"
        )

        // When & Then
        unicodeNames.forEach { name ->
            val result = OnboardingValidationUtils.validateName(name)
            assertTrue(result is ValidationResult.Success, "Failed for Unicode name: $name")
        }
    }

    @Test
    fun validateHeight_withBoundaryValues_shouldWork() {
        // Given
        val metricBoundaries = listOf(100.0, 250.0) // Min and max for metric
        val imperialBoundaries = listOf(36.0, 96.0) // Min and max for imperial

        // When & Then
        metricBoundaries.forEach { height ->
            val result = OnboardingValidationUtils.validateHeight(height, UnitSystem.METRIC)
            assertTrue(result is ValidationResult.Success, "Failed for boundary metric height: $height")
        }

        imperialBoundaries.forEach { height ->
            val result = OnboardingValidationUtils.validateHeight(height, UnitSystem.IMPERIAL)
            assertTrue(result is ValidationResult.Success, "Failed for boundary imperial height: $height")
        }
    }

    @Test
    fun validateWeight_withBoundaryValues_shouldWork() {
        // Given
        val metricBoundaries = listOf(30.0, 300.0) // Min and max for metric
        val imperialBoundaries = listOf(66.0, 660.0) // Min and max for imperial

        // When & Then
        metricBoundaries.forEach { weight ->
            val result = OnboardingValidationUtils.validateWeight(weight, UnitSystem.METRIC)
            assertTrue(result is ValidationResult.Success, "Failed for boundary metric weight: $weight")
        }

        imperialBoundaries.forEach { weight ->
            val result = OnboardingValidationUtils.validateWeight(weight, UnitSystem.IMPERIAL)
            assertTrue(result is ValidationResult.Success, "Failed for boundary imperial weight: $weight")
        }
    }

    @Test
    fun validateBirthday_withLeapYearDate_shouldWork() {
        // Given
        val calendar = Calendar.getInstance()
        calendar.set(2000, Calendar.FEBRUARY, 29) // Leap year date
        val leapYearDate = calendar.time

        // When
        val result = OnboardingValidationUtils.validateBirthday(leapYearDate)

        // Then
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun validationErrors_helperMethods_shouldWork() {
        // Given
        val errors = ValidationErrors(
            nameError = "Name error",
            heightError = "Height error"
        )

        // When & Then
        assertTrue(errors.hasErrors())
        assertEquals(2, errors.getAllErrors().size)
        assertTrue(errors.getAllErrors().contains("Name error"))
        assertTrue(errors.getAllErrors().contains("Height error"))

        val clearedErrors = errors.clearAll()
        assertFalse(clearedErrors.hasErrors())

        val fieldClearedErrors = errors.clearFieldError(ValidationField.NAME)
        assertEquals(null, fieldClearedErrors.nameError)
        assertEquals("Height error", fieldClearedErrors.heightError)

        val fieldSetErrors = ValidationErrors().setFieldError(ValidationField.WEIGHT, "Weight error")
        assertEquals("Weight error", fieldSetErrors.weightError)
    }
}