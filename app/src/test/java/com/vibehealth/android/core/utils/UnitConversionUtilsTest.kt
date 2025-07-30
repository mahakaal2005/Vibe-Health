package com.vibehealth.android.core.utils

import com.vibehealth.android.domain.user.UnitSystem
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for UnitConversionUtils with precision and edge case testing
 */
class UnitConversionUtilsTest {

    companion object {
        private const val PRECISION_DELTA = 0.01 // Acceptable precision difference
    }

    @Test
    fun convertHeight_metricToImperial_shouldConvertCorrectly() {
        // Given
        val testCases = mapOf(
            170.0 to 66.9, // 170 cm to inches
            180.0 to 70.9, // 180 cm to inches
            160.0 to 63.0, // 160 cm to inches
            200.0 to 78.7  // 200 cm to inches
        )

        // When & Then
        testCases.forEach { (cm, expectedInches) ->
            val result = UnitConversionUtils.convertHeight(cm, UnitSystem.METRIC, UnitSystem.IMPERIAL)
            assertTrue(result is ConversionResult.Success, "Failed to convert $cm cm")
            val actualInches = (result as ConversionResult.Success).data
            assertTrue(abs(actualInches - expectedInches) < PRECISION_DELTA, 
                "Expected $expectedInches, got $actualInches for $cm cm")
        }
    }

    @Test
    fun convertHeight_imperialToMetric_shouldConvertCorrectly() {
        // Given
        val testCases = mapOf(
            72.0 to 183.0, // 72 inches (6'0") to cm
            60.0 to 152.0, // 60 inches (5'0") to cm
            66.0 to 168.0, // 66 inches (5'6") to cm
            78.0 to 198.0  // 78 inches (6'6") to cm
        )

        // When & Then
        testCases.forEach { (inches, expectedCm) ->
            val result = UnitConversionUtils.convertHeight(inches, UnitSystem.IMPERIAL, UnitSystem.METRIC)
            assertTrue(result is ConversionResult.Success, "Failed to convert $inches inches")
            val actualCm = (result as ConversionResult.Success).data
            assertTrue(abs(actualCm - expectedCm) < 1.0, 
                "Expected $expectedCm, got $actualCm for $inches inches")
        }
    }

    @Test
    fun convertHeight_sameUnitSystem_shouldReturnOriginalValue() {
        // Given
        val height = 175.0

        // When
        val metricResult = UnitConversionUtils.convertHeight(height, UnitSystem.METRIC, UnitSystem.METRIC)
        val imperialResult = UnitConversionUtils.convertHeight(height, UnitSystem.IMPERIAL, UnitSystem.IMPERIAL)

        // Then
        assertTrue(metricResult is ConversionResult.Success)
        assertTrue(imperialResult is ConversionResult.Success)
        assertEquals(height, (metricResult as ConversionResult.Success).data)
        assertEquals(height, (imperialResult as ConversionResult.Success).data)
    }

    @Test
    fun convertWeight_metricToImperial_shouldConvertCorrectly() {
        // Given
        val testCases = mapOf(
            70.0 to 154.32, // 70 kg to lbs
            80.0 to 176.37, // 80 kg to lbs
            60.0 to 132.28, // 60 kg to lbs
            100.0 to 220.46 // 100 kg to lbs
        )

        // When & Then
        testCases.forEach { (kg, expectedLbs) ->
            val result = UnitConversionUtils.convertWeight(kg, UnitSystem.METRIC, UnitSystem.IMPERIAL)
            assertTrue(result is ConversionResult.Success, "Failed to convert $kg kg")
            val actualLbs = (result as ConversionResult.Success).data
            assertTrue(abs(actualLbs - expectedLbs) < PRECISION_DELTA, 
                "Expected $expectedLbs, got $actualLbs for $kg kg")
        }
    }

    @Test
    fun convertWeight_imperialToMetric_shouldConvertCorrectly() {
        // Given
        val testCases = mapOf(
            150.0 to 68.04, // 150 lbs to kg
            200.0 to 90.72, // 200 lbs to kg
            120.0 to 54.43, // 120 lbs to kg
            180.0 to 81.65  // 180 lbs to kg
        )

        // When & Then
        testCases.forEach { (lbs, expectedKg) ->
            val result = UnitConversionUtils.convertWeight(lbs, UnitSystem.IMPERIAL, UnitSystem.METRIC)
            assertTrue(result is ConversionResult.Success, "Failed to convert $lbs lbs")
            val actualKg = (result as ConversionResult.Success).data
            assertTrue(abs(actualKg - expectedKg) < PRECISION_DELTA, 
                "Expected $expectedKg, got $actualKg for $lbs lbs")
        }
    }

