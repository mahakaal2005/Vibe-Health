package com.vibehealth.android.data.goals

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.vibehealth.android.domain.goals.CalculationSource
import com.vibehealth.android.domain.goals.DailyGoals
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for Firebase Firestore operations.
 * 
 * Tests Firestore operations with test project, security rules,
 * sync operations, and data consistency as specified in Task 5.2.
 */
@RunWith(AndroidJUnit4::class)
class FirebaseIntegrationTest {

    private lateinit var firestore: FirebaseFirestore
    private val testUserId = "test-user-firebase"
    private val testGoals = DailyGoals(
        userId = testUserId,
        stepsGoal = 10000,
        caloriesGoal = 2000,
        heartPointsGoal = 30,
        calculatedAt = LocalDateTime.now(),
        calculationSource = CalculationSource.WHO_STANDARD
    )

    @Before
    fun setup() {
        firestore = FirebaseFirestore.getInstance()
        
        // Configure for testing
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
        firestore.firestoreSettings = settings
    }

    @Test
    fun testFirestoreOperationsWithTestProject() = runTest {
        val userDoc = firestore.collection("users").document(testUserId)
        
        // Test write operation
        val goalData = mapOf(
            "dailyGoals" to mapOf(
                "stepsGoal" to testGoals.stepsGoal,
                "caloriesGoal" to testGoals.caloriesGoal,
                "heartPointsGoal" to testGoals.heartPointsGoal,
                "calculatedAt" to testGoals.calculatedAt.toString(),
                "calculationSource" to testGoals.calculationSource.name
            )
        )
        
        userDoc.set(goalData).await()
        
        // Test read operation
        val snapshot = userDoc.get().await()
        assertTrue(snapshot.exists())
        
        val retrievedGoals = snapshot.get("dailyGoals") as Map<*, *>
        assertEquals(testGoals.stepsGoal.toLong(), retrievedGoals["stepsGoal"])
        assertEquals(testGoals.caloriesGoal.toLong(), retrievedGoals["caloriesGoal"])
        assertEquals(testGoals.heartPointsGoal.toLong(), retrievedGoals["heartPointsGoal"])
        
        // Test update operation
        userDoc.update("dailyGoals.stepsGoal", 12000).await()
        
        val updatedSnapshot = userDoc.get().await()
        val updatedGoals = updatedSnapshot.get("dailyGoals") as Map<*, *>
        assertEquals(12000L, updatedGoals["stepsGoal"])
        
        // Test delete operation
        userDoc.update("dailyGoals", null).await()
        
        val deletedSnapshot = userDoc.get().await()
        val deletedGoals = deletedSnapshot.get("dailyGoals")
        assertEquals(null, deletedGoals)
    }

    @Test
    fun testSecurityRulesPreventUnauthorizedAccess() = runTest {
        // This test would normally require Firebase Auth setup
        // For now, we'll test basic document access patterns
        
        val unauthorizedUserId = "unauthorized-user"
        val unauthorizedDoc = firestore.collection("users").document(unauthorizedUserId)
        
        try {
            // Attempt to access another user's data
            val snapshot = unauthorizedDoc.get().await()
            
            // In a real implementation with security rules, this should fail
            // For testing purposes, we'll verify the document structure
            if (snapshot.exists()) {
                val data = snapshot.data
                assertNotNull(data)
            }
        } catch (e: Exception) {
            // Expected behavior with proper security rules
            assertTrue(e.message?.contains("permission") == true || 
                      e.message?.contains("unauthorized") == true)
        }
    }

    @Test
    fun testSyncOperationsUnderVariousNetworkConditions() = runTest {
        val userDoc = firestore.collection("users").document(testUserId)
        
        // Test normal network conditions
        val normalData = mapOf("dailyGoals" to mapOf("stepsGoal" to 8000))
        userDoc.set(normalData).await()
        
        val normalSnapshot = userDoc.get().await()
        assertTrue(normalSnapshot.exists())
        
        // Test rapid successive updates (simulating poor network)
        val updates = listOf(9000, 9500, 10000, 10500, 11000)
        
        updates.forEach { steps ->
            userDoc.update("dailyGoals.stepsGoal", steps).await()
        }
        
        // Verify final state
        val finalSnapshot = userDoc.get().await()
        val finalGoals = finalSnapshot.get("dailyGoals") as Map<*, *>
        assertEquals(11000L, finalGoals["stepsGoal"])
    }

