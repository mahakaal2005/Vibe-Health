package com.vibehealth.android.core.utils

import com.vibehealth.android.domain.common.UnitSystem
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Comprehensive unit conversion utilities with precision handling and edge case management
 */
object UnitConversionUtils {

    // Conversion constants with high precision
    private const val CM_TO_INCHES = 0.393701
    private const val INCHES_TO_CM = 2.54
    private const val KG_TO_LBS = 2.20462262185
    private const val LBS_TO_KG = 0.45359237
    private const val INCHES_PER_FOOT = 12.0

    // Precision settings
    private const val HEIGHT_PRECISION = 1
    private const val WEIGHT_PRECISION = 2
    private const val CONVERSION_PRECISION = 6

    /**
     * Convert height between unit systems with precision handling
     */
    fun convertHeight(
        value: Double,
        fromSystem: UnitSystem,
        toSystem: UnitSystem
    ): ConversionResult<Double> {
        if (fromSystem == toSystem) {
            return ConversionResult.Success(value)
        }

        return try {
            val result = when {
                fromSystem == UnitSystem.METRIC && toSystem == UnitSystem.IMPERIAL -> {
                    // Convert cm to total inches
                    val inches = value * CM_TO_INCHES
                    BigDecimal(inches).setScale(HEIGHT_PRECISION, RoundingMode.HALF_UP).toDouble()
                }
                fromSystem == UnitSystem.IMPERIAL && toSystem == UnitSystem.METRIC -> {
                    // Convert total inches to cm
                    val cm = value * INCHES_TO_CM
                    BigDecimal(cm).setScale(HEIGHT_PRECISION, RoundingMode.HALF_UP).toDouble()
                }
                else -> value
            }

            // Validate result is within reasonable bounds
            if (isValidHeight(result, toSystem)) {
                ConversionResult.Success(result)
            } else {
                ConversionResult.Error("Converted height is outside valid range")
            }
        } catch (e: Exception) {
            ConversionResult.Error("Height conversion failed: ${e.message}")
        }
    }

    /**
     * Convert weight between unit systems with precision handling
     */
    fun convertWeight(
        value: Double,
        fromSystem: UnitSystem,
        toSystem: UnitSystem
    ): ConversionResult<Double> {
        if (fromSystem == toSystem) {
            return ConversionResult.Success(value)
        }

        return try {
            val result = when {
                fromSystem == UnitSystem.METRIC && toSystem == UnitSystem.IMPERIAL -> {
                    // Convert kg to lbs
                    val lbs = value * KG_TO_LBS
                    BigDecimal(lbs).setScale(WEIGHT_PRECISION, RoundingMode.HALF_UP).toDouble()
                }
                fromSystem == UnitSystem.IMPERIAL && toSystem == UnitSystem.METRIC -> {
                    // Convert lbs to kg
                    val kg = value * LBS_TO_KG
                    BigDecimal(kg).setScale(WEIGHT_PRECISION, RoundingMode.HALF_UP).toDouble()
                }
                else -> value
            }

            // Validate result is within reasonable bounds
            if (isValidWeight(result, toSystem)) {
                ConversionResult.Success(result)
            } else {
                ConversionResult.Error("Converted weight is outside valid range")
            }
        } catch (e: Exception) {
            ConversionResult.Error("Weight conversion failed: ${e.message}")
        }
    }

    /**
     * Convert height from cm to feet and inches
     */
    fun convertCmToFeetInches(cm: Int): Pair<Int, Int> {
        val totalInches = (cm * CM_TO_INCHES).toInt()
        val feet = totalInches / INCHES_PER_FOOT.toInt()
        val inches = totalInches % INCHES_PER_FOOT.toInt()
        return Pair(feet, inches)
    }

    /**
     * Convert feet and inches to total inches
     */
    fun convertFeetInchesToTotalInches(feet: Int, inches: Int): Double {
        return feet * INCHES_PER_FOOT + inches
    }

    /**
     * Convert feet and inches to cm
     */
    fun convertFeetInchesToCm(feet: Int, inches: Int): Int {
        val totalInches = convertFeetInchesToTotalInches(feet, inches)
        return (totalInches * INCHES_TO_CM).toInt()
    }

    /**
     * Parse imperial height string and convert to total inches
     */
    fun parseImperialHeight(heightString: String): ConversionResult<Double> {
        return try {
            val cleaned = heightString.replace("\"", "").replace("'", " ").trim()
            val parts = cleaned.split(" ", "'").filter { it.isNotBlank() }
            
            val totalInches = when (parts.size) {
                1 -> {
                    val value = parts[0].toDouble()
                    // If value is greater than 12, assume it's total inches, otherwise feet
                    if (value > 12) value else value * INCHES_PER_FOOT
                }
                2 -> {
                    val feet = parts[0].toDouble()
                    val inches = parts[1].toDouble()
                    feet * INCHES_PER_FOOT + inches
                }
                else -> return ConversionResult.Error("Invalid height format")
            }

            if (isValidHeight(totalInches, UnitSystem.IMPERIAL)) {
                ConversionResult.Success(totalInches)
            } else {
                ConversionResult.Error("Height is outside valid range")
            }
        } catch (e: NumberFormatException) {
            ConversionResult.Error("Invalid number format in height")
        }
    }

