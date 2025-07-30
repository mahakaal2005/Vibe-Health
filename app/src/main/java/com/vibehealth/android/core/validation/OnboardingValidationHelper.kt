package com.vibehealth.android.core.validation

import com.vibehealth.android.core.utils.UnitConversionUtils
import com.vibehealth.android.core.utils.ConversionResult
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.common.UnitSystem
import com.vibehealth.android.domain.user.UserProfile
import com.vibehealth.android.domain.onboarding.ValidationErrors
import com.vibehealth.android.domain.onboarding.ValidationField
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive validation helper that integrates validation and unit conversion
 * with real-time feedback and caching for performance
 */
@Singleton
class OnboardingValidationHelper @Inject constructor() {

    companion object {
        private const val VALIDATION_CACHE_SIZE = 100
        private const val CACHE_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes
    }

    // Cache for validation results to improve performance
    private val validationCache = mutableMapOf<String, CachedValidationResult>()

    /**
     * Validate complete onboarding data with unit conversion
     */
    fun validateCompleteOnboardingData(
        name: String,
        birthday: Date?,
        height: String, // String to handle various input formats
        weight: String, // String to handle various input formats
        gender: Gender?,
        unitSystem: UnitSystem
    ): OnboardingValidationResult {
        
        val errors = ValidationErrors()
        var convertedHeight: Double? = null
        var convertedWeight: Double? = null

        // Validate name
        val nameResult = OnboardingValidationUtils.validateName(name)
        if (nameResult is ValidationResult.Error) {
            errors.copy(nameError = nameResult.message)
        }

        // Validate birthday
        val birthdayResult = OnboardingValidationUtils.validateBirthday(birthday)
        if (birthdayResult is ValidationResult.Error) {
            errors.copy(birthdayError = birthdayResult.message)
        }

        // Parse and validate height
        val heightParseResult = parseHeight(height, unitSystem)
        when (heightParseResult) {
            is HeightParseResult.Success -> {
                convertedHeight = heightParseResult.value
                val heightValidation = OnboardingValidationUtils.validateHeight(convertedHeight, unitSystem)
                if (heightValidation is ValidationResult.Error) {
                    errors.copy(heightError = heightValidation.message)
                }
            }
            is HeightParseResult.Error -> {
                errors.copy(heightError = heightParseResult.message)
            }
        }

        // Parse and validate weight
        val weightParseResult = parseWeight(weight)
        when (weightParseResult) {
            is WeightParseResult.Success -> {
                convertedWeight = weightParseResult.value
                val weightValidation = OnboardingValidationUtils.validateWeight(convertedWeight, unitSystem)
                if (weightValidation is ValidationResult.Error) {
                    errors.copy(weightError = weightValidation.message)
                }
            }
            is WeightParseResult.Error -> {
                errors.copy(weightError = weightParseResult.message)
            }
        }

        // Validate gender
        val genderResult = OnboardingValidationUtils.validateGender(gender)
        if (genderResult is ValidationResult.Error) {
            errors.copy(genderError = genderResult.message)
        }

        return OnboardingValidationResult(
            errors = errors,
            parsedHeight = convertedHeight,
            parsedWeight = convertedWeight,
            isValid = !errors.hasErrors()
        )
    }

