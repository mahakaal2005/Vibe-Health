package com.vibehealth.android.debug

import android.content.Context
import android.util.Log
import com.vibehealth.android.data.user.local.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug utility specifically for foreign key constraint issues
 */
@Singleton
class ForeignKeyDebugger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Comprehensive foreign key constraint debugging
     */
    suspend fun debugForeignKeyConstraints(): ForeignKeyDebugResult {
        return try {
            Log.d("ForeignKeyDebugger", "🔍 Starting comprehensive foreign key debugging...")
            
            val database = AppDatabase.getDatabase(context, "test_passphrase")
            val userProfileDao = database.userProfileDao()
            val goalDao = database.goalDao()
            
            // 1. Check foreign key pragma settings
            val foreignKeysEnabled = database.openHelper.readableDatabase.use { db ->
                db.query("PRAGMA foreign_keys").use { cursor ->
                    cursor.moveToFirst() && cursor.getInt(0) == 1
                }
            }
            Log.d("ForeignKeyDebugger", "🔗 Foreign keys enabled: $foreignKeysEnabled")
            
            // 2. Check table schemas
            val userProfileSchema = database.openHelper.readableDatabase.use { db ->
                db.query("PRAGMA table_info(user_profiles)").use { cursor ->
                    val columns = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        val columnName = cursor.getString(1)
                        val columnType = cursor.getString(2)
                        val isPrimaryKey = cursor.getInt(5) == 1
                        columns.add("$columnName ($columnType)${if (isPrimaryKey) " PK" else ""}")
                    }
                    columns
                }
            }
            Log.d("ForeignKeyDebugger", "📋 user_profiles schema: $userProfileSchema")
            
            val dailyGoalsSchema = database.openHelper.readableDatabase.use { db ->
                db.query("PRAGMA table_info(daily_goals)").use { cursor ->
                    val columns = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        val columnName = cursor.getString(1)
                        val columnType = cursor.getString(2)
                        val isPrimaryKey = cursor.getInt(5) == 1
                        columns.add("$columnName ($columnType)${if (isPrimaryKey) " PK" else ""}")
                    }
                    columns
                }
            }
            Log.d("ForeignKeyDebugger", "📋 daily_goals schema: $dailyGoalsSchema")
            
            // 3. Check foreign key definitions
            val foreignKeyList = database.openHelper.readableDatabase.use { db ->
                db.query("PRAGMA foreign_key_list(daily_goals)").use { cursor ->
                    val foreignKeys = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        val id = cursor.getInt(0)
                        val seq = cursor.getInt(1)
                        val table = cursor.getString(2)
                        val from = cursor.getString(3)
                        val to = cursor.getString(4)
                        val onUpdate = cursor.getString(5)
                        val onDelete = cursor.getString(6)
                        val match = cursor.getString(7)
                        foreignKeys.add("FK$id: $from -> $table.$to (onDelete: $onDelete)")
                    }
                    foreignKeys
                }
            }
            Log.d("ForeignKeyDebugger", "🔗 Foreign key definitions: $foreignKeyList")
            
            // 4. Check actual data
            val allProfiles = userProfileDao.getAllUserProfiles()
            Log.d("ForeignKeyDebugger", "👥 User profiles in database:")
            allProfiles.forEach { profile ->
                Log.d("ForeignKeyDebugger", "  - ID: '${profile.userId}' (length: ${profile.userId.length})")
            }
            
            // 5. Check for foreign key violations
            val violations = database.openHelper.readableDatabase.use { db ->
                db.query("PRAGMA foreign_key_check(daily_goals)").use { cursor ->
                    val violations = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        val table = cursor.getString(0)
                        val rowId = cursor.getLong(1)
                        val parent = cursor.getString(2)
                        val fkId = cursor.getInt(3)
                        violations.add("Table: $table, Row: $rowId, Parent: $parent, FK: $fkId")
                    }
                    violations
                }
            }
            Log.d("ForeignKeyDebugger", "⚠️ Foreign key violations: $violations")
            
            // 6. Test a simple insert to see what happens
            val testUserId = if (allProfiles.isNotEmpty()) allProfiles.first().userId else "test"
            Log.d("ForeignKeyDebugger", "🧪 Testing insert with user ID: '$testUserId'")
            
            // Try to manually insert a goal to see the exact error
            val testResult = try {
                database.openHelper.writableDatabase.use { db ->
                    val sql = """
                        INSERT INTO daily_goals (
                            id, user_id, steps_goal, calories_goal, heart_points_goal,
                            calculated_at, calculation_source, created_at, updated_at, is_dirty
                        ) VALUES (
                            'test-id', ?, 7500, 1800, 21,
                            '2025-08-01T01:00:00', 'FALLBACK_DEFAULT', 
                            ${System.currentTimeMillis()}, ${System.currentTimeMillis()}, 0
                        )
                    """.trimIndent()
                    
                    db.execSQL(sql, arrayOf(testUserId))
                    "SUCCESS"
                }
            } catch (e: Exception) {
                Log.e("ForeignKeyDebugger", "❌ Test insert failed", e)
                e.message ?: "Unknown error"
            }
            
            ForeignKeyDebugResult(
                foreignKeysEnabled = foreignKeysEnabled,
                userProfileSchema = userProfileSchema,
                dailyGoalsSchema = dailyGoalsSchema,
                foreignKeyDefinitions = foreignKeyList,
                userProfiles = allProfiles.map { "${it.userId} (${it.userId.length} chars)" },
                violations = violations,
                testInsertResult = testResult
            )
            
        } catch (e: Exception) {
            Log.e("ForeignKeyDebugger", "❌ Foreign key debugging failed", e)
            ForeignKeyDebugResult(
                foreignKeysEnabled = false,
                userProfileSchema = emptyList(),
                dailyGoalsSchema = emptyList(),
                foreignKeyDefinitions = emptyList(),
                userProfiles = emptyList(),
                violations = listOf("Debug failed: ${e.message}"),
                testInsertResult = "FAILED: ${e.message}"
            )
        }
    }
}

/**
 * Result of foreign key debugging
 */
data class ForeignKeyDebugResult(
    val foreignKeysEnabled: Boolean,
    val userProfileSchema: List<String>,
    val dailyGoalsSchema: List<String>,
    val foreignKeyDefinitions: List<String>,
    val userProfiles: List<String>,
    val violations: List<String>,
    val testInsertResult: String
) {
    override fun toString(): String {
        return """
            Foreign Key Debug Results:
            - Foreign Keys Enabled: $foreignKeysEnabled
            - User Profile Schema: $userProfileSchema
            - Daily Goals Schema: $dailyGoalsSchema
            - Foreign Key Definitions: $foreignKeyDefinitions
            - User Profiles: $userProfiles
            - Violations: $violations
            - Test Insert Result: $testInsertResult
        """.trimIndent()
    }
}