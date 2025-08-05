package com.vibehealth.android.di

import com.vibehealth.android.data.discover.ContentCache
import com.vibehealth.android.data.discover.ContentCacheImpl
import com.vibehealth.android.data.discover.HealthContentRepository
import com.vibehealth.android.data.discover.HealthContentRepositoryImpl
import com.vibehealth.android.data.discover.HealthContentService
import com.vibehealth.android.data.discover.HealthContentServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DiscoverModule - Dependency injection for Health Content Discovery Hub
 * Following existing Hilt patterns and dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DiscoverModule {
    
    @Binds
    @Singleton
    abstract fun bindHealthContentRepository(
        healthContentRepositoryImpl: HealthContentRepositoryImpl
    ): HealthContentRepository
    
    @Binds
    @Singleton
    abstract fun bindHealthContentService(
        healthContentServiceImpl: HealthContentServiceImpl
    ): HealthContentService
    
    @Binds
    @Singleton
    abstract fun bindContentCache(
        contentCacheImpl: ContentCacheImpl
    ): ContentCache
}