package com.vibehealth.android.domain.user

import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * User profile data model for Firestore
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
    val heightInCm: Double = 0.0,
    
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
        return weightInKg * 2.20462
    }
    
    /**
     * Get formatted height string based on unit system
     */
    fun getFormattedHeight(): String {
        return when (unitSystem) {
            UnitSystem.METRIC -> "${heightInCm.toInt()} cm"
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
            UnitSystem.METRIC -> "${weightInKg.toInt()} kg"
            UnitSystem.IMPERIAL -> "${getWeightInPounds().toInt()} lbs"
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
}

/**
 * Gender options for user profile
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
}

/**
 * Unit system preference
 */
enum class UnitSystem {
    @PropertyName("METRIC")
    METRIC,
    
    @PropertyName("IMPERIAL")
    IMPERIAL;
    
    fun getDisplayName(): String {
        return when (this) {
            METRIC -> "Metric (cm, kg)"
            IMPERIAL -> "Imperial (ft/in, lbs)"
        }
    }
}