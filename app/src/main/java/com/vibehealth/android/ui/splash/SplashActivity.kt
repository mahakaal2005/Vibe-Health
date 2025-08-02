package com.vibehealth.android.ui.splash

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.vibehealth.android.MainActivity
import com.vibehealth.android.databinding.ActivitySplashBinding
import com.vibehealth.android.ui.auth.AuthActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Splash screen that checks authentication state and navigates accordingly
 * Implements design system specifications with sage green gradient background
 */
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySplashBinding
    private val viewModel: SplashViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupStatusBar()
        setupObservers()
        
        // Start observing auth state
        viewModel.observeAuthState()
    }
    
    /**
     * Configure status bar for sage green background with light content
     */
    private fun setupStatusBar() {
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false // Light content on dark background
        
        // Handle system bars insets
        binding.root.setOnApplyWindowInsetsListener { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    
    /**
     * Set up observers for ViewModel LiveData
     */
    private fun setupObservers() {
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observe navigation events
        viewModel.navigationEvent.observe(this) { event ->
            when (event) {
                is NavigationEvent.NavigateToLogin -> {
                    navigateToAuth()
                }
                is NavigationEvent.NavigateToMain -> {
                    navigateToMain()
                }
                is NavigationEvent.NavigateToOnboarding -> {
                    // TODO: Navigate to onboarding when implemented
                    navigateToMain()
                }
            }
        }
    }
    
    /**
     * Navigate to authentication screens
     */
    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
        
        // Add fade transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
    
    /**
     * Navigate to main application
     */
    private fun navigateToMain() {
        Log.d("VIBE_FIX_CRASH", "VIBE_FIX: SplashActivity navigateToMain() started")
        
        try {
            val intent = Intent(this, MainActivity::class.java)
            Log.d("VIBE_FIX_CRASH", "VIBE_FIX: Intent created for MainActivity")
            
            startActivity(intent)
            Log.d("VIBE_FIX_CRASH", "VIBE_FIX: MainActivity startActivity() called")
            
            finish()
            Log.d("VIBE_FIX_CRASH", "VIBE_FIX: SplashActivity finish() called")
            
            // Add fade transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            Log.d("VIBE_FIX_CRASH", "VIBE_FIX: Transition animation applied")
            
        } catch (e: Exception) {
            Log.e("VIBE_FIX_CRASH", "VIBE_FIX: FATAL ERROR in navigateToMain()", e)
            Log.e("VIBE_FIX_CRASH", "VIBE_FIX: Navigation exception: ${e.message}")
            throw e
        }
    }
}