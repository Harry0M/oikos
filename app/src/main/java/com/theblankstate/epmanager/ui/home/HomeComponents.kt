package com.theblankstate.epmanager.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.theblankstate.epmanager.ui.components.formatAmount
import com.theblankstate.epmanager.ui.components.WaveBackground
import com.theblankstate.epmanager.ui.components.DoodlePattern
import com.theblankstate.epmanager.ui.theme.Spacing

@Composable
fun AccountCard(
    data: AccountCardData,
    currencySymbol: String,
    onClick: () -> Unit
) {
    // Dynamic Color Logic
    val todayIncome = data.todayIncome
    val todayExpense = data.todayExpense
    
    // Determine state
    val isWarningState = (todayIncome > 0 && todayExpense > (todayIncome * 0.5)) || (todayIncome == 0.0 && todayExpense > 0)
    val isHealthyState = (todayIncome > 0 && todayExpense <= (todayIncome * 0.5)) || (todayIncome > 0 && todayExpense == 0.0)
    
    // Theme-aware colors
    val isDark = isSystemInDarkTheme()
    
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer
    
    // Custom Success Colors (Green)
    val successContainer = if (isDark) Color(0xFF005324) else Color(0xFFC2E8CE)
    val onSuccessContainer = if (isDark) Color(0xFF86Dba3) else Color(0xFF00210B)
    
    // Neutral Colors
    val surfaceContainer = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val containerColor = when {
        isWarningState -> errorContainer
        isHealthyState -> successContainer
        else -> surfaceContainer
    }
    
    val contentColor = when {
        isWarningState -> onErrorContainer
        isHealthyState -> onSuccessContainer
        else -> onSurfaceVariant
    }
    
    val subLabelColor = contentColor.copy(alpha = 0.7f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Background Animations (Shared)
            WaveBackground(
                modifier = Modifier.fillMaxSize(),
                color = contentColor.copy(alpha = 0.05f)
            )
            
            DoodlePattern(
                modifier = Modifier.fillMaxSize(),
                color = contentColor.copy(alpha = 0.05f)
            )
            
            // 2. Content Layer
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Section: Header & Balance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = data.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = subLabelColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatAmount(data.balance, currencySymbol),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                    
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        contentColor = contentColor,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when(data.type) {
                                    "CASH" -> Icons.Filled.Money
                                    "BANK" -> Icons.Filled.AccountBalance
                                    "UPI" -> Icons.Filled.PhoneAndroid
                                    "CREDIT_CARD" -> Icons.Filled.CreditCard
                                    "TOTAL" -> Icons.Filled.AccountBalanceWallet
                                    else -> Icons.Filled.AccountBalanceWallet
                                },
                                contentDescription = null,
                                tint = contentColor
                            )
                        }
                    }
                }
                
                // Middle Section: Today's Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Income
                    Column {
                        Text(
                            text = "Income today",
                            style = MaterialTheme.typography.labelSmall,
                            color = subLabelColor
                        )
                        Text(
                            text = "+${formatAmount(data.todayIncome, currencySymbol)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isHealthyState) contentColor else Color(0xFF16A34A)
                        )
                    }
                    
                    // Expense
                    Column {
                        Text(
                            text = "Spent today",
                            style = MaterialTheme.typography.labelSmall,
                            color = subLabelColor
                        )
                        Text(
                            text = "-${formatAmount(data.todayExpense, currencySymbol)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isWarningState) contentColor else MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                // Bottom Section: Dual Graph
                Column {
                    Text(
                        text = "This Month Activity",
                        style = MaterialTheme.typography.labelSmall,
                        color = subLabelColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DualLineGraph(
                        incomeData = data.incomeGraphData,
                        expenseData = data.spendingGraphData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        incomeColor = if(isHealthyState) contentColor else Color(0xFF16A34A),
                        expenseColor = if(isWarningState) contentColor else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun DualLineGraph(
    incomeData: List<Float>,
    expenseData: List<Float>,
    modifier: Modifier = Modifier,
    incomeColor: Color,
    expenseColor: Color
) {
    val allData = incomeData + expenseData
    if (allData.all { it == 0f }) {
         Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                "No activity",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
        return
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        val maxVal = (incomeData + expenseData).maxOrNull() ?: 1f
        val safeMax = if (maxVal == 0f) 1f else maxVal
        
        // Helper to draw a single line
        fun drawLineStr(data: List<Float>, color: Color) {
            if (data.isEmpty()) return
            
            val points = data.mapIndexed { index, value ->
                val x = (index.toFloat() / (data.size - 1)) * width
                val y = height - ((value / safeMax) * height)
                androidx.compose.ui.geometry.Offset(x, y)
            }
            
            val path = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val ctrl1 = androidx.compose.ui.geometry.Offset((p0.x + p1.x) / 2, p0.y)
                        val ctrl2 = androidx.compose.ui.geometry.Offset((p0.x + p1.x) / 2, p1.y)
                        cubicTo(ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, p1.x, p1.y)
                    }
                }
            }
            
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            
            // Gradient Fill
            val fillPath = Path().apply {
                addPath(path)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                )
            )
        }
        
        drawLineStr(incomeData, incomeColor)
        drawLineStr(expenseData, expenseColor)
    }
}
