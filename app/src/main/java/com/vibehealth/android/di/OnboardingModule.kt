package com.vibehealth.android.di

import com.vibehealth.android.core.integration.DefaultGoalCalculationIntegration
import com.vibehealth.android.core.integration.GoalCalculationIntegration
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for onboarding-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class OnboardingModule {

    /**
     * Provide goal calculation integration implementation
     */
    @Binds
    @Singleton
    abstract fun bindGoalCalculationIntegration(
        implementation: DefaultGoalCalculationIntegration
    ): GoalCalculationIntegration
}