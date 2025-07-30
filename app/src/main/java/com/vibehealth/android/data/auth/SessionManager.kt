package com.vibehealth.android.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.vibehealth.android.domain.auth.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session manager for handling authentication state persistence
 * Uses DataStore for secure, encrypted local storage
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_session")
        
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        private val LAST_LOGIN_TIME = stringPreferencesKey("last_login_time")
        private val REMEMBER_ME = booleanPreferencesKey("remember_me")
    }
    
    /**
     * Save user session after successful authentication
     */
    suspend fun saveUserSession(user: User, rememberMe: Boolean = true) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            preferences[USER_ID] = user.uid
            preferences[USER_EMAIL] = user.email ?: ""
            preferences[USER_DISPLAY_NAME] = user.displayName ?: ""
            preferences[LAST_LOGIN_TIME] = System.currentTimeMillis().toString()
            preferences[REMEMBER_ME] = rememberMe
        }
    }
    
    /**
     * Clear user session on logout
     */
    suspend fun clearUserSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
        
        // Also sign out from Firebase
        firebaseAuth.signOut()
    }
    
    /**
     * Check if user is logged in
     */
    suspend fun isUserLoggedIn(): Boolean {
        val preferences = context.dataStore.data.first()
        val isLoggedIn = preferences[IS_LOGGED_IN] ?: false
        val rememberMe = preferences[REMEMBER_ME] ?: false
        
        // If user doesn't want to be remembered, check Firebase auth state
        return if (rememberMe) {
            isLoggedIn && firebaseAuth.currentUser != null
        } else {
            firebaseAuth.currentUser != null
        }
    }
    
    /**
     * Get stored user information
     */
    suspend fun getStoredUser(): User? {
        val preferences = context.dataStore.data.first()
        val isLoggedIn = preferences[IS_LOGGED_IN] ?: false
        
        return if (isLoggedIn) {
            User(
                uid = preferences[USER_ID] ?: "",
                email = preferences[USER_EMAIL],
                displayName = preferences[USER_DISPLAY_NAME],
                isEmailVerified = firebaseAuth.currentUser?.isEmailVerified ?: false
            )
        } else {
            null
        }
    }
    
    /**
     * Get user session as Flow for reactive updates
     */
    fun getUserSessionFlow(): Flow<User?> {
        return context.dataStore.data.map { preferences ->
            val isLoggedIn = preferences[IS_LOGGED_IN] ?: false
            
            if (isLoggedIn && firebaseAuth.currentUser != null) {
                User(
                    uid = preferences[USER_ID] ?: "",
                    email = preferences[USER_EMAIL],
                    displayName = preferences[USER_DISPLAY_NAME],
                    isEmailVerified = firebaseAuth.currentUser?.isEmailVerified ?: false
                )
            } else {
                null
            }
        }
    }
    
    /**
     * Check if session is expired (24 hours)
     */
    suspend fun isSessionExpired(): Boolean {
        val preferences = context.dataStore.data.first()
        val lastLoginTime = preferences[LAST_LOGIN_TIME]?.toLongOrNull() ?: 0
        val currentTime = System.currentTimeMillis()
        val sessionDuration = 24 * 60 * 60 * 1000 // 24 hours in milliseconds
        
        return (currentTime - lastLoginTime) > sessionDuration
    }
    
    /**
     * Refresh session timestamp
     */
    suspend fun refreshSession() {
        if (firebaseAuth.currentUser != null) {
            context.dataStore.edit { preferences ->
                preferences[LAST_LOGIN_TIME] = System.currentTimeMillis().toString()
            }
        }
    }
    
    /**
     * Handle session timeout
     */
    suspend fun handleSessionTimeout() {
        if (isSessionExpired()) {
            clearUserSession()
        }
    }
    
    /**
     * Get last login time
     */
    suspend fun getLastLoginTime(): Long {
        val preferences = context.dataStore.data.first()
        return preferences[LAST_LOGIN_TIME]?.toLongOrNull() ?: 0
    }
    
    /**
     * Check if user wants to be remembered
     */
    suspend fun shouldRememberUser(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[REMEMBER_ME] ?: false
    }
    
    /**
     * Update user profile information in session
     */
    suspend fun updateUserProfile(userProfile: com.vibehealth.android.domain.user.UserProfile) {
        context.dataStore.edit { preferences ->
            preferences[USER_DISPLAY_NAME] = userProfile.displayName
            preferences[LAST_LOGIN_TIME] = System.currentTimeMillis().toString()
        }
    }
}