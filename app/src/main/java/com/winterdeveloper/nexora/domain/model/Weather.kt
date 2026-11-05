package com.winterdeveloper.nexora.domain.model

enum class WeatherCondition {
    CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, SHOWERS, THUNDERSTORM, UNKNOWN;

    companion object {
        /** Maps a WMO weather interpretation code (as returned by Open-Meteo). */
        fun fromWmoCode(code: Int): WeatherCondition = when (code) {
            0 -> CLEAR
            1, 2 -> PARTLY_CLOUDY
            3 -> CLOUDY
            45, 48 -> FOG
            51, 53, 55, 56, 57 -> DRIZZLE
            61, 63, 65, 66, 67 -> RAIN
            71, 73, 75, 77, 85, 86 -> SNOW
            80, 81, 82 -> SHOWERS
            95, 96, 99 -> THUNDERSTORM
            else -> UNKNOWN
        }
    }
}

/** Last successful weather reading. Temperature is always stored in Celsius. */
data class WeatherReport(
    val cityName: String,
    val temperatureC: Double,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val fetchedAtMillis: Long,
)

data class CityResult(
    val name: String,
    val region: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
) {
    val displayName: String
        get() = listOfNotNull(name, region, country).distinct().joinToString(", ")
}
