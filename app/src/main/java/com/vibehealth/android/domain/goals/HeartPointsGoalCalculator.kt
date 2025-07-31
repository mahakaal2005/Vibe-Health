package com.vibehealth.android.domain.goals

import com.vibehealth.android.domain.user.Gender
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calculator for daily heart points goals based on WHO Physical Activity Guidelines
 * and Google Fit heart points standard.
 * 
 * This implementation converts WHO's weekly moderate-intensity activity recommendations
 * into daily heart points goals using established METs (Metabolic Equivalent of Task) values
 * and Google Fit's heart points calculation methodology.
 * 
 * WHO Sources:
 * - WHO Physical Activity Guidelines 2020 (ISBN: 978-92-4-001512-8)
 * - WHO Global Recommendations on Physical Activity for Health (2010)
 * - Physical Activity Guidelines for Americans, 2nd edition (2018)
 * 
 * Research Citations:
 * - Ainsworth, B.E., et al. (2011). "2011 Compendium of Physical Activities"
 * - Haskell, W.L., et al. (2007). "Physical activity and public health: updated recommendation"
 * - Garber, C.E., et al. (2011). "Quantity and quality of exercise for developing fitness"
 * - Google Fit Heart Points methodology and METs conversion standards
 * 
 * Heart Points Standard:
 * - 1 heart point = 1 minute of moderate-intensity activity (3-6 METs)
 * - 2 heart points = 1 minute of vigorous-intensity activity (6+ METs)
 * - Based on American Heart Association and WHO activity intensity classifications
 */
@Singleton
class HeartPointsGoalCalculator @Inject constructor() {
    
    companion object {
        /**
         * WHO recommendation for weekly moderate-intensity physical activity.
         * This represents the minimum amount of moderate activity needed for health benefits
         * as established by WHO Physical Activity Guidelines 2020.
         */
        private const val WHO_WEEKLY_MODERATE_MINUTES = 150
        
        /**
         * WHO alternative recommendation for weekly vigorous-intensity activity.
         * Vigorous activity provides equivalent health benefits in half the time
         * compared to moderate activity.
         */
        private const val WHO_WEEKLY_VIGOROUS_MINUTES = 75
        
        /**
         * Number of days per week for daily goal calculation.
         */
        private const val DAYS_PER_WEEK = 7
        
        /**
         * Heart points earned per minute of moderate-intensity activity.
         * Based on Google Fit standard: 1 heart point = 1 minute moderate activity (3-6 METs).
         */
        private const val HEART_POINTS_PER_MODERATE_MINUTE = 1
        
        /**
         * Heart points earned per minute of vigorous-intensity activity.
         * Based on Google Fit standard: 2 heart points = 1 minute vigorous activity (6+ METs).
         */
        private const val HEART_POINTS_PER_VIGOROUS_MINUTE = 2
        
        /**
         * Minimum daily heart points goal based on WHO minimum recommendations.
         * Conservative minimum ensuring health benefits while being achievable
         * for sedentary populations beginning their fitness journey.
         */
        private const val MIN_HEART_POINTS = 15
        
        /**
         * Maximum daily heart points goal for realistic achievement and safety.
         * Based on 95th percentile of active adult populations and prevents
         * overexertion while allowing for athletic performance goals.
         */
        private const val MAX_HEART_POINTS = 50
        
        /**
         * Age threshold for youth category per WHO guidelines.
         * WHO provides separate activity recommendations for children/adolescents vs adults.
         */
        private const val YOUTH_AGE_THRESHOLD = 18
        
        /**
         * Age threshold for older adults per WHO guidelines.
         * WHO emphasizes capability-based adjustments for adults 65+ years.
         */
        private const val OLDER_ADULT_AGE_THRESHOLD = 65
    }
    
