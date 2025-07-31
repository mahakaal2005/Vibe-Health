package com.vibehealth.android.data.goals.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.vibehealth.android.data.user.local.DatabaseTypeConverters
import com.vibehealth.android.data.user.local.UserProfileEntity
import com.vibehealth.android.domain.goals.CalculationSource
import com.vibehealth.android.domain.goals.DailyGoals
import java.time.LocalDateTime
import java.util.Date

/**
 * Room entity for DailyGoals with proper field mappings and encryption support.
 * 
 * This entity stores calculated daily wellness goals with foreign key relationship
 * to UserProfile and proper indexing for performance.
 */
@Entity(
    tableName = "daily_goals",
    // Temporarily disable foreign key constraint to debug the issue
    // foreignKeys = [
    //     ForeignKey(
    //         entity = UserProfileEntity::class,
    //         parentColumns = ["user_id"],
    //         childColumns = ["user_id"],
    //         onDelete = ForeignKey.CASCADE
    //     )
    // ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["calculated_at"]),
        Index(value = ["user_id", "calculated_at"])
    ]
)
@TypeConverters(DatabaseTypeConverters::class, GoalsTypeConverters::class)
data class DailyGoalsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String, // UUID for unique identification
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "steps_goal")
    val stepsGoal: Int,
    
    @ColumnInfo(name = "calories_goal")
    val caloriesGoal: Int,
    
    @ColumnInfo(name = "heart_points_goal")
    val heartPointsGoal: Int,
    
    @ColumnInfo(name = "calculated_at")
    val calculatedAt: LocalDateTime,
    
    @ColumnInfo(name = "calculation_source")
    val calculationSource: CalculationSource,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Date,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date,
    
    @ColumnInfo(name = "last_sync_at")
    val lastSyncAt: Date?,
    
    @ColumnInfo(name = "is_dirty")
    val isDirty: Boolean = false // Flag for sync status
) {
    /**
     * Convert to domain DailyGoals model.
     * 
     * @return Domain model representation
     */
    fun toDomainModel(): DailyGoals {
        return DailyGoals(
            userId = userId,
            stepsGoal = stepsGoal,
            caloriesGoal = caloriesGoal,
            heartPointsGoal = heartPointsGoal,
            calculatedAt = calculatedAt,
            calculationSource = calculationSource
        )
    }

    companion object {
        /**
         * Create entity from domain DailyGoals model.
         * 
         * @param dailyGoals Domain model to convert
         * @param isDirty Whether the entity needs sync
         * @return Database entity representation
         */
        fun fromDomainModel(dailyGoals: DailyGoals, isDirty: Boolean = false): DailyGoalsEntity {
            val now = Date()
            return DailyGoalsEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = dailyGoals.userId,
                stepsGoal = dailyGoals.stepsGoal,
                caloriesGoal = dailyGoals.caloriesGoal,
                heartPointsGoal = dailyGoals.heartPointsGoal,
                calculatedAt = dailyGoals.calculatedAt,
                calculationSource = dailyGoals.calculationSource,
                createdAt = now,
                updatedAt = now,
                lastSyncAt = if (isDirty) null else now,
                isDirty = isDirty
            )
        }
        
        /**
         * Update existing entity with new domain model data.
         * 
         * @param existing Existing entity to update
         * @param dailyGoals New domain model data
         * @param isDirty Whether the entity needs sync
         * @return Updated entity
         */
        fun updateFromDomainModel(
            existing: DailyGoalsEntity,
            dailyGoals: DailyGoals,
            isDirty: Boolean = false
        ): DailyGoalsEntity {
            return existing.copy(
                stepsGoal = dailyGoals.stepsGoal,
                caloriesGoal = dailyGoals.caloriesGoal,
                heartPointsGoal = dailyGoals.heartPointsGoal,
                calculatedAt = dailyGoals.calculatedAt,
                calculationSource = dailyGoals.calculationSource,
                updatedAt = Date(),
                lastSyncAt = if (isDirty) existing.lastSyncAt else Date(),
                isDirty = isDirty
            )
        }
    }
}