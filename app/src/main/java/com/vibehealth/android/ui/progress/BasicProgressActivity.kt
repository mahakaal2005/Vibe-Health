package com.vibehealth.android.ui.progress

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.vibehealth.android.R
import com.vibehealth.android.databinding.ActivityBasicProgressBinding
import com.vibehealth.android.ui.progress.models.ProgressUiState
import com.vibehealth.android.ui.progress.models.WeeklyProgressData
import com.vibehealth.android.ui.progress.models.CelebrationType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * BasicProgressActivity - Detailed progress history view following Companion Principle
 * 
 * This activity provides users with detailed weekly wellness data visualization,
 * transforming potentially overwhelming data into encouraging exploration of their
 * wellness journey. Designed for "The Urban Achiever" with calm clarity and
 * supportive guidance throughout.
 * 
 * Features:
 * - Portrait-locked for standard Android phones (5-7 inches)
 * - Sage Green & Warm Neutrals color palette (#6B8E6B, #7A8471, #B5846B)
 * - 8-point grid system for consistent spacing
 * - Motion System with 150-300ms transitions
 * - WCAG 2.1 Level AA accessibility compliance
 * - Offline-first design with supportive messaging
 */
@AndroidEntryPoint
class BasicProgressActivity : AppCompatActivity() {
    
    companion object {
        private const val EXTRA_SUPPORTIVE_MESSAGE = "extra_supportive_message"
        private const val EXTRA_CELEBRATION_CONTEXT = "extra_celebration_context"
        
        /**
         * Creates an intent to launch BasicProgressActivity with supportive context
         */
        fun createIntent(
            context: Context,
            supportiveMessage: String = "Let's explore your wellness journey together!",
            celebrationContext: String = "We're excited to show you how far you've come!"
        ): Intent {
            return Intent(context, BasicProgressActivity::class.java).apply {
                putExtra(EXTRA_SUPPORTIVE_MESSAGE, supportiveMessage)
                putExtra(EXTRA_CELEBRATION_CONTEXT, celebrationContext)
            }
        }
    }
    
    private lateinit var binding: ActivityBasicProgressBinding
    private val progressViewModel: BasicProgressViewModel by viewModels()
    
    // Animation manager for supportive transitions
    private val animationManager = ProgressAnimationManager()
    
    // Accessibility manager for WCAG 2.1 Level AA compliance
    private val accessibilityManager = ProgressAccessibilityManager(this)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set portrait orientation for standard Android phone experience
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        binding = ActivityBasicProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupSupportiveUI()
        setupAccessibility()
        setupObservers()
        setupSupportiveNavigation()
        
        // Load progress data with encouraging context
        loadProgressWithSupportiveContext()
    }
    
    /**
     * Sets up the supportive UI following Companion Principle
     */
    private fun setupSupportiveUI() {
        // Configure supportive toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.your_wellness_journey)
        }
        
        // Apply window insets for edge-to-edge experience
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Set up supportive context message from intent
        val supportiveMessage = intent.getStringExtra(EXTRA_SUPPORTIVE_MESSAGE)
        val celebrationContext = intent.getStringExtra(EXTRA_CELEBRATION_CONTEXT)
        
        if (!supportiveMessage.isNullOrEmpty()) {
            binding.progressContext.text = supportiveMessage
            binding.progressContext.visibility = View.VISIBLE
        }
        
        // Animate supportive entrance with Motion System timing
        animateEntranceWithSupportiveFeel()
    }
    
    /**
     * Sets up comprehensive accessibility support (WCAG 2.1 Level AA)
     */
    private fun setupAccessibility() {
        accessibilityManager.setupProgressAccessibility(binding)
        
        // Configure supportive content descriptions
        binding.stepsProgressGraph.contentDescription = 
            getString(R.string.accessibility_steps_progress_description)
        binding.caloriesProgressGraph.contentDescription = 
            getString(R.string.accessibility_calories_progress_description)
        binding.heartPointsProgressGraph.contentDescription = 
            getString(R.string.accessibility_heart_points_progress_description)
        
        // Set up keyboard navigation with supportive focus
        accessibilityManager.configureKeyboardNavigation(
            listOf(
                binding.stepsProgressGraph,
                binding.caloriesProgressGraph,
                binding.heartPointsProgressGraph
            )
        )
    }
    
    /**
     * Sets up observers for ViewModel state with supportive feedback and animations
     */
    private fun setupObservers() {
        lifecycleScope.launch {
            progressViewModel.uiState.collect { state ->
                handleProgressStateWithAnimations(state)
            }
        }
        
        lifecycleScope.launch {
            progressViewModel.supportiveMessages.collect { message ->
                if (message.isNotEmpty()) {
                    showSupportiveMessage(message)
                }
            }
        }
        
        lifecycleScope.launch {
            progressViewModel.celebratoryFeedback.collect { feedback ->
                if (feedback.isNotEmpty()) {
                    showCelebratoryFeedback(feedback)
                }
            }
        }
    }
    
    /**
     * Sets up supportive navigation with back button handling
     */
    private fun setupSupportiveNavigation() {
        binding.toolbar.setNavigationOnClickListener {
            // Animate supportive exit transition
            animateExitWithSupportiveFeel {
                finish()
            }
        }
    }
    

    
    /**
     * Shows encouraging loading state that maintains user confidence
     */
    private fun showEncouragingLoadingState() {
        binding.progressLoadingContainer.visibility = View.VISIBLE
        binding.progressContentContainer.visibility = View.GONE
        binding.progressErrorContainer.visibility = View.GONE
        
        // Animate loading with supportive feel
        animationManager.animateEncouragingLoading(binding.progressLoadingIndicator)
        
        binding.progressLoadingMessage.text = getString(R.string.progress_loading_encouraging_message)
    }
    
    /**
     * Shows supportive error state with encouraging recovery options
     */
    private fun showSupportiveErrorState(errorMessage: String) {
        binding.progressLoadingContainer.visibility = View.GONE
        binding.progressContentContainer.visibility = View.GONE
        binding.progressErrorContainer.visibility = View.VISIBLE
        
        binding.progressErrorMessage.text = errorMessage
        binding.progressRetryButton.setOnClickListener {
            // Retry with supportive feedback
            progressViewModel.retryLoadingWithEncouragement()
        }
        
        // Announce error with supportive tone
        binding.root.announceForAccessibility(
            getString(R.string.progress_error_accessibility_announcement, errorMessage)
        )
    }
    
    /**
     * Shows encouraging empty state that motivates future tracking
     */
    private fun showEncouragingEmptyState() {
        binding.progressLoadingContainer.visibility = View.GONE
        binding.progressContentContainer.visibility = View.VISIBLE
        binding.progressErrorContainer.visibility = View.GONE
        
        // Show supportive empty state content
        binding.progressEmptyStateContainer.visibility = View.VISIBLE
        binding.progressGraphsContainer.visibility = View.GONE
        
        binding.progressEmptyStateMessage.text = getString(R.string.progress_empty_state_encouraging_message)
        binding.progressEmptyStateGuidance.text = getString(R.string.progress_empty_state_supportive_guidance)
        
        // Animate empty state with gentle, encouraging feel
        animationManager.animateEncouragingEmptyState(binding.progressEmptyStateContainer)
    }
    
    /**
     * Shows progress data with celebratory feedback and supportive context
     */
    private fun showProgressDataWithCelebration(state: ProgressUiState) {
        binding.progressLoadingContainer.visibility = View.GONE
        binding.progressContentContainer.visibility = View.VISIBLE
        binding.progressErrorContainer.visibility = View.GONE
        binding.progressEmptyStateContainer.visibility = View.GONE
        binding.progressGraphsContainer.visibility = View.VISIBLE
        
        // Update progress graphs with supportive data presentation
        state.weeklyData?.let { weeklyData ->
            updateProgressGraphsWithSupportiveContext(weeklyData)
        }
        
        // Show supportive insights if available
        if (state.supportiveMessage != null) {
            showSupportiveInsights(state.supportiveMessage)
        }
        
        // Celebrate achievements if present
        if (state.celebratoryFeedback != null) {
            animationManager.celebrateProgressAchievements(
                binding.progressGraphsContainer,
                state.celebratoryFeedback
            )
        }
    }
    
    /**
     * Updates progress graphs with supportive context and encouraging messaging
     */
    private fun updateProgressGraphsWithSupportiveContext(weeklyData: WeeklyProgressData) {
        // Update each graph with supportive data presentation
        binding.stepsProgressGraph.updateWithSupportiveData(
            weeklyData.getStepsData(),
            weeklyData.getStepsSupportiveMessage()
        )
        
        binding.caloriesProgressGraph.updateWithSupportiveData(
            weeklyData.getCaloriesData(),
            weeklyData.getCaloriesSupportiveMessage()
        )
        
        binding.heartPointsProgressGraph.updateWithSupportiveData(
            weeklyData.getHeartPointsData(),
            weeklyData.getHeartPointsSupportiveMessage()
        )
        
        // Animate graph reveal with celebratory feel
        animationManager.animateProgressGraphReveal(
            listOf(
                binding.stepsProgressGraph,
                binding.caloriesProgressGraph,
                binding.heartPointsProgressGraph
            )
        )
    }
    
    /**
     * Shows supportive insights about progress patterns
     */
    private fun showSupportiveInsights(insights: String) {
        binding.progressInsightsContainer.visibility = View.VISIBLE
        binding.progressInsightsText.text = insights
        
        // Animate insights with gentle, encouraging feel
        animationManager.animateSupportiveInsights(binding.progressInsightsContainer)
    }
    
    /**
     * Shows supportive message with gentle animation
     */
    private fun showSupportiveMessage(message: String) {
        // Implementation for showing supportive messages
        binding.root.announceForAccessibility(message)
    }
    
    /**
     * Shows celebratory feedback with encouraging animation
     */
    private fun showCelebratoryFeedback(feedback: String) {
        // Implementation for celebratory feedback
        binding.root.announceForAccessibility(feedback)
    }
    
    /**
     * Loads progress data with encouraging context
     */
    private fun loadProgressWithSupportiveContext() {
        val celebrationContext = intent.getStringExtra(EXTRA_CELEBRATION_CONTEXT)
        progressViewModel.loadProgressWithEncouragement(celebrationContext)
    }
    
    /**
     * Animates entrance with supportive, welcoming feel (150-300ms)
     */
    private fun animateEntranceWithSupportiveFeel() {
        binding.root.alpha = 0f
        binding.root.translationY = 50f
        
        binding.root.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(250L) // Within Motion System guidelines
            .setInterpolator(DecelerateInterpolator()) // Calm, supportive easing
            .start()
    }
    
    /**
     * Animates exit with supportive transition (150-300ms)
     */
    private fun animateExitWithSupportiveFeel(onComplete: () -> Unit) {
        binding.root.animate()
            .alpha(0f)
            .translationY(-30f)
            .setDuration(200L) // Within Motion System guidelines
            .setInterpolator(DecelerateInterpolator()) // Gentle exit
            .withEndAction(onComplete)
            .start()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        animateExitWithSupportiveFeel {
            finish()
        }
        return true
    }
    
    override fun onBackPressed() {
        animateExitWithSupportiveFeel {
            super.onBackPressed()
        }
    }
    
    /**
     * Handles progress state changes with supportive UI updates and Motion System animations
     */
    private fun handleProgressStateWithAnimations(state: ProgressUiState) {
        when {
            state.isLoading -> {
                animationManager.animateSupportiveStateTransition(
                    binding.progressContentContainer,
                    binding.progressLoadingContainer
                ) {
                    binding.progressLoadingMessage.text = state.encouragingLoadingMessage
                    animationManager.animateEncouragingLoading(binding.progressLoadingIndicator)
                }
            }
            
            state.errorMessage != null -> {
                animationManager.animateSupportiveStateTransition(
                    binding.progressLoadingContainer,
                    binding.progressErrorContainer
                ) {
                    binding.progressErrorMessage.text = state.errorMessage
                    binding.progressErrorGuidance.text = state.primarySupportiveMessage
                    animationManager.animateSupportiveError(binding.progressErrorContainer)
                }
            }
            
            state.showEmptyState -> {
                animationManager.animateSupportiveStateTransition(
                    binding.progressLoadingContainer,
                    binding.progressEmptyStateContainer
                ) {
                    binding.progressEmptyStateMessage.text = state.supportiveEmptyStateMessage
                    binding.progressEmptyStateGuidance.text = state.encouragingEmptyStateGuidance
                    animationManager.animateSupportiveInsights(binding.progressEmptyStateContainer)
                }
            }
            
            state.weeklyData != null -> {
                animationManager.animateSupportiveStateTransition(
                    binding.progressLoadingContainer,
                    binding.progressContentContainer
                ) {
                    displayProgressDataWithAnimations(state.weeklyData, state.hasAchievements)
                    
                    // Handle celebratory feedback with appropriate animation
                    if (state.hasAchievements) {
                        val celebrationType = when {
                            state.weeklyData.weeklyTotals.activeDays >= 6 -> CelebrationType.MAJOR
                            state.weeklyData.weeklyTotals.activeDays >= 4 -> CelebrationType.MODERATE
                            else -> CelebrationType.GENTLE
                        }
                        
                        animationManager.celebrateProgressAchievements(
                            binding.progressContentContainer,
                            state.celebratoryFeedback ?: "Celebrating your wellness achievements!"
                        )
                    }
                }
            }
        }
        
        // Update supportive messaging with gentle animation
        state.primarySupportiveMessage?.let { message ->
            binding.progressSupportiveMessage.text = message
            if (binding.progressSupportiveMessage.visibility != View.VISIBLE) {
                binding.progressSupportiveMessage.alpha = 0f
                binding.progressSupportiveMessage.visibility = View.VISIBLE
                binding.progressSupportiveMessage.animate()
                    .alpha(1f)
                    .setDuration(200L)
                    .start()
            }
        } ?: run {
            if (binding.progressSupportiveMessage.visibility == View.VISIBLE) {
                binding.progressSupportiveMessage.animate()
                    .alpha(0f)
                    .setDuration(200L)
                    .withEndAction {
                        binding.progressSupportiveMessage.visibility = View.GONE
                    }
                    .start()
            }
        }
        
        // Handle offline indicator with supportive animation
        if (state.offlineMode && binding.offlineIndicator.visibility != View.VISIBLE) {
            binding.offlineMessage.text = state.supportiveOfflineMessage
            binding.offlineIndicator.alpha = 0f
            binding.offlineIndicator.visibility = View.VISIBLE
            binding.offlineIndicator.animate()
                .alpha(1f)
                .setDuration(250L)
                .start()
        } else if (!state.offlineMode && binding.offlineIndicator.visibility == View.VISIBLE) {
            binding.offlineIndicator.animate()
                .alpha(0f)
                .setDuration(250L)
                .withEndAction {
                    binding.offlineIndicator.visibility = View.GONE
                }
                .start()
        }
    }
    
    /**
     * Displays progress data with celebratory animations and Motion System timing
     */
    private fun displayProgressDataWithAnimations(
        weeklyData: WeeklyProgressData,
        hasAchievements: Boolean
    ) {
        // Animate progress graphs with staggered reveal
        val progressGraphs = listOf(
            binding.stepsProgressGraph,
            binding.caloriesProgressGraph,
            binding.heartPointsProgressGraph
        )
        
        animationManager.animateProgressGraphReveal(progressGraphs)
        
        // Show achievement badges if any
        if (hasAchievements) {
            weeklyData.celebratoryMessages.forEachIndexed { index, message ->
                val celebrationType = when {
                    message.contains("perfect", ignoreCase = true) -> CelebrationType.MAJOR
                    message.contains("great", ignoreCase = true) -> CelebrationType.MODERATE
                    else -> CelebrationType.GENTLE
                }
                
                // Stagger achievement badge animations
                binding.root.postDelayed({
                    // This would animate individual achievement badges
                    // Implementation depends on the specific badge views
                }, index * 200L)
            }
        }
        
        // Update progress indicators with animated progression
        weeklyData.weeklyTotals.let { totals ->
            // These would animate actual progress bars when they exist in the layout
            // For now, just demonstrate the animation capability
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cancel animations to prevent memory leaks
        animationManager.cancelAnimations()
    }
    
    /**
     * Pause animations when activity goes to background
     */
    override fun onPause() {
        super.onPause()
        animationManager.pauseAnimations()
    }
    
    /**
     * Resume animations when activity comes to foreground
     */
    override fun onResume() {
        super.onResume()
        animationManager.resumeAnimations()
    }
}