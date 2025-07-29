package com.vibehealth.android.ui.auth

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
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
        setupBackPressedCallback()
        
        // Handle deep links after a brief delay to ensure navigation is fully set up
        binding.root.post {
            handleDeepLinks()
        }
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
                    // Login is the default start destination, no navigation needed
                    return
                }
                "/register" -> {
                    // Navigate to register only if explicitly requested via deep link
                    val navHostFragment = supportFragmentManager
                        .findFragmentById(R.id.nav_host_fragment_auth) as NavHostFragment
                    val navController = navHostFragment.navController
                    
                    // Post the navigation to avoid conflicts with initial fragment loading
                    binding.root.post {
                        if (navController.currentDestination?.id == R.id.loginFragment) {
                            navController.navigate(R.id.action_login_to_register)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Set up back button handling using modern OnBackPressedDispatcher
     */
    private fun setupBackPressedCallback() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment_auth) as NavHostFragment
                val navController = navHostFragment.navController
                
                when (navController.currentDestination?.id) {
                    R.id.loginFragment -> {
                        // On login fragment, exit the app
                        finish()
                    }
                    R.id.registerFragment -> {
                        // On register fragment, go back to login and clear back stack
                        navController.navigate(R.id.action_register_to_login)
                    }
                    else -> {
                        // Default behavior
                        if (!navController.popBackStack()) {
                            finish()
                        }
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    
    /**
     * Override pending transition for smooth activity transitions
     */
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}