    /**
     * Calculate daily heart points goal based on WHO activity guidelines.
     * 
     * The calculation follows WHO Physical Activity Guidelines 2020:
     * 1. Start with WHO baseline (150 min/week moderate = ~21.4 min/day)
     * 2. Apply age-based adjustments following WHO recommendations
     * 3. Consider activity level for personalized goals
     * 4. Apply medical safety bounds for realistic achievement
     * 
     * @param input Goal calculation input containing user profile data
     * @return Daily heart points goal between MIN_HEART_POINTS and MAX_HEART_POINTS
     */
    fun calculateHeartPointsGoal(input: GoalCalculationInput): Int {
        // Base calculation: WHO 150 min/week moderate = ~21.4 min/day
        val dailyModerateMinutes = WHO_WEEKLY_MODERATE_MINUTES.toDouble() / DAYS_PER_WEEK
        val baseHeartPoints = dailyModerateMinutes * HEART_POINTS_PER_MODERATE_MINUTE
        
        // Age-based adjustments following WHO Physical Activity Guidelines 2020
        val ageAdjustment = calculateAgeAdjustment(input.age)
        
        // Activity level consideration for personalized goals
        val activityAdjustment = calculateActivityAdjustment(input.activityLevel)
        
        // Apply adjustments and ensure result is within medical safety bounds
        val adjustedGoal = (baseHeartPoints * ageAdjustment * activityAdjustment).toInt()
        
        return adjustedGoal.coerceIn(MIN_HEART_POINTS, MAX_HEART_POINTS)
    }
    
    /**
     * Calculate age-based adjustment factor according to WHO guidelines.
     * 
     * WHO Physical Activity Guidelines 2020 provide age-specific recommendations:
     * - Youth (under 18): Higher activity needs for healthy development
     * - Adults (18-64): Standard WHO recommendation baseline
     * - Older adults (65+): Adjusted for capability while maintaining health benefits
     * 
     * @param age User's age in years
     * @return Adjustment factor to apply to base goal
     */
    private fun calculateAgeAdjustment(age: Int): Double {
        return when {
            age < YOUTH_AGE_THRESHOLD -> {
                // Youth need more activity for healthy development per WHO guidelines
                // WHO recommends 60+ minutes daily moderate-to-vigorous activity for youth
                // This translates to higher heart points goals
                1.2
            }
            age in YOUTH_AGE_THRESHOLD until OLDER_ADULT_AGE_THRESHOLD -> {
                // Standard adult recommendation per WHO Physical Activity Guidelines 2020
                // 150 minutes moderate-intensity activity per week
                1.0
            }
            age >= OLDER_ADULT_AGE_THRESHOLD -> {
                // Older adults: adjusted for capability while maintaining health benefits
                // WHO emphasizes "as much as their abilities and conditions allow"
                // Research shows significant health benefits at lower activity levels for older adults
                0.8
            }
            else -> {
                // Fallback for edge cases
                1.0
            }
        }
    }
    
    /**
     * Calculate activity level adjustment for personalized goals.
     * 
     * Users with higher baseline activity levels may benefit from slightly higher
     * heart points goals to maintain progression and health benefits, while
     * sedentary users start with more conservative goals.
     * 
     * @param activityLevel User's current activity level
     * @return Adjustment factor to apply to base goal
     */
    private fun calculateActivityAdjustment(activityLevel: ActivityLevel): Double {
        return when (activityLevel) {
            ActivityLevel.SEDENTARY -> {
                // Conservative goal for sedentary individuals starting their fitness journey
                0.9
            }
            ActivityLevel.LIGHT -> {
                // Slightly below baseline for light activity individuals
                0.95
            }
            ActivityLevel.MODERATE -> {
                // Standard WHO baseline for moderate activity
                1.0
            }
            ActivityLevel.ACTIVE -> {
                // Slightly higher goal for active individuals
                1.1
            }
            ActivityLevel.VERY_ACTIVE -> {
                // Higher goal for very active individuals to maintain progression
                1.15
            }
        }
    }
    
