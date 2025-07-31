package com.vibehealth.android.di

import com.vibehealth.android.data.dashboard.DashboardRepository
import com.vibehealth.android.data.dashboard.DashboardRepositoryImpl
import com.vibehealth.android.domain.dashboard.DashboardUseCase
import com.vibehealth.android.domain.dashboard.DashboardUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for dashboard-related components.
 * Provides dashboard use cases and repositories.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DashboardModule {
    
    @Binds
    @Singleton
    abstract fun bindDashboardUseCase(
        dashboardUseCaseImpl: DashboardUseCaseImpl
    ): DashboardUseCase
    
    @Binds
    @Singleton
    abstract fun bindDashboardRepository(
        dashboardRepositoryImpl: DashboardRepositoryImpl
    ): DashboardRepository
}