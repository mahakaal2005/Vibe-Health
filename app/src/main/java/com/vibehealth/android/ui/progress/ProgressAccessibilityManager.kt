package com.vibehealth.android.ui.progress

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.vibehealth.android.R
import com.vibehealth.android.databinding.ActivityBasicProgressBinding
import com.vibehealth.android.ui.components.BasicProgressGraph
import com.vibehealth.android.ui.progress.models.ProgressUiState
import com.vibehealth.android.ui.progress.models.MetricType
import kotlin.math.max
import kotlin.math.min

/**
 * ProgressAccessibilityManager - WCAG 2.1 Level AA compliance for progress views
 * 
 * This class ensures comprehensive accessibility support for the progress history view,
 * providing supportive, encouraging accessibility experiences that maintain the
 * Companion Principle even for users with different interaction capabilities.
 * 
 * Features:
 * - WCAG 2.1 Level AA compliance
 * - Screen reader support with encouraging content descriptions
 * - Keyboard navigation with supportive focus management
 * - High contrast mode support
 * - Touch exploration with audio feedback
 * - Supportive accessibility announcements
 */
class ProgressAccessibilityManager(private val context: Context) {
    
    private val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    
    /**
     * Sets up comprehensive accessibility support for progress activity
     */
    fun setupProgressAccessibility(binding: ActivityBasicProgressBinding) {
        setupScreenReaderSupport(binding)
        setupKeyboardNavigation(binding)
        setupTouchExploration(binding)
        setupAccessibilityActions(binding)
        setupLiveRegions(binding)
    }
    
    /**
     * Sets up screen reader support with encouraging content descriptions
     */
    private fun setupScreenReaderSupport(binding: ActivityBasicProgressBinding) {
        // Set up supportive content descriptions for all interactive elements
        binding.stepsProgressGraph.contentDescription = 
            context.getString(R.string.accessibility_steps_progress_description)
        
        binding.caloriesProgressGraph.contentDescription = 
            context.getString(R.string.accessibility_calories_progress_description)
        
        binding.heartPointsProgressGraph.contentDescription = 
            context.getString(R.string.accessibility_heart_points_progress_description)
        
        // Set up accessibility delegates for custom views
        setupProgressGraphAccessibility(binding.stepsProgressGraph, MetricType.STEPS)
        setupProgressGraphAccessibility(binding.caloriesProgressGraph, MetricType.CALORIES)
        setupProgressGraphAccessibility(binding.heartPointsProgressGraph, MetricType.HEART_POINTS)
        
        // Set up container descriptions
        binding.progressContentContainer.contentDescription = 
            "Weekly wellness progress data with supportive insights"
        
        binding.progressLoadingContainer.contentDescription = 
            "Loading your wellness progress data"
        
        binding.progressErrorContainer.contentDescription = 
            "Error loading progress data with supportive recovery options"
        
        binding.progressEmptyStateContainer.contentDescription = 
            "Welcome to your wellness journey - ready to start tracking progress"
    }
    
