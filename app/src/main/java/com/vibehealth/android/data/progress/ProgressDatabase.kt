package com.vibehealth.android.data.progress

import androidx.room.*
import androidx.room.TypeConverters
import com.vibehealth.android.data.goals.local.GoalsTypeConverters
import com.vibehealth.android.domain.user.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Room database for offline-first progress data caching
 */
@Database(
    entities = [
        WeeklyProgressCacheEntity::class,
        DailyProgressCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(GoalsTypeConverters::class, ProgressTypeConverters::class)
abstract class ProgressDatabase : RoomDatabase() {
    abstract fun weeklyProgressDao(): WeeklyProgressDao
    abstract fun dailyProgressDao(): DailyProgressDao
}

/**
 * Entity for caching weekly progress data
 */
@Entity(tableName = "weekly_progress_cache")
data class WeeklyProgressCacheEntity(
    @PrimaryKey
    val weekStartDate: LocalDate,
    val totalSteps: Int,
    val totalCalories: Double,
    val totalHeartPoints: Int,
    val activeDays: Int,
    val celebratoryMessages: List<String>,
    val lastUpdated: Long,
    val syncStatus: SyncStatus,
    val syncReason: String? = null
)

/**
 * Entity for caching daily progress data
 */
@Entity(
    tableName = "daily_progress_cache",
    primaryKeys = ["date", "weekStartDate"],
    foreignKeys = [
        ForeignKey(
            entity = WeeklyProgressCacheEntity::class,
            parentColumns = ["weekStartDate"],
            childColumns = ["weekStartDate"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DailyProgressCacheEntity(
    val date: LocalDate,
    val weekStartDate: LocalDate,
    val steps: Int,
    val calories: Double,
    val heartPoints: Int,
    val hasActivity: Boolean,
    val supportiveContext: String,
    val lastUpdated: Long
)

/**
 * DAO for weekly progress cache operations
 */
@Dao
interface WeeklyProgressDao {
    
    @Query("SELECT * FROM weekly_progress_cache WHERE weekStartDate = :weekStartDate")
    suspend fun getWeeklyProgress(weekStartDate: LocalDate): WeeklyProgressCacheEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(weeklyProgress: WeeklyProgressCacheEntity)
    
    @Query("UPDATE weekly_progress_cache SET syncStatus = :status, syncReason = :reason WHERE weekStartDate = :weekStartDate")
    suspend fun updateSyncStatus(weekStartDate: LocalDate, status: SyncStatus, reason: String?)
    
    @Query("SELECT * FROM weekly_progress_cache WHERE syncStatus = 'NEEDS_SYNC' ORDER BY weekStartDate DESC")
    suspend fun getDataNeedingSync(): List<WeeklyProgressCacheEntity>
    
    @Query("DELETE FROM weekly_progress_cache WHERE weekStartDate < :cutoffDate")
    suspend fun deleteOldData(cutoffDate: LocalDate)
    
    @Query("DELETE FROM weekly_progress_cache")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM weekly_progress_cache")
    suspend fun getTotalWeeksCount(): Int
    
    @Query("SELECT COUNT(*) FROM weekly_progress_cache WHERE syncStatus = 'SYNCED'")
    suspend fun getSyncedWeeksCount(): Int
    
    @Query("SELECT COUNT(*) FROM weekly_progress_cache WHERE syncStatus = 'NEEDS_SYNC'")
    suspend fun getNeedsSyncWeeksCount(): Int
    
    @Query("SELECT MIN(weekStartDate) FROM weekly_progress_cache")
    suspend fun getOldestDataDate(): LocalDate?
    
    @Query("SELECT MAX(weekStartDate) FROM weekly_progress_cache")
    suspend fun getNewestDataDate(): LocalDate?
    
    @Query("SELECT * FROM weekly_progress_cache ORDER BY weekStartDate DESC LIMIT :limit")
    suspend fun getRecentWeeks(limit: Int): List<WeeklyProgressCacheEntity>
    
    @Query("SELECT * FROM weekly_progress_cache ORDER BY weekStartDate DESC")
    fun observeAllWeeklyProgress(): Flow<List<WeeklyProgressCacheEntity>>
}

/**
 * DAO for daily progress cache operations
 */
@Dao
interface DailyProgressDao {
    
    @Query("SELECT * FROM daily_progress_cache WHERE weekStartDate = :weekStartDate ORDER BY date ASC")
    suspend fun getDailyProgressForWeek(weekStartDate: LocalDate): List<DailyProgressCacheEntity>
    
    @Query("SELECT * FROM daily_progress_cache WHERE date = :date")
    suspend fun getDailyProgress(date: LocalDate): DailyProgressCacheEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(dailyProgress: DailyProgressCacheEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(dailyProgress: List<DailyProgressCacheEntity>)
    
    @Query("DELETE FROM daily_progress_cache WHERE weekStartDate < :cutoffDate")
    suspend fun deleteOldData(cutoffDate: LocalDate)
    
    @Query("DELETE FROM daily_progress_cache")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM daily_progress_cache WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getDailyProgressInRange(startDate: LocalDate, endDate: LocalDate): List<DailyProgressCacheEntity>
    
    @Query("SELECT * FROM daily_progress_cache WHERE hasActivity = 1 ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentActiveDays(limit: Int): List<DailyProgressCacheEntity>
    
    @Query("SELECT COUNT(*) FROM daily_progress_cache WHERE hasActivity = 1")
    suspend fun getTotalActiveDaysCount(): Int
    
    @Query("SELECT * FROM daily_progress_cache ORDER BY date DESC")
    fun observeAllDailyProgress(): Flow<List<DailyProgressCacheEntity>>
}

/**
 * Type converters for progress-specific data types
 */
class ProgressTypeConverters {
    
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toSyncStatus(status: String): SyncStatus {
        return SyncStatus.valueOf(status)
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString("|")
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split("|")
    }
    
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String {
        return date.toString()
    }
    
    @TypeConverter
    fun toLocalDate(dateString: String): LocalDate {
        return LocalDate.parse(dateString)
    }
}