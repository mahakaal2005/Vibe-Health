package com.vibehealth.android.data.user.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.common.UnitSystem
import com.vibehealth.android.domain.user.UserProfile
import java.util.Date

/**
 * Room entity for UserProfile with proper field mappings and encryption support
 */
@Entity(tableName = "user_profiles")
@TypeConverters(DatabaseTypeConverters::class)
data class UserProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "email")
    val email: String,
    
    @ColumnInfo(name = "display_name")
    val displayName: String,
    
    @ColumnInfo(name = "first_name")
    val firstName: String,
    
    @ColumnInfo(name = "last_name")
    val lastName: String,
    
    @ColumnInfo(name = "birthday")
    val birthday: Date?,
    
    @ColumnInfo(name = "gender")
    val gender: Gender,
    
    @ColumnInfo(name = "unit_system")
    val unitSystem: UnitSystem,
    
    @ColumnInfo(name = "height_in_cm")
    val heightInCm: Int,
    
    @ColumnInfo(name = "weight_in_kg")
    val weightInKg: Double,
    
    @ColumnInfo(name = "has_completed_onboarding")
    val hasCompletedOnboarding: Boolean,
    
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
     * Convert to domain UserProfile
     */
    fun toDomainModel(): UserProfile {
        return UserProfile(
            userId = userId,
            email = email,
            displayName = displayName,
            firstName = firstName,
            lastName = lastName,
            birthday = birthday,
            gender = gender,
            unitSystem = unitSystem,
            heightInCm = heightInCm,
            weightInKg = weightInKg,
            hasCompletedOnboarding = hasCompletedOnboarding,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        /**
         * Create entity from domain UserProfile
         */
        fun fromDomainModel(userProfile: UserProfile, isDirty: Boolean = false): UserProfileEntity {
            return UserProfileEntity(
                userId = userProfile.userId,
                email = userProfile.email,
                displayName = userProfile.displayName,
                firstName = userProfile.firstName,
                lastName = userProfile.lastName,
                birthday = userProfile.birthday,
                gender = userProfile.gender,
                unitSystem = userProfile.unitSystem,
                heightInCm = userProfile.heightInCm,
                weightInKg = userProfile.weightInKg,
                hasCompletedOnboarding = userProfile.hasCompletedOnboarding,
                createdAt = userProfile.createdAt,
                updatedAt = userProfile.updatedAt,
                lastSyncAt = if (isDirty) null else Date(),
                isDirty = isDirty
            )
        }
    }
}