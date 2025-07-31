package com.vibehealth.android.ui.accessibility

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.vibehealth.android.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Accessibility manager for WCAG 2.1 Level AA compliance.
 * Provides comprehensive accessibility features for the dashboard and other components.
 */
@Singleton
class AccessibilityManager @Inject constructor(
    private val context: Context
) {
    
    private val systemAccessibilityManager = 
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    
    /**
     * Checks if accessibility services are enabled.
     */
    fun isAccessibilityEnabled(): Boolean {
        return systemAccessibilityManager.isEnabled
    }
    
    /**
     * Checks if TalkBack or other screen readers are active.
     */
    fun isScreenReaderActive(): Boolean {
        return systemAccessibilityManager.isTouchExplorationEnabled
    }
    
    /**
     * Checks if reduced motion is preferred.
     */
    fun isReducedMotionPreferred(): Boolean {
        // Check system settings for reduced motion preference
        return try {
            val resolver = context.contentResolver
            val animationScale = android.provider.Settings.Global.getFloat(
                resolver, 
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 
                1.0f
            )
            animationScale == 0f
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Calculates color contrast ratio between two colors.
     * Returns ratio where 4.5:1 is minimum for WCAG AA compliance.
     */
    fun calculateContrastRatio(foreground: Int, background: Int): Double {
        val foregroundLuminance = calculateLuminance(foreground)
        val backgroundLuminance = calculateLuminance(background)
        
        val lighter = maxOf(foregroundLuminance, backgroundLuminance)
        val darker = minOf(foregroundLuminance, backgroundLuminance)
        
        return (lighter + 0.05) / (darker + 0.05)
    }
    
    /**
     * Checks if color combination meets WCAG AA standards.
     */
    fun meetsWCAGAAStandards(foreground: Int, background: Int): Boolean {
        return calculateContrastRatio(foreground, background) >= 4.5
    }
    
    /**
     * Gets accessible color for the given background.
     */
    fun getAccessibleTextColor(backgroundColor: Int): Int {
        val whiteContrast = calculateContrastRatio(Color.WHITE, backgroundColor)
        val blackContrast = calculateContrastRatio(Color.BLACK, backgroundColor)
        
        return if (whiteContrast >= blackContrast) Color.WHITE else Color.BLACK
    }
    
    /**
     * Sets up keyboard navigation for a view.
     */
    fun setupKeyboardNavigation(view: View) {
        view.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            
            // Add focus change listener for visual feedback
            setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.background = ContextCompat.getDrawable(context, R.drawable.focus_indicator)
                } else {
                    v.background = null
                }
            }
        }
    }
    
    /**
     * Announces text for screen readers.
     */
    fun announceForAccessibility(view: View, text: String) {
        if (isScreenReaderActive()) {
            view.announceForAccessibility(text)
        }
    }
    
    /**
     * Creates comprehensive content description for complex views.
     */
    fun createContentDescription(
        viewType: String,
        currentState: String,
        actionHint: String? = null
    ): String {
        return buildString {
            append(viewType)
            append(". ")
            append(currentState)
            actionHint?.let {
                append(". ")
                append(it)
            }
        }
    }
    
    /**
     * Validates color accessibility for the dashboard rings.
     */
    fun validateRingColors(): AccessibilityValidationResult {
        val sageGreen = ContextCompat.getColor(context, R.color.sage_green)
        val warmGrayGreen = ContextCompat.getColor(context, R.color.warm_gray_green)
        val softCoral = ContextCompat.getColor(context, R.color.soft_coral)
        val background = ContextCompat.getColor(context, R.color.background_light)
        
        val results = mutableListOf<ColorValidation>()
        
        results.add(ColorValidation(
            colorName = "Sage Green",
            color = sageGreen,
            background = background,
            contrastRatio = calculateContrastRatio(sageGreen, background),
            meetsStandards = meetsWCAGAAStandards(sageGreen, background)
        ))
        
        results.add(ColorValidation(
            colorName = "Warm Gray Green",
            color = warmGrayGreen,
            background = background,
            contrastRatio = calculateContrastRatio(warmGrayGreen, background),
            meetsStandards = meetsWCAGAAStandards(warmGrayGreen, background)
        ))
        
        results.add(ColorValidation(
            colorName = "Soft Coral",
            color = softCoral,
            background = background,
            contrastRatio = calculateContrastRatio(softCoral, background),
            meetsStandards = meetsWCAGAAStandards(softCoral, background)
        ))
        
        return AccessibilityValidationResult(
            validations = results,
            allColorsValid = results.all { it.meetsStandards }
        )
    }
    
    /**
     * Calculates relative luminance of a color.
     */
    private fun calculateLuminance(color: Int): Double {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        
        val rLinear = if (r <= 0.03928) r / 12.92 else kotlin.math.pow((r + 0.055) / 1.055, 2.4)
        val gLinear = if (g <= 0.03928) g / 12.92 else kotlin.math.pow((g + 0.055) / 1.055, 2.4)
        val bLinear = if (b <= 0.03928) b / 12.92 else kotlin.math.pow((b + 0.055) / 1.055, 2.4)
        
        return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear
    }
}

/**
 * Data class for color validation results.
 */
data class ColorValidation(
    val colorName: String,
    val color: Int,
    val background: Int,
    val contrastRatio: Double,
    val meetsStandards: Boolean
)

/**
 * Data class for accessibility validation results.
 */
data class AccessibilityValidationResult(
    val validations: List<ColorValidation>,
    val allColorsValid: Boolean
)