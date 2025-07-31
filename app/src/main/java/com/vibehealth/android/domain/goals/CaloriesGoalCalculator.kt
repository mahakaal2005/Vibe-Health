package com.vibehealth.android.domain.goals

import com.vibehealth.android.domain.user.Gender
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calculator for daily calorie goals based on Harris-Benedict Revised (1984) equation
 * and WHO activity level recommendations.
 * 
 * This implementation uses established BMR (Basal Metabolic Rate) calculations combined
 * with activity factors to determine Total Daily Energy Expenditure (TDEE), which serves
 * as the daily calorie goal for maintaining healthy weight.
 * 
 * Research Citations:
 * - Harris, J.A. & Benedict, F.G. (1919). "A Biometric Study of Human Basal Metabolism"
 * - Roza, A.M. & Shizgal, H.M. (1984). "The Harris Benedict equation reevaluated"
 * - Mifflin, M.D., et al. (1990). "A new predictive equation for resting energy expenditure"
 * - WHO Technical Report Series 724 (1985). "Energy and protein requirements"
 * - Dietary Guidelines for Americans 2020-2025, USDA and HHS
 * 
 * Medical Sources:
 * - American Dietetic Association Position Paper on Weight Management (2009)
 * - Academy of Nutrition and Dietetics Evidence-Based Practice Guidelines
 * - WHO/FAO Expert Consultation on Human Energy Requirements (2004)
 */
@Singleton
class CaloriesGoalCalculator @Inject constructor() {
    
    companion object {
        /**
         * Minimum daily calorie intake based on medical safety guidelines.
         * This represents the minimum calories needed to support basic metabolic functions
         * and prevent malnutrition, based on WHO recommendations and clinical practice.
         */
        private const val MIN_CALORIES_GOAL = 1200
        
        /**
         * Maximum daily calorie intake for safety and realistic goals.
         * Based on 95th percentile of active adult populations and medical guidelines
         * to prevent excessive caloric intake that could lead to weight gain.
         */
        private const val MAX_CALORIES_GOAL = 4000
        
        /**
         * Harris-Benedict equation constants for males (Revised 1984).
         * These constants were refined from the original 1919 equation based on
         * modern population data and improved measurement techniques.
         */
        private const val MALE_BMR_CONSTANT = 88.362
        private const val MALE_WEIGHT_FACTOR = 13.397
        private const val MALE_HEIGHT_FACTOR = 4.799
        private const val MALE_AGE_FACTOR = 5.677
        
        /**
         * Harris-Benedict equation constants for females (Revised 1984).
         * Gender-specific constants account for physiological differences in
         * muscle mass, body composition, and metabolic rate.
         */
        private const val FEMALE_BMR_CONSTANT = 447.593
        private const val FEMALE_WEIGHT_FACTOR = 9.247
        private const val FEMALE_HEIGHT_FACTOR = 3.098
        private const val FEMALE_AGE_FACTOR = 4.330
        
        /**
         * Mifflin-St Jeor equation constants for gender-neutral calculation.
         * Used for inclusive gender options to avoid assumptions about biological sex
         * while maintaining calculation accuracy. This equation is considered more
         * accurate for modern populations than Harris-Benedict.
         */
        private const val MIFFLIN_WEIGHT_FACTOR = 10.0
        private const val MIFFLIN_HEIGHT_FACTOR = 6.25
        private const val MIFFLIN_AGE_FACTOR = 5.0
        private const val MIFFLIN_CONSTANT = 5.0
    }
    
    /**
     * Calculate daily calorie goal based on BMR and activity level.
     * 
     * The calculation follows established nutritional science:
     * 1. Calculate BMR using appropriate equation based on gender
     * 2. Multiply by activity factor to get TDEE
     * 3. Apply medical safety bounds
     * 
     * @param input Goal calculation input containing user profile data
     * @return Daily calorie goal between MIN_CALORIES_GOAL and MAX_CALORIES_GOAL
     */
    fun calculateCaloriesGoal(input: GoalCalculationInput): Int {
        // Calculate Basal Metabolic Rate using appropriate equation
        val bmr = calculateBMR(input)
        
        // Calculate Total Daily Energy Expenditure using activity factor
        val tdee = bmr * input.activityLevel.factor
        
        // Apply medical safety bounds and return as integer
        return tdee.toInt().coerceIn(MIN_CALORIES_GOAL, MAX_CALORIES_GOAL)
    }
    