    @Test
    fun convertWeight_sameUnitSystem_shouldReturnOriginalValue() {
        // Given
        val weight = 75.5

        // When
        val metricResult = UnitConversionUtils.convertWeight(weight, UnitSystem.METRIC, UnitSystem.METRIC)
        val imperialResult = UnitConversionUtils.convertWeight(weight, UnitSystem.IMPERIAL, UnitSystem.IMPERIAL)

        // Then
        assertTrue(metricResult is ConversionResult.Success)
        assertTrue(imperialResult is ConversionResult.Success)
        assertEquals(weight, (metricResult as ConversionResult.Success).data)
        assertEquals(weight, (imperialResult as ConversionResult.Success).data)
    }

    @Test
    fun convertHeight_withInvalidValues_shouldReturnError() {
        // Given
        val invalidHeights = listOf(-10.0, 0.0, 500.0, 1000.0)

        // When & Then
        invalidHeights.forEach { height ->
            val result = UnitConversionUtils.convertHeight(height, UnitSystem.METRIC, UnitSystem.IMPERIAL)
            assertTrue(result is ConversionResult.Error, "Should fail for invalid height: $height")
        }
    }

    @Test
    fun convertWeight_withInvalidValues_shouldReturnError() {
        // Given
        val invalidWeights = listOf(-10.0, 0.0, 1000.0, 2000.0)

        // When & Then
        invalidWeights.forEach { weight ->
            val result = UnitConversionUtils.convertWeight(weight, UnitSystem.METRIC, UnitSystem.IMPERIAL)
            assertTrue(result is ConversionResult.Error, "Should fail for invalid weight: $weight")
        }
    }

    @Test
    fun convertCmToFeetInches_shouldConvertCorrectly() {
        // Given
        val testCases = mapOf(
            170 to Pair(5, 7), // 170 cm = 5'7"
            180 to Pair(5, 11), // 180 cm = 5'11"
            160 to Pair(5, 3),  // 160 cm = 5'3"
            183 to Pair(6, 0)   // 183 cm = 6'0"
        )

        // When & Then
        testCases.forEach { (cm, expected) ->
            val result = UnitConversionUtils.convertCmToFeetInches(cm)
            assertEquals(expected, result, "Failed for $cm cm")
        }
    }

    @Test
    fun convertFeetInchesToTotalInches_shouldConvertCorrectly() {
        // Given
        val testCases = mapOf(
            Pair(5, 7) to 67.0,  // 5'7" = 67 inches
            Pair(6, 0) to 72.0,  // 6'0" = 72 inches
            Pair(5, 11) to 71.0, // 5'11" = 71 inches
            Pair(4, 10) to 58.0  // 4'10" = 58 inches
        )

        // When & Then
        testCases.forEach { (feetInches, expectedInches) ->
            val result = UnitConversionUtils.convertFeetInchesToTotalInches(feetInches.first, feetInches.second)
            assertEquals(expectedInches, result, "Failed for ${feetInches.first}'${feetInches.second}\"")
        }
    }

    @Test
    fun convertFeetInchesToCm_shouldConvertCorrectly() {
        // Given
        val testCases = mapOf(
            Pair(5, 7) to 170,  // 5'7" = 170 cm
            Pair(6, 0) to 183,  // 6'0" = 183 cm
            Pair(5, 11) to 180, // 5'11" = 180 cm
            Pair(5, 3) to 160   // 5'3" = 160 cm
        )

        // When & Then
        testCases.forEach { (feetInches, expectedCm) ->
            val result = UnitConversionUtils.convertFeetInchesToCm(feetInches.first, feetInches.second)
            assertTrue(abs(result - expectedCm) <= 1, 
                "Expected $expectedCm, got $result for ${feetInches.first}'${feetInches.second}\"")
        }
    }

