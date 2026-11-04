package com.soilcarbon.engine.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.soilcarbon.engine.domain.model.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationProvider(private val context: Context) {

    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(context) }
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun lastKnown(): GeoPoint? {
        if (!hasPermission()) return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        val best = providers.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
        return best?.toGeoPoint()
    }

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): GeoPoint? {
        if (!hasPermission()) return null
        return suspendCancellableCoroutine { continuation ->
            val token = CancellationTokenSource()
            continuation.invokeOnCancellation { token.cancel() }
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                .addOnSuccessListener { location ->
                    continuation.resume(location?.toGeoPoint() ?: lastKnown())
                }
                .addOnFailureListener {
                    continuation.resume(lastKnown())
                }
        }
    }

    private fun Location.toGeoPoint(): GeoPoint = GeoPoint(
        latitude = latitude,
        longitude = longitude,
        altitudeMeters = if (hasAltitude()) altitude else null,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        provider = provider ?: "unknown",
        timestampMillis = time.takeIf { it > 0 } ?: System.currentTimeMillis()
    )
}
