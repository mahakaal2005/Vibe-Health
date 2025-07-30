package com.vibehealth.android.domain.user

import com.vibehealth.android.domain.common.UnitSystem
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Data class for physical measurements with robust unit conversion methods and precision handling
 */
data class PhysicalMeasurements(
    val height: Double,
    val weight: Double,
    val unitSystem: UnitSystem
) {
    companion object {
        // Conversion constants
        private const val CM_TO_INCHES = 0.393701
        private const val INCHES_TO_CM = 2.54
        private const val KG_TO_LBS = 2.20462
        private const val LBS_TO_KG = 0.453592
        private const val INCHES_PER_FOOT = 12.0

        // Precision settings
        private const val HEIGHT_PRECISION = 1
        private const val WEIGHT_PRECISION = 2

        /**
         * Parse height from imperial format (e.g., "5'10" or "5 10")
         */
        fun parseImperialHeight(heightString: String): Double? {
            return try {
                val cleaned = heightString.replace("\"", "").replace("'", " ").trim()
                val parts = cleaned.split(" ", "'").filter { it.isNotBlank() }
                
                when (parts.size) {
                    1 -> {
                        // Just inches
                        parts[0].toDouble()
                    }
                    2 -> {
                        // Feet and inches
                        val feet = parts[0].toDouble()
                        val inches = parts[1].toDouble()
                        feet * INCHES_PER_FOOT + inches
                    }
                    else -> null
                }
            } catch (e: NumberFormatException) {
                null
            }
        }

        /**
         * Create PhysicalMeasurements with data sanitization
         */
        fun createSanitized(
            height: Double,
            weight: Double,
            unitSystem: UnitSystem
        ): PhysicalMeasurements? {
            val measurements = PhysicalMeasurements(height, weight, unitSystem)
            return if (measurements.isValid()) measurements else null
        }
    }

    /**
     * Get height in centimeters, converting from imperial if necessary
     */
    fun getHeightInCm(): Int {
        return when (unitSystem) {
            UnitSystem.METRIC -> height.toInt()
            UnitSystem.IMPERIAL -> {
                // Height in imperial is stored as total inches
                val totalInches = height
                val cm = totalInches * INCHES_TO_CM
                BigDecimal(cm).setScale(0, RoundingMode.HALF_UP).toInt()
            }
        }
    }

    /**
     * Get weight in kilograms, converting from imperial if necessary
     */
    fun getWeightInKg(): Double {
        return when (unitSystem) {
            UnitSystem.METRIC -> weight
            UnitSystem.IMPERIAL -> {
                val kg = weight * LBS_TO_KG
                BigDecimal(kg).setScale(WEIGHT_PRECISION, RoundingMode.HALF_UP).toDouble()
            }
        }
    }

    /**
     * Get height in the current unit system with proper formatting
     */
    fun getFormattedHeight(): String {
        return when (unitSystem) {
            UnitSystem.METRIC -> "${height.toInt()} cm"
            UnitSystem.IMPERIAL -> {
                val totalInches = height.toInt()
                val feet = totalInches / INCHES_PER_FOOT.toInt()
                val inches = totalInches % INCHES_PER_FOOT.toInt()
                "${feet}'${inches}\""
            }
        }
    }

    /**
     * Get weight in the current unit system with proper formatting
     */
    fun getFormattedWeight(): String {
        return when (unitSystem) {
            UnitSystem.METRIC -> "${BigDecimal(weight).setScale(WEIGHT_PRECISION, RoundingMode.HALF_UP)} kg"
            UnitSystem.IMPERIAL -> "${BigDecimal(weight).setScale(WEIGHT_PRECISION, RoundingMode.HALF_UP)} lbs"
        }
    }

    /**
     * Convert height from one unit system to another
     */
    fun convertHeight(targetSystem: UnitSystem): Double {
        if (unitSystem == targetSystem) return height

        return when {
            unitSystem == UnitSystem.METRIC && targetSystem == UnitSystem.IMPERIAL -> {
                // Convert cm to total inches
                val inches = height * CM_TO_INCHES
                BigDecimal(inches).setScale(HEIGHT_PRECISION, RoundingMode.HALF_UP).toDouble()
            }
            unitSystem == UnitSystem.IMPERIAL && targetSystem == UnitSystem.METRIC -> {
                // Convert total inches to cm
                val cm = height * INCHES_TO_CM
                BigDecimal(cm).setScale(HEIGHT_PRECISION, RoundingMode.HALF_UP).toDouble()
            }
            else -> height
        }
    }

    /**
     * Convert weight from one unit system to another
     */
    fun convertWeight(targetSystem: UnitSystem): Double {
        if (unitSystem == targetSystem) return weight

        return when {
            unitSystem == UnitSystem.METRIC && targetSystem == UnitSystem.IMPERIAL -> {
                // Convert kg to lbs
                val lbs = weight * KG_TO_LBS
                BigDecimal(lbs).setScale(WEIGHT_PRECISION, RoundingMode.HALF_UP).toDouble()
            }
            unitSystem == UnitSystem.IMPERIAL && targetSystem == UnitSystem.METRIC -> {
                // Convert lbs to kg
                val kg = weight * LBS_TO_KG
                BigDecimal(kg).setScale(WEIGHT_PRECISION, RoundingMode.HALF_UP).toDouble()
            }
            else -> weight
        }
    }

    /**
     * Convert this measurement to a different unit system
     */
    fun convertTo(targetSystem: UnitSystem): PhysicalMeasurements {
        if (unitSystem == targetSystem) return this

        return PhysicalMeasurements(
            height = convertHeight(targetSystem),
            weight = convertWeight(targetSystem),
            unitSystem = targetSystem
        )
    }

    /**
     * Validate height based on unit system and reasonable ranges
     */
    fun isValidHeight(): Boolean {
        return when (unitSystem) {
            UnitSystem.METRIC -> height in 100.0..250.0 // 100cm to 250cm
            UnitSystem.IMPERIAL -> {
                val totalInches = height
                totalInches in 36.0..96.0 // 3'0" to 8'0"
            }
        }
    }

    /**
     * Validate weight based on unit system and reasonable ranges
     */
    fun isValidWeight(): Boolean {
        return when (unitSystem) {
            UnitSystem.METRIC -> weight in 30.0..300.0 // 30kg to 300kg
            UnitSystem.IMPERIAL -> weight in 66.0..660.0 // 66lbs to 660lbs
        }
    }

    /**
     * Validate both height and weight
     */
    fun isValid(): Boolean = isValidHeight() && isValidWeight()
}