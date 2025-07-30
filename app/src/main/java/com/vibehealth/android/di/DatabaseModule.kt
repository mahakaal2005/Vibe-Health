package com.vibehealth.android.di

import android.content.Context
import com.vibehealth.android.core.security.EncryptionHelper
import com.vibehealth.android.data.user.UserProfileRepository
import com.vibehealth.android.data.user.local.AppDatabase
import com.vibehealth.android.data.user.local.UserProfileDao
import com.vibehealth.android.data.user.remote.UserProfileService
import com.vibehealth.android.domain.user.UserRepository
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database and repository dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    abstract fun bindUserRepository(
        userProfileRepository: UserProfileRepository
    ): UserRepository

    companion object {
        @Provides
        @Singleton
        fun provideEncryptionHelper(@ApplicationContext context: Context): EncryptionHelper {
            return EncryptionHelper.getInstance(context)
        }

        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context,
            encryptionHelper: EncryptionHelper
        ): AppDatabase {
            val passphrase = encryptionHelper.getDatabasePassphrase()
            return AppDatabase.getDatabase(context, passphrase)
        }

        @Provides
        fun provideUserProfileDao(database: AppDatabase): UserProfileDao {
            return database.userProfileDao()
        }

        @Provides
        @Singleton
        fun provideUserProfileService(): UserProfileService {
            return UserProfileService()
        }
    }
}