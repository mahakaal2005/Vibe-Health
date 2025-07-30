package com.vibehealth.android.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.vibehealth.android.MainActivity
import com.vibehealth.android.R
import com.vibehealth.android.databinding.ActivityOnboardingBinding
import com.vibehealth.android.domain.onboarding.OnboardingStep
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Onboarding activity with navigation and progress management
 */
@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val viewModel: OnboardingViewModel by viewModels()
    
    @javax.inject.Inject
    lateinit var integrationManager: com.vibehealth.android.core.integration.OnboardingIntegrationManager

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleBackNavigation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupObservers()
        setupBackPressedHandler()
        
        // Restore state if needed
        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        } else {
            // Start with welcome screen
            navigateToStep(OnboardingStep.WELCOME)
        }
    }

    private fun setupUI() {
        // Configure system UI
        setupSystemUI()
        
        // Setup toolbar if needed
        setupToolbar()
    }

    private fun setupSystemUI() {
        // Set status bar color to sage green
        window.statusBarColor = ContextCompat.getColor(this, R.color.sage_green)
        
        // Set navigation bar color
        window.navigationBarColor = ContextCompat.getColor(this, android.R.color.transparent)
        
        // Keep screen on during onboarding
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Remove problematic fullscreen flags that cause scaling issues
        // Use standard system UI like AuthActivity
    }

    private fun setupToolbar() {
        // Hide action bar since we're using custom toolbar in fragments
        supportActionBar?.hide()
    }

    private fun setupObservers() {
        // Observe current step changes
        viewModel.currentStep.observe(this) { step ->
            navigateToStep(step)
        }

        // Observe navigation events - this was missing!
        viewModel.navigationEvent.observe(this) { event ->
            when (event) {
                is OnboardingNavigationEvent.OnboardingComplete -> {
                    // Navigate to main app when onboarding is complete
                    navigateToMainApp()
                }
                else -> {
                    // Handle other navigation events if needed
                }
            }
        }
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun navigateToStep(step: OnboardingStep) {
        val fragment = createFragmentForStep(step)
        val tag = step.name

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .commit()
    }

    private fun createFragmentForStep(step: OnboardingStep): Fragment {
        return when (step) {
            OnboardingStep.WELCOME -> WelcomeFragment.newInstance()
            OnboardingStep.PERSONAL_INFO -> PersonalInfoFragment.newInstance()
            OnboardingStep.PHYSICAL_INFO -> PhysicalInfoFragment.newInstance()
            OnboardingStep.COMPLETION -> CompletionFragment.newInstance()
        }
    }

    private fun animateStepTransition(isForward: Boolean) {
        // Simplified - remove animations for now to focus on basic navigation
        // Animations can be added back once basic navigation is working
    }

    private fun updateProgressBar(progress: Float) {
        // Update progress bar if it exists in the current fragment
        // This is handled by individual fragments, but we could add a global progress bar here
    }

    private fun handleBackNavigation() {
        val currentStep = viewModel.currentStep.value ?: OnboardingStep.WELCOME
        
        when (currentStep) {
            OnboardingStep.WELCOME -> {
                // Exit onboarding
                showExitConfirmation()
            }
            else -> {
                // Navigate to previous step
                viewModel.navigateToPreviousStep()
            }
        }
    }

    private fun showExitConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Exit Onboarding")
            .setMessage("Are you sure you want to exit? Your progress will be lost.")
            .setPositiveButton("Exit") { _, _ ->
                finish()
            }
            .setNegativeButton("Continue", null)
            .show()
    }

    private fun handleErrorRecovery(event: ErrorRecoveryEvent) {
        when (event) {
            is ErrorRecoveryEvent.NetworkError -> {
                showNetworkErrorDialog()
            }
            is ErrorRecoveryEvent.StorageError -> {
                showStorageErrorDialog()
            }
            is ErrorRecoveryEvent.MaxRetriesExceeded -> {
                showMaxRetriesDialog()
            }
            is ErrorRecoveryEvent.UnknownError -> {
                showUnknownErrorDialog(event.message)
            }
        }
    }

    private fun showNetworkErrorDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Network Error")
            .setMessage("Please check your internet connection and try again.")
            .setPositiveButton("Retry") { _, _ ->
                viewModel.retryOnboardingCompletion()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showStorageErrorDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Storage Error")
            .setMessage("Unable to save your information. Please try again.")
            .setPositiveButton("Retry") { _, _ ->
                viewModel.retryOnboardingCompletion()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMaxRetriesDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Unable to Complete")
            .setMessage("We're having trouble completing your onboarding. Please try again later or contact support.")
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .show()
    }

    private fun showUnknownErrorDialog(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage("Something went wrong: $message")
            .setPositiveButton("Retry") { _, _ ->
                viewModel.retryOnboardingCompletion()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToMainApp() {
        // Use direct navigation to avoid async issues
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        // Add extra to indicate user just completed onboarding
        intent.putExtra("onboarding_just_completed", true)
        
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        
        // Save current state - simplified approach
        // The ViewModel handles its own state persistence
        // Additional state saving can be added here as needed
    }

    private fun restoreState(savedInstanceState: Bundle) {
        // Restore state from Bundle - simplified approach
        // The ViewModel will handle state restoration through its own mechanisms
        // Additional state restoration can be added here as needed
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Clear keep screen on flag
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val EXTRA_SKIP_WELCOME = "skip_welcome"
        
        fun createIntent(context: android.content.Context, skipWelcome: Boolean = false): Intent {
            return Intent(context, OnboardingActivity::class.java).apply {
                putExtra(EXTRA_SKIP_WELCOME, skipWelcome)
            }
        }
    }
}