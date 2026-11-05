package com.winterdeveloper.nexora.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.winterdeveloper.nexora.domain.model.Appearance
import com.winterdeveloper.nexora.domain.model.LauncherSettings
import com.winterdeveloper.nexora.domain.model.WeatherSettings
import com.winterdeveloper.nexora.domain.repository.SettingsRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "nexora_settings")

internal inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T): T =
    value?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default

class SettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    private object Keys {
        val themeId = stringPreferencesKey("theme_id")
        val background = stringPreferencesKey("background")
        val wallpaperDim = floatPreferencesKey("wallpaper_dim")
        val iconSize = intPreferencesKey("icon_size_dp")
        val iconShape = stringPreferencesKey("icon_shape")
        val gridColumns = intPreferencesKey("grid_columns")
        val showLabels = booleanPreferencesKey("show_labels")
        val animations = booleanPreferencesKey("animations")
        val clockStyle = stringPreferencesKey("clock_style")
        val use24Hour = booleanPreferencesKey("use_24_hour")
        val showSeconds = booleanPreferencesKey("show_seconds")
        val weatherEnabled = booleanPreferencesKey("weather_enabled")
        val weatherCity = stringPreferencesKey("weather_city")
        val weatherLat = doublePreferencesKey("weather_lat")
        val weatherLon = doublePreferencesKey("weather_lon")
        val weatherUnit = stringPreferencesKey("weather_unit")
        val badges = booleanPreferencesKey("badges_enabled")
    }

    override val settings: Flow<LauncherSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }
        .distinctUntilChanged()

    override suspend fun update(transform: (LauncherSettings) -> LauncherSettings) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs.write(updated.copy(appearance = updated.appearance.sanitized()))
        }
    }

    private fun Preferences.toSettings(): LauncherSettings {
        val d = Appearance()
        val w = WeatherSettings()
        return LauncherSettings(
            appearance = Appearance(
                themeId = this[Keys.themeId] ?: d.themeId,
                background = enumOrDefault(this[Keys.background], d.background),
                wallpaperDim = this[Keys.wallpaperDim] ?: d.wallpaperDim,
                iconSizeDp = this[Keys.iconSize] ?: d.iconSizeDp,
                iconShape = enumOrDefault(this[Keys.iconShape], d.iconShape),
                gridColumns = this[Keys.gridColumns] ?: d.gridColumns,
                showLabels = this[Keys.showLabels] ?: d.showLabels,
                animationsEnabled = this[Keys.animations] ?: d.animationsEnabled,
                clockStyle = enumOrDefault(this[Keys.clockStyle], d.clockStyle),
                use24Hour = this[Keys.use24Hour] ?: d.use24Hour,
                showSeconds = this[Keys.showSeconds] ?: d.showSeconds,
            ).sanitized(),
            weather = WeatherSettings(
                enabled = this[Keys.weatherEnabled] ?: w.enabled,
                cityName = this[Keys.weatherCity],
                latitude = this[Keys.weatherLat],
                longitude = this[Keys.weatherLon],
                unit = enumOrDefault(this[Keys.weatherUnit], w.unit),
            ),
            badgesEnabled = this[Keys.badges] ?: false,
        )
    }

    private fun MutablePreferences.write(s: LauncherSettings) {
        val a = s.appearance
        this[Keys.themeId] = a.themeId
        this[Keys.background] = a.background.name
        this[Keys.wallpaperDim] = a.wallpaperDim
        this[Keys.iconSize] = a.iconSizeDp
        this[Keys.iconShape] = a.iconShape.name
        this[Keys.gridColumns] = a.gridColumns
        this[Keys.showLabels] = a.showLabels
        this[Keys.animations] = a.animationsEnabled
        this[Keys.clockStyle] = a.clockStyle.name
        this[Keys.use24Hour] = a.use24Hour
        this[Keys.showSeconds] = a.showSeconds

        val w = s.weather
        this[Keys.weatherEnabled] = w.enabled
        this[Keys.weatherUnit] = w.unit.name
        if (w.cityName != null && w.latitude != null && w.longitude != null) {
            this[Keys.weatherCity] = w.cityName
            this[Keys.weatherLat] = w.latitude
            this[Keys.weatherLon] = w.longitude
        } else {
            remove(Keys.weatherCity)
            remove(Keys.weatherLat)
            remove(Keys.weatherLon)
        }
        this[Keys.badges] = s.badgesEnabled
    }
}

/** JSON form of [Appearance], used by theme profiles and backups. Unknown or missing fields fall back to defaults. */
object AppearanceJson {
    fun encode(a: Appearance): org.json.JSONObject = org.json.JSONObject().apply {
        put("themeId", a.themeId)
        put("background", a.background.name)
        put("wallpaperDim", a.wallpaperDim.toDouble())
        put("iconSizeDp", a.iconSizeDp)
        put("iconShape", a.iconShape.name)
        put("gridColumns", a.gridColumns)
        put("showLabels", a.showLabels)
        put("animationsEnabled", a.animationsEnabled)
        put("clockStyle", a.clockStyle.name)
        put("use24Hour", a.use24Hour)
        put("showSeconds", a.showSeconds)
    }

    fun decode(o: org.json.JSONObject): Appearance {
        val d = Appearance()
        return Appearance(
            themeId = o.optString("themeId", d.themeId),
            background = enumOrDefault(o.optString("background"), d.background),
            wallpaperDim = o.optDouble("wallpaperDim", d.wallpaperDim.toDouble()).toFloat(),
            iconSizeDp = o.optInt("iconSizeDp", d.iconSizeDp),
            iconShape = enumOrDefault(o.optString("iconShape"), d.iconShape),
            gridColumns = o.optInt("gridColumns", d.gridColumns),
            showLabels = o.optBoolean("showLabels", d.showLabels),
            animationsEnabled = o.optBoolean("animationsEnabled", d.animationsEnabled),
            clockStyle = enumOrDefault(o.optString("clockStyle"), d.clockStyle),
            use24Hour = o.optBoolean("use24Hour", d.use24Hour),
            showSeconds = o.optBoolean("showSeconds", d.showSeconds),
        ).sanitized()
    }
}
