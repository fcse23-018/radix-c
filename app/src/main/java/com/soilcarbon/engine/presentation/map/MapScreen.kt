package com.soilcarbon.engine.presentation.map

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soilcarbon.engine.presentation.components.ColorSwatch
import com.soilcarbon.engine.presentation.components.EmptyState
import com.soilcarbon.engine.presentation.components.InfoRow
import com.soilcarbon.engine.presentation.components.formatNumber
import com.soilcarbon.engine.presentation.components.formatPercent
import com.soilcarbon.engine.presentation.components.formatUtc
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(
    onOpenReview: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val onSelect by rememberUpdatedState(viewModel::select)

    Configuration.getInstance().userAgentValue = context.packageName
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            controller.setZoom(4.0)
            controller.setCenter(OsmGeoPoint(20.0, 0.0))
        }
    }

    DisposableEffect(Unit) {
        onDispose { mapView.onDetach() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                map.overlays.clear()
                val icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_mylocation)
                state.mappedSamples.forEach { sample ->
                    val location = sample.location ?: return@forEach
                    val marker = Marker(map).apply {
                        position = OsmGeoPoint(location.latitude, location.longitude, location.altitudeMeters ?: 0.0)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = sample.displayName
                        snippet = "${formatPercent(sample.prediction?.socPercent)} SOC · " +
                            "${formatNumber(sample.prediction?.uncertainty95)} ±95%"
                        this.icon = icon
                        setOnMarkerClickListener { _, _ ->
                            onSelect(sample.id)
                            true
                        }
                    }
                    map.overlays.add(marker)
                }
                state.selected?.location?.let { selected ->
                    map.controller.setCenter(OsmGeoPoint(selected.latitude, selected.longitude))
                    if (map.zoomLevelDouble < 13.0) map.controller.setZoom(14.0)
                }
                state.mappedSamples.firstOrNull()?.location?.let { first ->
                    map.controller.setCenter(OsmGeoPoint(first.latitude, first.longitude))
                    map.controller.setZoom(13.0)
                }
                map.invalidate()
            }
        )

        if (state.mappedSamples.isEmpty()) {
            EmptyState(
                title = "No georeferenced samples",
                message = "Samples captured with a GPS fix will appear here. Enable location access " +
                    "on the capture screen, then tap the map pins to inspect results.",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            )
        }

        state.selected?.let { sample ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        sample.lab?.let { ColorSwatch(it, Modifier.size(44.dp)) }
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                sample.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                sample.location?.formatted() ?: "No location",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Outlined.Map, contentDescription = null)
                    }
                    Spacer(Modifier.height(6.dp))
                    InfoRow("SOC / SOM", "${formatPercent(sample.prediction?.socPercent)} / ${formatPercent(sample.prediction?.somPercent)}")
                    InfoRow("Uncertainty ±95%", formatNumber(sample.prediction?.uncertainty95))
                    InfoRow("Captured", formatUtc(sample.createdAtMillis))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpenReview(sample.id) }, modifier = Modifier.weight(1f)) {
                            Text("Open sample review")
                        }
                    }
                }
            }
        }
    }
}
