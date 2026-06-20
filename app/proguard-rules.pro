# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep domain models used by Gson
-keep class com.example.weatherwidget.TomorrowResponse { *; }
-keep class com.example.weatherwidget.TomorrowData { *; }
-keep class com.example.weatherwidget.TomorrowTimeline { *; }
-keep class com.example.weatherwidget.TomorrowInterval { *; }
-keep class com.example.weatherwidget.TomorrowValues { *; }
-keep class com.example.weatherwidget.WeatherResponse { *; }
-keep class com.example.weatherwidget.Current { *; }
-keep class com.example.weatherwidget.DailyWeather { *; }
-keep class com.example.weatherwidget.WeatherCache$CachedWeather { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