    @Test
    fun parseImperialHeight_withValidFormats_shouldReturnCorrectInches() {
        // Given
        val testCases = mapOf(
            "5'10\"" to 70.0,
            "5'10" to 70.0,
            "5 10" to 70.0,
            "6'0\"" to 72.0,
            "6'0" to 72.0,
            "6 0" to 72.0,
            "72" to 72.0,    // Just inches
            "5" to 60.0      // Just feet (converted to inches)
        )

        // When & Then
        testCases.forEach { (input, expected) ->
            val result = UnitConversionUtils.parseImperialHeight(input)
            assertTrue(result is ConversionResult.Success, "Failed to parse: $input")
            assertEquals(expected, (result as ConversionResult.Success).data, "Incorrect result for: $input")
        }
    }

    @Test
    fun parseImperialHeight_withInvalidFormats_shouldReturnError() {
        // Given
        val invalidFormats = listOf(
            "",
            "abc",
            "5'",
            "'10",
            "5'10'5",
            "invalid",
            "25'0", // Invalid feet value
            "5'20"  // Invalid inches value
        )

        // When & Then
        invalidFormats.forEach { input ->
            val result = UnitConversionUtils.parseImperialHeight(input)
            assertTrue(result is ConversionResult.Error, "Should return error for: $input")
        }
    }

    @Test
    fun formatHeight_shouldFormatCorrectly() {
        // Given
        val metricHeight = 175.0
        val imperialHeight = 70.0 // inches

        // When
        val metricFormatted = UnitConversionUtils.formatHeight(metricHeight, UnitSystem.METRIC)
        val imperialFormatted = UnitConversionUtils.formatHeight(imperialHeight, UnitSystem.IMPERIAL)

        // Then
        assertEquals("175 cm", metricFormatted)
        assertEquals("5'10\"", imperialFormatted)
    }

    @Test
    fun formatWeight_shouldFormatCorrectly() {
        // Given
        val metricWeight = 70.5
        val imperialWeight = 154.32

        // When
        val metricFormatted = UnitConversionUtils.formatWeight(metricWeight, UnitSystem.METRIC)
        val imperialFormatted = UnitConversionUtils.formatWeight(imperialWeight, UnitSystem.IMPERIAL)

        // Then
        assertEquals("70.50 kg", metricFormatted)
        assertEquals("154.32 lbs", imperialFormatted)
    }

    @Test
    fun getHeightRange_shouldReturnCorrectRanges() {
        // When
        val metricRange = UnitConversionUtils.getHeightRange(UnitSystem.METRIC)
        val imperialRange = UnitConversionUtils.getHeightRange(UnitSystem.IMPERIAL)

        // Then
        assertEquals(Pair(100.0, 250.0), metricRange)
        assertEquals(Pair(36.0, 96.0), imperialRange)
    }

    @Test
    fun getWeightRange_shouldReturnCorrectRanges() {
        // When
        val metricRange = UnitConversionUtils.getWeightRange(UnitSystem.METRIC)
        val imperialRange = UnitConversionUtils.getWeightRange(UnitSystem.IMPERIAL)

        // Then
        assertEquals(Pair(30.0, 300.0), metricRange)
        assertEquals(Pair(66.0, 660.0), imperialRange)
    }

    @Test
    fun convertToMetric_shouldConvertBothMeasurements() {
        // Given
        val imperialHeight = 70.0 // inches
        val imperialWeight = 154.32 // lbs

        // When
        val result = UnitConversionUtils.convertToMetric(
            imperialHeight, 
            imperialWeight, 
            UnitSystem.IMPERIAL
        )

        // Then
        assertTrue(result is ConversionResult.Success)
        val (heightInCm, weightInKg) = (result as ConversionResult.Success).data
        
        assertTrue(abs(heightInCm - 178) <= 1) // ~178 cm
        assertTrue(abs(weightInKg - 70.0) < 0.1) // ~70 kg
    }

    @Test
    fun convertToMetric_withMetricInput_shouldReturnSameValues() {
        // Given
        val metricHeight = 175.0
        val metricWeight = 70.0

        // When
        val result = UnitConversionUtils.convertToMetric(
            metricHeight, 
            metricWeight, 
            UnitSystem.METRIC
        )

        // Then
        assertTrue(result is ConversionResult.Success)
        val (heightInCm, weightInKg) = (result as ConversionResult.Success).data
        
        assertEquals(175, heightInCm)
        assertEquals(70.0, weightInKg)
    }