    /**
     * Validate single field with caching for performance
     */
    fun validateFieldWithCache(
        field: ValidationField,
        value: String,
        unitSystem: UnitSystem,
        currentErrors: ValidationErrors
    ): ValidationErrors {
        
        val cacheKey = "${field.name}_${value}_${unitSystem.name}"
        val cachedResult = validationCache[cacheKey]
        
        // Check cache validity
        if (cachedResult != null && 
            System.currentTimeMillis() - cachedResult.timestamp < CACHE_EXPIRY_MS) {
            return if (cachedResult.isValid) {
                currentErrors.clearFieldError(field)
            } else {
                currentErrors.setFieldError(field, cachedResult.errorMessage ?: "Invalid input")
            }
        }

        // Perform validation
        val result = when (field) {
            ValidationField.NAME -> {
                val nameResult = OnboardingValidationUtils.validateName(value)
                nameResult is ValidationResult.Success
            }
            ValidationField.BIRTHDAY -> {
                // For birthday, value would need to be parsed to Date first
                true // Simplified for this example
            }
            ValidationField.HEIGHT -> {
                val heightResult = parseHeight(value, unitSystem)
                if (heightResult is HeightParseResult.Success) {
                    val validation = OnboardingValidationUtils.validateHeight(heightResult.value, unitSystem)
                    validation is ValidationResult.Success
                } else {
                    false
                }
            }
            ValidationField.WEIGHT -> {
                val weightResult = parseWeight(value)
                if (weightResult is WeightParseResult.Success) {
                    val validation = OnboardingValidationUtils.validateWeight(weightResult.value, unitSystem)
                    validation is ValidationResult.Success
                } else {
                    false
                }
            }
            ValidationField.GENDER -> {
                // Gender validation would be handled differently
                true
            }
        }

        // Cache result
        cacheValidationResult(cacheKey, result, if (result) null else "Invalid input")

        return if (result) {
            currentErrors.clearFieldError(field)
        } else {
            currentErrors.setFieldError(field, "Invalid input")
        }
    }

    /**
     * Parse height input handling various formats
     */
    private fun parseHeight(heightInput: String, unitSystem: UnitSystem): HeightParseResult {
        if (heightInput.isBlank()) {
            return HeightParseResult.Error("Height is required")
        }

        return try {
            when (unitSystem) {
                UnitSystem.METRIC -> {
                    val height = heightInput.toDouble()
                    HeightParseResult.Success(height)
                }
                UnitSystem.IMPERIAL -> {
                    val parseResult = UnitConversionUtils.parseImperialHeight(heightInput)
                    when (parseResult) {
                        is ConversionResult.Success -> HeightParseResult.Success(parseResult.data)
                        is ConversionResult.Error -> HeightParseResult.Error(parseResult.message)
                    }
                }
            }
        } catch (e: NumberFormatException) {
            HeightParseResult.Error("Please enter a valid height")
        }
    }

    /**
     * Parse weight input
     */
    private fun parseWeight(weightInput: String): WeightParseResult {
        if (weightInput.isBlank()) {
            return WeightParseResult.Error("Weight is required")
        }

        return try {
            val weight = weightInput.toDouble()
            WeightParseResult.Success(weight)
        } catch (e: NumberFormatException) {
            WeightParseResult.Error("Please enter a valid weight")
        }
    }

    /**
     * Convert measurements to metric for storage
     */
    fun convertToMetricForStorage(
        height: Double,
        weight: Double,
        unitSystem: UnitSystem
    ): MetricConversionResult {
        val conversionResult = UnitConversionUtils.convertToMetric(height, weight, unitSystem)
        
        return when (conversionResult) {
            is ConversionResult.Success -> {
                val (heightInCm, weightInKg) = conversionResult.data
                MetricConversionResult.Success(heightInCm, weightInKg)
            }
            is ConversionResult.Error -> {
                MetricConversionResult.Error(conversionResult.message)
            }
        }
    }

    /**
     * Validate user profile completeness
     */
    fun validateUserProfileCompleteness(userProfile: UserProfile): ProfileCompletenessResult {
        val missingFields = mutableListOf<String>()

        if (userProfile.displayName.isBlank()) {
            missingFields.add("Name")
        }
        if (userProfile.birthday == null) {
            missingFields.add("Birthday")
        }
        if (userProfile.heightInCm <= 0) {
            missingFields.add("Height")
        }
        if (userProfile.weightInKg <= 0.0) {
            missingFields.add("Weight")
        }

        return ProfileCompletenessResult(
            isComplete = missingFields.isEmpty(),
            missingFields = missingFields,
            completionPercentage = calculateCompletionPercentage(missingFields.size)
        )
    }

