package com.vibehealth.android.ui.progress

import android.content.Context
import com.vibehealth.android.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ProgressErrorHandler - Handles errors with supportive, encouraging messaging
 * 
 * This class transforms technical errors into supportive, user-friendly messages
 * that maintain the Companion Principle even during error states. All error
 * messaging is designed to be encouraging and provide clear guidance for recovery.
 */
@Singleton
class ProgressErrorHandler @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {
    
    /**
     * Handles progress errors with supportive recovery guidance
     */
    fun handleProgressError(error: ProgressError): ProgressErrorResponse {
        return when (error) {
            is ProgressError.NetworkError -> ProgressErrorResponse.EncouragingRetry(
                supportiveMessage = "We're having trouble connecting right now. Your wellness journey is important to us - let's try again in a moment.",
                recoveryGuidance = "Your progress data is safely stored and will be available once we reconnect.",
                retryAction = { /* Retry logic */ }
            )
            
            is ProgressError.DataNotFound -> ProgressErrorResponse.SupportiveEmptyState(
                supportiveMessage = "Your wellness journey is just beginning! Every step counts, and we're here to celebrate your progress.",
                recoveryGuidance = "Start tracking your daily activities to see your progress here. Even small steps lead to big changes over time."
            )
            
            is ProgressError.CacheError -> ProgressErrorResponse.GentleFallback(
                supportiveMessage = "We're working on displaying your progress beautifully. Your wellness data is safe with us.",
                recoveryGuidance = "Your progress is still being tracked perfectly - just the visualization needs a moment."
            )
            
            is ProgressError.UnknownError -> ProgressErrorResponse.ReassuringSolution(
                supportiveMessage = "Something unexpected happened, but don't worry - your wellness data is secure.",
                recoveryGuidance = "Let's try again together. Your wellness journey is too important to let technical hiccups stop us."
            )
        }
    }
    
    /**
     * Creates encouraging empty state message
     */
    fun createEncouragingEmptyState(): String {
        return "Every wellness journey starts with a single step. We're excited to celebrate your progress as you begin tracking your daily activities!"
    }
    
    /**
     * Creates supportive offline message
     */
    fun createSupportiveOfflineMessage(): String {
        return "You can still view your progress while offline. We'll sync everything when you're connected again."
    }
    
    /**
     * Creates gentle loading failure message
     */
    fun createGentleLoadingFailure(): String {
        return "We're having a little trouble loading your progress right now. Your wellness journey is important to us - let's try again!"
    }
    
    /**
     * Creates encouraging loading states with reassuring feedback
     */
    fun createEncouragingLoadingState(loadingPhase: LoadingPhase): ProgressLoadingState {
        return when (loadingPhase) {
            LoadingPhase.INITIAL -> ProgressLoadingState(
                message = "Getting your wellness progress ready...",
                encouragingContext = "We're excited to show you how far you've come!",
                showProgress = true,
                progressPercentage = 0
            )
            
            LoadingPhase.FETCHING_DATA -> ProgressLoadingState(
                message = "Loading your wellness data...",
                encouragingContext = "Your progress tells an inspiring story of dedication!",
                showProgress = true,
                progressPercentage = 30
            )
            
            LoadingPhase.PROCESSING_INSIGHTS -> ProgressLoadingState(
                message = "Preparing your personalized insights...",
                encouragingContext = "We're analyzing your achievements and progress patterns!",
                showProgress = true,
                progressPercentage = 70
            )
            
            LoadingPhase.FINALIZING -> ProgressLoadingState(
                message = "Almost ready to celebrate your progress!",
                encouragingContext = "Your wellness journey is about to be revealed!",
                showProgress = true,
                progressPercentage = 95
            )
        }
    }
    
    /**
     * Creates supportive empty states that motivate without guilt
     */
    fun createSupportiveEmptyState(emptyStateType: EmptyStateType): ProgressEmptyState {
        return when (emptyStateType) {
            EmptyStateType.NO_DATA_YET -> ProgressEmptyState(
                title = "Your Wellness Journey Awaits!",
                message = "This is where you'll see your progress as you start tracking your activities. Every journey begins with a single step!",
                actionText = "Start Tracking",
                encouragingContext = "We're here to support you every step of the way on your wellness journey.",
                iconResource = R.drawable.ic_wellness_journey_start,
                showMotivationalTip = true,
                motivationalTip = "Tip: Even small activities like a 5-minute walk contribute to your overall wellness!"
            )
            
            EmptyStateType.WEEK_NO_ACTIVITY -> ProgressEmptyState(
                title = "A Fresh Week Ahead",
                message = "This week is a new opportunity for wellness activities. Rest is important too, and every week offers a chance to move forward!",
                actionText = "Explore Activities",
                encouragingContext = "Your wellness journey is unique to you - there's no pressure, just support.",
                iconResource = R.drawable.ic_wellness_journey_start,
                showMotivationalTip = true,
                motivationalTip = "Remember: Progress isn't always about doing more - sometimes it's about consistency and self-care."
            )
            
            EmptyStateType.OFFLINE_NO_SYNC -> ProgressEmptyState(
                title = "Offline Mode Active",
                message = "We're working with your locally stored progress data. When you're back online, we'll sync everything seamlessly!",
                actionText = "View Offline Data",
                encouragingContext = "Your progress tracking continues even when offline - nothing is lost!",
                iconResource = R.drawable.ic_offline,
                showMotivationalTip = false,
                motivationalTip = null
            )
        }
    }
    
