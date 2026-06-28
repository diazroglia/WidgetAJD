package com.example.weatherwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Refreshes the weather widget from WorkManager, keeping network/location work
 * out of the widget broadcast receiver lifecycle.
 */
class WeatherUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = getWidgetIds(context, appWidgetManager)

            if (ids.isEmpty()) {
                cancelPeriodicUpdates(context)
                return Result.success()
            }

            WeatherWidget.updateWidgets(context, appWidgetManager, ids)
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Weather update worker failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "WeatherUpdateWorker"
        private const val WORK_NAME = "weather_widget_periodic_update"
        private const val IMMEDIATE_WORK_NAME = "weather_widget_immediate_update"

        fun enqueueImmediateUpdate(context: Context, replaceExisting: Boolean = false) {
            val appContext = context.applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            if (getWidgetIds(appContext, appWidgetManager).isEmpty()) return

            val request = OneTimeWorkRequestBuilder<WeatherUpdateWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(appContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                request
            )
        }

        /**
         * Enqueues a periodic work request that refreshes the widget every 30 minutes.
         * Safe to call multiple times — uses [ExistingPeriodicWorkPolicy.UPDATE] so
         * changes in this schedule are applied after an app update.
         */
        fun schedulePeriodicUpdates(context: Context) {
            val appContext = context.applicationContext
            // Only schedule if there is at least one widget instance placed.
            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            if (getWidgetIds(appContext, appWidgetManager).isEmpty()) return

            val request = PeriodicWorkRequestBuilder<WeatherUpdateWorker>(
                30, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /**
         * Cancels periodic updates — call when the last widget is removed.
         */
        fun cancelPeriodicUpdates(context: Context) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.cancelUniqueWork(WORK_NAME)
            workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
        }

        private fun getWidgetIds(
            context: Context,
            appWidgetManager: AppWidgetManager
        ): IntArray {
            return appWidgetManager.getAppWidgetIds(
                ComponentName(context, WeatherWidget::class.java)
            )
        }
    }
}