    /**
     * Get validation suggestions for better user experience
     */
    fun getValidationSuggestions(field: ValidationField, currentValue: String, unitSystem: UnitSystem): List<String> {
        return when (field) {
            ValidationField.NAME -> listOf(
                "Use your full name as it appears on official documents",
                "Only letters, spaces, hyphens, and apostrophes are allowed"
            )
            ValidationField.HEIGHT -> when (unitSystem) {
                UnitSystem.METRIC -> listOf(
                    "Enter height in centimeters (e.g., 175)",
                    "Height should be between 100 and 250 cm"
                )
                UnitSystem.IMPERIAL -> listOf(
                    "Enter height as feet'inches (e.g., 5'10\")",
                    "You can also use formats like '5 10' or '70' (inches)"
                )
            }
            ValidationField.WEIGHT -> when (unitSystem) {
                UnitSystem.METRIC -> listOf(
                    "Enter weight in kilograms (e.g., 70.5)",
                    "Weight should be between 30 and 300 kg"
                )
                UnitSystem.IMPERIAL -> listOf(
                    "Enter weight in pounds (e.g., 154)",
                    "Weight should be between 66 and 660 lbs"
                )
            }
            ValidationField.BIRTHDAY -> listOf(
                "Select your birth date from the calendar",
                "You must be between 13 and 120 years old"
            )
            ValidationField.GENDER -> listOf(
                "Select the option that best represents you",
                "This information helps us provide better recommendations"
            )
        }
    }

    /**
     * Clear validation cache
     */
    fun clearCache() {
        validationCache.clear()
    }

    /**
     * Get cache statistics for monitoring
     */
    fun getCacheStats(): ValidationCacheStats {
        val now = System.currentTimeMillis()
        val validEntries = validationCache.values.count { 
            now - it.timestamp < CACHE_EXPIRY_MS 
        }
        
        return ValidationCacheStats(
            totalEntries = validationCache.size,
            validEntries = validEntries,
            expiredEntries = validationCache.size - validEntries
        )
    }

    /**
     * Cache validation result
     */
    private fun cacheValidationResult(key: String, isValid: Boolean, errorMessage: String?) {
        // Implement LRU cache behavior
        if (validationCache.size >= VALIDATION_CACHE_SIZE) {
            val oldestKey = validationCache.keys.first()
            validationCache.remove(oldestKey)
        }
        
        validationCache[key] = CachedValidationResult(
            isValid = isValid,
            errorMessage = errorMessage,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Calculate completion percentage
     */
    private fun calculateCompletionPercentage(missingFieldsCount: Int): Int {
        val totalFields = 4 // name, birthday, height, weight
        val completedFields = totalFields - missingFieldsCount
        return (completedFields * 100) / totalFields
    }
}

/**
 * Data classes for validation results
 */
data class OnboardingValidationResult(
    val errors: ValidationErrors,
    val parsedHeight: Double?,
    val parsedWeight: Double?,
    val isValid: Boolean
)

sealed class HeightParseResult {
    data class Success(val value: Double) : HeightParseResult()
    data class Error(val message: String) : HeightParseResult()
}

sealed class WeightParseResult {
    data class Success(val value: Double) : WeightParseResult()
    data class Error(val message: String) : WeightParseResult()
}

sealed class MetricConversionResult {
    data class Success(val heightInCm: Int, val weightInKg: Double) : MetricConversionResult()
    data class Error(val message: String) : MetricConversionResult()
}

data class ProfileCompletenessResult(
    val isComplete: Boolean,
    val missingFields: List<String>,
    val completionPercentage: Int
)

data class ValidationCacheStats(
    val totalEntries: Int,
    val validEntries: Int,
    val expiredEntries: Int
)

private data class CachedValidationResult(
    val isValid: Boolean,
    val errorMessage: String?,
    val timestamp: Long
)