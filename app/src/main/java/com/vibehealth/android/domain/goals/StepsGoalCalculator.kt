package com.vibehealth.android.domain.goals

import com.vibehealth.android.domain.user.Gender
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calculator for daily steps goals based on WHO Physical Activity Guidelines 2020.
 * 
 * This implementation follows WHO recommendations for physical activity, using 10,000 steps
 * as the baseline with age and gender-based adjustments derived from WHO Physical Activity
 * Guidelines 2020 and supporting research literature.
 * 
 * WHO Sources:
 * - WHO Physical Activity Guidelines 2020 (ISBN: 978-92-4-001512-8)
 * - WHO Global Recommendations on Physical Activity for Health (2010)
 * - Physical Activity Guidelines Advisory Committee Scientific Report (2018)
 * 
 * Research Citations:
 * - Tudor-Locke, C., et al. (2011). "How many steps/day are enough? for adults." 
 *   International Journal of Behavioral Nutrition and Physical Activity, 8(1), 79.
 * - Saint-Maurice, P. F., et al. (2020). "Association of daily step count and step intensity 
 *   with mortality among US adults." JAMA, 323(12), 1151-1160.
 * - Paluch, A. E., et al. (2022). "Daily steps and all-cause mortality: a meta-analysis 
 *   of 15 international cohorts." The Lancet Public Health, 7(3), e219-e228.
 */
@Singleton
class StepsGoalCalculator @Inject constructor() {
    
    companion object {
        /**
         * WHO baseline recommendation of 10,000 steps per day for adults.
         * This represents approximately 150 minutes of moderate-intensity activity per week
         * as recommended by WHO Physical Activity Guidelines 2020.
         */
        private const val BASE_STEPS_GOAL = 10000
        
        /**
         * Minimum steps goal based on WHO minimum recommendations.
         * Research shows health benefits begin at 4,000-5,000 steps/day for older adults
         * and sedentary populations (Paluch et al., 2022).
         */
        private const val MIN_STEPS_GOAL = 5000
        
        /**
         * Maximum steps goal for safety and realistic achievement.
         * Based on 95th percentile of active adult populations and medical safety guidelines
         * to prevent overexertion and injury risk.
         */
        private const val MAX_STEPS_GOAL = 20000
        
        /**
         * Age threshold for youth category per WHO guidelines.
         * WHO Physical Activity Guidelines 2020 define separate recommendations for
         * children and adolescents (5-17 years) vs adults (18+ years).
         */
        private const val YOUTH_AGE_THRESHOLD = 18
        
        /**
         * Age threshold for older adults per WHO guidelines.
         * WHO guidelines provide specific considerations for adults 65+ years,
         * emphasizing capability-based adjustments while maintaining health benefits.
         */
        private const val OLDER_ADULT_AGE_THRESHOLD = 65
    }
    
    /**
     * Calculate daily steps goal based on WHO standards and user profile.
     * 
     * The calculation applies WHO Physical Activity Guidelines 2020 with age-based
     * adjustments and minimal gender-based modifications based on research data.
     * All adjustments maintain medical safety bounds and realistic achievement targets.
     * 
     * @param input Goal calculation input containing user profile data
     * @return Daily steps goal between MIN_STEPS_GOAL and MAX_STEPS_GOAL
     */
    fun calculateStepsGoal(input: GoalCalculationInput): Int {
        val baseGoal = BASE_STEPS_GOAL
        
        // Age adjustments based on WHO Physical Activity Guidelines 2020
        val ageAdjustment = calculateAgeAdjustment(input.age)
        
        // Gender adjustments based on research data (minimal, evidence-based)
        val genderAdjustment = calculateGenderAdjustment(input.gender)
        
        // Apply adjustments and ensure result is within medical safety bounds
        val calculatedGoal = (baseGoal * ageAdjustment * genderAdjustment).toInt()
        
        return calculatedGoal.coerceIn(MIN_STEPS_GOAL, MAX_STEPS_GOAL)
    }
    
    /**
     * Calculate age-based adjustment factor according to WHO guidelines.
     * 
     * WHO Physical Activity Guidelines 2020 provide different recommendations by age group:
     * - Youth (under 18): Higher activity needs for development and growth
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
                // This translates to approximately 12,000+ steps for adolescents
                1.2
            }
            age in YOUTH_AGE_THRESHOLD until OLDER_ADULT_AGE_THRESHOLD -> {
                // Standard adult recommendation per WHO Physical Activity Guidelines 2020
                // 150 minutes moderate-intensity activity per week ≈ 10,000 steps/day
                1.0
            }
            age >= OLDER_ADULT_AGE_THRESHOLD -> {
                // Older adults: adjusted for capability while maintaining health benefits
                // WHO emphasizes "as much as their abilities and conditions allow"
                // Research shows 7,000-8,000 steps provide significant health benefits for older adults
                0.8
            }
            else -> {
                // Fallback for edge cases
                1.0
            }
        }
    }
    
    /**
     * Calculate gender-based adjustment factor based on research data.
     * 
     * Adjustments are minimal and based on physiological research rather than assumptions.
     * Research indicates slight differences in optimal step counts between biological sexes
     * due to stride length and metabolic factors (Tudor-Locke et al., 2011).
     * 
     * For inclusive gender options, we use neutral adjustments to avoid assumptions
     * about biological characteristics.
     * 
     * @param gender User's gender selection
     * @return Adjustment factor to apply to base goal
     */
    private fun calculateGenderAdjustment(gender: Gender): Double {
        return when (gender) {
            Gender.MALE -> {
                // Slight increase based on research showing males may benefit from
                // marginally higher step counts due to average stride length differences
                1.05
            }
            Gender.FEMALE -> {
                // Slight decrease based on research data, but still within WHO recommendations
                // Accounts for average physiological differences while maintaining health benefits
                0.95
            }
            Gender.OTHER, Gender.PREFER_NOT_TO_SAY -> {
                // Neutral adjustment to avoid assumptions about biological characteristics
                // Uses WHO standard baseline without gender-based modifications
                1.0
            }
        }
    }
}