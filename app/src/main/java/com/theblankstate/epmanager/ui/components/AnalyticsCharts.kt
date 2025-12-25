package com.theblankstate.epmanager.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.theblankstate.epmanager.ui.analytics.AccountBalance
import com.theblankstate.epmanager.ui.analytics.DailySpending
import com.theblankstate.epmanager.ui.theme.Spacing

/**
 * Circular gauge showing financial health score (0-100)
 */
@Composable
fun FinancialHealthGauge(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    strokeWidth: Dp = 16.dp
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) score / 100f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "health_gauge"
    )
    
    LaunchedEffect(Unit) {
        animationPlayed = true
    }
    
    val color = when {
        score >= 70 -> Color(0xFF22C55E) // Green
        score >= 40 -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFEF4444) // Red
    }
    
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val sweepAngle = 240f
            val startAngle = 150f
            
            // Background arc
            drawArc(
                color = backgroundColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth.toPx() / 2, strokeWidth.toPx() / 2),
                size = Size(
                    size.toPx() - strokeWidth.toPx(),
                    size.toPx() - strokeWidth.toPx()
                )
            )
            
            // Progress arc
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(strokeWidth.toPx() / 2, strokeWidth.toPx() / 2),
                size = Size(
                    size.toPx() - strokeWidth.toPx(),
                    size.toPx() - strokeWidth.toPx()
                )
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = "Health Score",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Horizontal progress bar showing savings rate
 */
@Composable
fun SavingsRateBar(
    rate: Double,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp
) {
    val displayRate = rate.coerceIn(-50.0, 100.0)
    val progress = ((displayRate + 50) / 150).toFloat().coerceIn(0f, 1f)
    
    val color = when {
        rate >= 20 -> Color(0xFF22C55E) // Green
        rate >= 0 -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFEF4444) // Red
    }
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Savings Rate",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${rate.toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(height / 2))
                    .background(color)
            )
        }
    }
}

/**
 * Line chart showing daily spending for last 7 days
 */
@Composable
fun DailySpendingLineChart(
    data: List<DailySpending>,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) return
    
    val maxSpending = data.maxOfOrNull { it.amount } ?: 1.0
    val pointColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.surfaceVariant
    
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val width = size.width
            val chartHeight = size.height - 20.dp.toPx()
            val stepX = width / (data.size - 1).coerceAtLeast(1)
            
            // Draw grid lines
            for (i in 0..3) {
                val y = chartHeight * i / 3
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            
            // Build path
            val path = Path()
            data.forEachIndexed { index, spending ->
                val x = stepX * index
                val y = chartHeight - (spending.amount / maxSpending * chartHeight).toFloat()
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            
            // Draw line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Draw points
            data.forEachIndexed { index, spending ->
                val x = stepX * index
                val y = chartHeight - (spending.amount / maxSpending * chartHeight).toFloat()
                
                drawCircle(
                    color = pointColor,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(Spacing.xs))
        
        // Day labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { spending ->
                Text(
                    text = spending.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Simple horizontal bar chart for account balances
 */
@Composable
fun AccountBalanceChart(
    accounts: List<AccountBalance>,
    modifier: Modifier = Modifier
) {
    if (accounts.isEmpty()) return
    
    val maxBalance = accounts.maxOfOrNull { kotlin.math.abs(it.balance) } ?: 1.0
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        accounts.take(5).forEach { accountBalance ->
            val progress = (kotlin.math.abs(accountBalance.balance) / maxBalance).toFloat()
            val isNegative = accountBalance.balance < 0
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Color dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(accountBalance.account.color))
                )
                
                // Name
                Text(
                    text = accountBalance.account.name,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(80.dp)
                )
                
                // Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isNegative) Color(0xFFEF4444)
                                else Color(accountBalance.account.color)
                            )
                    )
                }
                
                // Percentage
                Text(
                    text = "${accountBalance.percentage.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/**
 * Goal progress mini bar
 */
@Composable
fun GoalProgressMiniBar(
    name: String,
    progress: Float,
    color: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(color))
            )
        }
        
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}
