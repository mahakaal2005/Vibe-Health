package com.vibehealth.android.ui.discover.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.Priority
import com.vibehealth.android.R
import com.vibehealth.android.databinding.ItemHealthContentCardBinding
import com.vibehealth.android.ui.discover.models.HealthContent
import com.vibehealth.android.core.accessibility.AccessibilityHelper

/**
 * NewsAdapter - Specialized adapter for health news
 * Implements distinct UI/UX for news content with timeliness-focused design
 */
class NewsAdapter(
    private val onNewsClick: (HealthContent.News) -> Unit,
    private val onNewsShare: (HealthContent.News) -> Unit,
    private val onNewsBookmark: (HealthContent.News) -> Unit,
    private val accessibilityHelper: AccessibilityHelper
) : ListAdapter<HealthContent.News, NewsAdapter.NewsViewHolder>(NewsDiffCallback()) {
    
    companion object {
        private const val TAG = "DISCOVER_UI"
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        Log.d(TAG, "Creating news-specific ViewHolder")
        val binding = ItemHealthContentCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NewsViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = getItem(position)
        Log.d(TAG, "Binding news card: ${news.title}")
        holder.bind(news)
    }
    
    fun filter(query: String) {
        // Implement news filtering
        Log.d("DISCOVER_CONTENT", "Filtering news: $query")
    }
    
    fun filterBySources(sources: List<String>) {
        // Implement source filtering
        Log.d("DISCOVER_CONTENT", "Filtering news by sources: $sources")
    }
    
    fun filterByRecency(recency: List<String>) {
        // Implement recency filtering
        Log.d("DISCOVER_CONTENT", "Filtering news by recency: $recency")
    }
    
    fun showBreakingNewsOnly() {
        // Show only breaking news
        Log.d("DISCOVER_CONTENT", "Showing breaking news only")
    }
    
    inner class NewsViewHolder(
        private val binding: ItemHealthContentCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(news: HealthContent.News) {
            Log.d(TAG, "Binding news with timeliness-focused design: ${news.title}")
            
            // Set news-specific content
            binding.contentTitle.text = news.title
            binding.contentDescription.text = news.summary
            binding.supportiveContext.text = news.supportiveContext
            binding.contentCategory.text = news.category.displayName
            
            // Format publish date for news
            val dateFormat = android.text.format.DateFormat.format("MMM dd, yyyy", news.publishedDate)
            binding.contentDuration.text = "Published $dateFormat"
            
            // Load news thumbnail image
            loadNewsThumbnail(news)
            
            // Setup accessibility for news
            setupNewsAccessibility(news)
            
            // Set click listeners
            binding.root.setOnClickListener {
                Log.d(TAG, "News card clicked: ${news.title}")
                onNewsClick(news)
            }
            
            binding.shareButton.setOnClickListener {
                Log.d(TAG, "News share clicked: ${news.title}")
                onNewsShare(news)
            }
        }
        
        private fun loadNewsThumbnail(news: HealthContent.News) {
            val context = binding.root.context
            
            // Create optimized request options for faster loading
            val requestOptions = RequestOptions()
                .placeholder(R.color.sage_green_light) // Light placeholder while loading
                .error(R.drawable.ic_discover) // Fallback icon for news if image fails to load
                .transform(RoundedCorners(16)) // Rounded corners for better visual appeal
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both original and resized
                .skipMemoryCache(false) // Use memory cache for faster access
                .priority(Priority.HIGH) // High priority for visible content
            
            // Load thumbnail image using Glide with optimizations
            if (!news.thumbnailUrl.isNullOrEmpty()) {
                Log.d(TAG, "Loading news thumbnail: ${news.thumbnailUrl}")
                Glide.with(context)
                    .load(news.thumbnailUrl)
                    .apply(requestOptions)
                    .thumbnail(0.1f) // Load low-res thumbnail first for faster perceived loading
                    .into(binding.contentThumbnail)
            } else {
                Log.d(TAG, "No thumbnail URL available for news: ${news.title}")
                // Set fallback image for news without thumbnails
                Glide.with(context)
                    .load(R.drawable.ic_discover)
                    .apply(requestOptions)
                    .into(binding.contentThumbnail)
            }
        }
        
        private fun setupNewsAccessibility(news: HealthContent.News) {
            val breakingText = if (news.isBreaking) "Breaking news: " else "Health news: "
            val newsDescription = "$breakingText${news.title}. " +
                    "${news.summary} " +
                    "Source: ${news.source}. " +
                    "Published: ${android.text.format.DateFormat.format("MMM dd, yyyy", news.publishedDate)}. " +
                    "Double tap to read."
            
            accessibilityHelper.setContentDescription(
                binding.root,
                newsDescription,
                "Health news card"
            )
        }
    }
    
    private class NewsDiffCallback : DiffUtil.ItemCallback<HealthContent.News>() {
        override fun areItemsTheSame(oldItem: HealthContent.News, newItem: HealthContent.News): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: HealthContent.News, newItem: HealthContent.News): Boolean {
            return oldItem == newItem
        }
    }
}