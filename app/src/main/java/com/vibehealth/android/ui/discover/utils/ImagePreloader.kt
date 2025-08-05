package com.vibehealth.android.ui.discover.utils

import android.content.Context
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.vibehealth.android.ui.discover.models.HealthContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ImagePreloader - Utility for preloading content images for faster display
 */
object ImagePreloader {
    
    private const val TAG = "IMAGE_PRELOADER"
    
    /**
     * Preloads images for a list of content items
     */
    suspend fun preloadContentImages(context: Context, content: List<HealthContent>) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Starting preload of ${content.size} content images")
            
            val requestOptions = RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both original and resized
                .skipMemoryCache(false) // Use memory cache for faster access
            
            content.forEach { item ->
                try {
                    if (!item.thumbnailUrl.isNullOrEmpty()) {
                        Log.d(TAG, "Preloading image: ${item.thumbnailUrl}")
                        
                        // Preload the image into Glide's cache
                        Glide.with(context)
                            .load(item.thumbnailUrl)
                            .apply(requestOptions)
                            .preload(300, 200) // Preload at typical thumbnail size
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to preload image for ${item.title}: ${e.message}")
                }
            }
            
            Log.d(TAG, "Image preloading completed")
        }
    }
    
    /**
     * Preloads images for specific content types
     */
    suspend fun preloadVideoThumbnails(context: Context, videos: List<HealthContent.Video>) {
        preloadContentImages(context, videos)
    }
    
    suspend fun preloadNewsThumbnails(context: Context, news: List<HealthContent.News>) {
        preloadContentImages(context, news)
    }
    
    suspend fun preloadArticleThumbnails(context: Context, articles: List<HealthContent.Article>) {
        preloadContentImages(context, articles)
    }
    
    /**
     * Clears preloaded images from cache if needed
     */
    fun clearImageCache(context: Context) {
        Log.d(TAG, "Clearing image cache")
        Glide.get(context).clearMemory()
        
        // Clear disk cache on background thread
        Thread {
            Glide.get(context).clearDiskCache()
        }.start()
    }
}