package com.vibehealth.android.ui.discover

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.vibehealth.android.databinding.ActivityContentViewerBinding
import com.vibehealth.android.ui.discover.models.HealthContent
import com.vibehealth.android.core.accessibility.AccessibilityHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * ContentViewerActivity - Full content display for articles and videos
 * Implements secure WebView configuration and Material Design 3 integration
 */
@AndroidEntryPoint
class ContentViewerActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "DISCOVER_UI"
        private const val EXTRA_CONTENT_ID = "extra_content_id"
        private const val EXTRA_CONTENT_TYPE = "extra_content_type"
        
        fun createIntent(context: Context, contentId: String, contentType: String = "general"): Intent {
            return Intent(context, ContentViewerActivity::class.java).apply {
                putExtra(EXTRA_CONTENT_ID, contentId)
                putExtra(EXTRA_CONTENT_TYPE, contentType)
            }
        }
    }
    
    private lateinit var binding: ActivityContentViewerBinding
    private val contentViewerViewModel: ContentViewerViewModel by viewModels()
    
    @Inject
    lateinit var accessibilityHelper: AccessibilityHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "Creating ContentViewerActivity for full content display")
        Log.d(TAG, "Implementing navigation and transitions")
        
        binding = ActivityContentViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupWebView()
        setupObservers()
        setupAccessibilitySupport()
        
        val contentId = intent.getStringExtra(EXTRA_CONTENT_ID)
        if (contentId != null) {
            Log.d(TAG, "Loading content with ID: $contentId")
            contentViewerViewModel.loadContent(contentId)
        } else {
            Log.e("DISCOVER_ERRORS", "No content ID provided")
            finish()
        }
    }
    
    private fun setupToolbar() {
        Log.d(TAG, "Applying existing design system consistency")
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Wellness Content"
        }
    }
    
    private fun setupWebView() {
        Log.d("DISCOVER_SECURITY", "Implementing secure WebView configuration")
        
        binding.contentWebView.apply {
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    // Security: Only allow trusted domains
                    return if (url != null && isTrustedUrl(url)) {
                        false // Let WebView handle the URL
                    } else {
                        Log.w("DISCOVER_SECURITY", "Blocked untrusted URL: $url")
                        true // Block the URL
                    }
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "Content loaded successfully")
                    hideLoadingState()
                }
            }
            
            settings.apply {
                javaScriptEnabled = false // Disable JavaScript for security
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                domStorageEnabled = false
                databaseEnabled = false
                // setAppCacheEnabled is deprecated, cache is disabled by default
            }
        }
    }
    
    private fun setupObservers() {
        Log.d(TAG, "Setting up content observation")
        
        contentViewerViewModel.contentState.observe(this) { content ->
            when (content) {
                is HealthContent.Article -> {
                    Log.d(TAG, "Displaying article content: ${content.title}")
                    displayArticleContent(content)
                }
                is HealthContent.Video -> {
                    Log.d(TAG, "Integrating video player functionality")
                    displayVideoContent(content)
                }
                is HealthContent.News -> {
                    Log.d(TAG, "Displaying news content: ${content.title}")
                    displayNewsContent(content)
                }
                null -> {
                    Log.e("DISCOVER_ERRORS", "Content not found")
                    showErrorState()
                }
            }
        }
        
        contentViewerViewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                showLoadingState()
            } else {
                hideLoadingState()
            }
        }
    }
    
    private fun displayArticleContent(article: HealthContent.Article) {
        Log.d(TAG, "Rendering article with secure WebView")
        
        supportActionBar?.title = article.title
        
        // Create HTML content with supportive styling
        val htmlContent = createArticleHtml(article)
        
        binding.contentWebView.loadDataWithBaseURL(
            null,
            htmlContent,
            "text/html",
            "UTF-8",
            null
        )
        
        setupShareButton(article)
    }
    
    private fun displayVideoContent(video: HealthContent.Video) {
        Log.d(TAG, "Setting up video player for content")
        
        supportActionBar?.title = video.title
        
        // For now, display video information in WebView
        // In production, this would integrate with ExoPlayer or similar
        val htmlContent = createVideoHtml(video)
        
        binding.contentWebView.loadDataWithBaseURL(
            null,
            htmlContent,
            "text/html",
            "UTF-8",
            null
        )
        
        setupShareButton(video)
    }
    
    private fun displayNewsContent(news: HealthContent.News) {
        Log.d(TAG, "Setting up news display for content")
        
        supportActionBar?.title = news.title
        
        // Display news information in WebView
        val htmlContent = createNewsHtml(news)
        
        binding.contentWebView.loadDataWithBaseURL(
            null,
            htmlContent,
            "text/html",
            "UTF-8",
            null
        )
        
        setupShareButton(news)
    }
    
    private fun createArticleHtml(article: HealthContent.Article): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: 'Roboto', sans-serif;
                        line-height: 1.6;
                        color: #2C2C2C;
                        background-color: #FAFBFA;
                        padding: 16px;
                        margin: 0;
                    }
                    .header {
                        background-color: #E8F5E8;
                        padding: 16px;
                        border-radius: 12px;
                        border: 1px solid #6B8E6B;
                        margin-bottom: 24px;
                    }
                    .title {
                        color: #6B8E6B;
                        font-size: 24px;
                        font-weight: bold;
                        margin-bottom: 8px;
                    }
                    .supportive-context {
                        color: #B5846B;
                        font-style: italic;
                        font-size: 14px;
                        margin-bottom: 16px;
                    }
                    .meta {
                        color: #7A8471;
                        font-size: 12px;
                        margin-bottom: 16px;
                    }
                    .content {
                        font-size: 16px;
                        line-height: 1.8;
                    }
                    .category {
                        background-color: #6B8E6B;
                        color: white;
                        padding: 4px 12px;
                        border-radius: 16px;
                        font-size: 12px;
                        display: inline-block;
                        margin-bottom: 16px;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="category">${article.category.displayName}</div>
                    <h1 class="title">${article.title}</h1>
                    <p class="supportive-context">${article.supportiveContext}</p>
                    <div class="meta">
                        By ${article.author} • ${article.readingTimeMinutes} min read
                    </div>
                </div>
                <div class="content">
                    <p>${article.description}</p>
                    <p>${article.contentPreview}</p>
                    <p><em>This content is provided for educational purposes and supports your wellness journey. Always consult with healthcare professionals for personalized advice.</em></p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
    
    private fun createVideoHtml(video: HealthContent.Video): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: 'Roboto', sans-serif;
                        line-height: 1.6;
                        color: #2C2C2C;
                        background-color: #FAFBFA;
                        padding: 16px;
                        margin: 0;
                    }
                    .header {
                        background-color: #E8F5E8;
                        padding: 16px;
                        border-radius: 12px;
                        border: 1px solid #6B8E6B;
                        margin-bottom: 24px;
                    }
                    .title {
                        color: #6B8E6B;
                        font-size: 24px;
                        font-weight: bold;
                        margin-bottom: 8px;
                    }
                    .supportive-context {
                        color: #B5846B;
                        font-style: italic;
                        font-size: 14px;
                        margin-bottom: 16px;
                    }
                    .meta {
                        color: #7A8471;
                        font-size: 12px;
                        margin-bottom: 16px;
                    }
                    .content {
                        font-size: 16px;
                        line-height: 1.8;
                    }
                    .category {
                        background-color: #6B8E6B;
                        color: white;
                        padding: 4px 12px;
                        border-radius: 16px;
                        font-size: 12px;
                        display: inline-block;
                        margin-bottom: 16px;
                    }
                    .video-placeholder {
                        background-color: #F5F7F5;
                        border: 2px dashed #6B8E6B;
                        border-radius: 8px;
                        padding: 32px;
                        text-align: center;
                        margin: 24px 0;
                        color: #7A8471;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="category">${video.category.displayName}</div>
                    <h1 class="title">${video.title}</h1>
                    <p class="supportive-context">${video.supportiveContext}</p>
                    <div class="meta">
                        ${video.channelName} • ${video.durationMinutes} min watch
                    </div>
                </div>
                <div class="video-placeholder">
                    <p>🎥 Video Player</p>
                    <p>Video content would be displayed here with a secure video player</p>
                    <p><small>Source: ${video.channelName}</small></p>
                </div>
                <div class="content">
                    <p>${video.description}</p>
                    <p><em>This video content supports your wellness journey. Take what resonates with you and always listen to your body.</em></p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
    
    private fun createNewsHtml(news: HealthContent.News): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: 'Roboto', sans-serif;
                        line-height: 1.6;
                        color: #2C2C2C;
                        background-color: #FAFBFA;
                        padding: 16px;
                        margin: 0;
                    }
                    .news-header {
                        background: linear-gradient(135deg, #6B8E6B, #7A8471);
                        color: white;
                        padding: 20px;
                        border-radius: 12px;
                        margin-bottom: 20px;
                    }
                    .news-title {
                        font-size: 24px;
                        font-weight: bold;
                        margin-bottom: 10px;
                    }
                    .news-meta {
                        font-size: 14px;
                        opacity: 0.9;
                    }
                    .breaking-badge {
                        background: #B5846B;
                        color: white;
                        padding: 4px 8px;
                        border-radius: 4px;
                        font-size: 12px;
                        font-weight: bold;
                        margin-bottom: 10px;
                        display: inline-block;
                    }
                    .news-content {
                        background: white;
                        padding: 20px;
                        border-radius: 12px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                    }
                    .supportive-context {
                        background: #F0F4F0;
                        padding: 16px;
                        border-radius: 8px;
                        border-left: 4px solid #6B8E6B;
                        margin: 20px 0;
                        font-style: italic;
                    }
                </style>
            </head>
            <body>
                <div class="news-header">
                    ${if (news.isBreaking) "<div class=\"breaking-badge\">BREAKING NEWS</div>" else ""}
                    <div class="news-title">${news.title}</div>
                    <div class="news-meta">
                        Source: ${news.source} • 
                        Published: ${android.text.format.DateFormat.format("MMM dd, yyyy 'at' h:mm a", news.publishedDate)}
                    </div>
                </div>
                
                <div class="news-content">
                    <p><strong>Summary:</strong> ${news.summary}</p>
                    <p>${news.description}</p>
                </div>
                
                <div class="supportive-context">
                    ${news.supportiveContext}
                </div>
            </body>
            </html>
        """.trimIndent()
    }
    
    private fun setupShareButton(content: HealthContent) {
        Log.d(TAG, "Implementing content sharing functionality")
        
        binding.shareButton.setOnClickListener {
            shareContent(content)
        }
    }
    
    private fun shareContent(content: HealthContent) {
        Log.d(TAG, "Sharing content with proper attribution")
        
        val shareText = when (content) {
            is HealthContent.Article -> {
                "Check out this wellness article: ${content.title}\n\n${content.description}\n\nShared from Vibe Health 🌿"
            }
            is HealthContent.Video -> {
                "Watch this wellness video: ${content.title}\n\n${content.description}\n\nShared from Vibe Health 🌿"
            }
            is HealthContent.News -> {
                "Breaking health news: ${content.title}\n\n${content.summary}\n\nShared from Vibe Health 🌿"
            }
        }
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Wellness Content from Vibe Health")
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share wellness content"))
    }
    
    private fun setupAccessibilitySupport() {
        Log.d("DISCOVER_ACCESSIBILITY", "Setting up accessibility for content viewer")
        
        // Setup WebView accessibility
        binding.contentWebView.settings.apply {
            // Enable accessibility features for WebView
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }
        
        // Set content descriptions
        accessibilityHelper.setContentDescription(
            binding.contentWebView,
            "Wellness content viewer. Contains the full article or video content",
            "Swipe to scroll through content"
        )
        
        accessibilityHelper.setContentDescription(
            binding.shareButton,
            "Share this wellness content",
            "Double tap to share with others"
        )
        
        // Ensure minimum touch targets
        accessibilityHelper.ensureMinimumTouchTarget(binding.shareButton)
        
        // Apply dynamic font scaling to toolbar
        supportActionBar?.let { actionBar ->
            // Font scaling for toolbar title is handled by system
        }
        
        Log.d("DISCOVER_ACCESSIBILITY", "Content viewer accessibility configured")
    }
    
    private fun isTrustedUrl(url: String): Boolean {
        val trustedDomains = listOf(
            "cdc.gov", "nih.gov", "nutrition.gov", "healthline.com",
            "mayoclinic.org", "webmd.com", "youtube.com", "who.int"
        )
        
        return trustedDomains.any { domain ->
            url.contains(domain, ignoreCase = true)
        }
    }
    
    private fun showLoadingState() {
        binding.loadingProgressBar.visibility = android.view.View.VISIBLE
        binding.contentWebView.visibility = android.view.View.GONE
    }
    
    private fun hideLoadingState() {
        binding.loadingProgressBar.visibility = android.view.View.GONE
        binding.contentWebView.visibility = android.view.View.VISIBLE
    }
    
    private fun showErrorState() {
        // TODO: Implement proper error state UI
        Log.e("DISCOVER_ERRORS", "Content viewer error state")
        finish()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                Log.d(TAG, "Navigation back button pressed")
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onBackPressed() {
        Log.d(TAG, "Implementing proper navigation and back button handling")
        super.onBackPressed()
        // Add transition animation
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}