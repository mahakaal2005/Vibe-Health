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
 * ArticleAdapter - Specialized adapter for health articles
 * Implements distinct UI/UX for article content with reading-focused design
 */
class ArticleAdapter(
    private val onArticleClick: (HealthContent.Article) -> Unit,
    private val onArticleShare: (HealthContent.Article) -> Unit,
    private val onArticleBookmark: (HealthContent.Article) -> Unit,
    private val accessibilityHelper: AccessibilityHelper
) : ListAdapter<HealthContent.Article, ArticleAdapter.ArticleViewHolder>(ArticleDiffCallback()) {
    
    companion object {
        private const val TAG = "DISCOVER_UI"
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        Log.d(TAG, "Creating article-specific ViewHolder")
        val binding = ItemHealthContentCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ArticleViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = getItem(position)
        Log.d(TAG, "Binding article card: ${article.title}")
        holder.bind(article)
    }
    
    fun filter(query: String) {
        // Implement article filtering
        Log.d("DISCOVER_CONTENT", "Filtering articles: $query")
    }
    
    fun filterByReadingTime(readingTimes: List<String>) {
        // Implement reading time filtering
        Log.d("DISCOVER_CONTENT", "Filtering articles by reading time: $readingTimes")
    }
    
    inner class ArticleViewHolder(
        private val binding: ItemHealthContentCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(article: HealthContent.Article) {
            Log.d(TAG, "Binding article with reading-focused design: ${article.title}")
            
            // Set article-specific content
            binding.contentTitle.text = article.title
            binding.contentDescription.text = article.description
            binding.supportiveContext.text = article.supportiveContext
            binding.contentCategory.text = article.category.displayName
            binding.contentDuration.text = "${article.readingTimeMinutes} min read"
            
            // Load article thumbnail image
            loadArticleThumbnail(article)
            
            // Setup accessibility for articles
            setupArticleAccessibility(article)
            
            // Set click listeners
            binding.root.setOnClickListener {
                Log.d(TAG, "Article card clicked: ${article.title}")
                onArticleClick(article)
            }
            
            binding.shareButton.setOnClickListener {
                Log.d(TAG, "Article share clicked: ${article.title}")
                onArticleShare(article)
            }
        }
        
        private fun loadArticleThumbnail(article: HealthContent.Article) {
            val context = binding.root.context
            
            // Create optimized request options for faster loading
            val requestOptions = RequestOptions()
                .placeholder(R.color.sage_green_light) // Light placeholder while loading
                .error(R.drawable.ic_discover) // Fallback icon for articles if image fails to load
                .transform(RoundedCorners(16)) // Rounded corners for better visual appeal
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both original and resized
                .skipMemoryCache(false) // Use memory cache for faster access
                .priority(Priority.HIGH) // High priority for visible content
            
            // Load thumbnail image using Glide with optimizations
            if (!article.thumbnailUrl.isNullOrEmpty()) {
                Log.d(TAG, "Loading article thumbnail: ${article.thumbnailUrl}")
                Glide.with(context)
                    .load(article.thumbnailUrl)
                    .apply(requestOptions)
                    .thumbnail(0.1f) // Load low-res thumbnail first for faster perceived loading
                    .into(binding.contentThumbnail)
            } else {
                Log.d(TAG, "No thumbnail URL available for article: ${article.title}")
                // Set fallback image for articles without thumbnails
                Glide.with(context)
                    .load(R.drawable.ic_discover)
                    .apply(requestOptions)
                    .into(binding.contentThumbnail)
            }
        }
        
        private fun setupArticleAccessibility(article: HealthContent.Article) {
            val articleDescription = "Health article: ${article.title}. " +
                    "${article.description} " +
                    "Reading time: ${article.readingTimeMinutes} minutes. " +
                    "By ${article.author}. Double tap to read."
            
            accessibilityHelper.setContentDescription(
                binding.root,
                articleDescription,
                "Health article card"
            )
        }
    }
    
    private class ArticleDiffCallback : DiffUtil.ItemCallback<HealthContent.Article>() {
        override fun areItemsTheSame(oldItem: HealthContent.Article, newItem: HealthContent.Article): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: HealthContent.Article, newItem: HealthContent.Article): Boolean {
            return oldItem == newItem
        }
    }
}