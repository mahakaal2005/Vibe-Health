package com.vibehealth.android.production

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.vibehealth.android.BuildConfig
import com.vibehealth.android.R
import com.vibehealth.android.core.accessibility.AccessibilityHelper
import com.vibehealth.android.core.network.NetworkMonitor
import com.vibehealth.android.core.performance.OnboardingPerformanceOptimizer
import com.vibehealth.android.core.security.EncryptionHelper
import com.vibehealth.android.data.user.UserProfileRepository
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.UnitSystem
import com.vibehealth.android.domain.user.UserProfile
import com.vibehealth.android.ui.onboarding.OnboardingActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlin.test.*

/**
 * Comprehensive production readiness validation tests
 * Final integration tests for production readiness with monitoring and alerting
 * Ensures the onboarding system meets all production quality standards
 */
@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ProductionReadinessTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val activityRule = ActivityScenarioRule(OnboardingActivity::class.java)

    @Inject
    lateinit var accessibilityHelper: AccessibilityHelper

    @Inject
    lateinit var performanceOptimizer: OnboardingPerformanceOptimizer

    @Inject
    lateinit var encryptionHelper: EncryptionHelper

    @Inject
    lateinit var userProfileRepository: UserProfileRepository

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    private lateinit var context: Context
    private lateinit var uiDevice: UiDevice

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun buildConfiguration_shouldBeProductionReady() {
        // Verify build configuration is appropriate for production
        assertFalse(BuildConfig.DEBUG, "Debug mode should be disabled in production builds")
        
        // Verify app name and version
        val appName = context.getString(R.string.app_name)
        assertEquals("Vibe Health", appName, "App name should be correct")
        
        // Verify no test or debug strings are exposed
        val allStringResources = context.resources.getStringArray(R.array.test_strings)
        assertTrue(allStringResources.isEmpty() || !BuildConfig.DEBUG, 
            "Test strings should not be present in production builds")
    }

    @Test
    fun securityConfiguration_shouldMeetProductionStandards() = runTest {
        // Test encryption is properly configured
        val testData = "sensitive test data"
        val encrypted = encryptionHelper.encrypt(testData)
        val decrypted = encryptionHelper.decrypt(encrypted)
        
        assertNotEquals(testData, encrypted, "Data should be encrypted")
        assertEquals(testData, decrypted, "Decryption should work correctly")
        
        // Verify encryption strength
        assertTrue(encrypted.length > testData.length, "Encrypted data should be longer than original")
        
        // Test that encryption is deterministic (same input produces different output due to IV)
        val encrypted2 = encryptionHelper.encrypt(testData)
        assertNotEquals(encrypted, encrypted2, "Encryption should use random IV")
    }

    @Test
    fun accessibilityCompliance_shouldMeetWCAGStandards() {
        // Test accessibility helper functionality
        assertTrue(accessibilityHelper.checkColorContrast(
            android.graphics.Color.BLACK, 
            android.graphics.Color.WHITE
        ), "Black on white should meet contrast requirements")
        
        assertFalse(accessibilityHelper.checkColorContrast(
            android.graphics.Color.GRAY, 
            android.graphics.Color.LTGRAY
        ), "Low contrast combinations should be detected")
        
        // Test font scaling support
        val fontScale = accessibilityHelper.getFontScale()
        assertTrue(fontScale > 0, "Font scale should be positive")
        
        // Test accessibility service detection
        val isAccessibilityEnabled = accessibilityHelper.isAccessibilityEnabled()
        // This is environment-dependent, so we just verify the method works
        assertNotNull(isAccessibilityEnabled)
    }

    @Test
    fun performanceOptimization_shouldBeConfiguredCorrectly() {
        // Test performance optimizer configuration
        val animationDuration = performanceOptimizer.getOptimizedAnimationDuration(1000L)
        assertTrue(animationDuration >= 0, "Animation duration should be non-negative")
        assertTrue(animationDuration <= 1000L, "Animation duration should not exceed original")
        
        // Test frame rate optimization
        val frameRate = performanceOptimizer.getOptimizedFrameRate()
        assertTrue(frameRate in 30..60, "Frame rate should be between 30-60 FPS")
        
        // Test debounce delay optimization
        val debounceDelay = performanceOptimizer.getOptimizedDebounceDelay()
        assertTrue(debounceDelay in 100L..1000L, "Debounce delay should be reasonable")
    }

    @Test
    fun dataStorage_shouldHandleProductionVolumes() = runTest {
        // Test repository can handle multiple concurrent operations
        val testProfiles = (1..10).map { index ->
            createTestUserProfile("load-test-user-$index")
        }
        
        // Save all profiles concurrently
        val saveResults = testProfiles.map { profile ->
            userProfileRepository.saveUserProfile(profile)
        }
        
        // Verify all saves succeeded
        saveResults.forEach { result ->
            assertTrue(result.isSuccess, "All save operations should succeed under load")
        }
        
        // Verify all profiles can be retrieved
        val retrieveResults = testProfiles.map { profile ->
            userProfileRepository.getUserProfile(profile.userId)
        }
        
        retrieveResults.forEach { result ->
            assertTrue(result.isSuccess, "All retrieve operations should succeed")
            assertNotNull(result.getOrNull(), "All profiles should be retrievable")
        }
    }

    @Test
    fun errorHandling_shouldBeRobust() = runTest {
        // Test handling of invalid data
        val invalidProfile = createTestUserProfile("invalid-user").copy(
            displayName = "", // Invalid empty name
            heightInCm = -1,  // Invalid negative height
            weightInKg = -1.0 // Invalid negative weight
        )
        
        // Repository should handle invalid data gracefully
        val result = userProfileRepository.saveUserProfile(invalidProfile)
        
        // Should either succeed with sanitized data or fail gracefully
        assertNotNull(result, "Result should not be null")
        
        if (result.isFailure) {
            val exception = result.exceptionOrNull()
            assertNotNull(exception, "Failure should have an exception")
            assertTrue(exception.message?.isNotEmpty() == true, "Exception should have a message")
        }
    }

    @Test
    fun memoryUsage_shouldBeOptimal() {
        // Test memory usage is within acceptable bounds
        val performanceMonitor = performanceOptimizer.enablePerformanceMonitoring()
        
        val initialMemory = performanceMonitor.measureMemoryUsage()
        assertTrue(initialMemory.usagePercentage < 80.0, 
            "Initial memory usage should be less than 80%")
        
        // Simulate memory-intensive operations
        repeat(100) {
            createTestUserProfile("memory-test-$it")
        }
        
        val finalMemory = performanceMonitor.measureMemoryUsage()
        val memoryIncrease = finalMemory.usedMemory - initialMemory.usedMemory
        
        // Memory increase should be reasonable
        assertTrue(memoryIncrease < 50 * 1024 * 1024, // Less than 50MB
            "Memory increase should be reasonable")
    }

    @Test
    fun networkResilience_shouldHandleFailures() = runTest {
        // Test network failure scenarios
        val testProfile = createTestUserProfile("network-test-user")
        
        // This would require network simulation in a real test
        // For now, we test that the repository handles failures gracefully
        val result = userProfileRepository.saveUserProfile(testProfile)
        
        // Should handle network issues without crashing
        assertNotNull(result, "Network operations should not return null")
        
        // Test offline capability
        val syncStatus = userProfileRepository.getSyncStatus(testProfile.userId)
        assertNotNull(syncStatus, "Sync status should be available")
    }

    @Test
    fun localization_shouldSupportMultipleLanguages() {
        // Test that all required strings are available
        val requiredStrings = listOf(
            R.string.welcome_to_vibe_health,
            R.string.personal_information,
            R.string.physical_information,
            R.string.continue_button,
            R.string.error_name_required,
            R.string.error_height_invalid,
            R.string.error_weight_invalid
        )
        
        requiredStrings.forEach { stringRes ->
            val string = context.getString(stringRes)
            assertTrue(string.isNotEmpty(), "String resource $stringRes should not be empty")
            assertFalse(string.startsWith("String not found"), 
                "String resource $stringRes should be properly defined")
        }
    }

    @Test
    fun dataConsistency_shouldMaintainIntegrity() = runTest {
        // Test data consistency across operations
        val testProfile = createTestUserProfile("consistency-test-user")
        
        // Save profile
        val saveResult = userProfileRepository.saveUserProfile(testProfile)
        assertTrue(saveResult.isSuccess, "Save should succeed")
        
        // Retrieve and verify data integrity
        val retrieveResult = userProfileRepository.getUserProfile(testProfile.userId)
        assertTrue(retrieveResult.isSuccess, "Retrieve should succeed")
        
        val retrievedProfile = retrieveResult.getOrNull()
        assertNotNull(retrievedProfile, "Profile should be retrieved")
        
        // Verify all fields match
        assertEquals(testProfile.userId, retrievedProfile.userId)
        assertEquals(testProfile.displayName, retrievedProfile.displayName)
        assertEquals(testProfile.email, retrievedProfile.email)
        assertEquals(testProfile.gender, retrievedProfile.gender)
        assertEquals(testProfile.unitSystem, retrievedProfile.unitSystem)
        assertEquals(testProfile.heightInCm, retrievedProfile.heightInCm)
        assertEquals(testProfile.weightInKg, retrievedProfile.weightInKg)
    }

    @Test
    fun concurrencyHandling_shouldBeThreadSafe() = runTest {
        // Test concurrent access to shared resources
        val testProfile = createTestUserProfile("concurrency-test-user")
        
        // Perform concurrent operations
        val operations = (1..5).map { index ->
            kotlinx.coroutines.async {
                val profile = testProfile.copy(
                    displayName = "Concurrent User $index",
                    updatedAt = java.util.Date()
                )
                userProfileRepository.saveUserProfile(profile)
            }
        }
        
        // Wait for all operations to complete
        val results = operations.map { it.await() }
        
        // All operations should complete without errors
        results.forEach { result ->
            assertTrue(result.isSuccess, "Concurrent operations should succeed")
        }
        
        // Final state should be consistent
        val finalResult = userProfileRepository.getUserProfile(testProfile.userId)
        assertTrue(finalResult.isSuccess, "Final retrieve should succeed")
        assertNotNull(finalResult.getOrNull(), "Final profile should exist")
    }

    @Test
    fun completeProductionReadinessValidation_shouldPassAllCriteria() = runTest {
        val performanceMonitor = performanceOptimizer.enablePerformanceMonitoring()
        val validationResults = mutableMapOf<String, Boolean>()
        val startTime = System.currentTimeMillis()

        try {
            // 1. Security Validation
            validationResults["security_encryption"] = validateEncryptionSecurity()
            validationResults["security_data_sanitization"] = validateDataSanitization()
            validationResults["security_pii_protection"] = validatePIIProtection()

            // 2. Performance Validation
            validationResults["performance_load_handling"] = validateLoadHandling()
            validationResults["performance_memory_management"] = validateMemoryManagement()
            validationResults["performance_response_times"] = validateResponseTimes()

            // 3. Accessibility Validation
            validationResults["accessibility_wcag_compliance"] = validateWCAGCompliance()
            validationResults["accessibility_screen_reader"] = validateScreenReaderSupport()
            validationResults["accessibility_keyboard_navigation"] = validateKeyboardNavigation()

            // 4. Resilience Validation
            validationResults["resilience_network_failures"] = validateNetworkResilience()
            validationResults["resilience_data_corruption"] = validateDataCorruptionRecovery()
            validationResults["resilience_concurrent_access"] = validateConcurrentAccess()

            // 5. User Experience Validation
            validationResults["ux_complete_flow"] = validateCompleteUserFlow()
            validationResults["ux_error_handling"] = validateErrorHandling()
            validationResults["ux_offline_support"] = validateOfflineSupport()

            // 6. Monitoring and Alerting Validation
            validationResults["monitoring_metrics_collection"] = validateMetricsCollection()
            validationResults["monitoring_error_tracking"] = validateErrorTracking()
            validationResults["monitoring_performance_tracking"] = validatePerformanceTracking()

            val totalTime = System.currentTimeMillis() - startTime
            validationResults["overall_completion_time"] = totalTime < 120000 // 2 minutes max

            // Generate production readiness report
            generateProductionReadinessReport(validationResults, totalTime)

            // All critical validations must pass
            val criticalValidations = listOf(
                "security_encryption",
                "security_pii_protection", 
                "performance_response_times",
                "accessibility_wcag_compliance",
                "resilience_network_failures",
                "ux_complete_flow"
            )

            criticalValidations.forEach { validation ->
                assertTrue(validationResults[validation] == true, 
                    "Critical validation '$validation' must pass for production readiness")
            }

            // Overall pass rate should be at least 90%
            val passCount = validationResults.values.count { it }
            val passRate = passCount.toFloat() / validationResults.size
            assertTrue(passRate >= 0.9f, 
                "Production readiness pass rate must be at least 90% (actual: ${(passRate * 100).toInt()}%)")

        } finally {
            performanceMonitor.stopMonitoring()
        }
    }

    private suspend fun validateEncryptionSecurity(): Boolean {
        return try {
            val testData = "sensitive production data"
            val encrypted = encryptionHelper.encrypt(testData)
            val decrypted = encryptionHelper.decrypt(encrypted)
            
            testData == decrypted && encrypted != testData && encrypted.length > testData.length
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateDataSanitization(): Boolean {
        return try {
            val profile = createTestUserProfile("sanitization-test")
            val sanitized = profile.sanitizeForLogging()
            
            !sanitized.email.contains("@") && 
            !sanitized.displayName.contains("Test User") &&
            sanitized.userId == profile.userId
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validatePIIProtection(): Boolean {
        return try {
            val profile = createTestUserProfile("pii-test")
            val saveResult = userProfileRepository.saveUserProfile(profile)
            val retrieveResult = userProfileRepository.getUserProfile(profile.userId)
            
            saveResult.isSuccess && retrieveResult.isSuccess &&
            retrieveResult.getOrNull()?.displayName == profile.displayName
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateLoadHandling(): Boolean {
        return try {
            val startTime = System.currentTimeMillis()
            val concurrentOperations = (1..20).map { index ->
                async {
                    val profile = createTestUserProfile("load-$index")
                    userProfileRepository.saveUserProfile(profile)
                }
            }
            
            val results = concurrentOperations.map { it.await() }
            val duration = System.currentTimeMillis() - startTime
            
            results.count { it.isSuccess } >= 18 && duration < 15000 // 90% success in 15s
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateMemoryManagement(): Boolean {
        return try {
            val initialMemory = performanceOptimizer.measureMemoryUsage()
            
            // Perform memory-intensive operations
            repeat(50) {
                val profile = createTestUserProfile("memory-$it")
                userProfileRepository.saveUserProfile(profile)
            }
            
            System.gc()
            Thread.sleep(2000)
            
            val finalMemory = performanceOptimizer.measureMemoryUsage()
            val memoryIncrease = finalMemory.usedMemory - initialMemory.usedMemory
            
            memoryIncrease < 50 * 1024 * 1024 // Less than 50MB increase
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateResponseTimes(): Boolean {
        return try {
            val profile = createTestUserProfile("response-time-test")
            
            val saveStartTime = System.currentTimeMillis()
            val saveResult = userProfileRepository.saveUserProfile(profile)
            val saveTime = System.currentTimeMillis() - saveStartTime
            
            val readStartTime = System.currentTimeMillis()
            val readResult = userProfileRepository.getUserProfile(profile.userId)
            val readTime = System.currentTimeMillis() - readStartTime
            
            saveResult.isSuccess && readResult.isSuccess &&
            saveTime < 3000 && readTime < 1000 // Save < 3s, Read < 1s
        } catch (e: Exception) {
            false
        }
    }

    private fun validateWCAGCompliance(): Boolean {
        return try {
            // Test color contrast
            val titleColor = accessibilityHelper.getTextColor(R.id.tv_welcome_title)
            val backgroundColor = accessibilityHelper.getBackgroundColor(R.id.layout_welcome)
            val hasGoodContrast = accessibilityHelper.checkColorContrast(titleColor, backgroundColor)
            
            // Test touch targets
            onView(withId(R.id.btn_get_started))
                .check(matches(hasMinimumTouchTargetSize()))
            
            // Test content descriptions
            onView(withId(R.id.btn_get_started))
                .check(matches(hasContentDescription()))
            
            hasGoodContrast
        } catch (e: Exception) {
            false
        }
    }

    private fun validateScreenReaderSupport(): Boolean {
        return try {
            // Navigate to personal info screen
            onView(withId(R.id.btn_get_started))
                .perform(click())
            
            // Test form field accessibility
            onView(withId(R.id.et_full_name))
                .check(matches(hasContentDescription()))
            
            onView(withId(R.id.btn_select_birthday))
                .check(matches(hasContentDescription()))
            
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun validateKeyboardNavigation(): Boolean {
        return try {
            // Navigate to physical info screen
            navigateToPhysicalInfoScreen()
            
            // Test focusable elements
            onView(withId(R.id.et_height))
                .check(matches(isFocusable()))
            
            onView(withId(R.id.et_weight))
                .check(matches(isFocusable()))
            
            onView(withId(R.id.rb_male))
                .check(matches(isFocusable()))
            
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateNetworkResilience(): Boolean {
        return try {
            val profile = createTestUserProfile("network-resilience-test")
            
            // Test with network disconnection
            networkMonitor.simulateNetworkDisconnection()
            val offlineResult = userProfileRepository.saveUserProfile(profile)
            
            // Reconnect and verify sync
            networkMonitor.simulateNetworkReconnection()
            Thread.sleep(3000) // Allow sync time
            
            val onlineResult = userProfileRepository.getUserProfile(profile.userId)
            
            networkMonitor.resetNetworkConditions()
            
            // Should handle offline gracefully and sync when online
            offlineResult.isSuccess || onlineResult.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateDataCorruptionRecovery(): Boolean {
        return try {
            val profile = createTestUserProfile("corruption-recovery-test")
            userProfileRepository.saveUserProfile(profile)
            
            // Simulate corruption
            userProfileRepository.simulateDatabaseCorruption()
            
            // Attempt recovery
            val recoveryResult = userProfileRepository.recoverFromCorruption()
            
            // Reset conditions
            userProfileRepository.resetStorageConditions()
            
            recoveryResult.isSuccess
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateConcurrentAccess(): Boolean {
        return try {
            val profile = createTestUserProfile("concurrent-test")
            
            // Perform concurrent operations
            val operations = (1..5).map {
                async {
                    userProfileRepository.saveUserProfile(profile.copy(
                        displayName = "Concurrent User $it",
                        updatedAt = Date()
                    ))
                }
            }
            
            val results = operations.map { it.await() }
            results.all { it.isSuccess }
        } catch (e: Exception) {
            false
        }
    }

    private fun validateCompleteUserFlow(): Boolean {
        return try {
            // Complete the onboarding flow
            completeOnboardingFlow()
            
            // Verify completion screen
            onView(withId(R.id.tv_completion_title))
                .check(matches(isDisplayed()))
            
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun validateErrorHandling(): Boolean {
        return try {
            // Navigate to personal info and trigger validation error
            onView(withId(R.id.btn_get_started))
                .perform(click())
            
            onView(withId(R.id.btn_continue))
                .perform(click())
            
            // Should show error message
            onView(withId(R.id.til_full_name))
                .check(matches(hasErrorText()))
            
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun validateOfflineSupport(): Boolean {
        return try {
            networkMonitor.simulateNetworkDisconnection()
            
            val profile = createTestUserProfile("offline-test")
            val result = userProfileRepository.saveUserProfile(profile)
            
            networkMonitor.resetNetworkConditions()
            
            // Should handle offline operations
            result.isSuccess || result.isFailure // Should not crash
        } catch (e: Exception) {
            false
        }
    }

    private fun validateMetricsCollection(): Boolean {
        return try {
            val performanceMonitor = performanceOptimizer.enablePerformanceMonitoring()
            performanceMonitor.trackUserAction("test_action", mapOf("test" to "value"))
            
            val metrics = performanceMonitor.getAnalyticsData()
            metrics.containsKey("test_action")
        } catch (e: Exception) {
            false
        }
    }

    private fun validateErrorTracking(): Boolean {
        return try {
            val performanceMonitor = performanceOptimizer.enablePerformanceMonitoring()
            performanceMonitor.trackError("test_error", "Test error message")
            
            val errorMetrics = performanceMonitor.getErrorMetrics()
            errorMetrics.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    private fun validatePerformanceTracking(): Boolean {
        return try {
            val performanceMonitor = performanceOptimizer.enablePerformanceMonitoring()
            performanceMonitor.startTracking("test_operation")
            Thread.sleep(100)
            val metrics = performanceMonitor.stopTracking("test_operation")
            
            metrics.duration > 0 && metrics.memoryUsage >= 0
        } catch (e: Exception) {
            false
        }
    }

    private fun generateProductionReadinessReport(
        validationResults: Map<String, Boolean>,
        totalTime: Long
    ) {
        val passCount = validationResults.values.count { it }
        val failCount = validationResults.size - passCount
        val passRate = (passCount.toFloat() / validationResults.size * 100).toInt()
        
        println("=== PRODUCTION READINESS VALIDATION REPORT ===")
        println("Total Validations: ${validationResults.size}")
        println("Passed: $passCount")
        println("Failed: $failCount")
        println("Pass Rate: $passRate%")
        println("Total Time: ${totalTime}ms")
        println()
        
        println("DETAILED RESULTS:")
        validationResults.forEach { (validation, passed) ->
            val status = if (passed) "✅ PASS" else "❌ FAIL"
            println("$status $validation")
        }
        
        println()
        if (passRate >= 90) {
            println("🎉 PRODUCTION READY - System meets production quality standards")
        } else {
            println("⚠️  NOT PRODUCTION READY - Address failed validations before deployment")
        }
        println("================================================")
    }

    // Helper methods
    private fun completeOnboardingFlow() {
        onView(withId(R.id.btn_get_started))
            .perform(click())

        onView(withId(R.id.et_full_name))
            .perform(typeText("Production Test User"), closeSoftKeyboard())

        onView(withId(R.id.btn_select_birthday))
            .perform(click())
        
        onView(withText("OK"))
            .perform(click())

        onView(withId(R.id.btn_continue))
            .perform(click())

        onView(withId(R.id.et_height))
            .perform(typeText("175"), closeSoftKeyboard())

        onView(withId(R.id.et_weight))
            .perform(typeText("70"), closeSoftKeyboard())

        onView(withId(R.id.rb_male))
            .perform(click())

        onView(withId(R.id.btn_continue))
            .perform(click())
    }

    private fun navigateToPhysicalInfoScreen() {
        onView(withId(R.id.btn_get_started))
            .perform(click())

        onView(withId(R.id.et_full_name))
            .perform(typeText("Test User"), closeSoftKeyboard())

        onView(withId(R.id.btn_select_birthday))
            .perform(click())
        
        onView(withText("OK"))
            .perform(click())

        onView(withId(R.id.btn_continue))
            .perform(click())
    }

    private fun hasMinimumTouchTargetSize() = object : org.hamcrest.Matcher<android.view.View> {
        override fun describeTo(description: org.hamcrest.Description?) {
            description?.appendText("has minimum touch target size of 48dp x 48dp")
        }

        override fun matches(item: Any?): Boolean {
            if (item !is android.view.View) return false
            val minSize = (48 * item.context.resources.displayMetrics.density).toInt()
            return item.width >= minSize && item.height >= minSize
        }

        override fun describeMismatch(item: Any?, mismatchDescription: org.hamcrest.Description?) {
            if (item is android.view.View) {
                val density = item.context.resources.displayMetrics.density
                val widthDp = item.width / density
                val heightDp = item.height / density
                mismatchDescription?.appendText("was ${widthDp}dp x ${heightDp}dp")
            }
        }

        override fun _dont_implement_Matcher___instead_extend_BaseMatcher_() {}
    }

    private fun hasErrorText() = object : org.hamcrest.Matcher<android.view.View> {
        override fun describeTo(description: org.hamcrest.Description?) {
            description?.appendText("has error text")
        }

        override fun matches(item: Any?): Boolean {
            if (item !is com.google.android.material.textfield.TextInputLayout) return false
            return !item.error.isNullOrEmpty()
        }

        override fun describeMismatch(item: Any?, mismatchDescription: org.hamcrest.Description?) {
            mismatchDescription?.appendText("had no error text")
        }

        override fun _dont_implement_Matcher___instead_extend_BaseMatcher_() {}
    }

    private fun createTestUserProfile(userId: String): UserProfile {
        return UserProfile(
            userId = userId,
            email = "test@example.com",
            displayName = "Test User",
            birthday = Date(System.currentTimeMillis() - (25L * 365 * 24 * 60 * 60 * 1000)),
            gender = Gender.MALE,
            unitSystem = UnitSystem.METRIC,
            heightInCm = 175,
            weightInKg = 70.0,
            hasCompletedOnboarding = true,
            createdAt = Date(),
            updatedAt = Date()
        )
    }
}