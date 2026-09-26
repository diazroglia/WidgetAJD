package com.example.weatherwidget

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * Refreshes the widget when the user moves away from the last known location.
 *
 * Periodic WorkManager jobs are deferred for hours by Doze and Samsung's app freezer, so
 * a city change could go unnoticed until a manual refresh. A geofence exit is delivered by
 * Play Services even when the app process is dead, which makes it a reliable trigger.
 */
object LocationChangeGeofence {

    private const val TAG = "LocationChangeGeofence"
    private const val GEOFENCE_ID = "last_widget_location"
    private const val RADIUS_METERS = 2_000f
    private const val RESPONSIVENESS_MS = 5 * 60 * 1000

    /**
     * Centers the geofence on the given point, replacing the previous one.
     * Requires fine + background location; silently skipped otherwise.
     */
    fun register(context: Context, latitude: Double, longitude: Double) {
        val appContext = context.applicationContext
        if (!hasRequiredPermissions(appContext)) return

        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(latitude, longitude, RADIUS_METERS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .setNotificationResponsiveness(RESPONSIVENESS_MS)
            .build()

        // No initial trigger: registering from a stale location must not fire an
        // immediate exit, which would loop refresh -> stale location -> exit.
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofence(geofence)
            .build()

        try {
            LocationServices.getGeofencingClient(appContext)
                .addGeofences(request, pendingIntent(appContext))
                .addOnFailureListener { e ->
                    android.util.Log.w(TAG, "Could not register geofence", e)
                }
        } catch (e: SecurityException) {
            android.util.Log.w(TAG, "Missing permission for geofence", e)
        }
    }

    private fun hasRequiredPermissions(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        return fine && background
    }

    // Must be mutable: Play Services adds the geofencing event extras to the intent.
    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceExitReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}

class GeofenceExitReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            android.util.Log.w("GeofenceExitReceiver", "Geofence error: ${event.errorCode}")
            return
        }
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT) return

        // The worker fetches the new location and re-centers the geofence there.
        WeatherUpdateWorker.enqueueImmediateUpdate(context, replaceExisting = true)
    }
}
