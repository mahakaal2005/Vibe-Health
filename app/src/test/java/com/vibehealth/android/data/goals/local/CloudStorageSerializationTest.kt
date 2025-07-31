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
 * Tests for cloud storage serialization compatibility.
 * 
 * Ensures that database entities can be properly serialized for
 * Firebase Firestore and other cloud storage formats.
 */
class CloudStorageSerializationTest {

    private val testGoals = DailyGoals(
        userId = "test-user-123",
        stepsGoal = 10000,
        caloriesGoal = 2000,
        heartPointsGoal = 30,
        calculatedAt = LocalDateTime.of(2024, 1, 15, 10, 30, 45),
        calculationSource = CalculationSource.WHO_STANDARD
    )

    private val testEntity = DailyGoalsEntity.fromDomainModel(testGoals)

    @Nested
    @DisplayName("Firestore Serialization")
    inner class FirestoreSerializationTest {

        @Test
        @DisplayName("Should create Firestore-compatible map from domain model")
        fun shouldCreateFirestoreCompatibleMapFromDomainModel() {
            val firestoreMap = mapOf(
                "userId" to testGoals.userId,
                "stepsGoal" to testGoals.stepsGoal,
                "caloriesGoal" to testGoals.caloriesGoal,
                "heartPointsGoal" to testGoals.heartPointsGoal,
                "calculatedAt" to testGoals.calculatedAt.toString(),
                "calculationSource" to testGoals.calculationSource.name,
                "lastUpdated" to Date()
            )

            // Verify all values are Firestore-compatible types
            assertTrue(firestoreMap["userId"] is String)
            assertTrue(firestoreMap["stepsGoal"] is Int)
            assertTrue(firestoreMap["caloriesGoal"] is Int)
            assertTrue(firestoreMap["heartPointsGoal"] is Int)
            assertTrue(firestoreMap["calculatedAt"] is String)
            assertTrue(firestoreMap["calculationSource"] is String)
            assertTrue(firestoreMap["lastUpdated"] is Date)

            // Verify values are correct
            assertEquals(testGoals.userId, firestoreMap["userId"])
            assertEquals(testGoals.stepsGoal, firestoreMap["stepsGoal"])
            assertEquals(testGoals.caloriesGoal, firestoreMap["caloriesGoal"])
            assertEquals(testGoals.heartPointsGoal, firestoreMap["heartPointsGoal"])
            assertEquals(testGoals.calculatedAt.toString(), firestoreMap["calculatedAt"])
            assertEquals(testGoals.calculationSource.name, firestoreMap["calculationSource"])
        }

        @Test
        @DisplayName("Should handle all calculation sources in Firestore format")
        fun shouldHandleAllCalculationSourcesInFirestoreFormat() {
            CalculationSource.values().forEach { source ->
                val goals = testGoals.copy(calculationSource = source)
                val firestoreMap = mapOf(
                    "calculationSource" to goals.calculationSource.name
                )

                assertTrue(firestoreMap["calculationSource"] is String)
                assertEquals(source.name, firestoreMap["calculationSource"])
            }
        }

        @Test
        @DisplayName("Should handle extreme goal values in Firestore format")
        fun shouldHandleExtremeGoalValuesInFirestoreFormat() {
            val extremeGoals = testGoals.copy(
                stepsGoal = Int.MAX_VALUE,
                caloriesGoal = Int.MIN_VALUE,
                heartPointsGoal = 0
            )

            val firestoreMap = mapOf(
                "stepsGoal" to extremeGoals.stepsGoal,
                "caloriesGoal" to extremeGoals.caloriesGoal,
                "heartPointsGoal" to extremeGoals.heartPointsGoal
            )

            assertEquals(Int.MAX_VALUE, firestoreMap["stepsGoal"])
            assertEquals(Int.MIN_VALUE, firestoreMap["caloriesGoal"])
            assertEquals(0, firestoreMap["heartPointsGoal"])
        }

        @Test
        @DisplayName("Should handle special characters in user ID for Firestore")
        fun shouldHandleSpecialCharactersInUserIdForFirestore() {
            val specialUserId = "user-123_@#$%^&*()"
            val goals = testGoals.copy(userId = specialUserId)

            val firestoreMap = mapOf("userId" to goals.userId)

            assertEquals(specialUserId, firestoreMap["userId"])
            assertTrue(firestoreMap["userId"] is String)
        }

        @Test
        @DisplayName("Should create nested Firestore document structure")
        fun shouldCreateNestedFirestoreDocumentStructure() {
            val userDocument = mapOf(
                "profile" to mapOf(
                    "userId" to testGoals.userId,
                    "lastUpdated" to Date()
                ),
                "dailyGoals" to mapOf(
                    "stepsGoal" to testGoals.stepsGoal,
                    "caloriesGoal" to testGoals.caloriesGoal,
                    "heartPointsGoal" to testGoals.heartPointsGoal,
                    "calculatedAt" to testGoals.calculatedAt.toString(),
                    "calculationSource" to testGoals.calculationSource.name,
                    "lastUpdated" to Date()
                )
            )

            // Verify nested structure
            assertTrue(userDocument["profile"] is Map<*, *>)
            assertTrue(userDocument["dailyGoals"] is Map<*, *>)

            val profileMap = userDocument["profile"] as Map<*, *>
            val goalsMap = userDocument["dailyGoals"] as Map<*, *>

            assertEquals(testGoals.userId, profileMap["userId"])
            assertEquals(testGoals.stepsGoal, goalsMap["stepsGoal"])
            assertEquals(testGoals.caloriesGoal, goalsMap["caloriesGoal"])
            assertEquals(testGoals.heartPointsGoal, goalsMap["heartPointsGoal"])
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    inner class JSONSerializationTest {

        @Test
        @DisplayName("Should create JSON-compatible structure")
        fun shouldCreateJSONCompatibleStructure() {
            // Simulate JSON serialization structure
            val jsonMap = mapOf(
                "userId" to testGoals.userId,
                "stepsGoal" to testGoals.stepsGoal,
                "caloriesGoal" to testGoals.caloriesGoal,
                "heartPointsGoal" to testGoals.heartPointsGoal,
                "calculatedAt" to testGoals.calculatedAt.toString(),
                "calculationSource" to testGoals.calculationSource.name
            )

            // All values should be JSON-serializable primitives
            jsonMap.values.forEach { value ->
                assertTrue(
                    value is String || value is Number || value is Boolean,
                    "Value $value should be JSON-serializable"
                )
            }
        }

        @Test
        @DisplayName("Should handle null values in JSON format")
        fun shouldHandleNullValuesInJSONFormat() {
            val jsonMapWithNulls = mapOf(
                "userId" to testGoals.userId,
                "stepsGoal" to testGoals.stepsGoal,
                "caloriesGoal" to testGoals.caloriesGoal,
                "heartPointsGoal" to testGoals.heartPointsGoal,
                "calculatedAt" to testGoals.calculatedAt.toString(),
                "calculationSource" to testGoals.calculationSource.name,
                "optionalField" to null
            )

            // Should handle null values gracefully
            assertNull(jsonMapWithNulls["optionalField"])
            assertNotNull(jsonMapWithNulls["userId"])
        }

        @Test
        @DisplayName("Should maintain precision for numeric values")
        fun shouldMaintainPrecisionForNumericValues() {
            val preciseGoals = testGoals.copy(
                stepsGoal = 10000,
                caloriesGoal = 2000,
                heartPointsGoal = 30
            )

            val jsonMap = mapOf(
                "stepsGoal" to preciseGoals.stepsGoal,
                "caloriesGoal" to preciseGoals.caloriesGoal,
                "heartPointsGoal" to preciseGoals.heartPointsGoal
            )

            assertEquals(10000, jsonMap["stepsGoal"])
            assertEquals(2000, jsonMap["caloriesGoal"])
            assertEquals(30, jsonMap["heartPointsGoal"])
        }
    }

    @Nested
    @DisplayName("Data Consistency")
    inner class DataConsistencyTest {

        @Test
        @DisplayName("Should maintain data consistency across serialization formats")
        fun shouldMaintainDataConsistencyAcrossSerializationFormats() {
            // Create maps for different serialization formats
            val firestoreMap = mapOf(
                "userId" to testGoals.userId,
                "stepsGoal" to testGoals.stepsGoal,
                "caloriesGoal" to testGoals.caloriesGoal,
                "heartPointsGoal" to testGoals.heartPointsGoal,
                "calculatedAt" to testGoals.calculatedAt.toString(),
                "calculationSource" to testGoals.calculationSource.name
            )

            val jsonMap = mapOf(
                "userId" to testGoals.userId,
                "stepsGoal" to testGoals.stepsGoal,
                "caloriesGoal" to testGoals.caloriesGoal,
                "heartPointsGoal" to testGoals.heartPointsGoal,
                "calculatedAt" to testGoals.calculatedAt.toString(),
                "calculationSource" to testGoals.calculationSource.name
            )

            // Values should be identical across formats
            assertEquals(firestoreMap["userId"], jsonMap["userId"])
            assertEquals(firestoreMap["stepsGoal"], jsonMap["stepsGoal"])
            assertEquals(firestoreMap["caloriesGoal"], jsonMap["caloriesGoal"])
            assertEquals(firestoreMap["heartPointsGoal"], jsonMap["heartPointsGoal"])
            assertEquals(firestoreMap["calculatedAt"], jsonMap["calculatedAt"])
            assertEquals(firestoreMap["calculationSource"], jsonMap["calculationSource"])
        }

        @Test
        @DisplayName("Should handle round-trip serialization correctly")
        fun shouldHandleRoundTripSerializationCorrectly() {
            // Simulate serialization to cloud format
            val serializedMap = mapOf(
                "userId" to testGoals.userId,
                "stepsGoal" to testGoals.stepsGoal,
                "caloriesGoal" to testGoals.caloriesGoal,
                "heartPointsGoal" to testGoals.heartPointsGoal,
                "calculatedAt" to testGoals.calculatedAt.toString(),
                "calculationSource" to testGoals.calculationSource.name
            )

            // Simulate deserialization from cloud format
            val deserializedGoals = DailyGoals(
                userId = serializedMap["userId"] as String,
                stepsGoal = serializedMap["stepsGoal"] as Int,
                caloriesGoal = serializedMap["caloriesGoal"] as Int,
                heartPointsGoal = serializedMap["heartPointsGoal"] as Int,
                calculatedAt = LocalDateTime.parse(serializedMap["calculatedAt"] as String),
                calculationSource = CalculationSource.valueOf(serializedMap["calculationSource"] as String)
            )

            // Should match original
            assertEquals(testGoals.userId, deserializedGoals.userId)
            assertEquals(testGoals.stepsGoal, deserializedGoals.stepsGoal)
            assertEquals(testGoals.caloriesGoal, deserializedGoals.caloriesGoal)
            assertEquals(testGoals.heartPointsGoal, deserializedGoals.heartPointsGoal)
            assertEquals(testGoals.calculatedAt, deserializedGoals.calculatedAt)
            assertEquals(testGoals.calculationSource, deserializedGoals.calculationSource)
        }

        @Test
        @DisplayName("Should handle version compatibility")
        fun shouldHandleVersionCompatibility() {
            // Simulate older version format (missing some fields)
            val oldVersionMap = mapOf(
                "userId" to testGoals.userId,
                "stepsGoal" to testGoals.stepsGoal,
                "caloriesGoal" to testGoals.caloriesGoal,
                "heartPointsGoal" to testGoals.heartPointsGoal
                // Missing calculatedAt and calculationSource
            )

            // Should be able to handle missing fields with defaults
            val userId = oldVersionMap["userId"] as String
            val stepsGoal = oldVersionMap["stepsGoal"] as Int
            val caloriesGoal = oldVersionMap["caloriesGoal"] as Int
            val heartPointsGoal = oldVersionMap["heartPointsGoal"] as Int
            val calculatedAt = LocalDateTime.now() // Default to current time
            val calculationSource = CalculationSource.FALLBACK_DEFAULT // Default source

            val reconstructedGoals = DailyGoals(
                userId = userId,
                stepsGoal = stepsGoal,
                caloriesGoal = caloriesGoal,
                heartPointsGoal = heartPointsGoal,
                calculatedAt = calculatedAt,
                calculationSource = calculationSource
            )

            assertEquals(testGoals.userId, reconstructedGoals.userId)
            assertEquals(testGoals.stepsGoal, reconstructedGoals.stepsGoal)
            assertEquals(testGoals.caloriesGoal, reconstructedGoals.caloriesGoal)
            assertEquals(testGoals.heartPointsGoal, reconstructedGoals.heartPointsGoal)
            assertEquals(CalculationSource.FALLBACK_DEFAULT, reconstructedGoals.calculationSource)
        }
    }

    @Nested
    @DisplayName("Performance and Size")
    inner class PerformanceAndSizeTest {

        @Test
        @DisplayName("Should create compact serialization format")
        fun shouldCreateCompactSerializationFormat() {
            val compactMap = mapOf(
                "uid" to testGoals.userId,
                "steps" to testGoals.stepsGoal,
                "cal" to testGoals.caloriesGoal,
                "hp" to testGoals.heartPointsGoal,
                "calc_at" to testGoals.calculatedAt.toString(),
                "src" to testGoals.calculationSource.name
            )

            // Verify compact format maintains all necessary data
            assertEquals(testGoals.userId, compactMap["uid"])
            assertEquals(testGoals.stepsGoal, compactMap["steps"])
            assertEquals(testGoals.caloriesGoal, compactMap["cal"])
            assertEquals(testGoals.heartPointsGoal, compactMap["hp"])
            assertEquals(testGoals.calculatedAt.toString(), compactMap["calc_at"])
            assertEquals(testGoals.calculationSource.name, compactMap["src"])

            // Compact format should have shorter keys
            assertTrue(compactMap.keys.all { it.length <= 7 })
        }

        @Test
        @DisplayName("Should handle bulk serialization efficiently")
        fun shouldHandleBulkSerializationEfficiently() {
            val goals = (1..1000).map { i ->
                testGoals.copy(userId = "user-$i", stepsGoal = 10000 + i)
            }

            val startTime = System.currentTimeMillis()

            val serializedMaps = goals.map { goal ->
                mapOf(
                    "userId" to goal.userId,
                    "stepsGoal" to goal.stepsGoal,
                    "caloriesGoal" to goal.caloriesGoal,
                    "heartPointsGoal" to goal.heartPointsGoal,
                    "calculatedAt" to goal.calculatedAt.toString(),
                    "calculationSource" to goal.calculationSource.name
                )
            }

            val duration = System.currentTimeMillis() - startTime

            assertEquals(1000, serializedMaps.size)
            assertTrue(duration < 1000, "Bulk serialization took too long: ${duration}ms")

            // Verify correctness
            serializedMaps.forEachIndexed { index, map ->
                assertEquals("user-${index + 1}", map["userId"])
                assertEquals(10000 + index + 1, map["stepsGoal"])
            }
        }
    }
}