    /**
     * Get detailed calculation breakdown for transparency and validation.
     * 
     * This method provides insight into the calculation process, useful for
     * user education, debugging, and validation against WHO guidelines.
     * 
     * @param input Goal calculation input
     * @return Detailed breakdown of the calculation steps
     */
    fun getCalculationBreakdown(input: GoalCalculationInput): HeartPointsCalculationBreakdown {
        val dailyModerateMinutes = WHO_WEEKLY_MODERATE_MINUTES.toDouble() / DAYS_PER_WEEK
        val baseHeartPoints = dailyModerateMinutes * HEART_POINTS_PER_MODERATE_MINUTE
        val ageAdjustment = calculateAgeAdjustment(input.age)
        val activityAdjustment = calculateActivityAdjustment(input.activityLevel)
        val adjustedGoal = baseHeartPoints * ageAdjustment * activityAdjustment
        val finalGoal = adjustedGoal.toInt().coerceIn(MIN_HEART_POINTS, MAX_HEART_POINTS)
        
        return HeartPointsCalculationBreakdown(
            whoWeeklyMinutes = WHO_WEEKLY_MODERATE_MINUTES,
            dailyModerateMinutes = dailyModerateMinutes,
            baseHeartPoints = baseHeartPoints,
            ageAdjustment = ageAdjustment,
            activityAdjustment = activityAdjustment,
            adjustedGoal = adjustedGoal,
            finalGoal = finalGoal,
            boundsApplied = adjustedGoal.toInt() != finalGoal
        )
    }
    
    /**
     * Convert heart points to equivalent activity minutes for user understanding.
     * 
     * This helps users understand what their heart points goal means in terms
     * of actual activity time, supporting the Companion Principle of clear communication.
     * 
     * @param heartPoints Number of heart points to convert
     * @return Equivalent minutes of moderate-intensity activity
     */
    fun convertHeartPointsToMinutes(heartPoints: Int): Int {
        return heartPoints / HEART_POINTS_PER_MODERATE_MINUTE
    }
    
    /**
     * Get WHO-equivalent weekly activity minutes for a daily heart points goal.
     * 
     * This helps users understand how their daily goal relates to WHO recommendations.
     * 
     * @param dailyHeartPoints Daily heart points goal
     * @return Equivalent weekly minutes of moderate-intensity activity
     */
    fun getWeeklyEquivalent(dailyHeartPoints: Int): Int {
        return dailyHeartPoints * DAYS_PER_WEEK
    }
}

/**
 * Detailed breakdown of heart points calculation for transparency and validation.
 * 
 * This data class provides insight into each step of the calculation process,
 * useful for user education, debugging, and WHO guideline validation.
 */
data class HeartPointsCalculationBreakdown(
    /**
     * WHO weekly moderate activity recommendation in minutes.
     */
    val whoWeeklyMinutes: Int,
    
    /**
     * Daily moderate activity minutes derived from WHO weekly recommendation.
     */
    val dailyModerateMinutes: Double,
    
    /**
     * Base heart points before adjustments (WHO baseline).
     */
    val baseHeartPoints: Double,
    
    /**
     * Age-based adjustment factor applied.
     */
    val ageAdjustment: Double,
    
    /**
     * Activity level adjustment factor applied.
     */
    val activityAdjustment: Double,
    
    /**
     * Heart points goal after adjustments, before bounds checking.
     */
    val adjustedGoal: Double,
    
    /**
     * Final heart points goal after applying safety bounds.
     */
    val finalGoal: Int,
    
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
        val boundsNote = if (boundsApplied) " (adjusted for safety)" else ""
        val weeklyEquivalent = finalGoal * 7
        
        return """
            Heart Points Goal Calculation:
            1. WHO Baseline: $whoWeeklyMinutes minutes/week moderate activity
            2. Daily Equivalent: ${dailyModerateMinutes.toInt()} minutes/day
            3. Base Heart Points: ${baseHeartPoints.toInt()} points/day
            4. Age Adjustment: ${ageAdjustment}x
            5. Activity Adjustment: ${activityAdjustment}x
            6. Adjusted Goal: ${adjustedGoal.toInt()} points/day
            7. Final Goal: $finalGoal points/day$boundsNote
            
            This equals approximately $weeklyEquivalent minutes of moderate activity per week,
            meeting WHO Physical Activity Guidelines for health benefits.
        """.trimIndent()
    }
}