    /**
     * Calculate Basal Metabolic Rate using gender-appropriate equations.
     * 
     * Uses Harris-Benedict Revised (1984) for male/female genders and
     * Mifflin-St Jeor equation for inclusive gender options to avoid
     * assumptions about biological characteristics.
     * 
     * @param input Goal calculation input with user profile data
     * @return BMR in calories per day
     */
    private fun calculateBMR(input: GoalCalculationInput): Double {
        return when (input.gender) {
            Gender.MALE -> {
                // Harris-Benedict Revised (1984) for males
                // BMR = 88.362 + (13.397 × weight in kg) + (4.799 × height in cm) - (5.677 × age in years)
                MALE_BMR_CONSTANT + 
                (MALE_WEIGHT_FACTOR * input.weightInKg) + 
                (MALE_HEIGHT_FACTOR * input.heightInCm) - 
                (MALE_AGE_FACTOR * input.age)
            }
            
            Gender.FEMALE -> {
                // Harris-Benedict Revised (1984) for females
                // BMR = 447.593 + (9.247 × weight in kg) + (3.098 × height in cm) - (4.330 × age in years)
                FEMALE_BMR_CONSTANT + 
                (FEMALE_WEIGHT_FACTOR * input.weightInKg) + 
                (FEMALE_HEIGHT_FACTOR * input.heightInCm) - 
                (FEMALE_AGE_FACTOR * input.age)
            }
            
            Gender.OTHER, Gender.PREFER_NOT_TO_SAY -> {
                // Mifflin-St Jeor equation (gender-neutral, more accurate for modern populations)
                // BMR = (10 × weight in kg) + (6.25 × height in cm) - (5 × age in years) + 5
                // This avoids assumptions about biological sex while maintaining accuracy
                (MIFFLIN_WEIGHT_FACTOR * input.weightInKg) + 
                (MIFFLIN_HEIGHT_FACTOR * input.heightInCm) - 
                (MIFFLIN_AGE_FACTOR * input.age) + 
                MIFFLIN_CONSTANT
            }
        }
    }
    
    /**
     * Get detailed calculation breakdown for transparency and debugging.
     * 
     * This method provides insight into the calculation process, useful for
     * user education, debugging, and validation against known examples.
     * 
     * @param input Goal calculation input
     * @return Detailed breakdown of the calculation steps
     */
    fun getCalculationBreakdown(input: GoalCalculationInput): CalorieCalculationBreakdown {
        val bmr = calculateBMR(input)
        val tdee = bmr * input.activityLevel.factor
        val finalGoal = tdee.toInt().coerceIn(MIN_CALORIES_GOAL, MAX_CALORIES_GOAL)
        
        return CalorieCalculationBreakdown(
            bmr = bmr,
            activityLevel = input.activityLevel,
            activityFactor = input.activityLevel.factor,
            tdee = tdee,
            finalGoal = finalGoal,
            equation = when (input.gender) {
                Gender.MALE, Gender.FEMALE -> "Harris-Benedict Revised (1984)"
                Gender.OTHER, Gender.PREFER_NOT_TO_SAY -> "Mifflin-St Jeor (1990)"
            },
            boundsApplied = tdee.toInt() != finalGoal
        )
    }
}

/**
 * Detailed breakdown of calorie calculation for transparency and validation.
 * 
 * This data class provides insight into each step of the calculation process,
 * useful for user education, debugging, and medical validation.
 */
data class CalorieCalculationBreakdown(
    /**
     * Basal Metabolic Rate in calories per day.
     * The number of calories needed for basic metabolic functions at rest.
     */
    val bmr: Double,
    
    /**
     * Activity level used in the calculation.
     */
    val activityLevel: ActivityLevel,
    
    /**
     * Activity factor multiplier applied to BMR.
     */
    val activityFactor: Double,
    
    /**
     * Total Daily Energy Expenditure before bounds checking.
     * BMR × Activity Factor = calories needed including activity.
     */
    val tdee: Double,
    
    /**
     * Final calorie goal after applying medical safety bounds.
     */
    val finalGoal: Int,
    
    /**
     * Name of the equation used for BMR calculation.
     */
    val equation: String,
    
    /**
     * Whether medical safety bounds were applied to limit the result.
     */
    val boundsApplied: Boolean
) {
    
    /**
     * Get a human-readable explanation of the calculation.
     * 
     * @return Formatted string explaining the calculation steps
     */
    fun getExplanation(): String {
        val boundsNote = if (boundsApplied) " (adjusted for medical safety)" else ""
        return """
            Calorie Goal Calculation:
            1. BMR using $equation: ${bmr.toInt()} calories/day
            2. Activity Level: ${activityLevel.description}
            3. Activity Factor: ${activityFactor}x
            4. TDEE: ${bmr.toInt()} × ${activityFactor} = ${tdee.toInt()} calories/day
            5. Final Goal: $finalGoal calories/day$boundsNote
        """.trimIndent()
    }
}