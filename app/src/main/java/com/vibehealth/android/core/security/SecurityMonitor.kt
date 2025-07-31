package com.vibehealth.android.core.security

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Security monitoring and alerting system.
 * 
 * Monitors for unusual patterns, security threats, and provides
 * automated security scanning as specified in Task 6.2.
 */
@Singleton
class SecurityMonitor @Inject constructor() {

    private val _securityStatus = MutableStateFlow(SecurityStatus())
    val securityStatus: StateFlow<SecurityStatus> = _securityStatus.asStateFlow()

    private val securityEvents = mutableListOf<SecurityEvent>()
    private val maxEventHistory = 1000
    private val monitoringScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var lastSecurityScan = 0L
    private val securityScanInterval = 5 * 60 * 1000L // 5 minutes

    init {
        startSecurityMonitoring()
    }

    /**
     * Records a security event for analysis.
     */
    fun recordSecurityEvent(event: SecurityEvent) {
        synchronized(securityEvents) {
            securityEvents.add(event)
            if (securityEvents.size > maxEventHistory) {
                securityEvents.removeAt(0)
            }
        }

        // Analyze event for immediate threats
        analyzeSecurityEvent(event)
        
        // Update security status
        updateSecurityStatus()
    }

    /**
     * Performs comprehensive security scan.
     */
    suspend fun performSecurityScan(): SecurityScanResult {
        val scanStartTime = System.currentTimeMillis()
        val findings = mutableListOf<SecurityFinding>()

        try {
            // Scan for unusual activity patterns
            findings.addAll(scanForUnusualPatterns())
            
            // Scan for potential security threats
            findings.addAll(scanForSecurityThreats())
            
            // Scan for configuration issues
            findings.addAll(scanForConfigurationIssues())
            
            // Scan for data integrity issues
            findings.addAll(scanForDataIntegrityIssues())

            val scanDuration = System.currentTimeMillis() - scanStartTime
            lastSecurityScan = System.currentTimeMillis()

            return SecurityScanResult.Success(
                findings = findings,
                scanDuration = scanDuration,
                timestamp = lastSecurityScan
            )
        } catch (e: Exception) {
            return SecurityScanResult.Error("Security scan failed: ${e.message}")
        }
    }

    /**
     * Gets security insights and recommendations.
     */
    fun getSecurityInsights(): List<SecurityInsight> {
        val insights = mutableListOf<SecurityInsight>()
        val recentEvents = getRecentEvents(24 * 60 * 60 * 1000L) // Last 24 hours

        // Analyze authentication failures
        val authFailures = recentEvents.count { it.type == SecurityEventType.AUTHENTICATION_FAILURE }
        if (authFailures > 10) {
            insights.add(
                SecurityInsight(
                    type = InsightType.WARNING,
                    category = "Authentication",
                    message = "$authFailures authentication failures in the last 24 hours",
                    recommendation = "Review authentication logs and consider implementing additional security measures",
                    severity = SecuritySeverity.MEDIUM
                )
            )
        }

        // Analyze rate limit violations
        val rateLimitViolations = recentEvents.count { it.type == SecurityEventType.RATE_LIMIT_VIOLATION }
        if (rateLimitViolations > 5) {
            insights.add(
                SecurityInsight(
                    type = InsightType.ERROR,
                    category = "Rate Limiting",
                    message = "$rateLimitViolations rate limit violations detected",
                    recommendation = "Investigate potential abuse or adjust rate limits",
                    severity = SecuritySeverity.HIGH
                )
            )
        }

        // Analyze encryption issues
        val encryptionFailures = recentEvents.count { it.type == SecurityEventType.ENCRYPTION_FAILURE }
        if (encryptionFailures > 0) {
            insights.add(
                SecurityInsight(
                    type = InsightType.CRITICAL,
                    category = "Encryption",
                    message = "$encryptionFailures encryption failures detected",
                    recommendation = "Immediate investigation required - potential data security risk",
                    severity = SecuritySeverity.CRITICAL
                )
            )
        }

        // Analyze data access patterns
        val suspiciousAccess = recentEvents.count { it.type == SecurityEventType.SUSPICIOUS_ACCESS }
        if (suspiciousAccess > 0) {
            insights.add(
                SecurityInsight(
                    type = InsightType.WARNING,
                    category = "Data Access",
                    message = "$suspiciousAccess suspicious data access attempts",
                    recommendation = "Review access patterns and user permissions",
                    severity = SecuritySeverity.MEDIUM
                )
            )
        }

        return insights
    }

