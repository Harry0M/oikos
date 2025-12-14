package com.theblankstate.epmanager.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Get the current location of the device.
     * Tries to get the last known location first (faster), then fetches current location.
     * Returns null if permissions are missing or location cannot be retrieved.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        return try {
            // Try to get last known location first as it's immediate
            val lastLocation = fusedLocationClient.lastLocation.await()
            
            // If last location is recent (e.g. within 15 mins), return it
            // Otherwise, or if null, try to get current location
            if (lastLocation != null && System.currentTimeMillis() - lastLocation.time < 15 * 60 * 1000) {
                lastLocation
            } else {
                // Request a fresh location update
                // Note: accurate location might be slow or fail indoors
                val cancellationTokenSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                ).await() ?: lastLocation // Fallback to last location if current fails
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get a human-readable address name from latitude and longitude.
     */
    suspend fun getLocationName(location: Location): String? {
        if (!Geocoder.isPresent()) return null

        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            // In Android 13+ (API 33), there's a suspend version, but for compatibility we use blocking in IO context
            // Since we are likely calling this from a coroutine, we can suppress the deprecation or handle it
            // For simplicity in this helper, we'll rely on the older API wrapped in try-catch
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                // Try to get the most relevant name (feature name, sub-locality, or locality)
                address.featureName ?: address.subLocality ?: address.locality
            } else {
                null
            }
        } catch (e: Exception) {
            // Geocoder can fail with IOException if network is unavailable
            e.printStackTrace()
            null
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
