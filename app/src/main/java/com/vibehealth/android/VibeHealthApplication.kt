package com.vibehealth.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vibehealth.android.core.session.SessionRefreshWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class VibeHealthApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        scheduleSessionRefresh()
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    /**
     * Schedule periodic session refresh to handle timeouts
     */
    private fun scheduleSessionRefresh() {
        val sessionRefreshRequest = PeriodicWorkRequestBuilder<SessionRefreshWorker>(
            repeatInterval = 1, // Check every hour
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SessionRefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            sessionRefreshRequest
        )
    }
}