    /**
     * Checks if immediate security action is required.
     */
    fun requiresImmediateAction(): Boolean {
        val criticalEvents = getRecentEvents(60 * 60 * 1000L) // Last hour
            .filter { it.severity == SecuritySeverity.CRITICAL }
        
        return criticalEvents.isNotEmpty()
    }

    /**
     * Gets security metrics for dashboard.
     */
    fun getSecurityMetrics(): SecurityMetrics {
        val recentEvents = getRecentEvents(24 * 60 * 60 * 1000L)
        
        return SecurityMetrics(
            totalEvents = securityEvents.size,
            recentEvents = recentEvents.size,
            criticalEvents = recentEvents.count { it.severity == SecuritySeverity.CRITICAL },
            warningEvents = recentEvents.count { it.severity == SecuritySeverity.MEDIUM },
            lastScanTime = lastSecurityScan,
            overallSecurityScore = calculateSecurityScore()
        )
    }

    private fun startSecurityMonitoring() {
        monitoringScope.launch {
            while (true) {
                try {
                    if (System.currentTimeMillis() - lastSecurityScan > securityScanInterval) {
                        performSecurityScan()
                    }
                    
                    // Check for immediate threats
                    checkForImmediateThreats()
                    
                    delay(60 * 1000L) // Check every minute
                } catch (e: Exception) {
                    // Log error and continue monitoring
                    delay(5 * 60 * 1000L) // Wait 5 minutes before retrying
                }
            }
        }
    }

    private fun analyzeSecurityEvent(event: SecurityEvent) {
        when (event.type) {
            SecurityEventType.AUTHENTICATION_FAILURE -> {
                checkForBruteForceAttack(event.userId)
            }
            SecurityEventType.RATE_LIMIT_VIOLATION -> {
                checkForDDoSAttack(event.userId)
            }
            SecurityEventType.ENCRYPTION_FAILURE -> {
                triggerCriticalAlert("Encryption failure detected", event)
            }
            SecurityEventType.SUSPICIOUS_ACCESS -> {
                checkForUnauthorizedAccess(event.userId)
            }
            else -> {
                // Standard event processing
            }
        }
    }

    private fun checkForBruteForceAttack(userId: String?) {
        if (userId == null) return
        
        val recentFailures = getRecentEvents(5 * 60 * 1000L) // Last 5 minutes
            .filter { it.userId == userId && it.type == SecurityEventType.AUTHENTICATION_FAILURE }
        
        if (recentFailures.size >= 5) {
            triggerSecurityAlert(
                "Potential brute force attack detected for user $userId",
                SecuritySeverity.HIGH
            )
        }
    }

    private fun checkForDDoSAttack(userId: String?) {
        val recentViolations = getRecentEvents(60 * 1000L) // Last minute
            .filter { it.type == SecurityEventType.RATE_LIMIT_VIOLATION }
        
        if (recentViolations.size >= 10) {
            triggerSecurityAlert(
                "Potential DDoS attack detected - ${recentViolations.size} rate limit violations",
                SecuritySeverity.CRITICAL
            )
        }
    }

    private fun checkForUnauthorizedAccess(userId: String?) {
        if (userId == null) return
        
        val recentAccess = getRecentEvents(60 * 60 * 1000L) // Last hour
            .filter { it.userId == userId && it.type == SecurityEventType.SUSPICIOUS_ACCESS }
        
        if (recentAccess.size >= 3) {
            triggerSecurityAlert(
                "Multiple suspicious access attempts by user $userId",
                SecuritySeverity.MEDIUM
            )
        }
    }

