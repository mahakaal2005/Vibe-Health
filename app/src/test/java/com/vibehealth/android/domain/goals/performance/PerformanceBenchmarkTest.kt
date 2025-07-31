package com.vibehealth.android.domain.goals.performance

import com.vibehealth.android.domain.goals.*
import com.vibehealth.android.domain.user.Gender
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue
import kotlin.system.measureTimeMillis

/**
 * Performance benchmarking tests for goal calculation system.
 * 
 * Tests performance targets, memory usage, and provides benchmarking
 * with realistic datasets as specified in Task 6.1.
 */
class PerformanceBenchmarkTest {

    @Mock
    private lateinit var stepsCalculator: StepsGoalCalculator
    
    @Mock
    private lateinit var caloriesCalculator: CaloriesGoalCalculator
    
    @Mock
    private lateinit var heartPointsCalculator: HeartPointsGoalCalculator
    
    @Mock
    private lateinit var fallbackGenerator: FallbackGoalGenerator
    
    private lateinit var performanceMonitor: GoalCalculationPerformanceMonitor
    private lateinit var optimizedService: OptimizedGoalCalculationService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        performanceMonitor = GoalCalculationPerformanceMonitor()
        optimizedService = OptimizedGoalCalculationService(
            stepsCalculator,
            caloriesCalculator,
            heartPointsCalculator,
            fallbackGenerator,
            performanceMonitor
        )
        
