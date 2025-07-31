package com.vibehealth.android.monitoring

import android.os.Build
import android.util.Log
import com.vibehealth.android.data.dashboard.privacy.DashboardPrivacyManager
import com.vibehealth.android.ui.dashboard.models.ErrorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Error monitoring and reporting system for dashboard.
 * Implements comprehensive error tracking, performance alerts, and crash reporting.
 */
@Singleton
class DashboardErrorMonitor @Inject constructor(
    private val privacyManager: DashboardPrivacyManager
) {
    
    companion object {
        private const val TAG = "DashboardErrorMonitor"
        private const val FRAME_RATE_THRESHOLD = 55f // Alert if below 55fps
        private const val LOAD_TIME_THRESHOLD = 1000L // Alert if above 1 second
        private const val MEMORY_THRESHOLD = 100f // Alert if above 100MB
    }
    
    /**
     * Reports dashboard errors with privacy-safe information.
     */
    suspend fun reportError(
        error: Throwable,
        context: String,
        userId: String? = null
    ) {
        withContext(Dispatchers.IO) {
            try {
                val safeErrorInfo = privacyManager.validateErrorSafety(error, userId)
                
                val errorReport = ErrorReport(
                    errorType = safeErrorInfo.errorType,
                    message = safeErrorInfo.safeMessage,
                    context = context,
                    timestamp = LocalDateTime.now(),
                    deviceInfo = getDeviceInfo(),
                    containedSensitiveInfo = safeErrorInfo.containedSensitiveInfo
                )
                
                logErrorReport(errorReport)
                
                // Send to crash reporting service (Firebase Crashlytics, etc.)
                sendToCrashReporting(errorReport)
                
                // Track error patterns
                trackErrorPattern(errorReport)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report error", e)
            }
        }
    }
    
    /**
     * Monitors animation performance and alerts on frame drops.
     */
    suspend fun monitorAnimationPerformance(
        frameRate: Float,
        animationType: String,
        durationMs: Long
    ) {
        withContext(Dispatchers.IO) {
            try {
                if (frameRate < FRAME_RATE_THRESHOLD) {
                    val performanceAlert = PerformanceAlert(
                        alertType = AlertType.FRAME_RATE_DROP,
                        severity = calculateSeverity(frameRate, FRAME_RATE_THRESHOLD),
                        details = mapOf(
                            "frame_rate" to frameRate,
                            "animation_type" to animationType,
                            "duration_ms" to durationMs,
                            "threshold" to FRAME_RATE_THRESHOLD
                        ),
                        timestamp = LocalDateTime.now(),
                        deviceInfo = getDeviceInfo()
                    )
                    
                    reportPerformanceAlert(performanceAlert)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to monitor animation performance", e)
            }
        }
    }
    
    /**
     * Monitors dashboard load times and alerts on slow performance.
     */
    suspend fun monitorLoadTime(
        loadTimeMs: Long,
        dataSource: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                if (loadTimeMs > LOAD_TIME_THRESHOLD) {
                    val performanceAlert = PerformanceAlert(
                        alertType = AlertType.SLOW_LOAD_TIME,
                        severity = calculateSeverity(loadTimeMs.toFloat(), LOAD_TIME_THRESHOLD.toFloat()),
                        details = mapOf(
                            "load_time_ms" to loadTimeMs,
                            "data_source" to dataSource,
                            "threshold_ms" to LOAD_TIME_THRESHOLD
                        ),
                        timestamp = LocalDateTime.now(),
                        deviceInfo = getDeviceInfo()
                    )
                    
                    reportPerformanceAlert(performanceAlert)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to monitor load time", e)
            }
        }
    }
    
    /**
     * Monitors memory usage and alerts on high consumption.
     */
    suspend fun monitorMemoryUsage(
        memoryUsageMb: Float,
        context: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                if (memoryUsageMb > MEMORY_THRESHOLD) {
                    val performanceAlert = PerformanceAlert(
                        alertType = AlertType.HIGH_MEMORY_USAGE,
                        severity = calculateSeverity(memoryUsageMb, MEMORY_THRESHOLD),
                        details = mapOf(
                            "memory_usage_mb" to memoryUsageMb,
                            "context" to context,
                            "threshold_mb" to MEMORY_THRESHOLD
                        ),
                        timestamp = LocalDateTime.now(),
                        deviceInfo = getDeviceInfo()
                    )
                    
                    reportPerformanceAlert(performanceAlert)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to monitor memory usage", e)
            }
        }
    }
    
    /**
     * Monitors user experience metrics and reports issues.
     */
    suspend fun monitorUserExperience(
        interactionResponseTimeMs: Long,
        interactionType: String
    ) {
        withContext(Dispatchers.IO) {
            try {
                val responseThreshold = when (interactionType) {
                    "tap" -> 100L
                    "scroll" -> 16L // 60fps = 16ms per frame
                    "animation" -> 250L
                    else -> 200L
                }
                
                if (interactionResponseTimeMs > responseThreshold) {
                    val uxAlert = UserExperienceAlert(
                        alertType = UXAlertType.SLOW_INTERACTION_RESPONSE,
                        interactionType = interactionType,
                        responseTimeMs = interactionResponseTimeMs,
                        thresholdMs = responseThreshold,
                        timestamp = LocalDateTime.now(),
                        deviceInfo = getDeviceInfo()
                    )
                    
                    reportUserExperienceAlert(uxAlert)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to monitor user experience", e)
            }
        }
    }
    
    /**
     * Reports crash with privacy-safe information.
     */
    suspend fun reportCrash(
        crashInfo: CrashInfo,
        userId: String? = null
    ) {
        withContext(Dispatchers.IO) {
            try {
                val safeCrashInfo = sanitizeCrashInfo(crashInfo, userId)
                
                val crashReport = CrashReport(
                    crashType = safeCrashInfo.crashType,
                    message = safeCrashInfo.message,
                    stackTrace = safeCrashInfo.stackTrace,
                    timestamp = LocalDateTime.now(),
                    deviceInfo = getDeviceInfo(),
                    appState = safeCrashInfo.appState
                )
                
                logCrashReport(crashReport)
                sendToCrashReporting(crashReport)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report crash", e)
            }
        }
    }
    
    /**
     * Gets device information for error reporting.
     */
    private fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.SDK_INT,
            appVersion = "1.0.0", // Would get from BuildConfig
            availableMemoryMb = getAvailableMemory()
        )
    }
    
    /**
     * Calculates severity based on threshold breach.
     */
    private fun calculateSeverity(value: Float, threshold: Float): Severity {
        val ratio = value / threshold
        return when {
            ratio > 2.0f -> Severity.CRITICAL
            ratio > 1.5f -> Severity.HIGH
            ratio > 1.2f -> Severity.MEDIUM
            else -> Severity.LOW
        }
    }
    
    /**
     * Gets available memory in MB.
     */
    private fun getAvailableMemory(): Float {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val availableMemory = maxMemory - totalMemory + freeMemory
        return availableMemory / (1024f * 1024f) // Convert to MB
    }
    
    /**
     * Sanitizes crash information for privacy.
     */
    private fun sanitizeCrashInfo(crashInfo: CrashInfo, userId: String?): CrashInfo {
        return crashInfo.copy(
            message = privacyManager.sanitizeErrorMessage(crashInfo.message, userId),
            stackTrace = sanitizeStackTrace(crashInfo.stackTrace, userId)
        )
    }
    
    /**
     * Sanitizes stack trace for privacy.
     */
    private fun sanitizeStackTrace(stackTrace: String, userId: String?): String {
        return privacyManager.sanitizeErrorMessage(stackTrace, userId)
    }
    
    /**
     * Logs error report.
     */
    private fun logErrorReport(errorReport: ErrorReport) {
        Log.e(TAG, "Error Report: ${errorReport.errorType} - ${errorReport.message}")
        
        if (errorReport.containedSensitiveInfo) {
            Log.w(TAG, "Error report contained sensitive information that was sanitized")
        }
    }
    
    /**
     * Reports performance alert.
     */
    private fun reportPerformanceAlert(alert: PerformanceAlert) {
        Log.w(TAG, "Performance Alert: ${alert.alertType} - Severity: ${alert.severity}")
        
        // In production, this would send to monitoring service
        // monitoringService.sendAlert(alert)
    }
    
    /**
     * Reports user experience alert.
     */
    private fun reportUserExperienceAlert(alert: UserExperienceAlert) {
        Log.w(TAG, "UX Alert: ${alert.alertType} - ${alert.interactionType} took ${alert.responseTimeMs}ms")
        
        // In production, this would send to UX monitoring service
        // uxMonitoringService.sendAlert(alert)
    }
    
    /**
     * Logs crash report.
     */
    private fun logCrashReport(crashReport: CrashReport) {
        Log.e(TAG, "Crash Report: ${crashReport.crashType} - ${crashReport.message}")
    }
    
    /**
     * Sends to crash reporting service.
     */
    private fun sendToCrashReporting(report: Any) {
        // In production, this would send to Firebase Crashlytics, Bugsnag, etc.
        // crashReportingService.report(report)
    }
    
    /**
     * Tracks error patterns for analysis.
     */
    private fun trackErrorPattern(errorReport: ErrorReport) {
        // Track error frequency, common patterns, etc.
        Log.d(TAG, "Tracking error pattern: ${errorReport.errorType}")
    }
}

