package com.vibehealth.android.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.vibehealth.android.data.goals.local.DailyGoalsEntity
import com.vibehealth.android.data.goals.local.GoalDao
import com.vibehealth.android.data.goals.local.GoalsTypeConverters
import com.vibehealth.android.data.user.local.UserProfileEntity
import com.vibehealth.android.data.user.local.UserProfileDao

@Database(
    entities = [
        DailyGoalsEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(GoalsTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun goalDao(): GoalDao
    abstract fun userProfileDao(): UserProfileDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vibe_health_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}