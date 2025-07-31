package com.vibehealth.android.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vibehealth.android.domain.user.UserProfile
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Date

/**
 * Authentication service that handles user creation and initial Firestore document setup
 */
@Singleton
class AuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val TAG = "AuthService"
    }
    
    /**
     * Create initial user document in Firestore after successful authentication
     */
    suspend fun createInitialUserDocument(userId: String, email: String, displayName: String? = null): Result<Unit> {
        return try {
            // Check if user document already exists
            val existingDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            if (!existingDoc.exists()) {
                // Create initial user document with basic auth data
                val initialUserData = mapOf(
                    "userId" to userId,
                    "email" to email,
                    "displayName" to (displayName ?: ""),
                    "firstName" to "",
                    "lastName" to "",
                    "birthday" to null,
                    "gender" to "PREFER_NOT_TO_SAY",
                    "unitSystem" to "METRIC",
                    "heightInCm" to 0,
                    "weightInKg" to 0.0,
                    "hasCompletedOnboarding" to false,
                    "createdAt" to Date(),
                    "updatedAt" to Date()
                )
                
                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .set(initialUserData)
                    .await()
                
                android.util.Log.d(TAG, "✅ Initial user document created for userId: $userId")
            } else {
                android.util.Log.d(TAG, "ℹ️ User document already exists for userId: $userId")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to create initial user document", e)
            Result.failure(e)
        }
    }
    
    /**
     * Handle post-authentication setup
     */
    suspend fun handlePostAuthenticationSetup(): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                createInitialUserDocument(
                    userId = currentUser.uid,
                    email = currentUser.email ?: "",
                    displayName = currentUser.displayName
                )
            } else {
                Result.failure(Exception("No authenticated user found"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to handle post-authentication setup", e)
            Result.failure(e)
        }
    }
    
    /**
     * Ensure user document exists and has proper structure
     */
    suspend fun ensureUserDocumentExists(): Result<UserProfile?> {
        return try {
            val currentUser = firebaseAuth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("No authenticated user"))
            }
            
            // First, ensure the document exists
            createInitialUserDocument(
                userId = currentUser.uid,
                email = currentUser.email ?: "",
                displayName = currentUser.displayName
            )
            
            // Then read it back
            val document = firestore.collection(USERS_COLLECTION)
                .document(currentUser.uid)
                .get()
                .await()
            
            if (document.exists()) {
                val userProfile = document.toObject(UserProfile::class.java)
                android.util.Log.d(TAG, "✅ User document verified: ${userProfile?.userId}")
                Result.success(userProfile)
            } else {
                android.util.Log.e(TAG, "❌ User document still doesn't exist after creation attempt")
                Result.failure(Exception("Failed to create user document"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to ensure user document exists", e)
            Result.failure(e)
        }
    }
}