package com.vibehealth.android.data.goals.local

import androidx.room.TypeConverter
import com.vibehealth.android.domain.goals.CalculationSource
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Type converters for goals-specific types in Room database.
 * 
 * These converters handle the conversion between domain types and database-storable types
 * for goal calculation data.
 */
class GoalsTypeConverters {

    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    }

    /**
     * Convert LocalDateTime to String for storage.
     * 
     * @param dateTime LocalDateTime to convert
     * @return ISO formatted string representation
     */
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(DATE_TIME_FORMATTER)
    }

    /**
     * Convert String to LocalDateTime from storage.
     * 
     * @param dateTimeString ISO formatted string
     * @return LocalDateTime instance or null if invalid
     */
    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return try {
            dateTimeString?.let { LocalDateTime.parse(it, DATE_TIME_FORMATTER) }
        } catch (e: Exception) {
            // Log the error and return null for graceful handling
            android.util.Log.w("GoalsTypeConverters", "Failed to parse LocalDateTime: $dateTimeString", e)
            null
        }
    }

    /**
     * Convert CalculationSource enum to String for storage.
     * 
     * @param source CalculationSource enum value
     * @return String representation
     */
    @TypeConverter
    fun fromCalculationSource(source: CalculationSource): String {
        return source.name
    }

    /**
     * Convert String to CalculationSource enum from storage.
     * 
     * @param sourceString String representation
     * @return CalculationSource enum value with fallback
     */
    @TypeConverter
    fun toCalculationSource(sourceString: String): CalculationSource {
        return try {
            CalculationSource.valueOf(sourceString)
        } catch (e: IllegalArgumentException) {
            // Log the error and return fallback default
            android.util.Log.w("GoalsTypeConverters", "Unknown CalculationSource: $sourceString, using fallback", e)
            CalculationSource.FALLBACK_DEFAULT
        }
    }
}