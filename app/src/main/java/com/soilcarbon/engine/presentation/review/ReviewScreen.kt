package com.soilcarbon.engine.presentation.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soilcarbon.engine.domain.model.SampleStatus
import com.soilcarbon.engine.domain.model.SoilSample
import com.soilcarbon.engine.presentation.components.ColorSwatch
import com.soilcarbon.engine.presentation.components.EmptyState
import com.soilcarbon.engine.presentation.components.InfoRow
import com.soilcarbon.engine.presentation.components.SectionHeader
import com.soilcarbon.engine.presentation.components.StatsRow
import com.soilcarbon.engine.presentation.components.formatNumber
import com.soilcarbon.engine.presentation.components.formatPercent
import com.soilcarbon.engine.presentation.components.formatUtc
import com.soilcarbon.engine.presentation.components.syncLabel
import java.util.Locale

@Composable
fun ReviewScreen(
    modifier: Modifier = Modifier,
    focusSampleId: String? = null,
    onFocusConsumed: () -> Unit = {},
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(focusSampleId) {
        if (focusSampleId != null) {
            viewModel.select(focusSampleId)
            onFocusConsumed()
        }
    }
    val selected = state.selected
    if (selected != null) {
        SampleDetail(
            sample = selected,
            isExporting = state.exporting,
            message = state.exportMessage,
            error = state.error,
            onBack = { viewModel.select(null) },
            onExport = { viewModel.export(ExportKind.BUNDLE, onlySelected = true) },
            onDelete = viewModel::deleteSelected,
            modifier = modifier
        )
    } else {
        SampleList(
            state = state,
            onSelect = { viewModel.select(it) },
            onExport = { viewModel.export(it) },
            onBannerDismiss = viewModel::clearBanners,
            modifier = modifier
        )
    }
}

@Composable
private fun SampleList(
    state: ReviewUiState,
    onSelect: (String) -> Unit,
    onExport: (ExportKind) -> Unit,
    onBannerDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showExportDialog by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(
                    title = "Sample review",
                    subtitle = "Every record carries its full feature vector, calibration and provenance.",
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showExportDialog = true }) {
                    Icon(Icons.Outlined.FileDownload, contentDescription = "Export all")
                }
            }
        }
        item { StatsRow(state.stats) }
        if (state.exportMessage != null || state.error != null) {
            item {
                Banner(
                    text = state.error ?: state.exportMessage.orEmpty(),
                    error = state.error != null,
                    onDismiss = onBannerDismiss
                )
            }
        }
        if (state.samples.isEmpty()) {
            item {
                EmptyState(
                    title = "No samples yet",
                    message = "Capture a calibrated sample from the Capture workspace. " +
                        "All data stays encrypted on this device until you export it."
                )
            }
        } else {
            items(state.samples, key = { it.id }) { sample ->
                SampleCard(sample = sample, onClick = { onSelect(sample.id) })
            }
        }
        item { Spacer(Modifier.height(30.dp)) }
    }

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onSelect = {
                showExportDialog = false
                onExport(it)
            }
        )
    }
}

