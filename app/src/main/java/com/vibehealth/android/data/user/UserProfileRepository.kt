package com.vibehealth.android.data.user

import com.google.firebase.firestore.FirebaseFirestore
import com.vibehealth.android.domain.user.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for user profile data operations
 */
@Singleton
class UserProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        private const val USERS_COLLECTION = "users"
    }
    
    /**
     * Get user profile by user ID
     */
    suspend fun getUserProfile(userId: String): Result<UserProfile?> {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                val userProfile = document.toObject(UserProfile::class.java)
                Result.success(userProfile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Save user profile to Firestore
     */
    suspend fun saveUserProfile(userId: String, userProfile: UserProfile): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(userProfile)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if user has completed onboarding
     */
    suspend fun hasCompletedOnboarding(userId: String): Result<Boolean> {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                val userProfile = document.toObject(UserProfile::class.java)
                Result.success(userProfile?.hasCompletedOnboarding ?: false)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Mark onboarding as completed
     */
    suspend fun markOnboardingCompleted(userId: String): Result<Unit> {
        return updateUserProfile(userId, mapOf("hasCompletedOnboarding" to true))
    }
    
    /**
     * Get user profile as Flow for reactive updates
     */
    fun getUserProfileFlow(userId: String): Flow<UserProfile?> = flow {
        try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                val userProfile = document.toObject(UserProfile::class.java)
                emit(userProfile)
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }
}