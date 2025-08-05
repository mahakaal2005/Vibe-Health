package com.vibehealth.android.ui.discover.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

import com.vibehealth.android.databinding.FragmentVideosBinding
import com.vibehealth.android.ui.discover.DiscoverViewModel
import com.vibehealth.android.ui.discover.adapters.VideoAdapter
import com.vibehealth.android.ui.discover.decorations.VideoCardDecoration
import com.vibehealth.android.ui.discover.dialogs.VideoPreviewDialog
import com.vibehealth.android.ui.discover.models.HealthContent
import com.vibehealth.android.ui.discover.models.ContentState
import com.vibehealth.android.core.accessibility.AccessibilityHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * VideosFragment - Specialized fragment for health videos
 * Provides distinct UI/UX for video content with visual-focused design
 */
@AndroidEntryPoint
class VideosFragment : Fragment() {
    
    companion object {
        private const val TAG = "DISCOVER_CATEGORIZATION"
        
        fun newInstance(): VideosFragment {
            return VideosFragment()
        }
    }
    
    private var _binding: FragmentVideosBinding? = null
    private val binding get() = _binding!!
    
    private val discoverViewModel: DiscoverViewModel by viewModels({ requireParentFragment() })
    private lateinit var videoAdapter: VideoAdapter
    
