package com.soilcarbon.engine.presentation.capture

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soilcarbon.engine.domain.calibration.NormalizedPoint
import com.soilcarbon.engine.domain.model.CameraExposureParameters
import com.soilcarbon.engine.presentation.components.SectionHeader
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CaptureScreen(
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var showControls by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        cameraGranted = result[Manifest.permission.CAMERA] == true
        viewModel.onPermissionResult(cameraGranted)
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) viewModel.refreshLocation()
    }

    LaunchedEffect(Unit) {
        viewModel.onPermissionResult(cameraGranted)
        if (!cameraGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.refreshLocation()
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(cameraGranted) {
        if (cameraGranted) viewModel.startCamera(lifecycleOwner, previewView)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopCamera() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            TargetOverlay(
                detection = state.detection,
                modifier = Modifier.fillMaxSize()
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SensorPill(
                    icon = { Icon(Icons.Outlined.ScreenRotation, null, Modifier.size(14.dp)) },
                    text = state.imu?.let { "${it.maxTiltDegrees.roundToInt()}°" } ?: "tilt —",
                    ok = state.levelOk
                )
                SensorPill(
                    icon = { Icon(Icons.Outlined.LightMode, null, Modifier.size(14.dp)) },
                    text = state.light?.let { "${it.lux.roundToInt()} lx" } ?: "lux —",
                    ok = state.lightOk
                )
                SensorPill(
                    icon = { Icon(Icons.Outlined.GpsFixed, null, Modifier.size(14.dp)) },
                    text = if (state.location != null) "GPS" else "GPS —",
                    ok = state.gpsOk
                )
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.62f)
            ) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = viewModel.guidance(state),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    state.detection?.let { detection ->
                        Text(
                            text = "Markers ${detection.detectedIds.sorted().joinToString(",")} · " +
                                "patches ${detection.patchQuads.size}/6 · rotation " +
                                "${detection.rotationDegrees.roundToInt()}°",
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            if (!cameraGranted) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.CameraAlt, null)
                        Spacer(Modifier.height(8.dp))
                        Text("Camera and location access are required", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }) { Text("Grant permissions") }
                    }
                }
            }
            if (state.busy) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text(state.analysisStep, color = Color.White)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = "Field capture",
                subtitle = "Place the card over level soil, fill the frame, and capture RAW + JPEG."
            )

            if (state.cameraError != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        state.cameraError ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.refreshLocation() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.GpsFixed, null, Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (state.locating) "Locating…" else "Refresh GPS")
                }
                OutlinedButton(
                    onClick = { showControls = !showControls },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Thermostat, null, Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Camera controls")
                    Icon(Icons.Outlined.ExpandMore, null, Modifier.size(16.dp))
                }
            }

            if (showControls) {
                ManualControls(
                    exposure = state.exposure,
                    onUpdate = viewModel::updateExposure,
                    onApply = viewModel::applyExposure
                )
            }

            OutlinedTextField(
                value = state.tag,
                onValueChange = viewModel::setTag,
                label = { Text("Sample tag (optional)") },
                placeholder = { Text("e.g. North paddock A3") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::setNotes,
                label = { Text("Field notes") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.moisturePercent,
                onValueChange = viewModel::setMoisture,
                label = { Text("Gravimetric moisture % (optional)") },
                supportingText = { Text("Blank = no EPO correction. Enter a lab or gravimetric value when available.") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (state.location != null) {
                Text(
                    text = "GPS ${state.location?.formatted()} · ±" +
                        "${state.location?.accuracyMeters?.roundToInt() ?: "?"} m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            state.message?.let { message ->
                val demo = state.lastSample?.prediction?.isDemo == true
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (demo) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                ) {
                    Column(Modifier.padding(12.dp)) {
                        if (demo) {
                            Text(
                                "DEMO – NOT VALID",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (state.lastSample?.prediction?.degraded == true) {
                            Text(
                                "DEGRADED: at least one model input used a default value.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            message,
                            color = if (demo) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                        TextButton(onClick = onOpenReview) { Text("Open review") }
                    }
                }
            }
            state.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (state.busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(state.analysisStep, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = viewModel::capture,
                enabled = state.canCapture,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Outlined.CameraAlt, null)
                Spacer(Modifier.size(8.dp))
                Text(if (state.busy) "Processing…" else "Capture calibrated sample")
            }
            Text(
                text = "Gate: tilt ≤ ${state.settings.maxTiltDegrees.toInt()}°, " +
                    "light ≥ ${state.settings.minLux.toInt()} lux, " +
                    "GPS ${if (state.settings.requireGps) "required" else "optional"}, " +
                    "all 5 markers aligned.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SensorPill(
    icon: @Composable () -> Unit,
    text: String,
    ok: Boolean
) {
    Surface(
        color = if (ok) Color(0xFF1B5E20).copy(alpha = 0.88f) else Color(0xFF7F1D1D).copy(alpha = 0.88f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.heightIn(min = 26.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides Color.White
            ) {
                icon()
                Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun TargetOverlay(
    detection: com.soilcarbon.engine.domain.calibration.TargetDetection?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val card = detection?.cardQuad.orEmpty()
        val aperture = detection?.apertureQuad.orEmpty()
        if (card.size == 4) {
            val path = Path().apply {
                moveTo(card[0].x * size.width, card[0].y * size.height)
                lineTo(card[1].x * size.width, card[1].y * size.height)
                lineTo(card[2].x * size.width, card[2].y * size.height)
                lineTo(card[3].x * size.width, card[3].y * size.height)
                close()
            }
            drawPath(path, Color(0xFF9CD67F).copy(alpha = 0.16f))
            drawPath(path, Color(0xFF9CD67F), style = Stroke(width = 3f))
        }
        if (aperture.size == 4) {
            val path = Path().apply {
                moveTo(aperture[0].x * size.width, aperture[0].y * size.height)
                lineTo(aperture[1].x * size.width, aperture[1].y * size.height)
                lineTo(aperture[2].x * size.width, aperture[2].y * size.height)
                lineTo(aperture[3].x * size.width, aperture[3].y * size.height)
                close()
            }
            drawPath(path, Color(0xFFFFC107), style = Stroke(width = 5f))
        }
        detection?.markers?.forEach { marker ->
            drawCircle(
                color = Color(0xFF80D8FF),
                radius = 5f,
                center = Offset(marker.center.x * size.width, marker.center.y * size.height)
            )
        }
    }
}

@Composable
private fun ManualControls(
    exposure: CameraExposureParameters?,
    onUpdate: (Int, Long, Float, Int) -> Unit,
    onApply: () -> Unit
) {
    var iso by remember(exposure?.iso) { mutableFloatStateOf((exposure?.iso ?: 100).toFloat()) }
    var shutterMs by remember(exposure?.exposureTimeNs) {
        mutableFloatStateOf(((exposure?.exposureTimeNs ?: 8_000_000L) / 1_000_000.0).toFloat())
    }
    var focus by remember(exposure?.focusDistanceDiopters) {
        mutableFloatStateOf(exposure?.focusDistanceDiopters ?: 6.67f)
    }
    var kelvin by remember(exposure?.whiteBalanceKelvin) {
        mutableFloatStateOf((exposure?.whiteBalanceKelvin ?: 6500).toFloat())
    }

    fun push() {
        onUpdate(iso.toInt(), (shutterMs * 1_000_000).toLong(), focus, kelvin.toInt())
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Manual exposure (locked)", fontWeight = FontWeight.SemiBold)
            Text(
                "AE, AWB and AF are locked for reproducible reflectance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            SliderRow("ISO", String.format(Locale.US, "%.0f", iso)) {
                Slider(
                    value = iso,
                    onValueChange = { iso = it; push() },
                    valueRange = 50f..6400f
                )
            }
            SliderRow("Shutter", String.format(Locale.US, "%.2f ms", shutterMs)) {
                Slider(
                    value = shutterMs,
                    onValueChange = { shutterMs = it; push() },
                    valueRange = 0.05f..120f
                )
            }
            SliderRow("Focus", String.format(Locale.US, "%.2f dpt", focus)) {
                Slider(
                    value = focus,
                    onValueChange = { focus = it; push() },
                    valueRange = 0f..10f
                )
            }
            SliderRow("White balance", "${kelvin.toInt()} K") {
                Slider(
                    value = kelvin,
                    onValueChange = { kelvin = it; push() },
                    valueRange = 2500f..9000f
                )
            }
            FilledTonalButton(onClick = onApply, modifier = Modifier.fillMaxWidth()) {
                Text("Apply to preview")
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: String,
    content: @Composable () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(0.32f))
        Box(Modifier.weight(0.68f)) { content() }
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(0.30f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Suppress("unused")
private fun NormalizedPoint.toOffset(width: Float, height: Float): Offset =
    Offset(x * width, y * height)
