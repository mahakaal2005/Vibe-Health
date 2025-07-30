package com.vibehealth.android.domain.user

import com.google.firebase.firestore.PropertyName
import com.vibehealth.android.domain.common.UnitSystem
import java.util.Date
import java.util.Calendar
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * User profile data model for Firestore with enhanced onboarding support
 */
data class UserProfile(
    @PropertyName("userId")
    val userId: String = "",
    
    @PropertyName("email")
    val email: String = "",
    
    @PropertyName("displayName")
    val displayName: String = "",
    
    @PropertyName("firstName")
    val firstName: String = "",
    
    @PropertyName("lastName")
    val lastName: String = "",
    
    @PropertyName("birthday")
    val birthday: Date? = null,
    
    @PropertyName("gender")
    val gender: Gender = Gender.PREFER_NOT_TO_SAY,
    
    @PropertyName("unitSystem")
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    
    @PropertyName("heightInCm")
    val heightInCm: Int = 0,
    
    @PropertyName("weightInKg")
    val weightInKg: Double = 0.0,
    
    @PropertyName("hasCompletedOnboarding")
    val hasCompletedOnboarding: Boolean = false,
    
    @PropertyName("createdAt")
    val createdAt: Date = Date(),
    
    @PropertyName("updatedAt")
    val updatedAt: Date = Date()
) {
    
    /**
     * Get calculated age from birthday
     */
    fun getAge(): Int {
        return birthday?.let { birthDate ->
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
            age
        } ?: 0
    }

    /**
     * Get height in feet and inches for display
     */
    fun getHeightInFeetInches(): Pair<Int, Int> {
        val totalInches = (heightInCm / 2.54).toInt()
        val feet = totalInches / 12
        val inches = totalInches % 12
        return Pair(feet, inches)
    }
    
    /**
     * Get weight in pounds for display
     */
    fun getWeightInPounds(): Double {
        return BigDecimal(weightInKg * 2.20462).setScale(2, RoundingMode.HALF_UP).toDouble()
    }
    
    /**
     * Get formatted height string based on unit system
     */
    fun getFormattedHeight(): String {
        return when (unitSystem) {
            UnitSystem.METRIC -> "$heightInCm cm"
            UnitSystem.IMPERIAL -> {
                val (feet, inches) = getHeightInFeetInches()
                "$feet'$inches\""
            }
        }
    }
    
    /**
     * Get formatted weight string based on unit system
     */
    fun getFormattedWeight(): String {
        return when (unitSystem) {
            UnitSystem.METRIC -> "${BigDecimal(weightInKg).setScale(1, RoundingMode.HALF_UP)} kg"
            UnitSystem.IMPERIAL -> "${getWeightInPounds()} lbs"
        }
    }
    
    /**
     * Get full name
     */
    fun getFullName(): String {
        return if (firstName.isNotBlank() && lastName.isNotBlank()) {
            "$firstName $lastName"
        } else {
            displayName.ifBlank { email }
        }
    }

    /**
     * Validate if the profile has all required onboarding data
     */
    fun isOnboardingDataComplete(): Boolean {
        return displayName.isNotBlank() &&
               birthday != null &&
               heightInCm > 0 &&
               weightInKg > 0.0
    }



    /**
     * Create a sanitized copy with PII handling guidelines
     */
    fun sanitizeForLogging(): UserProfile {
        return copy(
            email = if (email.isNotBlank()) "[EMAIL_REDACTED]" else "",
            displayName = if (displayName.isNotBlank()) "[NAME_REDACTED]" else "",
            firstName = if (firstName.isNotBlank()) "[FIRST_NAME_REDACTED]" else "",
            lastName = if (lastName.isNotBlank()) "[LAST_NAME_REDACTED]" else ""
        )
    }

    companion object {
        /**
         * Create UserProfile with data sanitization
         */
        fun createSanitized(
            userId: String,
            email: String,
            displayName: String,
            birthday: Date?,
            gender: Gender,
            unitSystem: UnitSystem,
            heightInCm: Int,
            weightInKg: Double
        ): UserProfile? {
            // Validate input data
            if (userId.isBlank() || email.isBlank() || displayName.isBlank()) {
                return null
            }

            // Validate age range (13-120 years)
            birthday?.let { birthDate ->
                val age = calculateAge(birthDate)
                if (age < 13 || age > 120) {
                    return null
                }
            }

            // Validate height range (100-250 cm)
            if (heightInCm < 100 || heightInCm > 250) {
                return null
            }

            // Validate weight range (30-300 kg)
            if (weightInKg < 30.0 || weightInKg > 300.0) {
                return null
            }

            // Sanitize display name (remove special characters, limit length)
            val sanitizedDisplayName = displayName
                .replace(Regex("[^\\p{L}\\p{N}\\s'-]"), "")
                .take(50)
                .trim()

            if (sanitizedDisplayName.length < 2) {
                return null
            }

            return UserProfile(
                userId = userId,
                email = email,
                displayName = sanitizedDisplayName,
                birthday = birthday,
                gender = gender,
                unitSystem = unitSystem,
                heightInCm = heightInCm,
                weightInKg = BigDecimal(weightInKg).setScale(2, RoundingMode.HALF_UP).toDouble(),
                hasCompletedOnboarding = true,
                createdAt = Date(),
                updatedAt = Date()
            )
        }

        /**
         * Calculate age from birth date
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
    }
}

/**
 * Gender options for user profile with inclusive choices
 */
enum class Gender {
    @PropertyName("MALE")
    MALE,
    
    @PropertyName("FEMALE")
    FEMALE,
    
    @PropertyName("OTHER")
    OTHER,
    
    @PropertyName("PREFER_NOT_TO_SAY")
    PREFER_NOT_TO_SAY;
    
    fun getDisplayName(): String {
        return when (this) {
            MALE -> "Male"
            FEMALE -> "Female"
            OTHER -> "Other"
            PREFER_NOT_TO_SAY -> "Prefer not to say"
        }
    }

    companion object {
        /**
         * Get Gender from display name
         */
        fun fromDisplayName(displayName: String): Gender? {
            return values().find { it.getDisplayName() == displayName }
        }

        /**
         * Get all display names for UI
         */
        fun getAllDisplayNames(): List<String> {
            return values().map { it.getDisplayName() }
        }
    }
}