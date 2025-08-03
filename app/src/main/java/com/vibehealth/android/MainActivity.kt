package com.vibehealth.android

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.vibehealth.android.databinding.ActivityMainBinding
import com.vibehealth.android.ui.auth.AuthViewModel
import com.vibehealth.android.ui.profile.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * VIBE_FIX: Phase 3 - MainActivity with comprehensive crash logging
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "VIBE_FIX_CRASH"
    }
    
    private lateinit var binding: ActivityMainBinding
    
    // ViewModels for preloading data
    private val authViewModel: AuthViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "VIBE_FIX: MainActivity onCreate() started")
        
        try {
            super.onCreate(savedInstanceState)
            Log.d(TAG, "VIBE_FIX: super.onCreate() completed")
            

            
            Log.d(TAG, "VIBE_FIX: Inflating binding")
            binding = ActivityMainBinding.inflate(layoutInflater)
            Log.d(TAG, "VIBE_FIX: Binding inflated successfully")
            
            setContentView(binding.root)
            Log.d(TAG, "VIBE_FIX: setContentView completed")
            
            setupNavigation()
            Log.d(TAG, "VIBE_FIX: setupNavigation completed")
            
            // Preload profile data for seamless experience
            preloadProfileData()
            Log.d(TAG, "VIBE_FIX: preloadProfileData initiated")
            
            Log.d(TAG, "VIBE_FIX: MainActivity onCreate() completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "VIBE_FIX: FATAL ERROR in MainActivity onCreate()", e)
            Log.e(TAG, "VIBE_FIX: Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "VIBE_FIX: Exception message: ${e.message}")
            Log.e(TAG, "VIBE_FIX: Stack trace: ${e.stackTraceToString()}")
            throw e // Re-throw to see the crash
        }
    }
    
    /**
     * Set up bottom navigation with Navigation Component
     */
    private fun setupNavigation() {
        try {
            Log.d(TAG, "VIBE_FIX: setupNavigation() started")
            
            Log.d(TAG, "VIBE_FIX: Finding NavHostFragment")
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_main) as? NavHostFragment
            
            if (navHostFragment == null) {
                Log.e(TAG, "VIBE_FIX: NavHostFragment not found!")
                return
            }
            Log.d(TAG, "VIBE_FIX: NavHostFragment found successfully")
            
            Log.d(TAG, "VIBE_FIX: Getting NavController")
            val navController = navHostFragment.navController
            Log.d(TAG, "VIBE_FIX: NavController obtained successfully")
            
            Log.d(TAG, "VIBE_FIX: Finding BottomNavigationView")
            val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
            
            if (bottomNavigation == null) {
                Log.e(TAG, "VIBE_FIX: BottomNavigationView not found!")
                return
            }
            Log.d(TAG, "VIBE_FIX: BottomNavigationView found successfully")
            
            Log.d(TAG, "VIBE_FIX: Setting up navigation with controller")
            bottomNavigation.setupWithNavController(navController)
            Log.d(TAG, "VIBE_FIX: Navigation setup completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "VIBE_FIX: FATAL ERROR in setupNavigation()", e)
            Log.e(TAG, "VIBE_FIX: Navigation exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "VIBE_FIX: Navigation exception message: ${e.message}")
            Log.e(TAG, "VIBE_FIX: Navigation stack trace: ${e.stackTraceToString()}")
            throw e
        }
    }
    
    /**
     * Handle back button press
     */
    override fun onBackPressed() {
        try {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_main) as? NavHostFragment
            
            val navController = navHostFragment?.navController
            
            if (navController?.popBackStack() != true) {
                // If can't go back in navigation, minimize app instead of closing
                moveTaskToBack(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "VIBE_FIX: Back press handling failed", e)
            super.onBackPressed()
        }
    }
    
    /**
     * Preload profile data for seamless user experience and enable cross-fragment communication
     */
    private fun preloadProfileData() {
        try {
            Log.d(TAG, "🚀 Starting profile data preload")
            
            // Observe auth state and preload profile when user is authenticated
            authViewModel.authState.observe(this) { authState ->
                Log.d(TAG, "🔍 Auth state changed: ${authState?.javaClass?.simpleName}")
                // Check if user is authenticated and preload profile
                try {
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        Log.d(TAG, "🔄 Preloading profile for user: ${currentUser.uid}")
                        profileViewModel.loadProfile(currentUser.uid)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error preloading profile", e)
                }
            }
            
            // TASK 11: Enable cross-fragment communication through shared ProfileViewModel
            setupCrossFragmentCommunication()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Profile preload failed", e)
        }
    }
    
    /**
     * TASK 11: Setup cross-fragment communication for real-time profile updates
     */
    private fun setupCrossFragmentCommunication() {
        Log.d("PROFILE_REALTIME", "Setting up cross-fragment communication")
        
        // Observe profile changes for app-wide updates using StateFlow
        lifecycleScope.launch {
            profileViewModel.profileFlow.collect { profile ->
                if (profile != null) {
                    Log.d("PROFILE_REALTIME", "Profile updated across app - Name: ${profile.displayName}, Unit: ${profile.unitSystem}")
                    // Profile changes are automatically reflected through shared ViewModel
                }
            }
        }
        
        // Observe unit system changes for immediate app-wide unit display updates
        lifecycleScope.launch {
            profileViewModel.unitSystemFlow.collect { unitSystem ->
                if (unitSystem != null) {
                    Log.d("PROFILE_REALTIME", "Unit system updated app-wide: $unitSystem")
                    // Unit system changes are automatically reflected through shared ViewModel
                }
            }
        }
        
        // Observe display name changes for navigation headers and UI elements
        lifecycleScope.launch {
            profileViewModel.displayNameFlow.collect { displayName ->
                if (displayName != null) {
                    Log.d("PROFILE_REALTIME", "Display name updated app-wide: $displayName")
                    // Display name changes are automatically reflected through shared ViewModel
                }
            }
        }
    }

}