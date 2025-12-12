package com.theblankstate.epmanager.ui.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theblankstate.epmanager.ai.InsightType
import com.theblankstate.epmanager.ai.SpendingInsight
import com.theblankstate.epmanager.ai.SpendingPrediction
import com.theblankstate.epmanager.ui.components.formatCurrency
import com.theblankstate.epmanager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIInsightsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AIInsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show errors
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "AI Insights ✨",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadInsights() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Loading state
            if (uiState.isLoading) {
                item {
                    LoadingCard()
                }
            } else {
                // Prediction Card
                uiState.prediction?.let { prediction ->
                    item {
                        PredictionCard(prediction = prediction)
                    }
                }
                
                // Insights
                if (uiState.insights.isNotEmpty()) {
                    item {
                        Text(
                            text = "Your Insights",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    items(uiState.insights) { insight ->
                        InsightCard(insight = insight)
                    }
                }
                
                // Powered by
                item {
                    Text(
                        text = "Powered by Firebase AI (Gemini)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg)
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Text(
                text = "AI is analyzing your spending...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PredictionCard(prediction: SpendingPrediction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔮",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = "Next Month Prediction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            Text(
                text = formatCurrency(prediction.predictedAmount),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val confidenceColor = when (prediction.confidence) {
                    "High" -> Success
                    "Medium" -> Warning
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                
                Text(
                    text = "${prediction.confidence} confidence",
                    style = MaterialTheme.typography.labelMedium,
                    color = confidenceColor
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            Text(
                text = prediction.reasoning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun InsightCard(insight: SpendingInsight) {
    val (containerColor, contentColor) = when (insight.type) {
        InsightType.WARNING -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        InsightType.ACHIEVEMENT -> Pair(
            Success.copy(alpha = 0.15f),
            Success
        )
        InsightType.PREDICTION -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        InsightType.TIP -> Pair(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.onSurface
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = insight.emoji,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}
