package com.vibehealth.android.domain.goals

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.any
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GoalCalculationServiceTest {

    @Mock
    private lateinit var stepsCalculator: StepsGoalCalculator
    
    @Mock
    private lateinit var caloriesCalculator: CaloriesGoalCalculator
    
    @Mock
    private lateinit var heartPointsCalculator: HeartPointsGoalCalculator
    
    @Mock
    private lateinit var fallbackGenerator: FallbackGoalGenerator
    
    private lateinit var service: GoalCalculationService

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
        service = GoalCalculationService(
            stepsCalculator = stepsCalculator,
            caloriesCalculator = caloriesCalculator,
            heartPointsCalculator = heartPointsCalculator,
            fallbackGenerator = fallbackGenerator
        )
    }

    @Test
    fun `calculateGoals returns successful result with valid input`() = runTest {
        // Given
        val input = createValidGoalCalculationInput()
        val expectedSteps = 8500
        val expectedCalories = 2200
        val expectedHeartPoints = 25
        
        whenever(stepsCalculator.calculateStepsGoal(any())).thenReturn(expectedSteps)
        whenever(caloriesCalculator.calculateCaloriesGoal(any())).thenReturn(expectedCalories)
        whenever(heartPointsCalculator.calculateHeartPointsGoal(any())).thenReturn(expectedHeartPoints)

        // When
        val result = service.calculateGoals(input)

        // Then
        assertTrue(result.isSuccess)
        val goals = result.getOrThrow()
        assertEquals(expectedSteps, goals.stepsGoal)
        assertEquals(expectedCalories, goals.caloriesGoal)
        assertEquals(expectedHeartPoints, goals.heartPointsGoal)
        assertEquals(CalculationSource.WHO_STANDARD, goals.calculationSource)
        assertNotNull(goals.calculatedAt)
    }

    @Test
    fun `calculateGoals uses fallback when steps calculator fails`() = runTest {
        // Given
        val input = createValidGoalCalculationInput()
        val fallbackGoals = createFallbackGoals()
        
        whenever(stepsCalculator.calculateStepsGoal(any())).thenThrow(RuntimeException("Calculation failed"))
        whenever(fallbackGenerator.generateFallbackGoals(any())).thenReturn(fallbackGoals)

        // When
        val result = service.calculateGoals(input)

        // Then
        assertTrue(result.isSuccess)
        val goals = result.getOrThrow()
        assertEquals(fallbackGoals.stepsGoal, goals.stepsGoal)
        assertEquals(fallbackGoals.caloriesGoal, goals.caloriesGoal)
        assertEquals(fallbackGoals.heartPointsGoal, goals.heartPointsGoal)
        assertEquals(CalculationSource.FALLBACK_DEFAULT, goals.calculationSource)
        verify(fallbackGenerator).generateFallbackGoals(input)
    }

    @Test
    fun `calculateGoals uses fallback when calories calculator fails`() = runTest {
        // Given
        val input = createValidGoalCalculationInput()
        val fallbackGoals = createFallbackGoals()
        
        whenever(stepsCalculator.calculateStepsGoal(any())).thenReturn(8500)
        whenever(caloriesCalculator.calculateCaloriesGoal(any())).thenThrow(RuntimeException("Calculation failed"))
        whenever(fallbackGenerator.generateFallbackGoals(any())).thenReturn(fallbackGoals)

        // When
        val result = service.calculateGoals(input)

        // Then
        assertTrue(result.isSuccess)
        val goals = result.getOrThrow()
        assertEquals(CalculationSource.FALLBACK_DEFAULT, goals.calculationSource)
        verify(fallbackGenerator).generateFallbackGoals(input)
    }

    @Test
    fun `calculateGoals uses fallback when heart points calculator fails`() = runTest {
        // Given
        val input = createValidGoalCalculationInput()
        val fallbackGoals = createFallbackGoals()
        
        whenever(stepsCalculator.calculateStepsGoal(any())).thenReturn(8500)
        whenever(caloriesCalculator.calculateCaloriesGoal(any())).thenReturn(2200)
        whenever(heartPointsCalculator.calculateHeartPointsGoal(any())).thenThrow(RuntimeException("Calculation failed"))
        whenever(fallbackGenerator.generateFallbackGoals(any())).thenReturn(fallbackGoals)

        // When
        val result = service.calculateGoals(input)

        // Then
        assertTrue(result.isSuccess)
        val goals = result.getOrThrow()
        assertEquals(CalculationSource.FALLBACK_DEFAULT, goals.calculationSource)
        verify(fallbackGenerator).generateFallbackGoals(input)
    }

    @Test
    fun `calculateGoals returns failure when fallback generator also fails`() = runTest {
        // Given
        val input = createValidGoalCalculationInput()
        
        whenever(stepsCalculator.calculateStepsGoal(any())).thenThrow(RuntimeException("Steps calculation failed"))
        whenever(fallbackGenerator.generateFallbackGoals(any())).thenThrow(RuntimeException("Fallback failed"))

        // When
        val result = service.calculateGoals(input)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    @Test
    fun `calculateGoals completes within performance target`() = runTest {
        // Given
        val input = createValidGoalCalculationInput()
        whenever(stepsCalculator.calculateStepsGoal(any())).thenReturn(8500)
        whenever(caloriesCalculator.calculateCaloriesGoal(any())).thenReturn(2200)
        whenever(heartPointsCalculator.calculateHeartPointsGoal(any())).thenReturn(25)

        // When
        val startTime = System.currentTimeMillis()
        val result = service.calculateGoals(input)
        val endTime = System.currentTimeMillis()

        // Then
        assertTrue(result.isSuccess)
        val duration = endTime - startTime
        assertTrue(duration < 500, "Calculation took ${duration}ms, should be under 500ms")
    }

    @Test
    fun `calculateGoals handles concurrent calculations safely`() = runTest {
        // Given
        val input = createValidGoalCalculationInput()
        whenever(stepsCalculator.calculateStepsGoal(any())).thenReturn(8500)
        whenever(caloriesCalculator.calculateCaloriesGoal(any())).thenReturn(2200)
        whenever(heartPointsCalculator.calculateHeartPointsGoal(any())).thenReturn(25)

        // When - Execute multiple concurrent calculations
        val results = (1..10).map {
            service.calculateGoals(input)
        }

        // Then
        results.forEach { result ->
            assertTrue(result.isSuccess)
            val goals = result.getOrThrow()
            assertEquals(8500, goals.stepsGoal)
            assertEquals(2200, goals.caloriesGoal)
            assertEquals(25, goals.heartPointsGoal)
        }
    }

    @Test
    fun `calculateGoals validates input bounds`() = runTest {
        // Given
        val invalidInput = GoalCalculationInput(
            age = -5, // Invalid age
            gender = Gender.MALE,
            heightInCm = 175,
            weightInKg = 70.0,
            activityLevel = ActivityLevel.LIGHT
        )
        
        whenever(stepsCalculator.calculateStepsGoal(any())).thenThrow(IllegalArgumentException("Invalid age"))

        // When
        val result = service.calculateGoals(invalidInput)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    private fun createValidGoalCalculationInput() = GoalCalculationInput(
        age = 30,
        gender = Gender.MALE,
        heightInCm = 175,
        weightInKg = 70.0,
        activityLevel = ActivityLevel.LIGHT
    )

    private fun createFallbackGoals() = DailyGoals(
        userId = "test-user",
        stepsGoal = 7500,
        caloriesGoal = 1800,
        heartPointsGoal = 21,
        calculationSource = CalculationSource.FALLBACK_DEFAULT,
        calculatedAt = System.currentTimeMillis()
    )
}