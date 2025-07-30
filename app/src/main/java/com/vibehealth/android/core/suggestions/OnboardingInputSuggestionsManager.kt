package com.vibehealth.android.core.suggestions

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.common.UnitSystem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validation feedback for input suggestions
 */
sealed class ValidationFeedback(val message: String, val type: FeedbackType) {
    class Invalid(message: String) : ValidationFeedback(message, FeedbackType.INVALID)
    class Warning(message: String) : ValidationFeedback(message, FeedbackType.WARNING)
    class Good(message: String) : ValidationFeedback(message, FeedbackType.GOOD)
    class Acceptable(message: String) : ValidationFeedback(message, FeedbackType.ACCEPTABLE)
    
    companion object {
        fun invalid(message: String) = Invalid(message)
        fun warning(message: String) = Warning(message)
        fun good(message: String) = Good(message)
        fun acceptable(message: String) = Acceptable(message)
    }
}

/**
 * Manager for intelligent input suggestions in onboarding forms
 * Provides common names, reasonable defaults, and smart suggestions
 */
@Singleton
class OnboardingInputSuggestionsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Get common name suggestions for auto-complete
     */
    fun getNameSuggestions(): List<String> {
        return listOf(
            // Common Indian names
            "Priya Sharma", "Rohan Patel", "Ananya Singh", "Arjun Kumar",
            "Kavya Reddy", "Aditya Gupta", "Sneha Joshi", "Vikram Mehta",
            "Pooja Agarwal", "Rahul Verma", "Divya Nair", "Karan Shah",
            
            // Common international names
            "John Smith", "Sarah Johnson", "Michael Brown", "Emily Davis",
            "David Wilson", "Jessica Miller", "Christopher Moore", "Ashley Taylor",
            "Matthew Anderson", "Amanda Thomas", "Joshua Jackson", "Jennifer White",
            
            // Names with common patterns
            "Alex Johnson", "Sam Wilson", "Jordan Smith", "Taylor Brown",
            "Casey Davis", "Morgan Miller", "Riley Anderson", "Avery Thomas"
        )
    }

    /**
     * Set up name auto-complete for input field
     */
    fun setupNameAutoComplete(autoCompleteTextView: AutoCompleteTextView) {
        val suggestions = getNameSuggestions()
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, suggestions)
        autoCompleteTextView.setAdapter(adapter)
        autoCompleteTextView.threshold = 2 // Start suggesting after 2 characters
    }

    /**
     * Get reasonable height defaults based on unit system and gender
     */
    fun getHeightDefaults(unitSystem: UnitSystem, gender: Gender): HeightDefaults {
        return when (unitSystem) {
            UnitSystem.METRIC -> {
                when (gender) {
                    Gender.MALE -> HeightDefaults(
                        average = "175",
                        range = listOf("165", "170", "175", "180", "185"),
                        placeholder = "175"
                    )
                    Gender.FEMALE -> HeightDefaults(
                        average = "162",
                        range = listOf("155", "160", "162", "165", "170"),
                        placeholder = "162"
                    )
                    else -> HeightDefaults(
                        average = "168",
                        range = listOf("160", "165", "168", "172", "177"),
                        placeholder = "168"
                    )
                }
            }
            UnitSystem.IMPERIAL -> {
                when (gender) {
                    Gender.MALE -> HeightDefaults(
                        average = "5.9",
                        range = listOf("5.5", "5.7", "5.9", "5.11", "6.1"),
                        placeholder = "5.9"
                    )
                    Gender.FEMALE -> HeightDefaults(
                        average = "5.4",
                        range = listOf("5.1", "5.3", "5.4", "5.5", "5.7"),
                        placeholder = "5.4"
                    )
                    else -> HeightDefaults(
                        average = "5.6",
                        range = listOf("5.3", "5.4", "5.6", "5.8", "5.10"),
                        placeholder = "5.6"
                    )
                }
            }
        }
    }

    /**
     * Get reasonable weight defaults based on unit system and gender
     */
    fun getWeightDefaults(unitSystem: UnitSystem, gender: Gender): WeightDefaults {
        return when (unitSystem) {
            UnitSystem.METRIC -> {
                when (gender) {
                    Gender.MALE -> WeightDefaults(
                        average = "70",
                        range = listOf("60", "65", "70", "75", "80"),
                        placeholder = "70"
                    )
                    Gender.FEMALE -> WeightDefaults(
                        average = "55",
                        range = listOf("45", "50", "55", "60", "65"),
                        placeholder = "55"
                    )
                    else -> WeightDefaults(
                        average = "62",
                        range = listOf("52", "57", "62", "67", "72"),
                        placeholder = "62"
                    )
                }
            }
            UnitSystem.IMPERIAL -> {
                when (gender) {
                    Gender.MALE -> WeightDefaults(
                        average = "154",
                        range = listOf("132", "143", "154", "165", "176"),
                        placeholder = "154"
                    )
                    Gender.FEMALE -> WeightDefaults(
                        average = "121",
                        range = listOf("99", "110", "121", "132", "143"),
                        placeholder = "121"
                    )
                    else -> WeightDefaults(
                        average = "137",
                        range = listOf("115", "126", "137", "148", "159"),
                        placeholder = "137"
                    )
                }
            }
        }
    }

    /**
     * Get age-appropriate suggestions based on birthday
     */
    fun getAgeBasedSuggestions(age: Int): AgeBasedSuggestions {
        return when {
            age < 18 -> AgeBasedSuggestions(
                heightAdjustment = -0.05, // Slightly shorter
                weightAdjustment = -0.1,  // Lighter
                activityLevel = "High",
                recommendations = listOf(
                    "Focus on balanced nutrition for growth",
                    "Stay active with sports and activities",
                    "Get adequate sleep for development"
                )
            )
            age in 18..30 -> AgeBasedSuggestions(
                heightAdjustment = 0.0,   // Standard
                weightAdjustment = 0.0,   // Standard
                activityLevel = "Moderate to High",
                recommendations = listOf(
                    "Maintain regular exercise routine",
                    "Build healthy eating habits",
                    "Focus on stress management"
                )
            )
            age in 31..50 -> AgeBasedSuggestions(
                heightAdjustment = 0.0,   // Standard
                weightAdjustment = 0.05,  // Slightly heavier
                activityLevel = "Moderate",
                recommendations = listOf(
                    "Include strength training",
                    "Monitor cardiovascular health",
                    "Maintain work-life balance"
                )
            )
            else -> AgeBasedSuggestions(
                heightAdjustment = -0.02, // Slightly shorter due to age
                weightAdjustment = 0.1,   // May be heavier
                activityLevel = "Light to Moderate",
                recommendations = listOf(
                    "Focus on flexibility and balance",
                    "Maintain bone health",
                    "Regular health check-ups"
                )
            )
        }
    }

    /**
     * Get smart suggestions based on partial input
     */
    fun getSmartSuggestions(field: SuggestionField, partialInput: String, context: SuggestionContext): List<String> {
        return when (field) {
            SuggestionField.NAME -> {
                getNameSuggestions().filter { name -> 
                    name.contains(partialInput, ignoreCase = true) 
                }.take(5)
            }
            SuggestionField.HEIGHT -> {
                val defaults = getHeightDefaults(context.unitSystem, context.gender)
                defaults.range.filter { height -> 
                    height.startsWith(partialInput) 
                }
            }
            SuggestionField.WEIGHT -> {
                val defaults = getWeightDefaults(context.unitSystem, context.gender)
                defaults.range.filter { weight -> 
                    weight.startsWith(partialInput) 
                }
            }
        }
    }

    /**
     * Get validation hints for better user experience
     */
    fun getValidationHints(field: SuggestionField, unitSystem: UnitSystem): String {
        return when (field) {
            SuggestionField.NAME -> "Enter your full name (2-50 characters)"
            SuggestionField.HEIGHT -> when (unitSystem) {
                UnitSystem.METRIC -> "Height in centimeters (100-250 cm)"
                UnitSystem.IMPERIAL -> "Height in feet and inches (3'0\" - 8'0\")"
            }
            SuggestionField.WEIGHT -> when (unitSystem) {
                UnitSystem.METRIC -> "Weight in kilograms (30-300 kg)"
                UnitSystem.IMPERIAL -> "Weight in pounds (66-660 lbs)"
            }
        }
    }

    /**
     * Get contextual help text
     */
    fun getContextualHelp(field: SuggestionField): String {
        return when (field) {
            SuggestionField.NAME -> "We use your name to personalize your experience"
            SuggestionField.HEIGHT -> "Height helps us calculate your personalized daily goals"
            SuggestionField.WEIGHT -> "Weight is used for accurate calorie and activity recommendations"
        }
    }

    /**
     * Check if input is reasonable and provide feedback
     */
    fun validateInputReasonableness(field: SuggestionField, value: String, context: SuggestionContext): ValidationFeedback {
        return when (field) {
            SuggestionField.HEIGHT -> validateHeightReasonableness(value, context.unitSystem)
            SuggestionField.WEIGHT -> validateWeightReasonableness(value, context.unitSystem)
            SuggestionField.NAME -> validateNameReasonableness(value)
        }
    }

    private fun validateHeightReasonableness(height: String, unitSystem: UnitSystem): ValidationFeedback {
        val numericHeight = height.toDoubleOrNull() ?: return ValidationFeedback.Invalid("Please enter a valid number")
        
        return when (unitSystem) {
            UnitSystem.METRIC -> {
                when {
                    numericHeight < 120 -> ValidationFeedback.Warning("This seems quite short. Please double-check.")
                    numericHeight > 220 -> ValidationFeedback.Warning("This seems quite tall. Please double-check.")
                    numericHeight >= 150 && numericHeight <= 190 -> ValidationFeedback.Good("Height looks good!")
                    else -> ValidationFeedback.Acceptable("Height is within normal range")
                }
            }
            UnitSystem.IMPERIAL -> {
                when {
                    numericHeight < 3.5 -> ValidationFeedback.Warning("This seems quite short. Please double-check.")
                    numericHeight > 7.5 -> ValidationFeedback.Warning("This seems quite tall. Please double-check.")
                    numericHeight >= 4.9 && numericHeight <= 6.5 -> ValidationFeedback.Good("Height looks good!")
                    else -> ValidationFeedback.Acceptable("Height is within normal range")
                }
            }
        }
    }

    private fun validateWeightReasonableness(weight: String, unitSystem: UnitSystem): ValidationFeedback {
        val numericWeight = weight.toDoubleOrNull() ?: return ValidationFeedback.Invalid("Please enter a valid number")
        
        return when (unitSystem) {
            UnitSystem.METRIC -> {
                when {
                    numericWeight < 40 -> ValidationFeedback.Warning("This seems quite light. Please double-check.")
                    numericWeight > 150 -> ValidationFeedback.Warning("This seems quite heavy. Please double-check.")
                    numericWeight in 50.0..90.0 -> ValidationFeedback.Good("Weight looks good!")
                    else -> ValidationFeedback.Acceptable("Weight is within normal range")
                }
            }
            UnitSystem.IMPERIAL -> {
                when {
                    numericWeight < 88 -> ValidationFeedback.Warning("This seems quite light. Please double-check.")
                    numericWeight > 330 -> ValidationFeedback.Warning("This seems quite heavy. Please double-check.")
                    numericWeight in 110.0..200.0 -> ValidationFeedback.Good("Weight looks good!")
                    else -> ValidationFeedback.Acceptable("Weight is within normal range")
                }
            }
        }
    }

    private fun validateNameReasonableness(name: String): ValidationFeedback {
        return when {
            name.length < 2 -> ValidationFeedback.Invalid("Name should be at least 2 characters")
            name.length > 50 -> ValidationFeedback.Invalid("Name should be less than 50 characters")
            name.matches(Regex("^[a-zA-Z\\s'-]+$")) -> ValidationFeedback.Good("Name looks good!")
            else -> ValidationFeedback.Warning("Name contains unusual characters. Please double-check.")
        }
    }
}

// Data classes for suggestions
data class HeightDefaults(
    val average: String,
    val range: List<String>,
    val placeholder: String
)

data class WeightDefaults(
    val average: String,
    val range: List<String>,
    val placeholder: String
)

data class AgeBasedSuggestions(
    val heightAdjustment: Double,
    val weightAdjustment: Double,
    val activityLevel: String,
    val recommendations: List<String>
)

data class SuggestionContext(
    val gender: Gender,
    val unitSystem: UnitSystem,
    val age: Int? = null
)

enum class SuggestionField {
    NAME,
    HEIGHT,
    WEIGHT
}

enum class FeedbackType {
    GOOD,
    ACCEPTABLE,
    WARNING,
    INVALID
}