// Data classes for error reporting
data class ErrorReport(
    val errorType: String,
    val message: String,
    val context: String,
    val timestamp: LocalDateTime,
    val deviceInfo: DeviceInfo,
    val containedSensitiveInfo: Boolean
)

data class PerformanceAlert(
    val alertType: AlertType,
    val severity: Severity,
    val details: Map<String, Any>,
    val timestamp: LocalDateTime,
    val deviceInfo: DeviceInfo
)

data class UserExperienceAlert(
    val alertType: UXAlertType,
    val interactionType: String,
    val responseTimeMs: Long,
    val thresholdMs: Long,
    val timestamp: LocalDateTime,
    val deviceInfo: DeviceInfo
)

data class CrashReport(
    val crashType: String,
    val message: String,
    val stackTrace: String,
    val timestamp: LocalDateTime,
    val deviceInfo: DeviceInfo,
    val appState: String
)

data class CrashInfo(
    val crashType: String,
    val message: String,
    val stackTrace: String,
    val appState: String
)

data class DeviceInfo(
    val model: String,
    val manufacturer: String,
    val androidVersion: Int,
    val appVersion: String,
    val availableMemoryMb: Float
)

enum class AlertType {
    FRAME_RATE_DROP,
    SLOW_LOAD_TIME,
    HIGH_MEMORY_USAGE
}

enum class UXAlertType {
    SLOW_INTERACTION_RESPONSE
}

enum class Severity {
    LOW, MEDIUM, HIGH, CRITICAL
}