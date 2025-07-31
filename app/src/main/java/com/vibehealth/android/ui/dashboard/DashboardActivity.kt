package com.vibehealth.android.ui.dashboard

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vibehealth.android.databinding.ActivityDashboardBinding
import com.vibehealth.android.ui.dashboard.models.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Main dashboard activity hosting the triple-ring display system.
 * Serves as the primary visual interface for users to track daily wellness progress.
 * 
 * Following Story 1.4 requirements:
 * - Displays three distinct rings for steps, calories, and heart points
 * - Integrates with Story 1.3 goal calculation service
 * - Follows Material Design 3 and UI/UX specifications
 */
@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupObservers()
        setupUI()
        
        // Start dashboard data updates
        viewModel.startDashboardUpdates()
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.dashboardState.collect { state ->
                updateDashboardUI(state)
            }
        }
        
        lifecycleScope.launch {
            viewModel.animationTrigger.collect { event ->
                event?.let { handleAnimationEvent(it) }
            }
        }
    }
    
    private fun setupUI() {
        // Set up date and greeting
        setupDateAndGreeting()
        
        // Set up click listeners
        binding.retryButton.setOnClickListener {
            viewModel.refreshDashboard()
        }
        
        binding.setupButton.setOnClickListener {
            // Navigate to profile setup (would be implemented with navigation)
            // For now, just refresh to try loading goals
            viewModel.refreshDashboard()
        }
        
        binding.refreshFab.setOnClickListener {
            viewModel.refreshDashboard()
        }
    }
    
    private fun setupDateAndGreeting() {
        val today = java.time.LocalDate.now()
        val dateFormatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d")
        binding.dateText.text = today.format(dateFormatter)
        
        val hour = java.time.LocalTime.now().hour
        val greeting = when (hour) {
            in 5..11 -> "Good morning! Ready to start your wellness journey?"
            in 12..16 -> "Good afternoon! How's your progress today?"
            in 17..20 -> "Good evening! Let's finish strong today!"
            else -> "Your wellness journey continues"
        }
        binding.greetingText.text = greeting
    }
    
    private fun updateDashboardUI(state: DashboardState) {
        when (state.loadingState) {
            LoadingState.LOADING -> showLoadingState()
            LoadingState.LOADED -> showLoadedState(state)
            LoadingState.ERROR -> showErrorState(state.errorState)
            LoadingState.EMPTY -> showEmptyState()
        }
    }
    
    private fun showLoadingState() {
        binding.loadingIndicator.visibility = android.view.View.VISIBLE
        binding.tripleRingView.visibility = android.view.View.GONE
        binding.progressSummaryContainer.visibility = android.view.View.GONE
        binding.errorStateContainer.visibility = android.view.View.GONE
        binding.emptyStateContainer.visibility = android.view.View.GONE
        binding.goalSourceText.visibility = android.view.View.GONE
    }
    
    private fun showLoadedState(state: DashboardState) {
        binding.loadingIndicator.visibility = android.view.View.GONE
        binding.tripleRingView.visibility = android.view.View.VISIBLE
        binding.progressSummaryContainer.visibility = android.view.View.VISIBLE
        binding.errorStateContainer.visibility = android.view.View.GONE
        binding.emptyStateContainer.visibility = android.view.View.GONE
        binding.goalSourceText.visibility = android.view.View.VISIBLE
        
        // Update triple ring view
        val ringData = state.progress.getAllProgress().map { progressData ->
            RingDisplayData.fromProgressData(progressData)
        }
        binding.tripleRingView.updateProgress(ringData, true)
        
        // Update goal source text
        state.goals?.let { goals ->
            binding.goalSourceText.text = goals.calculationSource.getDisplayName()
        }
        
        // Update progress summary cards (would be implemented with card components)
        updateProgressSummaryCards(state.progress)
    }
    
    private fun showErrorState(errorState: ErrorState?) {
        binding.loadingIndicator.visibility = android.view.View.GONE
        binding.tripleRingView.visibility = android.view.View.GONE
        binding.progressSummaryContainer.visibility = android.view.View.GONE
        binding.errorStateContainer.visibility = android.view.View.VISIBLE
        binding.emptyStateContainer.visibility = android.view.View.GONE
        binding.goalSourceText.visibility = android.view.View.GONE
        
        binding.errorMessage.text = errorState?.message ?: "Unable to load your wellness data"
        binding.retryButton.visibility = if (errorState?.isRetryable == true) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }
    
    private fun showEmptyState() {
        binding.loadingIndicator.visibility = android.view.View.GONE
        binding.tripleRingView.visibility = android.view.View.GONE
        binding.progressSummaryContainer.visibility = android.view.View.GONE
        binding.errorStateContainer.visibility = android.view.View.GONE
        binding.emptyStateContainer.visibility = android.view.View.VISIBLE
        binding.goalSourceText.visibility = android.view.View.GONE
    }
    
    private fun updateProgressSummaryCards(progress: DailyProgress) {
        // This would update the individual progress summary cards
        // For now, we'll leave this as a placeholder since we need to implement the cards
    }
    
    private fun handleAnimationEvent(event: AnimationEvent) {
        when (event) {
            is AnimationEvent.ProgressUpdate -> {
                // Progress animations are handled by the TripleRingView itself
            }
            is AnimationEvent.GoalAchieved -> {
                binding.tripleRingView.celebrateGoalAchievement(event.achievedRings)
            }
            is AnimationEvent.DataRefreshed -> {
                // Could show a subtle refresh animation
            }
        }
    }
}