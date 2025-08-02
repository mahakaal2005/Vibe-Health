package com.vibehealth.android.domain.goals

import java.time.LocalDateTime

/**
 * VIBE_FIX: Phase 3 - Domain model for daily wellness goals
 */
data class DailyGoals(
    val userId: String,
    val stepsGoal: Int,
    val caloriesGoal: Int,
    val heartPointsGoal: Int,
    val calculationSource: CalculationSource,
    val calculatedAt: LocalDateTime,
    val isValid: Boolean = true,
    val isFresh: Boolean = true
) {
    companion object {
        fun createDefault(userId: String): DailyGoals {
            return DailyGoals(
                userId = userId,
                stepsGoal = 10000,
                caloriesGoal = 2000,
                heartPointsGoal = 30,
                calculationSource = CalculationSource.DEFAULT,
                calculatedAt = LocalDateTime.now()
            )
        }
    }
    
    fun withUpdatedTimestamp(): DailyGoals {
        return copy(
            calculatedAt = LocalDateTime.now(),
            isFresh = true
        )
    }
    
    // VIBE_FIX: Phase 3 - Additional utility methods
    fun getSummary(): String {
        return "Steps: $stepsGoal, Calories: $caloriesGoal, Heart Points: $heartPointsGoal"
    }
    
    fun sanitizeForLogging(): String {
        return "Goals(steps=$stepsGoal, calories=$caloriesGoal, heartPoints=$heartPointsGoal, source=${calculationSource.name})"
    }
}

enum class CalculationSource {
    DEFAULT,
    PERSONALIZED,
    MANUAL,
    WHO_STANDARD,
    FALLBACK_DEFAULT,
    USER_ADJUSTED;
    
    fun getDisplayName(): String {
        return when (this) {
            DEFAULT -> "Default Goals"
            PERSONALIZED -> "Personalized Goals"
            MANUAL -> "Manual Goals"
            WHO_STANDARD -> "WHO Standard"
            FALLBACK_DEFAULT -> "Fallback Default"
            USER_ADJUSTED -> "User Adjusted"
        }
    }
}