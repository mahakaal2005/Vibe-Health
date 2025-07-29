package com.vibehealth.android.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vibehealth.android.data.auth.AuthRepositoryImpl
import com.vibehealth.android.data.auth.FirebaseAuthService
import com.vibehealth.android.data.auth.SessionManager
import com.vibehealth.android.domain.auth.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for authentication dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    
    @Provides
    @Singleton
    fun provideFirebaseCrashlytics(): com.google.firebase.crashlytics.FirebaseCrashlytics {
        return com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseAuthService(): FirebaseAuthService {
        return FirebaseAuthService()
    }
    
    @Provides
    @Singleton
    fun provideSessionManager(
        @ApplicationContext context: Context,
        firebaseAuth: FirebaseAuth
    ): SessionManager {
        return SessionManager(context, firebaseAuth)
    }
    
    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuthService: FirebaseAuthService,
        firestore: FirebaseFirestore,
        sessionManager: SessionManager
    ): AuthRepository {
        return AuthRepositoryImpl(firebaseAuthService, firestore, sessionManager)
    }
    
    @Provides
    @Singleton
    fun provideAuthGuard(
        @ApplicationContext context: Context,
        sessionManager: SessionManager
    ): com.vibehealth.android.core.auth.AuthGuard {
        return com.vibehealth.android.core.auth.AuthGuard(context, sessionManager)
    }
}