package com.vibehealth.android.data.user.local

import androidx.room.TypeConverter
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.common.UnitSystem
import java.util.Date

/**
 * Type converters for Room database to handle custom types
 */
class DatabaseTypeConverters {

    /**
     * Convert Date to Long for storage
     */
    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    /**
     * Convert Long to Date from storage
     */
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }

    /**
     * Convert Gender enum to String for storage
     */
    @TypeConverter
    fun fromGender(gender: Gender): String {
        return gender.name
    }

    /**
     * Convert String to Gender enum from storage
     */
    @TypeConverter
    fun toGender(genderString: String): Gender {
        return try {
            Gender.valueOf(genderString)
        } catch (e: IllegalArgumentException) {
            Gender.PREFER_NOT_TO_SAY // Default fallback
        }
    }

    /**
     * Convert UnitSystem enum to String for storage
     */
    @TypeConverter
    fun fromUnitSystem(unitSystem: UnitSystem): String {
        return unitSystem.name
    }

    /**
     * Convert String to UnitSystem enum from storage
     */
    @TypeConverter
    fun toUnitSystem(unitSystemString: String): UnitSystem {
        return try {
            UnitSystem.valueOf(unitSystemString)
        } catch (e: IllegalArgumentException) {
            UnitSystem.METRIC // Default fallback
        }
    }
}