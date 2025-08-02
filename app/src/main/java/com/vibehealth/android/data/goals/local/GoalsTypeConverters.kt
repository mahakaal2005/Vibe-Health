package com.vibehealth.android.data.goals.local

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * VIBE_FIX: Phase 2 - Type converters for Room database
 */
class GoalsTypeConverters {
    
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(formatter)
    }
    
    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let { LocalDateTime.parse(it, formatter) }
    }
    
    @TypeConverter
    fun fromGoalCalculationSource(source: GoalCalculationSource): String {
        return source.name
    }
    
    @TypeConverter
    fun toGoalCalculationSource(sourceName: String): GoalCalculationSource {
        return GoalCalculationSource.valueOf(sourceName)
    }
}

