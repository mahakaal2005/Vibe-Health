package com.vibehealth.android.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import android.widget.TextView
import com.vibehealth.android.databinding.FragmentHomeBinding
import com.vibehealth.android.ui.dashboard.DashboardViewModel
import com.vibehealth.android.ui.dashboard.models.*
import com.vibehealth.android.domain.goals.GoalCalculationUseCase
import com.vibehealth.android.domain.auth.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Home fragment with integrated triple-ring dashboard display.
 * Shows personalized wellness progress with animated rings and progress cards.
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {
    
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val dashboardViewModel: DashboardViewModel by viewModels()
    
    @Inject
    lateinit var goalCalculationUseCase: GoalCalculationUseCase
    
    @Inject
    lateinit var authRepository: AuthRepository
    
    @Inject
    lateinit var firestoreDebugHelper: com.vibehealth.android.debug.FirestoreDebugHelper
    
    @Inject
    lateinit var synchronousGoalCreator: com.vibehealth.android.debug.SynchronousGoalCreator
    
    @Inject
    lateinit var personalizedGoalCreator: com.vibehealth.android.debug.PersonalizedGoalCreator
    
    @Inject
    lateinit var databaseMigrationTester: com.vibehealth.android.debug.DatabaseMigrationTester
    
    @Inject
    lateinit var foreignKeyDebugger: com.vibehealth.android.debug.ForeignKeyDebugger
    
    // Progress summary cards - we'll access them through the binding
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupObservers()
        
        // Initialize dashboard with proper goal injection if needed
        initializeDashboard()
        
        // Start dashboard data updates
        dashboardViewModel.startDashboardUpdates()
    }
    
    private fun setupUI() {
        // Set current date
        val currentDate = LocalDateTime.now()
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
        binding.dateText.text = currentDate.format(dateFormatter)
        
        // Set time-based greeting
        val hour = LocalDateTime.now().hour
        val greeting = when (hour) {
            in 5..11 -> "Good morning! Ready to start your wellness journey?"
            in 12..16 -> "Good afternoon! How's your progress today?"
            in 17..20 -> "Good evening! Let's finish strong today!"
            else -> "Your wellness journey continues"
        }
        binding.greetingText.text = greeting
        
        // Setup refresh button
        binding.refreshFab.setOnClickListener {
            dashboardViewModel.refreshDashboard()
        }
        
        // Setup retry button
        binding.retryButton.setOnClickListener {
            dashboardViewModel.refreshDashboard()
        }
        
        // Setup empty state button - will be overridden in showEmptyState()
        binding.setupButton.setOnClickListener {
            // This will be overridden in showEmptyState() method
        }
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            dashboardViewModel.dashboardState.collect { state ->
                android.util.Log.d("HomeFragment", "Dashboard state: ${state.loadingState}")
                if (state.errorState != null) {
                    android.util.Log.e("HomeFragment", "Dashboard error: ${state.errorState}")
                }
                updateDashboardUI(state)
            }
        }
        
        lifecycleScope.launch {
            dashboardViewModel.animationTrigger.collect { event ->
                event?.let { handleAnimationEvent(it) }
            }
        }
    }
    

    
    private fun updateDashboardUI(state: DashboardState) {
        android.util.Log.d("HomeFragment", "Updating dashboard UI with state: ${state.loadingState}")
        when (state.loadingState) {
            LoadingState.LOADING -> {
                showLoadingState()
            }
            LoadingState.LOADED -> {
                showLoadedState(state)
            }
            LoadingState.ERROR -> {
                showErrorState(state.errorState)
            }
            LoadingState.EMPTY -> {
                showEmptyState()
            }
        }
    }
    
    private fun showLoadingState() {
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.tripleRingView.visibility = View.GONE
        binding.progressSummaryContainer.visibility = View.GONE
        binding.errorStateContainer.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.GONE
    }
    
    private fun showLoadedState(state: DashboardState) {
        android.util.Log.d("HomeFragment", "Showing loaded state with goals: ${state.goals}")
        
        binding.loadingIndicator.visibility = View.GONE
        binding.tripleRingView.visibility = View.VISIBLE
        binding.progressSummaryContainer.visibility = View.VISIBLE
        binding.errorStateContainer.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.GONE
        binding.goalSourceText.visibility = View.VISIBLE
        
        // Update triple ring view with complete data
        val ringData = state.progress.getAllProgress().map { progressData ->
            RingDisplayData.fromProgressData(progressData)
        }
        binding.tripleRingView.updateProgress(ringData, true)
        
        // Update goal source text
        state.goals?.let { goals ->
            binding.goalSourceText.text = goals.calculationSource.getDisplayName()
        }
        
        // Update progress summary cards
        updateProgressCard(binding.stepsCard, binding.stepsCardText, state.progress.stepsProgress, RingType.STEPS)
        updateProgressCard(binding.caloriesCard, binding.caloriesCardText, state.progress.caloriesProgress, RingType.CALORIES)
        updateProgressCard(binding.heartPointsCard, binding.heartPointsCardText, state.progress.heartPointsProgress, RingType.HEART_POINTS)
    }
    
    private fun showErrorState(errorState: ErrorState?) {
        binding.loadingIndicator.visibility = View.GONE
        binding.tripleRingView.visibility = View.GONE
        binding.progressSummaryContainer.visibility = View.GONE
        binding.errorStateContainer.visibility = View.VISIBLE
        binding.emptyStateContainer.visibility = View.GONE
        
        errorState?.let { error ->
            binding.errorMessage.text = error.message
            binding.retryButton.visibility = if (error.isRetryable) View.VISIBLE else View.GONE
        }
    }
    
    private fun showEmptyState() {
        android.util.Log.d("HomeFragment", "Showing empty state - no goals found")
        binding.loadingIndicator.visibility = View.GONE
        binding.tripleRingView.visibility = View.GONE
        binding.progressSummaryContainer.visibility = View.GONE
        binding.errorStateContainer.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.VISIBLE
        binding.goalSourceText.visibility = View.GONE
        
        // Add click listener to setup button to trigger personalized goal calculation
        binding.setupButton.setOnClickListener {
            android.util.Log.d("HomeFragment", "Setup button clicked - triggering personalized goal creation")
            
            // Show loading state
            binding.loadingIndicator.visibility = View.VISIBLE
            binding.emptyStateContainer.visibility = View.GONE
            
            // Create personalized goals
            val success = personalizedGoalCreator.createPersonalizedGoals()
            android.util.Log.d("HomeFragment", "Setup button - Personalized goals created: $success")
            
            // Wait a moment then refresh dashboard
            lifecycleScope.launch {
                kotlinx.coroutines.delay(1000)
                dashboardViewModel.refreshDashboard()
            }
        }
    }
    
    private fun updateProgressSummaryCards(progress: DailyProgress) {
        updateProgressCard(binding.stepsCard, binding.stepsCardText, progress.stepsProgress, RingType.STEPS)
        updateProgressCard(binding.caloriesCard, binding.caloriesCardText, progress.caloriesProgress, RingType.CALORIES)
        updateProgressCard(binding.heartPointsCard, binding.heartPointsCardText, progress.heartPointsProgress, RingType.HEART_POINTS)
    }
    
    private fun updateProgressCard(cardView: View, textView: android.widget.TextView, progressData: ProgressData, ringType: RingType) {
        // Update card visibility
        cardView.visibility = View.VISIBLE
        
        // Update card content with progress information
        val progressText = "${progressData.current} / ${progressData.target} ${ringType.unit}\n${(progressData.percentage * 100).toInt()}%"
        textView.text = progressText
        
        // Update card color based on progress
        val materialCardView = cardView as com.google.android.material.card.MaterialCardView
        val color = if (progressData.isGoalAchieved) {
            android.graphics.Color.parseColor("#E8F5E8") // Light green for achieved goals
        } else {
            android.graphics.Color.parseColor("#FFFFFF") // White for in-progress goals
        }
        materialCardView.setCardBackgroundColor(color)
    }
    
    private fun handleAnimationEvent(event: AnimationEvent) {
        when (event) {
            is AnimationEvent.ProgressUpdate -> {
                // Handle progress update animations
                // The updateProgress method with animate=true will handle the animations
                val currentState = dashboardViewModel.dashboardState.value
                if (currentState.loadingState == LoadingState.LOADED) {
                    showLoadedState(currentState)
                }
            }
            is AnimationEvent.GoalAchieved -> {
                // Handle goal achievement celebrations
                binding.tripleRingView.celebrateGoalAchievement(event.achievedRings)
            }
            is AnimationEvent.DataRefreshed -> {
                // Handle data refresh animation
                // Could add a subtle refresh animation here if needed
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        dashboardViewModel.refreshDashboard()
    }
    
    override fun onPause() {
        super.onPause()
        // Pause updates when fragment is not visible
        dashboardViewModel.stopDashboardUpdates()
    }
    
    private fun triggerGoalCalculation() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("HomeFragment", "Attempting to trigger goal calculation...")
                
                // Show loading state
                binding.loadingIndicator.visibility = View.VISIBLE
                binding.emptyStateContainer.visibility = View.GONE
                
                // Get current user
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    android.util.Log.e("HomeFragment", "No authenticated user found")
                    showErrorMessage("Please log in again")
                    return@launch
                }
                
                android.util.Log.d("HomeFragment", "Triggering goal calculation for user: ${currentUser.uid}")
                
                // Trigger goal calculation
                val result = goalCalculationUseCase.calculateAndStoreGoals(currentUser.uid)
                
                when (result) {
                    is com.vibehealth.android.domain.goals.GoalCalculationResult.Success -> {
                        android.util.Log.d("HomeFragment", "Goal calculation successful")
                        // Refresh dashboard to show new goals
                        dashboardViewModel.refreshDashboard()
                    }
                    is com.vibehealth.android.domain.goals.GoalCalculationResult.Error -> {
                        android.util.Log.e("HomeFragment", "Goal calculation failed: ${result.message}")
                        showErrorMessage("Failed to calculate goals: ${result.message}")
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Failed to trigger goal calculation", e)
                showErrorMessage("Error: ${e.message}")
            }
        }
    }
    
    private fun showErrorMessage(message: String) {
        binding.loadingIndicator.visibility = View.GONE
        binding.emptyStateContainer.visibility = View.VISIBLE
        binding.emptyMessage.text = message
    }
    
    private fun debugUserProfile() {
        lifecycleScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                android.util.Log.d("HomeFragment", "Current user: $currentUser")
                
                if (currentUser != null) {
                    // TODO: Add UserRepository injection to check profile data
                    android.util.Log.d("HomeFragment", "User ID: ${currentUser.uid}")
                    android.util.Log.d("HomeFragment", "User email: ${currentUser.email}")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Error debugging user profile", e)
            }
        }
    }
    
    /**
     * Initialize dashboard with proper goal setup
     */
    private fun initializeDashboard() {
        android.util.Log.d("HomeFragment", "🚀 Initializing dashboard...")
        
        // Test database migration first
        lifecycleScope.launch {
            try {
                // Test database migration and foreign key constraints
                val migrationSuccess = databaseMigrationTester.testDatabaseMigration()
                android.util.Log.d("HomeFragment", "🧪 Database migration test result: $migrationSuccess")
                
                if (!migrationSuccess) {
                    android.util.Log.w("HomeFragment", "⚠️ Database migration test failed - proceeding with caution")
                }
                
                // Log database statistics
                val stats = databaseMigrationTester.getDatabaseStats()
                android.util.Log.d("HomeFragment", "📊 Database stats: $stats")
                
                // If migration test passed but we still have issues, run detailed foreign key debugging
                if (migrationSuccess) {
                    val debugResult = foreignKeyDebugger.debugForeignKeyConstraints()
                    android.util.Log.d("HomeFragment", "🔍 Foreign key debug result: $debugResult")
                }
                // Quick profile check
                val isComplete = com.vibehealth.android.debug.ManualFirestoreTest.quickProfileCheck()
                if (isComplete) {
                    android.util.Log.d("HomeFragment", "✅ Profile is complete")
                    
                    // Create personalized goals based on user profile
                    android.util.Log.d("HomeFragment", "🎯 About to call personalizedGoalCreator.createPersonalizedGoals()")
                    try {
                        val goalsCreated = personalizedGoalCreator.createPersonalizedGoals()
                        android.util.Log.d("HomeFragment", "🎯 Personalized goals creation result: $goalsCreated")
                        
                        if (!goalsCreated) {
                            android.util.Log.d("HomeFragment", "🔄 Personalized goals failed, using synchronous fallback...")
                            val fallbackResult = synchronousGoalCreator.createGoalsNow()
                            android.util.Log.d("HomeFragment", "🎯 Synchronous fallback result: $fallbackResult")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("HomeFragment", "🎯 Exception in personalizedGoalCreator", e)
                        // Fallback to synchronous goal creator
                        val fallbackResult = synchronousGoalCreator.createGoalsNow()
                        android.util.Log.d("HomeFragment", "🎯 Exception fallback result: $fallbackResult")
                    }
                    
                    // Small delay then refresh
                    kotlinx.coroutines.delay(1000)
                    dashboardViewModel.refreshDashboard()
                } else {
                    android.util.Log.d("HomeFragment", "⚠️ Profile incomplete - running fix and creating fallback goals")
                    com.vibehealth.android.debug.ManualFirestoreTest.runCompleteTest()
                    // Use personalized goal creator which will create fallback goals if profile is incomplete
                    android.util.Log.d("HomeFragment", "🎯 About to call personalizedGoalCreator (fallback case)")
                    try {
                        val goalsCreated = personalizedGoalCreator.createPersonalizedGoals()
                        android.util.Log.d("HomeFragment", "🎯 Fallback personalized goals creation result: $goalsCreated")
                    } catch (e: Exception) {
                        android.util.Log.e("HomeFragment", "🎯 Exception in personalizedGoalCreator (fallback)", e)
                        // Fallback to synchronous goal creator
                        val fallbackResult = synchronousGoalCreator.createGoalsNow()
                        android.util.Log.d("HomeFragment", "🎯 Final fallback goals creation result: $fallbackResult")
                    }
                    kotlinx.coroutines.delay(1000)
                    dashboardViewModel.refreshDashboard()
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "❌ Dashboard initialization failed", e)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        dashboardViewModel.stopDashboardUpdates()
        _binding = null
    }
}