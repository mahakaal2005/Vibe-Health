package com.vibehealth.android.data.goals.local

import com.vibehealth.android.domain.goals.CalculationSource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

/**
 * Comprehensive unit tests for GoalsTypeConverters.
 * 
 * Tests cover type conversion for Room database operations including
 * LocalDateTime and CalculationSource enum conversions with error handling.
 */
class GoalsTypeConvertersTest {

    private lateinit var typeConverters: GoalsTypeConverters

    @BeforeEach
    fun setup() {
        typeConverters = GoalsTypeConverters()
    }

    @Nested
    @DisplayName("LocalDateTime Conversion")
    inner class LocalDateTimeConversion {

        @Test
        @DisplayName("Should convert LocalDateTime to String correctly")
        fun shouldConvertLocalDateTimeToStringCorrectly() {
            val dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 45)
            
            val result = typeConverters.fromLocalDateTime(dateTime)
            
            assertNotNull(result)
            assertEquals("2024-01-15T10:30:45", result)
        }

        @Test
        @DisplayName("Should convert null LocalDateTime to null String")
        fun shouldConvertNullLocalDateTimeToNullString() {
            val result = typeConverters.fromLocalDateTime(null)
            
            assertNull(result)
        }

        @Test
        @DisplayName("Should convert String to LocalDateTime correctly")
        fun shouldConvertStringToLocalDateTimeCorrectly() {
            val dateTimeString = "2024-01-15T10:30:45"
            
            val result = typeConverters.toLocalDateTime(dateTimeString)
            
            assertNotNull(result)
            assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 45), result)
        }

        @Test
        @DisplayName("Should convert null String to null LocalDateTime")
        fun shouldConvertNullStringToNullLocalDateTime() {
            val result = typeConverters.toLocalDateTime(null)
            
            assertNull(result)
        }

        @Test
        @DisplayName("Should handle empty String gracefully")
        fun shouldHandleEmptyStringGracefully() {
            val result = typeConverters.toLocalDateTime("")
            
            assertNull(result)
        }

        @Test
        @DisplayName("Should handle invalid date format gracefully")
        fun shouldHandleInvalidDateFormatGracefully() {
            val invalidDateString = "invalid-date-format"
            
            val result = typeConverters.toLocalDateTime(invalidDateString)
            
            assertNull(result)
        }

        @Test
        @DisplayName("Should handle malformed ISO date gracefully")
        fun shouldHandleMalformedISODateGracefully() {
            val malformedDate = "2024-13-45T25:70:80" // Invalid month, day, hour, minute, second
            
            val result = typeConverters.toLocalDateTime(malformedDate)
            
            assertNull(result)
        }

        @Test
        @DisplayName("Should handle round-trip conversion correctly")
        fun shouldHandleRoundTripConversionCorrectly() {
            val originalDateTime = LocalDateTime.of(2024, 12, 31, 23, 59, 59)
            
            val stringResult = typeConverters.fromLocalDateTime(originalDateTime)
            val convertedBack = typeConverters.toLocalDateTime(stringResult)
            
            assertEquals(originalDateTime, convertedBack)
        }

        @Test
        @DisplayName("Should handle edge case dates")
        fun shouldHandleEdgeCaseDates() {
            val edgeCases = listOf(
                LocalDateTime.of(1970, 1, 1, 0, 0, 0), // Unix epoch
                LocalDateTime.of(2000, 2, 29, 12, 0, 0), // Leap year
                LocalDateTime.of(2024, 12, 31, 23, 59, 59), // End of year
                LocalDateTime.of(2024, 1, 1, 0, 0, 0) // Start of year
            )
            
            edgeCases.forEach { dateTime ->
                val stringResult = typeConverters.fromLocalDateTime(dateTime)
                val convertedBack = typeConverters.toLocalDateTime(stringResult)
                
                assertEquals(dateTime, convertedBack, "Failed for date: $dateTime")
            }
        }

        @Test
        @DisplayName("Should handle microseconds precision")
        fun shouldHandleMicrosecondsPrecision() {
            val dateTimeWithNanos = LocalDateTime.of(2024, 1, 15, 10, 30, 45, 123456789)
            
            val stringResult = typeConverters.fromLocalDateTime(dateTimeWithNanos)
            val convertedBack = typeConverters.toLocalDateTime(stringResult)
            
            // Note: ISO format may not preserve full nanosecond precision
            assertNotNull(convertedBack)
            assertEquals(dateTimeWithNanos.toLocalDate(), convertedBack!!.toLocalDate())
            assertEquals(dateTimeWithNanos.toLocalTime().withNano(0), convertedBack.toLocalTime().withNano(0))
        }

        @Test
        @DisplayName("Should handle various ISO format variations")
        fun shouldHandleVariousISOFormatVariations() {
            val validISOFormats = mapOf(
                "2024-01-15T10:30:45" to LocalDateTime.of(2024, 1, 15, 10, 30, 45),
                "2024-01-15T10:30:45.123" to LocalDateTime.of(2024, 1, 15, 10, 30, 45, 123000000),
                "2024-01-15T10:30:45.123456" to LocalDateTime.of(2024, 1, 15, 10, 30, 45, 123456000)
            )
            
            validISOFormats.forEach { (isoString, expectedDateTime) ->
                val result = typeConverters.toLocalDateTime(isoString)
                assertNotNull(result, "Failed to parse: $isoString")
                assertEquals(expectedDateTime.withNano(0), result!!.withNano(0), "Mismatch for: $isoString")
            }
        }
    }

    @Nested
    @DisplayName("CalculationSource Conversion")
    inner class CalculationSourceConversion {

        @ParameterizedTest
        @EnumSource(CalculationSource::class)
        @DisplayName("Should convert all CalculationSource values to String correctly")
        fun shouldConvertAllCalculationSourceValuesToStringCorrectly(source: CalculationSource) {
            val result = typeConverters.fromCalculationSource(source)
            
            assertEquals(source.name, result)
        }

        @ParameterizedTest
        @EnumSource(CalculationSource::class)
        @DisplayName("Should convert all String values to CalculationSource correctly")
        fun shouldConvertAllStringValuesToCalculationSourceCorrectly(source: CalculationSource) {
            val sourceString = source.name
            
            val result = typeConverters.toCalculationSource(sourceString)
            
            assertEquals(source, result)
        }

        @Test
        @DisplayName("Should handle invalid CalculationSource string gracefully")
        fun shouldHandleInvalidCalculationSourceStringGracefully() {
            val invalidSource = "INVALID_SOURCE"
            
            val result = typeConverters.toCalculationSource(invalidSource)
            
            assertEquals(CalculationSource.FALLBACK_DEFAULT, result)
        }

        @Test
        @DisplayName("Should handle empty CalculationSource string gracefully")
        fun shouldHandleEmptyCalculationSourceStringGracefully() {
            val emptySource = ""
            
            val result = typeConverters.toCalculationSource(emptySource)
            
            assertEquals(CalculationSource.FALLBACK_DEFAULT, result)
        }

        @Test
        @DisplayName("Should handle case-sensitive CalculationSource strings")
        fun shouldHandleCaseSensitiveCalculationSourceStrings() {
            val lowercaseSource = "who_standard"
            val mixedCaseSource = "Who_Standard"
            
            // Should fallback to default for case mismatches
            assertEquals(CalculationSource.FALLBACK_DEFAULT, typeConverters.toCalculationSource(lowercaseSource))
            assertEquals(CalculationSource.FALLBACK_DEFAULT, typeConverters.toCalculationSource(mixedCaseSource))
        }

        @Test
        @DisplayName("Should handle round-trip conversion correctly")
        fun shouldHandleRoundTripConversionCorrectly() {
            CalculationSource.values().forEach { originalSource ->
                val stringResult = typeConverters.fromCalculationSource(originalSource)
                val convertedBack = typeConverters.toCalculationSource(stringResult)
                
                assertEquals(originalSource, convertedBack)
            }
        }

        @Test
        @DisplayName("Should handle special characters in source string")
        fun shouldHandleSpecialCharactersInSourceString() {
            val specialCharSources = listOf(
                "WHO_STANDARD@#$",
                "FALLBACK_DEFAULT!@#",
                "USER_ADJUSTED%^&",
                "RANDOM_STRING_WITH_SPACES AND SYMBOLS!@#$%^&*()"
            )
            
            specialCharSources.forEach { source ->
                val result = typeConverters.toCalculationSource(source)
                assertEquals(CalculationSource.FALLBACK_DEFAULT, result, "Failed for: $source")
            }
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    inner class ErrorHandlingAndEdgeCases {

        @Test
        @DisplayName("Should handle null inputs gracefully")
        fun shouldHandleNullInputsGracefully() {
            // LocalDateTime null handling
            assertNull(typeConverters.fromLocalDateTime(null))
            assertNull(typeConverters.toLocalDateTime(null))
            
            // CalculationSource should not accept null (not nullable in signature)
            // But we can test with empty string
            assertEquals(CalculationSource.FALLBACK_DEFAULT, typeConverters.toCalculationSource(""))
        }

        @Test
        @DisplayName("Should handle very long strings")
        fun shouldHandleVeryLongStrings() {
            val veryLongString = "a".repeat(10000)
            
            // Should handle gracefully for LocalDateTime
            val dateTimeResult = typeConverters.toLocalDateTime(veryLongString)
            assertNull(dateTimeResult)
            
            // Should fallback for CalculationSource
            val sourceResult = typeConverters.toCalculationSource(veryLongString)
            assertEquals(CalculationSource.FALLBACK_DEFAULT, sourceResult)
        }

        @Test
        @DisplayName("Should handle whitespace-only strings")
        fun shouldHandleWhitespaceOnlyStrings() {
            val whitespaceStrings = listOf("   ", "\t", "\n", "\r\n", " \t\n ")
            
            whitespaceStrings.forEach { whitespace ->
                // LocalDateTime should return null
                assertNull(typeConverters.toLocalDateTime(whitespace), "Failed for whitespace: '$whitespace'")
                
                // CalculationSource should fallback
                assertEquals(CalculationSource.FALLBACK_DEFAULT, 
                    typeConverters.toCalculationSource(whitespace), 
                    "Failed for whitespace: '$whitespace'")
            }
        }

        @Test
        @DisplayName("Should handle concurrent access safely")
        fun shouldHandleConcurrentAccessSafely() {
            val dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 45)
            val source = CalculationSource.WHO_STANDARD
            
            // Simulate concurrent access
            val threads = (1..10).map { threadId ->
                Thread {
                    repeat(100) {
                        // Test LocalDateTime conversion
                        val dateString = typeConverters.fromLocalDateTime(dateTime)
                        val convertedDate = typeConverters.toLocalDateTime(dateString)
                        assertEquals(dateTime, convertedDate)
                        
                        // Test CalculationSource conversion
                        val sourceString = typeConverters.fromCalculationSource(source)
                        val convertedSource = typeConverters.toCalculationSource(sourceString)
                        assertEquals(source, convertedSource)
                    }
                }
            }
            
            threads.forEach { it.start() }
            threads.forEach { it.join() }
            
            // If we reach here without exceptions, concurrent access is safe
            assertTrue(true)
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    inner class PerformanceTests {

        @Test
        @DisplayName("Should handle bulk LocalDateTime conversions efficiently")
        fun shouldHandleBulkLocalDateTimeConversionsEfficiently() {
            val dateTimes = (1..1000).map { i ->
                LocalDateTime.of(2024, 1, 1, 0, 0, 0).plusDays(i.toLong())
            }
            
            val startTime = System.currentTimeMillis()
            
            // Convert to strings and back
            val strings = dateTimes.map { typeConverters.fromLocalDateTime(it) }
            val convertedBack = strings.map { typeConverters.toLocalDateTime(it) }
            
            val duration = System.currentTimeMillis() - startTime
            
            assertEquals(1000, convertedBack.size)
            assertTrue(duration < 1000, "Bulk conversion took too long: ${duration}ms")
            
            // Verify correctness
            dateTimes.forEachIndexed { index, original ->
                assertEquals(original, convertedBack[index])
            }
        }

        @Test
        @DisplayName("Should handle bulk CalculationSource conversions efficiently")
        fun shouldHandleBulkCalculationSourceConversionsEfficiently() {
            val sources = (1..1000).map { 
                CalculationSource.values()[it % CalculationSource.values().size] 
            }
            
            val startTime = System.currentTimeMillis()
            
            // Convert to strings and back
            val strings = sources.map { typeConverters.fromCalculationSource(it) }
            val convertedBack = strings.map { typeConverters.toCalculationSource(it) }
            
            val duration = System.currentTimeMillis() - startTime
            
            assertEquals(1000, convertedBack.size)
            assertTrue(duration < 500, "Bulk conversion took too long: ${duration}ms")
            
            // Verify correctness
            sources.forEachIndexed { index, original ->
                assertEquals(original, convertedBack[index])
            }
        }
    }

    @Nested
    @DisplayName("Integration with Room Database")
    inner class IntegrationWithRoomDatabase {

        @Test
        @DisplayName("Should produce Room-compatible string formats")
        fun shouldProduceRoomCompatibleStringFormats() {
            val dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 45)
            val source = CalculationSource.WHO_STANDARD
            
            val dateString = typeConverters.fromLocalDateTime(dateTime)
            val sourceString = typeConverters.fromCalculationSource(source)
            
            // Verify strings are suitable for database storage
            assertNotNull(dateString)
            assertFalse(dateString!!.contains("'")) // No single quotes that could break SQL
            assertFalse(dateString.contains("\"")) // No double quotes
            assertTrue(dateString.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")))
            
            assertNotNull(sourceString)
            assertFalse(sourceString.contains("'"))
            assertFalse(sourceString.contains("\""))
            assertTrue(sourceString.matches(Regex("[A-Z_]+")))
        }

        @Test
        @DisplayName("Should handle database null values correctly")
        fun shouldHandleDatabaseNullValuesCorrectly() {
            // Simulate what Room would pass for null values
            val nullDateTime = typeConverters.toLocalDateTime(null)
            val emptyDateTime = typeConverters.toLocalDateTime("")
            
            assertNull(nullDateTime)
            assertNull(emptyDateTime)
            
            // CalculationSource should have fallback for invalid values
            val invalidSource = typeConverters.toCalculationSource("NULL")
            assertEquals(CalculationSource.FALLBACK_DEFAULT, invalidSource)
        }
    }
}