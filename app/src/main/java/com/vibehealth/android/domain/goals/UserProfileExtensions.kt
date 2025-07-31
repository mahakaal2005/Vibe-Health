package com.vibehealth.android.domain.goals

import com.vibehealth.android.domain.user.UserProfile

/**
 * Extension functions for UserProfile to support goal calculations.
 * 
 * These extensions provide convenient conversion methods between UserProfile
 * and GoalCalculationInput, handling validation and data transformation.
 */

/**
 * Convert UserProfile to GoalCalculationInput for goal calculations.
 * 
 * This extension handles the conversion from the user's profile data
 * to the format required by goal calculation algorithms, including
 * age calculation from birthday and validation of input ranges.
 * 
 * @return GoalCalculationInput if profile data is valid, null otherwise
 */
fun UserProfile.toGoalCalculationInput(): GoalCalculationInput? {
    // Validate that required profile data is present
    if (!isOnboardingDataComplete()) {
        return null
    }
    
    // Calculate age from birthday
    val age = getAge()
    
    // Validate age is within reasonable bounds (13-120 years)
    if (age < 13 || age > 120) {
        return null
    }
    
    // Validate height and weight are within reasonable bounds
    if (heightInCm < 100 || heightInCm > 250) {
        return null
    }
    
    if (weightInKg < 30.0 || weightInKg > 300.0) {
        return null
    }
    
    return GoalCalculationInput(
        age = age,
        gender = gender,
        heightInCm = heightInCm,
        weightInKg = weightInKg,
        // Default to LIGHT activity level for urban professionals
        activityLevel = ActivityLevel.DEFAULT_FOR_URBAN_PROFESSIONALS
    )
}

/**
 * Check if the user profile is suitable for goal calculation.
 * 
 * This validates that all required fields are present and within
 * reasonable ranges for WHO-based goal calculations.
 * 
 * @return true if profile can be used for goal calculation
 */
fun UserProfile.isValidForGoalCalculation(): Boolean {
    return toGoalCalculationInput() != null
}