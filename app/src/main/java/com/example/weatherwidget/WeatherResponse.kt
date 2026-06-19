package com.example.weatherwidget

import com.google.gson.annotations.SerializedName

// Domain models (used by Widget)
data class WeatherResponse(
    val current: Current,
    val daily: DailyWeather
)

data class Current(
    val temperature: Double,
    val weatherCode: Int,
    val apparentTemperature: Double,
    val windSpeed: Double = 0.0,
    val humidity: Double = 0.0,
    val time: String? = null
)

data class DailyWeather(
    val maxTemp: List<Double>,
    val minTemp: List<Double>,
    val weatherCode: List<Int>
)

// Tomorrow.io DTOs
data class TomorrowResponse(
    @SerializedName("data") val data: TomorrowData
)

data class TomorrowData(
    @SerializedName("timelines") val timelines: List<TomorrowTimeline>
)

data class TomorrowTimeline(
    @SerializedName("timestep") val timestep: String, // "current" or "1d"
    @SerializedName("intervals") val intervals: List<TomorrowInterval>
)

data class TomorrowInterval(
    @SerializedName("startTime") val startTime: String,
    @SerializedName("values") val values: TomorrowValues
)

data class TomorrowValues(
    @SerializedName("temperature") val temperature: Double?,
    @SerializedName("temperatureApparent") val temperatureApparent: Double?,
    @SerializedName("temperatureMax") val temperatureMax: Double?,
    @SerializedName("temperatureMin") val temperatureMin: Double?,
    @SerializedName("weatherCode") val weatherCode: Int?,
    @SerializedName("windSpeed") val windSpeed: Double?,
    @SerializedName("humidity") val humidity: Double?
)
