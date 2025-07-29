package com.vibehealth.android.ui.splash

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.vibehealth.android.domain.auth.AuthRepository
import com.vibehealth.android.domain.auth.AuthState
import com.vibehealth.android.domain.auth.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockitoExtension::class)
class SplashViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @Mock
    private lateinit var mockAuthRepository: AuthRepository
    
    private lateinit var splashViewModel: SplashViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mock behaviors
        whenever(mockAuthRepository.getAuthStateFlow()).thenReturn(flowOf(AuthState.NotAuthenticated))
    }
    
    @Test
    fun `should navigate to login when user is not authenticated`() = runTest {
        // Given
        whenever(mockAuthRepository.getCurrentUser()).thenReturn(null)
        
        // When
        splashViewModel = SplashViewModel(mockAuthRepository)
        testDispatcher.scheduler.advanceTimeBy(1500) // Advance past splash duration
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(NavigationEvent.NavigateToLogin, splashViewModel.navigationEvent.value)
        assertEquals(false, splashViewModel.isLoading.value)
    }
    
    @Test
    fun `should navigate to main when user is authenticated`() = runTest {
        // Given
        val authenticatedUser = User("uid", "test@example.com")
        whenever(mockAuthRepository.getCurrentUser()).thenReturn(authenticatedUser)
        
        // When
        splashViewModel = SplashViewModel(mockAuthRepository)
        testDispatcher.scheduler.advanceTimeBy(1500) // Advance past splash duration
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(NavigationEvent.NavigateToMain, splashViewModel.navigationEvent.value)
        assertEquals(false, splashViewModel.isLoading.value)
    }
    
    @Test
    fun `should show loading initially`() {
        // When
        splashViewModel = SplashViewModel(mockAuthRepository)
        
        // Then
        assertTrue(splashViewModel.isLoading.value == true)
    }
    
    @Test
    fun `should navigate to login on authentication error`() = runTest {
        // Given
        whenever(mockAuthRepository.getCurrentUser()).thenThrow(RuntimeException("Auth error"))
        
        // When
        splashViewModel = SplashViewModel(mockAuthRepository)
        testDispatcher.scheduler.advanceTimeBy(1500) // Advance past splash duration
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(NavigationEvent.NavigateToLogin, splashViewModel.navigationEvent.value)
        assertEquals(false, splashViewModel.isLoading.value)
    }
    
    @Test
    fun `observeAuthState should handle authenticated state`() = runTest {
        // Given
        val authenticatedUser = User("uid", "test@example.com")
        val authState = AuthState.Authenticated(authenticatedUser)
        whenever(mockAuthRepository.getAuthStateFlow()).thenReturn(flowOf(authState))
        
        // When
        splashViewModel = SplashViewModel(mockAuthRepository)
        splashViewModel.observeAuthState()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(NavigationEvent.NavigateToMain, splashViewModel.navigationEvent.value)
        assertEquals(false, splashViewModel.isLoading.value)
    }
    
    @Test
    fun `observeAuthState should handle not authenticated state`() = runTest {
        // Given
        whenever(mockAuthRepository.getAuthStateFlow()).thenReturn(flowOf(AuthState.NotAuthenticated))
        
        // When
        splashViewModel = SplashViewModel(mockAuthRepository)
        splashViewModel.observeAuthState()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(NavigationEvent.NavigateToLogin, splashViewModel.navigationEvent.value)
        assertEquals(false, splashViewModel.isLoading.value)
    }
    
    @Test
    fun `observeAuthState should handle error state`() = runTest {
        // Given
        val errorState = AuthState.Error("Authentication error")
        whenever(mockAuthRepository.getAuthStateFlow()).thenReturn(flowOf(errorState))
        
        // When
        splashViewModel = SplashViewModel(mockAuthRepository)
        splashViewModel.observeAuthState()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(NavigationEvent.NavigateToLogin, splashViewModel.navigationEvent.value)
        assertEquals(false, splashViewModel.isLoading.value)
    }
    
    @Test
    fun `observeAuthState should handle loading state`() = runTest {
        // Given
        whenever(mockAuthRepository.getAuthStateFlow()).thenReturn(flowOf(AuthState.Loading))
        
        // When
        splashViewModel = SplashViewModel(mockAuthRepository)
        splashViewModel.observeAuthState()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        assertEquals(true, splashViewModel.isLoading.value)
    }
}