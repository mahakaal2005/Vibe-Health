package com.vibehealth.android.data.progress

import android.util.Log

import com.vibehealth.android.ui.progress.BasicProgressViewModel.ProgressDataResult
import com.vibehealth.android.ui.progress.models.WeeklyProgressData
import com.vibehealth.android.ui.progress.models.DailyProgressData
import com.vibehealth.android.ui.progress.models.SupportiveInsights
import com.vibehealth.android.ui.progress.models.GoalAchievements
import com.vibehealth.android.ui.progress.models.WeeklyTotals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BasicProgressRepository - Simplified version for progress history
 * 
 * This repository provides progress-specific data access with sample data
 * for testing and development purposes.
 */
@Singleton
class BasicProgressRepository @Inject constructor() {
    
    companion object {
        private const val TAG = "VIBE_FIX_REPOSITORY"
    }
    
    init {
        Log.d(TAG, "VIBE_FIX: BasicProgressRepository created successfully")
    }
    
    /**
     * Gets weekly progress data with supportive context and encouraging messaging
     * Simplified to work with existing dependencies only
     */
    fun getWeeklyProgressWithSupportiveContext(): Flow<ProgressDataResult> = flow {
        Log.d(TAG, "VIBE_FIX: getWeeklyProgressWithSupportiveContext() called")
        try {
            Log.d(TAG, "VIBE_FIX: About to create sample weekly data")
            // Create sample weekly data for now
            val weeklyData = createSampleWeeklyData()
            Log.d(TAG, "VIBE_FIX: Sample weekly data created successfully - hasAnyData: ${weeklyData.hasAnyData}")
            
            if (weeklyData.hasAnyData) {
                Log.d(TAG, "VIBE_FIX: Emitting Success result")
                emit(ProgressDataResult.Success(
                    data = weeklyData,
                    supportiveMessage = "Your progress is up to date! Keep up the great work on your wellness journey."
                ))
                Log.d(TAG, "VIBE_FIX: Success result emitted successfully")
            } else {
                Log.d(TAG, "VIBE_FIX: Emitting EmptyState result")
                // Handle empty state with encouraging messaging
                emit(ProgressDataResult.EmptyState(
                    encouragingMessage = "Your wellness journey is just beginning! Every step counts, and we're here to celebrate your progress."
                ))
                Log.d(TAG, "VIBE_FIX: EmptyState result emitted successfully")
            }
            
        } catch (exception: Exception) {
            Log.e(TAG, "VIBE_FIX: Exception in getWeeklyProgressWithSupportiveContext()", exception)
            throw exception // Re-throw for ViewModel error handling
        }
    }.catch { exception ->
        Log.e(TAG, "VIBE_FIX: Flow catch block - handling error", exception)
        // Handle errors with supportive messaging
        emit(ProgressDataResult.Error(
            exception = exception,
            supportiveMessage = "We're having trouble loading your progress right now, but your data is safe. Please try again in a moment."
        ))
        Log.d(TAG, "VIBE_FIX: Error result emitted successfully")
    }
    
    /**
     * Creates sample weekly progress data for testing
     */
    private fun createSampleWeeklyData(): WeeklyProgressData {
        Log.d(TAG, "VIBE_FIX: createSampleWeeklyData() started")
        try {
            val weekStartDate = LocalDate.now().minusDays(6)
            Log.d(TAG, "VIBE_FIX: Week start date calculated: $weekStartDate")
            
            val dailyDataList = mutableListOf<DailyProgressData>()
            Log.d(TAG, "VIBE_FIX: Daily data list initialized")
            
            // Create sample data for each day of the week
            val sampleStepsData = listOf(6500, 8200, 7800, 9100, 10500, 8900, 7600)
            val sampleCaloriesData = listOf(1800.0, 2100.0, 1950.0, 2200.0, 2400.0, 2050.0, 1900.0)
            val sampleHeartPointsData = listOf(18, 25, 22, 28, 35, 26, 20)
            Log.d(TAG, "VIBE_FIX: Sample data arrays created")
        
        for (i in 0..6) {
            val date = weekStartDate.plusDays(i.toLong())
            val steps = sampleStepsData[i]
            val calories = sampleCaloriesData[i]
            val heartPoints = sampleHeartPointsData[i]
            
            val goalAchievements = GoalAchievements(
                stepsGoalAchieved = steps >= 10000,
                caloriesGoalAchieved = calories >= 2000,
                heartPointsGoalAchieved = heartPoints >= 30
            )
            
            dailyDataList.add(DailyProgressData(
                date = date,
                steps = steps,
                calories = calories,
                heartPoints = heartPoints,
                goalAchievements = goalAchievements,
                supportiveContext = "Great progress on ${date.dayOfWeek}!"
            ))
        }
        
        val weeklyTotals = WeeklyTotals(
            totalSteps = sampleStepsData.sum(),
            totalCalories = sampleCaloriesData.sum(),
            totalHeartPoints = sampleHeartPointsData.sum(),
            activeDays = 7,
            averageStepsPerDay = sampleStepsData.average().toInt(),
            averageCaloriesPerDay = sampleCaloriesData.average(),
            averageHeartPointsPerDay = sampleHeartPointsData.average().toInt(),
            supportiveWeeklySummary = "Amazing week of activity!"
        )
        
        val supportiveInsights = SupportiveInsights(
            weeklyTrends = emptyList(),
            achievements = emptyList(),
            gentleGuidance = emptyList(),
            wellnessJourneyContext = "Your wellness journey is inspiring!",
            motivationalMessage = "Keep up the great work!"
        )
        
            return WeeklyProgressData(
                weekStartDate = weekStartDate,
                dailyData = dailyDataList,
                weeklyTotals = weeklyTotals,
                supportiveInsights = supportiveInsights,
                celebratoryMessages = listOf("Great job!", "Keep it up!")
            )
        } catch (exception: Exception) {
            Log.e(TAG, "VIBE_FIX: Exception in createSampleWeeklyData()", exception)
            throw exception
        }
    }
}