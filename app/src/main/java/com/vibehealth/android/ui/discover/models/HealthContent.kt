package com.vibehealth.android.ui.discover.models

import java.util.Date
import java.util.UUID

/**
 * Health Content data models with Companion Principle integration
 * Provides supportive, encouraging context for wellness content
 */
sealed class HealthContent {
    abstract val id: String
    abstract val title: String
    abstract val description: String
    abstract val category: ContentCategory
    abstract val thumbnailUrl: String?
    abstract val sourceUrl: String
    abstract val publishedDate: Date
    abstract val supportiveContext: String
    
    data class Article(
        override val id: String,
        override val title: String,
        override val description: String,
        override val category: ContentCategory,
        override val thumbnailUrl: String?,
        override val sourceUrl: String,
        override val publishedDate: Date,
        override val supportiveContext: String,
        val author: String,
        val readingTimeMinutes: Int,
        val contentPreview: String
    ) : HealthContent()
    
    data class Video(
        override val id: String,
        override val title: String,
        override val description: String,
        override val category: ContentCategory,
        override val thumbnailUrl: String?,
        override val sourceUrl: String,
        override val publishedDate: Date,
        override val supportiveContext: String,
        val durationMinutes: Int,
        val videoUrl: String,
        val channelName: String
    ) : HealthContent()
    
    data class News(
        override val id: String,
        override val title: String,
        override val description: String,
        override val category: ContentCategory,
        override val thumbnailUrl: String?,
        override val sourceUrl: String,
        override val publishedDate: Date,
        override val supportiveContext: String,
        val source: String,
        val isBreaking: Boolean,
        val summary: String
    ) : HealthContent()
    
    companion object {
        fun createWellnessTip(
            title: String,
            description: String,
            category: ContentCategory
        ): Article {
            return Article(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                category = category,
                thumbnailUrl = null,
                sourceUrl = "",
                publishedDate = Date(),
                supportiveContext = "A gentle wellness reminder from your health companion 💚",
                author = "Vibe Health Team",
                readingTimeMinutes = 2,
                contentPreview = description
            )
        }
    }
}

enum class ContentCategory(val displayName: String, val supportiveDescription: String) {
    NUTRITION("Nutrition", "Nourish your body with knowledge about healthy eating 🥗"),
    FITNESS("Fitness", "Discover joyful ways to move your body 🏃‍♀️"),
    MINDFULNESS("Mindfulness", "Find peace and balance in your daily life 🧘‍♂️"),
    SLEEP("Sleep", "Learn about restful, restorative sleep 😴"),
    STRESS_MANAGEMENT("Stress Management", "Gentle techniques for managing life's challenges 🌸"),
    GENERAL_WELLNESS("General Wellness", "Holistic approaches to feeling your best ✨")
}

/**
 * UI State management for content discovery
 */
sealed class ContentState {
    data class Loading(val message: String) : ContentState()
    
    data class Success(
        val content: List<HealthContent>,
        val supportiveMessage: String
    ) : ContentState()
    
    data class Error(
        val message: String,
        val retryAction: () -> Unit
    ) : ContentState()
    
    data class Empty(
        val encouragingMessage: String,
        val suggestedAction: String
    ) : ContentState()
}

data class ContentInteraction(
    val contentId: String,
    val interactionType: InteractionType,
    val timestamp: Date,
    val durationSeconds: Int? = null
)

enum class InteractionType {
    VIEW,
    SHARE,
    BOOKMARK,
    LIKE,
    COMPLETE_READ,
    COMPLETE_WATCH
}