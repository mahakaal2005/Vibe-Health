package com.vibehealth.android.ui.base

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.vibehealth.android.core.auth.AuthGuard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Base fragment that ensures user is authenticated before showing content
 */
@AndroidEntryPoint
abstract class AuthenticatedFragment : Fragment() {
    
    @Inject
    lateinit var authGuard: AuthGuard
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Check authentication when fragment is created
        lifecycleScope.launch {
            if (!authGuard.requireAuthentication()) {
                // User will be redirected to auth, no need to continue
                return@launch
            }
            
            // User is authenticated, proceed with fragment setup
            onAuthenticatedViewCreated(view, savedInstanceState)
        }
    }
    
    /**
     * Called after authentication is verified
     * Override this instead of onViewCreated in authenticated fragments
     */
    abstract fun onAuthenticatedViewCreated(view: View, savedInstanceState: Bundle?)
}