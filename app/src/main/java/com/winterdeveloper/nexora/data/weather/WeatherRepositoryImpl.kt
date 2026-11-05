package com.winterdeveloper.nexora.data.weather

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.winterdeveloper.nexora.domain.model.CityResult
import com.winterdeveloper.nexora.domain.model.WeatherCondition
import com.winterdeveloper.nexora.domain.model.WeatherReport
import com.winterdeveloper.nexora.domain.repository.SettingsRepository
import com.winterdeveloper.nexora.domain.repository.WeatherRepository
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val Context.weatherDataStore: DataStore<Preferences> by preferencesDataStore(name = "nexora_weather")

/**
 * Real weather from Open-Meteo (https://open-meteo.com): free, no API key, no account.
 * The user picks a city by name, so the launcher never asks for location permission.
 * Only the city's coordinates are sent; nothing identifying the user.
 */
class WeatherRepositoryImpl(
    context: Context,
    private val settings: SettingsRepository,
) : WeatherRepository {

    private val store = context.applicationContext.weatherDataStore
    private val mutex = Mutex()

    private object Keys {
        val city = stringPreferencesKey("city")
        val temp = doublePreferencesKey("temp_c")
        val code = intPreferencesKey("code")
        val isDay = booleanPreferencesKey("is_day")
        val fetchedAt = longPreferencesKey("fetched_at")
        val lat = doublePreferencesKey("lat")
        val lon = doublePreferencesKey("lon")
    }

    override val report: Flow<WeatherReport?> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p ->
            val city = p[Keys.city] ?: return@map null
            val temp = p[Keys.temp] ?: return@map null
            WeatherReport(
                cityName = city,
                temperatureC = temp,
                condition = WeatherCondition.fromWmoCode(p[Keys.code] ?: -1),
                isDay = p[Keys.isDay] ?: true,
                fetchedAtMillis = p[Keys.fetchedAt] ?: 0L,
            )
        }

    override suspend fun searchCity(query: String): Result<List<CityResult>> = withContext(Dispatchers.IO) {
        runCatchingIo {
            val q = URLEncoder.encode(query.trim(), "UTF-8")
            val lang = Locale.getDefault().language.ifBlank { "en" }
            val json = httpGet("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&language=$lang&format=json")
            val results = JSONObject(json).optJSONArray("results") ?: return@runCatchingIo emptyList()
            (0 until results.length()).map { i ->
                val o = results.getJSONObject(i)
                CityResult(
                    name = o.getString("name"),
                    region = o.optString("admin1").takeIf { it.isNotBlank() },
                    country = o.optString("country").takeIf { it.isNotBlank() },
                    latitude = o.getDouble("latitude"),
                    longitude = o.getDouble("longitude"),
                )
            }
        }
    }

    override suspend fun refresh(force: Boolean): Result<WeatherReport?> = mutex.withLock {
        val weather = settings.settings.first().weather
        if (!weather.enabled || !weather.hasLocation) return@withLock Result.success(null)
        val lat = weather.latitude ?: return@withLock Result.success(null)
        val lon = weather.longitude ?: return@withLock Result.success(null)

        val cached = store.data.first()
        val sameLocation = cached[Keys.lat] == lat && cached[Keys.lon] == lon
        val age = System.currentTimeMillis() - (cached[Keys.fetchedAt] ?: 0L)
        if (!force && sameLocation && age in 0 until MAX_AGE_MS) {
            return@withLock Result.success(report.first())
        }

        withContext(Dispatchers.IO) {
            runCatchingIo {
                val url = String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code,is_day&timezone=auto",
                    lat, lon,
                )
                val current = JSONObject(httpGet(url)).getJSONObject("current")
                val reading = WeatherReport(
                    cityName = weather.cityName ?: "",
                    temperatureC = current.getDouble("temperature_2m"),
                    condition = WeatherCondition.fromWmoCode(current.optInt("weather_code", -1)),
                    isDay = current.optInt("is_day", 1) == 1,
                    fetchedAtMillis = System.currentTimeMillis(),
                )
                store.edit { p ->
                    p[Keys.city] = reading.cityName
                    p[Keys.temp] = reading.temperatureC
                    p[Keys.code] = current.optInt("weather_code", -1)
                    p[Keys.isDay] = reading.isDay
                    p[Keys.fetchedAt] = reading.fetchedAtMillis
                    p[Keys.lat] = lat
                    p[Keys.lon] = lon
                }
                reading
            }
        }
    }

    override suspend fun clear() {
        store.edit { it.clear() }
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "NEXORA-OS-Launcher")
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private inline fun <T> runCatchingIo(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private companion object {
        const val TIMEOUT_MS = 10_000
        const val MAX_AGE_MS = 30L * 60L * 1000L
    }
}
