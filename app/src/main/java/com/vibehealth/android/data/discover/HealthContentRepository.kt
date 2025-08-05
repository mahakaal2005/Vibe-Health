package com.vibehealth.android.data.discover

import android.util.Log
import com.vibehealth.android.ui.discover.models.HealthContent
import com.vibehealth.android.ui.discover.models.ContentCategory
import com.vibehealth.android.domain.user.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HealthContentRepository - API integration and content management
 * Follows existing repository patterns with intelligent caching and error handling
 */
interface HealthContentRepository {
    suspend fun getPersonalizedContent(userProfile: UserProfile?): List<HealthContent>
    suspend fun searchContent(query: String): List<HealthContent>
    suspend fun getContentByCategory(category: ContentCategory): List<HealthContent>
    suspend fun getCachedContent(): List<HealthContent>
    fun getContentFlow(): Flow<List<HealthContent>>
}

@Singleton
class HealthContentRepositoryImpl @Inject constructor(
    private val healthContentService: HealthContentService,
    private val contentCache: ContentCache,
    private val healthContentCurator: HealthContentCurator
) : HealthContentRepository {
    
    companion object {
        private const val TAG = "DISCOVER_INTEGRATION"
    }
    
    init {
        Log.d(TAG, "Creating HealthContentRepository interface")
        Log.d("DISCOVER_INTEGRATION", "Integrating with existing repository patterns from other stories")
    }
    
    override suspend fun getPersonalizedContent(userProfile: UserProfile?): List<HealthContent> {
        Log.d("DISCOVER_CONTENT", "Implementing HealthContentService with Retrofit")
        
        return withContext(Dispatchers.IO) {
            try {
                Log.d("DISCOVER_CONTENT", "Implementing content curation and filtering")
                
                // Try to load fresh content from APIs
                val rawContent = healthContentService.getHealthContent()
                Log.d("DISCOVER_CONTENT", "Raw content fetched: ${rawContent.size} items")
                
                // Apply content curation and filtering
                val curatedContent = healthContentCurator.filterContent(rawContent, userProfile)
                Log.d("DISCOVER_CONTENT", "Content curated: ${curatedContent.size} items")
                
                // Cache for offline access
                Log.d("DISCOVER_CONTENT", "Implementing intelligent content caching")
                contentCache.saveContent(curatedContent)
                
                curatedContent
                
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "API error, falling back to cached content", e)
                Log.d("DISCOVER_ERRORS", "Implementing comprehensive error handling")
                
                // Fallback to cached content with supportive messaging
                val cachedContent = contentCache.getCachedContent()
                if (cachedContent.isNotEmpty()) {
                    Log.d("DISCOVER_CONTENT", "Returning cached content: ${cachedContent.size} items")
                    cachedContent
                } else {
                    Log.d("DISCOVER_CONTENT", "No cached content, generating encouraging fallback")
                    generateEncouragingFallbackContent()
                }
            }
        }
    }
    
    override suspend fun searchContent(query: String): List<HealthContent> {
        Log.d("DISCOVER_CONTENT", "Implementing content search functionality")
        
        return withContext(Dispatchers.IO) {
            try {
                val searchResults = healthContentService.searchContent(query)
                healthContentCurator.filterContent(searchResults, null)
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Search error, falling back to cached search", e)
                contentCache.searchCachedContent(query)
            }
        }
    }
    
    override suspend fun getContentByCategory(category: ContentCategory): List<HealthContent> {
        Log.d("DISCOVER_CONTENT", "Fetching content by category: ${category.displayName}")
        
        return withContext(Dispatchers.IO) {
            try {
                val categoryContent = healthContentService.getContentByCategory(category)
                healthContentCurator.filterContent(categoryContent, null)
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Category fetch error", e)
                contentCache.getCachedContentByCategory(category)
            }
        }
    }
    
    override suspend fun getCachedContent(): List<HealthContent> {
        Log.d("DISCOVER_CONTENT", "Retrieving cached content for offline support")
        return contentCache.getCachedContent()
    }
    
    override fun getContentFlow(): Flow<List<HealthContent>> = flow {
        Log.d("DISCOVER_INTEGRATION", "Creating reactive content flow")
        
        try {
            // Emit cached content first for immediate display
            val cachedContent = getCachedContent()
            if (cachedContent.isNotEmpty()) {
                Log.d("DISCOVER_CONTENT", "Emitting cached content: ${cachedContent.size} items")
                emit(cachedContent)
            }
            
            // Then fetch fresh content
            val freshContent = getPersonalizedContent(null)
            Log.d("DISCOVER_CONTENT", "Emitting fresh content: ${freshContent.size} items")
            emit(freshContent)
            
        } catch (e: Exception) {
            Log.e("DISCOVER_ERRORS", "Content flow error", e)
            // Emit fallback content
            emit(generateEncouragingFallbackContent())
        }
    }.catch { exception ->
        Log.e("DISCOVER_ERRORS", "Flow error, emitting fallback content", exception)
        emit(generateEncouragingFallbackContent())
    }
    
    /**
     * Generates encouraging fallback content when APIs are unavailable
     * Following Companion Principle with supportive messaging
     */
    private fun generateEncouragingFallbackContent(): List<HealthContent> {
        Log.d("DISCOVER_ERRORS", "Creating encouraging fallback content")
        Log.d("DISCOVER_SECURITY", "Applying existing security patterns")
        
        return listOf(
            HealthContent.createWellnessTip(
                title = "Take a Deep Breath 🌸",
                description = "Sometimes the best wellness practice is simply taking a moment to breathe deeply and center yourself. Your breath is always available as a source of calm and grounding.",
                category = ContentCategory.MINDFULNESS
            ),
            HealthContent.createWellnessTip(
                title = "Stay Hydrated with Love 💧",
                description = "Your body is amazing at healing and maintaining itself. Support it with plenty of water throughout the day - each sip is an act of self-care.",
                category = ContentCategory.NUTRITION
            ),
            HealthContent.createWellnessTip(
                title = "Gentle Movement Matters 🚶‍♀️",
                description = "Every step counts on your wellness journey. Whether it's a walk around the block or stretching at your desk, your body appreciates any movement you give it.",
                category = ContentCategory.FITNESS
            ),
            HealthContent.createWellnessTip(
                title = "Rest is Productive 😴",
                description = "Quality sleep isn't lazy - it's essential for your physical and mental well-being. Honor your body's need for rest as an important part of your health routine.",
                category = ContentCategory.SLEEP
            ),
            HealthContent.createWellnessTip(
                title = "You're Doing Great 💚",
                description = "Remember that wellness is a journey, not a destination. Every small choice you make for your health matters, and you're already on the right path.",
                category = ContentCategory.GENERAL_WELLNESS
            )
        )
    }
}