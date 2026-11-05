package com.winterdeveloper.nexora.core.designsystem

import androidx.annotation.StringRes
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winterdeveloper.nexora.R

/** A NEXORA colour theme. The background stays black/navy for OLED; accents change. */
@Immutable
data class NexoraPalette(
    val id: String,
    @StringRes val nameRes: Int,
    val accent: Color,
    val secondary: Color,
    val navy: Color = Color(0xFF070B1A),
    val black: Color = Color(0xFF000000),
)

object NexoraPalettes {
    val NeonBlue = NexoraPalette("neon_blue", R.string.theme_neon_blue, Color(0xFF00B8FF), Color(0xFF8A3DFF))
    val ElectricPurple = NexoraPalette("electric_purple", R.string.theme_electric_purple, Color(0xFF9D4DFF), Color(0xFF00B8FF))
    val CyberMint = NexoraPalette("cyber_mint", R.string.theme_cyber_mint, Color(0xFF00F0C0), Color(0xFF2D7BFF))
    val PlasmaPink = NexoraPalette("plasma_pink", R.string.theme_plasma_pink, Color(0xFFFF3D9A), Color(0xFF8A3DFF))
    val ArcticWhite = NexoraPalette("arctic_white", R.string.theme_arctic_white, Color(0xFFE6F2FF), Color(0xFF00B8FF))

    val all = listOf(NeonBlue, ElectricPurple, CyberMint, PlasmaPink, ArcticWhite)

    fun byId(id: String): NexoraPalette = all.firstOrNull { it.id == id } ?: NeonBlue
}

val LocalPalette = staticCompositionLocalOf { NexoraPalettes.NeonBlue }
val LocalAnimationsEnabled = staticCompositionLocalOf { true }

/** Use for every UI animation so the "animations" switch is respected everywhere. */
@Composable
fun <T> nexoraTween(durationMillis: Int = 280): FiniteAnimationSpec<T> =
    if (LocalAnimationsEnabled.current) tween(durationMillis) else snap()

private val NexoraTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Thin, letterSpacing = (-1).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Light, letterSpacing = 0.5.sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
        labelSmall = base.labelSmall.copy(letterSpacing = 1.2.sp),
    )
}

@Composable
fun NexoraTheme(
    palette: NexoraPalette = NexoraPalettes.NeonBlue,
    animationsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scheme = darkColorScheme(
        primary = palette.accent,
        onPrimary = Color.Black,
        secondary = palette.secondary,
        onSecondary = Color.White,
        tertiary = palette.secondary,
        background = palette.black,
        onBackground = Color(0xFFE8EEFF),
        surface = palette.navy,
        onSurface = Color(0xFFE8EEFF),
        surfaceVariant = Color(0xFF111833),
        onSurfaceVariant = Color(0xFFAAB4D4),
        surfaceContainer = Color(0xFF0B1124),
        surfaceContainerHigh = Color(0xFF111833),
        surfaceContainerHighest = Color(0xFF172042),
        surfaceContainerLow = Color(0xFF080D1D),
        outline = Color(0xFF2A3560),
        outlineVariant = Color(0xFF1B2446),
    )
    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalAnimationsEnabled provides animationsEnabled,
    ) {
        MaterialTheme(colorScheme = scheme, typography = NexoraTypography, content = content)
    }
}

/** Light glass surface: translucent fill and a neon gradient hairline. */
fun Modifier.glass(shape: Shape, palette: NexoraPalette, strong: Boolean = false): Modifier =
    this
        .background(Color(0xFF0A1024).copy(alpha = if (strong) 0.82f else 0.55f), shape)
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    palette.accent.copy(alpha = 0.55f),
                    Color.White.copy(alpha = 0.06f),
                    palette.secondary.copy(alpha = 0.40f),
                )
            ),
            shape = shape,
        )

/** Soft neon text glow used by headings and the clock. */
fun neonGlow(color: Color, radius: Float = 18f) = TextStyle(
    shadow = androidx.compose.ui.graphics.Shadow(color = color.copy(alpha = 0.75f), blurRadius = radius)
)
