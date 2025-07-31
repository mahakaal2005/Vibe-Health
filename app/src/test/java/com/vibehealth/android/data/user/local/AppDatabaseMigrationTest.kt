package com.vibehealth.android.data.user.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for AppDatabase migrations.
 * 
 * Tests database schema migrations to ensure data integrity
 * and proper table creation for goal calculation entities.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java,
        listOf(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            // Database version 1 should have user_profiles table
            execSQL("""
                CREATE TABLE IF NOT EXISTS `user_profiles` (
                    `user_id` TEXT NOT NULL PRIMARY KEY,
                    `email` TEXT NOT NULL,
                    `display_name` TEXT NOT NULL,
                    `first_name` TEXT,
                    `last_name` TEXT,
                    `birthday` INTEGER,
                    `gender` TEXT NOT NULL,
                    `unit_system` TEXT NOT NULL,
                    `height_in_cm` INTEGER NOT NULL,
                    `weight_in_kg` REAL NOT NULL,
                    `has_completed_onboarding` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL
                )
            """.trimIndent())
            
            // Insert test data
            execSQL("""
                INSERT INTO user_profiles VALUES (
                    'test-user-1',
                    'test@example.com',
                    'Test User',
                    'Test',
                    'User',
                    ${System.currentTimeMillis() - 25 * 365 * 24 * 60 * 60 * 1000L}, -- 25 years ago
                    'MALE',
                    'METRIC',
                    175,
                    70.0,
                    1,
                    ${System.currentTimeMillis()},
                    ${System.currentTimeMillis()}
                )
            """.trimIndent())
            
            close()
        }

        // Re-open the database with version 2 and provide MIGRATION_1_2 as the migration process
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseMigrations.MIGRATION_1_2)

        // Verify the schema after migration
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table'")
        val tableNames = mutableListOf<String>()
        
        while (cursor.moveToNext()) {
            tableNames.add(cursor.getString(0))
        }
        cursor.close()

        // Should have both user_profiles and daily_goals tables
        assertTrue(tableNames.contains("user_profiles"), "user_profiles table should exist")
        assertTrue(tableNames.contains("daily_goals"), "daily_goals table should exist")

        // Verify daily_goals table structure
        val dailyGoalsInfo = db.query("PRAGMA table_info(daily_goals)")
        val columnNames = mutableListOf<String>()
        
        while (dailyGoalsInfo.moveToNext()) {
            columnNames.add(dailyGoalsInfo.getString(1)) // Column name is at index 1
        }
        dailyGoalsInfo.close()

        // Verify all expected columns exist
        val expectedColumns = listOf(
            "id", "user_id", "steps_goal", "calories_goal", "heart_points_goal",
            "calculated_at", "calculation_source", "created_at", "updated_at",
            "last_sync_at", "is_dirty"
        )
        
        expectedColumns.forEach { column ->
            assertTrue(columnNames.contains(column), "Column $column should exist in daily_goals table")
        }

        // Verify indexes were created
        val indexInfo = db.query("SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='daily_goals'")
        val indexNames = mutableListOf<String>()
        
        while (indexInfo.moveToNext()) {
            indexNames.add(indexInfo.getString(0))
        }
        indexInfo.close()

        // Should have the expected indexes
        assertTrue(indexNames.any { it.contains("user_id") }, "Should have user_id index")
        assertTrue(indexNames.any { it.contains("calculated_at") }, "Should have calculated_at index")

        // Verify existing data is preserved
        val userQuery = db.query("SELECT COUNT(*) FROM user_profiles WHERE user_id = 'test-user-1'")
        userQuery.moveToFirst()
        assertEquals(1, userQuery.getInt(0), "Existing user data should be preserved")
        userQuery.close()

        // Verify we can insert into the new table
        db.execSQL("""
            INSERT INTO daily_goals VALUES (
                'test-goal-1',
                'test-user-1',
                10000,
                2000,
                30,
                '2024-01-15T10:30:45',
                'WHO_STANDARD',
                ${System.currentTimeMillis()},
                ${System.currentTimeMillis()},
                NULL,
                0
            )
        """.trimIndent())

        // Verify the insert worked
        val goalQuery = db.query("SELECT COUNT(*) FROM daily_goals WHERE id = 'test-goal-1'")
        goalQuery.moveToFirst()
        assertEquals(1, goalQuery.getInt(0), "Should be able to insert into daily_goals table")
        goalQuery.close()

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun testAllMigrationsFromScratch() {
        // Create the database with the latest version directly
        val db = helper.createDatabase(TEST_DB, 2)
        
        // Verify both tables exist
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table'")
        val tableNames = mutableListOf<String>()
        
        while (cursor.moveToNext()) {
            tableNames.add(cursor.getString(0))
        }
        cursor.close()

        assertTrue(tableNames.contains("user_profiles"))
        assertTrue(tableNames.contains("daily_goals"))
        
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun testForeignKeyConstraint() {
        val db = helper.createDatabase(TEST_DB, 2)
        
        // Enable foreign key constraints
        db.execSQL("PRAGMA foreign_keys=ON")
        
        // Insert a user first
        db.execSQL("""
            INSERT INTO user_profiles VALUES (
                'test-user-fk',
                'test@example.com',
                'Test User',
                'Test',
                'User',
                ${System.currentTimeMillis() - 25 * 365 * 24 * 60 * 60 * 1000L},
                'MALE',
                'METRIC',
                175,
                70.0,
                1,
                ${System.currentTimeMillis()},
                ${System.currentTimeMillis()}
            )
        """.trimIndent())
        
        // Insert a goal with valid foreign key
        db.execSQL("""
            INSERT INTO daily_goals VALUES (
                'test-goal-fk',
                'test-user-fk',
                10000,
                2000,
                30,
                '2024-01-15T10:30:45',
                'WHO_STANDARD',
                ${System.currentTimeMillis()},
                ${System.currentTimeMillis()},
                NULL,
                0
            )
        """.trimIndent())
        
        // Verify the insert worked
        val goalQuery = db.query("SELECT COUNT(*) FROM daily_goals WHERE user_id = 'test-user-fk'")
        goalQuery.moveToFirst()
        assertEquals(1, goalQuery.getInt(0))
        goalQuery.close()
        
        // Test cascade delete
        db.execSQL("DELETE FROM user_profiles WHERE user_id = 'test-user-fk'")
        
        // Goal should be deleted due to CASCADE
        val cascadeQuery = db.query("SELECT COUNT(*) FROM daily_goals WHERE user_id = 'test-user-fk'")
        cascadeQuery.moveToFirst()
        assertEquals(0, cascadeQuery.getInt(0), "Goal should be deleted due to foreign key cascade")
        cascadeQuery.close()
        
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun testIndexPerformance() {
        val db = helper.createDatabase(TEST_DB, 2)
        
        // Insert test user
        db.execSQL("""
            INSERT INTO user_profiles VALUES (
                'test-user-perf',
                'test@example.com',
                'Test User',
                'Test',
                'User',
                ${System.currentTimeMillis() - 25 * 365 * 24 * 60 * 60 * 1000L},
                'MALE',
                'METRIC',
                175,
                70.0,
                1,
                ${System.currentTimeMillis()},
                ${System.currentTimeMillis()}
            )
        """.trimIndent())
        
        // Insert multiple goals to test index performance
        repeat(100) { i ->
            db.execSQL("""
                INSERT INTO daily_goals VALUES (
                    'test-goal-$i',
                    'test-user-perf',
                    ${10000 + i},
                    ${2000 + i},
                    ${30 + (i % 10)},
                    '2024-01-${String.format("%02d", (i % 28) + 1)}T10:30:45',
                    'WHO_STANDARD',
                    ${System.currentTimeMillis() + i},
                    ${System.currentTimeMillis() + i},
                    NULL,
                    0
                )
            """.trimIndent())
        }
        
        // Test query performance with index
        val startTime = System.currentTimeMillis()
        val query = db.query("""
            SELECT * FROM daily_goals 
            WHERE user_id = 'test-user-perf' 
            ORDER BY calculated_at DESC 
            LIMIT 10
        """.trimIndent())
        
        val resultCount = query.count
        query.close()
        val duration = System.currentTimeMillis() - startTime
        
        assertEquals(10, resultCount)
        assertTrue(duration < 100, "Query should be fast with proper indexing: ${duration}ms")
        
        db.close()
    }
}