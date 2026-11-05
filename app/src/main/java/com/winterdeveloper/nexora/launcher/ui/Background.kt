package com.winterdeveloper.nexora.launcher.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.winterdeveloper.nexora.core.designsystem.LocalAnimationsEnabled
import com.winterdeveloper.nexora.core.designsystem.LocalPalette
import com.winterdeveloper.nexora.domain.model.Appearance
import com.winterdeveloper.nexora.domain.model.BackgroundMode

/**
 * Draws what sits behind the home screen.
 * SYSTEM_WALLPAPER: the window is transparent (windowShowWallpaper) so the real wallpaper,
 * including live wallpapers, shows through; only a dim layer is drawn for readability.
 */
@Composable
fun LauncherBackground(appearance: Appearance, modifier: Modifier = Modifier) {
    when (appearance.background) {
        BackgroundMode.SYSTEM_WALLPAPER -> Box(
            modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = appearance.wallpaperDim))
        )
        BackgroundMode.OLED_BLACK -> Box(modifier.fillMaxSize().background(Color.Black))
        BackgroundMode.NEXORA_GRADIENT -> NexoraGradient(modifier)
    }
}

@Composable
private fun NexoraGradient(modifier: Modifier) {
    val palette = LocalPalette.current
    val phase = if (LocalAnimationsEnabled.current) {
        val transition = rememberInfiniteTransition(label = "bg")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Reverse),
            label = "bgPhase",
        )
        p
    } else {
        0.5f
    }
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(Color.Black, palette.navy, Color.Black)))
        val r = size.maxDimension * 0.65f
        drawRect(
            Brush.radialGradient(
                colors = listOf(palette.accent.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(size.width * (0.1f + 0.2f * phase), size.height * 0.12f),
                radius = r,
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(palette.secondary.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(size.width * (0.95f - 0.2f * phase), size.height * 0.85f),
                radius = r,
            )
        )
        // Fine horizon line for the cyberpunk look.
        val y = size.height * 0.62f
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, palette.accent.copy(alpha = 0.25f), Color.Transparent)
            ),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.5f,
        )
    }
}
