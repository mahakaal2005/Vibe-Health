package com.vibehealth.android.ui.discover.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.vibehealth.android.core.accessibility.AccessibilityHelper
import com.vibehealth.android.databinding.FragmentNewsBinding
import com.vibehealth.android.ui.discover.DiscoverViewModel
import com.vibehealth.android.ui.discover.adapters.NewsAdapter
import com.vibehealth.android.ui.discover.models.ContentState
import com.vibehealth.android.ui.discover.models.HealthContent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * NewsFragment - Displays health news content with clean, minimal interface
 */
@AndroidEntryPoint
class NewsFragment : Fragment() {
    
    companion object {
        private const val TAG = "DISCOVER_CATEGORIZATION"
        
        fun newInstance(): NewsFragment {
            return NewsFragment()
        }
    }
    
    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!
    
    private val discoverViewModel: DiscoverViewModel by activityViewModels()
    
    private lateinit var newsAdapter: NewsAdapter
    
    @Inject
    lateinit var accessibilityHelper: AccessibilityHelper
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "Creating NewsFragment with distinct news-focused design")
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("DISCOVER_UI", "Implementing completely distinct category fragments")
        Log.d("DISCOVER_UI", "Setting up news-specific UI with timeliness-focused design")
        
        setupNewsRecyclerView()
        setupNewsSpecificUI()
        observeNewsContent()
        setupAccessibilityForNews()
        setupNewsRefresh()
        
        // Load news only if not already preloaded
        if (!discoverViewModel.isContentPreloaded()) {
            Log.d("DISCOVER_PERFORMANCE", "Content not preloaded, loading news now")
            discoverViewModel.loadNews()
        } else {
            Log.d("DISCOVER_PERFORMANCE", "Content already preloaded, skipping news load")
        }
    }
    
    private fun setupNewsRecyclerView() {
        Log.d("DISCOVER_UI", "Enhancing ContentAdapter for specialized content types")
        Log.d("DISCOVER_UI", "Creating NewsViewHolder with news-specific layout")
        
        newsAdapter = NewsAdapter(
            onNewsClick = { news -> navigateToNewsViewer(news) },
            onNewsShare = { news -> shareNews(news) },
            onNewsBookmark = { news -> bookmarkNews(news) },
            accessibilityHelper = accessibilityHelper
        )
        
        binding.newsRecyclerView.apply {
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
        
        Log.d("DISCOVER_UI", "News RecyclerView configured with specialized ViewHolder")
    }
    
    private fun setupNewsSpecificUI() {
        Log.d("DISCOVER_UI", "Implementing completely distinct category-specific UI layouts")
        Log.d("DISCOVER_UI", "News-specific UI configured with distinct visual treatment")
    }
    
    private fun observeNewsContent() {
        Log.d("DISCOVER_CONTENT", "Observing news-specific content")
        
        discoverViewModel.newsContent.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ContentState.Loading -> {
                    Log.d("DISCOVER_UI", "Showing news loading state")
                    binding.newsSwipeRefresh.isRefreshing = false
                    binding.newsLoadingState.root.visibility = View.VISIBLE
                    binding.newsRecyclerView.visibility = View.GONE
                    binding.newsEmptyState.visibility = View.GONE
                    binding.newsErrorState.root.visibility = View.GONE
                }
                is ContentState.Success -> {
                    Log.d("DISCOVER_UI", "Displaying news content: ${state.content.size} items")
                    binding.newsSwipeRefresh.isRefreshing = false
                    binding.newsLoadingState.root.visibility = View.GONE
                    binding.newsRecyclerView.visibility = View.VISIBLE
                    binding.newsEmptyState.visibility = View.GONE
                    binding.newsErrorState.root.visibility = View.GONE
                    
                    // Filter only news content and submit to adapter
                    val newsContent = state.content.filterIsInstance<HealthContent.News>()
                    newsAdapter.submitList(newsContent)
                }
                is ContentState.Error -> {
                    Log.d("DISCOVER_UI", "Showing news error state")
                    binding.newsSwipeRefresh.isRefreshing = false
                    binding.newsLoadingState.root.visibility = View.GONE
                    binding.newsRecyclerView.visibility = View.GONE
                    binding.newsEmptyState.visibility = View.GONE
                    binding.newsErrorState.root.visibility = View.VISIBLE
                }
                is ContentState.Empty -> {
                    Log.d("DISCOVER_UI", "Showing news empty state")
                    binding.newsSwipeRefresh.isRefreshing = false
                    binding.newsLoadingState.root.visibility = View.GONE
                    binding.newsRecyclerView.visibility = View.GONE
                    binding.newsEmptyState.visibility = View.VISIBLE
                    binding.newsErrorState.root.visibility = View.GONE
                    
                    binding.newsEmptyMessage.text = state.encouragingMessage
                }
            }
        }
        
        // Observe breaking news count
        discoverViewModel.breakingNewsCount.observe(viewLifecycleOwner) { count ->
            Log.d("DISCOVER_CONTENT", "Breaking news count updated: $count")
            Log.d("DISCOVER_CONTENT", "Checking for breaking news")
        }
    }
    
    private fun setupAccessibilityForNews() {
        Log.d("DISCOVER_ACCESSIBILITY", "Enhancing accessibility for categorized content")
        Log.d("DISCOVER_ACCESSIBILITY", "Setting up news-specific accessibility features")
        
        // Setup RecyclerView accessibility
        accessibilityHelper.setContentDescription(
            binding.newsRecyclerView,
            "Health news list",
            "Swipe to browse health news articles"
        )
        
        Log.d("DISCOVER_ACCESSIBILITY", "News accessibility features configured")
    }
    
    private fun setupNewsRefresh() {
        binding.newsSwipeRefresh.setOnRefreshListener {
            Log.d("DISCOVER_CONTENT", "Refreshing news content")
            discoverViewModel.loadNews()
        }
        
        // Configure refresh colors
        binding.newsSwipeRefresh.setColorSchemeColors(
            requireContext().getColor(android.R.color.holo_green_light),
            requireContext().getColor(android.R.color.holo_orange_light),
            requireContext().getColor(android.R.color.holo_red_light)
        )
    }
    
    private fun navigateToNewsViewer(news: HealthContent.News) {
        Log.d("DISCOVER_UI", "Navigating to news viewer: ${news.title}")
        
        // Navigate to specialized news viewer
        val intent = com.vibehealth.android.ui.discover.ContentViewerActivity.createIntent(
            requireContext(), 
            news.id,
            "news"
        )
        startActivity(intent)
    }
    
    private fun shareNews(news: HealthContent.News) {
        Log.d("DISCOVER_UI", "Sharing news: ${news.title}")
        
        // Implement news sharing
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, "${news.title}\n\n${news.sourceUrl}")
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Health News: ${news.title}")
        }
        
        startActivity(android.content.Intent.createChooser(shareIntent, "Share News"))
    }
    
    private fun bookmarkNews(news: HealthContent.News) {
        Log.d("DISCOVER_CONTENT", "Bookmarking news: ${news.title}")
        discoverViewModel.bookmarkContent(news)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}