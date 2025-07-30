package com.vibehealth.android.ui.onboarding

import com.vibehealth.android.domain.onboarding.OnboardingStep

/**
 * Navigation events for onboarding flow
 */
sealed class OnboardingNavigationEvent {
    data class NavigateForward(val step: OnboardingStep) : OnboardingNavigationEvent()
    data class NavigateBackward(val step: OnboardingStep) : OnboardingNavigationEvent()
    data class UnitSystemChanged(
        val newSystem: com.vibehealth.android.domain.common.UnitSystem,
        val convertedHeight: Double?,
        val convertedWeight: Double?
    ) : OnboardingNavigationEvent()
    object OnboardingComplete : OnboardingNavigationEvent()
}

/**
 * Error recovery events for onboarding
 */
sealed class ErrorRecoveryEvent {
    object NetworkError : ErrorRecoveryEvent()
    object StorageError : ErrorRecoveryEvent()
    object MaxRetriesExceeded : ErrorRecoveryEvent()
    data class UnknownError(val message: String) : ErrorRecoveryEvent()
}