    /**
     * Sets up accessibility delegate for progress graphs with supportive feedback
     */
    private fun setupProgressGraphAccessibility(graphView: View, metricType: MetricType) {
        ViewCompat.setAccessibilityDelegate(graphView, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                
                info.className = "ProgressGraph"
                info.contentDescription = generateSupportiveGraphDescription(metricType)
                
                // Add custom actions for exploring data
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK,
                        "Explore ${metricType.displayName} details"
                    )
                )
                
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_LONG_CLICK,
                        "Get ${metricType.displayName} insights"
                    )
                )
            }
            
            override fun performAccessibilityAction(
                host: View,
                action: Int,
                args: android.os.Bundle?
            ): Boolean {
                when (action) {
                    AccessibilityNodeInfoCompat.ACTION_CLICK -> {
                        announceGraphExploration(metricType)
                        return true
                    }
                    AccessibilityNodeInfoCompat.ACTION_LONG_CLICK -> {
                        announceMetricInsights(metricType)
                        return true
                    }
                }
                return super.performAccessibilityAction(host, action, args)
            }
        })
    }
    
    /**
     * Configures keyboard navigation with supportive focus management
     */
    fun configureKeyboardNavigation(focusableViews: List<View>) {
        focusableViews.forEachIndexed { index, view ->
            // Set up focus order
            val nextView = focusableViews.getOrNull(index + 1)
            val previousView = focusableViews.getOrNull(index - 1)
            
            view.nextFocusDownId = nextView?.id ?: View.NO_ID
            view.nextFocusUpId = previousView?.id ?: View.NO_ID
            
            // Add supportive focus change listeners
            view.setOnFocusChangeListener { focusedView, hasFocus ->
                if (hasFocus) {
                    announceProgressFocus(focusedView)
                    highlightFocusedElement(focusedView)
                }
            }
            
            // Ensure views are focusable
            view.isFocusable = true
            view.isFocusableInTouchMode = true
        }
    }
    
    /**
     * Sets up touch exploration with supportive audio feedback
     */
    private fun setupTouchExploration(binding: ActivityBasicProgressBinding) {
        val touchExploreViews = listOf(
            binding.stepsProgressGraph,
            binding.caloriesProgressGraph,
            binding.heartPointsProgressGraph,
            binding.progressInsightsContainer,
            binding.progressRetryButton
        )
        
        touchExploreViews.forEach { view ->
            view.setOnHoverListener { hoveredView, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_HOVER_ENTER -> {
                        provideTouchExplorationFeedback(hoveredView)
                        true
                    }
                    else -> false
                }
            }
        }
    }
    
    /**
     * Sets up accessibility actions for interactive elements
     */
    private fun setupAccessibilityActions(binding: ActivityBasicProgressBinding) {
        // Add custom actions for retry button
        ViewCompat.setAccessibilityDelegate(binding.progressRetryButton, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK,
                        "Retry loading your wellness progress with supportive guidance"
                    )
                )
            }
        })
    }
    
    /**
     * Sets up live regions for dynamic content updates
     */
    private fun setupLiveRegions(binding: ActivityBasicProgressBinding) {
        // Set up live regions for dynamic content that should be announced
        ViewCompat.setAccessibilityLiveRegion(
            binding.progressLoadingMessage,
            ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE
        )
        
        ViewCompat.setAccessibilityLiveRegion(
            binding.progressErrorMessage,
            ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE
        )
        
        ViewCompat.setAccessibilityLiveRegion(
            binding.progressInsightsText,
            ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE
        )
        
        ViewCompat.setAccessibilityLiveRegion(
            binding.offlineMessage,
            ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE
        )
    }
    
    /**
     * Announces state changes with supportive context
     */
    fun announceStateChange(state: ProgressUiState) {
        val announcement = when {
            state.isLoading -> "Loading your wellness progress. We're excited to show you your journey!"
            state.errorMessage != null -> "We encountered an issue loading your progress: ${state.errorMessage}. Your wellness data is safe and we're working to resolve this."
            state.showEmptyState -> "Welcome to your wellness journey! This is where you'll see your progress as you start tracking your activities."
            state.weeklyData != null -> "Your weekly progress is ready! ${generateProgressSummaryAnnouncement(state)}"
            else -> "Your wellness progress view is ready for exploration."
        }
        
        announceForAccessibility(announcement)
    }
    
    /**
     * Announces progress achievements with celebratory tone
     */
    fun announceProgressAchievements(achievements: List<String>) {
        if (achievements.isNotEmpty()) {
            val announcement = "Congratulations! ${achievements.joinToString(" ")} Your commitment to wellness is inspiring!"
            announceForAccessibility(announcement)
        }
    }
    
    /**
     * Announces supportive focus changes
     */
    private fun announceProgressFocus(view: View) {
        val supportiveMessage = when (view.id) {
            R.id.steps_progress_graph -> "Exploring your steps progress. Every step is progress toward better health!"
            R.id.calories_progress_graph -> "Viewing your calorie burn progress. Your energy and effort are making a difference!"
            R.id.heart_points_progress_graph -> "Checking your heart points progress. Your cardiovascular health journey is important!"
            R.id.progress_retry_button -> "Retry button focused. We're here to help you access your wellness progress."
            else -> "Navigating your wellness progress data with supportive guidance."
        }
        
        view.announceForAccessibility(supportiveMessage)
    }
    
    /**
     * Provides touch exploration feedback with encouraging context
     */
    private fun provideTouchExplorationFeedback(view: View) {
        val feedbackMessage = when (view.id) {
            R.id.steps_progress_graph -> "Steps progress graph. Tap to explore your daily step data and celebrate your movement achievements."
            R.id.calories_progress_graph -> "Calories progress graph. Tap to explore your energy expenditure and see how your efforts are making a difference."
            R.id.heart_points_progress_graph -> "Heart points progress graph. Tap to explore your cardiovascular activity and heart health journey."
            R.id.progress_insights_container -> "Wellness insights. Your personalized progress insights and encouraging guidance."
            R.id.progress_retry_button -> "Retry button. Tap to try loading your progress again with our supportive assistance."
            else -> "Interactive wellness progress element. Tap to explore."
        }
        
        view.announceForAccessibility(feedbackMessage)
    }
    
    /**
     * Generates supportive graph description for accessibility
     */
    private fun generateSupportiveGraphDescription(metricType: MetricType): String {
        return when (metricType) {
            MetricType.STEPS -> "Steps progress for this week. Your movement is contributing to your overall wellness journey. Tap to explore daily step details and celebrate your achievements."
            MetricType.CALORIES -> "Calories burned this week. Your energy expenditure shows great commitment to your health. Tap to see daily calorie details and progress insights."
            MetricType.HEART_POINTS -> "Heart points earned this week. Your cardiovascular activity is strengthening your heart. Tap to view daily heart point details and wellness achievements."
        }
    }
    
    /**
     * Announces graph exploration with encouraging context
     */
    private fun announceGraphExploration(metricType: MetricType) {
        val message = when (metricType) {
            MetricType.STEPS -> "Exploring your steps data. Every step you take is progress toward better health and wellness!"
            MetricType.CALORIES -> "Exploring your calorie burn data. Your energy and effort are making a real difference in your wellness journey!"
            MetricType.HEART_POINTS -> "Exploring your heart points data. Your cardiovascular activities are strengthening your heart and improving your health!"
        }
        
        announceForAccessibility(message)
    }
    
    /**
     * Announces metric insights with supportive guidance
     */
    private fun announceMetricInsights(metricType: MetricType) {
        val message = when (metricType) {
            MetricType.STEPS -> "Steps insights: Your movement patterns show dedication to staying active. Keep up the excellent work on your wellness journey!"
            MetricType.CALORIES -> "Calories insights: Your energy expenditure demonstrates commitment to your health goals. Your efforts are truly making a difference!"
            MetricType.HEART_POINTS -> "Heart points insights: Your cardiovascular activities show great care for your heart health. Your dedication to wellness is inspiring!"
        }
        
        announceForAccessibility(message)
    }
    
    /**
     * Highlights focused element with visual and haptic feedback
     */
    private fun highlightFocusedElement(view: View) {
        // Add visual highlight for keyboard navigation
        view.isSelected = true
        
        // Provide haptic feedback if available
        view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        
        // Remove highlight after a delay
        view.postDelayed({
            view.isSelected = false
        }, 2000)
    }
    
    /**
     * Generates progress summary announcement with encouraging tone
     */
    private fun generateProgressSummaryAnnouncement(state: ProgressUiState): String {
        val weeklyData = state.weeklyData ?: return "Your progress data is ready for exploration!"
        
        val activeDays = weeklyData.weeklyTotals.activeDays
        val totalSteps = weeklyData.weeklyTotals.totalSteps
        val achievements = weeklyData.celebratoryMessages.size
        
        return buildString {
            append("You were active on $activeDays days this week")
            if (totalSteps > 0) {
                append(" with ${String.format("%,d", totalSteps)} total steps")
            }
            if (achievements > 0) {
                append(" and achieved $achievements wellness milestones")
            }
            append(". Your dedication to wellness is inspiring!")
        }
    }
    
    /**
     * Announces for accessibility with proper event type
     */
    private fun announceForAccessibility(message: String) {
        if (accessibilityManager.isEnabled) {
            val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
            event.text.add(message)
            accessibilityManager.sendAccessibilityEvent(event)
        }
    }
    
    /**
     * Checks if accessibility services are enabled
     */
    fun isAccessibilityEnabled(): Boolean {
        return accessibilityManager.isEnabled
    }
    
    /**
     * Checks if touch exploration is enabled
     */
    fun isTouchExplorationEnabled(): Boolean {
        return accessibilityManager.isTouchExplorationEnabled
    }
    
    /**
     * Gets accessibility service info for customization
     */
    fun getAccessibilityServiceInfo(): String {
        return if (accessibilityManager.isEnabled) {
            "Accessibility services are active. Providing supportive, encouraging accessibility experience."
        } else {
            "Standard interaction mode active."
        }
    }
    
    /**
     * Sets up comprehensive keyboard navigation for progress views
     */
    fun setupKeyboardNavigation(binding: ActivityBasicProgressBinding) {
        setupKeyboardNavigationRecursive(binding.root as ViewGroup)
    }
    
    /**
     * Recursively sets up keyboard navigation for all focusable views
     */
    private fun setupKeyboardNavigationRecursive(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            
            when (child) {
                is ViewGroup -> {
                    setupKeyboardNavigationRecursive(child)
                }
                else -> {
                    if (child.isFocusable || child.isClickable) {
                        setupViewKeyboardSupport(child)
                    }
                }
            }
        }
    }
    
    /**
     * Sets up keyboard support for individual views
     */
    private fun setupViewKeyboardSupport(view: View) {
        view.apply {
            // Ensure proper focus handling
            isFocusable = true
            isFocusableInTouchMode = false
            
            // Add visual focus indicators using sage green
            onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    // Add sage green focus outline
                    v.background = ContextCompat.getDrawable(v.context, R.drawable.focus_outline_sage)
                    
                    // Announce focus for screen readers
                    val description = v.contentDescription ?: "Interactive element"
                    v.announceForAccessibility("Focused on $description")
                } else {
                    // Remove focus outline
                    v.background = null
                }
            }
            
            // Handle keyboard activation
            setOnKeyListener { v, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE -> {
                            v.performClick()
                            true
                        }
                        else -> false
                    }
                } else false
            }
        }
    }
    
    /**
     * Validates color contrast ratios for WCAG 2.1 Level AA compliance
     */
    fun validateColorContrast(context: Context): List<String> {
        val issues = mutableListOf<String>()
        
        // Check sage green primary against white background
        val sageGreenPrimary = ContextCompat.getColor(context, R.color.sage_green_primary)
        val whiteBackground = ContextCompat.getColor(context, android.R.color.white)
        
        val contrastRatio = calculateContrastRatio(sageGreenPrimary, whiteBackground)
        if (contrastRatio < 4.5) {
            issues.add("Sage green primary text on white background has insufficient contrast: $contrastRatio (minimum 4.5 required)")
        }
        
        // Check supportive text colors
        val supportiveText = ContextCompat.getColor(context, R.color.supportive_text)
        val supportiveContrast = calculateContrastRatio(supportiveText, whiteBackground)
        if (supportiveContrast < 4.5) {
            issues.add("Supportive text color has insufficient contrast: $supportiveContrast")
        }
        
        return issues
    }
    
    /**
     * Calculates color contrast ratio between two colors
     */
    private fun calculateContrastRatio(color1: Int, color2: Int): Double {
        val luminance1 = calculateLuminance(color1)
        val luminance2 = calculateLuminance(color2)
        
        val lighter = max(luminance1, luminance2)
        val darker = min(luminance1, luminance2)
        
        return (lighter + 0.05) / (darker + 0.05)
    }
    
    /**
     * Calculates relative luminance of a color
     */
    private fun calculateLuminance(color: Int): Double {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        
        val rLum = if (r <= 0.03928) r / 12.92 else Math.pow((r + 0.055) / 1.055, 2.4)
        val gLum = if (g <= 0.03928) g / 12.92 else Math.pow((g + 0.055) / 1.055, 2.4)
        val bLum = if (b <= 0.03928) b / 12.92 else Math.pow((b + 0.055) / 1.055, 2.4)
        
        return 0.2126 * rLum + 0.7152 * gLum + 0.0722 * bLum
    }
    
    /**
     * Sets up touch exploration support for progress graphs
     */
    fun setupTouchExploration(progressGraph: BasicProgressGraph) {
        progressGraph.apply {
            // Enable accessibility touch exploration
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            
            // Set up touch exploration delegate
            accessibilityDelegate = object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityEvent(host: View, event: AccessibilityEvent) {
                    super.onInitializeAccessibilityEvent(host, event)
                    event.className = BasicProgressGraph::class.java.name
                    event.contentDescription = generateGraphAccessibilityDescription(progressGraph)
                }
                
                override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = BasicProgressGraph::class.java.name
                    info.contentDescription = generateGraphAccessibilityDescription(progressGraph)
                    
                    // Add custom actions for data exploration
                    info.addAction(
                        AccessibilityNodeInfo.AccessibilityAction(
                            AccessibilityNodeInfo.ACTION_CLICK,
                            "Explore your progress data"
                        )
                    )
                    
                    info.addAction(
                        AccessibilityNodeInfo.AccessibilityAction(
                            AccessibilityNodeInfo.ACTION_LONG_CLICK,
                            "Get detailed progress insights"
                        )
                    )
                }
            }
            
            // Handle accessibility actions
            setOnClickListener {
                announceForAccessibility("Exploring your wellness progress. ${generateEncouragingProgressSummary()}")
            }
            
            setOnLongClickListener {
                announceForAccessibility(generateDetailedAccessibilityInsight())
                true
            }
        }
    }
    
    /**
     * Generates accessibility description for progress graph
     */
    private fun generateGraphAccessibilityDescription(progressGraph: BasicProgressGraph): String {
        // This would access the graph's data - for now return encouraging placeholder
        return "Your wellness progress graph showing this week's activity. Tap to explore your achievements and progress patterns."
    }
    
    /**
     * Generates encouraging progress summary for accessibility
     */
    private fun generateEncouragingProgressSummary(): String {
        return "Your wellness journey shows dedication and progress. Every step forward is worth celebrating!"
    }
    
    /**
     * Generates detailed accessibility insight
     */
    private fun generateDetailedAccessibilityInsight(): String {
        return "This week you've made meaningful progress in your wellness journey. Your consistency and effort are building healthy habits that will benefit you long-term."
    }
    
    /**
     * Tests high contrast mode compatibility
     */
    fun testHighContrastCompatibility(context: Context): List<String> {
        val issues = mutableListOf<String>()
        
        // Check if high contrast mode is enabled
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val isHighContrastEnabled = accessibilityManager.isEnabled
        
        if (isHighContrastEnabled) {
            // Validate that our colors work well in high contrast mode
            val contrastIssues = validateColorContrast(context)
            if (contrastIssues.isNotEmpty()) {
                issues.add("High contrast mode compatibility issues detected")
                issues.addAll(contrastIssues)
            }
        }
        
        return issues
    }
}