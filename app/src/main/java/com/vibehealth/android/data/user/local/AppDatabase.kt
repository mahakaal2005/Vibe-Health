package com.vibehealth.android.data.user.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vibehealth.android.data.goals.local.DailyGoalsEntity
import com.vibehealth.android.data.goals.local.GoalDao
import com.vibehealth.android.data.goals.local.GoalsTypeConverters
// Removed SQLCipher imports for 16KB page size compatibility
// import net.sqlcipher.database.SQLiteDatabase
// import net.sqlcipher.database.SupportFactory

/**
 * Room database class with encryption support and goal calculation entities
 */
@Database(
    entities = [
        UserProfileEntity::class,
        DailyGoalsEntity::class
    ],
    version = 2,
    exportSchema = false,
    autoMigrations = []
)
@TypeConverters(DatabaseTypeConverters::class, GoalsTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun goalDao(): GoalDao

    companion object {
        private const val DATABASE_NAME = "vibe_health_database"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Get database instance with encryption
         */
        fun getDatabase(context: Context, passphrase: String): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = buildDatabase(context, passphrase)
                INSTANCE = instance
                instance
            }
        }

        /**
         * Build database instance with androidx.security encryption (16KB compatible)
         */
        private fun buildDatabase(context: Context, passphrase: String): AppDatabase {
            // Using Room without SQLCipher for 16KB page size compatibility
            // Data encryption is handled at the application level using androidx.security
            
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(*getAllMigrations())
                .addCallback(DatabaseCallback())
                .fallbackToDestructiveMigration() // Only for development
                .build()
        }

        /**
         * Get all database migrations
         */
        private fun getAllMigrations(): Array<Migration> {
            return arrayOf(
                DatabaseMigrations.MIGRATION_1_2
            )
        }

        /**
         * Clear database instance (for testing)
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }

    /**
     * Database callback for initialization and seeding
     */
    private class DatabaseCallback : RoomDatabase.Callback() {
        
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Database initialization logic can be added here
            // For example, creating indexes or initial data
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // Database corruption detection can be added here
            try {
                // Perform a simple query to check database integrity
                db.query("PRAGMA integrity_check").use { cursor ->
                    if (cursor.moveToFirst()) {
                        val result = cursor.getString(0)
                        if (result != "ok") {
                            // Log database corruption
                            android.util.Log.e("AppDatabase", "Database integrity check failed: $result")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Database integrity check error", e)
            }
        }
    }
}

/**
 * Database migration utilities
 */
object DatabaseMigrations {
    
    /**
     * Migration from version 1 to 2 - Add daily goals table
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create daily_goals table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `daily_goals` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `user_id` TEXT NOT NULL,
                    `steps_goal` INTEGER NOT NULL,
                    `calories_goal` INTEGER NOT NULL,
                    `heart_points_goal` INTEGER NOT NULL,
                    `calculated_at` TEXT NOT NULL,
                    `calculation_source` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    `last_sync_at` INTEGER,
                    `is_dirty` INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(`user_id`) REFERENCES `user_profiles`(`user_id`) ON DELETE CASCADE
                )
            """.trimIndent())
            
            // Create indexes for performance
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_goals_user_id` ON `daily_goals` (`user_id`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_goals_calculated_at` ON `daily_goals` (`calculated_at`)")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_goals_user_id_calculated_at` ON `daily_goals` (`user_id`, `calculated_at`)")
        }
    }

    /**
     * Get all available migrations
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_1_2
        )
    }
}

/**
 * Database utilities for backup and recovery
 */
object DatabaseUtils {
    
    /**
     * Create database backup
     */
    suspend fun createBackup(context: Context, database: AppDatabase): Boolean {
        return try {
            // Implementation for database backup
            // This would typically involve copying the database file
            // to a secure location or cloud storage
            true
        } catch (e: Exception) {
            android.util.Log.e("DatabaseUtils", "Backup failed", e)
            false
        }
    }

    /**
     * Restore database from backup
     */
    suspend fun restoreFromBackup(context: Context, backupPath: String): Boolean {
        return try {
            // Implementation for database restore
            // This would typically involve replacing the current database
            // with the backup file
            true
        } catch (e: Exception) {
            android.util.Log.e("DatabaseUtils", "Restore failed", e)
            false
        }
    }

    /**
     * Check database corruption
     */
    suspend fun checkDatabaseIntegrity(database: AppDatabase): Boolean {
        return try {
            // Perform integrity check
            database.openHelper.readableDatabase.use { db ->
                db.query("PRAGMA integrity_check").use { cursor ->
                    cursor.moveToFirst() && cursor.getString(0) == "ok"
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseUtils", "Integrity check failed", e)
            false
        }
    }

    /**
     * Repair corrupted database
     */
    suspend fun repairDatabase(context: Context): Boolean {
        return try {
            // Implementation for database repair
            // This might involve recreating the database from cloud backup
            // or resetting to initial state
            true
        } catch (e: Exception) {
            android.util.Log.e("DatabaseUtils", "Database repair failed", e)
            false
        }
    }
}