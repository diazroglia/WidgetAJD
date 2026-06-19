package com.example.weatherwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeatherWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH || intent.action == Intent.ACTION_USER_PRESENT) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WeatherWidget::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.example.weatherwidget.ACTION_REFRESH"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_weather)

            // Update Date - Full day name
            val dateFormat = java.text.SimpleDateFormat("EEEE, dd 'de' MMMM", java.util.Locale("es", "ES"))
            val now = java.util.Date()
            val formattedDate = dateFormat.format(now).replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale("es", "ES")) else it.toString() }
            views.setTextViewText(R.id.date_text, formattedDate)

            // Setup refresh intent
            val refreshIntent = Intent(context, WeatherWidget::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent)

            // Setup click on widget to open MainActivity
            val mainIntent = Intent(context, MainActivity::class.java)
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, mainPendingIntent)

            // Update the widget immediately
            appWidgetManager.updateAppWidget(appWidgetId, views)

            // Fetch data in background
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Try to get current location first
                    val currentLocation = LocationHelper.getCurrentLocation(context)

                    val (latitude, longitude, cityName) = if (currentLocation != null) {
                        val (lat, lon) = currentLocation
                        // Get actual city name from coordinates
                        val city = LocationHelper.getCityName(context, lat, lon) ?: context.getString(R.string.my_location)
                        // Save the newly acquired location
                        LocationHelper.saveLocation(context, lat, lon, city)
                        Triple(lat, lon, city)
                    } else {
                        // If no current location, try to get saved location
                        val savedLocation = LocationHelper.getSavedLocation(context)

                        savedLocation ?: // Fallback to Montevideo if location is not available
                        Triple(-34.9011, -56.1645, context.getString(R.string.fallback_city))
                    }

                    // Fetch weather data
                    val weatherData = WeatherRepository.getCurrentWeather(latitude, longitude)
                    android.util.Log.d("WeatherWidget", "Response: $weatherData")

                    // Update UI on main thread
                    withContext(Dispatchers.Main) {
                        views.setTextViewText(R.id.location_text, cityName)
                        views.setTextViewText(
                            R.id.temperature_text,
                            context.getString(R.string.temp_format, weatherData.current.temperature.toInt())
                        )
                        views.setTextViewText(
                            R.id.weather_icon_text,
                            getWeatherEmoji(weatherData.current.weatherCode)
                        )
                        val feelsLike = weatherData.current.apparentTemperature.toInt()
                        views.setTextViewText(
                            R.id.feels_like_text,
                            context.getString(R.string.feels_like_format, feelsLike)
                        )
                        views.setTextViewText(
                            R.id.condition_text,
                            getWeatherDescription(context, weatherData.current.weatherCode)
                        )

                        // Set min/max temperatures from daily data
                        if (weatherData.daily.minTemp.isNotEmpty() && weatherData.daily.maxTemp.isNotEmpty()) {
                            val minTemp = weatherData.daily.minTemp[0].toInt()
                            val maxTemp = weatherData.daily.maxTemp[0].toInt()
                            
                            views.setTextViewText(R.id.min_temp_text, context.getString(R.string.min_temp_format, minTemp))
                            views.setTextViewText(R.id.max_temp_text, context.getString(R.string.max_temp_format, maxTemp))
                            
                            val daysArr = arrayOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")
                            val cal = java.util.Calendar.getInstance()
                            
                            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                            val d1Name = daysArr[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
                            val emoji1 = getWeatherEmoji(weatherData.daily.weatherCode.getOrNull(1) ?: 0)
                            val f1 = "$d1Name\n$emoji1\n${weatherData.daily.minTemp.getOrNull(1)?.toInt() ?: 0}°/${weatherData.daily.maxTemp.getOrNull(1)?.toInt() ?: 0}°"
                            
                            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                            val d2Name = daysArr[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
                            val emoji2 = getWeatherEmoji(weatherData.daily.weatherCode.getOrNull(2) ?: 0)
                            val f2 = "$d2Name\n$emoji2\n${weatherData.daily.minTemp.getOrNull(2)?.toInt() ?: 0}°/${weatherData.daily.maxTemp.getOrNull(2)?.toInt() ?: 0}°"
                            
                            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                            val d3Name = daysArr[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
                            val emoji3 = getWeatherEmoji(weatherData.daily.weatherCode.getOrNull(3) ?: 0)
                            val f3 = "$d3Name\n$emoji3\n${weatherData.daily.minTemp.getOrNull(3)?.toInt() ?: 0}°/${weatherData.daily.maxTemp.getOrNull(3)?.toInt() ?: 0}°"

                            views.setTextViewText(R.id.forecast_day1_text, f1)
                            views.setTextViewText(R.id.forecast_day2_text, f2)
                            views.setTextViewText(R.id.forecast_day3_text, f3)
                            
                            // Save to cache for offline use
                            WeatherCache.saveWeather(
                                context,
                                weatherData.current.temperature.toInt(),
                                weatherData.current.weatherCode,
                                minTemp,
                                maxTemp,
                                feelsLike,
                                cityName,
                                f1,
                                f2,
                                f3
                            )
                        }

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WeatherWidget", "Error fetching weather: ${e.javaClass.simpleName}: ${e.message}", e)
                    
                    // Try to load cached data
                    withContext(Dispatchers.Main) {
                        val cachedWeather = WeatherCache.getCachedWeather(context)
                        
                        if (cachedWeather != null) {
                            // Show cached data
                            views.setTextViewText(R.id.location_text, cachedWeather.cityName)
                            views.setTextViewText(R.id.temperature_text, context.getString(R.string.temp_format, cachedWeather.temperature))
                            views.setTextViewText(
                                R.id.weather_icon_text,
                                getWeatherEmoji(cachedWeather.weatherCode)
                            )
                            views.setTextViewText(R.id.feels_like_text, context.getString(R.string.feels_like_format, cachedWeather.feelsLike))
                            views.setTextViewText(
                                R.id.condition_text,
                                getWeatherDescription(context, cachedWeather.weatherCode)
                            )
                            views.setTextViewText(R.id.min_temp_text, context.getString(R.string.min_temp_format, cachedWeather.minTemp))
                            views.setTextViewText(R.id.max_temp_text, context.getString(R.string.max_temp_format, cachedWeather.maxTemp))
                            views.setTextViewText(R.id.forecast_day1_text, cachedWeather.f1)
                            views.setTextViewText(R.id.forecast_day2_text, cachedWeather.f2)
                            views.setTextViewText(R.id.forecast_day3_text, cachedWeather.f3)
                        } else {
                            // No cached data available
                            views.setTextViewText(R.id.location_text, context.getString(R.string.widget_error))
                            val errorMsg = when {
                                e is java.net.UnknownHostException -> context.getString(R.string.no_internet)
                                e.message?.contains("Unable to resolve host") == true -> context.getString(R.string.no_internet)
                                else -> context.getString(R.string.open_app)
                            }
                            views.setTextViewText(R.id.condition_text, errorMsg)
                            views.setTextViewText(R.id.weather_icon_text, "❌")
                            views.setTextViewText(R.id.temperature_text, "--")
                            views.setTextViewText(R.id.feels_like_text, "")
                            views.setTextViewText(R.id.min_temp_text, "Min: --")
                            views.setTextViewText(R.id.max_temp_text, "Max: --")
                            views.setTextViewText(R.id.forecast_day1_text, "")
                            views.setTextViewText(R.id.forecast_day2_text, "")
                            views.setTextViewText(R.id.forecast_day3_text, "")
                        }
                        
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }

        private fun getWeatherDescription(context: Context, code: Int): String {
            return when (code) {
                1000 -> context.getString(R.string.weather_clear_sky)
                1100 -> context.getString(R.string.weather_mostly_clear)
                1101 -> context.getString(R.string.weather_partly_cloudy)
                1102 -> context.getString(R.string.weather_mostly_cloudy)
                1001 -> context.getString(R.string.weather_cloudy)
                2000, 2100 -> context.getString(R.string.weather_fog)
                4000 -> context.getString(R.string.weather_drizzle)
                4001, 4200 -> context.getString(R.string.weather_rain)
                4201 -> context.getString(R.string.weather_heavy_rain)
                5000, 5001, 5100 -> context.getString(R.string.weather_snow)
                5101 -> context.getString(R.string.weather_heavy_snow)
                6000, 6001, 6200, 6201 -> context.getString(R.string.weather_freezing_rain)
                7000, 7101, 7102 -> context.getString(R.string.weather_hail)
                8000 -> context.getString(R.string.weather_thunderstorm)
                else -> context.getString(R.string.weather_unknown)
            }
        }

        private fun getWeatherEmoji(code: Int): String {
            return when (code) {
                1000 -> "☀️" // Cielo despejado
                1100 -> "🌤️" // Mayormente despejado
                1101 -> "⛅" // Parcialmente nublado
                1102 -> "🌥️" // Mayormente nublado
                1001 -> "☁️" // Nublado
                2000, 2100 -> "🌫️" // Niebla
                4000 -> "🌧️" // Llovizna
                4001, 4200 -> "🌧️" // Lluvia
                4201 -> "⛈️" // Lluvia fuerte
                5000, 5001, 5100 -> "❄️" // Nieve
                5101 -> "❄️" // Nieve fuerte
                6000, 6001, 6200, 6201 -> "🌨️" // Lluvia helada
                7000, 7101, 7102 -> "🌨️" // Granizo
                8000 -> "⛈️" // Tormenta
                else -> "🌡️" // Desconocido
            }
        }
    }
}