package com.vibehealth.android.domain.onboarding

/**
 * Sealed class representing the result of onboarding operations
 */
sealed class OnboardingResult {
    object Success : OnboardingResult()
    object Loading : OnboardingResult()
    data class Error(
        val exception: Exception,
        val userMessage: String = "Something went wrong. Please try again.",
        val canRetry: Boolean = true
    ) : OnboardingResult()
    
    /**
     * Check if the result is successful
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * Check if the result is an error
     */
    fun isError(): Boolean = this is Error
    
    /**
     * Check if the result is loading
     */
    fun isLoading(): Boolean = this is Loading
    
    /**
     * Get error message if this is an error result
     */
    fun getErrorMessage(): String? = (this as? Error)?.userMessage
    
    /**
     * Get exception if this is an error result
     */
    fun getErrorException(): Exception? = (this as? Error)?.exception
    
    /**
     * Check if retry is possible for error results
     */
    fun canRetryOperation(): Boolean = (this as? Error)?.canRetry ?: false
}