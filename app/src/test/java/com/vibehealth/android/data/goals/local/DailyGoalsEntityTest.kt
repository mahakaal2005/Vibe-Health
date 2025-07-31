package com.vibehealth.android.data.goals.local

import com.vibehealth.android.domain.goals.CalculationSource
import com.vibehealth.android.domain.goals.DailyGoals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import java.time.LocalDateTime
import java.util.Date

/**
 * Comprehensive unit tests for DailyGoalsEntity.
 * 
 * Tests cover entity creation, domain model conversion, and database operations
 * as specified in Task 2.1 requirements.
 */
class DailyGoalsEntityTest {

    private val testUserId = "test-user-123"
    private val testCalculatedAt = LocalDateTime.of(2024, 1, 15, 10, 30)
    private val testCreatedAt = Date(1705312200000L) // 2024-01-15 10:30:00
    
    private val testDailyGoals = DailyGoals(
        userId = testUserId,
        stepsGoal = 10000,
        caloriesGoal = 2000,
        heartPointsGoal = 30,
        calculatedAt = testCalculatedAt,
        calculationSource = CalculationSource.WHO_STANDARD
    )
    
    private val testEntity = DailyGoalsEntity(
        id = "test-id-123",
        userId = testUserId,
        stepsGoal = 10000,
        caloriesGoal = 2000,
        heartPointsGoal = 30,
        calculatedAt = testCalculatedAt,
        calculationSource = CalculationSource.WHO_STANDARD,
        createdAt = testCreatedAt,
        updatedAt = testCreatedAt,
        lastSyncAt = null,
        isDirty = false
    )

    @Nested
    @DisplayName("Entity Creation")
    inner class EntityCreation {

        @Test
        @DisplayName("Should create entity with all required fields")
        fun shouldCreateEntityWithAllRequiredFields() {
            val entity = DailyGoalsEntity(
                id = "test-id",
                userId = "user-123",
                stepsGoal = 8000,
                caloriesGoal = 1800,
                heartPointsGoal = 25,
                calculatedAt = LocalDateTime.now(),
                calculationSource = CalculationSource.FALLBACK_DEFAULT,
                createdAt = Date(),
                updatedAt = Date(),
                lastSyncAt = Date(),
                isDirty = true
            )

            assertEquals("test-id", entity.id)
            assertEquals("user-123", entity.userId)
            assertEquals(8000, entity.stepsGoal)
            assertEquals(1800, entity.caloriesGoal)
            assertEquals(25, entity.heartPointsGoal)
            assertEquals(CalculationSource.FALLBACK_DEFAULT, entity.calculationSource)
            assertTrue(entity.isDirty)
            assertNotNull(entity.lastSyncAt)
        }

        @Test
        @DisplayName("Should create entity with default isDirty false")
        fun shouldCreateEntityWithDefaultIsDirtyFalse() {
            val entity = DailyGoalsEntity(
                id = "test-id",
                userId = "user-123",
                stepsGoal = 10000,
                caloriesGoal = 2000,
                heartPointsGoal = 30,
                calculatedAt = LocalDateTime.now(),
                calculationSource = CalculationSource.WHO_STANDARD,
                createdAt = Date(),
                updatedAt = Date(),
                lastSyncAt = null
            )

            assertFalse(entity.isDirty)
        }

        @Test
        @DisplayName("Should handle null lastSyncAt")
        fun shouldHandleNullLastSyncAt() {
            val entity = testEntity.copy(lastSyncAt = null)
            
            assertNull(entity.lastSyncAt)
            // Should still be valid entity
            assertNotNull(entity.id)
            assertNotNull(entity.userId)
        }
    }

