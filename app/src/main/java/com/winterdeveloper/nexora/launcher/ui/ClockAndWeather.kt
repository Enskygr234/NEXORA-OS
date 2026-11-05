package com.winterdeveloper.nexora.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winterdeveloper.nexora.R
import com.winterdeveloper.nexora.core.designsystem.LocalPalette
import com.winterdeveloper.nexora.core.designsystem.glass
import com.winterdeveloper.nexora.core.designsystem.neonGlow
import com.winterdeveloper.nexora.domain.model.Appearance
import com.winterdeveloper.nexora.domain.model.ClockStyle
import com.winterdeveloper.nexora.domain.model.TemperatureUnit
import com.winterdeveloper.nexora.domain.model.WeatherCondition
import com.winterdeveloper.nexora.domain.model.WeatherReport
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/** Current time, re-emitted exactly on each second or minute boundary. */
@Composable
private fun rememberNow(withSeconds: Boolean): LocalDateTime {
    val now by produceState(LocalDateTime.now(), withSeconds) {
        while (true) {
            val millis = System.currentTimeMillis()
            val step = if (withSeconds) 1_000L else 60_000L
            delay(step - millis % step + 5)
            value = LocalDateTime.now()
        }
    }
    return now
}

@Composable
fun HomeClock(
    appearance: Appearance,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val now = rememberNow(appearance.showSeconds)
    val locale = Locale.getDefault()
    val timePattern = buildString {
        append(if (appearance.use24Hour) "HH:mm" else "h:mm")
        if (appearance.showSeconds) append(":ss")
    }
    val time = now.format(DateTimeFormatter.ofPattern(timePattern, locale))
    val amPm = if (appearance.use24Hour) "" else now.format(DateTimeFormatter.ofPattern("a", locale))
    val date = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    val alignStart = appearance.clockStyle == ClockStyle.MINIMAL
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignStart) Alignment.Start else Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.clickable(onClick = onClockClick),
        ) {
            when (appearance.clockStyle) {
                ClockStyle.LARGE -> Text(
                    text = time,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 86.sp, lineHeight = 90.sp),
                    color = Color.White,
                )
                ClockStyle.NEON -> Text(
                    text = time,
                    style = MaterialTheme.typography.displayLarge
                        .copy(
                            fontSize = 80.sp,
                            lineHeight = 86.sp,
                            fontWeight = FontWeight.ExtraLight,
                            brush = Brush.linearGradient(listOf(palette.accent, palette.secondary)),
                        )
                        .merge(neonGlow(palette.accent, 28f)),
                )
                ClockStyle.MINIMAL -> Text(
                    text = time,
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Light),
                    color = Color.White,
                )
            }
            if (amPm.isNotEmpty()) {
                Text(
                    text = amPm,
                    modifier = Modifier.padding(start = 6.dp, bottom = 14.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.accent,
                )
            }
        }
        Text(
            text = date,
            modifier = Modifier.clickable(onClick = onDateClick).padding(vertical = 2.dp),
            style = MaterialTheme.typography.titleMedium.merge(TextStyle(letterSpacing = 1.5.sp)),
            color = if (appearance.clockStyle == ClockStyle.NEON) palette.accent else Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
fun WeatherChip(
    report: WeatherReport?,
    unit: TemperatureUnit,
    hasCity: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .glass(shape, palette)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (!hasCity) {
            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.weather_set_city), style = MaterialTheme.typography.labelLarge, color = Color.White)
            return@Row
        }
        if (report == null) {
            Icon(Icons.Rounded.Cloud, contentDescription = null, tint = palette.accent.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.weather_loading), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
            return@Row
        }
        Icon(
            imageVector = weatherIcon(report.condition, report.isDay),
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatTemperature(report.temperatureC, unit),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${stringResource(weatherLabel(report.condition))} · ${report.cityName}",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.75f),
            maxLines = 1,
        )
    }
}

fun formatTemperature(celsius: Double, unit: TemperatureUnit): String = when (unit) {
    TemperatureUnit.CELSIUS -> "${celsius.roundToInt()}°C"
    TemperatureUnit.FAHRENHEIT -> "${(celsius * 9.0 / 5.0 + 32.0).roundToInt()}°F"
}

private fun weatherIcon(condition: WeatherCondition, isDay: Boolean): ImageVector = when (condition) {
    WeatherCondition.CLEAR -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.Bedtime
    WeatherCondition.PARTLY_CLOUDY -> Icons.Rounded.CloudQueue
    WeatherCondition.CLOUDY -> Icons.Rounded.Cloud
    WeatherCondition.FOG -> Icons.Rounded.Dehaze
    WeatherCondition.DRIZZLE -> Icons.Rounded.Grain
    WeatherCondition.RAIN, WeatherCondition.SHOWERS -> Icons.Rounded.WaterDrop
    WeatherCondition.SNOW -> Icons.Rounded.AcUnit
    WeatherCondition.THUNDERSTORM -> Icons.Rounded.Thunderstorm
    WeatherCondition.UNKNOWN -> Icons.Rounded.Cloud
}

private fun weatherLabel(condition: WeatherCondition): Int = when (condition) {
    WeatherCondition.CLEAR -> R.string.weather_clear
    WeatherCondition.PARTLY_CLOUDY -> R.string.weather_partly_cloudy
    WeatherCondition.CLOUDY -> R.string.weather_cloudy
    WeatherCondition.FOG -> R.string.weather_fog
    WeatherCondition.DRIZZLE -> R.string.weather_drizzle
    WeatherCondition.RAIN -> R.string.weather_rain
    WeatherCondition.SHOWERS -> R.string.weather_showers
    WeatherCondition.SNOW -> R.string.weather_snow
    WeatherCondition.THUNDERSTORM -> R.string.weather_thunderstorm
    WeatherCondition.UNKNOWN -> R.string.weather_unknown
}
