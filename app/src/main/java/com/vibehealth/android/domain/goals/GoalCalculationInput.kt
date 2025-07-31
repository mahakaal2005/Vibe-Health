package com.vibehealth.android.domain.goals

import com.vibehealth.android.domain.user.Gender

/**
 * Input data model for goal calculations based on user profile information.
 * 
 * This data class encapsulates all the necessary user profile data required
 * for WHO-based goal calculations, with validation and type safety.
 */
data class GoalCalculationInput(
    /**
     * User's age in years, calculated from birthday.
     * Used for age-based adjustments per WHO Physical Activity Guidelines 2020.
     * Valid range: 13-120 years (validated by caller).
     */
    val age: Int,
    
    /**
     * User's gender selection for minimal physiological adjustments.
     * Based on research data for stride length and metabolic differences.
     */
    val gender: Gender,
    
    /**
     * User's height in centimeters (always stored in metric).
     * Used for BMR calculations and activity level assessments.
     * Valid range: 100-250 cm (validated by caller).
     */
    val heightInCm: Int,
    
    /**
     * User's weight in kilograms (always stored in metric).
     * Used for BMR calculations and calorie goal determination.
     * Valid range: 30-300 kg (validated by caller).
     */
    val weightInKg: Double,
    
    /**
     * User's activity level for goal adjustments.
     * Defaults to LIGHT activity level per WHO population data for urban professionals.
     */
    val activityLevel: ActivityLevel = ActivityLevel.DEFAULT_FOR_URBAN_PROFESSIONALS
) {
    
    /**
     * Validate that all input values are within reasonable ranges.
     * This provides an additional safety check beyond caller validation.
     * 
     * @return true if all values are within acceptable ranges
     */
    fun isValid(): Boolean {
        return age in 13..120 &&
               heightInCm in 100..250 &&
               weightInKg in 30.0..300.0
    }
    
    /**
     * Create a sanitized copy for logging purposes, removing sensitive data.
     * Used for debugging and monitoring while protecting user privacy.
     * 
     * @return Copy with anonymized data suitable for logging
     */
    fun sanitizeForLogging(): GoalCalculationInput {
        return copy(
            // Keep age ranges for debugging but anonymize exact values
            age = when {
                age < 18 -> -1  // Youth category
                age < 65 -> -2  // Adult category  
                else -> -3      // Older adult category
            },
            // Keep height/weight ranges but anonymize exact values
            heightInCm = when {
                heightInCm < 160 -> -1  // Below average
                heightInCm < 180 -> -2  // Average
                else -> -3              // Above average
            },
            weightInKg = when {
                weightInKg < 60 -> -1.0   // Below average
                weightInKg < 80 -> -2.0   // Average
                else -> -3.0              // Above average
            }
        )
    }
}

/**
 * Activity level enumeration based on WHO physical activity categories.
 * 
 * Each level includes a multiplier factor for BMR calculations using the
 * Harris-Benedict equation, following established medical and nutritional guidelines.
 */
enum class ActivityLevel(
    /**
     * Activity factor for BMR multiplication in calorie calculations.
     * Based on Harris-Benedict equation and WHO activity level definitions.
     */
    val factor: Double,
    
    /**
     * Human-readable description of the activity level.
     */
    val description: String
) {
    /**
     * Sedentary lifestyle with little to no exercise.
     * Typical for desk jobs with minimal physical activity.
     */
    SEDENTARY(1.2, "Little to no exercise, desk job"),
    
    /**
     * Light activity with occasional exercise.
     * Default for urban professionals per WHO population data.
     */
    LIGHT(1.375, "Light exercise 1-3 days/week"),
    
    /**
     * Moderate activity with regular exercise routine.
     * Meets WHO minimum recommendations for physical activity.
     */
    MODERATE(1.55, "Moderate exercise 3-5 days/week"),
    
    /**
     * Active lifestyle with frequent exercise.
     * Exceeds WHO minimum recommendations.
     */
    ACTIVE(1.725, "Heavy exercise 6-7 days/week"),
    
    /**
     * Very active lifestyle with intense daily exercise.
     * Athletes or individuals with physically demanding jobs.
     */
    VERY_ACTIVE(1.9, "Very heavy exercise, physical job");
    
    companion object {
        /**
         * Default activity level for urban professionals.
         * Based on WHO population data showing most urban workers fall into
         * the light activity category due to sedentary work environments
         * with some recreational activity.
         */
        val DEFAULT_FOR_URBAN_PROFESSIONALS = LIGHT
    }
}