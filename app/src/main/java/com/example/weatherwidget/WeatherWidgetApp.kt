package com.example.weatherwidget

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

class WeatherWidgetApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Best effort: since Android 8, USER_PRESENT is not delivered to manifest-declared
        // receivers, and since Android 14 it is deferred while this process is cached, so
        // it only fires promptly if the app ran recently. The periodic worker is the
        // reliable refresh path.
        // Must be EXPORTED: with NOT_EXPORTED the system's USER_PRESENT never arrives.
        ContextCompat.registerReceiver(
            this,
            UnlockReceiver(),
            IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private class UnlockReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Unlocks are frequent; throttle them to protect the Tomorrow.io rate limit.
            val lastUpdate = WeatherCache.getCachedWeather(context)?.lastUpdate ?: 0L
            if (System.currentTimeMillis() - lastUpdate < UNLOCK_MIN_INTERVAL_MS) return

            WeatherUpdateWorker.enqueueImmediateUpdate(context)
        }
    }

    companion object {
        private const val UNLOCK_MIN_INTERVAL_MS = 10 * 60 * 1000L
    }
}
