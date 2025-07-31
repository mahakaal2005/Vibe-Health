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
        private const val RING_WIDTH_DP = 24f
        private const val RING_SPACING_DP = 16f // 8-point grid system
        private const val CENTER_RADIUS_DP = 120f
        private const val START_ANGLE = -90f // Start from top
        private const val MAX_SWEEP_ANGLE = 360f
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
    }
    
    private val backgroundRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ringWidth
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.outline)
        alpha = 50 // Light background rings
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = dpToPx(16f)
        color = ContextCompat.getColor(context, R.color.text_primary)
    }
    
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
    
    /**
     * Draws background rings to show the full circle outline.
     * Uses different stroke patterns for accessibility and visual differentiation.
     */
    private fun drawBackgroundRings(canvas: Canvas, centerX: Float, centerY: Float) {
        for (i in 0..2) {
            val radius = centerRadius - (i * (ringWidth + ringSpacing))
            
            // Create different stroke patterns for accessibility
            val backgroundPaint = Paint(backgroundRingPaint).apply {
                when (i) {
                    0 -> {
                        // Outermost ring (Steps) - solid line
                        pathEffect = null
                    }
                    1 -> {
                        // Middle ring (Calories) - dashed line
                        pathEffect = DashPathEffect(floatArrayOf(dpToPx(8f), dpToPx(4f)), 0f)
                    }
                    2 -> {
                        // Innermost ring (Heart Points) - dotted line
                        pathEffect = DashPathEffect(floatArrayOf(dpToPx(2f), dpToPx(6f)), 0f)
                    }
                }
            }
            
            canvas.drawCircle(centerX, centerY, radius, backgroundPaint)
        }
    }
    
    /**
     * Draws the progress rings with current progress values.
     * Implements smooth curves and proper stroke width with visual indicators.
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
            
            // Set ring color with proper alpha for visual hierarchy
            ringPaint.color = ringData.color
            ringPaint.alpha = if (ringData.progress > 0) 255 else 100
            
            // Calculate sweep angle based on progress
            val sweepAngle = (ringData.progress * MAX_SWEEP_ANGLE).coerceIn(0f, MAX_SWEEP_ANGLE)
            
            // Draw progress arc with smooth curves
            if (sweepAngle > 0) {
                canvas.drawArc(rect, START_ANGLE, sweepAngle, false, ringPaint)
                
                // Draw progress indicator dot at the end of the arc
                if (ringData.progress < 1f && ringData.progress > 0) {
                    drawProgressIndicator(canvas, centerX, centerY, radius, START_ANGLE + sweepAngle, ringData.color)
                }
            }
            
            // Draw percentage text for each ring
            drawRingPercentageText(canvas, centerX, centerY, radius, ringData)
        }
    }
    
    /**
     * Draws a small indicator dot at the end of the progress arc.
     */
    private fun drawProgressIndicator(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, angle: Float, color: Int) {
        val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        
        val angleRad = Math.toRadians(angle.toDouble())
        val indicatorX = centerX + radius * kotlin.math.cos(angleRad).toFloat()
        val indicatorY = centerY + radius * kotlin.math.sin(angleRad).toFloat()
        
        canvas.drawCircle(indicatorX, indicatorY, dpToPx(4f), indicatorPaint)
    }
    
    /**
     * Draws percentage text and current/target values for accessibility.
     */
    private fun drawRingPercentageText(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, ringData: RingDisplayData) {
        // Position text outside the ring
        val textRadius = radius + ringWidth / 2 + dpToPx(8f)
        val textAngle = START_ANGLE + 45f // Position at 45 degrees
        val angleRad = Math.toRadians(textAngle.toDouble())
        
        val textX = centerX + textRadius * kotlin.math.cos(angleRad).toFloat()
        val textY = centerY + textRadius * kotlin.math.sin(angleRad).toFloat()
        
        // Configure text paint for ring labels
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = dpToPx(12f)
            color = ringData.color
            isFakeBoldText = true
        }
        
        // Draw percentage
        val percentageText = "${ringData.getProgressPercentage()}%"
        canvas.drawText(percentageText, textX, textY, labelPaint)
        
        // Draw ring type label below percentage
        labelPaint.textSize = dpToPx(10f)
        labelPaint.isFakeBoldText = false
        labelPaint.color = ContextCompat.getColor(context, R.color.text_secondary)
        
        val labelText = when (ringData.ringType) {
            RingType.STEPS -> "STEPS"
            RingType.CALORIES -> "CAL"
            RingType.HEART_POINTS -> "HEART"
        }
        
        canvas.drawText(labelText, textX, textY + dpToPx(16f), labelPaint)
    }
    
    /**
     * Draws center content with summary information.
     */
    private fun drawCenterContent(canvas: Canvas, centerX: Float, centerY: Float) {
        if (ringsData.isEmpty()) return
        
        // Draw today's date
        val dateText = java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("MMM dd")
        )
        
        textPaint.textSize = dpToPx(14f)
        textPaint.color = ContextCompat.getColor(context, R.color.text_secondary)
        canvas.drawText(dateText, centerX, centerY - dpToPx(20f), textPaint)
        
        // Draw overall progress summary
        val completedGoals = ringsData.count { it.progress >= 1f }
        val summaryText = when (completedGoals) {
            3 -> "All Goals\nComplete!"
            2 -> "2 of 3\nGoals"
            1 -> "1 of 3\nGoals"
            else -> "Keep\nGoing!"
        }
        
        textPaint.textSize = dpToPx(16f)
        textPaint.color = ContextCompat.getColor(context, R.color.text_primary)
        
        val lines = summaryText.split("\n")
        lines.forEachIndexed { index, line ->
            val yOffset = (index - lines.size / 2f + 0.5f) * dpToPx(20f)
            canvas.drawText(line, centerX, centerY + yOffset, textPaint)
        }
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