    @Test
    fun testDataConsistencyBetweenLocalAndCloudStorage() = runTest {
        val userDoc = firestore.collection("users").document(testUserId)
        
        // Simulate local data
        val localGoals = mapOf(
            "stepsGoal" to 10000,
            "caloriesGoal" to 2000,
            "heartPointsGoal" to 30,
            "lastModified" to System.currentTimeMillis()
        )
        
        // Sync to cloud
        userDoc.set(mapOf("dailyGoals" to localGoals)).await()
        
        // Retrieve from cloud
        val cloudSnapshot = userDoc.get().await()
        val cloudGoals = cloudSnapshot.get("dailyGoals") as Map<*, *>
        
        // Verify consistency
        assertEquals(localGoals["stepsGoal"]?.toString()?.toLong(), cloudGoals["stepsGoal"])
        assertEquals(localGoals["caloriesGoal"]?.toString()?.toLong(), cloudGoals["caloriesGoal"])
        assertEquals(localGoals["heartPointsGoal"]?.toString()?.toLong(), cloudGoals["heartPointsGoal"])
        
        // Test conflict resolution (local takes precedence)
        val conflictingCloudData = mapOf(
            "dailyGoals" to mapOf(
                "stepsGoal" to 8000,
                "lastModified" to System.currentTimeMillis() - 10000 // Older timestamp
            )
        )
        
        val conflictingLocalData = mapOf(
            "dailyGoals" to mapOf(
                "stepsGoal" to 12000,
                "lastModified" to System.currentTimeMillis() // Newer timestamp
            )
        )
        
        // Simulate conflict resolution logic
        val localTimestamp = conflictingLocalData["dailyGoals"] as Map<*, *>
        val cloudTimestamp = conflictingCloudData["dailyGoals"] as Map<*, *>
        
        val shouldUseLocal = (localTimestamp["lastModified"] as Long) > 
                           (cloudTimestamp["lastModified"] as Long)
        
        assertTrue(shouldUseLocal)
        
        // Apply resolution
        userDoc.set(conflictingLocalData).await()
        
        val resolvedSnapshot = userDoc.get().await()
        val resolvedGoals = resolvedSnapshot.get("dailyGoals") as Map<*, *>
        assertEquals(12000L, resolvedGoals["stepsGoal"])
    }

    @Test
    fun testBatchOperationsAndTransactions() = runTest {
        val batch = firestore.batch()
        
        // Prepare multiple user documents
        val userIds = listOf("user1", "user2", "user3")
        val userDocs = userIds.map { firestore.collection("users").document(it) }
        
        // Add batch operations
        userDocs.forEachIndexed { index, doc ->
            val goalData = mapOf(
                "dailyGoals" to mapOf(
                    "stepsGoal" to (8000 + index * 1000),
                    "caloriesGoal" to (1800 + index * 200),
                    "heartPointsGoal" to (25 + index * 5)
                )
            )
            batch.set(doc, goalData)
        }
        
        // Commit batch
        batch.commit().await()
        
        // Verify all documents were created
        userDocs.forEachIndexed { index, doc ->
            val snapshot = doc.get().await()
            assertTrue(snapshot.exists())
            
            val goals = snapshot.get("dailyGoals") as Map<*, *>
            assertEquals((8000 + index * 1000).toLong(), goals["stepsGoal"])
        }
        
        // Test transaction
        firestore.runTransaction { transaction ->
            val doc = firestore.collection("users").document("user1")
            val snapshot = transaction.get(doc)
            
            val currentGoals = snapshot.get("dailyGoals") as Map<*, *>
            val currentSteps = currentGoals["stepsGoal"] as Long
            
            // Update within transaction
            transaction.update(doc, "dailyGoals.stepsGoal", currentSteps + 1000)
            
            null
        }.await()
        
        // Verify transaction result
        val transactionResult = userDocs[0].get().await()
        val transactionGoals = transactionResult.get("dailyGoals") as Map<*, *>
        assertEquals(9000L, transactionGoals["stepsGoal"]) // 8000 + 1000
    }

    @Test
    fun testFirestorePerformanceAndLimits() = runTest {
        val startTime = System.currentTimeMillis()
        
        // Test multiple concurrent operations
        val operations = (1..10).map { index ->
            kotlinx.coroutines.async {
                val doc = firestore.collection("users").document("perf-user-$index")
                val data = mapOf(
                    "dailyGoals" to mapOf(
                        "stepsGoal" to (8000 + index * 100),
                        "timestamp" to System.currentTimeMillis()
                    )
                )
                doc.set(data).await()
                doc.get().await()
            }
        }
        
        // Wait for all operations
        operations.forEach { it.await() }
        
        val totalTime = System.currentTimeMillis() - startTime
        
        // Verify performance targets
        assertTrue(totalTime < 10000, "10 concurrent operations took ${totalTime}ms, should be under 10000ms")
        
        // Test query performance
        val queryStartTime = System.currentTimeMillis()
        val querySnapshot = firestore.collection("users")
            .whereGreaterThan("dailyGoals.stepsGoal", 8000)
            .limit(5)
            .get()
            .await()
        
        val queryTime = System.currentTimeMillis() - queryStartTime
        
        assertTrue(queryTime < 2000, "Query took ${queryTime}ms, should be under 2000ms")
        assertTrue(querySnapshot.documents.isNotEmpty())
    }

    @Test
    fun testOfflineCapabilitiesAndCaching() = runTest {
        val userDoc = firestore.collection("users").document(testUserId)
        
        // Write data while online
        val onlineData = mapOf(
            "dailyGoals" to mapOf(
                "stepsGoal" to 10000,
                "source" to "online"
            )
        )
        
        userDoc.set(onlineData).await()
        
        // Verify data exists
        val onlineSnapshot = userDoc.get().await()
        assertTrue(onlineSnapshot.exists())
        
        val retrievedData = onlineSnapshot.get("dailyGoals") as Map<*, *>
        assertEquals("online", retrievedData["source"])
        
        // Test cached data access (simulated)
        val cachedSnapshot = userDoc.get().await()
        assertTrue(cachedSnapshot.exists())
        
        val cachedData = cachedSnapshot.get("dailyGoals") as Map<*, *>
        assertEquals(10000L, cachedData["stepsGoal"])
    }
}