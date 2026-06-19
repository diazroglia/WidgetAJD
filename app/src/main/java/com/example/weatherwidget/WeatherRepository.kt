package com.example.weatherwidget

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WeatherRepository {
    private const val BASE_URL = "https://api.tomorrow.io/"
    private const val API_KEY = "bVnE3WhwiBzYwhEmqRlwWnKmlJIEZBe3"

    private val api: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherResponse {
        val location = "$lat,$lon"
        val tomorrowResponse = api.getWeather(
            location = location,
            apikey = API_KEY,
            fields = "temperature,temperatureApparent,weatherCode,temperatureMax,temperatureMin,windSpeed,humidity",
            timesteps = "current,1d"
        )

        // Parse timelines. We must find them by timestep.
        val currentTimeline = tomorrowResponse.data.timelines.find { it.timestep == "current" }
        val dailyTimeline = tomorrowResponse.data.timelines.find { it.timestep == "1d" }

        val currentValues = currentTimeline?.intervals?.firstOrNull()?.values
        val dailyIntervals = dailyTimeline?.intervals ?: emptyList()

        val temp = currentValues?.temperature ?: 0.0
        val windSpeed = currentValues?.windSpeed ?: 0.0 // in m/s (metric default)
        val humidity = currentValues?.humidity ?: 0.0 // in %

        // Water vapor pressure (e) in hPa
        val e = (humidity / 100.0) * 6.105 * Math.exp((17.27 * temp) / (237.7 + temp))
        // Apparent temperature formula (Australian Bureau of Meteorology - BOM)
        val apparentTemp = temp + 0.33 * e - 0.70 * windSpeed - 4.0

        val current = Current(
            temperature = temp,
            weatherCode = currentValues?.weatherCode ?: 0,
            apparentTemperature = apparentTemp,
            windSpeed = windSpeed,
            humidity = humidity,
            time = currentTimeline?.intervals?.firstOrNull()?.startTime
        )

        val daily = DailyWeather(
            maxTemp = dailyIntervals.map { it.values.temperatureMax ?: 0.0 },
            minTemp = dailyIntervals.map { it.values.temperatureMin ?: 0.0 },
            weatherCode = dailyIntervals.map { it.values.weatherCode ?: 0 }
        )

        return WeatherResponse(current = current, daily = daily)
    }
}
