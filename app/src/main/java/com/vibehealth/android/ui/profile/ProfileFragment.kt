package com.vibehealth.android.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.vibehealth.android.databinding.FragmentProfileBinding
import com.vibehealth.android.domain.auth.AuthState
import com.vibehealth.android.ui.auth.AuthViewModel
import com.vibehealth.android.ui.base.AuthenticatedFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Profile fragment with logout functionality
 * Shows user information and provides access to settings
 */
@AndroidEntryPoint
class ProfileFragment : AuthenticatedFragment() {
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onAuthenticatedViewCreated(view: View, savedInstanceState: Bundle?) {
        setupUI()
        setupObservers()
    }
    
    private fun setupUI() {
        // Set up logout button
        binding.logoutButton.setOnClickListener {
            logout()
        }
    }
    
    private fun setupObservers() {
        // Observe authentication state
        authViewModel.authState.observe(viewLifecycleOwner) { authState ->
            when (authState) {
                is AuthState.Authenticated -> {
                    // Update UI with user information
                    binding.userEmail.text = authState.user.email ?: "No email"
                    binding.userName.text = authState.user.displayName ?: "User"
                    binding.logoutButton.isEnabled = true
                }
                is AuthState.NotAuthenticated -> {
                    // User will be redirected by MainActivity
                }
                is AuthState.Error -> {
                    // Handle error - disable logout button
                    binding.logoutButton.isEnabled = false
                }
                is AuthState.Loading -> {
                    // Show loading state
                    binding.logoutButton.isEnabled = false
                }
            }
        }
    }
    
    private fun logout() {
        lifecycleScope.launch {
            authViewModel.signOut()
            // Navigation will be handled by MainActivity auth observer
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}