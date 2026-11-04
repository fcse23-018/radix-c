package com.soilcarbon.engine.presentation.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soilcarbon.engine.core.Constants
import com.soilcarbon.engine.domain.model.AppSettings
import com.soilcarbon.engine.domain.model.UserRole
import com.soilcarbon.engine.presentation.components.InfoRow
import com.soilcarbon.engine.presentation.components.SectionHeader
import com.soilcarbon.engine.presentation.components.formatNumber
import com.soilcarbon.engine.presentation.components.formatPercent
import com.soilcarbon.engine.presentation.components.formatUtc
import java.util.Locale

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(14.dp))
            SectionHeader(
                title = "Settings",
                subtitle = "Workspace role, capture policy, calibration card and model identity."
            )
        }
        if (state.message != null || state.error != null) {
            item {
                Banner(
                    text = state.error ?: state.message.orEmpty(),
                    error = state.error != null,
                    onDismiss = viewModel::clearBanners
                )
            }
        }
        item {
            SettingsCard(icon = Icons.Outlined.Tune, title = "Working role") {
                Text(
                    "Switches the workspace between capture, agronomy and data-science tooling.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                UserRole.entries.forEach { role ->
                    RoleRow(
                        role = role,
                        selected = settings.role == role,
                        onSelect = { viewModel.setRole(role) }
                    )
                }
            }
        }
        item {
            SoilDefaultsCard(
                settings = settings,
                onSave = viewModel::saveSoilDefaults
            )
        }
        item {
            CapturePolicyCard(
                settings = settings,
                onChange = viewModel::setCapturePolicy
            )
        }
        item {
            NetworkModelCard(
                settings = settings,
                onNetworkPolicy = viewModel::setNetworkPolicy,
                onGpu = viewModel::setGpuEnabled,
                onRefreshModel = viewModel::refreshModel,
                loadingModel = state.loadingModel,
                modelInfo = state.modelInfo
            )
        }
        item {
            CalibrationCard(
                state = state,
                onClear = viewModel::clearCalibration
            )
        }
        item {
            SyncCard(
                state = state,
                onSync = viewModel::syncNow
            )
        }
        item {
            SettingsCard(icon = Icons.Outlined.Science, title = "About") {
                InfoRow("Application", Constants.APP_NAME)
                InfoRow("SRS document", "${Constants.SRS_DOCUMENT_ID} v${Constants.SRS_VERSION}")
                InfoRow("SOC → SOM factor", Constants.VAN_BEMMELEN_FACTOR.toString())
                InfoRow("Minimum API level", "28 (Android 9)")
                Text(
                    "Estimates are research-grade screening values. Validate against dry combustion " +
                        "(ISO 10694) before regulatory, trading or carbon-credit use.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun RoleRow(role: UserRole, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        onClick = onSelect,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(Modifier.weight(1f)) {
                Text(role.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    role.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SoilDefaultsCard(settings: AppSettings, onSave: (Double, Double) -> Unit) {
    var bulkText by rememberSaveable(settings.bulkDensityGcm3) {
        mutableStateOf(settings.bulkDensityGcm3.toString())
    }
    var depthText by rememberSaveable(settings.depthCm) { mutableStateOf(settings.depthCm.toString()) }
    val bulk = bulkText.toDoubleOrNull()
    val depth = depthText.toDoubleOrNull()
    val valid = bulk != null && bulk in 0.5..2.5 && depth != null && depth in 1.0..200.0

    SettingsCard(icon = Icons.Outlined.Palette, title = "Soil defaults") {
        Text(
            "Used for the carbon-stock conversion (Mg C/ha). Override per sampling protocol.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = bulkText,
                onValueChange = { bulkText = it },
                label = { Text("Bulk density") },
                suffix = { Text("g/cm³") },
                singleLine = true,
                isError = bulkText.isNotBlank() && (bulk == null || bulk !in 0.5..2.5),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = depthText,
                onValueChange = { depthText = it },
                label = { Text("Depth") },
                suffix = { Text("cm") },
                singleLine = true,
                isError = depthText.isNotBlank() && (depth == null || depth !in 1.0..200.0),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { if (bulk != null && depth != null) onSave(bulk, depth) },
            enabled = valid
        ) { Text("Save soil defaults") }
    }
}

@Composable
private fun CapturePolicyCard(
    settings: AppSettings,
    onChange: (Boolean, Boolean, Float) -> Unit
) {
    var minLux by rememberSaveable(settings.minLux) { mutableFloatStateOf(settings.minLux) }

    SettingsCard(icon = Icons.Outlined.Tune, title = "Capture policy") {
        ToggleRow(
            title = "Require RAW + JPEG",
            subtitle = "Reject captures that do not produce both DNG and JPEG.",
            checked = settings.requireRawCapture,
            onCheckedChange = { onChange(it, settings.requireGps, settings.minLux) }
        )
        ToggleRow(
            title = "Require GPS fix",
            subtitle = "Block capture when no valid location is available.",
            checked = settings.requireGps,
            onCheckedChange = { onChange(settings.requireRawCapture, it, settings.minLux) }
        )
        HorizontalDivider(Modifier.padding(vertical = 6.dp))
        Text(
            "Minimum illuminance: ${minLux.toInt()} lux",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Slider(
            value = minLux,
            onValueChange = { minLux = it },
            valueRange = 10f..500f,
            steps = 48
        )
        Text(
            "The live gate warns below this level. Diffuse daylight is strongly preferred.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick = {
                onChange(
                    settings.requireRawCapture,
                    settings.requireGps,
                    minLux.coerceIn(10f, 1000f)
                )
            }
        ) { Text("Apply light threshold") }
    }
}

@Composable
private fun NetworkModelCard(
    settings: AppSettings,
    onNetworkPolicy: (Boolean, Boolean) -> Unit,
    onGpu: (Boolean) -> Unit,
    onRefreshModel: () -> Unit,
    loadingModel: Boolean,
    modelInfo: com.soilcarbon.engine.domain.model.ModelInfo?
) {
    SettingsCard(icon = Icons.Outlined.Memory, title = "Network & inference") {
        ToggleRow(
            title = "Allow network covariate lookup",
            subtitle = "SoilGrids, Open-Meteo and Sentinel NDVI. Cached results are used offline.",
            checked = settings.allowNetworkSync,
            onCheckedChange = { onNetworkPolicy(it, settings.autoSync) }
        )
        ToggleRow(
            title = "Automatic background sync",
            subtitle = "Refresh covariate caches with WorkManager when connectivity allows.",
            checked = settings.autoSync,
            onCheckedChange = { onNetworkPolicy(settings.allowNetworkSync, it) }
        )
        ToggleRow(
            title = "Prefer GPU delegate",
            subtitle = "Falls back to NNAPI or CPU when the delegate is unavailable.",
            checked = settings.enableGpuDelegate,
            onCheckedChange = onGpu
        )
        HorizontalDivider(Modifier.padding(vertical = 6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Model", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (loadingModel) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = onRefreshModel) { Text("Reload") }
            }
        }
        val model = modelInfo?.metadata
        if (model == null) {
            Text("Model metadata not loaded.", style = MaterialTheme.typography.bodySmall)
        } else {
            InfoRow("Identifier", "${model.modelId} @ ${model.modelVersion}")
            InfoRow("Status", if (modelInfo.loaded) "TFLite asset loaded" else "Heuristic fallback", emphasize = true)
            InfoRow("Validation RMSE", formatPercent(model.validationRmsePercent))
            InfoRow("Validation R²", formatNumber(model.validationR2, 3))
            InfoRow("Uncertainty", model.uncertaintyMode)
            InfoRow("SHA-256", modelInfo.sha256.take(24) + "…")
            if (!modelInfo.fallbackReason.isNullOrBlank()) {
                Text(
                    modelInfo.fallbackReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CalibrationCard(state: SettingsUiState, onClear: () -> Unit) {
    val spec = state.cardSpec
    SettingsCard(icon = Icons.Outlined.Palette, title = "Calibration target") {
        if (spec == null) {
            Text("Card specification unavailable; compiled-in defaults are active.")
        } else {
            InfoRow("Card version", spec.version)
            InfoRow(
                "Physical size",
                "%.0f × %.0f mm".format(Locale.US, spec.cardWidthMm, spec.cardHeightMm)
            )
            InfoRow("ArUco markers", spec.markers.joinToString(", ") { it.id.toString() })
            InfoRow("Marker size", "%.0f mm".format(Locale.US, spec.markers.firstOrNull()?.sizeMm ?: 0.0))
            InfoRow(
                "Aperture",
                "%.0f × %.0f mm".format(Locale.US, spec.aperture.widthMm, spec.aperture.heightMm)
            )
            InfoRow(
                "Patches",
                spec.patches.joinToString(" · ") { "%.0f%%".format(Locale.US, it.targetReflectance * 100) }
            )
            if (spec.notes.isNotBlank()) {
                Text(
                    spec.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("Active profile", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        val calibration = state.calibration
        if (calibration == null) {
            Text(
                "No calibration profile stored. The first card-aligned capture fits the six-patch " +
                    "D65 transfer function automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            InfoRow("Card", calibration.cardVersion)
            InfoRow("Quality", calibration.result.qualityLabel(), emphasize = true)
            InfoRow("RMSE", formatNumber(calibration.result.overallRmse, 4))
            InfoRow("Worst R²", formatNumber(calibration.result.worstRSquared, 4))
            InfoRow("Illumination variation", formatNumber(calibration.result.illuminationVariation, 4))
            InfoRow("Fitted", formatUtc(calibration.createdAtMillis))
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = onClear, enabled = !state.clearingCalibration) {
                if (state.clearingCalibration) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(6.dp))
                }
                Text("Clear calibration profile")
            }
        }
    }
}

@Composable
private fun SyncCard(state: SettingsUiState, onSync: () -> Unit) {
    SettingsCard(icon = Icons.Outlined.CloudSync, title = "Covariate sync") {
        InfoRow("Last sync", formatUtc(state.settings.lastSyncAtMillis))
        state.settings.lastSyncMessage?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onSync, enabled = !state.syncing && state.settings.allowNetworkSync) {
            if (state.syncing) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
            }
            Text("Sync now")
        }
        if (!state.settings.allowNetworkSync) {
            Text(
                "Enable network covariate lookup above to sync.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.size(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
