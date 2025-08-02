package com.vibehealth.android.data.goals.local

/**
 * VIBE_FIX: Phase 2 - Enum for goal calculation sources in database
 */
enum class GoalCalculationSource {
    DEFAULT,
    PERSONALIZED,
    MANUAL,
    WHO_STANDARD,
    FALLBACK_DEFAULT,
    USER_ADJUSTED
}