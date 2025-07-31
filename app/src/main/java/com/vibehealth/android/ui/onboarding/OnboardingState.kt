package com.vibehealth.android.ui.onboarding

/**
 * Represents the different states of the onboarding process
 */
sealed class OnboardingState {
    object PersonalInfo : OnboardingState()
    object PhysicalInfo : OnboardingState()
    object Completing : OnboardingState()
    object Completed : OnboardingState()
    data class Error(val message: String) : OnboardingState()
}

/**
 * Validation errors for onboarding forms
 */
data class ValidationErrors(
    val nameError: String? = null,
    val birthdayError: String? = null,
    val heightError: String? = null,
    val weightError: String? = null
) {
    fun hasErrors(): Boolean {
        return nameError != null || birthdayError != null || heightError != null || weightError != null
    }
}