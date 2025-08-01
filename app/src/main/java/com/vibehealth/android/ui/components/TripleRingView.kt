package com.vibehealth.android.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat
import com.vibehealth.android.R
import com.vibehealth.android.ui.dashboard.models.RingDisplayData
import com.vibehealth.android.ui.dashboard.models.RingType
import kotlin.math.min

/**
 * Custom view component for displaying the triple-ring wellness progress.
 * Implements the TripleRingVisual component from UI/UX specification.
 * 
 * Features:
 * - Three concentric rings for Steps, Calories, Heart Points
 * - Material Design 3 specifications with 8-point grid system
 * - UI/UX color palette: Sage Green (#6B8E6B), Warm Gray-Green (#7A8471), Soft Coral (#B5846B)
 * - Portrait-locked Android phone optimization
 * - Accessibility support with content descriptions
 */
class TripleRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    companion object {
        private const val RING_WIDTH_DP = 10f // Slightly thicker for better visibility (6-10dp range)
        private const val RING_SPACING_DP = 12f // Reduced spacing for compact design
        private const val CENTER_RADIUS_DP = 80f // Smaller center radius for compact size
        private const val START_ANGLE = -90f // Start from top
        private const val MAX_SWEEP_ANGLE = 360f
        private const val SHADOW_RADIUS_DP = 8f // Soft shadow for depth
        private const val SHADOW_OFFSET_DP = 2f // Subtle shadow offset
    }
    
    // Ring configuration following UI/UX specifications
    private val ringWidth = dpToPx(RING_WIDTH_DP)
    private val ringSpacing = dpToPx(RING_SPACING_DP)
    private val centerRadius = dpToPx(CENTER_RADIUS_DP)
    
    // UI/UX color palette
    private val sageGreen = Color.parseColor("#6B8E6B")
    private val warmGrayGreen = Color.parseColor("#7A8471")
    private val softCoral = Color.parseColor("#B5846B")
    
    // Paint objects for drawing
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ringWidth
        strokeCap = Paint.Cap.ROUND
        // No shadow for clean flat design
    }
    
    private val backgroundRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ringWidth
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#E0E0E0") // Faint gray for better visibility on beige
        alpha = 120 // More visible background rings
    }
    
    // Removed shadow paint for clean flat design
    
    // Ring data
    private var ringsData: List<RingDisplayData> = emptyList()
    
    // Animation manager
    private val animationManager = RingAnimationManager()
    
    // Accessibility helper
    private val accessibilityHelper = RingAccessibilityHelper(this)
    
    init {
        setupAccessibility()
        setupAnimationManager()
        
        // Set default ring data for preview
        if (isInEditMode) {
            setPreviewData()
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Calculate required size based on rings
        val outerRingRadius = centerRadius + (2 * (ringWidth + ringSpacing))
        val requiredSize = (outerRingRadius * 2 + ringWidth).toInt()
        
        val size = min(
            resolveSize(requiredSize, widthMeasureSpec),
            resolveSize(requiredSize, heightMeasureSpec)
        )
        
        setMeasuredDimension(size, size)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        
        // No background shadow for clean flat design
        
        // Draw background rings first
        drawBackgroundRings(canvas, centerX, centerY)
        
        // Draw progress rings
        drawProgressRings(canvas, centerX, centerY)
        
        // Draw center content
        drawCenterContent(canvas, centerX, centerY)
    }
    
    /**
     * Updates the ring progress data with optional animation.
     * 
     * @param newData List of RingDisplayData for the three rings
     * @param animate Whether to animate the change
     */
    fun updateProgress(newData: List<RingDisplayData>, animate: Boolean = true) {
        val sortedNewData = newData.sortedBy { it.getRingPosition() }
        
        if (animate && ringsData.isNotEmpty()) {
            // Animate from current to new data
            animationManager.animateProgressUpdate(
                tripleRingView = this,
                fromData = ringsData,
                toData = sortedNewData
            ) {
                // Update accessibility after animation completes
                accessibilityHelper.updateAccessibilityDescription()
            }
        } else {
            // Update immediately without animation
            ringsData = sortedNewData
            invalidate()
            accessibilityHelper.updateAccessibilityDescription()
        }
    }
    
    /**
     * Celebrates goal achievement with animation.
     * 
     * @param achievedRings List of RingType that achieved their goals
     */
    fun celebrateGoalAchievement(achievedRings: List<RingType>) {
        animationManager.celebrateGoalAchievement(
            tripleRingView = this,
            achievedRings = achievedRings
        )
    }
    
    /**
     * Gets current rings data for accessibility and animation purposes.
     */
    fun getRingsData(): List<RingDisplayData> = ringsData
    
    // Removed background shadow function for clean flat design
    
    /**
     * Draws background rings to show the full circle outline.
     * Uses faint gray circular tracks for better visibility on beige background.
     */
    private fun drawBackgroundRings(canvas: Canvas, centerX: Float, centerY: Float) {
        for (i in 0..2) {
            val radius = centerRadius - (i * (ringWidth + ringSpacing))
            
            // Create consistent background rings with better visibility
            val backgroundPaint = Paint(backgroundRingPaint).apply {
                // All rings use solid lines for better visibility on beige background
                pathEffect = null
                // Slightly different opacity for each ring for subtle differentiation
                alpha = when (i) {
                    0 -> 140 // Outermost ring (Steps) - most visible
                    1 -> 120 // Middle ring (Calories) - medium visibility
                    2 -> 100 // Innermost ring (Heart Points) - subtle
                    else -> 120
                }
            }
            
            canvas.drawCircle(centerX, centerY, radius, backgroundPaint)
        }
    }
    
    /**
     * Draws the progress rings with enhanced visibility and depth.
     * Implements smooth curves with better contrast against beige background.
     */
    private fun drawProgressRings(canvas: Canvas, centerX: Float, centerY: Float) {
        ringsData.forEach { ringData ->
            val position = ringData.getRingPosition()
            val radius = centerRadius - (position * (ringWidth + ringSpacing))
            
            val rect = RectF(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
            )
            
            // Enhanced ring color with better visibility
            val enhancedColor = enhanceColorForVisibility(ringData.color)
            ringPaint.color = enhancedColor
            ringPaint.alpha = if (ringData.progress > 0) 255 else 120
            
            // Calculate sweep angle based on progress
            val sweepAngle = (ringData.progress * MAX_SWEEP_ANGLE).coerceIn(0f, MAX_SWEEP_ANGLE)
            
            // Draw progress arc with enhanced visibility
            if (sweepAngle > 0) {
                canvas.drawArc(rect, START_ANGLE, sweepAngle, false, ringPaint)
                
                // Draw enhanced progress indicator dot
                if (ringData.progress < 1f && ringData.progress > 0) {
                    drawProgressIndicator(canvas, centerX, centerY, radius, START_ANGLE + sweepAngle, enhancedColor)
                }
            }
        }
    }
    
    /**
     * Enhances ring colors for better visibility on beige background while maintaining palette.
     */
    private fun enhanceColorForVisibility(originalColor: Int): Int {
        return when (originalColor) {
            sageGreen -> Color.parseColor("#5A7A5A") // Slightly darker sage green
            warmGrayGreen -> Color.parseColor("#6B7562") // Slightly darker warm gray-green
            softCoral -> Color.parseColor("#A0735A") // Slightly darker soft coral
            else -> originalColor
        }
    }
    
    /**
     * Draws an enhanced indicator dot with shadow for better visibility.
     */
    private fun drawProgressIndicator(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, angle: Float, color: Int) {
        val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
            // Add subtle shadow to indicator dot
            setShadowLayer(dpToPx(2f), 0f, dpToPx(1f), Color.argb(60, 0, 0, 0))
        }
        
        val angleRad = Math.toRadians(angle.toDouble())
        val indicatorX = centerX + radius * kotlin.math.cos(angleRad).toFloat()
        val indicatorY = centerY + radius * kotlin.math.sin(angleRad).toFloat()
        
        // Draw slightly larger indicator for better visibility
        canvas.drawCircle(indicatorX, indicatorY, dpToPx(5f), indicatorPaint)
        
        // Add white center dot for better contrast
        val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            style = Paint.Style.FILL
            alpha = 200
        }
        canvas.drawCircle(indicatorX, indicatorY, dpToPx(2f), centerDotPaint)
    }
    

    
    /**
     * Draws center content - now empty for clean minimalist design.
     */
    private fun drawCenterContent(canvas: Canvas, centerX: Float, centerY: Float) {
        // No center text for clean minimalist ring design
    }
    
    /**
     * Sets up the animation manager with accessibility preferences.
     */
    private fun setupAnimationManager() {
        // Check for reduced motion preference
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? android.view.accessibility.AccessibilityManager
        val isReducedMotionEnabled = accessibilityManager?.isEnabled == true
        animationManager.setReducedMotionEnabled(isReducedMotionEnabled)
    }
    
    /**
     * Sets up accessibility support for screen readers.
     */
    private fun setupAccessibility() {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        
        accessibilityDelegate = object : AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfo
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                
                info.className = TripleRingView::class.java.name
                info.contentDescription = accessibilityHelper.generateAccessibilityDescription()
                
                // Add custom action for detailed progress
                info.addAction(
                    AccessibilityNodeInfo.AccessibilityAction(
                        AccessibilityNodeInfo.ACTION_CLICK,
                        "View detailed progress"
                    )
                )
            }
        }
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Cancel animations when view is detached
        animationManager.cancelAnimations()
    }
    
    /**
     * Sets preview data for Android Studio layout editor.
     */
    private fun setPreviewData() {
        ringsData = listOf(
            RingDisplayData(
                ringType = RingType.STEPS,
                progress = 0.75f,
                currentValue = "7,500",
                targetValue = "10,000",
                color = sageGreen,
                isAnimating = false,
                accessibilityLabel = "Steps: 7,500 of 10,000 steps, 75 percent complete"
            ),
            RingDisplayData(
                ringType = RingType.CALORIES,
                progress = 0.60f,
                currentValue = "1,200",
                targetValue = "2,000",
                color = warmGrayGreen,
                isAnimating = false,
                accessibilityLabel = "Calories: 1,200 of 2,000 cal, 60 percent complete"
            ),
            RingDisplayData(
                ringType = RingType.HEART_POINTS,
                progress = 0.40f,
                currentValue = "12",
                targetValue = "30",
                color = softCoral,
                isAnimating = false,
                accessibilityLabel = "Heart Points: 12 of 30 points, 40 percent complete"
            )
        )
    }
    
    /**
     * Converts dp to pixels.
     */
    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
    
    /**
     * Accessibility helper class for screen reader support.
     */
    private class RingAccessibilityHelper(private val ringView: TripleRingView) {
        
        fun generateAccessibilityDescription(): String {
            val ringsData = ringView.getRingsData()
            if (ringsData.isEmpty()) {
                return "Daily wellness progress rings. No data available."
            }
            
            return buildString {
                append("Daily wellness progress. ")
                ringsData.forEach { ring ->
                    append("${ring.accessibilityLabel}. ")
                }
            }
        }
        
        fun updateAccessibilityDescription() {
            ringView.contentDescription = generateAccessibilityDescription()
        }
        
        fun announceProgressUpdate(ringType: RingType, newProgress: Float) {
            val announcement = "${ringType.displayName} progress updated to ${(newProgress * 100).toInt()} percent"
            ringView.announceForAccessibility(announcement)
        }
    }
}