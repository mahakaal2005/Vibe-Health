package com.vibehealth.android.ui.dashboard.models

/**
 * Enum representing the three types of wellness rings in the dashboard.
 * Each ring tracks a different aspect of daily wellness progress.
 */
enum class RingType(
    val displayName: String,
    val unit: String,
    val description: String
) {
    STEPS(
        displayName = "Steps",
        unit = "steps",
        description = "Daily step count toward your movement goal"
    ),
    
    CALORIES(
        displayName = "Calories",
        unit = "cal",
        description = "Calories burned toward your daily energy goal"
    ),
    
    HEART_POINTS(
        displayName = "Heart Points",
        unit = "points",
        description = "Heart points earned from moderate to vigorous activity"
    );
    
    /**
     * Returns the default color for this ring type based on UI/UX specifications.
     */
    fun getDefaultColor(): Int {
        return when (this) {
            STEPS -> 0xFF6B8E6B.toInt()      // Sage green
            CALORIES -> 0xFF7A8471.toInt()   // Warm gray-green
            HEART_POINTS -> 0xFFB5846B.toInt() // Soft coral
        }
    }
    
    /**
     * Returns the dark mode color for this ring type.
     */
    fun getDarkModeColor(): Int {
        return when (this) {
            STEPS -> 0xFF81C784.toInt()      // Lighter sage green
            CALORIES -> 0xFFA5D6A7.toInt()   // Lighter warm gray-green
            HEART_POINTS -> 0xFFFF8A80.toInt() // Lighter soft coral
        }
    }
}