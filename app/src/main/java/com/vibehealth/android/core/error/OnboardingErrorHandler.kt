package com.vibehealth.android.core.error

import android.content.Context
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.vibehealth.android.R
import com.vibehealth.android.core.network.NetworkMonitor
import com.vibehealth.android.core.security.DataSanitizationHelper
import com.vibehealth.android.domain.onboarding.OnboardingResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Comprehensive error handler for onboarding with user-friendly messaging
 */
@Singleton
class OnboardingErrorHandler @Inject constructor(
    private val context: Context,
    private val networkMonitor: NetworkMonitor
) {

    /**
     * Handle errors and convert to user-friendly OnboardingResult
     */
    suspend fun handleError(
        exception: Exception,
        operation: String = "operation"
    ): OnboardingResult {
        
        // Log error without PII
        logError(exception, operation)
        
        return when {
            isNetworkError(exception) -> handleNetworkError(exception)
            isStorageError(exception) -> handleStorageError(exception)
            isValidationError(exception) -> handleValidationError(exception)
            isFirebaseError(exception) -> handleFirebaseError(exception)
            else -> handleUnknownError(exception)
        }
    }

    /**
     * Handle network-related errors
     */
    private suspend fun handleNetworkError(exception: Exception): OnboardingResult {
        val isOnline = networkMonitor.isOnline.first()
        
        return if (!isOnline) {
            OnboardingResult.Error(
                exception = exception,
                userMessage = context.getString(R.string.error_network_offline),
                canRetry = true
            )
        } else {
            OnboardingResult.Error(
                exception = exception,
                userMessage = context.getString(R.string.error_network_timeout),
                canRetry = true
            )
        }
    }

    /**
     * Handle storage-related errors
     */
    private fun handleStorageError(exception: Exception): OnboardingResult {
        return OnboardingResult.Error(
            exception = exception,
            userMessage = context.getString(R.string.error_storage_failed),
            canRetry = true
        )
    }

    /**
     * Handle validation errors
     */
    private fun handleValidationError(exception: Exception): OnboardingResult {
        return OnboardingResult.Error(
            exception = exception,
            userMessage = context.getString(R.string.error_validation_failed),
            canRetry = false
        )
    }

    /**
     * Handle Firebase-specific errors
     */
    private fun handleFirebaseError(exception: Exception): OnboardingResult {
        val firebaseException = exception as? FirebaseFirestoreException
        
        return when (firebaseException?.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE -> {
                OnboardingResult.Error(
                    exception = exception,
                    userMessage = context.getString(R.string.error_service_unavailable),
                    canRetry = true
                )
            }
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> {
                OnboardingResult.Error(
                    exception = exception,
                    userMessage = context.getString(R.string.error_request_timeout),
                    canRetry = true
                )
            }
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                OnboardingResult.Error(
                    exception = exception,
                    userMessage = context.getString(R.string.error_permission_denied),
                    canRetry = false
                )
            }
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> {
                OnboardingResult.Error(
                    exception = exception,
                    userMessage = context.getString(R.string.error_quota_exceeded),
                    canRetry = true
                )
            }
            else -> {
                OnboardingResult.Error(
                    exception = exception,
                    userMessage = context.getString(R.string.error_firebase_general),
                    canRetry = true
                )
            }
        }
    }

    /**
     * Handle unknown errors
     */
    private fun handleUnknownError(exception: Exception): OnboardingResult {
        return OnboardingResult.Error(
            exception = exception,
            userMessage = context.getString(R.string.error_unknown_onboarding),
            canRetry = true
        )
    }

    /**
     * Check if error is network-related
     */
    private fun isNetworkError(exception: Exception): Boolean {
        val message = exception.message?.lowercase() ?: ""
        return message.contains("network") ||
               message.contains("connection") ||
               message.contains("timeout") ||
               message.contains("unreachable") ||
               exception is java.net.UnknownHostException ||
               exception is java.net.SocketTimeoutException ||
               exception is java.net.ConnectException
    }

    /**
     * Check if error is storage-related
     */
    private fun isStorageError(exception: Exception): Boolean {
        val message = exception.message?.lowercase() ?: ""
        return message.contains("storage") ||
               message.contains("database") ||
               message.contains("disk") ||
               message.contains("space") ||
               exception is java.io.IOException
    }

    /**
     * Check if error is validation-related
     */
    private fun isValidationError(exception: Exception): Boolean {
        val message = exception.message?.lowercase() ?: ""
        return message.contains("validation") ||
               message.contains("invalid") ||
               message.contains("format") ||
               exception is IllegalArgumentException
    }

    /**
     * Check if error is Firebase-related
     */
    private fun isFirebaseError(exception: Exception): Boolean {
        return exception is FirebaseException ||
               exception is FirebaseFirestoreException
    }

    /**
     * Log error without exposing PII
     */
    private fun logError(exception: Exception, operation: String) {
        val sanitizedMessage = DataSanitizationHelper.sanitizeExceptionMessage(exception)
        android.util.Log.e("OnboardingErrorHandler", "Error in $operation: $sanitizedMessage", exception)
        
        // Create audit log entry
        val logEntry = DataSanitizationHelper.createAuditLogEntry(
            action = "error_$operation",
            userId = "[USER_ID_REDACTED]",
            success = false,
            additionalInfo = mapOf(
                "error_type" to exception.javaClass.simpleName,
                "error_message" to sanitizedMessage
            )
        )
        
        android.util.Log.i("OnboardingErrorHandler", "Audit: $logEntry")
    }

    /**
     * Get retry strategy based on error type
     */
    fun getRetryStrategy(exception: Exception): RetryStrategy {
        return when {
            isNetworkError(exception) -> RetryStrategy.EXPONENTIAL_BACKOFF
            isStorageError(exception) -> RetryStrategy.LINEAR_BACKOFF
            isFirebaseError(exception) -> {
                val firebaseException = exception as? FirebaseFirestoreException
                when (firebaseException?.code) {
                    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> RetryStrategy.LONG_DELAY
                    FirebaseFirestoreException.Code.UNAVAILABLE -> RetryStrategy.EXPONENTIAL_BACKOFF
                    else -> RetryStrategy.LINEAR_BACKOFF
                }
            }
            else -> RetryStrategy.LINEAR_BACKOFF
        }
    }

    /**
     * Calculate retry delay based on strategy
     */
    fun calculateRetryDelay(attempt: Int, strategy: RetryStrategy): Long {
        return when (strategy) {
            RetryStrategy.LINEAR_BACKOFF -> 1000L * attempt
            RetryStrategy.EXPONENTIAL_BACKOFF -> (1000L * 2.0.pow(attempt.toDouble())).toLong()
            RetryStrategy.LONG_DELAY -> 30000L // 30 seconds
        }.coerceAtMost(60000L) // Max 1 minute
    }
}

enum class RetryStrategy {
    LINEAR_BACKOFF,
    EXPONENTIAL_BACKOFF,
    LONG_DELAY
}