    @Inject
    lateinit var accessibilityHelper: AccessibilityHelper
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "Creating VideosFragment with distinct video-focused design")
        _binding = FragmentVideosBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("DISCOVER_UI", "Implementing completely distinct category fragments")
        Log.d("DISCOVER_UI", "Setting up video-specific UI with visual-focused design")
        
        setupVideoRecyclerView()
        setupVideoSpecificUI()
        observeVideoContent()
        setupAccessibilityForVideos()
        
        // Load videos only if not already preloaded
        if (!discoverViewModel.isContentPreloaded()) {
            Log.d("DISCOVER_PERFORMANCE", "Content not preloaded, loading videos now")
            discoverViewModel.loadVideos()
        } else {
            Log.d("DISCOVER_PERFORMANCE", "Content already preloaded, skipping videos load")
        }
    }
    
    private fun setupVideoRecyclerView() {
        Log.d("DISCOVER_UI", "Enhancing ContentAdapter for specialized content types")
        Log.d("DISCOVER_UI", "Creating VideoViewHolder with video-specific layout")
        
        videoAdapter = VideoAdapter(
            onVideoClick = { video -> navigateToVideoViewer(video) },
            onVideoShare = { video -> shareVideo(video) },
            onVideoBookmark = { video -> bookmarkVideo(video) },
            onVideoPreview = { video -> previewVideo(video) },
            accessibilityHelper = accessibilityHelper
        )
        
        // Use LinearLayoutManager for consistent single-column layout
        binding.videosRecyclerView.apply {
            adapter = videoAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            // Add video-specific item decoration
            addItemDecoration(VideoCardDecoration())
        }
        
        Log.d("DISCOVER_UI", "Video RecyclerView configured with consistent single-column layout")
    }
    
    private fun setupVideoSpecificUI() {
        Log.d("DISCOVER_UI", "Implementing completely distinct category-specific UI layouts")
        
        // Setup SwipeRefreshLayout
        binding.videosSwipeRefresh.setOnRefreshListener {
            Log.d("DISCOVER_CONTENT", "Refreshing videos content")
            discoverViewModel.loadVideos()
        }
        
        // Configure refresh colors
        binding.videosSwipeRefresh.setColorSchemeColors(
            requireContext().getColor(android.R.color.holo_green_light),
            requireContext().getColor(android.R.color.holo_orange_light),
            requireContext().getColor(android.R.color.holo_red_light)
        )
        
        Log.d("DISCOVER_UI", "Video-specific UI configured with distinct visual treatment")
    }
    

    

    

    
    private fun observeVideoContent() {
        Log.d("DISCOVER_CONTENT", "Observing video-specific content")
        
        discoverViewModel.videoContent.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ContentState.Loading -> {
                    Log.d("DISCOVER_UI", "Showing video loading state")
                    showVideoLoadingState(state.message)
                }
                is ContentState.Success -> {
                    Log.d("DISCOVER_UI", "Showing videos: ${state.content.size} items")
                    showVideoContent(state.content.filterIsInstance<HealthContent.Video>())
                }
                is ContentState.Error -> {
                    Log.d("DISCOVER_ERRORS", "Showing video error state")
                    showVideoErrorState(state.message, state.retryAction)
                }
                is ContentState.Empty -> {
                    Log.d("DISCOVER_UI", "Showing video empty state")
                    showVideoEmptyState(state.encouragingMessage)
                }
            }
        }
    }
    
    private fun setupAccessibilityForVideos() {
        Log.d("DISCOVER_ACCESSIBILITY", "Enhancing accessibility for categorized content")
        Log.d("DISCOVER_ACCESSIBILITY", "Setting up video-specific accessibility features")
        
        // Set content description for videos section
        accessibilityHelper.setContentDescription(
            binding.root,
            "Health videos section. Watch engaging wellness videos to inspire your health journey",
            "Swipe to browse health videos"
        )
        
        // Setup search accessibility

        
        Log.d("DISCOVER_ACCESSIBILITY", "Video accessibility features configured")
    }
    
    private fun navigateToVideoViewer(video: HealthContent.Video) {
        Log.d("DISCOVER_UI", "Navigating to video viewer: ${video.title}")
        
        // Navigate to specialized video viewer
        val intent = com.vibehealth.android.ui.discover.ContentViewerActivity.createIntent(
            requireContext(), 
            video.id,
            "video"
        )
        startActivity(intent)
        
        // Track video engagement
        discoverViewModel.trackContentEngagement(video, "VIEW")
    }
    
    private fun shareVideo(video: HealthContent.Video) {
        Log.d("DISCOVER_UI", "Sharing video: ${video.title}")
        
        val shareText = "🎥 Watch this wellness video: ${video.title}\n\n" +
                "${video.description}\n\n" +
                "Duration: ${video.durationMinutes} minutes\n" +
                "Channel: ${video.channelName}\n\n" +
                "Shared from Vibe Health 🌿"
        
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Health Video: ${video.title}")
        }
        
        startActivity(android.content.Intent.createChooser(shareIntent, "Share video"))
        discoverViewModel.trackContentEngagement(video, "SHARE")
    }
    
    private fun bookmarkVideo(video: HealthContent.Video) {
        Log.d("DISCOVER_UI", "Bookmarking video: ${video.title}")
        discoverViewModel.bookmarkContent(video)
    }
    
    private fun previewVideo(video: HealthContent.Video) {
        Log.d("DISCOVER_UI", "Previewing video: ${video.title}")
        // Show video preview in a dialog or mini player
        showVideoPreviewDialog(video)
    }
    

    
    private fun showVideoPreviewDialog(video: HealthContent.Video) {
        // Show video preview in a dialog
        val previewDialog = VideoPreviewDialog.newInstance(video)
        previewDialog.show(childFragmentManager, "video_preview")
    }
    
    private fun showVideoLoadingState(message: String) {
        binding.videosSwipeRefresh.isRefreshing = false
        binding.videosLoadingState.root.visibility = View.VISIBLE
        binding.videosRecyclerView.visibility = View.GONE
        binding.videosErrorState.root.visibility = View.GONE
        binding.videosEmptyState.visibility = View.GONE
        
        binding.videosLoadingState.loadingMessage.text = "Loading inspiring wellness videos for you... 🎥"
    }
    
    private fun showVideoContent(videos: List<HealthContent.Video>) {
        binding.videosSwipeRefresh.isRefreshing = false
        binding.videosLoadingState.root.visibility = View.GONE
        binding.videosRecyclerView.visibility = View.VISIBLE
        binding.videosErrorState.root.visibility = View.GONE
        binding.videosEmptyState.visibility = View.GONE
        
        videoAdapter.submitList(videos)
        
        if (videos.isEmpty()) {
            showVideoEmptyState("No wellness videos available right now. Check back soon for inspiring content! 🎥")
        }
    }
    
    private fun showVideoErrorState(message: String, retryAction: () -> Unit) {
        binding.videosSwipeRefresh.isRefreshing = false
        binding.videosLoadingState.root.visibility = View.GONE
        binding.videosRecyclerView.visibility = View.GONE
        binding.videosErrorState.root.visibility = View.VISIBLE
        binding.videosEmptyState.visibility = View.GONE
        
        binding.videosErrorState.offlineMessage.text = "We're having trouble loading videos right now. Your wellness journey continues - let's try again! 🎥"
        binding.videosErrorState.retryButton.setOnClickListener { retryAction() }
    }
    
    private fun showVideoEmptyState(message: String) {
        binding.videosSwipeRefresh.isRefreshing = false
        binding.videosLoadingState.root.visibility = View.GONE
        binding.videosRecyclerView.visibility = View.GONE
        binding.videosErrorState.root.visibility = View.GONE
        binding.videosEmptyState.visibility = View.VISIBLE
        
        binding.videosEmptyMessage.text = message
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "VideosFragment view destroyed - memory cleanup completed")
    }
}