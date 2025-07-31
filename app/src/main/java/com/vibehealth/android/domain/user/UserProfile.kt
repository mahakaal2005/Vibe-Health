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
     * Check if the user profile is suitable for goal calculation.
     * 
     * This validates that all required fields are present and within
     * reasonable ranges for WHO-based goal calculations.
     * 
     * @return true if profile can be used for goal calculation
     */
    fun isValidForGoalCalculation(): Boolean {
        // Check if basic onboarding data is complete
        if (!isOnboardingDataComplete()) {
            return false
        }
        
        // Validate age is within reasonable bounds (13-120 years)
        val age = getAge()
        if (age < 13 || age > 120) {
            return false
        }
        
        // Validate height is within reasonable bounds (100-250 cm)
        if (heightInCm < 100 || heightInCm > 250) {
            return false
        }
        
        // Validate weight is within reasonable bounds (30-300 kg)
        if (weightInKg < 30.0 || weightInKg > 300.0) {
            return false
        }
        
        return true
    }

    /**
     * Get BMI (Body Mass Index) calculation.
     * 
     * @return BMI value or null if height/weight are invalid
     */
    fun getBMI(): Double? {
        return if (heightInCm > 0 && weightInKg > 0) {
            val heightInMeters = heightInCm / 100.0
            weightInKg / (heightInMeters * heightInMeters)
        } else {
            null
        }
    }

    /**
     * Get BMI category based on WHO standards.
     * 
     * @return BMI category string or null if BMI cannot be calculated
     */
    fun getBMICategory(): String? {
        val bmi = getBMI() ?: return null
        
        return when {
            bmi < 18.5 -> "Underweight"
            bmi < 25.0 -> "Normal weight"
            bmi < 30.0 -> "Overweight"
            else -> "Obese"
        }
    }

    /**
     * Check if profile data has changed in ways that affect goal calculation.
     * 
     * @param other Previous version of the profile
     * @return true if goal-affecting fields have changed
     */
    fun hasGoalCalculationRelevantChanges(other: UserProfile): Boolean {
        return birthday != other.birthday ||
               gender != other.gender ||
               heightInCm != other.heightInCm ||
               weightInKg != other.weightInKg
    }

    /**
     * Get age category for goal calculation adjustments.
     * 
     * @return Age category string based on WHO guidelines
     */
    fun getAgeCategory(): String {
        val age = getAge()
        return when {
            age < 18 -> "Youth"
            age in 18..64 -> "Adult"
            age >= 65 -> "Older Adult"
            else -> "Unknown"
        }
    }

    /**
     * Convert UserProfile to GoalCalculationInput for goal calculations.
     * 
     * This method handles the conversion from the user's profile data
     * to the format required by goal calculation algorithms, including
     * age calculation from birthday and validation of input ranges.
     * 
     * @return GoalCalculationInput if profile data is valid, null otherwise
     */
    fun toGoalCalculationInput(): com.vibehealth.android.domain.goals.GoalCalculationInput? {
        // Validate that required profile data is present
        if (!isValidForGoalCalculation()) {
            return null
        }
        
        // Calculate age from birthday
        val age = getAge()
        
        return com.vibehealth.android.domain.goals.GoalCalculationInput(
            age = age,
            gender = gender,
            heightInCm = heightInCm,
            weightInKg = weightInKg,
            // Default to LIGHT activity level for urban professionals
            activityLevel = com.vibehealth.android.domain.goals.ActivityLevel.DEFAULT_FOR_URBAN_PROFESSIONALS
        )
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