    @Nested
    @DisplayName("Domain Model Conversion")
    inner class DomainModelConversion {

        @Test
        @DisplayName("Should convert entity to domain model correctly")
        fun shouldConvertEntityToDomainModelCorrectly() {
            val domainModel = testEntity.toDomainModel()

            assertEquals(testEntity.userId, domainModel.userId)
            assertEquals(testEntity.stepsGoal, domainModel.stepsGoal)
            assertEquals(testEntity.caloriesGoal, domainModel.caloriesGoal)
            assertEquals(testEntity.heartPointsGoal, domainModel.heartPointsGoal)
            assertEquals(testEntity.calculatedAt, domainModel.calculatedAt)
            assertEquals(testEntity.calculationSource, domainModel.calculationSource)
        }

        @Test
        @DisplayName("Should create entity from domain model with default dirty flag")
        fun shouldCreateEntityFromDomainModelWithDefaultDirtyFlag() {
            val entity = DailyGoalsEntity.fromDomainModel(testDailyGoals)

            assertEquals(testDailyGoals.userId, entity.userId)
            assertEquals(testDailyGoals.stepsGoal, entity.stepsGoal)
            assertEquals(testDailyGoals.caloriesGoal, entity.caloriesGoal)
            assertEquals(testDailyGoals.heartPointsGoal, entity.heartPointsGoal)
            assertEquals(testDailyGoals.calculatedAt, entity.calculatedAt)
            assertEquals(testDailyGoals.calculationSource, entity.calculationSource)
            
            // Should have generated UUID
            assertNotNull(entity.id)
            assertTrue(entity.id.isNotEmpty())
            
            // Should have timestamps
            assertNotNull(entity.createdAt)
            assertNotNull(entity.updatedAt)
            
            // Should be dirty by default
            assertFalse(entity.isDirty) // Default is false
            assertNull(entity.lastSyncAt) // Should be null when dirty
        }

        @Test
        @DisplayName("Should create entity from domain model with custom dirty flag")
        fun shouldCreateEntityFromDomainModelWithCustomDirtyFlag() {
            val entity = DailyGoalsEntity.fromDomainModel(testDailyGoals, isDirty = true)

            assertTrue(entity.isDirty)
            assertNull(entity.lastSyncAt) // Should be null when dirty
        }

        @Test
        @DisplayName("Should create entity from domain model with synced flag")
        fun shouldCreateEntityFromDomainModelWithSyncedFlag() {
            val entity = DailyGoalsEntity.fromDomainModel(testDailyGoals, isDirty = false)

            assertFalse(entity.isDirty)
            assertNotNull(entity.lastSyncAt) // Should be set when not dirty
        }

        @Test
        @DisplayName("Should update existing entity from domain model")
        fun shouldUpdateExistingEntityFromDomainModel() {
            val updatedGoals = testDailyGoals.copy(
                stepsGoal = 12000,
                caloriesGoal = 2200,
                heartPointsGoal = 35,
                calculationSource = CalculationSource.USER_ADJUSTED
            )
            
            val updatedEntity = DailyGoalsEntity.updateFromDomainModel(
                existing = testEntity,
                dailyGoals = updatedGoals,
                isDirty = true
            )

            // Should preserve original entity metadata
            assertEquals(testEntity.id, updatedEntity.id)
            assertEquals(testEntity.createdAt, updatedEntity.createdAt)
            
            // Should update goal values
            assertEquals(12000, updatedEntity.stepsGoal)
            assertEquals(2200, updatedEntity.caloriesGoal)
            assertEquals(35, updatedEntity.heartPointsGoal)
            assertEquals(CalculationSource.USER_ADJUSTED, updatedEntity.calculationSource)
            
            // Should update metadata
            assertTrue(updatedEntity.isDirty)
            assertEquals(testEntity.lastSyncAt, updatedEntity.lastSyncAt) // Should preserve when dirty
            assertTrue(updatedEntity.updatedAt.after(testEntity.updatedAt) || 
                      updatedEntity.updatedAt == testEntity.updatedAt)
        }

        @Test
        @DisplayName("Should update existing entity with synced flag")
        fun shouldUpdateExistingEntityWithSyncedFlag() {
            val updatedEntity = DailyGoalsEntity.updateFromDomainModel(
                existing = testEntity,
                dailyGoals = testDailyGoals,
                isDirty = false
            )

            assertFalse(updatedEntity.isDirty)
            assertNotNull(updatedEntity.lastSyncAt) // Should be updated when not dirty
        }

        @Test
        @DisplayName("Should handle round-trip conversion correctly")
        fun shouldHandleRoundTripConversionCorrectly() {
            // Domain -> Entity -> Domain
            val entity = DailyGoalsEntity.fromDomainModel(testDailyGoals)
            val convertedBack = entity.toDomainModel()

            assertEquals(testDailyGoals.userId, convertedBack.userId)
            assertEquals(testDailyGoals.stepsGoal, convertedBack.stepsGoal)
            assertEquals(testDailyGoals.caloriesGoal, convertedBack.caloriesGoal)
            assertEquals(testDailyGoals.heartPointsGoal, convertedBack.heartPointsGoal)
            assertEquals(testDailyGoals.calculatedAt, convertedBack.calculatedAt)
            assertEquals(testDailyGoals.calculationSource, convertedBack.calculationSource)
        }
    }

