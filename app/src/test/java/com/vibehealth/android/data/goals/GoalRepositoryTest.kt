package com.vibehealth.android.data.goals

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.CollectionReference
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.vibehealth.android.core.security.EncryptionHelper
import com.vibehealth.android.core.security.EncryptionResult
import com.vibehealth.android.data.goals.local.DailyGoalsEntity
import com.vibehealth.android.data.goals.local.GoalDao
import com.vibehealth.android.domain.goals.CalculationSource
import com.vibehealth.android.domain.goals.DailyGoals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.Date

/**
 * Comprehensive unit tests for GoalRepository.
 * 
 * Tests cover local storage, cloud sync, encryption, error handling,
 * and offline-first behavior as specified in Task 2.2 requirements.
 */
class GoalRepositoryTest {

    @Mock
    private lateinit var goalDao: GoalDao
    
    @Mock
    private lateinit var firestore: FirebaseFirestore
    
    @Mock
    private lateinit var encryptionHelper: EncryptionHelper
    
    @Mock
    private lateinit var collectionReference: CollectionReference
    
    @Mock
    private lateinit var documentReference: DocumentReference
    
    @Mock
    private lateinit var task: Task<Void>
    
    private lateinit var goalRepository: GoalRepository
    
    private val testUserId = "test-user-123"
    private val testGoals = DailyGoals(
        userId = testUserId,
        stepsGoal = 10000,
        caloriesGoal = 2000,
        heartPointsGoal = 30,
        calculatedAt = LocalDateTime.now(),
        calculationSource = CalculationSource.WHO_STANDARD
    )
    
    private val testEntity = DailyGoalsEntity(
        id = "test-id",
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

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Setup Firestore mocks
        whenever(firestore.collection("users")).thenReturn(collectionReference)
        whenever(collectionReference.document(any())).thenReturn(documentReference)
        whenever(documentReference.set(any(), any())).thenReturn(task)
        whenever(documentReference.update(any<Map<String, Any>>())).thenReturn(task)
        whenever(task.await()).thenReturn(null)
        
        // Setup encryption mocks
        whenever(encryptionHelper.encrypt(any())).thenReturn(EncryptionResult.Success("encrypted_data"))
        whenever(encryptionHelper.decrypt(any())).thenReturn(EncryptionResult.Success(testUserId))
        
        goalRepository = GoalRepository(goalDao, firestore, encryptionHelper)
    }