    @Test
    fun convertToMetric_withInvalidValues_shouldReturnError() {
        // Given
        val invalidHeight = -10.0
        val validWeight = 70.0

        // When
        val result = UnitConversionUtils.convertToMetric(
            invalidHeight, 
            validWeight, 
            UnitSystem.METRIC
        )

        // Then
        assertTrue(result is ConversionResult.Error)
    }

    @Test
    fun createConversionTestMatrix_shouldReturnTestCases() {
        // When
        val testMatrix = UnitConversionUtils.createConversionTestMatrix()

        // Then
        assertTrue(testMatrix.isNotEmpty())
        assertTrue(testMatrix.size >= 8) // Should have at least 8 test cases
        
        // Verify test case structure
        testMatrix.forEach { testCase ->
            assertNotNull(testCase.description)
            assertTrue(testCase.inputValue > 0)
            assertNotNull(testCase.fromSystem)
            assertNotNull(testCase.toSystem)
            assertTrue(testCase.expectedResult > 0)
        }
    }

    @Test
    fun conversionResult_helperMethods_shouldWork() {
        // Given
        val successResult = ConversionResult.Success(42.0)
        val errorResult = ConversionResult.Error("Test error")

        // Then
        assertTrue(successResult is ConversionResult.Success)
        assertFalse(successResult is ConversionResult.Error)
        assertEquals(42.0, successResult.data)

        assertTrue(errorResult is ConversionResult.Error)
        assertFalse(errorResult is ConversionResult.Success)
        assertEquals("Test error", errorResult.message)
    }

    @Test
    fun precisionHandling_shouldMaintainAccuracy() {
        // Given
        val preciseWeight = 70.123456789

        // When
        val result = UnitConversionUtils.convertWeight(preciseWeight, UnitSystem.METRIC, UnitSystem.IMPERIAL)

        // Then
        assertTrue(result is ConversionResult.Success)
        val convertedWeight = (result as ConversionResult.Success).data
        
        // Should maintain 2 decimal places precision
        val decimalPlaces = convertedWeight.toString().substringAfter('.').length
        assertTrue(decimalPlaces <= 2, "Should maintain precision, got $decimalPlaces decimal places")
    }

    @Test
    fun edgeCaseConversions_shouldHandleBoundaryValues() {
        // Given
        val minMetricHeight = 100.0
        val maxMetricHeight = 250.0
        val minImperialHeight = 36.0
        val maxImperialHeight = 96.0

        // When & Then - Test boundary conversions
        val minMetricResult = UnitConversionUtils.convertHeight(minMetricHeight, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val maxMetricResult = UnitConversionUtils.convertHeight(maxMetricHeight, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val minImperialResult = UnitConversionUtils.convertHeight(minImperialHeight, UnitSystem.IMPERIAL, UnitSystem.METRIC)
        val maxImperialResult = UnitConversionUtils.convertHeight(maxImperialHeight, UnitSystem.IMPERIAL, UnitSystem.METRIC)

        assertTrue(minMetricResult is ConversionResult.Success)
        assertTrue(maxMetricResult is ConversionResult.Success)
        assertTrue(minImperialResult is ConversionResult.Success)
        assertTrue(maxImperialResult is ConversionResult.Success)
    }

    @Test
    fun roundTripConversion_shouldMaintainAccuracy() {
        // Given
        val originalHeight = 175.0 // cm
        val originalWeight = 70.0 // kg

        // When - Convert to imperial and back to metric
        val heightToImperial = UnitConversionUtils.convertHeight(originalHeight, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val heightBackToMetric = UnitConversionUtils.convertHeight(
            (heightToImperial as ConversionResult.Success).data, 
            UnitSystem.IMPERIAL, 
            UnitSystem.METRIC
        )

        val weightToImperial = UnitConversionUtils.convertWeight(originalWeight, UnitSystem.METRIC, UnitSystem.IMPERIAL)
        val weightBackToMetric = UnitConversionUtils.convertWeight(
            (weightToImperial as ConversionResult.Success).data, 
            UnitSystem.IMPERIAL, 
            UnitSystem.METRIC
        )

        // Then - Should be close to original values
        val finalHeight = (heightBackToMetric as ConversionResult.Success).data
        val finalWeight = (weightBackToMetric as ConversionResult.Success).data

        assertTrue(abs(finalHeight - originalHeight) < 1.0, "Height round-trip error too large")
        assertTrue(abs(finalWeight - originalWeight) < 0.1, "Weight round-trip error too large")
    }
}