package com.greetings.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class LocationReport(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestamp: Instant,
) {
    fun emailBody(deviceDetails: String): String {
        val lat = String.format(Locale.US, "%.6f", latitude)
        val lon = String.format(Locale.US, "%.6f", longitude)
        val time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(timestamp)
        return "Device details: $deviceDetails\n" +
            "Location: $lat, $lon\n" +
            "Accuracy: about ${accuracyMeters.toInt()} m\n" +
            "Timestamp: $time\n" +
            "Map: https://maps.google.com/?q=$lat,$lon"
    }

    fun shareText(): String {
        val lat = String.format(Locale.US, "%.6f", latitude)
        val lon = String.format(Locale.US, "%.6f", longitude)
        val time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(timestamp)
        return "My location: $lat, $lon\n" +
            "Accuracy: about ${accuracyMeters.toInt()} m\n" +
            "Time: $time\n" +
            "Map: https://maps.google.com/?q=$lat,$lon"
    }
}

object LocationRepository {
    fun hasPrecisePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun hasLocationPermission(context: Context): Boolean =
        hasPrecisePermission(context) ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun fetchCurrentLocation(
        context: Context,
        onResult: (Result<LocationReport>) -> Unit,
    ): CancellationTokenSource? {
        if (!hasLocationPermission(context)) {
            onResult(Result.failure(SecurityException("Location permission has not been granted.")))
            return null
        }

        val locationManager = context.getSystemService(LocationManager::class.java)
        if (locationManager == null || !LocationManagerCompat.isLocationEnabled(locationManager)) {
            onResult(Result.failure(IllegalStateException("Device location is turned off. Enable Location in Android settings and try again.")))
            return null
        }

        val priority = if (hasPrecisePermission(context)) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
        val request = CurrentLocationRequest.Builder()
            .setPriority(priority)
            .setMaxUpdateAgeMillis(0)
            .setDurationMillis(20_000)
            .build()
        val cancellationToken = CancellationTokenSource()
        return try {
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(request, cancellationToken.token)
                .addOnCompleteListener { task ->
                    if (cancellationToken.token.isCancellationRequested) return@addOnCompleteListener
                    if (!task.isSuccessful) {
                        onResult(Result.failure(task.exception ?: IllegalStateException("Location is unavailable.")))
                        return@addOnCompleteListener
                    }
                    val location = task.result
                    if (location == null) {
                        onResult(Result.failure(IllegalStateException("No fresh location fix was available. Check that location is enabled.")))
                        return@addOnCompleteListener
                    }
                    onResult(validateLocation(location))
                }
            cancellationToken
        } catch (exception: SecurityException) {
            cancellationToken.cancel()
            onResult(Result.failure(exception))
            null
        } catch (exception: IllegalStateException) {
            cancellationToken.cancel()
            onResult(Result.failure(exception))
            null
        }
    }

    private fun validateLocation(location: Location): Result<LocationReport> {
        val age = System.currentTimeMillis() - location.time
        if (!location.latitude.isFinite() || location.latitude !in -90.0..90.0) {
            return Result.failure(IllegalStateException("The location fix has an invalid latitude."))
        }
        if (!location.longitude.isFinite() || location.longitude !in -180.0..180.0) {
            return Result.failure(IllegalStateException("The location fix has an invalid longitude."))
        }
        if (!location.hasAccuracy() || !location.accuracy.isFinite() || location.accuracy < 0f) {
            return Result.failure(IllegalStateException("The location fix does not include valid accuracy information."))
        }
        if (age < 0 || age > MAX_LOCATION_AGE_MILLIS) {
            return Result.failure(IllegalStateException("The location fix is stale. Please try again with location enabled."))
        }
        return Result.success(
            LocationReport(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracyMeters = location.accuracy,
                timestamp = Instant.ofEpochMilli(location.time),
            ),
        )
    }

    private const val MAX_LOCATION_AGE_MILLIS = 60_000L
}
