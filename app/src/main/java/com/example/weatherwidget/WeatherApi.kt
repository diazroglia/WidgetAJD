package com.example.weatherwidget

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v4/timelines")
    suspend fun getWeather(
        @Query("location") location: String,
        @Query("apikey") apikey: String,
        @Query("fields") fields: String,
        @Query("timesteps") timesteps: String,
        @Query("units") units: String = "metric"
    ): TomorrowResponse
}
