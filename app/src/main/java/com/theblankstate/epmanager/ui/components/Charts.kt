package com.theblankstate.epmanager.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.theblankstate.epmanager.ui.analytics.CategorySpending
import com.theblankstate.epmanager.ui.theme.Spacing

/**
 * Donut/Pie chart for category spending
 */
@Composable
fun SpendingPieChart(
    categorySpending: List<CategorySpending>,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 32.dp
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "pie_animation"
    )
    
    LaunchedEffect(Unit) {
        animationPlayed = true
    }
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidthPx = strokeWidth.toPx()
            val radius = (size.toPx() - strokeWidthPx) / 2
            val center = Offset(size.toPx() / 2, size.toPx() / 2)
            
            var startAngle = -90f
            
            categorySpending.forEach { spending ->
                val sweepAngle = (spending.percentage / 100f) * 360f * animatedProgress
                
                drawArc(
                    color = Color(spending.category.color),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
                
                startAngle += sweepAngle
            }
        }
        
        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${categorySpending.size}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "categories",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Bar chart for monthly comparison
 */
@Composable
fun MonthlyBarChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 150.dp
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "bar_animation"
    )
    
    LaunchedEffect(Unit) {
        animationPlayed = true
    }
    
    val maxValue = data.maxOfOrNull { it.second } ?: 1.0
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (month, value) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Bar
                val barHeight = if (maxValue > 0) {
                    ((value / maxValue) * (height.value - 30) * animatedProgress).dp
                } else {
                    4.dp
                }
                
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(barHeight.coerceAtLeast(4.dp))
                        .clip(MaterialTheme.shapes.small)
                        .background(barColor.copy(alpha = 0.7f + (value / maxValue * 0.3f).toFloat()))
                )
                
                Spacer(modifier = Modifier.height(Spacing.xs))
                
                // Label
                Text(
                    text = month,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Progress bar for budget or category limit
 */
@Composable
fun SpendingProgressBar(
    spent: Double,
    budget: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (budget > 0) (spent / budget).coerceIn(0.0, 1.0) else 0.0
    val isOverBudget = spent > budget
    
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) progress.toFloat() else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "progress_animation"
    )
    
    LaunchedEffect(Unit) {
        animationPlayed = true
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(CircleShape)
                .background(if (isOverBudget) MaterialTheme.colorScheme.error else color)
        )
    }
}

/**
 * Legend item for pie chart
 */
@Composable
fun ChartLegendItem(
    color: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