    /**
     * Creates celebratory success states for positive reinforcement
     */
    fun createCelebratorySuccessState(
        activeDays: Int,
        achievements: List<String>
    ): ProgressSuccessState {
        val hasAchievements = achievements.isNotEmpty()
        
        return when {
            hasAchievements && activeDays >= 5 -> ProgressSuccessState(
                title = "Outstanding Week! 🌟",
                message = "You've had an incredible week with ${achievements.size} achievements and activity on $activeDays days!",
                celebratoryMessage = achievements.joinToString(" "),
                encouragingContext = "Your consistency and dedication are truly inspiring. You're building lasting healthy habits!",
                showCelebration = true,
                celebrationType = CelebrationType.MAJOR
            )
            
            hasAchievements -> ProgressSuccessState(
                title = "Great Progress! 🎉",
                message = "You've achieved some wonderful milestones this week!",
                celebratoryMessage = achievements.joinToString(" "),
                encouragingContext = "Every achievement, big or small, is worth celebrating on your wellness journey!",
                showCelebration = true,
                celebrationType = CelebrationType.MODERATE
            )
            
            activeDays >= 4 -> ProgressSuccessState(
                title = "Excellent Consistency! 💪",
                message = "You were active on $activeDays days this week - that's fantastic consistency!",
                celebratoryMessage = "Your dedication to staying active is building strong, healthy habits!",
                encouragingContext = "Consistency is the key to lasting wellness, and you're demonstrating it beautifully!",
                showCelebration = true,
                celebrationType = CelebrationType.MODERATE
            )
            
            else -> ProgressSuccessState(
                title = "Your Progress Matters! 💚",
                message = "Every step on your wellness journey counts, and we're here to support you!",
                celebratoryMessage = "You're making progress at your own pace, and that's exactly right!",
                encouragingContext = "Wellness is a personal journey, and you're moving forward in your own meaningful way!",
                showCelebration = false,
                celebrationType = CelebrationType.GENTLE
            )
        }
    }
    
    /**
     * Handles offline scenarios with supportive messaging
     */
    fun handleOfflineState(
        hasLocalData: Boolean,
        lastSyncTime: Long?
    ): ProgressOfflineState {
        val timeSinceSync = lastSyncTime?.let { 
            System.currentTimeMillis() - it 
        }
        
        return when {
            !hasLocalData -> ProgressOfflineState(
                title = "Offline Mode",
                message = "You're currently offline, but don't worry! Your progress tracking will continue locally and sync when you're back online.",
                supportiveAction = "Continue Offline",
                encouragingContext = "Your wellness journey doesn't pause for connectivity issues!",
                showOfflineIndicator = true,
                canContinueOffline = true
            )
            
            timeSinceSync != null && timeSinceSync > 24 * 60 * 60 * 1000 -> { // 24 hours
                ProgressOfflineState(
                    title = "Sync When Ready",
                    message = "You have local progress data from your recent activities. We'll sync everything when you're back online!",
                    supportiveAction = "View Local Progress",
                    encouragingContext = "Your progress is safely stored locally and ready to sync when convenient!",
                    showOfflineIndicator = true,
                    canContinueOffline = true
                )
            }
            
            else -> ProgressOfflineState(
                title = "Working Offline",
                message = "Using your most recent progress data. Everything will sync automatically when you're back online!",
                supportiveAction = "Continue",
                encouragingContext = "Your wellness tracking continues seamlessly, online or offline!",
                showOfflineIndicator = true,
                canContinueOffline = true
            )
        }
    }
}

/**
 * Sealed class for progress error responses with supportive messaging
 */
sealed class ProgressErrorResponse {
    data class EncouragingRetry(
        val supportiveMessage: String,
        val recoveryGuidance: String,
        val retryAction: () -> Unit
    ) : ProgressErrorResponse()
    
    data class SupportiveEmptyState(
        val supportiveMessage: String,
        val recoveryGuidance: String
    ) : ProgressErrorResponse()
    
    data class GentleFallback(
        val supportiveMessage: String,
        val recoveryGuidance: String
    ) : ProgressErrorResponse()
    
    data class ReassuringSolution(
        val supportiveMessage: String,
        val recoveryGuidance: String
    ) : ProgressErrorResponse()
}

/**
 * Sealed class for progress errors
 */
sealed class ProgressError {
    object NetworkError : ProgressError()
    object DataNotFound : ProgressError()
    object CacheError : ProgressError()
    data class UnknownError(val exception: Throwable) : ProgressError()
    
    companion object {
        fun fromException(exception: Throwable): ProgressError {
            return when (exception) {
                is java.net.UnknownHostException,
                is java.net.SocketTimeoutException -> NetworkError
                is NoSuchElementException -> DataNotFound
                else -> UnknownError(exception)
            }
        }
    }
}



/**
 * Empty state types for supportive messaging
 */
enum class EmptyStateType {
    NO_DATA_YET,
    WEEK_NO_ACTIVITY,
    OFFLINE_NO_SYNC
}



/**
 * Data class for encouraging loading states
 */
data class ProgressLoadingState(
    val message: String,
    val encouragingContext: String,
    val showProgress: Boolean,
    val progressPercentage: Int
)

/**
 * Data class for supportive empty states
 */
data class ProgressEmptyState(
    val title: String,
    val message: String,
    val actionText: String,
    val encouragingContext: String,
    val iconResource: Int,
    val showMotivationalTip: Boolean,
    val motivationalTip: String?
)

/**
 * Data class for celebratory success states
 */
data class ProgressSuccessState(
    val title: String,
    val message: String,
    val celebratoryMessage: String,
    val encouragingContext: String,
    val showCelebration: Boolean,
    val celebrationType: CelebrationType
)

/**
 * Data class for offline states with supportive messaging
 */
data class ProgressOfflineState(
    val title: String,
    val message: String,
    val supportiveAction: String,
    val encouragingContext: String,
    val showOfflineIndicator: Boolean,
    val canContinueOffline: Boolean
)