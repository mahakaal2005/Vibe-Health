package com.vibehealth.android.core.accessibility

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.vibehealth.android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Accessibility helper for ensuring WCAG 2.1 Level AA compliance
 * Handles dynamic font scaling, high contrast, and screen reader support
 */
@Singleton
class AccessibilityHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    
    /**
     * Check if accessibility services are enabled
     */
    fun isAccessibilityEnabled(): Boolean {
        return accessibilityManager.isEnabled
    }
    
    /**
     * Check if TalkBack or other screen readers are active
     */
    fun isScreenReaderActive(): Boolean {
        return accessibilityManager.isTouchExplorationEnabled
    }
    
    /**
     * Get system font scale factor
     */
    fun getFontScale(): Float {
        return context.resources.configuration.fontScale
    }
    
    /**
     * Check if large text is enabled (font scale > 1.3)
     */
    fun isLargeTextEnabled(): Boolean {
        return getFontScale() > 1.3f
    }
    
    /**
     * Apply dynamic font scaling up to 200% as per requirements
     */
    fun applyDynamicFontScaling(textView: TextView, baseTextSize: Float) {
        val fontScale = getFontScale().coerceAtMost(2.0f) // Max 200% scaling
        textView.textSize = baseTextSize * fontScale
    }
    
    /**
     * Check if high contrast mode is enabled
     */
    fun isHighContrastEnabled(): Boolean {
        return (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
    
    /**
     * Ensure minimum touch target size (48dp × 48dp)
     */
    fun ensureMinimumTouchTarget(view: View) {
        val minSize = context.resources.getDimensionPixelSize(R.dimen.touch_target_min)
        
        view.post {
            val layoutParams = view.layoutParams
            if (view.width < minSize || view.height < minSize) {
                layoutParams.width = layoutParams.width.coerceAtLeast(minSize)
                layoutParams.height = layoutParams.height.coerceAtLeast(minSize)
                view.layoutParams = layoutParams
            }
        }
    }
    
    /**
     * Set proper content description for screen readers
     */
    fun setContentDescription(view: View, description: String, hint: String? = null) {
        val fullDescription = if (hint != null) {
            "$description. $hint"
        } else {
            description
        }
        
        view.contentDescription = fullDescription
    }
    
    /**
     * Set accessibility role for better screen reader support
     */
    fun setAccessibilityRole(view: View, role: String) {
        ViewCompat.setAccessibilityDelegate(view, object : androidx.core.view.AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = role
            }
        })
    }
    
    /**
     * Check color contrast ratio for WCAG compliance (4.5:1 minimum)
     */
    fun checkColorContrast(foregroundColor: Int, backgroundColor: Int): Boolean {
        val contrastRatio = calculateContrastRatio(foregroundColor, backgroundColor)
        return contrastRatio >= 4.5 // WCAG AA standard
    }
    
    /**
     * Calculate color contrast ratio
     */
    private fun calculateContrastRatio(color1: Int, color2: Int): Double {
        val luminance1 = calculateLuminance(color1)
        val luminance2 = calculateLuminance(color2)
        
        val lighter = maxOf(luminance1, luminance2)
        val darker = minOf(luminance1, luminance2)
        
        return (lighter + 0.05) / (darker + 0.05)
    }
    
    /**
     * Calculate relative luminance of a color
     */
    private fun calculateLuminance(color: Int): Double {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        
        val rLinear = if (r <= 0.03928) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)
        val gLinear = if (g <= 0.03928) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)
        val bLinear = if (b <= 0.03928) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)
        
        return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear
    }
    
    /**
     * Apply high contrast colors if needed
     */
    fun applyHighContrastIfNeeded(view: View) {
        if (isHighContrastEnabled()) {
            when (view) {
                is TextView -> {
                    view.setTextColor(ContextCompat.getColor(context, R.color.text_primary_dark))
                }
            }
        }
    }
    
    /**
     * Set up keyboard navigation for view group
     */
    fun setupKeyboardNavigation(viewGroup: ViewGroup) {
        val focusableViews = mutableListOf<View>()
        
        // Find all focusable views
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child.isFocusable) {
                focusableViews.add(child)
            }
        }
        
        // Set up next focus IDs for keyboard navigation
        for (i in focusableViews.indices) {
            val currentView = focusableViews[i]
            val nextView = if (i < focusableViews.size - 1) focusableViews[i + 1] else focusableViews[0]
            val prevView = if (i > 0) focusableViews[i - 1] else focusableViews.last()
            
            currentView.nextFocusDownId = nextView.id
            currentView.nextFocusUpId = prevView.id
        }
    }
    
    /**
     * Announce message to screen reader
     */
    fun announceForAccessibility(view: View, message: String) {
        if (isScreenReaderActive()) {
            view.announceForAccessibility(message)
        }
    }
    
    /**
     * Set up focus indicators for keyboard navigation
     */
    fun setupFocusIndicators(view: View) {
        view.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.background = ContextCompat.getDrawable(context, R.drawable.focus_indicator)
            } else {
                v.background = null
            }
        }
    }
    
    /**
     * Check if animations should be reduced
     */
    fun shouldReduceAnimations(): Boolean {
        return accessibilityManager.isEnabled && 
               android.provider.Settings.Global.getFloat(
                   context.contentResolver,
                   android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                   1.0f
               ) == 0.0f
    }
    
    /**
     * Apply accessibility-friendly animation duration
     */
    fun getAccessibleAnimationDuration(defaultDuration: Long): Long {
        return if (shouldReduceAnimations()) {
            0L // No animations
        } else {
            defaultDuration
        }
    }
}