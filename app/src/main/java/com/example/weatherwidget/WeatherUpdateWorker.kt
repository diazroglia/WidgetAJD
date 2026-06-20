package com.example.weatherwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Periodically refreshes the weather widget even when the system's
 * [AppWidgetManager] update cycle is throttled by battery optimization.
 *
 * It simply broadcasts the widget's own [WeatherWidget.ACTION_REFRESH] intent,
 * so all the fetching logic stays centralized in [WeatherWidget].
 */
class WeatherUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            val intent = Intent(context, WeatherWidget::class.java).apply {
                action = WeatherWidget.ACTION_REFRESH
            }
            context.sendBroadcast(intent)
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Weather update worker failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "WeatherUpdateWorker"
        private const val WORK_NAME = "weather_widget_periodic_update"

        /**
         * Enqueues a periodic work request that refreshes the widget every 30 minutes.
         * Safe to call multiple times — uses [ExistingPeriodicWorkPolicy.KEEP] so the
         * already-scheduled work is preserved.
         */
        fun schedulePeriodicUpdates(context: Context) {
            // Only schedule if there is at least one widget instance placed.
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WeatherWidget::class.java)
            )
            if (ids.isEmpty()) return

            val request = PeriodicWorkRequestBuilder<WeatherUpdateWorker>(
                30, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Cancels periodic updates — call when the last widget is removed.
         */
        fun cancelPeriodicUpdates(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
