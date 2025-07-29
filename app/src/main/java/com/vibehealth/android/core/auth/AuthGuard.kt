package com.vibehealth.android.core.auth

import android.content.Context
import android.content.Intent
import com.vibehealth.android.data.auth.SessionManager
import com.vibehealth.android.ui.auth.AuthActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication guard to protect routes and ensure proper authentication flow
 */
@Singleton
class AuthGuard @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) {
    
    /**
     * Check if user is authenticated and redirect if not
     */
    suspend fun requireAuthentication(): Boolean {
        return if (sessionManager.isUserLoggedIn()) {
            true
        } else {
            redirectToAuth()
            false
        }
    }
    
    /**
     * Redirect to authentication flow
     */
    private fun redirectToAuth() {
        val intent = Intent(context, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
    
    /**
     * Check if user has completed onboarding
     */
    suspend fun hasCompletedOnboarding(): Boolean {
        // TODO: Implement onboarding check
        // For now, assume onboarding is always complete
        return true
    }
    
    /**
     * Get authentication redirect intent
     */
    fun getAuthIntent(): Intent {
        return Intent(context, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }
}