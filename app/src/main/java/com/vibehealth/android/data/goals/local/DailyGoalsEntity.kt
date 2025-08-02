package com.vibehealth.android.data.goals.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * VIBE_FIX: Phase 2 - Room entity for daily goals
 */
@Entity(tableName = "daily_goals")
data class DailyGoalsEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val stepsGoal: Int,
    val caloriesGoal: Int,
    val heartPointsGoal: Int,
    val calculationSource: GoalCalculationSource,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isActive: Boolean = true
) {
    companion object {
        fun create(
            userId: String,
            stepsGoal: Int = 10000,
            caloriesGoal: Int = 2000,
            heartPointsGoal: Int = 30,
            calculationSource: GoalCalculationSource = GoalCalculationSource.DEFAULT
        ): DailyGoalsEntity {
            val now = LocalDateTime.now()
            return DailyGoalsEntity(
                id = "${userId}_${now.toLocalDate()}",
                userId = userId,
                stepsGoal = stepsGoal,
                caloriesGoal = caloriesGoal,
                heartPointsGoal = heartPointsGoal,
                calculationSource = calculationSource,
                createdAt = now,
                updatedAt = now
            )
        }
        
        // VIBE_FIX: Phase 3 - Companion function for domain to entity conversion
        fun fromDomainModel(domainGoals: com.vibehealth.android.domain.goals.DailyGoals, isDirty: Boolean = false): DailyGoalsEntity {
            return DailyGoalsEntity(
                id = "${domainGoals.userId}_${domainGoals.calculatedAt.toLocalDate()}",
                userId = domainGoals.userId,
                stepsGoal = domainGoals.stepsGoal,
                caloriesGoal = domainGoals.caloriesGoal,
                heartPointsGoal = domainGoals.heartPointsGoal,
                calculationSource = when (domainGoals.calculationSource) {
                    com.vibehealth.android.domain.goals.CalculationSource.DEFAULT -> GoalCalculationSource.DEFAULT
                    com.vibehealth.android.domain.goals.CalculationSource.PERSONALIZED -> GoalCalculationSource.PERSONALIZED
                    com.vibehealth.android.domain.goals.CalculationSource.MANUAL -> GoalCalculationSource.MANUAL
                    com.vibehealth.android.domain.goals.CalculationSource.WHO_STANDARD -> GoalCalculationSource.WHO_STANDARD
                    com.vibehealth.android.domain.goals.CalculationSource.FALLBACK_DEFAULT -> GoalCalculationSource.FALLBACK_DEFAULT
                    com.vibehealth.android.domain.goals.CalculationSource.USER_ADJUSTED -> GoalCalculationSource.USER_ADJUSTED
                },
                createdAt = domainGoals.calculatedAt,
                updatedAt = domainGoals.calculatedAt,
                isActive = domainGoals.isValid
            )
        }
    }
    
    // VIBE_FIX: Phase 3 - Extension methods for domain conversion
    fun toDomainModel(): com.vibehealth.android.domain.goals.DailyGoals {
        return com.vibehealth.android.domain.goals.DailyGoals(
            userId = userId,
            stepsGoal = stepsGoal,
            caloriesGoal = caloriesGoal,
            heartPointsGoal = heartPointsGoal,
            calculationSource = when (calculationSource) {
                GoalCalculationSource.DEFAULT -> com.vibehealth.android.domain.goals.CalculationSource.DEFAULT
                GoalCalculationSource.PERSONALIZED -> com.vibehealth.android.domain.goals.CalculationSource.PERSONALIZED
                GoalCalculationSource.MANUAL -> com.vibehealth.android.domain.goals.CalculationSource.MANUAL
                GoalCalculationSource.WHO_STANDARD -> com.vibehealth.android.domain.goals.CalculationSource.WHO_STANDARD
                GoalCalculationSource.FALLBACK_DEFAULT -> com.vibehealth.android.domain.goals.CalculationSource.FALLBACK_DEFAULT
                GoalCalculationSource.USER_ADJUSTED -> com.vibehealth.android.domain.goals.CalculationSource.USER_ADJUSTED
            },
            calculatedAt = createdAt,
            isValid = isActive,
            isFresh = true
        )
    }
}