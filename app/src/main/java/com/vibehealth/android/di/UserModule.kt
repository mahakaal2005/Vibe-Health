package com.vibehealth.android.di

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.vibehealth.android.core.security.EncryptionHelper
import com.vibehealth.android.data.user.UserProfileRepository
import com.vibehealth.android.data.user.local.UserProfileDao
import com.vibehealth.android.data.user.remote.UserProfileService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UserModule {
    
    @Provides
    @Singleton
    fun provideUserProfileRepository(
        userProfileDao: UserProfileDao,
        userProfileService: UserProfileService,
        encryptionHelper: EncryptionHelper,
        @ApplicationContext context: Context
    ): UserProfileRepository {
        return UserProfileRepository(userProfileDao, userProfileService, encryptionHelper, context)
    }
}