package com.vibehealth.android.core.session

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vibehealth.android.data.auth.SessionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker to handle session refresh and timeout
 */
@HiltWorker
class SessionRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sessionManager: SessionManager
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            // Check and handle session timeout
            sessionManager.handleSessionTimeout()
            
            // Refresh session if user is still logged in
            if (sessionManager.isUserLoggedIn()) {
                sessionManager.refreshSession()
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    companion object {
        const val WORK_NAME = "session_refresh_work"
    }
}