    @Nested
    @DisplayName("Local Storage Operations")
    inner class LocalStorageOperations {

        @Test
        @DisplayName("Should save goals locally with encryption")
        fun shouldSaveGoalsLocallyWithEncryption() = runTest {
            // Given
            whenever(goalDao.upsertGoals(any<DailyGoalsEntity>())).thenReturn(Unit)
            
            // When
            val result = goalRepository.saveGoalsLocally(testGoals)
            
            // Then
            assertTrue(result.isSuccess)
            assertEquals(testGoals, result.getOrNull())
            
            verify(encryptionHelper).encrypt(testUserId)
            verify(goalDao).upsertGoals(any<DailyGoalsEntity>())
        }

        @Test
        @DisplayName("Should handle local save failure gracefully")
        fun shouldHandleLocalSaveFailure() = runTest {
            // Given
            val exception = RuntimeException("Database error")
            whenever(goalDao.upsertGoals(any<DailyGoalsEntity>())).thenThrow(exception)
            
            // When
            val result = goalRepository.saveGoalsLocally(testGoals)
            
            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

        @Test
        @DisplayName("Should get current goals with decryption")
        fun shouldGetCurrentGoalsWithDecryption() = runTest {
            // Given
            whenever(goalDao.getCurrentGoalsForUser(testUserId)).thenReturn(flowOf(testEntity))
            
            // When
            val flow = goalRepository.getCurrentGoals(testUserId)
            
            // Then
            flow.collect { goals ->
                assertNotNull(goals)
                assertEquals(testGoals.userId, goals!!.userId)
                assertEquals(testGoals.stepsGoal, goals.stepsGoal)
                assertEquals(testGoals.caloriesGoal, goals.caloriesGoal)
                assertEquals(testGoals.heartPointsGoal, goals.heartPointsGoal)
            }
            
            verify(encryptionHelper).decrypt(testUserId)
        }

        @Test
        @DisplayName("Should return null when no goals found")
        fun shouldReturnNullWhenNoGoalsFound() = runTest {
            // Given
            whenever(goalDao.getCurrentGoalsForUser(testUserId)).thenReturn(flowOf(null))
            
            // When
            val flow = goalRepository.getCurrentGoals(testUserId)
            
            // Then
            flow.collect { goals ->
                assertNull(goals)
            }
        }

        @Test
        @DisplayName("Should handle decryption failure gracefully")
        fun shouldHandleDecryptionFailure() = runTest {
            // Given
            whenever(goalDao.getCurrentGoalsForUser(testUserId)).thenReturn(flowOf(testEntity))
            whenever(encryptionHelper.decrypt(any())).thenReturn(EncryptionResult.Error("Decryption failed"))
            
            // When
            val flow = goalRepository.getCurrentGoals(testUserId)
            
            // Then
            flow.collect { goals ->
                assertNull(goals)
            }
        }

        @Test
        @DisplayName("Should get current goals synchronously")
        fun shouldGetCurrentGoalsSync() = runTest {
            // Given
            whenever(goalDao.getCurrentGoalsForUserSync(testUserId)).thenReturn(testEntity)
            
            // When
            val goals = goalRepository.getCurrentGoalsSync(testUserId)
            
            // Then
            assertNotNull(goals)
            assertEquals(testGoals.userId, goals!!.userId)
            verify(encryptionHelper).decrypt(testUserId)
        }

        @Test
        @DisplayName("Should check if user has goals")
        fun shouldCheckIfUserHasGoals() = runTest {
            // Given
            whenever(goalDao.hasGoalsForUser(testUserId)).thenReturn(true)
            
            // When
            val hasGoals = goalRepository.hasGoalsForUser(testUserId)
            
            // Then
            assertTrue(hasGoals)
        }

        @Test
        @DisplayName("Should get last calculation time")
        fun shouldGetLastCalculationTime() = runTest {
            // Given
            val calculationTime = LocalDateTime.now()
            whenever(goalDao.getLastCalculationTime(testUserId)).thenReturn(calculationTime)
            
            // When
            val lastTime = goalRepository.getLastCalculationTime(testUserId)
            
            // Then
            assertEquals(calculationTime, lastTime)
        }
    }

    @Nested
    @DisplayName("Cloud Synchronization")
    inner class CloudSynchronization {

        @Test
        @DisplayName("Should sync goals to cloud successfully")
        fun shouldSyncGoalsToCloudSuccessfully() = runTest {
            // Given
            whenever(goalDao.markGoalsAsSynced(any(), any())).thenReturn(Unit)
            
            // When
            val result = goalRepository.syncGoalsToCloud(testGoals)
            
            // Then
            assertTrue(result.isSuccess)
            verify(documentReference).set(any(), any())
            verify(goalDao).markGoalsAsSynced(any(), any())
        }

        @Test
        @DisplayName("Should retry sync on failure with exponential backoff")
        fun shouldRetrySyncOnFailure() = runTest {
            // Given
            val exception = RuntimeException("Network error")
            whenever(documentReference.set(any(), any())).thenReturn(task)
            whenever(task.await()).thenThrow(exception)
            
            // When
            val result = goalRepository.syncGoalsToCloud(testGoals)
            
            // Then
            assertTrue(result.isFailure)
            verify(documentReference, times(3)).set(any(), any()) // 3 retry attempts
        }

        @Test
        @DisplayName("Should sync all dirty goals")
        fun shouldSyncAllDirtyGoals() = runTest {
            // Given
            val dirtyEntities = listOf(testEntity.copy(isDirty = true))
            whenever(goalDao.getDirtyGoals()).thenReturn(dirtyEntities)
            whenever(goalDao.markGoalsAsSynced(any(), any())).thenReturn(Unit)
            
            // When
            val result = goalRepository.syncAllDirtyGoals()
            
            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull())
            verify(documentReference).set(any(), any())
        }

        @Test
        @DisplayName("Should handle partial sync failures in batch")
        fun shouldHandlePartialSyncFailures() = runTest {
            // Given
            val dirtyEntities = listOf(
                testEntity.copy(id = "1", isDirty = true),
                testEntity.copy(id = "2", isDirty = true)
            )
            whenever(goalDao.getDirtyGoals()).thenReturn(dirtyEntities)
            whenever(goalDao.markGoalsAsSynced(any(), any())).thenReturn(Unit)
            
            // First sync succeeds, second fails
            whenever(documentReference.set(any(), any()))
                .thenReturn(task)
                .thenThrow(RuntimeException("Network error"))
            
            // When
            val result = goalRepository.syncAllDirtyGoals()
            
            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull()) // Only one succeeded
        }
    }

    @Nested
    @DisplayName("Combined Operations")
    inner class CombinedOperations {

        @Test
        @DisplayName("Should save and sync goals successfully")
        fun shouldSaveAndSyncGoalsSuccessfully() = runTest {
            // Given
            whenever(goalDao.upsertGoals(any<DailyGoalsEntity>())).thenReturn(Unit)
            whenever(goalDao.markGoalsAsSynced(any(), any())).thenReturn(Unit)
            
            // When
            val result = goalRepository.saveAndSyncGoals(testGoals)
            
            // Then
            assertTrue(result.isSuccess)
            assertEquals(testGoals, result.getOrNull())
            
            verify(goalDao).upsertGoals(any<DailyGoalsEntity>())
            verify(documentReference).set(any(), any())
        }

        @Test
        @DisplayName("Should succeed if local save works but sync fails")
        fun shouldSucceedIfLocalSaveWorksButSyncFails() = runTest {
            // Given
            whenever(goalDao.upsertGoals(any<DailyGoalsEntity>())).thenReturn(Unit)
            whenever(documentReference.set(any(), any())).thenReturn(task)
            whenever(task.await()).thenThrow(RuntimeException("Network error"))
            
            // When
            val result = goalRepository.saveAndSyncGoals(testGoals)
            
            // Then
            assertTrue(result.isSuccess) // Should succeed because local save worked
            assertEquals(testGoals, result.getOrNull())
        }

        @Test
        @DisplayName("Should fail if local save fails")
        fun shouldFailIfLocalSaveFails() = runTest {
            // Given
            val exception = RuntimeException("Database error")
            whenever(goalDao.upsertGoals(any<DailyGoalsEntity>())).thenThrow(exception)
            
            // When
            val result = goalRepository.saveAndSyncGoals(testGoals)
            
            // Then
            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
            
            // Should not attempt sync if local save fails
            verify(documentReference, never()).set(any(), any())
        }
    }

    @Nested
    @DisplayName("Data Management")
    inner class DataManagement {

        @Test
        @DisplayName("Should delete goals for user from both local and cloud")
        fun shouldDeleteGoalsForUser() = runTest {
            // Given
            whenever(goalDao.deleteGoalsForUser(testUserId)).thenReturn(1)
            whenever(documentReference.update(any<Map<String, Any>>())).thenReturn(task)
            
            // When
            val result = goalRepository.deleteGoalsForUser(testUserId)
            
            // Then
            assertTrue(result.isSuccess)
            verify(goalDao).deleteGoalsForUser(testUserId)
            verify(documentReference).update(mapOf("dailyGoals" to null))
        }

        @Test
        @DisplayName("Should succeed local deletion even if cloud deletion fails")
        fun shouldSucceedLocalDeletionEvenIfCloudDeletionFails() = runTest {
            // Given
            whenever(goalDao.deleteGoalsForUser(testUserId)).thenReturn(1)
            whenever(documentReference.update(any<Map<String, Any>>())).thenReturn(task)
            whenever(task.await()).thenThrow(RuntimeException("Network error"))
            
            // When
            val result = goalRepository.deleteGoalsForUser(testUserId)
            
            // Then
            assertTrue(result.isSuccess) // Should succeed because local deletion worked
        }

        @Test
        @DisplayName("Should get all goals for user with decryption")
        fun shouldGetAllGoalsForUser() = runTest {
            // Given
            val entities = listOf(testEntity, testEntity.copy(id = "2"))
            whenever(goalDao.getAllGoalsForUser(testUserId)).thenReturn(flowOf(entities))
            
            // When
            val flow = goalRepository.getAllGoalsForUser(testUserId)
            
            // Then
            flow.collect { goalsList ->
                assertEquals(2, goalsList.size)
                goalsList.forEach { goals ->
                    assertEquals(testUserId, goals.userId)
                }
            }
            
            verify(encryptionHelper, times(2)).decrypt(testUserId)
        }

        @Test
        @DisplayName("Should filter out goals that fail decryption")
        fun shouldFilterOutGoalsThatFailDecryption() = runTest {
            // Given
            val entities = listOf(testEntity, testEntity.copy(id = "2"))
            whenever(goalDao.getAllGoalsForUser(testUserId)).thenReturn(flowOf(entities))
            whenever(encryptionHelper.decrypt(testUserId))
                .thenReturn(EncryptionResult.Success(testUserId))
                .thenReturn(EncryptionResult.Error("Decryption failed"))
            
            // When
            val flow = goalRepository.getAllGoalsForUser(testUserId)
            
            // Then
            flow.collect { goalsList ->
                assertEquals(1, goalsList.size) // Only one should succeed
            }
        }

        @Test
        @DisplayName("Should cleanup old goals")
        fun shouldCleanupOldGoals() = runTest {
            // Given
            val beforeDate = LocalDateTime.now().minusDays(30)
            whenever(goalDao.deleteOldGoals(beforeDate)).thenReturn(5)
            
            // When
            val deletedCount = goalRepository.cleanupOldGoals(beforeDate)
            
            // Then
            assertEquals(5, deletedCount)
            verify(goalDao).deleteOldGoals(beforeDate)
        }
    }

    @Nested
    @DisplayName("Encryption Handling")
    inner class EncryptionHandling {

        @Test
        @DisplayName("Should handle encryption failure gracefully during save")
        fun shouldHandleEncryptionFailureDuringSave() = runTest {
            // Given
            whenever(encryptionHelper.encrypt(any())).thenReturn(EncryptionResult.Error("Encryption failed"))
            whenever(goalDao.upsertGoals(any<DailyGoalsEntity>())).thenReturn(Unit)
            
            // When
            val result = goalRepository.saveGoalsLocally(testGoals)
            
            // Then
            assertTrue(result.isSuccess) // Should still succeed with original data
            verify(goalDao).upsertGoals(any<DailyGoalsEntity>())
        }

        @Test
        @DisplayName("Should handle decryption failure gracefully during retrieval")
        fun shouldHandleDecryptionFailureDuringRetrieval() = runTest {
            // Given
            whenever(goalDao.getCurrentGoalsForUserSync(testUserId)).thenReturn(testEntity)
            whenever(encryptionHelper.decrypt(any())).thenReturn(EncryptionResult.Error("Decryption failed"))
            
            // When
            val goals = goalRepository.getCurrentGoalsSync(testUserId)
            
            // Then
            assertNotNull(goals) // Should still return goals with original data
            assertEquals(testUserId, goals!!.userId)
        }

        @Test
        @DisplayName("Should handle encryption exception gracefully")
        fun shouldHandleEncryptionExceptionGracefully() = runTest {
            // Given
            whenever(encryptionHelper.encrypt(any())).thenThrow(RuntimeException("Encryption error"))
            whenever(goalDao.upsertGoals(any<DailyGoalsEntity>())).thenReturn(Unit)
            
            // When
            val result = goalRepository.saveGoalsLocally(testGoals)
            
            // Then
            assertTrue(result.isSuccess) // Should fallback to unencrypted
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("Should handle database exceptions gracefully")
        fun shouldHandleDatabaseExceptionsGracefully() = runTest {
            // Given
            whenever(goalDao.hasGoalsForUser(testUserId)).thenThrow(RuntimeException("Database error"))
            
            // When
            val hasGoals = goalRepository.hasGoalsForUser(testUserId)
            
            // Then
            assertFalse(hasGoals) // Should return false on error
        }

        @Test
        @DisplayName("Should handle null responses gracefully")
        fun shouldHandleNullResponsesGracefully() = runTest {
            // Given
            whenever(goalDao.getLastCalculationTime(testUserId)).thenReturn(null)
            
            // When
            val lastTime = goalRepository.getLastCalculationTime(testUserId)
            
            // Then
            assertNull(lastTime)
        }

        @Test
        @DisplayName("Should handle cleanup failures gracefully")
        fun shouldHandleCleanupFailuresGracefully() = runTest {
            // Given
            val beforeDate = LocalDateTime.now().minusDays(30)
            whenever(goalDao.deleteOldGoals(beforeDate)).thenThrow(RuntimeException("Cleanup error"))
            
            // When
            val deletedCount = goalRepository.cleanupOldGoals(beforeDate)
            
            // Then
            assertEquals(0, deletedCount) // Should return 0 on error
        }
    }

    @Nested
    @DisplayName("Offline-First Behavior")
    inner class OfflineFirstBehavior {

        @Test
        @DisplayName("Should prioritize local storage over cloud sync")
        fun shouldPrioritizeLocalStorageOverCloudSync() = runTest {
            // Given
            whenever(goalDao.upsertGoals(any<DailyGoalsEntity>())).thenReturn(Unit)
            whenever(documentReference.set(any(), any())).thenReturn(task)
            whenever(task.await()).thenThrow(RuntimeException("Network unavailable"))
            
            // When
            val result = goalRepository.saveAndSyncGoals(testGoals)
            
            // Then
            assertTrue(result.isSuccess) // Should succeed because local save worked
            verify(goalDao).upsertGoals(any<DailyGoalsEntity>()) // Local save attempted
            verify(documentReference).set(any(), any()) // Cloud sync attempted but failed
        }

        @Test
        @DisplayName("Should work entirely offline")
        fun shouldWorkEntirelyOffline() = runTest {
            // Given - no cloud operations, only local
            whenever(goalDao.upsertGoals(any<DailyGoalsEntity>())).thenReturn(Unit)
            whenever(goalDao.getCurrentGoalsForUserSync(testUserId)).thenReturn(testEntity)
            
            // When
            val saveResult = goalRepository.saveGoalsLocally(testGoals, markAsDirty = false)
            val retrieveResult = goalRepository.getCurrentGoalsSync(testUserId)
            
            // Then
            assertTrue(saveResult.isSuccess)
            assertNotNull(retrieveResult)
            assertEquals(testGoals.userId, retrieveResult!!.userId)
            
            // Verify no cloud operations were attempted
            verify(firestore, never()).collection(any())
        }
    }
}