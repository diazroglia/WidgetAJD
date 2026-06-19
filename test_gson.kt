import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class TomorrowResponse(
    @SerializedName("data") val data: TomorrowData
)

data class TomorrowData(
    @SerializedName("timelines") val timelines: List<TomorrowTimeline>
)

data class TomorrowTimeline(
    @SerializedName("timestep") val timestep: String,
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
    @SerializedName("weatherCode") val weatherCode: Int?
)

data class WeatherResponse(
    val current: Current,
    val daily: DailyWeather
)

data class Current(
    val temperature: Double,
    val weatherCode: Int,
    val apparentTemperature: Double,
    val time: String? = null
)

data class DailyWeather(
    val maxTemp: List<Double>,
    val minTemp: List<Double>,
    val weatherCode: List<Int>
)

fun main() {
    val json = """{"data":{"timelines":[{"timestep":"1d","endTime":"2026-05-21T09:00:00Z","startTime":"2026-05-16T09:00:00Z","intervals":[{"startTime":"2026-05-16T09:00:00Z","values":{"temperature":15.5,"temperatureApparent":15.5,"temperatureMax":15.5,"temperatureMin":8.8,"weatherCode":1001}},{"startTime":"2026-05-17T09:00:00Z","values":{"temperature":12.69,"temperatureApparent":12.7,"temperatureMax":12.69,"temperatureMin":10.88,"weatherCode":1000}}]},{"timestep":"current","endTime":"2026-05-16T16:50:00Z","startTime":"2026-05-16T16:50:00Z","intervals":[{"startTime":"2026-05-16T16:50:00Z","values":{"temperature":14.68,"temperatureApparent":14.7,"temperatureMax":14.68,"temperatureMin":14.68,"weatherCode":4200}}]}]}}"""
    val gson = Gson()
    val tomorrowResponse = gson.fromJson(json, TomorrowResponse::class.java)
    
    val currentTimeline = tomorrowResponse.data.timelines.find { it.timestep == "current" }
    val dailyTimeline = tomorrowResponse.data.timelines.find { it.timestep == "1d" }

    val currentValues = currentTimeline?.intervals?.firstOrNull()?.values
    val dailyIntervals = dailyTimeline?.intervals ?: emptyList()

    val current = Current(
        temperature = currentValues?.temperature ?: 0.0,
        weatherCode = currentValues?.weatherCode ?: 0,
        apparentTemperature = currentValues?.temperatureApparent ?: 0.0,
        time = currentTimeline?.intervals?.firstOrNull()?.startTime
    )

    val daily = DailyWeather(
        maxTemp = dailyIntervals.map { it.values.temperatureMax ?: 0.0 },
        minTemp = dailyIntervals.map { it.values.temperatureMin ?: 0.0 },
        weatherCode = dailyIntervals.map { it.values.weatherCode ?: 0 }
    )

    val weatherResponse = WeatherResponse(current = current, daily = daily)
    println(weatherResponse)
    
    val f1 = "Dom\n☀️\n${weatherResponse.daily.minTemp.getOrNull(1)?.toInt() ?: 0}°/${weatherResponse.daily.maxTemp.getOrNull(1)?.toInt() ?: 0}°"
    println("f1 = " + f1)
}
