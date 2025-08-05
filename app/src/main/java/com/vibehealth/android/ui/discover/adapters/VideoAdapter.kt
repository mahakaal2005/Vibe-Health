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
 * VideoAdapter - Specialized adapter for health videos
 * Implements distinct UI/UX for video content with visual-focused design
 */
class VideoAdapter(
    private val onVideoClick: (HealthContent.Video) -> Unit,
    private val onVideoShare: (HealthContent.Video) -> Unit,
    private val onVideoBookmark: (HealthContent.Video) -> Unit,
    private val onVideoPreview: (HealthContent.Video) -> Unit,
    private val accessibilityHelper: AccessibilityHelper
) : ListAdapter<HealthContent.Video, VideoAdapter.VideoViewHolder>(VideoDiffCallback()) {
    
    companion object {
        private const val TAG = "DISCOVER_UI"
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        Log.d(TAG, "Creating video-specific ViewHolder")
        val binding = ItemHealthContentCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VideoViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = getItem(position)
        Log.d(TAG, "Binding video card: ${video.title}")
        holder.bind(video)
    }
    
    fun isFeaturedVideo(position: Int): Boolean {
        // Determine if video should span full width
        return position == 0 // First video is featured
    }
    
    fun filter(query: String) {
        // Implement video filtering
        Log.d("DISCOVER_CONTENT", "Filtering videos: $query")
    }
    
    fun filterByDuration(durations: List<String>) {
        // Implement duration filtering
        Log.d("DISCOVER_CONTENT", "Filtering videos by duration: $durations")
    }
    
    fun filterByChannels(channels: List<String>) {
        // Implement channel filtering
        Log.d("DISCOVER_CONTENT", "Filtering videos by channels: $channels")
    }
    
    fun shuffleVideos() {
        // Shuffle video order
        Log.d("DISCOVER_CONTENT", "Shuffling videos")
    }
    
    inner class VideoViewHolder(
        private val binding: ItemHealthContentCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(video: HealthContent.Video) {
            Log.d(TAG, "Binding video with visual-focused design: ${video.title}")
            
            // Set video-specific content
            binding.contentTitle.text = video.title
            binding.contentDescription.text = video.description
            binding.supportiveContext.text = video.supportiveContext
            binding.contentCategory.text = video.category.displayName
            binding.contentDuration.text = "${video.durationMinutes} min watch"
            
            // Load video thumbnail image
            loadVideoThumbnail(video)
            
            // Setup accessibility for videos
            setupVideoAccessibility(video)
            
            // Set click listeners
            binding.root.setOnClickListener {
                Log.d(TAG, "Video card clicked: ${video.title}")
                onVideoClick(video)
            }
            
            binding.shareButton.setOnClickListener {
                Log.d(TAG, "Video share clicked: ${video.title}")
                onVideoShare(video)
            }
            
            // Add preview functionality
            binding.root.setOnLongClickListener {
                Log.d(TAG, "Video preview requested: ${video.title}")
                onVideoPreview(video)
                true
            }
        }
        
        private fun loadVideoThumbnail(video: HealthContent.Video) {
            val context = binding.root.context
            
            // Create optimized request options for faster loading
            val requestOptions = RequestOptions()
                .placeholder(R.color.sage_green_light) // Light placeholder while loading
                .error(R.drawable.ic_discover) // Fallback icon if image fails to load
                .transform(RoundedCorners(16)) // Rounded corners for better visual appeal
                .centerCrop()
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL) // Cache both original and resized
                .skipMemoryCache(false) // Use memory cache for faster access
                .priority(com.bumptech.glide.Priority.HIGH) // High priority for visible content
            
            // Load thumbnail image using Glide with optimizations
            if (!video.thumbnailUrl.isNullOrEmpty()) {
                Log.d(TAG, "Loading video thumbnail: ${video.thumbnailUrl}")
                Glide.with(context)
                    .load(video.thumbnailUrl)
                    .apply(requestOptions)
                    .thumbnail(0.1f) // Load low-res thumbnail first for faster perceived loading
                    .into(binding.contentThumbnail)
            } else {
                Log.d(TAG, "No thumbnail URL available for video: ${video.title}")
                // Set fallback image for videos without thumbnails
                Glide.with(context)
                    .load(R.drawable.ic_discover)
                    .apply(requestOptions)
                    .into(binding.contentThumbnail)
            }
        }
        
        private fun setupVideoAccessibility(video: HealthContent.Video) {
            val videoDescription = "Health video: ${video.title}. " +
                    "${video.description} " +
                    "Duration: ${video.durationMinutes} minutes. " +
                    "Channel: ${video.channelName}. " +
                    "Double tap to watch, long press to preview."
            
            accessibilityHelper.setContentDescription(
                binding.root,
                videoDescription,
                "Health video card"
            )
        }
    }
    
    private class VideoDiffCallback : DiffUtil.ItemCallback<HealthContent.Video>() {
        override fun areItemsTheSame(oldItem: HealthContent.Video, newItem: HealthContent.Video): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: HealthContent.Video, newItem: HealthContent.Video): Boolean {
            return oldItem == newItem
        }
    }
}