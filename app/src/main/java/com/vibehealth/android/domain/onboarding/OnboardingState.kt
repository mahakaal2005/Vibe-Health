package com.vibehealth.android.domain.onboarding

/**
 * Sealed class representing the different states of the onboarding flow
 */
sealed class OnboardingState {
    object Loading : OnboardingState()
    object PersonalInfo : OnboardingState()
    object PhysicalInfo : OnboardingState()
    object Completing : OnboardingState()
    object Completed : OnboardingState()
    data class Error(val message: String, val exception: Exception? = null) : OnboardingState()
}