package com.vibehealth.android.core.error

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vibehealth.android.domain.auth.AuthError
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handling system with Firebase Crashlytics integration
 * Ensures no PII is logged while providing comprehensive error tracking
 */
@Singleton
class ErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashlytics: FirebaseCrashlytics
) {
    
    /**
     * Handle authentication errors with proper logging and user feedback
     */
    fun handleAuthError(error: Throwable, context: String = ""): String {
        // Log to Crashlytics without PII
        logErrorToCrashlytics(error, "AUTH_ERROR", context)
        
        // Return user-friendly message
        return when {
            error.message?.contains("network", ignoreCase = true) == true -> 
                "Please check your internet connection and try again"
            error.message?.contains("invalid-email", ignoreCase = true) == true -> 
                "Please enter a valid email address"
            error.message?.contains("user-not-found", ignoreCase = true) == true -> 
                "No account found with this email address"
            error.message?.contains("wrong-password", ignoreCase = true) == true -> 
                "The password you entered is incorrect"
            error.message?.contains("email-already-in-use", ignoreCase = true) == true -> 
                "An account with this email already exists"
            error.message?.contains("weak-password", ignoreCase = true) == true -> 
                "Password must be at least 8 characters long"
            error.message?.contains("too-many-requests", ignoreCase = true) == true -> 
                "Too many attempts. Please try again later"
            error.message?.contains("user-disabled", ignoreCase = true) == true -> 
                "This account has been disabled. Please contact support"
            else -> "Something went wrong. Please try again"
        }
    }
    
    /**
     * Handle network errors with retry mechanisms
     */
    fun handleNetworkError(error: Throwable, retryAction: (() -> Unit)? = null): NetworkErrorInfo {
        logErrorToCrashlytics(error, "NETWORK_ERROR")
        
        return NetworkErrorInfo(
            message = "Please check your internet connection and try again",
            isRetryable = true,
            retryAction = retryAction
        )
    }
    
    /**
     * Handle validation errors with supportive messaging
     */
    fun handleValidationError(error: Throwable, fieldName: String): String {
        logErrorToCrashlytics(error, "VALIDATION_ERROR", "field:$fieldName")
        
        return when (fieldName.lowercase()) {
            "email" -> "Please enter a valid email address"
            "password" -> "Password must be at least 8 characters long"
            "confirm_password" -> "Passwords do not match"
            "terms" -> "Please accept the Terms & Conditions to continue"
            else -> "Please check your input and try again"
        }
    }
    
    /**
     * Handle Firebase-specific errors
     */
    fun handleFirebaseError(error: Throwable): String {
        logErrorToCrashlytics(error, "FIREBASE_ERROR")
        
        return when {
            error.message?.contains("FirebaseNetworkException") == true -> 
                "Please check your internet connection"
            error.message?.contains("FirebaseAuthException") == true -> 
                handleAuthError(error)
            error.message?.contains("FirebaseFirestoreException") == true -> 
                "Unable to save data. Please try again"
            else -> "A service error occurred. Please try again"
        }
    }
    
    /**
     * Handle unexpected errors gracefully
     */
    fun handleUnexpectedError(error: Throwable, context: String = ""): String {
        logErrorToCrashlytics(error, "UNEXPECTED_ERROR", context)
        
        return "Something unexpected happened. Please try again"
    }
    
    /**
     * Log error to Firebase Crashlytics without PII
     */
    private fun logErrorToCrashlytics(
        error: Throwable,
        errorType: String,
        context: String = ""
    ) {
        try {
            // Set custom keys for better error tracking
            crashlytics.setCustomKey("error_type", errorType)
            crashlytics.setCustomKey("error_context", context)
            crashlytics.setCustomKey("app_version", getAppVersion())
            
            // Remove any potential PII from error message
            val sanitizedMessage = sanitizeErrorMessage(error.message ?: "Unknown error")
            
            // Create sanitized exception
            val sanitizedException = Exception(sanitizedMessage, error.cause)
            
            // Log to Crashlytics
            crashlytics.recordException(sanitizedException)
            
        } catch (e: Exception) {
            // Fail silently if Crashlytics logging fails
            // Don't let error logging break the app
        }
    }
    
    /**
     * Remove PII from error messages
     */
    private fun sanitizeErrorMessage(message: String): String {
        return message
            .replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "[EMAIL_REDACTED]")
            .replace(Regex("\\b\\d{10,}\\b"), "[PHONE_REDACTED]")
            .replace(Regex("\\b[A-Z]{2}\\d{2}[A-Z0-9]{4}\\d{7}([A-Z0-9]?){0,16}\\b"), "[IBAN_REDACTED]")
            .replace(Regex("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"), "[CARD_REDACTED]")
    }
    
    /**
     * Get app version for error context
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Create error report for debugging
     */
    fun createErrorReport(error: Throwable, userAction: String): ErrorReport {
        return ErrorReport(
            errorType = error.javaClass.simpleName,
            errorMessage = sanitizeErrorMessage(error.message ?: "Unknown error"),
            userAction = userAction,
            timestamp = System.currentTimeMillis(),
            appVersion = getAppVersion()
        )
    }
}

/**
 * Network error information with retry capability
 */
data class NetworkErrorInfo(
    val message: String,
    val isRetryable: Boolean,
    val retryAction: (() -> Unit)? = null
)

/**
 * Error report for debugging and analytics
 */
data class ErrorReport(
    val errorType: String,
    val errorMessage: String,
    val userAction: String,
    val timestamp: Long,
    val appVersion: String
)