    /**
     * Format height for display based on unit system
     */
    fun formatHeight(value: Double, unitSystem: UnitSystem): String {
        return when (unitSystem) {
            UnitSystem.METRIC -> "${value.toInt()} cm"
            UnitSystem.IMPERIAL -> {
                val totalInches = value.toInt()
                val feet = totalInches / INCHES_PER_FOOT.toInt()
                val inches = totalInches % INCHES_PER_FOOT.toInt()
                "${feet}'${inches}\""
            }
        }
    }

    /**
     * Format weight for display based on unit system
     */
    fun formatWeight(value: Double, unitSystem: UnitSystem): String {
        return when (unitSystem) {
            UnitSystem.METRIC -> "${BigDecimal(value).setScale(WEIGHT_PRECISION, RoundingMode.HALF_UP)} kg"
            UnitSystem.IMPERIAL -> "${BigDecimal(value).setScale(WEIGHT_PRECISION, RoundingMode.HALF_UP)} lbs"
        }
    }

    /**
     * Validate height based on unit system
     */
    private fun isValidHeight(height: Double, unitSystem: UnitSystem): Boolean {
        return when (unitSystem) {
            UnitSystem.METRIC -> height in 100.0..250.0 // 100cm to 250cm
            UnitSystem.IMPERIAL -> height in 36.0..96.0 // 3'0" to 8'0" in total inches
        }
    }

    /**
     * Validate weight based on unit system
     */
    private fun isValidWeight(weight: Double, unitSystem: UnitSystem): Boolean {
        return when (unitSystem) {
            UnitSystem.METRIC -> weight in 30.0..300.0 // 30kg to 300kg
            UnitSystem.IMPERIAL -> weight in 66.0..660.0 // 66lbs to 660lbs
        }
    }

    /**
     * Get height range for unit system
     */
    fun getHeightRange(unitSystem: UnitSystem): Pair<Double, Double> {
        return when (unitSystem) {
            UnitSystem.METRIC -> Pair(100.0, 250.0)
            UnitSystem.IMPERIAL -> Pair(36.0, 96.0) // Total inches
        }
    }

    /**
     * Get weight range for unit system
     */
    fun getWeightRange(unitSystem: UnitSystem): Pair<Double, Double> {
        return when (unitSystem) {
            UnitSystem.METRIC -> Pair(30.0, 300.0)
            UnitSystem.IMPERIAL -> Pair(66.0, 660.0)
        }
    }

    /**
     * Convert all measurements to metric (internal storage format)
     */
    fun convertToMetric(
        height: Double,
        weight: Double,
        fromSystem: UnitSystem
    ): ConversionResult<Pair<Int, Double>> {
        val heightResult = convertHeight(height, fromSystem, UnitSystem.METRIC)
        val weightResult = convertWeight(weight, fromSystem, UnitSystem.METRIC)

        return when {
            heightResult is ConversionResult.Error -> ConversionResult.Error(heightResult.message)
            weightResult is ConversionResult.Error -> ConversionResult.Error(weightResult.message)
            else -> {
                val heightInCm = (heightResult as ConversionResult.Success).data.toInt()
                val weightInKg = (weightResult as ConversionResult.Success).data
                ConversionResult.Success(Pair(heightInCm, weightInKg))
            }
        }
    }

    /**
     * Create conversion matrix for testing edge cases
     */
    fun createConversionTestMatrix(): List<ConversionTestCase> {
        return listOf(
            // Height conversions
            ConversionTestCase("Height: 170cm to imperial", 170.0, UnitSystem.METRIC, UnitSystem.IMPERIAL, 66.9),
            ConversionTestCase("Height: 72in to metric", 72.0, UnitSystem.IMPERIAL, UnitSystem.METRIC, 183.0),
            ConversionTestCase("Height: Edge case 100cm", 100.0, UnitSystem.METRIC, UnitSystem.IMPERIAL, 39.4),
            ConversionTestCase("Height: Edge case 250cm", 250.0, UnitSystem.METRIC, UnitSystem.IMPERIAL, 98.4),
            
            // Weight conversions
            ConversionTestCase("Weight: 70kg to imperial", 70.0, UnitSystem.METRIC, UnitSystem.IMPERIAL, 154.32),
            ConversionTestCase("Weight: 150lbs to metric", 150.0, UnitSystem.IMPERIAL, UnitSystem.METRIC, 68.04),
            ConversionTestCase("Weight: Edge case 30kg", 30.0, UnitSystem.METRIC, UnitSystem.IMPERIAL, 66.14),
            ConversionTestCase("Weight: Edge case 300kg", 300.0, UnitSystem.METRIC, UnitSystem.IMPERIAL, 661.39)
        )
    }
}

/**
 * Sealed class for conversion results
 */
sealed class ConversionResult<out T> {
    data class Success<T>(val data: T) : ConversionResult<T>()
    data class Error(val message: String) : ConversionResult<Nothing>()
}

/**
 * Data class for conversion test cases
 */
data class ConversionTestCase(
    val description: String,
    val inputValue: Double,
    val fromSystem: UnitSystem,
    val toSystem: UnitSystem,
    val expectedResult: Double
)