package com.example.weatherwidget

import android.content.Context
import android.content.SharedPreferences

object WeatherCache {
    private const val PREFS_NAME = "weather_cache_v3"
    private const val KEY_TEMPERATURE = "temperature"
    private const val KEY_WEATHER_CODE = "weather_code"
    private const val KEY_MIN_TEMP = "min_temp"
    private const val KEY_MAX_TEMP = "max_temp"
    private const val KEY_FEELS_LIKE = "feels_like"
    private const val KEY_CITY_NAME = "city_name"
    private const val KEY_F1 = "f1"
    private const val KEY_F2 = "f2"
    private const val KEY_F3 = "f3"
    private const val KEY_LAST_UPDATE = "last_update"

    data class CachedWeather(
        val temperature: Int,
        val weatherCode: Int,
        val minTemp: Int,
        val maxTemp: Int,
        val feelsLike: Int,
        val cityName: String,
        val f1: String,
        val f2: String,
        val f3: String,
        val lastUpdate: Long
    )

    fun saveWeather(
        context: Context,
        temperature: Int,
        weatherCode: Int,
        minTemp: Int,
        maxTemp: Int,
        feelsLike: Int,
        cityName: String,
        f1: String,
        f2: String,
        f3: String
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(KEY_TEMPERATURE, temperature)
            putInt(KEY_WEATHER_CODE, weatherCode)
            putInt(KEY_MIN_TEMP, minTemp)
            putInt(KEY_MAX_TEMP, maxTemp)
            putInt(KEY_FEELS_LIKE, feelsLike)
            putString(KEY_CITY_NAME, cityName)
            putString(KEY_F1, f1)
            putString(KEY_F2, f2)
            putString(KEY_F3, f3)
            putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            apply()
        }
    }

    fun getCachedWeather(context: Context): CachedWeather? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        if (!prefs.contains(KEY_TEMPERATURE)) {
            return null
        }

        return try {
            CachedWeather(
                temperature = prefs.getInt(KEY_TEMPERATURE, 0),
                weatherCode = prefs.getInt(KEY_WEATHER_CODE, 0),
                minTemp = prefs.getInt(KEY_MIN_TEMP, 0),
                maxTemp = prefs.getInt(KEY_MAX_TEMP, 0),
                feelsLike = prefs.getInt(KEY_FEELS_LIKE, 0),
                cityName = prefs.getString(KEY_CITY_NAME, "Ubicación") ?: "Ubicación",
                f1 = prefs.getString(KEY_F1, "") ?: "",
                f2 = prefs.getString(KEY_F2, "") ?: "",
                f3 = prefs.getString(KEY_F3, "") ?: "",
                lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun isCacheValid(context: Context, maxAgeHours: Int = 2): Boolean {
        val cached = getCachedWeather(context) ?: return false
        val ageMillis = System.currentTimeMillis() - cached.lastUpdate
        val ageHours = ageMillis / (1000 * 60 * 60)
        return ageHours < maxAgeHours
    }
}
