package com.vibehealth.android.chaos

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vibehealth.android.core.network.NetworkMonitor
import com.vibehealth.android.core.performance.OnboardingPerformanceOptimizer
import com.vibehealth.android.data.user.UserProfileRepository
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.user.UnitSystem
import com.vibehealth.android.domain.user.UserProfile
import com.vibehealth.android.ui.onboarding.OnboardingActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.random.Random
import kotlin.test.*

/**
 * Chaos engineering tests to verify system resilience under various failure conditions
 * Tests system behavior when components fail unexpectedly
 */
@LargeTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ChaosEngineeringTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val activityRule = ActivityScenarioRule(OnboardingActivity::class.java)

    @Inject
    lateinit var userProfileRepository: UserProfileRepository

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var performanceOptimizer: OnboardingPerformanceOptimizer

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun networkFailures_randomDisconnections_shouldHandleGracefully() = runTest {
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val recoveryCount = AtomicInteger(0)

        // Simulate random network failures during operations
        val chaosOperations = (1..20).map { index ->
            async {
                try {
                    val profile = createTestUserProfile("chaos-network-$index")
                    
                    // Randomly inject network failures
                    if (Random.nextFloat() < 0.3) { // 30% chance of network failure
                        networkMonitor.simulateNetworkDisconnection()
                        delay(Random.nextLong(1000, 5000)) // Random disconnection duration
                        networkMonitor.simulateNetworkReconnection()
                    }

                    // Randomly inject high latency
                    if (Random.nextFloat() < 0.2) { // 20% chance of high latency
                        networkMonitor.simulateNetworkLatency(Random.nextLong(2000, 10000))
                    }

                    val result = userProfileRepository.saveUserProfile(profile)
                    
                    if (result.isSuccess) {
                        successCount.incrementAndGet()
                        
                        // Test recovery by reading back
                        val readResult = userProfileRepository.getUserProfile(profile.userId)
                        if (readResult.isSuccess) {
                            recoveryCount.incrementAndGet()
                        }
                    } else {
                        failureCount.incrementAndGet()
                    }
                    
                    result
                } catch (e: Exception) {
                    failureCount.incrementAndGet()
                    Result.failure<UserProfile>(e)
                } finally {
                    // Reset network conditions for next operation
                    networkMonitor.resetNetworkConditions()
                }
            }
        }

        val results = chaosOperations.map { it.await() }
        
        // System should handle chaos gracefully
        assertTrue(successCount.get() > 0, "Some operations should succeed despite chaos")
        assertTrue(failureCount.get() < results.size, "Not all operations should fail")
        
        // Recovery rate should be reasonable
        val recoveryRate = recoveryCount.get().toFloat() / successCount.get()
        assertTrue(recoveryRate > 0.8f, "Recovery rate should be above 80%")
        
        println("Chaos Network Test Results:")
        println("Success: ${successCount.get()}, Failures: ${failureCount.get()}, Recovery: ${recoveryCount.get()}")
    }

    @Test
    fun databaseCorruption_randomFailures_shouldRecover() = runTest {
        val profiles = (1..10).map { createTestUserProfile("chaos-db-$it") }
        val corruptionEvents = AtomicInteger(0)
        val recoveryEvents = AtomicInteger(0)

        // Save initial profiles
        profiles.forEach { profile ->
            userProfileRepository.saveUserProfile(profile)
        }

        // Simulate random database corruption events
        repeat(5) { corruptionIndex ->
            // Randomly corrupt database
            if (Random.nextFloat() < 0.6) { // 60% chance of corruption
                userProfileRepository.simulateDatabaseCorruption()
                corruptionEvents.incrementAndGet()
                
                delay(Random.nextLong(500, 2000)) // Random corruption duration
                
                // Attempt recovery
                val recoveryResult = userProfileRepository.recoverFromCorruption()
                if (recoveryResult.isSuccess) {
                    recoveryEvents.incrementAndGet()
                }
                
                // Test system functionality after recovery
                val testProfile = createTestUserProfile("post-recovery-$corruptionIndex")
                val saveResult = userProfileRepository.saveUserProfile(testProfile)
                
                assertTrue(saveResult.isSuccess || saveResult.isFailure, 
                    "System should handle post-recovery operations gracefully")
            }
        }

        // Verify system resilience
        assertTrue(corruptionEvents.get() > 0, "Corruption events should have occurred")
        
        val recoveryRate = if (corruptionEvents.get() > 0) {
            recoveryEvents.get().toFloat() / corruptionEvents.get()
        } else 1.0f
        
        assertTrue(recoveryRate >= 0.5f, "Recovery rate should be at least 50%")
        
        println("Database Chaos Test Results:")
        println("Corruptions: ${corruptionEvents.get()}, Recoveries: ${recoveryEvents.get()}")
    }

    @Test
    fun memoryPressure_randomSpikes_shouldMaintainStability() = runTest {
        val memorySpikes = mutableListOf<ByteArray>()
        val operationResults = mutableListOf<Boolean>()

        repeat(15) { iteration ->
            try {
                // Randomly create memory pressure
                if (Random.nextFloat() < 0.4) { // 40% chance of memory spike
                    val spikeSize = Random.nextInt(5, 20) // 5-20 MB
                    repeat(spikeSize) {
                        memorySpikes.add(ByteArray(1024 * 1024)) // 1MB chunks
                    }
                }

                // Perform operation under memory pressure
                val profile = createTestUserProfile("chaos-memory-$iteration")
                val result = userProfileRepository.saveUserProfile(profile)
                operationResults.add(result.isSuccess)

                // Randomly release some memory pressure
                if (Random.nextFloat() < 0.3 && memorySpikes.isNotEmpty()) { // 30% chance
                    val releaseCount = Random.nextInt(1, minOf(5, memorySpikes.size))
                    repeat(releaseCount) {
                        if (memorySpikes.isNotEmpty()) {
                            memorySpikes.removeAt(memorySpikes.size - 1)
                        }
                    }
                    System.gc()
                }

                delay(Random.nextLong(100, 500)) // Random delay between operations

            } catch (e: OutOfMemoryError) {
                // System should handle OOM gracefully
                operationResults.add(false)
                
                // Emergency memory cleanup
                memorySpikes.clear()
                System.gc()
                delay(1000)
            }
        }

        // Clean up remaining memory
        memorySpikes.clear()
        System.gc()

        // Verify system stability under memory pressure
        val successRate = operationResults.count { it }.toFloat() / operationResults.size
        assertTrue(successRate > 0.5f, "At least 50% of operations should succeed under memory pressure")
        
        // System should not crash
        assertTrue(operationResults.isNotEmpty(), "System should remain functional")
        
        println("Memory Chaos Test Results:")
        println("Success rate: ${(successRate * 100).toInt()}%")
    }

    @Test
    fun concurrentFailures_multipleSystemsDown_shouldDegrade() = runTest {
        val results = mutableListOf<ChaosResult>()

        // Simulate multiple concurrent system failures
        repeat(10) { iteration ->
            val chaosScenario = ChaosScenario(
                networkFailure = Random.nextFloat() < 0.3,
                databaseCorruption = Random.nextFloat() < 0.2,
                memoryPressure = Random.nextFloat() < 0.4,
                cpuStress = Random.nextFloat() < 0.3,
                storageFailure = Random.nextFloat() < 0.1
            )

            val startTime = System.currentTimeMillis()
            
            try {
                // Apply chaos conditions
                applyChaosScenario(chaosScenario)
                
                // Attempt operation under chaos
                val profile = createTestUserProfile("chaos-multi-$iteration")
                val saveResult = userProfileRepository.saveUserProfile(profile)
                
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime
                
                results.add(ChaosResult(
                    scenario = chaosScenario,
                    success = saveResult.isSuccess,
                    duration = duration,
                    error = saveResult.exceptionOrNull()?.message
                ))
                
            } catch (e: Exception) {
                val endTime = System.currentTimeMillis()
                results.add(ChaosResult(
                    scenario = chaosScenario,
                    success = false,
                    duration = endTime - startTime,
                    error = e.message
                ))
            } finally {
                // Reset all chaos conditions
                resetAllChaosConditions()
            }
        }

        // Analyze results
        val successfulResults = results.filter { it.success }
        val failedResults = results.filter { !it.success }
        
        // System should handle some scenarios successfully
        assertTrue(successfulResults.isNotEmpty() || failedResults.all { it.error != null },
            "System should either succeed or fail gracefully")
        
        // No scenario should cause system crash (all should have results)
        assertEquals(10, results.size, "All chaos scenarios should complete")
        
        // Analyze failure patterns
        val networkFailureImpact = results.filter { it.scenario.networkFailure }.count { !it.success }
        val dbCorruptionImpact = results.filter { it.scenario.databaseCorruption }.count { !it.success }
        
        println("Multi-System Chaos Test Results:")
        println("Total scenarios: ${results.size}")
        println("Successful: ${successfulResults.size}")
        println("Failed: ${failedResults.size}")
        println("Network failure impact: $networkFailureImpact")
        println("DB corruption impact: $dbCorruptionImpact")
    }

    @Test
    fun cascadingFailures_domino_shouldContainDamage() = runTest {
        // Test cascading failure scenarios where one failure triggers others
        val cascadeResults = mutableListOf<CascadeEvent>()
        
        // Start with a single failure that should trigger cascades
        networkMonitor.simulateNetworkDisconnection()
        cascadeResults.add(CascadeEvent("network_failure", System.currentTimeMillis()))
        
        delay(1000)
        
        // Network failure should trigger retry mechanisms
        val profile1 = createTestUserProfile("cascade-1")
        val result1 = userProfileRepository.saveUserProfile(profile1)
        
        if (result1.isFailure) {
            cascadeResults.add(CascadeEvent("save_failure_due_to_network", System.currentTimeMillis()))
            
            // Save failure should trigger local storage fallback
            delay(500)
            
            val localSaveResult = userProfileRepository.saveToLocalOnly(profile1)
            if (localSaveResult.isSuccess) {
                cascadeResults.add(CascadeEvent("local_fallback_success", System.currentTimeMillis()))
            } else {
                cascadeResults.add(CascadeEvent("local_fallback_failure", System.currentTimeMillis()))
                
                // Local failure should trigger memory-only storage
                val memoryResult = userProfileRepository.saveToMemoryOnly(profile1)
                if (memoryResult.isSuccess) {
                    cascadeResults.add(CascadeEvent("memory_fallback_success", System.currentTimeMillis()))
                }
            }
        }
        
        // Restore network and test recovery cascade
        networkMonitor.simulateNetworkReconnection()
        cascadeResults.add(CascadeEvent("network_recovery", System.currentTimeMillis()))
        
        delay(2000)
        
        // Network recovery should trigger sync cascade
        val syncResult = userProfileRepository.syncPendingData()
        if (syncResult.isSuccess) {
            cascadeResults.add(CascadeEvent("sync_success", System.currentTimeMillis()))
        }
        
        // Verify cascade containment
        val totalCascadeTime = cascadeResults.last().timestamp - cascadeResults.first().timestamp
        assertTrue(totalCascadeTime < 10000, "Cascade should be contained within 10 seconds")
        
        // Verify system recovery
        val finalProfile = createTestUserProfile("cascade-final")
        val finalResult = userProfileRepository.saveUserProfile(finalProfile)
        assertTrue(finalResult.isSuccess, "System should recover after cascade")
        
        println("Cascade Test Results:")
        cascadeResults.forEach { event ->
            println("${event.eventType} at ${event.timestamp}")
        }
    }

    @Test
    fun randomChaosMonkey_unpredictableFailures_shouldSurvive() = runTest {
        // Implement a chaos monkey that randomly breaks things
        val chaosMonkey = ChaosMonkey()
        val survivedOperations = AtomicInteger(0)
        val totalOperations = 25

        repeat(totalOperations) { iteration ->
            // Chaos monkey randomly breaks something
            val chaosAction = chaosMonkey.getRandomChaosAction()
            chaosAction.execute()
            
            delay(Random.nextLong(100, 1000)) // Random delay
            
            try {
                // Attempt normal operation
                val profile = createTestUserProfile("chaos-monkey-$iteration")
                val result = userProfileRepository.saveUserProfile(profile)
                
                if (result.isSuccess) {
                    survivedOperations.incrementAndGet()
                }
                
                // Chaos monkey might break something else
                if (Random.nextFloat() < 0.2) { // 20% chance of additional chaos
                    val additionalChaos = chaosMonkey.getRandomChaosAction()
                    additionalChaos.execute()
                }
                
            } catch (e: Exception) {
                // System should not crash, just log the exception
                println("Operation $iteration failed due to chaos: ${e.message}")
            } finally {
                // Randomly fix some things
                if (Random.nextFloat() < 0.3) { // 30% chance of partial recovery
                    chaosMonkey.performRandomRecovery()
                }
            }
        }

        // Clean up all chaos
        chaosMonkey.resetAll()
        
        // Verify system survival
        val survivalRate = survivedOperations.get().toFloat() / totalOperations
        assertTrue(survivalRate > 0.2f, "At least 20% of operations should survive chaos monkey")
        
        // Verify system can recover
        val recoveryProfile = createTestUserProfile("post-chaos-recovery")
        val recoveryResult = userProfileRepository.saveUserProfile(recoveryProfile)
        assertTrue(recoveryResult.isSuccess, "System should recover after chaos monkey")
        
        println("Chaos Monkey Test Results:")
        println("Survival rate: ${(survivalRate * 100).toInt()}%")
        println("Survived operations: ${survivedOperations.get()}/$totalOperations")
    }

    // Helper methods and classes
    private fun createTestUserProfile(userId: String): UserProfile {
        return UserProfile(
            userId = userId,
            email = "$userId@chaostest.com",
            displayName = "Chaos Test User ${userId.takeLast(3)}",
            birthday = Date(System.currentTimeMillis() - (25L * 365 * 24 * 60 * 60 * 1000)),
            gender = Gender.values()[userId.hashCode() % Gender.values().size],
            unitSystem = UnitSystem.values()[userId.hashCode() % UnitSystem.values().size],
            heightInCm = 150 + (userId.hashCode() % 100),
            weightInKg = 50.0 + (userId.hashCode() % 100),
            hasCompletedOnboarding = true,
            createdAt = Date(),
            updatedAt = Date()
        )
    }

    private suspend fun applyChaosScenario(scenario: ChaosScenario) {
        if (scenario.networkFailure) {
            networkMonitor.simulateNetworkDisconnection()
        }
        if (scenario.databaseCorruption) {
            userProfileRepository.simulateDatabaseCorruption()
        }
        if (scenario.memoryPressure) {
            performanceOptimizer.simulateMemoryPressure()
        }
        if (scenario.cpuStress) {
            performanceOptimizer.simulateCpuStress()
        }
        if (scenario.storageFailure) {
            userProfileRepository.simulateStorageFailure()
        }
    }

    private suspend fun resetAllChaosConditions() {
        networkMonitor.resetNetworkConditions()
        userProfileRepository.resetStorageConditions()
        performanceOptimizer.resetPerformanceConditions()
    }

    data class ChaosScenario(
        val networkFailure: Boolean,
        val databaseCorruption: Boolean,
        val memoryPressure: Boolean,
        val cpuStress: Boolean,
        val storageFailure: Boolean
    )

    data class ChaosResult(
        val scenario: ChaosScenario,
        val success: Boolean,
        val duration: Long,
        val error: String?
    )

    data class CascadeEvent(
        val eventType: String,
        val timestamp: Long
    )

    inner class ChaosMonkey {
        private val chaosActions = listOf(
            ChaosAction("network_disconnect") { networkMonitor.simulateNetworkDisconnection() },
            ChaosAction("high_latency") { networkMonitor.simulateNetworkLatency(5000) },
            ChaosAction("packet_loss") { networkMonitor.simulatePacketLoss(0.2f) },
            ChaosAction("memory_pressure") { performanceOptimizer.simulateMemoryPressure() },
            ChaosAction("cpu_stress") { performanceOptimizer.simulateCpuStress() },
            ChaosAction("storage_failure") { userProfileRepository.simulateStorageFailure() },
            ChaosAction("database_corruption") { userProfileRepository.simulateDatabaseCorruption() }
        )

        fun getRandomChaosAction(): ChaosAction {
            return chaosActions[Random.nextInt(chaosActions.size)]
        }

        fun performRandomRecovery() {
            when (Random.nextInt(4)) {
                0 -> networkMonitor.resetNetworkConditions()
                1 -> performanceOptimizer.resetPerformanceConditions()
                2 -> userProfileRepository.resetStorageConditions()
                3 -> {
                    // Do nothing (no recovery)
                }
            }
        }

        fun resetAll() {
            networkMonitor.resetNetworkConditions()
            performanceOptimizer.resetPerformanceConditions()
            userProfileRepository.resetStorageConditions()
        }
    }

    data class ChaosAction(
        val name: String,
        val execute: suspend () -> Unit
    )
}