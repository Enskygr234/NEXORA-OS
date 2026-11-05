package com.winterdeveloper.nexora.domain.model

enum class BackgroundMode { NEXORA_GRADIENT, SYSTEM_WALLPAPER, OLED_BLACK }

enum class ClockStyle { LARGE, NEON, MINIMAL }

enum class IconShape { SYSTEM, CIRCLE, SQUIRCLE, ROUNDED_SQUARE }

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

/** Everything that makes up the look of the launcher. Saved as a theme profile. */
data class Appearance(
    val themeId: String = "neon_blue",
    val background: BackgroundMode = BackgroundMode.NEXORA_GRADIENT,
    val wallpaperDim: Float = 0.25f,
    val iconSizeDp: Int = 56,
    val iconShape: IconShape = IconShape.SYSTEM,
    val gridColumns: Int = 4,
    val showLabels: Boolean = true,
    val animationsEnabled: Boolean = true,
    val clockStyle: ClockStyle = ClockStyle.LARGE,
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = false,
) {
    companion object {
        const val MIN_ICON_DP = 40
        const val MAX_ICON_DP = 72
        const val MIN_COLUMNS = 3
        const val MAX_COLUMNS = 6
    }

    /** Clamps values that may come from an old or hand-edited backup. */
    fun sanitized(): Appearance = copy(
        wallpaperDim = wallpaperDim.coerceIn(0f, 0.8f),
        iconSizeDp = iconSizeDp.coerceIn(MIN_ICON_DP, MAX_ICON_DP),
        gridColumns = gridColumns.coerceIn(MIN_COLUMNS, MAX_COLUMNS),
    )
}

data class WeatherSettings(
    val enabled: Boolean = true,
    val cityName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val unit: TemperatureUnit = TemperatureUnit.CELSIUS,
) {
    val hasLocation: Boolean get() = latitude != null && longitude != null
}

data class LauncherSettings(
    val appearance: Appearance = Appearance(),
    val weather: WeatherSettings = WeatherSettings(),
    val badgesEnabled: Boolean = false,
)

data class ThemeProfile(
    val id: Long,
    val name: String,
    val appearance: Appearance,
)
