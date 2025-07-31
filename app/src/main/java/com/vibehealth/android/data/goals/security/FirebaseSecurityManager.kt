package com.vibehealth.android.data.goals.security

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase security manager with enhanced access controls and rate limiting.
 * 
 * Implements granular access controls, rate limiting, and user authentication
 * verification for all goal operations as specified in Task 6.2.
 */
@Singleton
class FirebaseSecurityManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    companion object {
        private const val MAX_REQUESTS_PER_MINUTE = 60
        private const val MAX_REQUESTS_PER_HOUR = 1000
        private const val MAX_BATCH_SIZE = 10
        private const val MAX_DOCUMENT_SIZE_KB = 1024 // 1MB
    }

    private val rateLimiter = RateLimiter()

    init {
        configureFirestoreSettings()
    }

    /**
     * Verifies user authentication for goal operations.
     */
    suspend fun verifyUserAuthentication(userId: String): AuthenticationResult {
        return try {
            val currentUser = auth.currentUser
            
            when {
                currentUser == null -> AuthenticationResult.NotAuthenticated
                currentUser.uid != userId -> AuthenticationResult.Unauthorized(
                    "User ${currentUser.uid} cannot access data for user $userId"
                )
                !currentUser.isEmailVerified -> AuthenticationResult.EmailNotVerified
                else -> {
                    // Verify token is still valid
                    val tokenResult = currentUser.getIdToken(false).await()
                    // Check if token is expired by comparing expiration time
                    val currentTime = System.currentTimeMillis() / 1000
                    if (tokenResult.expirationTimestamp < currentTime) {
                        AuthenticationResult.TokenExpired
                    } else {
                        AuthenticationResult.Authenticated(currentUser.uid)
                    }
                }
            }
        } catch (e: Exception) {
            AuthenticationResult.Error("Authentication verification failed: ${e.message}")
        }
    }

    /**
     * Checks rate limits before allowing operations.
     */
    suspend fun checkRateLimit(userId: String, operationType: OperationType): RateLimitResult {
        return rateLimiter.checkRateLimit(userId, operationType)
    }

    /**
     * Validates goal data before storage.
     */
    fun validateGoalData(goalData: Map<String, Any>): ValidationResult {
        val issues = mutableListOf<String>()

        // Check required fields
        val requiredFields = listOf("userId", "stepsGoal", "caloriesGoal", "heartPointsGoal", "calculatedAt")
        requiredFields.forEach { field ->
            if (!goalData.containsKey(field)) {
                issues.add("Missing required field: $field")
            }
        }

        // Validate data types and ranges
        goalData["stepsGoal"]?.let { steps ->
            if (steps !is Number || steps.toInt() !in 1000..50000) {
                issues.add("Invalid steps goal: must be between 1000 and 50000")
            }
        }

        goalData["caloriesGoal"]?.let { calories ->
            if (calories !is Number || calories.toInt() !in 800..5000) {
                issues.add("Invalid calories goal: must be between 800 and 5000")
            }
        }

        goalData["heartPointsGoal"]?.let { heartPoints ->
            if (heartPoints !is Number || heartPoints.toInt() !in 10..100) {
                issues.add("Invalid heart points goal: must be between 10 and 100")
            }
        }

        // Check document size
        val documentSize = estimateDocumentSize(goalData)
        if (documentSize > MAX_DOCUMENT_SIZE_KB * 1024) {
            issues.add("Document size (${documentSize / 1024}KB) exceeds maximum (${MAX_DOCUMENT_SIZE_KB}KB)")
        }

        // Validate user ID format
        goalData["userId"]?.let { userId ->
            if (userId !is String || !isValidUserId(userId as String)) {
                issues.add("Invalid user ID format")
            }
        }

        return if (issues.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(issues)
        }
    }

    /**
     * Sanitizes goal data to prevent injection attacks.
     */
    fun sanitizeGoalData(goalData: Map<String, Any>): Map<String, Any> {
        return goalData.mapValues { (key, value) ->
            when (value) {
                is String -> sanitizeString(value)
                is Number -> value
                is Boolean -> value
                is Map<*, *> -> sanitizeMap(value as Map<String, Any>)
                is List<*> -> sanitizeList(value)
                else -> value
            }
        }.filterKeys { key ->
            // Only allow known safe fields
            key in listOf(
                "userId", "stepsGoal", "caloriesGoal", "heartPointsGoal",
                "calculatedAt", "calculationSource", "timestamp"
            )
        }
    }

    /**
     * Creates secure document reference with access control.
     */
    fun createSecureDocumentReference(userId: String, documentId: String): SecureDocumentReference {
        return SecureDocumentReference(
            firestore = firestore,
            userId = userId,
            documentId = documentId,
            securityManager = this
        )
    }

    /**
     * Logs security events for monitoring.
     */
    fun logSecurityEvent(event: SecurityEvent) {
        // In a real implementation, this would log to a secure audit system
        when (event.severity) {
            SecurityEventSeverity.INFO -> {
                // Log informational events
            }
            SecurityEventSeverity.WARNING -> {
                // Log warning events and potentially alert
            }
            SecurityEventSeverity.CRITICAL -> {
                // Log critical events and immediately alert
                handleCriticalSecurityEvent(event)
            }
        }
    }

    /**
     * Gets security metrics for monitoring.
     */
    fun getSecurityMetrics(): SecurityMetrics {
        return SecurityMetrics(
            rateLimitViolations = rateLimiter.getViolationCount(),
            authenticationFailures = rateLimiter.getAuthFailureCount(),
            suspiciousActivities = rateLimiter.getSuspiciousActivityCount(),
            lastSecurityScan = System.currentTimeMillis()
        )
    }

    private fun configureFirestoreSettings() {
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        
        firestore.firestoreSettings = settings
    }

    private fun isValidUserId(userId: String): Boolean {
        // Validate user ID format (Firebase Auth UID format)
        return userId.matches(Regex("^[a-zA-Z0-9]{28}$"))
    }

    private fun sanitizeString(input: String): String {
        return input
            .replace(Regex("[<>\"'&]"), "") // Remove potential HTML/script characters
            .take(1000) // Limit length
            .trim()
    }

    private fun sanitizeMap(map: Map<String, Any>): Map<String, Any> {
        return map.mapValues { (_, value) ->
            when (value) {
                is String -> sanitizeString(value)
                is Map<*, *> -> sanitizeMap(value as Map<String, Any>)
                else -> value
            }
        }
    }

    private fun sanitizeList(list: List<*>): List<*> {
        return list.map { item ->
            when (item) {
                is String -> sanitizeString(item)
                is Map<*, *> -> sanitizeMap(item as Map<String, Any>)
                else -> item
            }
        }
    }

    private fun estimateDocumentSize(data: Map<String, Any>): Int {
        return data.toString().toByteArray().size
    }

    private fun handleCriticalSecurityEvent(event: SecurityEvent) {
        // In a real implementation, this would:
        // 1. Immediately alert security team
        // 2. Potentially block the user temporarily
        // 3. Log to secure audit system
        // 4. Trigger additional security measures
    }

    /**
     * Authentication result types.
     */
    sealed class AuthenticationResult {
        data class Authenticated(val userId: String) : AuthenticationResult()
        object NotAuthenticated : AuthenticationResult()
        data class Unauthorized(val reason: String) : AuthenticationResult()
        object EmailNotVerified : AuthenticationResult()
        object TokenExpired : AuthenticationResult()
        data class Error(val message: String) : AuthenticationResult()
    }

    /**
     * Rate limit result types.
     */
    sealed class RateLimitResult {
        object Allowed : RateLimitResult()
        data class Limited(val retryAfterMs: Long) : RateLimitResult()
        data class Blocked(val reason: String) : RateLimitResult()
    }

    /**
     * Validation result types.
     */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val issues: List<String>) : ValidationResult()
    }

    /**
     * Operation types for rate limiting.
     */
    enum class OperationType {
        READ, WRITE, DELETE, BATCH_WRITE
    }

    /**
     * Security event for audit logging.
     */
    data class SecurityEvent(
        val type: String,
        val userId: String?,
        val message: String,
        val severity: SecurityEventSeverity,
        val timestamp: Long = System.currentTimeMillis(),
        val metadata: Map<String, Any> = emptyMap()
    )

    /**
     * Security event severity levels.
     */
    enum class SecurityEventSeverity {
        INFO, WARNING, CRITICAL
    }

    /**
     * Security metrics for monitoring.
     */
    data class SecurityMetrics(
        val rateLimitViolations: Long,
        val authenticationFailures: Long,
        val suspiciousActivities: Long,
        val lastSecurityScan: Long
    )
}

