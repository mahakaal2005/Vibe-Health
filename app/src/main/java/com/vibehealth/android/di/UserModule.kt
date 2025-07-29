package com.vibehealth.android.di

import com.google.firebase.firestore.FirebaseFirestore
import com.vibehealth.android.data.user.UserProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserModule {
    
    @Provides
    @Singleton
    fun provideUserProfileRepository(
        firestore: FirebaseFirestore
    ): UserProfileRepository {
        return UserProfileRepository(firestore)
    }
}