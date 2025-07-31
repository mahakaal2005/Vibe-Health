package com.vibehealth.android.ui.dashboard.models

/**
 * Represents different error states that can occur in the dashboard.
 * Provides user-friendly error messages and retry options.
 */
sealed class ErrorState(
    val message: String,
    val isRetryable: Boolean = true
) {
    /**
     * No goals are available (user hasn't completed profile setup).
     */
    data class NoGoals(val userMessage: String) : ErrorState(
        message = userMessage,
        isRetryable = false
    )
    
    /**
     * Network error occurred while fetching data.
     */
    data class Network(val userMessage: String) : ErrorState(
        message = userMessage,
        isRetryable = true
    )
    
    /**
     * Data corruption or parsing error.
     */
    data class DataCorrupted(val userMessage: String) : ErrorState(
        message = userMessage,
        isRetryable = true
    )
    
    /**
     * Unknown error occurred.
     */
    data class Unknown(val userMessage: String) : ErrorState(
        message = userMessage,
        isRetryable = true
    )
}