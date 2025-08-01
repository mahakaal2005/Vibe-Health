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
    
    // Enhanced animation management
    private lateinit var animationManager: com.vibehealth.android.ui.dashboard.DashboardAnimationManager
    private lateinit var accessibilityManager: com.vibehealth.android.ui.accessibility.EnhancedAccessibilityManager
    private var isInitialized = false
    
    // Motivational quotes removed for clean ring design
    
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
        
        // Initialize managers
        animationManager = com.vibehealth.android.ui.dashboard.DashboardAnimationManager()
        accessibilityManager = com.vibehealth.android.ui.accessibility.EnhancedAccessibilityManager(requireContext())
        
        // Add animation manager to lifecycle
        lifecycle.addObserver(animationManager)
        
        setupUI()
        setupObservers()
        
        // Only initialize once to prevent double animations
        if (!isInitialized) {
            isInitialized = true
            
            // Initialize dashboard with proper goal injection if needed
            initializeDashboard()
            
            // Start dashboard data updates
            dashboardViewModel.startDashboardUpdates()
        }
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
        
        // Motivational quote removed for clean ring design
        
        // Setup enhanced refresh button with animations
        binding.refreshFab.setOnClickListener {
            // Add rotation animation
            binding.refreshFab.animate()
                .rotationBy(360f)
                .setDuration(500)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
            
            // Add bounce effect
            binding.refreshFab.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction {
                    binding.refreshFab.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                }
                .start()
            
            dashboardViewModel.refreshDashboard()
        }
        
        // Add tooltip functionality
        binding.refreshFab.setOnLongClickListener {
            android.widget.Toast.makeText(context, "Sync Data", android.widget.Toast.LENGTH_SHORT).show()
            true
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
        
        // Update triple ring view with enhanced animations
        val ringData = state.progress.getAllProgress().map { progressData ->
            RingDisplayData.fromProgressData(progressData)
        }
        
        // Use enhanced animation manager
        if (animationManager.shouldAnimateEntrance()) {
            // Initially show rings at 0% for fill-up animation
            val zeroProgressData = ringData.map { it.copy(progress = 0f) }
            binding.tripleRingView.updateProgress(zeroProgressData, false)
            
            // Animate ring entrance
            animationManager.animateRingEntrance(binding.tripleRingView) {
                // Start ring fill-up animation after entrance
                animationManager.animateRingFillUp(binding.tripleRingView, ringData)
            }
        } else {
            // Subsequent updates - just update without entrance animation
            binding.tripleRingView.alpha = 1f
            binding.tripleRingView.scaleX = 1f
            binding.tripleRingView.scaleY = 1f
            binding.tripleRingView.updateProgress(ringData, true) // Still animate progress changes
        }
        
        // Enhance accessibility
        val progressDataList = listOf(
            state.progress.stepsProgress,
            state.progress.caloriesProgress,
            state.progress.heartPointsProgress
        )
        accessibilityManager.enhanceRingAccessibility(binding.tripleRingView, progressDataList)
        
        // Update goal source text
        state.goals?.let { goals ->
            binding.goalSourceText.text = goals.calculationSource.getDisplayName()
        }
        
        // Update progress summary cards with staggered entrance animation
        val cards = listOf(
            Triple(binding.stepsCard, binding.stepsText, state.progress.stepsProgress to RingType.STEPS),
            Triple(binding.caloriesCard, binding.caloriesText, state.progress.caloriesProgress to RingType.CALORIES),
            Triple(binding.heartPointsCard, binding.heartPointsText, state.progress.heartPointsProgress to RingType.HEART_POINTS)
        )
        
        cards.forEachIndexed { index, (cardView, textView, progressPair) ->
            val (progressData, ringType) = progressPair
            updateProgressCard(cardView, textView, progressData, ringType)
            
            // Enhance card accessibility
            val materialCardView = cardView as com.google.android.material.card.MaterialCardView
            accessibilityManager.enhanceCardAccessibility(materialCardView, progressData, ringType)
            
            // Apply visual hierarchy based on progress
            val isHighestProgress = isHighestProgressCard(progressData)
            animationManager.animateCardEmphasis(materialCardView, isHighestProgress)
        }
        
        // Animate card entrance using the animation manager
        val cardViews = cards.map { it.first }
        animationManager.animateCardEntrance(cardViews)
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
        updateProgressCard(binding.stepsCard, binding.stepsText, progress.stepsProgress, RingType.STEPS)
        updateProgressCard(binding.caloriesCard, binding.caloriesText, progress.caloriesProgress, RingType.CALORIES)
        updateProgressCard(binding.heartPointsCard, binding.heartPointsText, progress.heartPointsProgress, RingType.HEART_POINTS)
    }
    
    private fun updateProgressCard(cardView: View, textView: android.widget.TextView, progressData: ProgressData, ringType: RingType) {
        // Update card visibility
        cardView.visibility = View.VISIBLE
        
        // Update main text (title)
        textView.text = ringType.displayName
        
        // Get number and percentage views for this card
        val (numberView, percentageView) = when (ringType) {
            RingType.STEPS -> Pair(binding.stepsNumber, binding.stepsPercentage)
            RingType.CALORIES -> Pair(binding.caloriesNumber, binding.caloriesPercentage)
            RingType.HEART_POINTS -> Pair(binding.heartPointsNumber, binding.heartPointsPercentage)
        }
        
        // Update actual numbers with proper formatting
        val formattedNumber = when (ringType) {
            RingType.STEPS -> {
                val steps = progressData.current.toInt()
                when {
                    steps >= 1000 -> "${steps / 1000},${String.format("%03d", steps % 1000)}"
                    else -> steps.toString()
                }
            }
            RingType.CALORIES -> "${progressData.current.toInt()} kcal"
            RingType.HEART_POINTS -> "${progressData.current.toInt()} pts"
        }
        numberView.text = formattedNumber
        
        // Update percentage
        val percentage = (progressData.percentage * 100).toInt()
        percentageView.text = "${percentage}%"
        
        // Update card appearance based on progress
        val materialCardView = cardView as com.google.android.material.card.MaterialCardView
        updateCardHierarchy(materialCardView, progressData, ringType)
    }
    
    private fun updateCardHierarchy(
        cardView: com.google.android.material.card.MaterialCardView,
        progressData: ProgressData,
        ringType: RingType
    ) {
        val isHighestProgress = isHighestProgressCard(progressData)
        
        if (isHighestProgress) {
            // Highlight the card with highest progress
            cardView.cardElevation = dpToPx(4f)
            cardView.setCardBackgroundColor(getHighlightColor(ringType))
            
            // Add subtle glow animation
            cardView.animate()
                .scaleX(1.02f)
                .scaleY(1.02f)
                .setDuration(200)
                .start()
        } else {
            // Normal card appearance
            cardView.cardElevation = dpToPx(2f)
            cardView.setCardBackgroundColor(android.graphics.Color.WHITE)
            
            // Reset scale
            cardView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .start()
        }
    }
    
    private fun isHighestProgressCard(currentProgress: ProgressData): Boolean {
        // This would be implemented to compare with other cards' progress
        // For now, highlight cards with >75% progress
        return currentProgress.percentage > 0.75f
    }
    
    private fun getHighlightColor(ringType: RingType): Int {
        return when (ringType) {
            RingType.STEPS -> android.graphics.Color.parseColor("#E8F5E8") // Light sage green
            RingType.CALORIES -> android.graphics.Color.parseColor("#F0F0E8") // Light warm gray
            RingType.HEART_POINTS -> android.graphics.Color.parseColor("#F5E8E8") // Light coral
        }
    }
    
    // Animation methods moved to DashboardAnimationManager
    
    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
    
    // Helper methods for card management
    
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
        // Only refresh if we're returning from background, not initial load
        if (dashboardViewModel.dashboardState.value.loadingState != LoadingState.LOADING) {
            dashboardViewModel.refreshDashboard()
        }
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
    
    // Motivational quote methods removed for clean ring design
    
    override fun onDestroyView() {
        super.onDestroyView()
        dashboardViewModel.stopDashboardUpdates()
        _binding = null
    }
}