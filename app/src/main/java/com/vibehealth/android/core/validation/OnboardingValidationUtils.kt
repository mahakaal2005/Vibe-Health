package com.vibehealth.android.core.validation

import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.common.UnitSystem
import com.vibehealth.android.domain.onboarding.ValidationErrors
import com.vibehealth.android.domain.onboarding.ValidationField
import java.util.Date
import java.util.Calendar
import java.util.regex.Pattern

/**
 * Comprehensive validation utilities for onboarding data with security considerations
 */
object OnboardingValidationUtils {

    // Validation constants
    private const val MIN_NAME_LENGTH = 2
    private const val MAX_NAME_LENGTH = 50
    private const val MIN_AGE = 13
    private const val MAX_AGE = 120
    
    // Height ranges
    private const val MIN_HEIGHT_CM = 100.0
    private const val MAX_HEIGHT_CM = 250.0
    private const val MIN_HEIGHT_INCHES = 36.0 // 3'0"
    private const val MAX_HEIGHT_INCHES = 96.0 // 8'0"
    
    // Weight ranges
    private const val MIN_WEIGHT_KG = 30.0
    private const val MAX_WEIGHT_KG = 300.0
    private const val MIN_WEIGHT_LBS = 66.0
    private const val MAX_WEIGHT_LBS = 660.0

    // Regex patterns for validation
    private val NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\s'-]{2,50}$")
    private val SANITIZATION_PATTERN = Pattern.compile("[^\\p{L}\\p{N}\\s'-]")

    /**
     * Validate display name with sanitization
     */
    fun validateName(name: String): ValidationResult<String> {
        val trimmedName = name.trim()
        
        return when {
            trimmedName.isBlank() -> ValidationResult.Error("Name is required")
            trimmedName.length < MIN_NAME_LENGTH -> ValidationResult.Error("Name must be at least $MIN_NAME_LENGTH characters")
            trimmedName.length > MAX_NAME_LENGTH -> ValidationResult.Error("Name must be less than $MAX_NAME_LENGTH characters")
            !NAME_PATTERN.matcher(trimmedName).matches() -> ValidationResult.Error("Name contains invalid characters")
            else -> {
                val sanitizedName = sanitizeName(trimmedName)
                if (sanitizedName.length < MIN_NAME_LENGTH) {
                    ValidationResult.Error("Name must contain at least $MIN_NAME_LENGTH valid characters")
                } else {
                    ValidationResult.Success(sanitizedName)
                }
            }
        }
    }

    /**
     * Validate birthday with age range checking
     */
    fun validateBirthday(birthday: Date?): ValidationResult<Date> {
        return when {
            birthday == null -> ValidationResult.Error("Birthday is required")
            birthday.after(Date()) -> ValidationResult.Error("Birthday cannot be in the future")
            else -> {
                val age = calculateAge(birthday)
                when {
                    age < MIN_AGE -> ValidationResult.Error("You must be at least $MIN_AGE years old")
                    age > MAX_AGE -> ValidationResult.Error("Please enter a valid birth date")
                    else -> ValidationResult.Success(birthday)
                }
            }
        }
    }

    /**
     * Validate height based on unit system
     */
    fun validateHeight(height: Double, unitSystem: UnitSystem): ValidationResult<Double> {
        return when {
            height <= 0 -> ValidationResult.Error("Height is required")
            unitSystem == UnitSystem.METRIC && (height < MIN_HEIGHT_CM || height > MAX_HEIGHT_CM) -> {
                ValidationResult.Error("Height must be between ${MIN_HEIGHT_CM.toInt()} and ${MAX_HEIGHT_CM.toInt()} cm")
            }
            unitSystem == UnitSystem.IMPERIAL && (height < MIN_HEIGHT_INCHES || height > MAX_HEIGHT_INCHES) -> {
                val minFeet = (MIN_HEIGHT_INCHES / 12).toInt()
                val maxFeet = (MAX_HEIGHT_INCHES / 12).toInt()
                ValidationResult.Error("Height must be between $minFeet'0\" and $maxFeet'0\"")
            }
            else -> ValidationResult.Success(height)
        }
    }

    /**
     * Validate weight based on unit system
     */
    fun validateWeight(weight: Double, unitSystem: UnitSystem): ValidationResult<Double> {
        return when {
            weight <= 0 -> ValidationResult.Error("Weight is required")
            unitSystem == UnitSystem.METRIC && (weight < MIN_WEIGHT_KG || weight > MAX_WEIGHT_KG) -> {
                ValidationResult.Error("Weight must be between ${MIN_WEIGHT_KG.toInt()} and ${MAX_WEIGHT_KG.toInt()} kg")
            }
            unitSystem == UnitSystem.IMPERIAL && (weight < MIN_WEIGHT_LBS || weight > MAX_WEIGHT_LBS) -> {
                ValidationResult.Error("Weight must be between ${MIN_WEIGHT_LBS.toInt()} and ${MAX_WEIGHT_LBS.toInt()} lbs")
            }
            else -> ValidationResult.Success(weight)
        }
    }

