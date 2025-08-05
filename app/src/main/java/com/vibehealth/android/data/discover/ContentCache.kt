package com.vibehealth.android.data.discover

import android.util.Log
import com.vibehealth.android.ui.discover.models.HealthContent
import com.vibehealth.android.ui.discover.models.ContentCategory
import com.vibehealth.android.core.security.EncryptionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ContentCache - Intelligent caching for offline support
 * Follows existing caching patterns with secure storage
 */
interface ContentCache {
    suspend fun saveContent(content: List<HealthContent>)
    suspend fun getCachedContent(): List<HealthContent>
    suspend fun searchCachedContent(query: String): List<HealthContent>
    suspend fun getCachedContentByCategory(category: ContentCategory): List<HealthContent>
    suspend fun clearCache()
}

@Singleton
class ContentCacheImpl @Inject constructor(
    private val encryptionHelper: EncryptionHelper
) : ContentCache {
    
    companion object {
        private const val TAG = "DISCOVER_CONTENT"
    }
    
    // In-memory cache for now - would be replaced with Room database in production
    private var cachedContent: List<HealthContent> = emptyList()
    private var cacheTimestamp: Long = 0
    private val cacheValidityDuration = 24 * 60 * 60 * 1000L // 24 hours
    
    init {
        Log.d(TAG, "Implementing intelligent content caching")
        Log.d("DISCOVER_INTEGRATION", "Integrating with existing offline-first patterns")
    }
    
    override suspend fun saveContent(content: List<HealthContent>) {
        Log.d(TAG, "Caching content for offline access: ${content.size} items")
        
        withContext(Dispatchers.IO) {
            try {
                // In production, this would use Room database with encryption
                cachedContent = content
                cacheTimestamp = System.currentTimeMillis()
                
                Log.d(TAG, "Content cached successfully")
                
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Cache save error", e)
            }
        }
    }
    
    override suspend fun getCachedContent(): List<HealthContent> {
        Log.d(TAG, "Retrieving cached content for offline support")
        
        return withContext(Dispatchers.IO) {
            try {
                if (isCacheValid()) {
                    Log.d(TAG, "Cache is valid, returning ${cachedContent.size} items")
                    cachedContent
                } else {
                    Log.d(TAG, "Cache expired, returning empty list")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Cache retrieval error", e)
                emptyList()
            }
        }
    }
    
    override suspend fun searchCachedContent(query: String): List<HealthContent> {
        Log.d(TAG, "Searching cached content for: $query")
        
        return withContext(Dispatchers.IO) {
            try {
                val cached = getCachedContent()
                cached.filter { content ->
                    content.title.contains(query, ignoreCase = true) ||
                    content.description.contains(query, ignoreCase = true) ||
                    content.category.displayName.contains(query, ignoreCase = true)
                }
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Cache search error", e)
                emptyList()
            }
        }
    }
    
    override suspend fun getCachedContentByCategory(category: ContentCategory): List<HealthContent> {
        Log.d(TAG, "Retrieving cached content by category: ${category.displayName}")
        
        return withContext(Dispatchers.IO) {
            try {
                val cached = getCachedContent()
                cached.filter { it.category == category }
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Cache category retrieval error", e)
                emptyList()
            }
        }
    }
    
    override suspend fun clearCache() {
        Log.d(TAG, "Clearing content cache")
        
        withContext(Dispatchers.IO) {
            try {
                cachedContent = emptyList()
                cacheTimestamp = 0
                Log.d(TAG, "Cache cleared successfully")
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Cache clear error", e)
            }
        }
    }
    
    private fun isCacheValid(): Boolean {
        val currentTime = System.currentTimeMillis()
        val isValid = (currentTime - cacheTimestamp) < cacheValidityDuration
        Log.d(TAG, "Cache validity check: $isValid")
        return isValid
    }
}