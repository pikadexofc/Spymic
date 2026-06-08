package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

@Composable
fun VisualizerCanvas(
    amplitudes: List<Int>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    maxDb: Int = 32767 // Max 16-bit PCM amplitude
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val barWidth = 8f
        val gap = 4f
        val numBars = (width / (barWidth + gap)).toInt()
        
        val recentAmplitudes = amplitudes.takeLast(numBars)
        
        val startX = width - (recentAmplitudes.size * (barWidth + gap))
        
        recentAmplitudes.forEachIndexed { index, amp ->
            val normalized = (amp.toFloat() / maxDb.toFloat()).coerceIn(0.01f, 1f)
            val barHeight = normalized * height
            
            val x = startX + index * (barWidth + gap)
            val yStart = (height - barHeight) / 2
            val yEnd = yStart + barHeight
            
            drawLine(
                color = color,
                start = Offset(x, yStart),
                end = Offset(x, yEnd),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