/**
 * Rate limiter implementation.
 */
private class RateLimiter {
    private val userRequestCounts = mutableMapOf<String, UserRequestTracker>()
    private var violationCount = 0L
    private var authFailureCount = 0L
    private var suspiciousActivityCount = 0L

    fun checkRateLimit(userId: String, operationType: FirebaseSecurityManager.OperationType): FirebaseSecurityManager.RateLimitResult {
        val tracker = userRequestCounts.getOrPut(userId) { UserRequestTracker() }
        
        return when {
            tracker.isBlocked() -> {
                FirebaseSecurityManager.RateLimitResult.Blocked("User temporarily blocked due to suspicious activity")
            }
            tracker.exceedsMinuteLimit() -> {
                violationCount++
                FirebaseSecurityManager.RateLimitResult.Limited(60000) // 1 minute
            }
            tracker.exceedsHourLimit() -> {
                violationCount++
                FirebaseSecurityManager.RateLimitResult.Limited(3600000) // 1 hour
            }
            else -> {
                tracker.recordRequest(operationType)
                FirebaseSecurityManager.RateLimitResult.Allowed
            }
        }
    }

    fun getViolationCount() = violationCount
    fun getAuthFailureCount() = authFailureCount
    fun getSuspiciousActivityCount() = suspiciousActivityCount