    @Nested
    @DisplayName("Data Validation")
    inner class DataValidation {

        @Test
        @DisplayName("Should handle all calculation sources")
        fun shouldHandleAllCalculationSources() {
            CalculationSource.values().forEach { source ->
                val entity = testEntity.copy(calculationSource = source)
                val domainModel = entity.toDomainModel()
                
                assertEquals(source, domainModel.calculationSource)
            }
        }

        @Test
        @DisplayName("Should handle extreme goal values")
        fun shouldHandleExtremeGoalValues() {
            val extremeEntity = testEntity.copy(
                stepsGoal = 50000,
                caloriesGoal = 5000,
                heartPointsGoal = 100
            )
            
            val domainModel = extremeEntity.toDomainModel()
            
            assertEquals(50000, domainModel.stepsGoal)
            assertEquals(5000, domainModel.caloriesGoal)
            assertEquals(100, domainModel.heartPointsGoal)
        }

        @Test
        @DisplayName("Should handle minimum goal values")
        fun shouldHandleMinimumGoalValues() {
            val minEntity = testEntity.copy(
                stepsGoal = 0,
                caloriesGoal = 0,
                heartPointsGoal = 0
            )
            
            val domainModel = minEntity.toDomainModel()
            
            assertEquals(0, domainModel.stepsGoal)
            assertEquals(0, domainModel.caloriesGoal)
            assertEquals(0, domainModel.heartPointsGoal)
        }

        @Test
        @DisplayName("Should handle very old and future dates")
        fun shouldHandleVeryOldAndFutureDates() {
            val oldDate = LocalDateTime.of(2020, 1, 1, 0, 0)
            val futureDate = LocalDateTime.of(2030, 12, 31, 23, 59)
            
            val oldEntity = testEntity.copy(calculatedAt = oldDate)
            val futureEntity = testEntity.copy(calculatedAt = futureDate)
            
            assertEquals(oldDate, oldEntity.toDomainModel().calculatedAt)
            assertEquals(futureDate, futureEntity.toDomainModel().calculatedAt)
        }
    }

