package com.winterdeveloper.nexora.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winterdeveloper.nexora.container
import com.winterdeveloper.nexora.core.designsystem.LocalPalette
import com.winterdeveloper.nexora.domain.model.IconShape
import com.winterdeveloper.nexora.domain.model.LauncherApp

@Composable
fun AppIconImage(
    app: LauncherApp,
    sizeDp: Int,
    shape: IconShape,
    modifier: Modifier = Modifier,
) {
    val loader = LocalContext.current.container.iconLoader
    val px = with(LocalDensity.current) { sizeDp.dp.roundToPx() }
    val image: ImageBitmap? by produceState(loader.cached(app.key, px, shape), app.key, px, shape) {
        value = loader.load(app.key, px, shape)
    }
    Box(modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        val bmp = image
        if (bmp != null) {
            Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size(sizeDp.dp))
        } else {
            Box(
                Modifier
                    .size((sizeDp * 0.8f).dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(30))
            )
        }
    }
}

@Composable
fun AppTile(
    app: LauncherApp,
    sizeDp: Int,
    shape: IconShape,
    showLabel: Boolean,
    badgeCount: Int,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Column(
        modifier = modifier
            .padding(vertical = 6.dp)
            .semantics { contentDescription = app.label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Box {
            AppIconImage(app, sizeDp, shape)
            if (badgeCount > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-2).dp)
                        .sizeIn(minWidth = 18.dp, minHeight = 18.dp)
                        .background(palette.accent, CircleShape)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = app.label,
                modifier = Modifier.width((sizeDp + 28).dp),
                style = MaterialTheme.typography.labelMedium.merge(
                    TextStyle(shadow = Shadow(Color.Black.copy(alpha = 0.8f), blurRadius = 6f))
                ),
                color = Color.White.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
