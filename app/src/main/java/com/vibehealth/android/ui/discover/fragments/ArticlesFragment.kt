package com.vibehealth.android.ui.discover.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.vibehealth.android.databinding.FragmentArticlesBinding
import com.vibehealth.android.ui.discover.DiscoverViewModel
import com.vibehealth.android.ui.discover.adapters.ArticleAdapter
import com.vibehealth.android.ui.discover.decorations.ArticleCardDecoration
import com.vibehealth.android.ui.discover.models.HealthContent
import com.vibehealth.android.ui.discover.models.ContentState
import com.vibehealth.android.core.accessibility.AccessibilityHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * ArticlesFragment - Specialized fragment for health articles
 * Provides distinct UI/UX for article content with reading-focused design
 */
@AndroidEntryPoint
class ArticlesFragment : Fragment() {
    
    companion object {
        private const val TAG = "DISCOVER_CATEGORIZATION"
        
        fun newInstance(): ArticlesFragment {
            return ArticlesFragment()
        }
    }
    
    private var _binding: FragmentArticlesBinding? = null
    private val binding get() = _binding!!
    
    private val discoverViewModel: DiscoverViewModel by viewModels({ requireParentFragment() })
    private lateinit var articleAdapter: ArticleAdapter
    
    @Inject
    lateinit var accessibilityHelper: AccessibilityHelper
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "Creating ArticlesFragment with distinct article-focused design")
        _binding = FragmentArticlesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("DISCOVER_UI", "Implementing completely distinct category fragments")
        Log.d("DISCOVER_UI", "Setting up article-specific UI with reading-focused design")
        
        setupArticleRecyclerView()
        setupArticleSpecificUI()
        observeArticleContent()
        setupAccessibilityForArticles()
        
        // Load articles only if not already preloaded
        if (!discoverViewModel.isContentPreloaded()) {
            Log.d("DISCOVER_PERFORMANCE", "Content not preloaded, loading articles now")
            discoverViewModel.loadArticles()
        } else {
            Log.d("DISCOVER_PERFORMANCE", "Content already preloaded, skipping articles load")
        }
    }
    
    private fun setupArticleRecyclerView() {
        Log.d("DISCOVER_UI", "Enhancing ContentAdapter for specialized content types")
        Log.d("DISCOVER_UI", "Creating ArticleViewHolder with article-specific layout")
        
        articleAdapter = ArticleAdapter(
            onArticleClick = { article -> navigateToArticleViewer(article) },
            onArticleShare = { article -> shareArticle(article) },
            onArticleBookmark = { article -> bookmarkArticle(article) },
            accessibilityHelper = accessibilityHelper
        )
        
        binding.articlesRecyclerView.apply {
            adapter = articleAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // Add article-specific item decoration
            addItemDecoration(ArticleCardDecoration())
        }
        
        Log.d("DISCOVER_UI", "Article RecyclerView configured with specialized ViewHolder")
    }
    
    private fun setupArticleSpecificUI() {
        Log.d("DISCOVER_UI", "Implementing completely distinct category-specific UI layouts")
        
        // Setup SwipeRefreshLayout
        binding.articlesSwipeRefresh.setOnRefreshListener {
            Log.d("DISCOVER_CONTENT", "Refreshing articles content")
            discoverViewModel.loadArticles()
        }
        
        // Configure refresh colors
        binding.articlesSwipeRefresh.setColorSchemeColors(
            requireContext().getColor(android.R.color.holo_green_light),
            requireContext().getColor(android.R.color.holo_orange_light),
            requireContext().getColor(android.R.color.holo_red_light)
        )
        
        Log.d("DISCOVER_UI", "Article-specific UI configured with distinct visual treatment")
    }
    

    
    private fun observeArticleContent() {
        Log.d("DISCOVER_CONTENT", "Observing article-specific content")
        
        discoverViewModel.articleContent.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ContentState.Loading -> {
                    Log.d("DISCOVER_UI", "Showing article loading state")
                    showArticleLoadingState(state.message)
                }
                is ContentState.Success -> {
                    Log.d("DISCOVER_UI", "Showing articles: ${state.content.size} items")
                    showArticleContent(state.content.filterIsInstance<HealthContent.Article>())
                }
                is ContentState.Error -> {
                    Log.d("DISCOVER_ERRORS", "Showing article error state")
                    showArticleErrorState(state.message, state.retryAction)
                }
                is ContentState.Empty -> {
                    Log.d("DISCOVER_UI", "Showing article empty state")
                    showArticleEmptyState(state.encouragingMessage)
                }
            }
        }
    }
    
    private fun setupAccessibilityForArticles() {
        Log.d("DISCOVER_ACCESSIBILITY", "Enhancing accessibility for categorized content")
        Log.d("DISCOVER_ACCESSIBILITY", "Setting up article-specific accessibility features")
        
        // Set content description for articles section
        accessibilityHelper.setContentDescription(
            binding.root,
            "Health articles section. Find in-depth wellness articles to support your learning journey",
            "Swipe to browse health articles"
        )
        
        // Setup RecyclerView accessibility
        accessibilityHelper.setContentDescription(
            binding.articlesRecyclerView,
            "Health articles list",
            "Swipe to browse health articles"
        )
        
        Log.d("DISCOVER_ACCESSIBILITY", "Article accessibility features configured")
    }
    
    private fun navigateToArticleViewer(article: HealthContent.Article) {
        Log.d("DISCOVER_UI", "Navigating to article viewer: ${article.title}")
        
        // Navigate to specialized article viewer
        val intent = com.vibehealth.android.ui.discover.ContentViewerActivity.createIntent(
            requireContext(), 
            article.id,
            "article"
        )
        startActivity(intent)
        
        // Track article engagement
        discoverViewModel.trackContentEngagement(article, "VIEW")
    }
    
    private fun shareArticle(article: HealthContent.Article) {
        Log.d("DISCOVER_UI", "Sharing article: ${article.title}")
        
        val shareText = "📚 Check out this wellness article: ${article.title}\n\n" +
                "${article.description}\n\n" +
                "Reading time: ${article.readingTimeMinutes} minutes\n" +
                "By ${article.author}\n\n" +
                "Shared from Vibe Health 🌿"
        
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Health Article: ${article.title}")
        }
        
        startActivity(android.content.Intent.createChooser(shareIntent, "Share article"))
        discoverViewModel.trackContentEngagement(article, "SHARE")
    }
    
    private fun bookmarkArticle(article: HealthContent.Article) {
        Log.d("DISCOVER_UI", "Bookmarking article: ${article.title}")
        discoverViewModel.bookmarkContent(article)
    }
    

    
    private fun showArticleLoadingState(message: String) {
        binding.articlesSwipeRefresh.isRefreshing = false
        binding.articlesLoadingState.root.visibility = View.VISIBLE
        binding.articlesRecyclerView.visibility = View.GONE
        binding.articlesErrorState.root.visibility = View.GONE
        binding.articlesEmptyState.visibility = View.GONE
        
        binding.articlesLoadingState.loadingMessage.text = "Loading inspiring articles for you... 📚"
    }
    
    private fun showArticleContent(articles: List<HealthContent.Article>) {
        binding.articlesSwipeRefresh.isRefreshing = false
        binding.articlesLoadingState.root.visibility = View.GONE
        binding.articlesRecyclerView.visibility = View.VISIBLE
        binding.articlesErrorState.root.visibility = View.GONE
        binding.articlesEmptyState.visibility = View.GONE
        
        articleAdapter.submitList(articles)
        
        if (articles.isEmpty()) {
            showArticleEmptyState("No articles available right now. Check back soon for new wellness insights! 📚")
        }
    }
    
    private fun showArticleErrorState(message: String, retryAction: () -> Unit) {
        binding.articlesSwipeRefresh.isRefreshing = false
        binding.articlesLoadingState.root.visibility = View.GONE
        binding.articlesRecyclerView.visibility = View.GONE
        binding.articlesErrorState.root.visibility = View.VISIBLE
        binding.articlesEmptyState.visibility = View.GONE
        
        binding.articlesErrorState.offlineMessage.text = "We're having trouble loading articles right now. Your wellness journey continues - let's try again! 📚"
        binding.articlesErrorState.retryButton.setOnClickListener { retryAction() }
    }
    
    private fun showArticleEmptyState(message: String) {
        binding.articlesSwipeRefresh.isRefreshing = false
        binding.articlesLoadingState.root.visibility = View.GONE
        binding.articlesRecyclerView.visibility = View.GONE
        binding.articlesErrorState.root.visibility = View.GONE
        binding.articlesEmptyState.visibility = View.VISIBLE
        
        binding.articlesEmptyMessage.text = message
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "ArticlesFragment view destroyed - memory cleanup completed")
    }
}