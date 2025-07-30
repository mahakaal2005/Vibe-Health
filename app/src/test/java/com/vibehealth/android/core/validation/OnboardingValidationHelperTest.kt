package com.vibehealth.android.core.validation

import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.UnitSystem
import com.vibehealth.android.domain.user.UserProfile
import com.vibehealth.android.domain.onboarding.ValidationField
import com.vibehealth.android.domain.onboarding.ValidationErrors
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for OnboardingValidationHelper
 */
class OnboardingValidationHelperTest {

    private lateinit var validationHelper: OnboardingValidationHelper

    @Before
    fun setup() {
        validationHelper = OnboardingValidationHelper()
    }

    @Test
    fun validateCompleteOnboardingData_withValidData_shouldReturnNoErrors() {
        // Given
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -25)
        val validBirthday = calendar.time

        // When
        val result = validationHelper.validateCompleteOnboardingData(
            name = "John Doe",
            birthday = validBirthday,
            height = "175",
            weight = "70.5",
            gender = Gender.MALE,
            unitSystem = UnitSystem.METRIC
        )

        // Then
        assertTrue(result.isValid)
        assertFalse(result.errors.hasErrors())
        assertNotNull(result.parsedHeight)
        assertNotNull(result.parsedWeight)
        assertEquals(175.0, result.parsedHeight)
        assertEquals(70.5, result.parsedWeight)
    }

    @Test
    fun validateCompleteOnboardingData_withImperialUnits_shouldParseCorrectly() {
        // Given
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -25)
        val validBirthday = calendar.time

        // When
        val result = validationHelper.validateCompleteOnboardingData(
            name = "Jane Doe",
            birthday = validBirthday,
            height = "5'8\"",
            weight = "140",
            gender = Gender.FEMALE,
            unitSystem = UnitSystem.IMPERIAL
        )

        // Then
        assertTrue(result.isValid)
        assertNotNull(result.parsedHeight)
        assertNotNull(result.parsedWeight)
        assertEquals(68.0, result.parsedHeight) // 5'8" = 68 inches
        assertEquals(140.0, result.parsedWeight)
    }

    @Test
    fun validateCompleteOnboardingData_withInvalidData_shouldReturnErrors() {
        // When
        val result = validationHelper.validateCompleteOnboardingData(
            name = "",
            birthday = null,
            height = "invalid",
            weight = "abc",
            gender = null,
            unitSystem = UnitSystem.METRIC
        )

        // Then
        assertFalse(result.isValid)
        assertTrue(result.errors.hasErrors())
        assertEquals(5, result.errors.getAllErrors().size)
    }

    @Test
    fun validateCompleteOnboardingData_withInvalidHeightFormat_shouldReturnHeightError() {
        // Given
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -25)
        val validBirthday = calendar.time

        // When
        val result = validationHelper.validateCompleteOnboardingData(
            name = "John Doe",
            birthday = validBirthday,
            height = "invalid_height",
            weight = "70",
            gender = Gender.MALE,
            unitSystem = UnitSystem.METRIC
        )

        // Then
        assertFalse(result.isValid)
        assertNotNull(result.errors.heightError)
        assertEquals(null, result.parsedHeight)
    }

    @Test
    fun validateCompleteOnboardingData_withInvalidWeightFormat_shouldReturnWeightError() {
        // Given
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -25)
        val validBirthday = calendar.time

        // When
        val result = validationHelper.validateCompleteOnboardingData(
            name = "John Doe",
            birthday = validBirthday,
            height = "175",
            weight = "invalid_weight",
            gender = Gender.MALE,
            unitSystem = UnitSystem.METRIC
        )

        // Then
        assertFalse(result.isValid)
        assertNotNull(result.errors.weightError)
        assertEquals(null, result.parsedWeight)
    }

    @Test
    fun validateFieldWithCache_shouldCacheResults() {
        // Given
        val initialErrors = ValidationErrors()
        val fieldValue = "John Doe"

        // When - First validation (should compute)
        val result1 = validationHelper.validateFieldWithCache(
            ValidationField.NAME,
            fieldValue,
            UnitSystem.METRIC,
            initialErrors
        )

        // When - Second validation (should use cache)
        val result2 = validationHelper.validateFieldWithCache(
            ValidationField.NAME,
            fieldValue,
            UnitSystem.METRIC,
            initialErrors
        )

        // Then
        assertEquals(result1.nameError, result2.nameError)
        
        // Verify cache has entries
        val cacheStats = validationHelper.getCacheStats()
        assertTrue(cacheStats.totalEntries > 0)
    }

    @Test
    fun convertToMetricForStorage_withImperialInput_shouldConvertCorrectly() {
        // Given
        val imperialHeight = 70.0 // inches
        val imperialWeight = 154.0 // lbs

        // When
        val result = validationHelper.convertToMetricForStorage(
            imperialHeight,
            imperialWeight,
            UnitSystem.IMPERIAL
        )

        // Then
        assertTrue(result is MetricConversionResult.Success)
        val (heightInCm, weightInKg) = result as MetricConversionResult.Success
        
        assertTrue(heightInCm > 170 && heightInCm < 180) // ~178 cm
        assertTrue(weightInKg > 65 && weightInKg < 75) // ~70 kg
    }

    @Test
    fun convertToMetricForStorage_withMetricInput_shouldReturnSameValues() {
        // Given
        val metricHeight = 175.0
        val metricWeight = 70.0

        // When
        val result = validationHelper.convertToMetricForStorage(
            metricHeight,
            metricWeight,
            UnitSystem.METRIC
        )

        // Then
        assertTrue(result is MetricConversionResult.Success)
        val (heightInCm, weightInKg) = result as MetricConversionResult.Success
        
        assertEquals(175, heightInCm)
        assertEquals(70.0, weightInKg)
    }

    @Test
    fun validateUserProfileCompleteness_withCompleteProfile_shouldReturnComplete() {
        // Given
        val completeProfile = UserProfile(
            userId = "test_user",
            email = "test@example.com",
            displayName = "John Doe",
            birthday = Date(),
            heightInCm = 175,
            weightInKg = 70.0,
            hasCompletedOnboarding = true
        )

        // When
        val result = validationHelper.validateUserProfileCompleteness(completeProfile)

        // Then
        assertTrue(result.isComplete)
        assertTrue(result.missingFields.isEmpty())
        assertEquals(100, result.completionPercentage)
    }

    @Test
    fun validateUserProfileCompleteness_withIncompleteProfile_shouldReturnMissingFields() {
        // Given
        val incompleteProfile = UserProfile(
            userId = "test_user",
            email = "test@example.com",
            displayName = "", // Missing
            birthday = null, // Missing
            heightInCm = 175,
            weightInKg = 0.0, // Missing (invalid)
            hasCompletedOnboarding = false
        )

        // When
        val result = validationHelper.validateUserProfileCompleteness(incompleteProfile)

        // Then
        assertFalse(result.isComplete)
        assertEquals(3, result.missingFields.size)
        assertTrue(result.missingFields.contains("Name"))
        assertTrue(result.missingFields.contains("Birthday"))
        assertTrue(result.missingFields.contains("Weight"))
        assertEquals(25, result.completionPercentage) // 1 out of 4 fields complete
    }

    @Test
    fun getValidationSuggestions_shouldReturnHelpfulSuggestions() {
        // When
        val nameSuggestions = validationHelper.getValidationSuggestions(
            ValidationField.NAME, 
            "John", 
            UnitSystem.METRIC
        )
        
        val heightSuggestions = validationHelper.getValidationSuggestions(
            ValidationField.HEIGHT, 
            "175", 
            UnitSystem.METRIC
        )
        
        val imperialHeightSuggestions = validationHelper.getValidationSuggestions(
            ValidationField.HEIGHT, 
            "5'8\"", 
            UnitSystem.IMPERIAL
        )

        // Then
        assertTrue(nameSuggestions.isNotEmpty())
        assertTrue(heightSuggestions.isNotEmpty())
        assertTrue(imperialHeightSuggestions.isNotEmpty())
        
        assertTrue(nameSuggestions.any { it.contains("full name") })
        assertTrue(heightSuggestions.any { it.contains("centimeters") })
        assertTrue(imperialHeightSuggestions.any { it.contains("feet'inches") })
    }

    @Test
    fun clearCache_shouldRemoveAllCachedEntries() {
        // Given - Add some cached entries
        validationHelper.validateFieldWithCache(
            ValidationField.NAME,
            "John Doe",
            UnitSystem.METRIC,
            ValidationErrors()
        )

        // Verify cache has entries
        val statsBefore = validationHelper.getCacheStats()
        assertTrue(statsBefore.totalEntries > 0)

        // When
        validationHelper.clearCache()

        // Then
        val statsAfter = validationHelper.getCacheStats()
        assertEquals(0, statsAfter.totalEntries)
    }

    @Test
    fun getCacheStats_shouldReturnAccurateStatistics() {
        // Given - Add some entries to cache
        repeat(5) { index ->
            validationHelper.validateFieldWithCache(
                ValidationField.NAME,
                "Name$index",
                UnitSystem.METRIC,
                ValidationErrors()
            )
        }

        // When
        val stats = validationHelper.getCacheStats()

        // Then
        assertEquals(5, stats.totalEntries)
        assertEquals(5, stats.validEntries) // All should be valid (recent)
        assertEquals(0, stats.expiredEntries)
    }

    @Test
    fun heightParseResult_shouldHandleVariousFormats() {
        // Test metric height parsing
        val metricResult = validationHelper.validateCompleteOnboardingData(
            name = "Test",
            birthday = Date(System.currentTimeMillis() - 25 * 365 * 24 * 60 * 60 * 1000L),
            height = "175.5",
            weight = "70",
            gender = Gender.MALE,
            unitSystem = UnitSystem.METRIC
        )
        
        assertEquals(175.5, metricResult.parsedHeight)

        // Test imperial height parsing
        val imperialResult = validationHelper.validateCompleteOnboardingData(
            name = "Test",
            birthday = Date(System.currentTimeMillis() - 25 * 365 * 24 * 60 * 60 * 1000L),
            height = "6'2\"",
            weight = "180",
            gender = Gender.MALE,
            unitSystem = UnitSystem.IMPERIAL
        )
        
        assertEquals(74.0, imperialResult.parsedHeight) // 6'2" = 74 inches
    }

    @Test
    fun weightParseResult_shouldHandleDecimalValues() {
        // Given
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -25)
        val validBirthday = calendar.time

        // When
        val result = validationHelper.validateCompleteOnboardingData(
            name = "Test User",
            birthday = validBirthday,
            height = "175",
            weight = "70.75",
            gender = Gender.MALE,
            unitSystem = UnitSystem.METRIC
        )

        // Then
        assertTrue(result.isValid)
        assertEquals(70.75, result.parsedWeight)
    }

    @Test
    fun validationHelper_shouldHandleEdgeCases() {
        // Test empty strings
        val emptyResult = validationHelper.validateCompleteOnboardingData(
            name = "",
            birthday = null,
            height = "",
            weight = "",
            gender = null,
            unitSystem = UnitSystem.METRIC
        )
        
        assertFalse(emptyResult.isValid)
        assertTrue(emptyResult.errors.hasErrors())

        // Test whitespace strings
        val whitespaceResult = validationHelper.validateCompleteOnboardingData(
            name = "   ",
            birthday = null,
            height = "   ",
            weight = "   ",
            gender = null,
            unitSystem = UnitSystem.METRIC
        )
        
        assertFalse(whitespaceResult.isValid)
        assertTrue(whitespaceResult.errors.hasErrors())
    }

    @Test
    fun profileCompletenessResult_shouldCalculatePercentageCorrectly() {
        // Test various completion levels
        val profiles = listOf(
            // 0% complete
            UserProfile(userId = "1", email = "test@test.com"),
            // 25% complete (1 out of 4 fields)
            UserProfile(userId = "2", email = "test@test.com", displayName = "John"),
            // 50% complete (2 out of 4 fields)
            UserProfile(userId = "3", email = "test@test.com", displayName = "John", birthday = Date()),
            // 75% complete (3 out of 4 fields)
            UserProfile(userId = "4", email = "test@test.com", displayName = "John", birthday = Date(), heightInCm = 175),
            // 100% complete (all 4 fields)
            UserProfile(userId = "5", email = "test@test.com", displayName = "John", birthday = Date(), heightInCm = 175, weightInKg = 70.0)
        )

        val expectedPercentages = listOf(0, 25, 50, 75, 100)

        profiles.forEachIndexed { index, profile ->
            val result = validationHelper.validateUserProfileCompleteness(profile)
            assertEquals(expectedPercentages[index], result.completionPercentage, 
                "Failed for profile $index")
        }
    }
}