package com.soilcarbon.engine.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soilcarbon.engine.domain.model.SampleStats
import com.soilcarbon.engine.domain.model.SyncState
import com.soilcarbon.engine.domain.vision.ColorScience
import com.soilcarbon.engine.domain.vision.LabColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun LabColor.toComposeColor(): Color {
    val rgb = ColorScience.xyzToSrgb(ColorScience.labToXyz(this))
    return Color(
        red = rgb.r.coerceIn(0.0, 1.0).toFloat(),
        green = rgb.g.coerceIn(0.0, 1.0).toFloat(),
        blue = rgb.b.coerceIn(0.0, 1.0).toFloat(),
        alpha = 1f
    )
}

fun formatUtc(millis: Long?): String {
    if (millis == null) return "—"
    val formatter = SimpleDateFormat("dd MMM yyyy HH:mm 'UTC'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
    return formatter.format(Date(millis))
}

fun formatPercent(value: Double?, digits: Int = 2): String =
    if (value == null || !value.isFinite()) "—"
    else "%.${digits}f%%".format(Locale.US, value)

fun formatNumber(value: Double?, digits: Int = 2): String =
    if (value == null || !value.isFinite()) "—" else "%.${digits}f".format(Locale.US, value)

fun syncLabel(state: SyncState): String = when (state) {
    SyncState.LOCAL_ONLY -> "Local only"
    SyncState.QUEUED -> "Queued"
    SyncState.SYNCED -> "Synced"
    SyncState.FAILED -> "Sync failed"
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.2f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    caption: String? = null,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(accent, CircleShape)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!caption.isNullOrBlank()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    AssistChip(
        onClick = {},
        enabled = false,
        modifier = modifier,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    )
}

@Composable
fun StatsRow(stats: SampleStats, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                label = "Samples",
                value = stats.total.toString(),
                caption = "${stats.analyzed} analysed",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Mean SOC",
                value = formatPercent(stats.meanSocPercent),
                caption = "visible-colour model",
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.tertiary
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                label = "Mean SOM",
                value = formatPercent(stats.meanSomPercent),
                caption = "SOC × 1.724",
                modifier = Modifier.weight(1f),
                accent = Color(0xFF8D6E63)
            )
            StatTile(
                label = "±95%",
                value = formatNumber(stats.meanUncertainty),
                caption = "mean uncertainty",
                modifier = Modifier.weight(1f),
                accent = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ColorSwatch(lab: LabColor, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .background(lab.toComposeColor(), RoundedCornerShape(12.dp))
    )
}
