# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Proyecto

Widget de clima para la pantalla de inicio de Android (Kotlin, módulo único `app`). Datos de Tomorrow.io; textos de UI en español (`res/values/strings.xml`), comentarios de código en inglés. No hay tests: `app/src/test` y `app/src/androidTest` no existen.

## Build

```sh
./gradlew assembleDebug      # APK debug
./gradlew assembleRelease    # APK firmado, minify + shrinkResources (R8)
./gradlew installDebug       # instala en el dispositivo conectado
```

- `secrets.properties` (raíz, gitignored) aporta `TOMORROW_API_KEY` → `BuildConfig.TOMORROW_API_KEY`, y `keystoreStorePassword` / `keystoreKeyPassword` para la firma release. Si falta, el build compila igual pero la clave queda vacía y el widget cae en el estado de error.
- La firma release apunta a una ruta absoluta: `D:/Antigravity/WidgetAJD/keyStore/ksajd.jks`.
- Al publicar una versión, subí `versionCode`/`versionName` en `app/build.gradle.kts`: `MainActivity` los muestra en pantalla.

## Arquitectura

Flujo de actualización (todo el trabajo de red y ubicación corre en WorkManager, fuera del ciclo de vida del receiver):

1. `WeatherWidget` (`AppWidgetProvider`) recibe `APPWIDGET_UPDATE`, `MY_PACKAGE_REPLACED` o su propio `ACTION_REFRESH` (botón de refresco) y encola trabajo en `WeatherUpdateWorker`. `updatePeriodMillis="0"`: el refresco periódico es solo el trabajo único de WorkManager cada 30 min; `onDisabled` lo cancela. El desbloqueo lo escucha `WeatherWidgetApp` (receiver registrado en código).
2. `WeatherUpdateWorker.doWork` llama a `WeatherWidget.updateWidgets`, que por cada instancia:
   - pinta primero fecha/reloj y los datos de `WeatherCache` (render inmediato),
   - obtiene ubicación con `LocationHelper` (timeout 4 s) → si falla, la ubicación guardada → si no hay, Montevideo (-34.9011, -56.1645),
   - consulta `WeatherRepository` y vuelve a pintar; ante excepción, repinta desde caché o muestra el estado de error.
3. `WeatherRepository` llama a `/v4/timelines` con `timesteps=current,1d` y mapea `TomorrowResponse` → `WeatherResponse` (modelo interno). La sensación térmica **no** viene de la API: se calcula con la fórmula BOM (temperatura, humedad, viento).

`MainActivity` pide permiso de ubicación, guarda lat/lon/ciudad (el widget la usa como respaldo) y encola un refresco del widget.

Cada ubicación fresca que obtiene el widget re-centra una geocerca de 2 km (`LocationChangeGeofence`); al salir de ella, Play Services despierta `GeofenceExitReceiver` aunque el proceso esté muerto y se encola un refresco. Es el disparador confiable de "cambié de ciudad".

### Detalles no obvios

- Dos archivos de SharedPreferences: `weather_prefs` (ubicación, lat/lon como String para conservar la precisión del Double) y `weather_cache_v3` (último clima). Si cambiás la forma de la caché, subí el sufijo de versión en lugar de migrar.
- La caché guarda los pronósticos f1–f3 como texto ya formateado (`"Lun\n☀️\n12°/18°"`), no como datos crudos.
- Los pronósticos usan `\u2060` (word joiner) después de `°/` para que el rango de temperaturas no se parta en dos líneas; `preventForecastWrap` lo aplica también a cachés viejas.
- Los códigos de clima de Tomorrow.io se mapean dos veces en `WeatherWidget` (`getWeatherDescription` y `getWeatherEmoji`): un código nuevo va en ambas.
- El worker corre en segundo plano: sin `ACCESS_BACKGROUND_LOCATION` ("Permitir todo el tiempo") la ubicación vuelve `null` y el widget se queda con la ciudad guardada. Justo después de salir de la app Android todavía la permite unos segundos, así que al probar esperá ~30 s antes de disparar el refresco.
- El refresco al desbloquear (`WeatherWidgetApp`) es de mejor esfuerzo: `USER_PRESENT` no llega a receivers del manifest y, con el proceso cacheado, Android 14+ lo posterga; si el proceso murió, no hay receiver.
- En Samsung (probado en S23 Ultra), con batería "Optimizada" el congelador de Samsung (`FreecessController`/OLAF en logcat) posterga el worker periódico durante horas: se vio un hueco de 15 h sin ejecuciones. Con batería "Sin restricciones" (la app aparece en `dumpsys deviceidle whitelist`) mejora; la geocerca cubre el cambio de ciudad igual.
- El `applicationId` es `com.antigravity.weatherwidget` (distinto del namespace `com.example.weatherwidget`): usalo en `adb`/`dumpsys`. Para ver la geocerca: `dumpsys activity service com.google.android.gms/com.google.android.location.internal.GoogleLocationManagerService | grep -A1 antigravity | grep GeofenceRequest`.
- El widget usa `RemoteViews`: el layout `widget_weather.xml` solo admite vistas compatibles con RemoteViews.

## Scripts sueltos en la raíz

`Inumet.py` y `test_tomorrow.py` son sondas manuales de las APIs de INUMET y Tomorrow.io; `parse_log.py` filtra `logcat500.txt`. No forman parte del build.
