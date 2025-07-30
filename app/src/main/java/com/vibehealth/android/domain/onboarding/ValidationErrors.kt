package com.vibehealth.android.domain.onboarding

/**
 * Data class for comprehensive form validation with field-specific errors
 */
data class ValidationErrors(
    val nameError: String? = null,
    val birthdayError: String? = null,
    val heightError: String? = null,
    val weightError: String? = null,
    val genderError: String? = null
) {
    /**
     * Check if there are any validation errors
     */
    fun hasErrors(): Boolean {
        return nameError != null || 
               birthdayError != null || 
               heightError != null || 
               weightError != null || 
               genderError != null
    }

    /**
     * Get all error messages as a list
     */
    fun getAllErrors(): List<String> {
        return listOfNotNull(nameError, birthdayError, heightError, weightError, genderError)
    }

    /**
     * Clear all errors
     */
    fun clearAll(): ValidationErrors {
        return ValidationErrors()
    }

    /**
     * Clear specific field error
     */
    fun clearFieldError(field: ValidationField): ValidationErrors {
        return when (field) {
            ValidationField.NAME -> copy(nameError = null)
            ValidationField.BIRTHDAY -> copy(birthdayError = null)
            ValidationField.HEIGHT -> copy(heightError = null)
            ValidationField.WEIGHT -> copy(weightError = null)
            ValidationField.GENDER -> copy(genderError = null)
        }
    }

    /**
     * Set specific field error
     */
    fun setFieldError(field: ValidationField, error: String): ValidationErrors {
        return when (field) {
            ValidationField.NAME -> copy(nameError = error)
            ValidationField.BIRTHDAY -> copy(birthdayError = error)
            ValidationField.HEIGHT -> copy(heightError = error)
            ValidationField.WEIGHT -> copy(weightError = error)
            ValidationField.GENDER -> copy(genderError = error)
        }
    }
}

/**
 * Enum representing validation fields
 */
enum class ValidationField {
    NAME, BIRTHDAY, HEIGHT, WEIGHT, GENDER
}