    private data class UserRequestTracker(
        private val minuteRequests: MutableList<Long> = mutableListOf(),
        private val hourRequests: MutableList<Long> = mutableListOf(),
        private var blockedUntil: Long = 0
    ) {
        fun recordRequest(operationType: FirebaseSecurityManager.OperationType) {
            val now = System.currentTimeMillis()
            minuteRequests.add(now)
            hourRequests.add(now)
            
            // Clean old requests
            cleanOldRequests()
        }

        fun exceedsMinuteLimit(): Boolean {
            cleanOldRequests()
            return minuteRequests.size > 60
        }

        fun exceedsHourLimit(): Boolean {
            cleanOldRequests()
            return hourRequests.size > 1000
        }

        fun isBlocked(): Boolean {
            return System.currentTimeMillis() < blockedUntil
        }

        private fun cleanOldRequests() {
            val now = System.currentTimeMillis()
            minuteRequests.removeAll { it < now - 60000 } // 1 minute
            hourRequests.removeAll { it < now - 3600000 } // 1 hour
        }
    }
}

/**
 * Secure document reference wrapper.
 */
class SecureDocumentReference(
    private val firestore: FirebaseFirestore,
    private val userId: String,
    private val documentId: String,
    private val securityManager: FirebaseSecurityManager
) {
    
    suspend fun secureSet(data: Map<String, Any>): Result<Unit> {
        return try {
            // Verify authentication
            val authResult = securityManager.verifyUserAuthentication(userId)
            if (authResult !is FirebaseSecurityManager.AuthenticationResult.Authenticated) {
                return Result.failure(SecurityException("Authentication failed: $authResult"))
            }

            // Check rate limits
            val rateLimitResult = securityManager.checkRateLimit(userId, FirebaseSecurityManager.OperationType.WRITE)
            if (rateLimitResult !is FirebaseSecurityManager.RateLimitResult.Allowed) {
                return Result.failure(SecurityException("Rate limit exceeded: $rateLimitResult"))
            }

            // Validate and sanitize data
            val validationResult = securityManager.validateGoalData(data)
            if (validationResult is FirebaseSecurityManager.ValidationResult.Invalid) {
                return Result.failure(IllegalArgumentException("Invalid data: ${validationResult.issues}"))
            }

            val sanitizedData = securityManager.sanitizeGoalData(data)

            // Perform secure write
            val docRef = firestore.collection("users").document(userId).collection("goals").document(documentId)
            docRef.set(sanitizedData).await()

            // Log security event
            securityManager.logSecurityEvent(
                FirebaseSecurityManager.SecurityEvent(
                    type = "GOAL_DATA_WRITE",
                    userId = userId,
                    message = "Goal data written successfully",
                    severity = FirebaseSecurityManager.SecurityEventSeverity.INFO
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            securityManager.logSecurityEvent(
                FirebaseSecurityManager.SecurityEvent(
                    type = "GOAL_DATA_WRITE_FAILED",
                    userId = userId,
                    message = "Goal data write failed: ${e.message}",
                    severity = FirebaseSecurityManager.SecurityEventSeverity.WARNING
                )
            )
            Result.failure(e)
        }
    }
}