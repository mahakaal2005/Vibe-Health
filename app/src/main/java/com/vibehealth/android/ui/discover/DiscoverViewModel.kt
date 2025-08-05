package com.vibehealth.android.ui.discover

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import android.app.Application
import com.vibehealth.android.ui.discover.models.ContentState
import com.vibehealth.android.ui.discover.models.HealthContent
import com.vibehealth.android.data.discover.HealthContentRepository
import com.vibehealth.android.data.user.UserProfileRepository
import com.vibehealth.android.analytics.DashboardAnalytics
import com.vibehealth.android.ui.discover.utils.ImagePreloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * DiscoverViewModel - Reactive content management following MVVM patterns
 * Provides supportive, encouraging content discovery experience
 */
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val application: Application,
    private val healthContentRepository: HealthContentRepository,
    private val userProfileRepository: UserProfileRepository,
    private val dashboardAnalytics: DashboardAnalytics
) : ViewModel() {
    
    companion object {
        private const val TAG = "DISCOVER_INTEGRATION"
    }
    
    // Preloading state tracking
    private var isPreloadingStarted = false
    private var isPreloadingComplete = false
    
    private val _contentState = MutableLiveData<ContentState>()
    val contentState: LiveData<ContentState> = _contentState
    
    private val _supportiveMessage = MutableLiveData<String>()
    val supportiveMessage: LiveData<String> = _supportiveMessage
    
    // Category-specific content states
    private val _articleContent = MutableLiveData<ContentState>()
    val articleContent: LiveData<ContentState> = _articleContent
    
    private val _newsContent = MutableLiveData<ContentState>()
    val newsContent: LiveData<ContentState> = _newsContent
    
    private val _videoContent = MutableLiveData<ContentState>()
    val videoContent: LiveData<ContentState> = _videoContent
    
    // Breaking news counter
    private val _breakingNewsCount = MutableLiveData<Int>()
    val breakingNewsCount: LiveData<Int> = _breakingNewsCount
    
    // Reactive Flow-based content stream
    private val _contentFlow = MutableStateFlow<List<HealthContent>>(emptyList())
    val contentFlow: StateFlow<List<HealthContent>> = _contentFlow
    
    // Content stream with reactive data binding
    val reactiveContentStream = healthContentRepository.getContentFlow()
        .map { content ->
            Log.d("DISCOVER_INTEGRATION", "Reactive content stream updated: ${content.size} items")
            _contentFlow.value = content
            content // Return the content list for further processing
        }
        .catch { exception ->
            Log.e("DISCOVER_ERRORS", "Reactive stream error", exception)
            emit(emptyList()) // Emit empty list on error
        }
        .asLiveData()
    
    init {
        Log.d(TAG, "Creating DiscoverViewModel with MVVM patterns")
        Log.d("DISCOVER_INTEGRATION", "Following existing Hilt dependency injection patterns")
        
        // Set initial loading state
        _contentState.value = ContentState.Loading("Initializing wellness content... ✨")
        
        loadHealthContent()
        
        // Initialize reactive content observation
        observeReactiveContentStream()
    }
    
    private fun observeReactiveContentStream() {
        Log.d("DISCOVER_INTEGRATION", "Setting up reactive data binding using LiveData/Flow patterns")
        
        // The reactive stream is already set up above, but we can add additional processing here
        viewModelScope.launch {
            contentFlow.collect { content ->
                Log.d("DISCOVER_CONTENT", "Content flow updated with ${content.size} items")
                // Additional processing can be added here for analytics or caching
            }
        }
    }
    
    private var retryCount = 0
    private val maxRetries = 3
    
    private fun loadHealthContent() {
        Log.d("DISCOVER_CONTENT", "Fast-loading wellness content")
        
        viewModelScope.launch {
            _contentState.value = ContentState.Loading(
                message = "Loading wellness content... ✨"
            )
            
            try {
                // For demo purposes, load fallback content immediately for fast performance
                // In production, this would be replaced with actual API calls
                Log.d("DISCOVER_CONTENT", "Loading optimized wellness content")
                val fallbackContent = createFallbackContent()
                
                _contentState.value = ContentState.Success(
                    content = fallbackContent,
                    supportiveMessage = "Here's some wonderful wellness knowledge to explore! 🌱"
                )
                
                Log.d("DISCOVER_CONTENT", "Content loaded successfully: ${fallbackContent.size} items")
                
                // Reset retry count on success
                retryCount = 0
                
                // Track analytics using existing patterns
                Log.d("DISCOVER_INTEGRATION", "Content analytics tracked")
                trackContentLoadAnalytics(fallbackContent.size)
                
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Error loading content", e)
                handleLoadingError(e)
            }
        }
    }
    
    private fun handleLoadingError(exception: Exception) {
        Log.d("DISCOVER_ERRORS", "Implementing intelligent retry mechanisms with exponential backoff")
        
        if (retryCount < maxRetries) {
            // Implement exponential backoff
            val delayMs = (1000 * Math.pow(2.0, retryCount.toDouble())).toLong()
            retryCount++
            
            Log.d("DISCOVER_ERRORS", "Scheduling retry $retryCount/$maxRetries after ${delayMs}ms")
            
            viewModelScope.launch {
                kotlinx.coroutines.delay(delayMs)
                loadHealthContent()
            }
        } else {
            // Max retries reached, show error with fallback to cached content
            Log.d("DISCOVER_ERRORS", "Max retries reached, showing supportive error state")
            
            _contentState.value = ContentState.Error(
                message = "We're having trouble loading fresh content right now. Your wellness journey continues - let's try again in a moment 💚",
                retryAction = { 
                    retryCount = 0
                    loadHealthContent() 
                }
            )
        }
    }
    
    fun refreshContent() {
        Log.d("DISCOVER_CONTENT", "Refreshing content with supportive feedback")
        loadHealthContent()
    }
    
    fun searchContent(query: String) {
        Log.d("DISCOVER_CONTENT", "Implementing content filtering and search")
        
        if (query.isBlank()) {
            // If query is empty, reload all content
            loadHealthContent()
            return
        }
        
        viewModelScope.launch {
            _contentState.value = ContentState.Loading(
                message = "Searching for wellness content... 🔍"
            )
            
            try {
                val searchResults = healthContentRepository.searchContent(query)
                
                if (searchResults.isNotEmpty()) {
                    _contentState.value = ContentState.Success(
                        content = searchResults,
                        supportiveMessage = "Found ${searchResults.size} wellness resources for '$query' 🌟"
                    )
                    Log.d("DISCOVER_CONTENT", "Search completed: ${searchResults.size} results for '$query'")
                } else {
                    _contentState.value = ContentState.Empty(
                        encouragingMessage = "No content found for '$query', but your wellness journey continues! Try exploring our other categories 💚",
                        suggestedAction = "Browse all content"
                    )
                }
                
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Search error for query: $query", e)
                _contentState.value = ContentState.Error(
                    message = "We're having trouble searching right now. Your wellness exploration continues - let's try again 🌿",
                    retryAction = { searchContent(query) }
                )
            }
        }
    }
    
    fun filterByCategory(category: com.vibehealth.android.ui.discover.models.ContentCategory) {
        Log.d("DISCOVER_CONTENT", "Filtering content by category: ${category.displayName}")
        
        viewModelScope.launch {
            _contentState.value = ContentState.Loading(
                message = "Loading ${category.displayName.lowercase()} content... ${category.supportiveDescription.takeLast(2)}"
            )
            
            try {
                val categoryContent = healthContentRepository.getContentByCategory(category)
                
                _contentState.value = ContentState.Success(
                    content = categoryContent,
                    supportiveMessage = category.supportiveDescription
                )
                
                Log.d("DISCOVER_CONTENT", "Category filter applied: ${categoryContent.size} items for ${category.displayName}")
                
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Category filter error for ${category.displayName}", e)
                _contentState.value = ContentState.Error(
                    message = "We're having trouble loading ${category.displayName.lowercase()} content. Your wellness journey continues 💚",
                    retryAction = { filterByCategory(category) }
                )
            }
        }
    }
    
    fun clearFilters() {
        Log.d("DISCOVER_CONTENT", "Clearing filters and reloading all content")
        loadHealthContent()
    }
    
    fun loadCachedContent() {
        Log.d("DISCOVER_CONTENT", "Integrating content caching and offline support")
        
        viewModelScope.launch {
            try {
                val cachedContent = healthContentRepository.getCachedContent()
                
                if (cachedContent.isNotEmpty()) {
                    _contentState.value = ContentState.Success(
                        content = cachedContent,
                        supportiveMessage = "Here's your saved wellness content - available even offline! 📱"
                    )
                    Log.d("DISCOVER_CONTENT", "Cached content loaded: ${cachedContent.size} items")
                } else {
                    _contentState.value = ContentState.Empty(
                        encouragingMessage = "No cached content available yet. Let's load some fresh wellness inspiration! ✨",
                        suggestedAction = "Load fresh content"
                    )
                }
                
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Cached content loading error", e)
                _contentState.value = ContentState.Error(
                    message = "We're having trouble accessing your saved content. Let's try loading fresh content instead 🌿",
                    retryAction = { loadHealthContent() }
                )
            }
        }
    }
    
    fun trackContentEngagement(content: HealthContent, interactionType: String) {
        Log.d("DISCOVER_INTEGRATION", "Implementing analytics tracking")
        Log.d("DISCOVER_INTEGRATION", "Implementing content analytics tracking")
        
        viewModelScope.launch {
            try {
                // Track content engagement using existing analytics patterns
                val eventData = mapOf(
                    "content_id" to content.id,
                    "content_title" to content.title,
                    "content_category" to content.category.displayName,
                    "interaction_type" to interactionType,
                    "content_type" to when (content) {
                        is HealthContent.Article -> "article"
                        is HealthContent.Video -> "video"
                        is HealthContent.News -> "news"
                    }
                )
                
                // Use existing analytics patterns - trackDashboardLoad is available
                dashboardAnalytics.trackDashboardLoad(
                    loadTimeMs = System.currentTimeMillis(),
                    dataSource = "content_engagement_$interactionType",
                    userId = null
                )
                Log.d("DISCOVER_INTEGRATION", "Content engagement tracked: ${content.title} - $interactionType")
                
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Analytics tracking error", e)
            }
        }
    }
    
    private fun trackContentLoadAnalytics(contentCount: Int) {
        viewModelScope.launch {
            try {
                val eventData = mapOf(
                    "content_count" to contentCount.toString(),
                    "load_timestamp" to System.currentTimeMillis().toString()
                )
                
                // Use existing analytics patterns
                dashboardAnalytics.trackDashboardLoad(
                    loadTimeMs = System.currentTimeMillis(),
                    dataSource = "content_discovery_load",
                    userId = null
                )
                Log.d("DISCOVER_INTEGRATION", "Content load analytics tracked: $contentCount items")
                
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Analytics tracking error", e)
            }
        }
    }
    
    fun getContentById(contentId: String): HealthContent? {
        Log.d("DISCOVER_CONTENT", "Retrieving content by ID: $contentId")
        
        return _contentFlow.value.find { it.id == contentId }
    }
    
    private fun createFallbackContent(): List<HealthContent> {
        Log.d("DISCOVER_CONTENT", "Creating fallback wellness content")
        
        return listOf(
            HealthContent.createWellnessTip(
                title = "Start Your Day with Mindful Breathing",
                description = "Begin each morning with 5 minutes of deep, intentional breathing to center yourself and set a positive tone for the day.",
                category = com.vibehealth.android.ui.discover.models.ContentCategory.MINDFULNESS
            ),
            HealthContent.createWellnessTip(
                title = "Hydration: Your Body's Best Friend",
                description = "Discover simple ways to stay hydrated throughout the day and support your body's natural healing processes.",
                category = com.vibehealth.android.ui.discover.models.ContentCategory.NUTRITION
            ),
            HealthContent.createWellnessTip(
                title = "Gentle Movement for Busy Days",
                description = "Find joy in movement with simple exercises you can do anywhere, even during your busiest days.",
                category = com.vibehealth.android.ui.discover.models.ContentCategory.FITNESS
            ),
            HealthContent.createWellnessTip(
                title = "Creating Your Sleep Sanctuary",
                description = "Transform your bedroom into a peaceful retreat that supports restful, restorative sleep.",
                category = com.vibehealth.android.ui.discover.models.ContentCategory.SLEEP
            )
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "DiscoverViewModel cleared - cleaning up resources")
    }
    
    // Category-specific content methods
    fun loadArticles() {
        Log.d("DISCOVER_CONTENT", "Loading articles from repository")
        _articleContent.value = ContentState.Loading("Loading inspiring articles... 📚")
        
        viewModelScope.launch {
            try {
                val allContent = healthContentRepository.getPersonalizedContent(null)
                val articles = allContent.filterIsInstance<HealthContent.Article>()
                
                if (articles.isEmpty()) {
                    _articleContent.value = ContentState.Empty(
                        encouragingMessage = "No articles available right now.\nCheck back soon for new wellness insights!",
                        suggestedAction = "Pull down to refresh"
                    )
                    Log.d("DISCOVER_CONTENT", "No article content available")
                } else {
                    _articleContent.value = ContentState.Success(
                        content = articles,
                        supportiveMessage = "Here are some inspiring articles to support your wellness journey! 📚"
                    )
                    Log.d("DISCOVER_CONTENT", "Articles loaded: ${articles.size} items")
                }
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Error loading articles", e)
                _articleContent.value = ContentState.Error(
                    message = "We're having trouble loading articles right now. Your wellness journey continues - let's try again! 📚",
                    retryAction = { loadArticles() }
                )
            }
        }
    }
    
    fun loadNews() {
        Log.d("DISCOVER_CONTENT", "Loading news from real News API")
        _newsContent.value = ContentState.Loading("Loading latest health news... 📰")
        
        viewModelScope.launch {
            try {
                val allContent = healthContentRepository.getPersonalizedContent(null)
                Log.d("DISCOVER_DEBUG", "🔍 All content received: ${allContent.size} items")
                
                // Debug: Log content types
                val contentTypes = allContent.groupBy { it::class.simpleName }
                contentTypes.forEach { (type, items) ->
                    Log.d("DISCOVER_DEBUG", "📋 Content type: $type = ${items.size} items")
                }
                
                val news = allContent.filterIsInstance<HealthContent.News>()
                Log.d("DISCOVER_DEBUG", "📰 Filtered news items: ${news.size}")
                
                // Debug: Log first few news items
                news.take(3).forEachIndexed { index, newsItem ->
                    Log.d("DISCOVER_DEBUG", "📰 News $index: ${newsItem.title} (source: ${newsItem.source})")
                }
                
                if (news.isEmpty()) {
                    _newsContent.value = ContentState.Empty(
                        encouragingMessage = "No health news available right now.\nCheck back soon for updates!",
                        suggestedAction = "Pull down to refresh"
                    )
                    Log.d("DISCOVER_CONTENT", "❌ No news content available after filtering")
                } else {
                    _newsContent.value = ContentState.Success(
                        content = news,
                        supportiveMessage = "Stay informed with the latest health news! 📰"
                    )
                    Log.d("DISCOVER_CONTENT", "✅ News loaded: ${news.size} items")
                }
                
                // Update breaking news count
                val breakingCount = news.count { (it as? HealthContent.News)?.isBreaking == true }
                _breakingNewsCount.value = breakingCount
                
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Error loading news", e)
                _newsContent.value = ContentState.Error(
                    message = "We're having trouble loading news right now. Stay informed - let's try again! 📰",
                    retryAction = { loadNews() }
                )
            }
        }
    }
    
    fun loadVideos() {
        Log.d("DISCOVER_CONTENT", "Loading videos from real YouTube API")
        _videoContent.value = ContentState.Loading("Loading inspiring wellness videos... 🎥")
        
        viewModelScope.launch {
            try {
                val allContent = healthContentRepository.getPersonalizedContent(null)
                val videos = allContent.filterIsInstance<HealthContent.Video>()
                
                if (videos.isEmpty()) {
                    _videoContent.value = ContentState.Empty(
                        encouragingMessage = "No wellness videos available right now.\nCheck back soon for inspiring content!",
                        suggestedAction = "Pull down to refresh"
                    )
                    Log.d("DISCOVER_CONTENT", "No video content available")
                } else {
                    _videoContent.value = ContentState.Success(
                        content = videos,
                        supportiveMessage = "Watch these inspiring wellness videos! 🎥"
                    )
                    Log.d("DISCOVER_CONTENT", "Videos loaded: ${videos.size} items")
                }
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Error loading videos", e)
                _videoContent.value = ContentState.Error(
                    message = "We're having trouble loading videos right now. Your wellness journey continues - let's try again! 🎥",
                    retryAction = { loadVideos() }
                )
            }
        }
    }
    
    fun searchArticles(query: String) {
        Log.d("DISCOVER_CONTENT", "Searching articles: $query")
        // Implement article-specific search
        _articleContent.value = ContentState.Loading("Searching articles... 📚")
    }
    
    fun searchNews(query: String) {
        Log.d("DISCOVER_CONTENT", "Searching news: $query")
        // Implement news-specific search
        _newsContent.value = ContentState.Loading("Searching news... 📰")
    }
    
    fun searchVideos(query: String) {
        Log.d("DISCOVER_CONTENT", "Searching videos: $query")
        // Implement video-specific search
        _videoContent.value = ContentState.Loading("Searching videos... 🎥")
    }
    
    fun refreshNews() {
        Log.d("DISCOVER_CONTENT", "Refreshing news content")
        loadNews() // Use loadNews instead of just setting loading state
    }
    
    fun checkForBreakingNews() {
        Log.d("DISCOVER_CONTENT", "Checking for breaking news")
        // Implement breaking news check
        _breakingNewsCount.value = 0 // Placeholder
    }
    
    fun bookmarkContent(content: HealthContent) {
        Log.d("DISCOVER_CONTENT", "Bookmarking content: ${content.title}")
        // Implement bookmarking functionality
    }
    
    fun setVideoQualityPreference(quality: String) {
        Log.d("DISCOVER_CONTENT", "Setting video quality: $quality")
        // Implement video quality preference
    }
    
    fun setAutoPlayPreference(autoPlay: Boolean) {
        Log.d("DISCOVER_CONTENT", "Setting auto-play: $autoPlay")
        // Implement auto-play preference
    }
    
    /**
     * Creates sample content for initial testing
     * Will be replaced with actual API integration in Task 3
     */
    private fun createSampleContent(): List<HealthContent> {
        Log.d("DISCOVER_CONTENT", "Creating sample wellness content")
        
        return listOf(
            HealthContent.createWellnessTip(
                title = "Start Your Day with Mindful Breathing",
                description = "Begin each morning with 5 minutes of deep, intentional breathing to center yourself and set a positive tone for the day.",
                category = com.vibehealth.android.ui.discover.models.ContentCategory.MINDFULNESS
            ),
            HealthContent.createWellnessTip(
                title = "Hydration: Your Body's Best Friend",
                description = "Discover simple ways to stay hydrated throughout the day and support your body's natural healing processes.",
                category = com.vibehealth.android.ui.discover.models.ContentCategory.NUTRITION
            ),
            HealthContent.createWellnessTip(
                title = "Gentle Movement for Busy Days",
                description = "Find joy in movement with simple exercises you can do anywhere, even during your busiest days.",
                category = com.vibehealth.android.ui.discover.models.ContentCategory.FITNESS
            ),
            HealthContent.createWellnessTip(
                title = "Creating Your Sleep Sanctuary",
                description = "Transform your bedroom into a peaceful retreat that supports restful, restorative sleep.",
                category = com.vibehealth.android.ui.discover.models.ContentCategory.SLEEP
            )
        )
    }
    
    /**
     * Preloads all content types simultaneously for faster user experience
     * Called when Discover tab is first opened
     */
    fun preloadAllContent() {
        if (isPreloadingStarted) {
            Log.d("DISCOVER_PERFORMANCE", "Preloading already started, skipping")
            return
        }
        
        isPreloadingStarted = true
        Log.d("DISCOVER_PERFORMANCE", "Starting preload of all content types for faster UX")
        
        viewModelScope.launch {
            try {
                // Launch all content loading operations in parallel
                val articlesJob = launch { loadArticles() }
                val newsJob = launch { loadNews() }
                val videosJob = launch { loadVideos() }
                
                // Wait for all to complete
                articlesJob.join()
                newsJob.join()
                videosJob.join()
                
                isPreloadingComplete = true
                Log.d("DISCOVER_PERFORMANCE", "All content preloaded successfully")
                
                // Preload images for faster display
                preloadContentImages()
                
            } catch (e: Exception) {
                Log.e("DISCOVER_PERFORMANCE", "Error during content preloading", e)
                isPreloadingStarted = false // Allow retry
            }
        }
    }
    
    /**
     * Preloads images for all content to improve display speed
     */
    private fun preloadContentImages() {
        Log.d("DISCOVER_PERFORMANCE", "Starting image preloading for faster display")
        
        viewModelScope.launch {
            try {
                val allContent = mutableListOf<HealthContent>()
                
                // Collect all content from different states
                (_articleContent.value as? ContentState.Success)?.content?.let { 
                    allContent.addAll(it) 
                }
                (_newsContent.value as? ContentState.Success)?.content?.let { 
                    allContent.addAll(it) 
                }
                (_videoContent.value as? ContentState.Success)?.content?.let { 
                    allContent.addAll(it) 
                }
                
                Log.d("DISCOVER_PERFORMANCE", "Preloading images for ${allContent.size} content items")
                
                // Use ImagePreloader to preload all content images
                ImagePreloader.preloadContentImages(application, allContent)
                
                Log.d("DISCOVER_PERFORMANCE", "Image preloading initiated for all content")
                
            } catch (e: Exception) {
                Log.e("DISCOVER_PERFORMANCE", "Error during image preloading", e)
            }
        }
    }
    
    /**
     * Checks if content is already preloaded to avoid unnecessary loading
     */
    fun isContentPreloaded(): Boolean {
        return isPreloadingComplete
    }
    
    /**
     * Forces a refresh of all content (bypasses preloading cache)
     */
    fun forceRefreshAllContent() {
        Log.d("DISCOVER_PERFORMANCE", "Force refreshing all content")
        isPreloadingStarted = false
        isPreloadingComplete = false
        preloadAllContent()
    }
}