package com.vibehealth.android.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehealth.android.domain.auth.AuthRepository
import com.vibehealth.android.domain.auth.AuthResult
import com.vibehealth.android.domain.auth.AuthState
import com.vibehealth.android.domain.auth.AuthUiState
import com.vibehealth.android.domain.auth.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication screens following MVVM pattern
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // Authentication state
    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState
    
    // UI state for forms
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    // Form fields
    private val _email = MutableLiveData("")
    val email: LiveData<String> = _email
    
    private val _password = MutableLiveData("")
    val password: LiveData<String> = _password
    
    private val _confirmPassword = MutableLiveData("")
    val confirmPassword: LiveData<String> = _confirmPassword
    
    // Track if user has attempted to submit the form
    private var _hasAttemptedSubmit = false
    
    init {
        // Start observing authentication state
        observeAuthState()
        checkCurrentAuthState()
    }
    
    /**
     * Sign in user with email and password
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            // Mark that user has attempted to submit the form
            _hasAttemptedSubmit = true
            
            // Validate all fields on submission
            val emailValidation = validateEmail(email)
            val passwordValidation = validatePassword(password)
            
            // If any validation fails, don't proceed
            if (!emailValidation.isValid || !passwordValidation.isValid) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Please fix the errors above"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            when (val result = authRepository.signIn(email, password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _authState.value = AuthState.Authenticated(result.user)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exception.message
                    )
                    _authState.value = AuthState.Error(
                        result.exception.message ?: "Sign in failed",
                        result.exception
                    )
                }
                is AuthResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
    
    /**
     * Sign up new user with email and password
     */
    fun signUp(email: String, password: String, confirmPassword: String) {
        viewModelScope.launch {
            // Mark that user has attempted to submit the form
            _hasAttemptedSubmit = true
            
            // Validate all fields on submission
            val emailValidation = validateEmail(email)
            val passwordValidation = validatePassword(password)
            val confirmValidation = authRepository.validatePasswordConfirmation(password, confirmPassword)
            
            // If any validation fails, don't proceed
            if (!emailValidation.isValid || !passwordValidation.isValid || !confirmValidation.isValid) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = confirmValidation.errorMessage ?: "Please fix the errors above"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            when (val result = authRepository.signUp(email, password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _authState.value = AuthState.Authenticated(result.user)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exception.message
                    )
                    _authState.value = AuthState.Error(
                        result.exception.message ?: "Sign up failed",
                        result.exception
                    )
                }
                is AuthResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
    
    /**
     * Sign out current user
     */
    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            when (val result = authRepository.signOut()) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    _authState.value = AuthState.NotAuthenticated
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exception.message
                    )
                }
                is AuthResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
    
    /**
     * Send password reset email
     */
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            when (val result = authRepository.sendPasswordResetEmail(email)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Password reset email sent successfully"
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exception.message
                    )
                }
                is AuthResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
    
    /**
     * Validate email in real-time
     */
    fun validateEmail(email: String): ValidationResult {
        val result = authRepository.validateEmail(email)
        _uiState.value = _uiState.value.copy(
            isEmailValid = result.isValid,
            emailError = result.errorMessage
        )
        updateFormValidation()
        return result
    }
    
    /**
     * Validate password in real-time
     */
    fun validatePassword(password: String): ValidationResult {
        val result = authRepository.validatePassword(password)
        _uiState.value = _uiState.value.copy(
            isPasswordValid = result.isValid,
            passwordError = result.errorMessage
        )
        updateFormValidation()
        return result
    }
    
    /**
     * Update email field
     */
    fun updateEmail(email: String) {
        _email.value = email
        // Only validate if user has already attempted to submit the form
        if (_hasAttemptedSubmit) {
            validateEmail(email)
        }
    }
    
    /**
     * Update password field
     */
    fun updatePassword(password: String) {
        _password.value = password
        // Only validate if user has already attempted to submit the form
        if (_hasAttemptedSubmit) {
            validatePassword(password)
        }
    }
    
    /**
     * Update confirm password field
     */
    fun updateConfirmPassword(confirmPassword: String) {
        _confirmPassword.value = confirmPassword
        // Only validate if user has already attempted to submit the form
        if (_hasAttemptedSubmit) {
            _password.value?.let { password ->
                if (password.isNotEmpty()) {
                    val result = authRepository.validatePasswordConfirmation(password, confirmPassword)
                    _uiState.value = _uiState.value.copy(
                        errorMessage = if (!result.isValid) result.errorMessage else null
                    )
                }
            }
        }
        updateFormValidation()
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Reset form state (call when navigating between auth screens)
     */
    fun resetFormState() {
        _hasAttemptedSubmit = false
        _uiState.value = AuthUiState()
        _email.value = ""
        _password.value = ""
        _confirmPassword.value = ""
    }
    
    /**
     * Check current authentication state
     */
    private fun checkCurrentAuthState() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val currentUser = authRepository.getCurrentUser()
            _authState.value = if (currentUser != null) {
                AuthState.Authenticated(currentUser)
            } else {
                AuthState.NotAuthenticated
            }
        }
    }
    
    /**
     * Observe authentication state changes
     */
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.getAuthStateFlow().collect { authState ->
                _authState.value = authState
            }
        }
    }
    
    /**
     * Update form validation state
     */
    private fun updateFormValidation() {
        val currentState = _uiState.value
        val isFormValid = currentState.isEmailValid && 
                         currentState.isPasswordValid && 
                         currentState.emailError == null && 
                         currentState.passwordError == null
        
        _uiState.value = currentState.copy(isFormValid = isFormValid)
    }
}