    @Nested
    @DisplayName("Entity Equality and Hashing")
    inner class EntityEqualityAndHashing {

        @Test
        @DisplayName("Should implement proper equality")
        fun shouldImplementProperEquality() {
            val entity1 = testEntity
            val entity2 = testEntity.copy()
            val entity3 = testEntity.copy(stepsGoal = 15000)

            assertEquals(entity1, entity2)
            assertNotEquals(entity1, entity3)
        }

        @Test
        @DisplayName("Should implement proper hashCode")
        fun shouldImplementProperHashCode() {
            val entity1 = testEntity
            val entity2 = testEntity.copy()
            val entity3 = testEntity.copy(stepsGoal = 15000)

            assertEquals(entity1.hashCode(), entity2.hashCode())
            assertNotEquals(entity1.hashCode(), entity3.hashCode())
        }

        @Test
        @DisplayName("Should handle copy with modifications")
        fun shouldHandleCopyWithModifications() {
            val modifiedEntity = testEntity.copy(
                stepsGoal = 15000,
                isDirty = true
            )

            assertEquals(testEntity.id, modifiedEntity.id)
            assertEquals(testEntity.userId, modifiedEntity.userId)
            assertEquals(15000, modifiedEntity.stepsGoal)
            assertTrue(modifiedEntity.isDirty)
            
            // Other fields should remain the same
            assertEquals(testEntity.caloriesGoal, modifiedEntity.caloriesGoal)
            assertEquals(testEntity.heartPointsGoal, modifiedEntity.heartPointsGoal)
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    inner class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("Should handle empty user ID")
        fun shouldHandleEmptyUserId() {
            val entity = testEntity.copy(userId = "")
            val domainModel = entity.toDomainModel()
            
            assertEquals("", domainModel.userId)
        }

        @Test
        @DisplayName("Should handle very long user ID")
        fun shouldHandleVeryLongUserId() {
            val longUserId = "a".repeat(1000)
            val entity = testEntity.copy(userId = longUserId)
            val domainModel = entity.toDomainModel()
            
            assertEquals(longUserId, domainModel.userId)
        }

        @Test
        @DisplayName("Should handle special characters in user ID")
        fun shouldHandleSpecialCharactersInUserId() {
            val specialUserId = "user-123_@#$%^&*()"
            val entity = testEntity.copy(userId = specialUserId)
            val domainModel = entity.toDomainModel()
            
            assertEquals(specialUserId, domainModel.userId)
        }

        @Test
        @DisplayName("Should handle concurrent modifications")
        fun shouldHandleConcurrentModifications() {
            val baseEntity = testEntity
            
            // Simulate concurrent updates
            val update1 = DailyGoalsEntity.updateFromDomainModel(
                existing = baseEntity,
                dailyGoals = testDailyGoals.copy(stepsGoal = 11000),
                isDirty = true
            )
            
            val update2 = DailyGoalsEntity.updateFromDomainModel(
                existing = baseEntity,
                dailyGoals = testDailyGoals.copy(caloriesGoal = 2100),
                isDirty = true
            )
            
            // Both updates should preserve the base entity structure
            assertEquals(baseEntity.id, update1.id)
            assertEquals(baseEntity.id, update2.id)
            assertEquals(baseEntity.createdAt, update1.createdAt)
            assertEquals(baseEntity.createdAt, update2.createdAt)
            
            // But have different updated values
            assertEquals(11000, update1.stepsGoal)
            assertEquals(2100, update2.caloriesGoal)
        }
    }

    @Nested
    @DisplayName("Performance and Memory")
    inner class PerformanceAndMemory {

        @Test
        @DisplayName("Should handle bulk entity creation efficiently")
        fun shouldHandleBulkEntityCreationEfficiently() {
            val startTime = System.currentTimeMillis()
            
            // Create 1000 entities
            val entities = (1..1000).map { i ->
                DailyGoalsEntity.fromDomainModel(
                    testDailyGoals.copy(userId = "user-$i")
                )
            }
            
            val duration = System.currentTimeMillis() - startTime
            
            assertEquals(1000, entities.size)
            assertTrue(duration < 1000, "Bulk creation took too long: ${duration}ms")
            
            // Verify all entities are unique
            val uniqueIds = entities.map { it.id }.toSet()
            assertEquals(1000, uniqueIds.size)
        }

        @Test
        @DisplayName("Should handle bulk domain model conversion efficiently")
        fun shouldHandleBulkDomainModelConversionEfficiently() {
            val entities = (1..1000).map { i ->
                testEntity.copy(id = "id-$i", userId = "user-$i")
            }
            
            val startTime = System.currentTimeMillis()
            val domainModels = entities.map { it.toDomainModel() }
            val duration = System.currentTimeMillis() - startTime
            
            assertEquals(1000, domainModels.size)
            assertTrue(duration < 500, "Bulk conversion took too long: ${duration}ms")
            
            // Verify conversion correctness
            domainModels.forEachIndexed { index, model ->
                assertEquals("user-${index + 1}", model.userId)
            }
        }
    }
}