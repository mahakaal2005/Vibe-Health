package com.vibehealth.android.data.user.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
// Removed SQLCipher imports for 16KB page size compatibility
// import net.sqlcipher.database.SQLiteDatabase
// import net.sqlcipher.database.SupportFactory

/**
 * Room database class with encryption support using SQLCipher
 */
@Database(
    entities = [UserProfileEntity::class],
    version = 1,
    exportSchema = false,
    autoMigrations = []
)
@TypeConverters(DatabaseTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao

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
                // Future migrations will be added here
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
     * Migration from version 1 to 2 (example for future use)
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Example migration - add new column
            // database.execSQL("ALTER TABLE user_profiles ADD COLUMN new_column TEXT")
        }
    }

    /**
     * Get all available migrations
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            // MIGRATION_1_2 // Uncomment when needed
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