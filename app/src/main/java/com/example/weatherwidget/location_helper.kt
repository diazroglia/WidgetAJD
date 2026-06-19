package com.example.weatherwidget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object LocationHelper {
    
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
        // Check if we have location permissions
        if (!hasLocationPermission(context)) {
            return null
        }

        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        return suspendCancellableCoroutine { continuation ->
            try {
                val cancellationTokenSource = CancellationTokenSource()
                
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        continuation.resume(Pair(location.latitude, location.longitude))
                    } else {
                        // If current location fails, try last known location
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                            if (lastLocation != null) {
                                continuation.resume(Pair(lastLocation.latitude, lastLocation.longitude))
                            } else {
                                continuation.resume(null)
                            }
                        }.addOnFailureListener {
                            continuation.resume(null)
                        }
                    }
                }.addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }

                continuation.invokeOnCancellation {
                    cancellationTokenSource.cancel()
                }
            } catch (e: SecurityException) {
                continuation.resume(null)
            }
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Save location to SharedPreferences for widget use
    fun saveLocation(context: Context, latitude: Double, longitude: Double, cityName: String) {
        val prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("latitude", latitude.toFloat())
            putFloat("longitude", longitude.toFloat())
            putString("city_name", cityName)
            apply()
        }
    }

    // Get saved location from SharedPreferences
    fun getSavedLocation(context: Context): Triple<Double, Double, String>? {
        val prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        
        try {
            if (!prefs.contains("latitude") || !prefs.contains("longitude")) {
                return null
            }
            
            val lat = prefs.getFloat("latitude", 0.0f).toDouble()
            val lon = prefs.getFloat("longitude", 0.0f).toDouble()
            val city = prefs.getString("city_name", null)

            return if (city != null) {
                Triple(lat, lon, city)
            } else {
                null
            }
        } catch (e: ClassCastException) {
            // Legacy data might be String, clear it so it gets refreshed
            prefs.edit().clear().apply()
            return null
        } catch (e: Exception) {
            return null
        }
    }

    // Get city name from coordinates using Geocoder
    fun getCityName(context: Context, latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = android.location.Geocoder(context, java.util.Locale("es", "ES"))
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                // Try to get the most specific location name available
                address.locality ?: address.subAdminArea ?: address.adminArea ?: address.countryName
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationHelper", "Error getting city name", e)
            null
        }
    }
}