    /**
     * Validate gender selection
     */
    fun validateGender(gender: Gender?): ValidationResult<Gender> {
        return when (gender) {
            null -> ValidationResult.Error("Please select a gender option")
            else -> ValidationResult.Success(gender)
        }
    }

    /**
     * Comprehensive validation for all onboarding data
     */
    fun validateOnboardingData(
        name: String,
        birthday: Date?,
        height: Double,
        weight: Double,
        gender: Gender?,
        unitSystem: UnitSystem
    ): ValidationErrors {
        val nameValidation = validateName(name)
        val birthdayValidation = validateBirthday(birthday)
        val heightValidation = validateHeight(height, unitSystem)
        val weightValidation = validateWeight(weight, unitSystem)
        val genderValidation = validateGender(gender)

        return ValidationErrors(
            nameError = if (nameValidation is ValidationResult.Error) nameValidation.message else null,
            birthdayError = if (birthdayValidation is ValidationResult.Error) birthdayValidation.message else null,
            heightError = if (heightValidation is ValidationResult.Error) heightValidation.message else null,
            weightError = if (weightValidation is ValidationResult.Error) weightValidation.message else null,
            genderError = if (genderValidation is ValidationResult.Error) genderValidation.message else null
        )
    }

    /**
     * Validate specific field and return updated ValidationErrors
     */
    fun validateField(
        field: ValidationField,
        value: Any?,
        unitSystem: UnitSystem,
        currentErrors: ValidationErrors
    ): ValidationErrors {
        return when (field) {
            ValidationField.NAME -> {
                val result = validateName(value as? String ?: "")
                if (result is ValidationResult.Error) {
                    currentErrors.setFieldError(field, result.message)
                } else {
                    currentErrors.clearFieldError(field)
                }
            }
            ValidationField.BIRTHDAY -> {
                val result = validateBirthday(value as? Date)
                if (result is ValidationResult.Error) {
                    currentErrors.setFieldError(field, result.message)
                } else {
                    currentErrors.clearFieldError(field)
                }
            }
            ValidationField.HEIGHT -> {
                val result = validateHeight(value as? Double ?: 0.0, unitSystem)
                if (result is ValidationResult.Error) {
                    currentErrors.setFieldError(field, result.message)
                } else {
                    currentErrors.clearFieldError(field)
                }
            }
            ValidationField.WEIGHT -> {
                val result = validateWeight(value as? Double ?: 0.0, unitSystem)
                if (result is ValidationResult.Error) {
                    currentErrors.setFieldError(field, result.message)
                } else {
                    currentErrors.clearFieldError(field)
                }
            }
            ValidationField.GENDER -> {
                val result = validateGender(value as? Gender)
                if (result is ValidationResult.Error) {
                    currentErrors.setFieldError(field, result.message)
                } else {
                    currentErrors.clearFieldError(field)
                }
            }
        }
    }

    /**
     * Sanitize name input to prevent injection attacks and malformed data
     */
    fun sanitizeName(name: String): String {
        return SANITIZATION_PATTERN.matcher(name.trim())
            .replaceAll("")
            .take(MAX_NAME_LENGTH)
            .trim()
    }

    /**
     * Parse imperial height string (e.g., "5'10", "5 10", "5'10\"")
     */
    fun parseImperialHeight(heightString: String): Double? {
        return try {
            val cleaned = heightString.replace("\"", "").replace("'", " ").trim()
            val parts = cleaned.split(" ", "'").filter { it.isNotBlank() }
            
            when (parts.size) {
                1 -> {
                    // Just inches or just feet (assume feet if > 12)
                    val value = parts[0].toDouble()
                    if (value > 12) value else value * 12 // Convert feet to inches
                }
                2 -> {
                    // Feet and inches
                    val feet = parts[0].toDouble()
                    val inches = parts[1].toDouble()
                    feet * 12 + inches
                }
                else -> null
            }
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Calculate age from birth date with leap year handling
     */
    private fun calculateAge(birthDate: Date): Int {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        calendar.time = birthDate
        val birthYear = calendar.get(Calendar.YEAR)
        val birthMonth = calendar.get(Calendar.MONTH)
        val birthDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        var age = currentYear - birthYear
        if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
            age--
        }
        return age
    }

    /**
     * Check if a date is a leap year
     */
    fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    /**
     * Get supportive error message following Companion Principle
     */
    fun getSupportiveErrorMessage(field: ValidationField, error: String): String {
        val supportivePrefix = when (field) {
            ValidationField.NAME -> "Let's get your name right. "
            ValidationField.BIRTHDAY -> "We need your birthday to personalize your goals. "
            ValidationField.HEIGHT -> "Your height helps us calculate accurate goals. "
            ValidationField.WEIGHT -> "Your weight is important for personalized recommendations. "
            ValidationField.GENDER -> "This information helps us provide better guidance. "
        }
        return supportivePrefix + error
    }
}

/**
 * Sealed class for validation results
 */
sealed class ValidationResult<out T> {
    data class Success<T>(val data: T) : ValidationResult<T>()
    data class Error(val message: String) : ValidationResult<Nothing>()
}