    private fun checkForImmediateThreats() {
        val criticalEvents = getRecentEvents(5 * 60 * 1000L) // Last 5 minutes
            .filter { it.severity == SecuritySeverity.CRITICAL }
        
        if (criticalEvents.isNotEmpty()) {
            triggerCriticalAlert("Critical security events detected", criticalEvents.first())
        }
    }

    private fun scanForUnusualPatterns(): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()
        val recentEvents = getRecentEvents(24 * 60 * 60 * 1000L)
        
        // Check for unusual activity spikes
        val hourlyEventCounts = recentEvents.groupBy { it.timestamp / (60 * 60 * 1000L) }
        val averageEventsPerHour = recentEvents.size / 24.0
        
        hourlyEventCounts.forEach { (hour, events) ->
            if (events.size > averageEventsPerHour * 3) {
                findings.add(
                    SecurityFinding(
                        type = FindingType.UNUSUAL_ACTIVITY,
                        severity = SecuritySeverity.MEDIUM,
                        description = "Unusual activity spike detected at hour $hour with ${events.size} events",
                        recommendation = "Investigate the cause of increased activity"
                    )
                )
            }
        }
        
        return findings
    }

    private fun scanForSecurityThreats(): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()
        val recentEvents = getRecentEvents(24 * 60 * 60 * 1000L)
        
        // Check for repeated failures from same user
        val userFailures = recentEvents
            .filter { it.type == SecurityEventType.AUTHENTICATION_FAILURE }
            .groupBy { it.userId }
        
        userFailures.forEach { (userId, failures) ->
            if (failures.size >= 10) {
                findings.add(
                    SecurityFinding(
                        type = FindingType.POTENTIAL_ATTACK,
                        severity = SecuritySeverity.HIGH,
                        description = "User $userId has ${failures.size} authentication failures",
                        recommendation = "Consider temporarily blocking this user"
                    )
                )
            }
        }
        
        return findings
    }

    private fun scanForConfigurationIssues(): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()
        
        // Check if security scanning is up to date
        if (System.currentTimeMillis() - lastSecurityScan > securityScanInterval * 2) {
            findings.add(
                SecurityFinding(
                    type = FindingType.CONFIGURATION_ISSUE,
                    severity = SecuritySeverity.LOW,
                    description = "Security scanning is behind schedule",
                    recommendation = "Ensure security monitoring is running properly"
                )
            )
        }
        
        return findings
    }

    private fun scanForDataIntegrityIssues(): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()
        
        // Check for encryption failures
        val encryptionFailures = getRecentEvents(24 * 60 * 60 * 1000L)
            .filter { it.type == SecurityEventType.ENCRYPTION_FAILURE }
        
        if (encryptionFailures.isNotEmpty()) {
            findings.add(
                SecurityFinding(
                    type = FindingType.DATA_INTEGRITY,
                    severity = SecuritySeverity.CRITICAL,
                    description = "${encryptionFailures.size} encryption failures detected",
                    recommendation = "Immediate investigation required - potential data compromise"
                )
            )
        }
        
        return findings
    }

    private fun triggerSecurityAlert(message: String, severity: SecuritySeverity) {
        val alert = SecurityAlert(
            message = message,
            severity = severity,
            timestamp = System.currentTimeMillis()
        )
        
        // In a real implementation, this would:
        // 1. Send notifications to security team
        // 2. Log to security information and event management (SIEM) system
        // 3. Potentially trigger automated responses
        
        recordSecurityEvent(
            SecurityEvent(
                type = SecurityEventType.SECURITY_ALERT,
                userId = null,
                message = message,
                severity = severity,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private fun triggerCriticalAlert(message: String, event: SecurityEvent) {
        // Critical alerts require immediate attention
        triggerSecurityAlert(message, SecuritySeverity.CRITICAL)
        
        // Additional critical alert handling
        // In a real implementation, this might:
        // 1. Send SMS/email to security team
        // 2. Create incident ticket
        // 3. Trigger emergency response procedures
    }

    private fun getRecentEvents(timeWindowMs: Long): List<SecurityEvent> {
        val cutoffTime = System.currentTimeMillis() - timeWindowMs
        return securityEvents.filter { it.timestamp >= cutoffTime }
    }

    private fun updateSecurityStatus() {
        val recentEvents = getRecentEvents(60 * 60 * 1000L) // Last hour
        val criticalCount = recentEvents.count { it.severity == SecuritySeverity.CRITICAL }
        val warningCount = recentEvents.count { it.severity == SecuritySeverity.MEDIUM }
        
        val status = when {
            criticalCount > 0 -> SecurityStatusLevel.CRITICAL
            warningCount > 5 -> SecurityStatusLevel.WARNING
            warningCount > 0 -> SecurityStatusLevel.CAUTION
            else -> SecurityStatusLevel.NORMAL
        }
        
        _securityStatus.value = SecurityStatus(
            level = status,
            lastUpdate = System.currentTimeMillis(),
            activeThreats = criticalCount,
            recentWarnings = warningCount
        )
    }

    private fun calculateSecurityScore(): Int {
        val recentEvents = getRecentEvents(24 * 60 * 60 * 1000L)
        val criticalEvents = recentEvents.count { it.severity == SecuritySeverity.CRITICAL }
        val warningEvents = recentEvents.count { it.severity == SecuritySeverity.MEDIUM }
        
        // Base score of 100, subtract points for security issues
        var score = 100
        score -= criticalEvents * 20
        score -= warningEvents * 5
        
        return maxOf(0, score)
    }

    /**
     * Security event data class.
     */
    data class SecurityEvent(
        val type: SecurityEventType,
        val userId: String?,
        val message: String,
        val severity: SecuritySeverity,
        val timestamp: Long,
        val metadata: Map<String, Any> = emptyMap()
    )

    /**
     * Security event types.
     */
    enum class SecurityEventType {
        AUTHENTICATION_FAILURE,
        RATE_LIMIT_VIOLATION,
        ENCRYPTION_FAILURE,
        SUSPICIOUS_ACCESS,
        DATA_BREACH_ATTEMPT,
        SECURITY_ALERT,
        CONFIGURATION_CHANGE,
        UNAUTHORIZED_ACCESS
    }

    /**
     * Security severity levels.
     */
    enum class SecuritySeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * Security status information.
     */
    data class SecurityStatus(
        val level: SecurityStatusLevel = SecurityStatusLevel.NORMAL,
        val lastUpdate: Long = System.currentTimeMillis(),
        val activeThreats: Int = 0,
        val recentWarnings: Int = 0
    )

    /**
     * Security status levels.
     */
    enum class SecurityStatusLevel {
        NORMAL, CAUTION, WARNING, CRITICAL
    }

    /**
     * Security scan result.
     */
    sealed class SecurityScanResult {
        data class Success(
            val findings: List<SecurityFinding>,
            val scanDuration: Long,
            val timestamp: Long
        ) : SecurityScanResult()
        
        data class Error(val message: String) : SecurityScanResult()
    }

    /**
     * Security finding from scans.
     */
    data class SecurityFinding(
        val type: FindingType,
        val severity: SecuritySeverity,
        val description: String,
        val recommendation: String
    )

    /**
     * Finding types.
     */
    enum class FindingType {
        UNUSUAL_ACTIVITY,
        POTENTIAL_ATTACK,
        CONFIGURATION_ISSUE,
        DATA_INTEGRITY,
        ACCESS_VIOLATION
    }

    /**
     * Security insight for recommendations.
     */
    data class SecurityInsight(
        val type: InsightType,
        val category: String,
        val message: String,
        val recommendation: String,
        val severity: SecuritySeverity
    )

    /**
     * Insight types.
     */
    enum class InsightType {
        INFO, WARNING, ERROR, CRITICAL
    }

    /**
     * Security metrics for monitoring.
     */
    data class SecurityMetrics(
        val totalEvents: Int,
        val recentEvents: Int,
        val criticalEvents: Int,
        val warningEvents: Int,
        val lastScanTime: Long,
        val overallSecurityScore: Int
    )

    /**
     * Security alert data class.
     */
    data class SecurityAlert(
        val message: String,
        val severity: SecuritySeverity,
        val timestamp: Long
    )
}