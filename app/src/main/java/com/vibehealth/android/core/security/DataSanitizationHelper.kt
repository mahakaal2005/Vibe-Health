package com.vibehealth.android.core.security

import com.vibehealth.android.domain.user.UserProfile
import java.util.regex.Pattern

/**
 * Helper class for data sanitization and PII handling guidelines
 */
object DataSanitizationHelper {

    // Patterns for PII detection
    private val EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val PHONE_PATTERN = Pattern.compile("\\b\\d{10,15}\\b")
    private val NAME_SANITIZATION_PATTERN = Pattern.compile("[^\\p{L}\\p{N}\\s'-]")

    /**
     * Sanitize user input to prevent injection attacks
     */
    fun sanitizeUserInput(input: String): String {
        return input
            .trim()
            .replace(Regex("[<>\"'&]"), "") // Remove potential HTML/script injection characters
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .take(255) // Limit length to prevent buffer overflow
    }

    /**
     * Sanitize name input specifically
     */
    fun sanitizeName(name: String): String {
        return NAME_SANITIZATION_PATTERN.matcher(name.trim())
            .replaceAll("")
            .replace(Regex("\\s+"), " ")
            .take(50)
            .trim()
    }

    /**
     * Remove PII from log messages
     */
    fun sanitizeForLogging(message: String): String {
        var sanitized = message
        
        // Replace email addresses
        sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("[EMAIL_REDACTED]")
        
        // Replace phone numbers
        sanitized = PHONE_PATTERN.matcher(sanitized).replaceAll("[PHONE_REDACTED]")
        
        // Replace common PII patterns
        sanitized = sanitized
            .replace(Regex("\\b[A-Z][a-z]+ [A-Z][a-z]+\\b"), "[NAME_REDACTED]") // Full names
            .replace(Regex("\\b\\d{4}-\\d{2}-\\d{2}\\b"), "[DATE_REDACTED]") // Dates
            .replace(Regex("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"), "[IP_REDACTED]") // IP addresses
        
        return sanitized
    }

    /**
     * Create a sanitized version of UserProfile for logging
     */
    fun sanitizeUserProfileForLogging(userProfile: UserProfile): Map<String, Any> {
        return mapOf(
            "userId" to if (userProfile.userId.isNotBlank()) "[USER_ID_REDACTED]" else "",
            "email" to if (userProfile.email.isNotBlank()) "[EMAIL_REDACTED]" else "",
            "displayName" to if (userProfile.displayName.isNotBlank()) "[NAME_REDACTED]" else "",
            "hasCompletedOnboarding" to userProfile.hasCompletedOnboarding,
            "gender" to userProfile.gender.name,
            "unitSystem" to userProfile.unitSystem.name,
            "heightInCm" to userProfile.heightInCm,
            "weightInKg" to userProfile.weightInKg,
            "age" to userProfile.getAge(),
            "createdAt" to userProfile.createdAt,
            "updatedAt" to userProfile.updatedAt
        )
    }

    /**
     * Validate that input doesn't contain potential security threats
     */
    fun isInputSecure(input: String): Boolean {
        val dangerousPatterns = listOf(
            "<script", "</script>", "javascript:", "vbscript:",
            "onload=", "onerror=", "onclick=", "onmouseover=",
            "eval(", "setTimeout(", "setInterval(",
            "document.cookie", "document.write",
            "SELECT ", "INSERT ", "UPDATE ", "DELETE ", "DROP ",
            "UNION ", "OR 1=1", "' OR '1'='1"
        )
        
        val lowerInput = input.lowercase()
        return dangerousPatterns.none { pattern -> 
            lowerInput.contains(pattern.lowercase()) 
        }
    }

    /**
     * Sanitize input to prevent SQL injection and XSS
     */
    fun sanitizeForDatabase(input: String): String {
        return input
            .replace("'", "''") // Escape single quotes for SQL
            .replace("\"", "&quot;") // Escape double quotes
            .replace("<", "&lt;") // Escape less than
            .replace(">", "&gt;") // Escape greater than
            .replace("&", "&amp;") // Escape ampersand
            .trim()
    }

    /**
     * Generate a hash for sensitive data that needs to be tracked but not stored
     */
    fun generateDataHash(data: String): String {
        return data.hashCode().toString()
    }

    /**
     * Check if a string contains PII that should be redacted
     */
    fun containsPII(text: String): Boolean {
        return EMAIL_PATTERN.matcher(text).find() ||
               PHONE_PATTERN.matcher(text).find() ||
               text.matches(Regex(".*\\b[A-Z][a-z]+ [A-Z][a-z]+\\b.*")) // Potential full names
    }

    /**
     * Create a safe version of exception message for logging
     */
    fun sanitizeExceptionMessage(exception: Exception): String {
        val message = exception.message ?: "Unknown error"
        return sanitizeForLogging(message)
    }

    /**
     * Validate email format without storing the actual email
     */
    fun isValidEmailFormat(email: String): Boolean {
        return EMAIL_PATTERN.matcher(email).matches() && email.length <= 254
    }

    /**
     * Create audit log entry without PII
     */
    fun createAuditLogEntry(
        action: String,
        userId: String,
        success: Boolean,
        additionalInfo: Map<String, Any> = emptyMap()
    ): Map<String, Any> {
        return mapOf(
            "action" to action,
            "userIdHash" to generateDataHash(userId),
            "success" to success,
            "timestamp" to System.currentTimeMillis(),
            "additionalInfo" to additionalInfo.mapValues { (_, value) ->
                when (value) {
                    is String -> if (containsPII(value)) "[PII_REDACTED]" else value
                    else -> value
                }
            }
        )
    }
}