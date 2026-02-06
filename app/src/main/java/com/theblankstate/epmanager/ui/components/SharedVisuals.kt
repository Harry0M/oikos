package com.theblankstate.epmanager.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WaveBackground(
    modifier: Modifier = Modifier,
    color: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "wavePhase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val midHeight = height * 0.6f
        val amplitude = height * 0.15f
        
        val path = Path()
        path.moveTo(0f, midHeight)
        
        for (x in 0..width.toInt() step 10) {
            val xPos = x.toFloat()
            val yPos = midHeight + amplitude * sin((xPos / width) * 2 * Math.PI + phase).toFloat()
            path.lineTo(xPos, yPos)
        }
        
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()
        
        drawPath(
            path = path,
            color = color
        )
    }
}

@Composable
fun DoodlePattern(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        drawCircle(
            color = color,
            radius = 40.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(width * 0.85f, height * 0.15f)
        )
        drawCircle(
            color = color,
            radius = 25.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(width * 0.1f, height * 0.5f)
        )
        drawCircle(
            color = color,
            radius = 15.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(width * 0.6f, height * 0.8f)
        )
    }
}
