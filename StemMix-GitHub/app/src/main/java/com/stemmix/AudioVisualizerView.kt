package com.stemmix

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.random.Random

/**
 * Custom view for audio visualization.
 * Tap to cycle through different visualization styles.
 */
class AudioVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    enum class VisualizerStyle {
        BARS,
        WAVEFORM,
        CIRCULAR,
        SPECTRUM
    }
    
    private var currentStyle = VisualizerStyle.BARS
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var audioData = FloatArray(128) { 0f }
    
    private val colors = arrayOf(
        Color.rgb(255, 0, 127),    // Pink
        Color.rgb(0, 255, 255),    // Cyan
        Color.rgb(255, 255, 0),    // Yellow
        Color.rgb(127, 0, 255),    // Purple
        Color.rgb(0, 255, 127)     // Green
    )
    
    init {
        paint.strokeWidth = 4f
        paint.style = Paint.Style.FILL
        
        setOnClickListener {
            cycleStyle()
            invalidate()
        }
    }
    
    fun updateAudioData(data: FloatArray) {
        // Normalize and smooth data
        for (i in audioData.indices) {
            val index = (i * data.size / audioData.size).coerceIn(0, data.size - 1)
            val value = abs(data[index]) / Short.MAX_VALUE
            audioData[i] = audioData[i] * 0.7f + value * 0.3f  // Smoothing
        }
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        when (currentStyle) {
            VisualizerStyle.BARS -> drawBars(canvas)
            VisualizerStyle.WAVEFORM -> drawWaveform(canvas)
            VisualizerStyle.CIRCULAR -> drawCircular(canvas)
            VisualizerStyle.SPECTRUM -> drawSpectrum(canvas)
        }
    }
    
    private fun drawBars(canvas: Canvas) {
        val barWidth = width.toFloat() / audioData.size
        
        for (i in audioData.indices) {
            val barHeight = audioData[i] * height
            val x = i * barWidth
            val y = height - barHeight
            
            paint.color = colors[i % colors.size]
            canvas.drawRect(x, y, x + barWidth - 2, height.toFloat(), paint)
        }
    }
    
    private fun drawWaveform(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        
        val stepX = width.toFloat() / audioData.size
        val midY = height / 2f
        
        for (i in 0 until audioData.size - 1) {
            val x1 = i * stepX
            val y1 = midY + (audioData[i] - 0.5f) * height
            val x2 = (i + 1) * stepX
            val y2 = midY + (audioData[i + 1] - 0.5f) * height
            
            paint.color = colors[i % colors.size]
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
        
        paint.style = Paint.Style.FILL
    }
    
    private fun drawCircular(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = height / 3f
        
        val angleStep = 360f / audioData.size
        
        for (i in audioData.indices) {
            val angle = i * angleStep
            val length = audioData[i] * radius
            
            val startX = centerX + Math.cos(Math.toRadians(angle.toDouble())).toFloat() * radius
            val startY = centerY + Math.sin(Math.toRadians(angle.toDouble())).toFloat() * radius
            val endX = centerX + Math.cos(Math.toRadians(angle.toDouble())).toFloat() * (radius + length)
            val endY = centerY + Math.sin(Math.toRadians(angle.toDouble())).toFloat() * (radius + length)
            
            paint.color = colors[i % colors.size]
            paint.strokeWidth = 3f
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
    }
    
    private fun drawSpectrum(canvas: Canvas) {
        // Frequency spectrum style
        val barWidth = width.toFloat() / audioData.size
        
        for (i in audioData.indices) {
            val barHeight = audioData[i] * height
            val x = i * barWidth
            
            // Gradient from bottom to top
            val colorIndex = (barHeight / height * colors.size).toInt().coerceIn(0, colors.size - 1)
            paint.color = colors[colorIndex]
            
            // Draw from center outward
            val centerY = height / 2f
            canvas.drawRect(x, centerY - barHeight / 2, x + barWidth - 2, centerY + barHeight / 2, paint)
        }
    }
    
    private fun cycleStyle() {
        currentStyle = when (currentStyle) {
            VisualizerStyle.BARS -> VisualizerStyle.WAVEFORM
            VisualizerStyle.WAVEFORM -> VisualizerStyle.CIRCULAR
            VisualizerStyle.CIRCULAR -> VisualizerStyle.SPECTRUM
            VisualizerStyle.SPECTRUM -> VisualizerStyle.BARS
        }
    }
}
