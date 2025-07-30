package com.vibehealth.android.core.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.vibehealth.android.domain.onboarding.OnboardingStep
import com.vibehealth.android.domain.user.Gender
import com.vibehealth.android.domain.common.UnitSystem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages onboarding progress persistence to handle app backgrounding and process death
 */
@Singleton
class OnboardingProgressManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding_progress")
        
        private val CURRENT_STEP = stringPreferencesKey("current_step")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_BIRTHDAY = longPreferencesKey("user_birthday")
        private val USER_HEIGHT = stringPreferencesKey("user_height")
        private val USER_WEIGHT = stringPreferencesKey("user_weight")
        private val USER_GENDER = stringPreferencesKey("user_gender")
        private val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        private val PROGRESS_TIMESTAMP = longPreferencesKey("progress_timestamp")
        private val IS_ONBOARDING_ACTIVE = booleanPreferencesKey("is_onboarding_active")
    }

    /**
     * Save current onboarding progress
     */
    suspend fun saveProgress(progressData: OnboardingProgressData) {
        context.onboardingDataStore.edit { preferences ->
            preferences[CURRENT_STEP] = progressData.currentStep.name
            preferences[USER_NAME] = progressData.userName
            progressData.userBirthday?.let { 
                preferences[USER_BIRTHDAY] = it.time 
            }
            preferences[USER_HEIGHT] = progressData.userHeight
            preferences[USER_WEIGHT] = progressData.userWeight
            preferences[USER_GENDER] = progressData.userGender.name
            preferences[UNIT_SYSTEM] = progressData.unitSystem.name
            preferences[PROGRESS_TIMESTAMP] = System.currentTimeMillis()
            preferences[IS_ONBOARDING_ACTIVE] = true
        }
    }

    /**
     * Get saved onboarding progress
     */
    suspend fun getProgress(): OnboardingProgressData? {
        val preferences = context.onboardingDataStore.data.first()
        
        if (preferences[IS_ONBOARDING_ACTIVE] != true) {
            return null
        }

        // Check if progress is stale (older than 24 hours)
        val timestamp = preferences[PROGRESS_TIMESTAMP] ?: 0
        val isStale = System.currentTimeMillis() - timestamp > 24 * 60 * 60 * 1000
        
        if (isStale) {
            clearProgress()
            return null
        }

        return try {
            OnboardingProgressData(
                currentStep = OnboardingStep.valueOf(preferences[CURRENT_STEP] ?: OnboardingStep.WELCOME.name),
                userName = preferences[USER_NAME] ?: "",
                userBirthday = preferences[USER_BIRTHDAY]?.let { Date(it) },
                userHeight = preferences[USER_HEIGHT] ?: "",
                userWeight = preferences[USER_WEIGHT] ?: "",
                userGender = Gender.valueOf(preferences[USER_GENDER] ?: Gender.PREFER_NOT_TO_SAY.name),
                unitSystem = UnitSystem.valueOf(preferences[UNIT_SYSTEM] ?: UnitSystem.METRIC.name)
            )
        } catch (e: Exception) {
            // If there's any error parsing saved data, clear it and return null
            clearProgress()
            null
        }
    }

    /**
     * Get progress as Flow for reactive updates
     */
    fun getProgressFlow(): Flow<OnboardingProgressData?> {
        return context.onboardingDataStore.data.map { preferences ->
            if (preferences[IS_ONBOARDING_ACTIVE] != true) {
                return@map null
            }

            try {
                OnboardingProgressData(
                    currentStep = OnboardingStep.valueOf(preferences[CURRENT_STEP] ?: OnboardingStep.WELCOME.name),
                    userName = preferences[USER_NAME] ?: "",
                    userBirthday = preferences[USER_BIRTHDAY]?.let { Date(it) },
                    userHeight = preferences[USER_HEIGHT] ?: "",
                    userWeight = preferences[USER_WEIGHT] ?: "",
                    userGender = Gender.valueOf(preferences[USER_GENDER] ?: Gender.PREFER_NOT_TO_SAY.name),
                    unitSystem = UnitSystem.valueOf(preferences[UNIT_SYSTEM] ?: UnitSystem.METRIC.name)
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Clear saved progress
     */
    suspend fun clearProgress() {
        context.onboardingDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Mark onboarding as completed
     */
    suspend fun markOnboardingCompleted() {
        context.onboardingDataStore.edit { preferences ->
            preferences[IS_ONBOARDING_ACTIVE] = false
        }
    }

    /**
     * Check if there's saved progress
     */
    suspend fun hasSavedProgress(): Boolean {
        val preferences = context.onboardingDataStore.data.first()
        return preferences[IS_ONBOARDING_ACTIVE] == true
    }

    /**
     * Update specific field in progress
     */
    suspend fun updateField(field: ProgressField, value: String) {
        context.onboardingDataStore.edit { preferences ->
            when (field) {
                ProgressField.NAME -> preferences[USER_NAME] = value
                ProgressField.HEIGHT -> preferences[USER_HEIGHT] = value
                ProgressField.WEIGHT -> preferences[USER_WEIGHT] = value
                ProgressField.GENDER -> preferences[USER_GENDER] = value
                ProgressField.UNIT_SYSTEM -> preferences[UNIT_SYSTEM] = value
            }
            preferences[PROGRESS_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    /**
     * Update birthday field
     */
    suspend fun updateBirthday(birthday: Date?) {
        context.onboardingDataStore.edit { preferences ->
            if (birthday != null) {
                preferences[USER_BIRTHDAY] = birthday.time
            } else {
                preferences.remove(USER_BIRTHDAY)
            }
            preferences[PROGRESS_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    /**
     * Update current step
     */
    suspend fun updateCurrentStep(step: OnboardingStep) {
        context.onboardingDataStore.edit { preferences ->
            preferences[CURRENT_STEP] = step.name
            preferences[PROGRESS_TIMESTAMP] = System.currentTimeMillis()
        }
    }
}

/**
 * Data class representing saved onboarding progress
 */
data class OnboardingProgressData(
    val currentStep: OnboardingStep,
    val userName: String,
    val userBirthday: Date?,
    val userHeight: String,
    val userWeight: String,
    val userGender: Gender,
    val unitSystem: UnitSystem
)

/**
 * Enum for progress fields that can be updated individually
 */
enum class ProgressField {
    NAME,
    HEIGHT,
    WEIGHT,
    GENDER,
    UNIT_SYSTEM
}