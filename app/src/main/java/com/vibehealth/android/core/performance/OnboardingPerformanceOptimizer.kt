package com.vibehealth.android.core.performance

import android.content.Context
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.vibehealth.android.core.accessibility.AccessibilityHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance optimizer for onboarding screens
 * Handles animation optimization, resource prefetching, and memory management
 */
@Singleton
class OnboardingPerformanceOptimizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accessibilityHelper: AccessibilityHelper
) : DefaultLifecycleObserver {

    private val optimizationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefetchCache = mutableMapOf<String, Any>()
    private var isLowEndDevice: Boolean = false

    init {
        detectDeviceCapabilities()
    }

    /**
     * Detect device capabilities for performance optimization
     */
    private fun detectDeviceCapabilities() {
        isLowEndDevice = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                activityManager.isLowRamDevice
            }
            else -> {
                // Fallback detection for older devices
                val runtime = Runtime.getRuntime()
                val maxMemory = runtime.maxMemory()
                maxMemory < 512 * 1024 * 1024 // Less than 512MB
            }
        }
    }

    /**
     * Get optimized animation duration based on device capabilities and accessibility settings
     */
    fun getOptimizedAnimationDuration(defaultDuration: Long): Long {
        return when {
            accessibilityHelper.shouldReduceAnimations() -> 0L
            isLowEndDevice -> (defaultDuration * 0.5).toLong()
            else -> defaultDuration
        }
    }

    /**
     * Prefetch resources for next onboarding step
     */
    fun prefetchNextStepResources(currentStep: com.vibehealth.android.domain.onboarding.OnboardingStep) {
        optimizationScope.launch {
            val nextStep = currentStep.getNextStep()
            nextStep?.let { step ->
                prefetchResourcesForStep(step)
            }
        }
    }

    /**
     * Prefetch resources for a specific step
     */
    private suspend fun prefetchResourcesForStep(step: com.vibehealth.android.domain.onboarding.OnboardingStep) {
        try {
            when (step) {
                com.vibehealth.android.domain.onboarding.OnboardingStep.PERSONAL_INFO -> {
                    // Prefetch validation resources
                    prefetchCache["name_validation"] = "cached"
                    prefetchCache["date_picker"] = "cached"
                }
                com.vibehealth.android.domain.onboarding.OnboardingStep.PHYSICAL_INFO -> {
                    // Prefetch unit conversion resources
                    prefetchCache["unit_conversion"] = "cached"
                    prefetchCache["validation_ranges"] = "cached"
                }
                com.vibehealth.android.domain.onboarding.OnboardingStep.COMPLETION -> {
                    // Prefetch goal calculation resources
                    prefetchCache["goal_calculation"] = "cached"
                }
                else -> {
                    // No specific prefetching needed
                }
            }
        } catch (e: Exception) {
            // Log error but don't fail the prefetch operation
            android.util.Log.w("OnboardingPerformance", "Failed to prefetch resources for step $step", e)
        }
    }

    /**
     * Optimize memory usage by clearing unused resources
     */
    fun optimizeMemoryUsage() {
        optimizationScope.launch {
            // Clear prefetch cache if memory is low
            if (isMemoryLow()) {
                prefetchCache.clear()
                System.gc() // Suggest garbage collection
            }
        }
    }

    /**
     * Check if device memory is low
     */
    private fun isMemoryLow(): Boolean {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryUsageRatio = usedMemory.toDouble() / maxMemory.toDouble()
        
        return memoryUsageRatio > 0.8 // More than 80% memory used
    }

    /**
     * Get optimized frame rate for animations
     */
    fun getOptimizedFrameRate(): Int {
        return when {
            isLowEndDevice -> 30 // Lower frame rate for low-end devices
            else -> 60 // Standard frame rate
        }
    }

    /**
     * Optimize image loading based on device capabilities
     */
    fun getOptimizedImageSize(originalSize: Int): Int {
        return when {
            isLowEndDevice -> (originalSize * 0.75).toInt()
            else -> originalSize
        }
    }

    /**
     * Enable performance monitoring for debugging
     */
    fun enablePerformanceMonitoring(): PerformanceMonitor {
        return PerformanceMonitor()
    }

    /**
     * Optimize layout inflation
     */
    fun shouldUseAsyncLayoutInflation(): Boolean {
        return !isLowEndDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }

    /**
     * Get debounce delay for input validation
     */
    fun getOptimizedDebounceDelay(): Long {
        return when {
            isLowEndDevice -> 500L // Longer delay for low-end devices
            else -> 300L // Standard delay
        }
    }

    /**
     * Optimize bitmap loading
     */
    fun getOptimizedBitmapConfig(): android.graphics.Bitmap.Config {
        return when {
            isLowEndDevice -> android.graphics.Bitmap.Config.RGB_565 // Lower memory usage
            else -> android.graphics.Bitmap.Config.ARGB_8888 // Better quality
        }
    }

    /**
     * Check if device supports hardware acceleration
     */
    fun supportsHardwareAcceleration(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && !isLowEndDevice
    }

    /**
     * Lifecycle callbacks for memory management
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // Resume prefetching when app becomes visible
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // Optimize memory when app goes to background
        optimizeMemoryUsage()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        // Clean up resources
        optimizationScope.cancel()
        prefetchCache.clear()
    }

    /**
     * Performance monitoring helper
     */
    inner class PerformanceMonitor {
        private val startTimes = mutableMapOf<String, Long>()

        fun startTiming(operation: String) {
            startTimes[operation] = System.currentTimeMillis()
        }

        fun endTiming(operation: String): Long {
            val startTime = startTimes.remove(operation) ?: return -1
            val duration = System.currentTimeMillis() - startTime
            
            // Log performance metrics (without PII)
            android.util.Log.d("OnboardingPerformance", "Operation '$operation' took ${duration}ms")
            
            return duration
        }

        fun measureMemoryUsage(): MemoryInfo {
            val runtime = Runtime.getRuntime()
            return MemoryInfo(
                usedMemory = runtime.totalMemory() - runtime.freeMemory(),
                totalMemory = runtime.totalMemory(),
                maxMemory = runtime.maxMemory()
            )
        }
    }

    /**
     * Memory information data class
     */
    data class MemoryInfo(
        val usedMemory: Long,
        val totalMemory: Long,
        val maxMemory: Long
    ) {
        val usagePercentage: Double
            get() = (usedMemory.toDouble() / maxMemory.toDouble()) * 100
    }
}

/**
 * Extension function to get next step
 */
private fun com.vibehealth.android.domain.onboarding.OnboardingStep.getNextStep(): com.vibehealth.android.domain.onboarding.OnboardingStep? {
    return when (this) {
        com.vibehealth.android.domain.onboarding.OnboardingStep.WELCOME -> com.vibehealth.android.domain.onboarding.OnboardingStep.PERSONAL_INFO
        com.vibehealth.android.domain.onboarding.OnboardingStep.PERSONAL_INFO -> com.vibehealth.android.domain.onboarding.OnboardingStep.PHYSICAL_INFO
        com.vibehealth.android.domain.onboarding.OnboardingStep.PHYSICAL_INFO -> com.vibehealth.android.domain.onboarding.OnboardingStep.COMPLETION
        com.vibehealth.android.domain.onboarding.OnboardingStep.COMPLETION -> null
    }
}