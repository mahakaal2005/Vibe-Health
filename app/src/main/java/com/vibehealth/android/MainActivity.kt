package com.vibehealth.android

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vibehealth.android.core.auth.AuthGuard
import com.vibehealth.android.data.user.UserProfileRepository
import com.vibehealth.android.databinding.ActivityMainBinding
import com.vibehealth.android.domain.auth.AuthState
import com.vibehealth.android.ui.auth.AuthActivity
import com.vibehealth.android.ui.auth.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()
    
    @Inject
    lateinit var authGuard: AuthGuard
    
    @Inject
    lateinit var userProfileRepository: UserProfileRepository
    
    @Inject
    lateinit var integrationManager: com.vibehealth.android.core.integration.OnboardingIntegrationManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check authentication before setting up UI
        lifecycleScope.launch {
            if (!authGuard.requireAuthentication()) {
                return@launch // Will be redirected to auth
            }
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupEdgeToEdge()
        setupNavigation()
        setupAuthObserver()
    }
    
    /**
     * Set up edge-to-edge display with proper insets
     */
    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    
    /**
     * Set up bottom navigation with Navigation Component
     */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        val navController = navHostFragment.navController
        
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation.setupWithNavController(navController)
    }
    
    /**
     * Set up authentication state observer
     */
    private fun setupAuthObserver() {
        authViewModel.authState.observe(this) { authState ->
            when (authState) {
                is AuthState.NotAuthenticated -> {
                    // User is not authenticated, redirect to auth flow
                    navigateToAuth()
                }
                is AuthState.Authenticated -> {
                    // User is authenticated, continue with main app
                    // Check if user needs onboarding
                    checkOnboardingStatus(authState.user)
                }
                is AuthState.Error -> {
                    // Handle authentication error
                    handleAuthError(authState.message)
                }
                is AuthState.Loading -> {
                    // Show loading state if needed
                }
            }
        }
    }
    
    /**
     * Navigate to authentication flow
     */
    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
    
    /**
     * Check if user needs onboarding
     */
    private fun checkOnboardingStatus(user: com.vibehealth.android.domain.auth.User) {
        // Check if user just completed onboarding
        val onboardingJustCompleted = intent.getBooleanExtra("onboarding_just_completed", false)
        if (onboardingJustCompleted) {
            return // Stay on main app
        }
        
        lifecycleScope.launch {
            try {
                val navigationDecision = integrationManager.handleExistingUserOnboardingCheck(user.uid)
                
                when (navigationDecision) {
                    com.vibehealth.android.core.integration.NavigationDecision.NAVIGATE_TO_ONBOARDING -> {
                        navigateToOnboarding()
                    }
                    com.vibehealth.android.core.integration.NavigationDecision.NAVIGATE_TO_MAIN_APP -> {
                        // Stay on main app - user has completed onboarding
                    }
                }
            } catch (e: Exception) {
                // Handle error - assume onboarding needed for safety
                android.util.Log.e("MainActivity", "Failed to check onboarding status", e)
                navigateToOnboarding()
            }
        }
    }
    
    /**
     * Navigate to onboarding flow
     */
    private fun navigateToOnboarding() {
        val intent = Intent(this, com.vibehealth.android.ui.onboarding.OnboardingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
    
    /**
     * Handle authentication errors
     */
    private fun handleAuthError(errorMessage: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Authentication Error")
            .setMessage("There was an issue with your authentication. Please sign in again.")
            .setPositiveButton("Sign In") { _, _ ->
                navigateToAuth()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * Create options menu with logout
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    /**
     * Handle options menu item selection
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    /**
     * Handle user logout
     */
    private fun logout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ ->
                lifecycleScope.launch {
                    authViewModel.signOut()
                    // Navigation will be handled by auth state observer
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Handle back button press
     */
    override fun onBackPressed() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        val navController = navHostFragment.navController
        
        if (!navController.popBackStack()) {
            // If can't go back in navigation, minimize app instead of closing
            moveTaskToBack(true)
        }
    }
}