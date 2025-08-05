package com.vibehealth.android.data.discover.api

import android.util.Log
import com.vibehealth.android.ui.discover.models.ContentCategory
import com.vibehealth.android.ui.discover.models.HealthContent
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * Mappers to convert API responses to domain models
 */

object ApiResponseMappers {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    /**
     * Convert News API article to HealthContent.News
     */
    fun NewsApiArticle.toHealthContent(): HealthContent.News {
        val publishedDate = try {
            dateFormat.parse(publishedAt) ?: Date()
        } catch (e: Exception) {
            Log.w("API_MAPPER", "Failed to parse date: $publishedAt", e)
            Date()
        }
        
        // Determine if this is breaking news based on recency and keywords
        val isBreaking = isBreakingNews(title, description, publishedDate)
        
        // Categorize content based on keywords
        val category = categorizeNewsContent(title, description)
        
        // Ensure thumbnail URL is properly handled - use urlToImage from News API
        val thumbnailUrl = if (!urlToImage.isNullOrBlank()) {
            urlToImage
        } else {
            // Fallback to a health-related stock image if no image provided
            "https://images.unsplash.com/photo-1559757148-5c350d0d3c56?w=400&h=300&fit=crop&crop=center"
        }
        
        Log.d("API_MAPPER", "News article thumbnail: ${if (urlToImage.isNullOrBlank()) "Using fallback" else "Using API image"}")
        
        return HealthContent.News(
            id = "news_${url.hashCode()}",
            title = title,
            description = description ?: "",
            category = category,
            thumbnailUrl = thumbnailUrl,
            sourceUrl = url,
            publishedDate = publishedDate,
            supportiveContext = generateSupportiveContext(title, category),
            source = source.name,
            isBreaking = isBreaking,
            summary = content?.take(200) ?: description?.take(200) ?: ""
        )
    }
    
    /**
     * Convert YouTube search item to HealthContent.Video
     */
    fun YouTubeSearchItem.toHealthContent(durationMinutes: Int = 10): HealthContent.Video {
        val publishedDate = try {
            dateFormat.parse(snippet.publishedAt) ?: Date()
        } catch (e: Exception) {
            Log.w("API_MAPPER", "Failed to parse YouTube date: ${snippet.publishedAt}", e)
            Date()
        }
        
        // Categorize video content
        val category = categorizeVideoContent(snippet.title, snippet.description)
        
        // Get best thumbnail
        val thumbnailUrl = snippet.thumbnails.high?.url 
            ?: snippet.thumbnails.medium?.url 
            ?: snippet.thumbnails.default?.url
        
        return HealthContent.Video(
            id = "video_${id.videoId}",
            title = snippet.title,
            description = snippet.description,
            category = category,
            thumbnailUrl = thumbnailUrl,
            sourceUrl = "https://youtube.com/watch?v=${id.videoId}",
            publishedDate = publishedDate,
            supportiveContext = generateSupportiveContext(snippet.title, category),
            durationMinutes = durationMinutes,
            videoUrl = "https://youtube.com/watch?v=${id.videoId}",
            channelName = snippet.channelTitle
        )
    }
    
    /**
     * Parse YouTube ISO 8601 duration (PT4M13S) to minutes
     */
    fun parseYouTubeDuration(duration: String): Int {
        return try {
            val pattern = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
            val matcher = pattern.matcher(duration)
            
            if (matcher.matches()) {
                val hours = matcher.group(1)?.toIntOrNull() ?: 0
                val minutes = matcher.group(2)?.toIntOrNull() ?: 0
                val seconds = matcher.group(3)?.toIntOrNull() ?: 0
                
                hours * 60 + minutes + if (seconds > 30) 1 else 0
            } else {
                10 // Default fallback
            }
        } catch (e: Exception) {
            Log.w("API_MAPPER", "Failed to parse duration: $duration", e)
            10 // Default fallback
        }
    }
    
    private fun isBreakingNews(title: String, description: String?, publishedDate: Date): Boolean {
        val now = Date()
        val hoursSincePublished = (now.time - publishedDate.time) / (1000 * 60 * 60)
        
        // Consider breaking if published within last 6 hours and contains breaking keywords
        val isRecent = hoursSincePublished <= 6
        val hasBreakingKeywords = listOf("breaking", "urgent", "alert", "just in", "developing")
            .any { keyword ->
                title.contains(keyword, ignoreCase = true) || 
                description?.contains(keyword, ignoreCase = true) == true
            }
        
        return isRecent && hasBreakingKeywords
    }
    
    private fun categorizeNewsContent(title: String, description: String?): ContentCategory {
        val text = "$title ${description ?: ""}".lowercase()
        
        return when {
            text.contains("fitness") || text.contains("exercise") || text.contains("workout") -> ContentCategory.FITNESS
            text.contains("nutrition") || text.contains("diet") || text.contains("food") -> ContentCategory.NUTRITION
            text.contains("mental") || text.contains("mindfulness") || text.contains("meditation") -> ContentCategory.MINDFULNESS
            text.contains("sleep") || text.contains("rest") || text.contains("insomnia") -> ContentCategory.SLEEP
            text.contains("stress") || text.contains("anxiety") || text.contains("pressure") -> ContentCategory.STRESS_MANAGEMENT
            else -> ContentCategory.GENERAL_WELLNESS // Default category
        }
    }
    
    private fun categorizeVideoContent(title: String, description: String): ContentCategory {
        val text = "$title $description".lowercase()
        
        return when {
            text.contains("yoga") || text.contains("workout") || text.contains("exercise") || text.contains("fitness") -> ContentCategory.FITNESS
            text.contains("nutrition") || text.contains("cooking") || text.contains("recipe") || text.contains("meal") -> ContentCategory.NUTRITION
            text.contains("meditation") || text.contains("mindfulness") || text.contains("breathing") -> ContentCategory.MINDFULNESS
            text.contains("sleep") || text.contains("rest") || text.contains("bedtime") -> ContentCategory.SLEEP
            text.contains("stress") || text.contains("anxiety") || text.contains("calm") -> ContentCategory.STRESS_MANAGEMENT
            else -> ContentCategory.GENERAL_WELLNESS // Default category
        }
    }
    
    private fun generateSupportiveContext(title: String, category: ContentCategory): String {
        return when (category) {
            ContentCategory.FITNESS -> "Movement and fitness are powerful tools for building strength, energy, and confidence. This content supports your journey toward physical wellness."
            ContentCategory.NUTRITION -> "Nourishing your body with good nutrition is an act of self-care that supports your overall health and vitality."
            ContentCategory.MINDFULNESS -> "Taking time for mindfulness and mental wellness helps you stay centered, reduce stress, and connect with your inner wisdom."
            ContentCategory.SLEEP -> "Quality sleep is essential for your physical and mental wellbeing. This content helps you create healthy sleep habits."
            ContentCategory.STRESS_MANAGEMENT -> "Managing stress with gentle, effective techniques supports your overall wellness and helps you navigate life's challenges."
            ContentCategory.GENERAL_WELLNESS -> "Supporting your overall wellness journey with balanced, holistic approaches to health and happiness."
        }
    }
}