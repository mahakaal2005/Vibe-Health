package com.vibehealth.android.ui.navigation

/**
 * General navigation events for the app
 */
sealed class NavigationEvent {
    object NavigateToLogin : NavigationEvent()
    object NavigateToMain : NavigationEvent()
    object NavigateToOnboarding : NavigationEvent()
}