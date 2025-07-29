package com.vibehealth.android.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.vibehealth.android.R
import com.vibehealth.android.databinding.ActivityAuthBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity that hosts authentication fragments with proper navigation
 * Implements design system specifications and navigation flow
 */
@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAuthBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupNavigation()
        handleDeepLinks()
    }
    
    /**
     * Set up navigation controller and back stack management
     */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_auth) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Handle back button properly
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment -> {
                    // Clear back stack when on login (entry point)
                }
                R.id.registerFragment -> {
                    // Allow back navigation to login
                }
            }
        }
    }
    
    /**
     * Handle deep links for authentication flows
     */
    private fun handleDeepLinks() {
        intent?.data?.let { uri ->
            when (uri.path) {
                "/login" -> {
                    // Navigate to login (default)
                }
                "/register" -> {
                    // Navigate to register
                    val navHostFragment = supportFragmentManager
                        .findFragmentById(R.id.nav_host_fragment_auth) as NavHostFragment
                    val navController = navHostFragment.navController
                    navController.navigate(R.id.registerFragment)
                }
            }
        }
    }
    
    /**
     * Handle back button press with proper navigation
     */
    override fun onBackPressed() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_auth) as NavHostFragment
        val navController = navHostFragment.navController
        
        if (!navController.popBackStack()) {
            // If can't go back in navigation, finish activity
            super.onBackPressed()
        }
    }
    
    /**
     * Override pending transition for smooth activity transitions
     */
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}