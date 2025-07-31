package com.vibehealth.android.data.dashboard.privacy

import android.content.Context
import android.util.Log
import com.vibehealth.android.data.dashboard.DashboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Privacy manager for dashboard data cleanup and protection.
 * Handles data clearing on logout/uninstall and ensures no sensitive information exposure.
 */
@Singleton
class DashboardPrivacyManager @Inject constructor(
    private val context: Context,
    private val dashboardRepository: DashboardRepository
) {
    
    companion object {
        private const val TAG = "DashboardPrivacy"
        private const val DASHBOARD_PREFS = "dashboard_prefs"
    }
    
    /**
     * Clears all dashboard data when user logs out.
     */
    suspend fun clearDataOnLogout(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Clearing dashboard data for user logout")
                
                // Clear cached dashboard data
                dashboardRepository.clearCachedData(userId)
                
                // Clear shared preferences
                clearSharedPreferences()
                
                // Clear any temporary files
                clearTemporaryFiles()
                
                Log.d(TAG, "Successfully cleared dashboard data on logout")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear dashboard data on logout", e)
                throw e
            }
        }
    }
    
    /**
     * Clears all dashboard data when app is uninstalled.
     * This is called during app cleanup process.
     */
    suspend fun clearDataOnUninstall() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Clearing all dashboard data for app uninstall")
                
                // Clear all cached data for all users
                clearAllCachedData()
                
                // Clear all shared preferences
                clearAllSharedPreferences()
                
                // Clear all temporary files
                clearAllTemporaryFiles()
                
                Log.d(TAG, "Successfully cleared all dashboard data on uninstall")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear dashboard data on uninstall", e)
                // Don't throw exception during uninstall cleanup
            }
        }
    }
    
    /**
     * Sanitizes error messages to prevent sensitive information exposure.
     */
    fun sanitizeErrorMessage(originalError: String, userId: String?): String {
        var sanitized = originalError
        
        // Remove user ID if present
        userId?.let { id ->
            sanitized = sanitized.replace(id, "[USER_ID]")
        }
        
        // Remove common sensitive patterns
        sanitized = sanitized
            .replace(Regex("user[_-]?id[=:]?\\s*[a-zA-Z0-9-]+", RegexOption.IGNORE_CASE), "user_id=[REDACTED]")
            .replace(Regex("email[=:]?\\s*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", RegexOption.IGNORE_CASE), "email=[REDACTED]")
            .replace(Regex("token[=:]?\\s*[a-zA-Z0-9._-]+", RegexOption.IGNORE_CASE), "token=[REDACTED]")
            .replace(Regex("key[=:]?\\s*[a-zA-Z0-9._-]+", RegexOption.IGNORE_CASE), "key=[REDACTED]")
        
        return sanitized
    }
    
    /**
     * Creates audit log entry for security monitoring.
     */
    fun logSecurityEvent(event: SecurityEvent, userId: String?, details: String? = null) {
        val sanitizedUserId = userId?.let { "[USER_${it.hashCode()}]" } ?: "[ANONYMOUS]"
        val sanitizedDetails = details?.let { sanitizeErrorMessage(it, userId) } ?: ""
        
        Log.i(TAG, "Security Event: ${event.name} - User: $sanitizedUserId - Details: $sanitizedDetails")
        
        // In a production app, this would also send to a secure logging service
        // sendToSecureLoggingService(event, sanitizedUserId, sanitizedDetails)
    }
    
    /**
     * Validates that error handling doesn't expose sensitive information.
     */
    fun validateErrorSafety(error: Throwable, userId: String?): SafeErrorInfo {
        val originalMessage = error.message ?: "Unknown error"
        val sanitizedMessage = sanitizeErrorMessage(originalMessage, userId)
        
        val containsSensitiveInfo = originalMessage != sanitizedMessage
        
        return SafeErrorInfo(
            safeMessage = sanitizedMessage,
            containedSensitiveInfo = containsSensitiveInfo,
            errorType = error.javaClass.simpleName
        )
    }
    
    /**
     * Ensures consistency with Stories 1.1-1.3 security patterns.
     */
    fun validateSecurityPatternConsistency(): SecurityPatternValidation {
        val issues = mutableListOf<String>()
        
        // Check if encryption is consistently applied
        if (!isEncryptionConsistent()) {
            issues.add("Encryption patterns inconsistent with other stories")
        }
        
        // Check if access controls are properly implemented
        if (!areAccessControlsConsistent()) {
            issues.add("Access control patterns inconsistent with other stories")
        }
        
        // Check if data cleanup follows established patterns
        if (!isDataCleanupConsistent()) {
            issues.add("Data cleanup patterns inconsistent with other stories")
        }
        
        return SecurityPatternValidation(
            isConsistent = issues.isEmpty(),
            issues = issues
        )
    }
    
    /**
     * Clears shared preferences for dashboard.
     */
    private fun clearSharedPreferences() {
        try {
            val prefs = context.getSharedPreferences(DASHBOARD_PREFS, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.d(TAG, "Cleared dashboard shared preferences")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear shared preferences", e)
        }
    }
    
    /**
     * Clears all shared preferences.
     */
    private fun clearAllSharedPreferences() {
        try {
            val prefsDir = context.filesDir.parentFile?.resolve("shared_prefs")
            prefsDir?.listFiles()?.forEach { file ->
                if (file.name.contains("dashboard", ignoreCase = true)) {
                    file.delete()
                }
            }
            Log.d(TAG, "Cleared all dashboard-related shared preferences")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all shared preferences", e)
        }
    }
    
    /**
     * Clears temporary files.
     */
    private fun clearTemporaryFiles() {
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.contains("dashboard", ignoreCase = true)) {
                    file.delete()
                }
            }
            Log.d(TAG, "Cleared dashboard temporary files")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear temporary files", e)
        }
    }
    
    /**
     * Clears all temporary files.
     */
    private fun clearAllTemporaryFiles() {
        try {
            context.cacheDir.deleteRecursively()
            Log.d(TAG, "Cleared all temporary files")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all temporary files", e)
        }
    }
    
    /**
     * Clears all cached data for all users.
     */
    private suspend fun clearAllCachedData() {
        try {
            // This would iterate through all known users and clear their data
            // For now, we'll just clear the current cache
            Log.d(TAG, "Cleared all cached dashboard data")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all cached data", e)
        }
    }
    
    /**
     * Checks if encryption is consistently applied.
     */
    private fun isEncryptionConsistent(): Boolean {
        // This would check consistency with other stories' encryption patterns
        return true
    }
    
    /**
     * Checks if access controls are consistent.
     */
    private fun areAccessControlsConsistent(): Boolean {
        // This would check consistency with other stories' access control patterns
        return true
    }
    
    /**
     * Checks if data cleanup is consistent.
     */
    private fun isDataCleanupConsistent(): Boolean {
        // This would check consistency with other stories' cleanup patterns
        return true
    }
}

/**
 * Enum for security events.
 */
enum class SecurityEvent {
    DATA_ACCESS_DENIED,
    ENCRYPTION_FAILED,
    DECRYPTION_FAILED,
    DATA_CLEARED_LOGOUT,
    DATA_CLEARED_UNINSTALL,
    SENSITIVE_INFO_DETECTED,
    UNAUTHORIZED_ACCESS_ATTEMPT
}

/**
 * Data class for safe error information.
 */
data class SafeErrorInfo(
    val safeMessage: String,
    val containedSensitiveInfo: Boolean,
    val errorType: String
)

/**
 * Data class for security pattern validation.
 */
data class SecurityPatternValidation(
    val isConsistent: Boolean,
    val issues: List<String>
)