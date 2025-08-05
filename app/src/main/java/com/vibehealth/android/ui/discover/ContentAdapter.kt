package com.vibehealth.android.ui.discover

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.vibehealth.android.databinding.ItemHealthContentCardBinding
import com.vibehealth.android.ui.discover.models.HealthContent
import com.vibehealth.android.core.accessibility.AccessibilityHelper

/**
 * ContentAdapter - RecyclerView adapter for health content cards
 * Implements Material Design 3 patterns with supportive, encouraging UI
 */
class ContentAdapter(
    private val onContentClick: (HealthContent) -> Unit,
    private val onContentShare: (HealthContent) -> Unit,
    private val accessibilityHelper: AccessibilityHelper
) : ListAdapter<HealthContent, ContentAdapter.ContentViewHolder>(ContentDiffCallback()) {
    
    companion object {
        private const val TAG = "DISCOVER_UI"
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentViewHolder {
        Log.d(TAG, "Creating content card view holder")
        val binding = ItemHealthContentCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContentViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ContentViewHolder, position: Int) {
        val content = getItem(position)
        Log.d(TAG, "Binding content card: ${content.title}")
        holder.bind(content)
    }
    
    inner class ContentViewHolder(
        private val binding: ItemHealthContentCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(content: HealthContent) {
            Log.d(TAG, "Binding health content: ${content.title}")
            Log.d("DISCOVER_ACCESSIBILITY", "Setting up accessibility for content card: ${content.title}")
            
            // Set content data
            binding.contentTitle.text = content.title
            binding.contentDescription.text = content.description
            binding.supportiveContext.text = content.supportiveContext
            binding.contentCategory.text = content.category.displayName
            
            // Set duration based on content type
            val durationText = when (content) {
                is HealthContent.Article -> {
                    "${content.readingTimeMinutes} min read"
                }
                is HealthContent.Video -> {
                    "${content.durationMinutes} min watch"
                }
                is HealthContent.News -> {
                    "Published ${android.text.format.DateFormat.format("MMM dd", content.publishedDate)}"
                }
            }
            binding.contentDuration.text = durationText
            
            // Setup accessibility support
            setupAccessibilityForCard(content, durationText)
            
            // Set click listeners
            binding.root.setOnClickListener {
                Log.d(TAG, "Content card clicked: ${content.title}")
                onContentClick(content)
            }
            
            binding.shareButton.setOnClickListener {
                Log.d(TAG, "Share button clicked for: ${content.title}")
                onContentShare(content)
            }
            
            Log.d(TAG, "Content card bound successfully with accessibility: ${content.title}")
        }
        
        private fun setupAccessibilityForCard(content: HealthContent, durationText: String) {
            // Set comprehensive content description for the card
            val contentType = when (content) {
                is HealthContent.Article -> "article"
                is HealthContent.Video -> "video"
                is HealthContent.News -> "news"
            }
            
            val cardDescription = "${content.category.displayName} $contentType: ${content.title}. " +
                    "${content.description} " +
                    "${content.supportiveContext} " +
                    "Duration: $durationText. Double tap to open."
            
            accessibilityHelper.setContentDescription(
                binding.root,
                cardDescription,
                "Wellness content card"
            )
            
            // Set accessibility role
            accessibilityHelper.setAccessibilityRole(binding.root, "android.widget.Button")
            
            // Setup share button accessibility
            accessibilityHelper.setContentDescription(
                binding.shareButton,
                "Share ${content.title}",
                "Double tap to share this wellness content"
            )
            
            // Ensure minimum touch targets
            accessibilityHelper.ensureMinimumTouchTarget(binding.root)
            accessibilityHelper.ensureMinimumTouchTarget(binding.shareButton)
            
            // Apply dynamic font scaling
            accessibilityHelper.applyDynamicFontScaling(binding.contentTitle, 18f)
            accessibilityHelper.applyDynamicFontScaling(binding.contentDescription, 14f)
            accessibilityHelper.applyDynamicFontScaling(binding.supportiveContext, 12f)
        }
    }
    
    private class ContentDiffCallback : DiffUtil.ItemCallback<HealthContent>() {
        override fun areItemsTheSame(oldItem: HealthContent, newItem: HealthContent): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: HealthContent, newItem: HealthContent): Boolean {
            return oldItem == newItem
        }
    }
}