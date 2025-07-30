package com.vibehealth.android.domain.onboarding

/**
 * Enum representing the different steps in the onboarding flow with step tracking
 */
enum class OnboardingStep(val stepNumber: Int, val totalSteps: Int) {
    WELCOME(0, 4),
    PERSONAL_INFO(1, 4),
    PHYSICAL_INFO(2, 4),
    COMPLETION(3, 4);

    /**
     * Get the progress percentage for this step
     */
    fun getProgressPercentage(): Float = stepNumber.toFloat() / totalSteps.toFloat()

    /**
     * Check if this is the first step
     */
    fun isFirstStep(): Boolean = stepNumber == 0

    /**
     * Check if this is the last step
     */
    fun isLastStep(): Boolean = stepNumber == totalSteps

    /**
     * Get the next step, or null if this is the last step
     */
    fun getNextStep(): OnboardingStep? = when (this) {
        WELCOME -> PERSONAL_INFO
        PERSONAL_INFO -> PHYSICAL_INFO
        PHYSICAL_INFO -> COMPLETION
        COMPLETION -> null
    }

    /**
     * Get the previous step, or null if this is the first step
     */
    fun getPreviousStep(): OnboardingStep? = when (this) {
        WELCOME -> null
        PERSONAL_INFO -> WELCOME
        PHYSICAL_INFO -> PERSONAL_INFO
        COMPLETION -> PHYSICAL_INFO
    }
}