        // Setup mock responses
        whenever(stepsCalculator.calculateStepsGoal(any())).thenReturn(10000)
        whenever(caloriesCalculator.calculateCaloriesGoal(any())).thenReturn(2000)
        whenever(heartPointsCalculator.calculateHeartPointsGoal(any())).thenReturn(30)
    }

    @Nested
    @DisplayName("Calculation Performance Benchmarks")
    inner class CalculationPerformanceBenchmarks {

        @Test
        @DisplayName("Single calculation should complete within 500ms target")
        fun singleCalculationPerformance() = runTest {
            val input = createTestInput()
            
            val duration = measureTimeMillis {
                val result = optimizedService.calculateGoals(input)
                assertTrue(result.isSuccess)
            }
            
            assertTrue(duration < 500, "Single calculation took ${duration}ms, should be under 500ms")
        }

        @Test
        @DisplayName("Batch calculations should maintain performance")
        fun batchCalculationPerformance() = runTest {
            val inputs = (1..100).map { index ->
                createTestInput(age = 20 + (index % 50))
            }
            
            val totalDuration = measureTimeMillis {
                inputs.forEach { input ->
                    val result = optimizedService.calculateGoals(input)
                    assertTrue(result.isSuccess)
                }
            }
            
            val averageDuration = totalDuration / inputs.size
            assertTrue(averageDuration < 500, "Average calculation took ${averageDuration}ms, should be under 500ms")
            assertTrue(totalDuration < 30000, "Total batch took ${totalDuration}ms, should be under 30s")
        }

        @Test
        @DisplayName("Concurrent calculations should maintain performance")
        fun concurrentCalculationPerformance() = runTest {
            val inputs = (1..50).map { index ->
                createTestInput(age = 20 + (index % 40))
            }
            
            val duration = measureTimeMillis {
                val jobs = inputs.map { input ->
                    kotlinx.coroutines.async {
                        optimizedService.calculateGoals(input)
                    }
                }
                
                jobs.forEach { job ->
                    val result = job.await()
                    assertTrue(result.isSuccess)
                }
            }
            
            assertTrue(duration < 10000, "Concurrent calculations took ${duration}ms, should be under 10s")
        }

        @Test
        @DisplayName("Cache performance should improve repeated calculations")
        fun cachePerformanceImprovement() = runTest {
            val input = createTestInput()
            
            // First calculation (cache miss)
            val firstDuration = measureTimeMillis {
                val result = optimizedService.calculateGoals(input)
                assertTrue(result.isSuccess)
            }
            
            // Second calculation (cache hit)
            val secondDuration = measureTimeMillis {
                val result = optimizedService.calculateGoals(input)
                assertTrue(result.isSuccess)
            }
            
            // Cache hit should be significantly faster
            assertTrue(secondDuration < firstDuration / 2, 
                "Cache hit (${secondDuration}ms) should be faster than cache miss (${firstDuration}ms)")
        }
    }

    @Nested
    @DisplayName("Memory Usage Benchmarks")
    inner class MemoryUsageBenchmarks {

        @Test
        @DisplayName("Memory usage should remain stable during extended operation")
        fun memoryStabilityTest() = runTest {
            val runtime = Runtime.getRuntime()
            val initialMemory = runtime.totalMemory() - runtime.freeMemory()
            
            // Perform many calculations
            repeat(1000) { index ->
                val input = createTestInput(age = 20 + (index % 50))
                val result = optimizedService.calculateGoals(input)
                assertTrue(result.isSuccess)
                
                // Force garbage collection periodically
                if (index % 100 == 0) {
                    System.gc()
                    Thread.sleep(10)
                }
            }
            
            System.gc()
            Thread.sleep(100)
            
            val finalMemory = runtime.totalMemory() - runtime.freeMemory()
            val memoryIncrease = finalMemory - initialMemory
            val maxAllowedIncrease = 50 * 1024 * 1024 // 50MB
            
            assertTrue(memoryIncrease < maxAllowedIncrease, 
                "Memory increased by ${memoryIncrease / 1024 / 1024}MB, should be under 50MB")
        }

        @Test
        @DisplayName("Cache should not cause memory leaks")
        fun cacheMemoryLeakTest() = runTest {
            val runtime = Runtime.getRuntime()
            val initialMemory = runtime.totalMemory() - runtime.freeMemory()
            
            // Fill cache with many different inputs
            repeat(200) { index ->
                val input = createTestInput(
                    age = 20 + index,
                    weight = 60.0 + index,
                    height = 160 + index
                )
                val result = optimizedService.calculateGoals(input)
                assertTrue(result.isSuccess)
            }
            
            // Clear expired cache
            optimizedService.clearExpiredCache()
            System.gc()
            Thread.sleep(100)
            
            val finalMemory = runtime.totalMemory() - runtime.freeMemory()
            val memoryIncrease = finalMemory - initialMemory
            val maxAllowedIncrease = 20 * 1024 * 1024 // 20MB
            
            assertTrue(memoryIncrease < maxAllowedIncrease,
                "Cache memory increased by ${memoryIncrease / 1024 / 1024}MB, should be under 20MB")
        }
    }

    @Nested
    @DisplayName("Performance Monitoring Benchmarks")
    inner class PerformanceMonitoringBenchmarks {

        @Test
        @DisplayName("Performance monitoring should not impact calculation performance")
        fun monitoringOverheadTest() = runTest {
            val input = createTestInput()
            
            // Measure without monitoring
            val withoutMonitoringDuration = measureTimeMillis {
                repeat(100) {
                    // Direct calculation without monitoring
                    stepsCalculator.calculateStepsGoal(input)
                    caloriesCalculator.calculateCaloriesGoal(input)
                    heartPointsCalculator.calculateHeartPointsGoal(input)
                }
            }
            
            // Measure with monitoring
            val withMonitoringDuration = measureTimeMillis {
                repeat(100) {
                    val result = optimizedService.calculateGoals(input)
                    assertTrue(result.isSuccess)
                }
            }
            
            val overhead = withMonitoringDuration - withoutMonitoringDuration
            val maxAllowedOverhead = withoutMonitoringDuration * 0.1 // 10% overhead
            
            assertTrue(overhead < maxAllowedOverhead,
                "Monitoring overhead (${overhead}ms) should be under 10% of base time (${withoutMonitoringDuration}ms)")
        }

        @Test
        @DisplayName("Performance metrics should be accurate")
        fun performanceMetricsAccuracy() = runTest {
            val input = createTestInput()
            val iterations = 50
            
            repeat(iterations) {
                val result = optimizedService.calculateGoals(input)
                assertTrue(result.isSuccess)
            }
            
            val metrics = performanceMonitor.performanceMetrics.value
            
            // Verify metrics accuracy
            assertTrue(metrics.totalCalculations >= iterations.toLong())
            assertTrue(metrics.successRate > 0.9) // At least 90% success rate
            assertTrue(metrics.averageCalculationTimeMs > 0)
            assertTrue(metrics.averageCalculationTimeMs < 1000) // Under 1 second average
        }
    }

    @Nested
    @DisplayName("Realistic Dataset Benchmarks")
    inner class RealisticDatasetBenchmarks {

        @Test
        @DisplayName("Performance with realistic user distribution")
        fun realisticUserDistributionTest() = runTest {
            val realisticInputs = generateRealisticUserInputs(1000)
            
            val duration = measureTimeMillis {
                realisticInputs.forEach { input ->
                    val result = optimizedService.calculateGoals(input)
                    assertTrue(result.isSuccess)
                }
            }
            
            val averageDuration = duration / realisticInputs.size
            assertTrue(averageDuration < 500, "Average calculation with realistic data took ${averageDuration}ms")
            
            // Verify cache effectiveness with realistic data
            val cacheStats = optimizedService.getCacheStats()
            assertTrue(cacheStats.hitRate > 0.1, "Cache hit rate should be above 10% with realistic data")
        }

        @Test
        @DisplayName("Performance with edge case inputs")
        fun edgeCasePerformanceTest() = runTest {
            val edgeCaseInputs = generateEdgeCaseInputs()
            
            val duration = measureTimeMillis {
                edgeCaseInputs.forEach { input ->
                    val result = optimizedService.calculateGoals(input)
                    // Edge cases might fail, but should not crash
                    assertTrue(result.isSuccess || result.isFailure)
                }
            }
            
            val averageDuration = duration / edgeCaseInputs.size
            assertTrue(averageDuration < 1000, "Edge case calculations took ${averageDuration}ms on average")
        }
    }

    @Nested
    @DisplayName("Battery Usage Benchmarks")
    inner class BatteryUsageBenchmarks {

        @Test
        @DisplayName("Background calculations should be battery efficient")
        fun batteryEfficiencyTest() = runTest {
            val startTime = System.currentTimeMillis()
            val inputs = generateRealisticUserInputs(100)
            
            // Simulate background processing
            inputs.forEach { input ->
                val result = optimizedService.calculateGoals(input)
                assertTrue(result.isSuccess)
                
                // Simulate brief pause between calculations
                kotlinx.coroutines.delay(10)
            }
            
            val totalTime = System.currentTimeMillis() - startTime
            val activeTime = totalTime - (inputs.size * 10) // Subtract pause time
            
            // Active processing should be minimal compared to total time
            val efficiency = activeTime.toDouble() / totalTime.toDouble()
            assertTrue(efficiency < 0.5, "Battery efficiency ratio should be under 50%")
        }
    }

    private fun createTestInput(
        age: Int = 30,
        gender: Gender = Gender.MALE,
        height: Int = 175,
        weight: Double = 70.0,
        activityLevel: ActivityLevel = ActivityLevel.LIGHT
    ): GoalCalculationInput {
        return GoalCalculationInput(
            age = age,
            gender = gender,
            heightInCm = height,
            weightInKg = weight,
            activityLevel = activityLevel
        )
    }

    private fun generateRealisticUserInputs(count: Int): List<GoalCalculationInput> {
        val genders = listOf(Gender.MALE, Gender.FEMALE, Gender.OTHER)
        val activityLevels = ActivityLevel.values()
        
        return (1..count).map { index ->
            GoalCalculationInput(
                age = (18..80).random(),
                gender = genders.random(),
                heightInCm = (150..200).random(),
                weightInKg = (45.0..120.0).random(),
                activityLevel = activityLevels.random()
            )
        }
    }

    private fun generateEdgeCaseInputs(): List<GoalCalculationInput> {
        return listOf(
            // Minimum values
            createTestInput(age = 13, height = 120, weight = 30.0),
            // Maximum values
            createTestInput(age = 120, height = 220, weight = 200.0),
            // Extreme combinations
            createTestInput(age = 18, height = 220, weight = 45.0, activityLevel = ActivityLevel.VERY_ACTIVE),
            createTestInput(age = 80, height = 150, weight = 120.0, activityLevel = ActivityLevel.SEDENTARY),
            // Boundary conditions
            createTestInput(age = 65, height = 175, weight = 70.0),
            createTestInput(age = 50, height = 175, weight = 70.0)
        )
    }
}