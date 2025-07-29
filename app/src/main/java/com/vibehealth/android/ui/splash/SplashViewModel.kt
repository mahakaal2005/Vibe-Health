package com.vibehealth.android.ui.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehealth.android.domain.auth.AuthRepository
import com.vibehealth.android.domain.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for splash screen that handles authentication state checking
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent
    
    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading
    
    init {
        checkAuthenticationState()
    }
    
    /**
     * Check current authentication state and navigate accordingly
     */
    private fun checkAuthenticationState() {
        viewModelScope.launch {
            // Show splash screen for minimum duration for branding
            delay(1500) // 1.5 seconds minimum splash duration
            
            try {
                val currentUser = authRepository.getCurrentUser()
                
                _navigationEvent.value = if (currentUser != null) {
                    // User is authenticated, check if onboarding is complete
                    // For now, navigate to main app (onboarding check can be added later)
                    NavigationEvent.NavigateToMain
                } else {
                    // User is not authenticated, go to login
                    NavigationEvent.NavigateToLogin
                }
            } catch (e: Exception) {
                // Error checking auth state, go to login to be safe
                _navigationEvent.value = NavigationEvent.NavigateToLogin
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Handle authentication state changes during splash
     */
    fun observeAuthState() {
        viewModelScope.launch {
            authRepository.getAuthStateFlow().collect { authState ->
                when (authState) {
                    is AuthState.Authenticated -> {
                        _navigationEvent.value = NavigationEvent.NavigateToMain
                        _isLoading.value = false
                    }
                    is AuthState.NotAuthenticated -> {
                        _navigationEvent.value = NavigationEvent.NavigateToLogin
                        _isLoading.value = false
                    }
                    is AuthState.Error -> {
                        _navigationEvent.value = NavigationEvent.NavigateToLogin
                        _isLoading.value = false
                    }
                    is AuthState.Loading -> {
                        _isLoading.value = true
                    }
                }
            }
        }
    }
}

/**
 * Navigation events from splash screen
 */
sealed class NavigationEvent {
    object NavigateToLogin : NavigationEvent()
    object NavigateToMain : NavigationEvent()
    object NavigateToOnboarding : NavigationEvent()
}