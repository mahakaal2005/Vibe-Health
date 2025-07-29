package com.vibehealth.android.core.responsive

import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper for responsive design across different Android screen sizes
 * Ensures consistent experience on 5" to 7" devices with portrait lock
 */
@Singleton
class ResponsiveLayoutHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    /**
     * Get screen size category
     */
    fun getScreenSizeCategory(): ScreenSize {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val screenHeightDp = displayMetrics.heightPixels / displayMetrics.density
        
        return when {
            screenWidthDp < 360 -> ScreenSize.SMALL
            screenWidthDp > 420 -> ScreenSize.LARGE
            else -> ScreenSize.MEDIUM
        }
    }
    
    /**
     * Check if device is in landscape mode (should be locked to portrait)
     */
    fun isLandscape(): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
    
    /**
     * Get safe area insets for edge-to-edge display
     */
    fun applySafeAreaInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }
    }
    
    /**
     * Handle keyboard visibility for input fields
     */
    fun handleKeyboardVisibility(rootView: View, onKeyboardToggle: (Boolean) -> Unit) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            onKeyboardToggle(imeVisible)
            insets
        }
    }
    
    /**
     * Adjust layout for different screen sizes
     */
    fun adjustLayoutForScreenSize(view: View) {
        val screenSize = getScreenSizeCategory()
        val layoutParams = view.layoutParams as? ViewGroup.MarginLayoutParams
        
        layoutParams?.let { params ->
            val horizontalMargin = when (screenSize) {
                ScreenSize.SMALL -> context.resources.getDimensionPixelSize(com.vibehealth.android.R.dimen.spacing_medium)
                ScreenSize.MEDIUM -> context.resources.getDimensionPixelSize(com.vibehealth.android.R.dimen.spacing_large)
                ScreenSize.LARGE -> context.resources.getDimensionPixelSize(com.vibehealth.android.R.dimen.spacing_xlarge)
            }
            
            params.marginStart = horizontalMargin
            params.marginEnd = horizontalMargin
            view.layoutParams = params
        }
    }
    
    /**
     * Get appropriate text size for screen size
     */
    fun getResponsiveTextSize(baseSize: Float): Float {
        val screenSize = getScreenSizeCategory()
        return when (screenSize) {
            ScreenSize.SMALL -> baseSize * 0.9f
            ScreenSize.MEDIUM -> baseSize
            ScreenSize.LARGE -> baseSize * 1.1f
        }
    }
    
    /**
     * Handle notch and cutout areas
     */
    fun handleDisplayCutout(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val cutout = insets.displayCutout
            cutout?.let {
                val cutoutPadding = maxOf(
                    it.safeInsetLeft,
                    it.safeInsetTop,
                    it.safeInsetRight,
                    it.safeInsetBottom
                )
                
                v.setPadding(
                    v.paddingLeft + cutoutPadding,
                    v.paddingTop + cutoutPadding,
                    v.paddingRight + cutoutPadding,
                    v.paddingBottom + cutoutPadding
                )
            }
            insets
        }
    }
    
    /**
     * Auto-scroll to focused input field when keyboard appears
     */
    fun setupAutoScrollForKeyboard(scrollView: androidx.core.widget.NestedScrollView, inputView: View) {
        inputView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollView.post {
                    scrollView.smoothScrollTo(0, inputView.bottom)
                }
            }
        }
    }
    
    /**
     * Check if device supports edge-to-edge display
     */
    fun supportsEdgeToEdge(): Boolean {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P
    }
    
    /**
     * Get density-independent pixel value
     */
    fun dpToPx(dp: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
    
    /**
     * Get pixel value in density-independent pixels
     */
    fun pxToDp(px: Int): Float {
        val density = context.resources.displayMetrics.density
        return px / density
    }
    
    enum class ScreenSize {
        SMALL,   // < 360dp width
        MEDIUM,  // 360-420dp width
        LARGE    // > 420dp width
    }
}