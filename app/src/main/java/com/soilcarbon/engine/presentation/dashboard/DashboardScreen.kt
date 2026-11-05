package com.soilcarbon.engine.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soilcarbon.engine.domain.model.ModelMetadata
import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.presentation.components.ColorSwatch
import com.soilcarbon.engine.presentation.components.EmptyState
import com.soilcarbon.engine.presentation.components.InfoRow
import com.soilcarbon.engine.presentation.components.SectionHeader
import com.soilcarbon.engine.presentation.components.StatTile
import com.soilcarbon.engine.presentation.components.StatsRow
import com.soilcarbon.engine.presentation.components.formatNumber
import com.soilcarbon.engine.presentation.components.formatPercent
import com.soilcarbon.engine.presentation.components.formatUtc
import java.util.Locale

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(14.dp))
            SectionHeader(
                title = "Field dashboard",
                subtitle = "Calibration health, model identity and the current SOC distribution."
            )
        }
        item { StatsRow(state.stats) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    label = "Mean confidence",
                    value = state.stats.meanConfidence?.let { "%.0f%%".format(Locale.US, it * 100) } ?: "—",
                    caption = "model confidence",
                    modifier = Modifier.weight(1f),
                    accent = MaterialTheme.colorScheme.primary
                )
                StatTile(
                    label = "Pending sync",
                    value = state.stats.pendingSync.toString(),
                    caption = "local records",
                    modifier = Modifier.weight(1f),
                    accent = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        if (state.message != null || state.error != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.error != null) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            state.error ?: state.message.orEmpty(),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = viewModel::clearBanners) { Text("Close") }
                    }
                }
            }
        }
        item {
            ModelCard(
                model = state.modelInfo?.metadata,
                loaded = state.modelInfo?.loaded == true,
                sha = state.modelInfo?.sha256,
                fallbackReason = state.modelInfo?.fallbackReason,
                loading = state.loadingModel,
                onRefresh = viewModel::refreshModel
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Radiometric calibration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    val calibration = state.calibration
                    if (calibration == null) {
                        Text(
                            "No calibration profile stored yet. Capture a card-aligned sample to fit the six-patch D65 transfer function.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        InfoRow("Card", calibration.cardVersion)
                        InfoRow("Quality", calibration.result.qualityLabel(), emphasize = true)
                        InfoRow("RMSE", formatNumber(calibration.result.overallRmse, 4))
                        InfoRow("Worst R²", formatNumber(calibration.result.worstRSquared, 4))
                        InfoRow("Illumination", formatNumber(calibration.result.illuminationVariation, 4))
                        InfoRow("Calibrated", formatUtc(calibration.createdAtMillis))
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CloudSync, null)
                        Spacer(Modifier.size(8.dp))
                        Text("Refresh covariates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Last refresh: ${formatUtc(state.settings.lastSyncAtMillis)}" +
                            (state.settings.lastSyncMessage?.let { "\n$it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = viewModel::syncNow,
                        enabled = !state.syncing
                    ) {
                        if (state.syncing) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.size(8.dp))
                        }
                        Text("Refresh covariates now")
                    }
                }
            }
        }
        item {
            SectionHeader(
                title = "Recent samples",
                subtitle = "${state.samples.size} stored · ${state.stats.analyzed} analysed"
            )
        }
        if (state.samples.isEmpty()) {
            item {
                EmptyState(
                    title = "No samples yet",
                    message = "The dashboard fills automatically as calibrated samples are captured."
                )
            }
        } else {
            items(state.samples.take(5), key = { it.id }) { sample ->
                RecentRow(sample)
            }
        }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

@Composable
private fun ModelCard(
    model: ModelMetadata?,
    loaded: Boolean,
    sha: String?,
    fallbackReason: String?,
    loading: Boolean,
    onRefresh: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Science, null)
                Spacer(Modifier.size(8.dp))
                Text("Inference model", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else OutlinedButton(onClick = onRefresh) { Text("Reload") }
            }
            Spacer(Modifier.height(6.dp))
            if (model == null) {
                Text("Reading model metadata…", style = MaterialTheme.typography.bodySmall)
            } else {
                InfoRow("Model", "${model.modelId} @ ${model.modelVersion}")
                InfoRow("Mode", if (loaded) "TFLite asset loaded" else "Heuristic fallback", emphasize = true)
                InfoRow("Validation RMSE", formatPercent(model.validationRmsePercent))
                InfoRow("Validation R²", formatNumber(model.validationR2, 3))
                InfoRow("Uncertainty", model.uncertaintyMode)
                InfoRow("Feature order", model.featureOrder.joinToString(", "))
                InfoRow("SHA-256", sha?.take(24)?.plus("…") ?: "—")
                if (!model.description.isBlank()) {
                    Text(
                        model.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (!fallbackReason.isNullOrBlank()) {
                    Text(
                        fallbackReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentRow(sample: SoilSample) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sample.lab?.let { ColorSwatch(it, Modifier.size(44.dp)) }
            Column(Modifier.weight(1f)) {
                Text(sample.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${formatPercent(sample.prediction?.socPercent)} SOC · ${formatUtc(sample.createdAtMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
