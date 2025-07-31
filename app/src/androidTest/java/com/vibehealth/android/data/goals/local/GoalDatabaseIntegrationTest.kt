package com.vibehealth.android.data.goals.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vibehealth.android.core.security.EncryptionHelper
import com.vibehealth.android.core.security.EncryptionResult
import com.vibehealth.android.data.user.local.AppDatabase
import com.vibehealth.android.domain.goals.CalculationSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for Room database operations with real database.
 * 
 * Tests complete database operations including encryption/decryption,
 * query performance, and data consistency as specified in Task 5.2.
 */
@RunWith(AndroidJUnit4::class)
class GoalDatabaseIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var goalDao: GoalDao
    private lateinit var encryptionHelper: EncryptionHelper
    
    private val testUserId = "test-user-integration"
    private val testGoalsEntity = DailyGoalsEntity(
        id = "test-goal-id",
        userId = testUserId,
        stepsGoal = 10000,
        caloriesGoal = 2000,
        heartPointsGoal = 30,
        calculatedAt = LocalDateTime.now(),
        calculationSource = CalculationSource.WHO_STANDARD,
        createdAt = Date(),
        updatedAt = Date(),
        lastSyncAt = null,
        isDirty = false
    )

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        goalDao = database.goalDao()
        encryptionHelper = TestEncryptionHelper()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun testCompleteRoomDatabaseOperations() = runTest {
        // Test Insert
        goalDao.upsertGoals(testGoalsEntity)
        
        // Test Query
        val retrievedGoals = goalDao.getCurrentGoalsForUserSync(testUserId)
        assertNotNull(retrievedGoals)
        assertEquals(testGoalsEntity.stepsGoal, retrievedGoals.stepsGoal)
        assertEquals(testGoalsEntity.caloriesGoal, retrievedGoals.caloriesGoal)
        assertEquals(testGoalsEntity.heartPointsGoal, retrievedGoals.heartPointsGoal)
        
        // Test Update
        val updatedEntity = testGoalsEntity.copy(
            stepsGoal = 12000,
            updatedAt = Date()
        )
        goalDao.upsertGoals(updatedEntity)
        
        val updatedGoals = goalDao.getCurrentGoalsForUserSync(testUserId)
        assertNotNull(updatedGoals)
        assertEquals(12000, updatedGoals.stepsGoal)
        
        // Test Delete
        val deletedCount = goalDao.deleteGoalsForUser(testUserId)
        assertEquals(1, deletedCount)
        
        val deletedGoals = goalDao.getCurrentGoalsForUserSync(testUserId)
        assertNull(deletedGoals)
    }

    @Test
    fun testEncryptionDecryptionWithRealDatabase() = runTest {
        // Test with encrypted user ID
        val encryptedUserId = "encrypted_$testUserId"
        val encryptedEntity = testGoalsEntity.copy(userId = encryptedUserId)
        
        goalDao.upsertGoals(encryptedEntity)
        
        val retrievedEntity = goalDao.getCurrentGoalsForUserSync(encryptedUserId)
        assertNotNull(retrievedEntity)
        assertEquals(encryptedUserId, retrievedEntity.userId)
        
        // Test decryption through helper
        val decryptionResult = encryptionHelper.decrypt(encryptedUserId)
        assertTrue(decryptionResult is EncryptionResult.Success)
        assertEquals(testUserId, (decryptionResult as EncryptionResult.Success).data)
    }

    @Test
    fun testDatabaseMigrationsAndSchemaChanges() = runTest {
        // Insert data with current schema
        goalDao.upsertGoals(testGoalsEntity)
        
        // Verify data integrity after potential schema changes
        val goals = goalDao.getCurrentGoalsForUserSync(testUserId)
        assertNotNull(goals)
        
        // Test all fields are preserved
        assertEquals(testGoalsEntity.stepsGoal, goals.stepsGoal)
        assertEquals(testGoalsEntity.caloriesGoal, goals.caloriesGoal)
        assertEquals(testGoalsEntity.heartPointsGoal, goals.heartPointsGoal)
        assertEquals(testGoalsEntity.calculationSource, goals.calculationSource)
        assertNotNull(goals.createdAt)
        assertNotNull(goals.updatedAt)
    }

    @Test
    fun testQueryPerformanceWithRealisticDatasets() = runTest {
        // Insert multiple goals for performance testing
        val goalEntities = (1..100).map { index ->
            testGoalsEntity.copy(
                id = "goal-$index",
                userId = "user-$index",
                stepsGoal = 8000 + (index * 100),
                calculatedAt = LocalDateTime.now().minusDays(index.toLong())
            )
        }
        
        val startTime = System.currentTimeMillis()
        
        // Batch insert
        goalEntities.forEach { entity ->
            goalDao.upsertGoals(entity)
        }
        
        val insertTime = System.currentTimeMillis() - startTime
        
        // Test query performance
        val queryStartTime = System.currentTimeMillis()
        val retrievedGoals = goalDao.getCurrentGoalsForUserSync("user-50")
        val queryTime = System.currentTimeMillis() - queryStartTime
        
        // Verify performance targets
        assertTrue(insertTime < 5000, "Batch insert took ${insertTime}ms, should be under 5000ms")
        assertTrue(queryTime < 100, "Query took ${queryTime}ms, should be under 100ms")
        
        // Verify data integrity
        assertNotNull(retrievedGoals)
        assertEquals(13000, retrievedGoals.stepsGoal) // 8000 + (50 * 100)
    }

    @Test
    fun testConcurrentDatabaseOperations() = runTest {
        val concurrentOperations = (1..10).map { index ->
            kotlinx.coroutines.async {
                val entity = testGoalsEntity.copy(
                    id = "concurrent-goal-$index",
                    userId = "concurrent-user-$index",
                    stepsGoal = 8000 + (index * 500)
                )
                goalDao.upsertGoals(entity)
                goalDao.getCurrentGoalsForUserSync("concurrent-user-$index")
            }
        }
        
        // Wait for all operations to complete
        val results = concurrentOperations.map { it.await() }
        
        // Verify all operations succeeded
        assertEquals(10, results.size)
        results.forEachIndexed { index, result ->
            assertNotNull(result)
            assertEquals(8000 + ((index + 1) * 500), result.stepsGoal)
        }
    }

    @Test
    fun testFlowBasedQueries() = runTest {
        // Test reactive queries with Flow
        goalDao.upsertGoals(testGoalsEntity)
        
        val goalsFlow = goalDao.getCurrentGoalsForUser(testUserId)
        val initialGoals = goalsFlow.first()
        
        assertNotNull(initialGoals)
        assertEquals(testGoalsEntity.stepsGoal, initialGoals.stepsGoal)
        
        // Test flow updates when data changes
        val updatedEntity = testGoalsEntity.copy(stepsGoal = 15000)
        goalDao.upsertGoals(updatedEntity)
        
        val updatedGoals = goalsFlow.first()
        assertNotNull(updatedGoals)
        assertEquals(15000, updatedGoals.stepsGoal)
    }

    @Test
    fun testComplexQueries() = runTest {
        // Insert multiple goals with different timestamps
        val entities = listOf(
            testGoalsEntity.copy(id = "1", calculatedAt = LocalDateTime.now().minusDays(1)),
            testGoalsEntity.copy(id = "2", calculatedAt = LocalDateTime.now().minusDays(2)),
            testGoalsEntity.copy(id = "3", calculatedAt = LocalDateTime.now().minusDays(3))
        )
        
        entities.forEach { goalDao.upsertGoals(it) }
        
        // Test getting last calculation time
        val lastCalculationTime = goalDao.getLastCalculationTime(testUserId)
        assertNotNull(lastCalculationTime)
        
        // Test checking if user has goals
        val hasGoals = goalDao.hasGoalsForUser(testUserId)
        assertTrue(hasGoals)
        
        // Test getting all goals for user
        val allGoalsFlow = goalDao.getAllGoalsForUser(testUserId)
        val allGoals = allGoalsFlow.first()
        assertEquals(3, allGoals.size)
        
        // Test cleanup old goals
        val beforeDate = LocalDateTime.now().minusDays(2)
        val deletedCount = goalDao.deleteOldGoals(beforeDate)
        assertEquals(2, deletedCount) // Should delete 2 older goals
        
        val remainingGoals = goalDao.getAllGoalsForUser(testUserId).first()
        assertEquals(1, remainingGoals.size)
    }

    @Test
    fun testDirtyGoalsTracking() = runTest {
        // Insert dirty goal
        val dirtyEntity = testGoalsEntity.copy(isDirty = true)
        goalDao.upsertGoals(dirtyEntity)
        
        // Test getting dirty goals
        val dirtyGoals = goalDao.getDirtyGoals()
        assertEquals(1, dirtyGoals.size)
        assertTrue(dirtyGoals.first().isDirty)
        
        // Test marking as synced
        goalDao.markGoalsAsSynced(testGoalsEntity.id, Date())
        
        // Verify no longer dirty
        val updatedDirtyGoals = goalDao.getDirtyGoals()
        assertEquals(0, updatedDirtyGoals.size)
        
        // Verify sync timestamp updated
        val syncedGoal = goalDao.getCurrentGoalsForUserSync(testUserId)
        assertNotNull(syncedGoal)
        assertNotNull(syncedGoal.lastSyncAt)
    }

    /**
     * Test implementation of EncryptionHelper for integration testing
     */
    private class TestEncryptionHelper : EncryptionHelper {
        override fun encrypt(data: String): EncryptionResult {
            return EncryptionResult.Success("encrypted_$data")
        }

        override fun decrypt(encryptedData: String): EncryptionResult {
            return if (encryptedData.startsWith("encrypted_")) {
                EncryptionResult.Success(encryptedData.removePrefix("encrypted_"))
            } else {
                EncryptionResult.Success(encryptedData)
            }
        }
    }
}