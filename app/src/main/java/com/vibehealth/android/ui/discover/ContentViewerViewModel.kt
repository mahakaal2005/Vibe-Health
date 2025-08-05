package com.vibehealth.android.ui.discover

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibehealth.android.ui.discover.models.HealthContent
import com.vibehealth.android.data.discover.HealthContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ContentViewerViewModel - Manages content viewing state and data
 * Follows existing MVVM patterns with reactive data management
 */
@HiltViewModel
class ContentViewerViewModel @Inject constructor(
    private val healthContentRepository: HealthContentRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "DISCOVER_UI"
    }
    
    private val _contentState = MutableLiveData<HealthContent?>()
    val contentState: LiveData<HealthContent?> = _contentState
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    fun loadContent(contentId: String) {
        Log.d(TAG, "Loading content for viewer: $contentId")
        
        _isLoading.value = true
        _errorMessage.value = null
        
        viewModelScope.launch {
            try {
                // First try to get from cached content
                val cachedContent = healthContentRepository.getCachedContent()
                val content = cachedContent.find { it.id == contentId }
                
                if (content != null) {
                    Log.d(TAG, "Content found in cache: ${content.title}")
                    _contentState.value = content
                    _isLoading.value = false
                } else {
                    Log.w(TAG, "Content not found in cache, trying fresh load")
                    // If not in cache, try to load fresh content
                    loadFreshContent(contentId)
                }
                
            } catch (e: Exception) {
                Log.e("DISCOVER_ERRORS", "Error loading content: $contentId", e)
                _errorMessage.value = "We're having trouble loading this content. Please try again 💚"
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun loadFreshContent(contentId: String) {
        try {
            val allContent = healthContentRepository.getPersonalizedContent(null)
            val content = allContent.find { it.id == contentId }
            
            if (content != null) {
                Log.d(TAG, "Content found in fresh load: ${content.title}")
                _contentState.value = content
            } else {
                Log.w(TAG, "Content not found: $contentId")
                _errorMessage.value = "This content is no longer available. Let's explore other wellness resources 🌿"
            }
            
        } catch (e: Exception) {
            Log.e("DISCOVER_ERRORS", "Error in fresh content load", e)
            _errorMessage.value = "We're having trouble loading this content. Your wellness journey continues 💚"
        } finally {
            _isLoading.value = false
        }
    }
    
    fun trackContentView(content: HealthContent) {
        Log.d("DISCOVER_INTEGRATION", "Tracking content view engagement")
        
        // TODO: Implement analytics tracking in Task 10
        Log.d("DISCOVER_INTEGRATION", "Content viewed: ${content.title}")
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ContentViewerViewModel cleared")
    }
}