package com.vibehealth.android.di

import com.vibehealth.android.domain.goals.CaloriesGoalCalculator
import com.vibehealth.android.domain.goals.FallbackGoalGenerator
import com.vibehealth.android.domain.goals.GoalCalculationService
import com.vibehealth.android.domain.goals.HeartPointsGoalCalculator
import com.vibehealth.android.domain.goals.StepsGoalCalculator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for goal calculation dependencies.
 * 
 * Provides dependency injection for WHO-based goal calculation components
 * following the established DI patterns in the application.
 */
@Module
@InstallIn(SingletonComponent::class)
object GoalsModule {
    
    /**
     * Provides StepsGoalCalculator as a singleton.
     * 
     * The calculator is stateless and thread-safe, making it suitable
     * for singleton scope to optimize performance and memory usage.
     */
    @Provides
    @Singleton
    fun provideStepsGoalCalculator(): StepsGoalCalculator {
        return StepsGoalCalculator()
    }
    
    /**
     * Provides CaloriesGoalCalculator as a singleton.
     * 
     * Uses Harris-Benedict Revised (1984) and Mifflin-St Jeor equations
     * for accurate BMR calculations with activity level adjustments.
     */
    @Provides
    @Singleton
    fun provideCaloriesGoalCalculator(): CaloriesGoalCalculator {
        return CaloriesGoalCalculator()
    }
    
    /**
     * Provides HeartPointsGoalCalculator as a singleton.
     * 
     * Converts WHO 150 min/week moderate activity to daily heart points
     * using Google Fit standard and METs values.
     */
    @Provides
    @Singleton
    fun provideHeartPointsGoalCalculator(): HeartPointsGoalCalculator {
        return HeartPointsGoalCalculator()
    }
    
    /**
     * Provides FallbackGoalGenerator as a singleton.
     * 
     * Generates medically-safe fallback goals when WHO-based calculation fails,
     * with age and gender adjustments based on available profile data.
     */
    @Provides
    @Singleton
    fun provideFallbackGoalGenerator(): FallbackGoalGenerator {
        return FallbackGoalGenerator()
    }
    
    /**
     * Provides GoalCalculationService as a singleton.
     * 
     * Orchestrates all three calculators and provides error handling
     * with fallback mechanisms for robust goal calculation.
     */
    @Provides
    @Singleton
    fun provideGoalCalculationService(
        stepsGoalCalculator: StepsGoalCalculator,
        caloriesGoalCalculator: CaloriesGoalCalculator,
        heartPointsGoalCalculator: HeartPointsGoalCalculator,
        fallbackGoalGenerator: FallbackGoalGenerator
    ): GoalCalculationService {
        return GoalCalculationService(
            stepsGoalCalculator,
            caloriesGoalCalculator,
            heartPointsGoalCalculator,
            fallbackGoalGenerator
        )
    }
}