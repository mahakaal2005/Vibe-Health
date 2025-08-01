package com.vibehealth.android.ui.components

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.vibehealth.android.ui.progress.models.DailyMetricData
import com.vibehealth.android.ui.progress.models.MetricType

/**
 * BasicProgressGraph - Custom view for displaying weekly progress data
 * 
 * This custom view displays weekly progress data with supportive visualization
 * following the Companion Principle. Uses the Sage Green color palette and
 * 8-point grid system for consistent, encouraging data presentation.
 * 
 * Features:
 * - Sage Green & Warm Neutrals color palette
 * - Supportive data visualization with encouraging context
 * - WCAG 2.1 Level AA accessibility compliance
 * - Hardware-accelerated rendering for 60fps performance
 * - Companion Principle messaging throughout
 */
class BasicProgressGraph @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    companion object {
        private const val GRAPH_PADDING = 32f
        private const val BAR_WIDTH_RATIO = 0.6f
        private const val CORNER_RADIUS = 8f
    }
    
    // Vibe Health Design System colors
    private val sageGreenPrimary = Color.parseColor("#6B8E6B")
    private val warmGrayGreen = Color.parseColor("#7A8471")
    private val softCoral = Color.parseColor("#B5846B")
    private val surfaceVariant = Color.parseColor("#F5F7F5")
    private val supportiveText = Color.parseColor("#6B7B6B")
    
    // Paint objects for drawing
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        color = supportiveText
        textAlign = Paint.Align.CENTER
    }
    
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#E5E7E5")
    }
    
    // Data properties
    private var weeklyData: List<DailyMetricData> = emptyList()
    private var metricType: MetricType = MetricType.STEPS
    private var supportiveMessage: String = ""
    
    /**
     * Updates the graph with supportive data and encouraging messaging
     */
    fun updateWithSupportiveData(data: List<DailyMetricData>, message: String) {
        weeklyData = data
        supportiveMessage = message
        invalidate()
        
        // Update accessibility description
        contentDescription = generateAccessibilityDescription()
    }
    
    /**
     * Sets the metric type for appropriate color theming
     */
    fun setMetricType(type: MetricType) {
        metricType = type
        updateBarColor()
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (weeklyData.isEmpty()) {
            drawEmptyState(canvas)
            return
        }
        
        drawSupportiveGraph(canvas)
    }
    
    /**
     * Draws the supportive progress graph with encouraging visualization
     */
    private fun drawSupportiveGraph(canvas: Canvas) {
        val graphWidth = width - (GRAPH_PADDING * 2)
        val graphHeight = height - (GRAPH_PADDING * 2)
        val barWidth = (graphWidth / weeklyData.size) * BAR_WIDTH_RATIO
        val barSpacing = graphWidth / weeklyData.size
        
        // Find max value for scaling
        val maxValue = weeklyData.maxOfOrNull { it.value } ?: 1f
        
        // Draw gentle grid lines
        drawSupportiveGridLines(canvas, graphHeight)
        
        // Draw encouraging progress bars
        weeklyData.forEachIndexed { index, data ->
            val barHeight = (data.value / maxValue) * graphHeight * 0.8f // Leave space for labels
            val barLeft = GRAPH_PADDING + (index * barSpacing) + ((barSpacing - barWidth) / 2)
            val barTop = GRAPH_PADDING + graphHeight - barHeight
            val barRight = barLeft + barWidth
            val barBottom = GRAPH_PADDING + graphHeight
            
            // Draw supportive progress bar
            val barRect = RectF(barLeft, barTop, barRight, barBottom)
            canvas.drawRoundRect(barRect, CORNER_RADIUS, CORNER_RADIUS, barPaint)
            
            // Draw encouraging day label
            val dayLabel = data.date.dayOfWeek.name.take(3)
            canvas.drawText(
                dayLabel,
                barLeft + (barWidth / 2),
                barBottom + 30f,
                textPaint
            )
            
            // Draw supportive value label if goal achieved
            if (data.isGoalAchieved) {
                val achievementPaint = Paint(textPaint).apply {
                    color = barPaint.color
                    textSize = 20f
                }
                canvas.drawText(
                    "✓",
                    barLeft + (barWidth / 2),
                    barTop - 10f,
                    achievementPaint
                )
            }
        }
    }
    
    /**
     * Draws supportive grid lines for better data readability
     */
    private fun drawSupportiveGridLines(canvas: Canvas, graphHeight: Float) {
        val gridLines = 4
        val lineSpacing = graphHeight / gridLines
        
        for (i in 1 until gridLines) {
            val y = GRAPH_PADDING + (i * lineSpacing)
            canvas.drawLine(
                GRAPH_PADDING,
                y,
                width - GRAPH_PADDING,
                y,
                gridPaint
            )
        }
    }
    
    /**
     * Draws encouraging empty state when no data is available
     */
    private fun drawEmptyState(canvas: Canvas) {
        val emptyMessage = "Your wellness journey starts here!"
        val emptyPaint = Paint(textPaint).apply {
            textSize = 32f
            color = supportiveText
        }
        
        canvas.drawText(
            emptyMessage,
            width / 2f,
            height / 2f,
            emptyPaint
        )
    }
    
    /**
     * Updates bar color based on metric type
     */
    private fun updateBarColor() {
        barPaint.color = when (metricType) {
            MetricType.STEPS -> sageGreenPrimary
            MetricType.CALORIES -> warmGrayGreen
            MetricType.HEART_POINTS -> softCoral
        }
    }
    
    /**
     * Generates accessibility description for the graph
     */
    private fun generateAccessibilityDescription(): String {
        if (weeklyData.isEmpty()) {
            return "Empty progress graph for ${metricType.displayName}. Ready to start tracking your wellness journey!"
        }
        
        val activeDays = weeklyData.count { it.value > 0 }
        val achievedDays = weeklyData.count { it.isGoalAchieved }
        
        return buildString {
            append("${metricType.displayName} progress graph for this week. ")
            append("You were active on $activeDays days")
            if (achievedDays > 0) {
                append(" and achieved your goal on $achievedDays days")
            }
            append(". $supportiveMessage")
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 400
        val desiredHeight = 200
        
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        
        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(desiredWidth, widthSize)
            else -> desiredWidth
        }
        
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
            else -> desiredHeight
        }
        
        setMeasuredDimension(width, height)
    }
    
    init {
        // Set up initial accessibility
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        contentDescription = "Progress graph ready for your wellness data"
        
        // Initialize with default metric type
        updateBarColor()
    }
}