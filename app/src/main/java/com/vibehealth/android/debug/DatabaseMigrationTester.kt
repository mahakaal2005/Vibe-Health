package com.vibehealth.android.debug

import android.content.Context
import android.util.Log
import com.vibehealth.android.data.user.local.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug utility to test database migration and foreign key constraints
 */
@Singleton
class DatabaseMigrationTester @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Test database migration and foreign key constraints
     */
    suspend fun testDatabaseMigration(): Boolean {
        return try {
            Log.d("DatabaseMigrationTester", "🧪 Testing database migration and foreign key constraints...")
            
            // Get database instance (this will trigger migration if needed)
            val database = AppDatabase.getDatabase(context, "test_passphrase")
            
            // Test database connection
            val userProfileDao = database.userProfileDao()
            val goalDao = database.goalDao()
            
            // Check if user profiles exist
            val profileCount = userProfileDao.getUserProfileCount()
            Log.d("DatabaseMigrationTester", "📊 User profiles in database: $profileCount")
            
            // Check if goals exist - we'll count by checking if any user has goals
            val allProfiles = userProfileDao.getAllUserProfiles()
            var totalGoalCount = 0
            for (profile in allProfiles) {
                totalGoalCount += goalDao.getGoalsCountForUser(profile.userId)
            }
            Log.d("DatabaseMigrationTester", "🎯 Goals in database: $totalGoalCount")
            
            // Test foreign key constraint by checking database pragma
            database.openHelper.readableDatabase.use { db ->
                db.query("PRAGMA foreign_keys").use { cursor ->
                    if (cursor.moveToFirst()) {
                        val foreignKeysEnabled = cursor.getInt(0) == 1
                        Log.d("DatabaseMigrationTester", "🔗 Foreign keys enabled: $foreignKeysEnabled")
                        
                        if (!foreignKeysEnabled) {
                            Log.w("DatabaseMigrationTester", "⚠️ Foreign keys are not enabled!")
                            return false
                        }
                    }
                }
                
                // Check for foreign key violations
                db.query("PRAGMA foreign_key_check").use { cursor ->
                    if (cursor.moveToFirst()) {
                        Log.w("DatabaseMigrationTester", "⚠️ Foreign key violations found:")
                        do {
                            val table = cursor.getString(0)
                            val rowId = cursor.getLong(1)
                            val parent = cursor.getString(2)
                            val fkId = cursor.getInt(3)
                            Log.w("DatabaseMigrationTester", "  - Table: $table, Row: $rowId, Parent: $parent, FK: $fkId")
                        } while (cursor.moveToNext())
                        return false
                    } else {
                        Log.d("DatabaseMigrationTester", "✅ No foreign key violations found")
                    }
                }
            }
            
            Log.d("DatabaseMigrationTester", "✅ Database migration test completed successfully")
            true
            
        } catch (e: Exception) {
            Log.e("DatabaseMigrationTester", "❌ Database migration test failed", e)
            false
        }
    }
    
    /**
     * Get database statistics
     */
    suspend fun getDatabaseStats(): MigrationStats {
        return try {
            val database = AppDatabase.getDatabase(context, "test_passphrase")
            val userProfileDao = database.userProfileDao()
            val goalDao = database.goalDao()
            
            val allProfiles = userProfileDao.getAllUserProfiles()
            var totalGoalCount = 0
            for (profile in allProfiles) {
                totalGoalCount += goalDao.getGoalsCountForUser(profile.userId)
            }
            
            MigrationStats(
                userProfileCount = userProfileDao.getUserProfileCount(),
                goalCount = totalGoalCount,
                databaseVersion = 4, // Current version
                foreignKeysEnabled = checkForeignKeysEnabled(database)
            )
        } catch (e: Exception) {
            Log.e("DatabaseMigrationTester", "Failed to get database stats", e)
            MigrationStats(0, 0, 0, false)
        }
    }
    
    private fun checkForeignKeysEnabled(database: AppDatabase): Boolean {
        return try {
            database.openHelper.readableDatabase.use { db ->
                db.query("PRAGMA foreign_keys").use { cursor ->
                    cursor.moveToFirst() && cursor.getInt(0) == 1
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Database migration statistics data class
 */
data class MigrationStats(
    val userProfileCount: Int,
    val goalCount: Int,
    val databaseVersion: Int,
    val foreignKeysEnabled: Boolean
) {
    override fun toString(): String {
        return """
            Database Statistics:
            - User Profiles: $userProfileCount
            - Goals: $goalCount  
            - Database Version: $databaseVersion
            - Foreign Keys Enabled: $foreignKeysEnabled
        """.trimIndent()
    }
}