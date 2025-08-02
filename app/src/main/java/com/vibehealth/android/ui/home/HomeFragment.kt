package com.vibehealth.android.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.vibehealth.android.databinding.FragmentHomeBinding
import com.vibehealth.android.ui.dashboard.DashboardViewModel
import com.vibehealth.android.ui.dashboard.models.*
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
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
    private var isInitialized = false
    
    
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("VIBE_FIX_CRASH", "VIBE_FIX: HomeFragment onCreateView() started")
        
        try {
            _binding = FragmentHomeBinding.inflate(inflater, container, false)
            Log.d("VIBE_FIX_CRASH", "VIBE_FIX: HomeFragment binding inflated successfully")
            return binding.root
        } catch (e: Exception) {
            Log.e("VIBE_FIX_CRASH", "VIBE_FIX: FATAL ERROR in HomeFragment onCreateView()", e)
            throw e
        }
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("VIBE_FIX_CRASH", "VIBE_FIX: HomeFragment onViewCreated() started")
        
        try {
            super.onViewCreated(view, savedInstanceState)
            Log.d("VIBE_FIX_CRASH", "VIBE_FIX: super.onViewCreated() completed")
            
            android.util.Log.d("VIBE_FIX", "Phase 1: HomeFragment onViewCreated - Basic setup")
            
            setupBasicUI()
            Log.d("VIBE_FIX_CRASH", "VIBE_FIX: setupBasicUI() completed")
            
            setupObservers()
            Log.d("VIBE_FIX_CRASH", "VIBE_FIX: setupObservers() completed")
            
            // Only initialize once to prevent double animations
            if (!isInitialized) {
                Log.d("VIBE_FIX_CRASH", "VIBE_FIX: Starting initialization")
                isInitialized = true
                android.util.Log.d("VIBE_FIX", "Phase 3: HomeFragment initialized with ViewModel")
                
                // Start dashboard updates
                dashboardViewModel.startDashboardUpdates()
            }
            
        } catch (e: Exception) {
            Log.e("VIBE_FIX_CRASH", "VIBE_FIX: FATAL ERROR in HomeFragment onViewCreated()", e)
            throw e
        }
    }
    
    private fun setupBasicUI() {
        android.util.Log.d("VIBE_FIX", "Phase 1: Setting up basic UI")
        
        try {
            // Set current date
            val currentDate = LocalDateTime.now()
            val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
            binding.dateText.text = currentDate.format(dateFormatter)
            
            // Set personalized greeting based on time of day
            val greeting = getPersonalizedGreeting()
            binding.greetingText.text = greeting
            
            // Enhanced refresh button with animation feedback
            binding.refreshFab.setOnClickListener {
                android.util.Log.d("VIBE_FIX", "Phase 3: Enhanced refresh button clicked")
                
                // Animate refresh feedback
                val animationManager = dashboardViewModel.getAnimationManager()
                animationManager.animateRefreshFeedback(binding.refreshFab) {
                    // Trigger dashboard refresh
                    dashboardViewModel.refreshDashboard()
                }
            }
            
            // Enhanced retry button with supportive feedback
            binding.retryButton.setOnClickListener {
                android.util.Log.d("VIBE_FIX", "Phase 3: Enhanced retry button clicked")
                
                // Provide encouraging feedback
                android.widget.Toast.makeText(context, "Let's try again! 💪", android.widget.Toast.LENGTH_SHORT).show()
                
                // Restart dashboard updates
                dashboardViewModel.startDashboardUpdates()
            }
            
            // Enhanced setup button with encouraging message
            binding.setupButton.setOnClickListener {
                android.util.Log.d("VIBE_FIX", "Phase 3: Enhanced setup button clicked")
                
                // Provide encouraging feedback
                android.widget.Toast.makeText(context, "Let's set up your wellness goals! ✨", android.widget.Toast.LENGTH_SHORT).show()
                
                // TODO: Navigate to onboarding/setup flow when available
                // For now, start dashboard updates
                dashboardViewModel.startDashboardUpdates()
            }
            
            android.util.Log.d("VIBE_FIX", "Phase 1: Basic UI setup completed successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("VIBE_FIX", "Phase 1: Error in setupBasicUI: ${e.message}", e)
        }
    }
    
    // VIBE_FIX: Phase 3 - Restored basic observers
    private fun setupObservers() {
        android.util.Log.d("VIBE_FIX", "Phase 3: Setting up enhanced dashboard observers")
        
        // Enhanced dashboard state observer with animation integration
        viewLifecycleOwner.lifecycleScope.launch {
            dashboardViewModel.dashboardState.collect { state ->
                android.util.Log.d("VIBE_FIX", "Phase 3: Dashboard state updated: ${state.loadingState}")
                updateEnhancedDashboardUI(state)
            }
        }
        
        // Animation trigger observer for celebrations and feedback
        viewLifecycleOwner.lifecycleScope.launch {
            dashboardViewModel.animationTrigger.collect { animationEvent ->
                animationEvent?.let { event ->
                    handleAnimationEvent(event)
                }
            }
        }
    }
    
    private fun updateEnhancedDashboardUI(state: DashboardState) {
        when (state.loadingState) {
            LoadingState.LOADING -> {
                android.util.Log.d("VIBE_FIX", "Phase 3: Showing enhanced loading state")
                showLoadingState()
            }
            LoadingState.LOADED -> {
                android.util.Log.d("VIBE_FIX", "Phase 3: Showing enhanced loaded state with animations")
                showLoadedState(state)
            }
            LoadingState.ERROR -> {
                android.util.Log.d("VIBE_FIX", "Phase 3: Showing enhanced error state")
                val errorMessage = state.errorState?.message ?: "Something went wrong"
                showErrorState(errorMessage)
            }
            LoadingState.EMPTY -> {
                android.util.Log.d("VIBE_FIX", "Phase 3: Showing enhanced empty state")
                showEmptyState()
            }
        }
    }
    
    /**
     * Shows loading state with encouraging message
     */
    private fun showLoadingState() {
        binding.loadingIndicator.visibility = android.view.View.VISIBLE
        binding.tripleRingView.visibility = android.view.View.GONE
        binding.progressSummaryContainer.visibility = android.view.View.GONE
        binding.errorStateContainer.visibility = android.view.View.GONE
        binding.emptyStateContainer.visibility = android.view.View.GONE
        
        // Update greeting with encouraging loading message
        binding.greetingText.text = "Loading your wellness journey..."
    }
    
    /**
     * Shows loaded state with enhanced animations
     */
    private fun showLoadedState(state: DashboardState) {
        // Hide loading and error states
        binding.loadingIndicator.visibility = android.view.View.GONE
        binding.errorStateContainer.visibility = android.view.View.GONE
        binding.emptyStateContainer.visibility = android.view.View.GONE
        
        // Get animation manager from ViewModel
        val animationManager = dashboardViewModel.getAnimationManager()
        
        // Show triple ring with entrance animation if first time
        if (animationManager.shouldAnimateEntrance()) {
            binding.tripleRingView.visibility = android.view.View.VISIBLE
            animationManager.animateRingEntrance(binding.tripleRingView) {
                // After ring entrance, animate the progress fill
                updateTripleRingDisplayWithAnimation(state.progress, animationManager)
                
                // Then animate the progress cards
                showProgressCardsWithAnimation(state.progress, animationManager)
            }
        } else {
            // Direct update without entrance animation
            binding.tripleRingView.visibility = android.view.View.VISIBLE
            updateTripleRingDisplayWithAnimation(state.progress, animationManager)
            showProgressCardsWithAnimation(state.progress, animationManager)
        }
        
        // Update motivational message
        updateMotivationalMessage(state.progress)
        
        // Hide WHO text for cleaner design
        binding.goalSourceText.visibility = android.view.View.GONE
        
        // Update greeting with personalized message
        binding.greetingText.text = getPersonalizedGreeting()
    }
    
    /**
     * Shows error state with supportive messaging
     */
    private fun showErrorState(errorMessage: String) {
        binding.loadingIndicator.visibility = android.view.View.GONE
        binding.tripleRingView.visibility = android.view.View.GONE
        binding.progressSummaryContainer.visibility = android.view.View.GONE
        binding.emptyStateContainer.visibility = android.view.View.GONE
        binding.errorStateContainer.visibility = android.view.View.VISIBLE
        
        // Update error message with supportive tone
        binding.errorMessage.text = errorMessage
        binding.greetingText.text = "We're here to help you get back on track"
    }
    
    /**
     * Shows empty state with encouraging setup message
     */
    private fun showEmptyState() {
        binding.loadingIndicator.visibility = android.view.View.GONE
        binding.tripleRingView.visibility = android.view.View.GONE
        binding.progressSummaryContainer.visibility = android.view.View.GONE
        binding.errorStateContainer.visibility = android.view.View.GONE
        binding.emptyStateContainer.visibility = android.view.View.VISIBLE
        
        // Update empty message with encouraging tone
        binding.emptyMessage.text = "Ready to start your wellness journey?"
        binding.greetingText.text = "Let's set up your personalized goals!"
    }
    
    /**
     * Shows progress cards with staggered animation
     */
    private fun showProgressCardsWithAnimation(progress: DailyProgress, animationManager: com.vibehealth.android.ui.dashboard.DashboardAnimationManager) {
        binding.progressSummaryContainer.visibility = android.view.View.VISIBLE
        
        // Update progress data first
        updateProgressSummary(progress)
        
        // Animate cards entrance
        val cardViews = listOf(
            binding.stepsCard,
            binding.caloriesCard,
            binding.heartPointsCard
        )
        
        animationManager.animateCardEntrance(cardViews)
        
        // Emphasize the highest progress card
        val highestProgressCard = getHighestProgressCard(progress)
        highestProgressCard?.let { card ->
            animationManager.animateCardEmphasis(card, true)
        }
    }
    
    /**
     * Gets the card with highest progress for emphasis
     */
    private fun getHighestProgressCard(progress: DailyProgress): android.view.View? {
        val progressValues = listOf(
            progress.stepsProgress.percentage to binding.stepsCard,
            progress.caloriesProgress.percentage to binding.caloriesCard,
            progress.heartPointsProgress.percentage to binding.heartPointsCard
        )
        
        return progressValues.maxByOrNull { it.first }?.second
    }
    
    /**
     * Updates triple ring display with enhanced animation support
     */
    private fun updateTripleRingDisplayWithAnimation(progress: DailyProgress, animationManager: com.vibehealth.android.ui.dashboard.DashboardAnimationManager) {
        android.util.Log.d("VIBE_FIX", "Phase 3: Updating triple ring display with animations")
        
        try {
            android.util.Log.d("VIBE_FIX", "Phase 3: Creating RingDisplayData - Steps: ${progress.stepsProgress.percentage}, Calories: ${progress.caloriesProgress.percentage}, Heart: ${progress.heartPointsProgress.percentage}")
            
            // Create RingDisplayData for each ring
            val stepsRingData = RingDisplayData.fromProgressData(progress.stepsProgress)
            val caloriesRingData = RingDisplayData.fromProgressData(progress.caloriesProgress)
            val heartPointsRingData = RingDisplayData.fromProgressData(progress.heartPointsProgress)
            
            val ringDataList = listOf(stepsRingData, caloriesRingData, heartPointsRingData)
            
            android.util.Log.d("VIBE_FIX", "Phase 3: About to call animateRingFillUp with ${ringDataList.size} rings")
            
            // Animate ring fill-up
            animationManager.animateRingFillUp(binding.tripleRingView, ringDataList)
            
            android.util.Log.d("VIBE_FIX", "Phase 3: Triple ring updated with animations - Steps: ${progress.stepsProgress.getPercentageInt()}%, Calories: ${progress.caloriesProgress.getPercentageInt()}%, Heart: ${progress.heartPointsProgress.getPercentageInt()}%")
            
        } catch (e: Exception) {
            android.util.Log.e("VIBE_FIX", "Phase 3: Error updating triple ring display with animations", e)
            
            // Fallback to basic update
            updateTripleRingDisplayBasic(progress)
        }
    }
    
    /**
     * Basic triple ring update without animations (fallback)
     */
    private fun updateTripleRingDisplayBasic(progress: DailyProgress) {
        try {
            val stepsRingData = RingDisplayData.fromProgressData(progress.stepsProgress)
            val caloriesRingData = RingDisplayData.fromProgressData(progress.caloriesProgress)
            val heartPointsRingData = RingDisplayData.fromProgressData(progress.heartPointsProgress)
            
            binding.tripleRingView.updateProgress(listOf(stepsRingData, caloriesRingData, heartPointsRingData), false)
            
        } catch (e: Exception) {
            android.util.Log.e("VIBE_FIX", "Phase 3: Error in basic triple ring update", e)
        }
    }
    
    // VIBE_FIX: Phase 3 - Update the progress summary cards
    private fun updateProgressSummary(progress: DailyProgress) {
        android.util.Log.d("VIBE_FIX", "Phase 3: Updating progress summary")
        
        try {
            // Update steps card
            binding.stepsNumber.text = "${progress.stepsProgress.current}/${progress.stepsProgress.target}"
            binding.stepsPercentage.text = "${progress.stepsProgress.getPercentageInt()}%"
            
            // Update calories card
            binding.caloriesNumber.text = "${progress.caloriesProgress.current}/${progress.caloriesProgress.target}"
            binding.caloriesPercentage.text = "${progress.caloriesProgress.getPercentageInt()}%"
            
            // Update heart points card
            binding.heartPointsNumber.text = "${progress.heartPointsProgress.current}/${progress.heartPointsProgress.target}"
            binding.heartPointsPercentage.text = "${progress.heartPointsProgress.getPercentageInt()}%"
            
            android.util.Log.d("VIBE_FIX", "Phase 3: Progress summary updated successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("VIBE_FIX", "Phase 3: Error updating progress summary", e)
        }
    }
    
    // VIBE_FIX: Phase 3 - Create personalized, time-based greeting
    private fun getPersonalizedGreeting(): String {
        val currentHour = java.time.LocalTime.now().hour
        return when (currentHour) {
            in 5..11 -> "Good morning! Ready to make today amazing?"
            in 12..16 -> "Good afternoon! Keep up the great momentum!"
            in 17..20 -> "Good evening! How's your wellness journey today?"
            else -> "Hello! Every step counts on your wellness journey!"
        }
    }
    
    // VIBE_FIX: Phase 3 - Add motivational messages based on progress
    private fun updateMotivationalMessage(progress: DailyProgress) {
        try {
            val averageProgress = (progress.stepsProgress.percentage + 
                                 progress.caloriesProgress.percentage + 
                                 progress.heartPointsProgress.percentage) / 3f
            
            val message = when {
                averageProgress >= 0.8f -> "🎉 Amazing work! You're crushing your goals today!"
                averageProgress >= 0.6f -> "💪 Great progress! You're well on your way to success!"
                averageProgress >= 0.4f -> "🌟 Keep it up! Every step brings you closer to your goals!"
                averageProgress >= 0.2f -> "🚀 You've got this! Small steps lead to big achievements!"
                else -> "✨ Ready to start your wellness journey? Every moment is a new beginning!"
            }
            
            binding.motivationalMessage.text = message
            binding.motivationalMessage.visibility = android.view.View.VISIBLE
            
            android.util.Log.d("VIBE_FIX", "Phase 3: Motivational message updated: $message")
            
        } catch (e: Exception) {
            android.util.Log.e("VIBE_FIX", "Phase 3: Error updating motivational message", e)
        }
    }
    

    
    // VIBE_FIX: Phase 1 - All complex dashboard methods removed temporarily
    // Will be restored in Phase 3 with proper dependencies
    
    override fun onResume() {
        super.onResume()
        android.util.Log.d("VIBE_FIX", "Phase 3: HomeFragment onResume - refreshing dashboard")
        dashboardViewModel.refreshDashboard()
    }
    
    override fun onPause() {
        super.onPause()
        android.util.Log.d("VIBE_FIX", "Phase 3: HomeFragment onPause - stopping updates")
        dashboardViewModel.stopDashboardUpdates()
    }
    
    /**
     * Handles animation events from the ViewModel
     */
    private fun handleAnimationEvent(event: AnimationEvent) {
        android.util.Log.d("VIBE_FIX", "Phase 3: Handling animation event: $event")
        
        when (event) {
            is AnimationEvent.GoalAchieved -> {
                // Celebrate goal achievement with triple ring animation
                val animationManager = dashboardViewModel.getAnimationManager()
                animationManager.celebrateGoalAchievement(
                    tripleRingView = binding.tripleRingView,
                    achievedRings = event.achievedRings
                ) {
                    // Show encouraging toast after celebration
                    val message = when (event.achievedRings.size) {
                        1 -> "🎉 Amazing! You achieved your ${event.achievedRings.first().displayName} goal!"
                        2 -> "🌟 Incredible! You achieved ${event.achievedRings.size} goals today!"
                        3 -> "🚀 Outstanding! You achieved all your wellness goals!"
                        else -> "✨ Great progress on your wellness journey!"
                    }
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                }
            }
            
            is AnimationEvent.DataRefreshed -> {
                // Provide visual feedback for refresh completion
                binding.greetingText.text = getPersonalizedGreeting()
                android.widget.Toast.makeText(context, "Your wellness data is up to date! ✨", android.widget.Toast.LENGTH_SHORT).show()
            }
            
            is AnimationEvent.ProgressUpdate -> {
                // Handle progress update animations
                android.util.Log.d("VIBE_FIX", "Phase 3: Progress update animation triggered with ${event.changes.size} changes")
                
                // Animate significant progress changes
                event.changes.filter { it.isSignificantChange() }.forEach { change ->
                    android.util.Log.d("VIBE_FIX", "Significant progress change: ${change.ringType} from ${change.fromProgress} to ${change.toProgress}")
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        android.util.Log.d("VIBE_FIX", "Phase 3: HomeFragment onDestroyView")
        dashboardViewModel.stopDashboardUpdates()
        _binding = null
    }
}