@Composable
private fun SampleCard(sample: SoilSample, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val lab = sample.lab
            if (lab != null) {
                ColorSwatch(lab)
            } else {
                Box(
                    Modifier
                        .size(56.dp)
                        .then(Modifier)
                )
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        sample.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                when {
                                    sample.prediction == null -> "Not analysed"
                                    sample.prediction.isDemo -> "DEMO – NOT VALID"
                                    sample.prediction.degraded -> "Degraded"
                                    else -> "Valid"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
                Text(
                    "${sample.prediction?.let { formatPercent(it.socPercent) } ?: "Not analysed"} SOC · " +
                        "±${formatNumber(sample.prediction?.uncertainty95)} · " +
                        "${sample.prediction?.confidenceScore?.let { "%.0f%%".format(Locale.US, it * 100) } ?: "—"} confidence",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${formatUtc(sample.createdAtMillis)} · ${syncLabel(sample.syncState)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (sample.status == SampleStatus.FAILED && sample.errorMessage != null) {
                    Text(
                        sample.errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SampleDetail(
    sample: SoilSample,
    isExporting: Boolean,
    message: String?,
    error: String?,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmDelete by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        sample.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${formatUtc(sample.createdAtMillis)} · ${sample.status.name} · ${syncLabel(sample.syncState)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete sample",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        if (message != null || error != null) {
            item { Banner(text = error ?: message.orEmpty(), error = error != null, onDismiss = {}) }
        }
        if (sample.prediction?.isDemo == true) {
            item {
                StatusCard(
                    title = "DEMO – NOT VALID",
                    body = "This release is running a placeholder model. The number below must not be " +
                        "used for reporting, accounting or land-management decisions."
                )
            }
        }
        if (sample.prediction?.degraded == true) {
            item {
                StatusCard(
                    title = "DEGRADED",
                    body = "At least one model input was a default rather than a measurement " +
                        "(most often satellite NDVI without a Sentinel token). Check provenance below."
                )
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (sample.prediction?.isDemo == true) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    sample.lab?.let { ColorSwatch(it, Modifier.size(72.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text("Soil organic carbon", style = MaterialTheme.typography.labelMedium)
                        Text(
                            formatPercent(sample.prediction?.socPercent),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "SOM ${formatPercent(sample.prediction?.somPercent)} · " +
                                "95% CI ${formatPercent(sample.prediction?.socLower95)} – " +
                                "${formatPercent(sample.prediction?.socUpper95)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp)) {
                    InfoRow("Confidence", sample.prediction?.let { "%.1f%%".format(Locale.US, it.confidenceScore * 100) } ?: "—", emphasize = true)
                    InfoRow("Report status", sample.prediction?.reportStatus ?: "—")
                    InfoRow("Degraded", if (sample.prediction?.degraded == true) "Yes — defaulted inputs" else "No")
                    InfoRow("Carbon stock", sample.prediction?.carbonStockMgPerHa?.let { "%.1f Mg C/ha".format(Locale.US, it) } ?: "—")
                    InfoRow("Model", sample.prediction?.let { "${it.modelId} @ ${it.modelVersion}" } ?: "—")
                    InfoRow("Demo only", if (sample.prediction?.isDemo == true) "Yes — not a valid estimate" else "No")
                    InfoRow("Inference", sample.prediction?.let { "${it.inferenceMillis} ms" } ?: "—")
                    if (!sample.prediction?.notes.isNullOrBlank()) {
                        Text(
                            sample.prediction?.notes.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
        item {
            DetailSection("Colour & texture") {
                InfoRow("Munsell", sample.munsell?.notation() ?: "—")
                InfoRow("L* / a* / b*", sample.lab?.let { "%.2f / %.2f / %.2f".format(Locale.US, it.l, it.a, it.b) } ?: "—")
                InfoRow("Reflectance R/G/B", sample.reflectance?.let {
                    "%.3f / %.3f / %.3f".format(Locale.US, it.r, it.g, it.b)
                } ?: "—")
                InfoRow("GLCM contrast", formatNumber(sample.texture?.contrast, 4))
                InfoRow("GLCM energy", formatNumber(sample.texture?.energy, 4))
                InfoRow("GLCM homogeneity", formatNumber(sample.texture?.homogeneity, 4))
                InfoRow("GLCM correlation", formatNumber(sample.texture?.correlation, 4))
            }
        }
        item {
            DetailSection("SRS feature vector (order fixed)") {
                sample.features?.toDisplayPairs()?.forEach { (label, value) ->
                    InfoRow(label, value)
                } ?: Text("Feature vector unavailable.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    sample.features?.toCanonicalString() ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (sample.prediction?.provenance?.isNotEmpty() == true) {
                    Spacer(Modifier.height(8.dp))
                    Text("Input provenance", style = MaterialTheme.typography.labelMedium)
                    sample.prediction.provenance.forEach { (feature, source) ->
                        InfoRow(feature, source, emphasize = source == "DEFAULT")
                    }
                }
            }
        }
        item {
            DetailSection("Radiometric calibration") {
                InfoRow("Card", sample.calibration?.cardVersion ?: "—")
                InfoRow("Quality", sample.calibration?.qualityLabel ?: "—", emphasize = true)
                InfoRow("RMSE", formatNumber(sample.calibration?.overallRmse, 4))
                InfoRow("Worst R²", formatNumber(sample.calibration?.worstRSquared, 4))
                InfoRow("Illumination variation", formatNumber(sample.calibration?.illuminationVariation, 4))
                InfoRow("Gain R/G/B", sample.calibration?.let {
                    "%.3f / %.3f / %.3f".format(Locale.US, it.redGain, it.greenGain, it.blueGain)
                } ?: "—")
                InfoRow("Offset R/G/B", sample.calibration?.let {
                    "%.3f / %.3f / %.3f".format(Locale.US, it.redOffset, it.greenOffset, it.blueOffset)
                } ?: "—")
            }
        }
        item {
            DetailSection("Moisture & covariates") {
                InfoRow("Moisture", sample.moisture?.let {
                    "%.1f%% (%s)".format(Locale.US, it.percentGravimetric, it.method.name)
                } ?: "Not measured")
                sample.moistureCorrectionNotes?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                InfoRow("NDVI (satellite)", formatNumber(sample.covariates?.ndvi?.value, 3))
                InfoRow("NDVI source", sample.covariates?.ndvi?.source?.name ?: "—")
                InfoRow("Clay %", formatPercent(sample.covariates?.clayPercent?.value, 1))
                InfoRow("Clay source", sample.covariates?.clayPercent?.source?.name ?: "—")
                InfoRow("SOC baseline", formatPercent(sample.covariates?.socBaselinePercent?.value))
                InfoRow("MAT / MAP", sample.covariates?.let {
                    "${formatNumber(it.meanAnnualTempC?.value, 1)} °C / " +
                        "${formatNumber(it.meanAnnualPrecipMm?.value, 0)} mm"
                } ?: "—")
                InfoRow("Offline fallback", if (sample.covariates?.isOfflineFallback == true) "Yes" else "No")
            }
        }
        item {
            DetailSection("Capture conditions") {
                InfoRow("GPS", sample.location?.formatted() ?: "—")
                InfoRow("Altitude", sample.location?.altitudeMeters?.let { "%.1f m".format(Locale.US, it) } ?: "—")
                InfoRow("Tilt", sample.imu?.let { "%.1f°".format(Locale.US, it.maxTiltDegrees) } ?: "—")
                InfoRow("Illuminance", sample.light?.let { "%.0f lux".format(Locale.US, it.lux) } ?: "—")
                InfoRow("Exposure", sample.exposure?.let { "ISO ${it.iso} · ${it.exposureTimeNs / 1_000_000} ms" } ?: "—")
                InfoRow("White balance", sample.exposure?.let { "${it.whiteBalanceKelvin} K" } ?: "—")
                InfoRow("RAW saved", if (sample.rawImagePath != null) "Yes (SHA-256)" else "No")
                InfoRow("Device", sample.exposure?.let { "${it.deviceManufacturer} ${it.deviceModel}" } ?: "—")
            }
        }
        item {
            DetailSection("Provenance") {
                InfoRow("SHA-256", sample.provenanceSha256 ?: "—")
                InfoRow("JPEG SHA-256", sample.jpegSha256 ?: "—")
                InfoRow("RAW SHA-256", sample.rawSha256 ?: "—")
                if (!sample.notes.isBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Notes", style = MaterialTheme.typography.labelMedium)
                    Text(sample.notes, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onExport,
                    enabled = !isExporting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Share, null, Modifier.size(18.dp))
                    }
                    Spacer(Modifier.size(6.dp))
                    Text("Export bundle")
                }
                OutlinedButton(onClick = { confirmDelete = true }) { Text("Delete") }
            }
        }
        item { Spacer(Modifier.height(30.dp)) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete sample?") },
            text = { Text("The encrypted image files remain in secure storage until purged by the OS. " +
                "The database record and provenance chain entry will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun StatusCard(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
            Text(
                body,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun Banner(text: String, error: Boolean, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (error) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = if (error) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer
            )
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    }
}

@Composable
private fun ExportDialog(onDismiss: () -> Unit, onSelect: (ExportKind) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export samples") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Files are written to the app cache and can be shared with any installed app. " +
                        "CSV is for spreadsheets; GeoJSON for GIS; the bundle retains every scientific field.",
                    style = MaterialTheme.typography.bodySmall
                )
                ExportKind.entries.forEach { kind ->
                    FilledTonalButton(
                        onClick = { onSelect(kind) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(kind.label) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
