@file:OptIn(ExperimentalMaterial3Api::class)

package com.winterdeveloper.nexora.widgets.ui

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.winterdeveloper.nexora.R
import com.winterdeveloper.nexora.container
import com.winterdeveloper.nexora.core.designsystem.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ProviderRow(val info: AppWidgetProviderInfo, val label: String, val appLabel: String, val sizeText: String)

@Composable
fun WidgetPickerSheet(
    onPick: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val host = context.container.widgetHost
    val palette = LocalPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val rows: List<ProviderRow>? by produceState<List<ProviderRow>?>(null) {
        value = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val dm = context.resources.displayMetrics
            host.providers().map { info ->
                val wDp = (info.minWidth / dm.density).toInt()
                val hDp = (info.minHeight / dm.density).toInt()
                ProviderRow(info, info.loadLabel(pm), host.appLabel(info), "$wDp × $hDp dp")
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = palette.navy) {
        Text(
            stringResource(R.string.widget_picker_title),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
        val list = rows
        when {
            list == null -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = palette.accent)
            }
            list.isEmpty() -> Text(
                stringResource(R.string.widget_picker_empty),
                modifier = Modifier.padding(24.dp),
                color = Color.White.copy(alpha = 0.7f),
            )
            else -> LazyColumn(
                Modifier.fillMaxWidth().navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(list, key = { "${it.info.provider.flattenToString()}#${it.info.profile.hashCode()}" }) { row ->
                    ProviderItem(row) { onPick(row.info) }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ProviderItem(row: ProviderRow, onClick: () -> Unit) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val preview: ImageBitmap? by produceState<ImageBitmap?>(null, row.info.provider) {
        value = withContext(Dispatchers.IO) {
            val density = context.resources.displayMetrics.densityDpi
            val drawable: Drawable? = try {
                row.info.loadPreviewImage(context, density) ?: row.info.loadIcon(context, density)
            } catch (e: Exception) {
                null
            }
            drawable?.let { d -> scaledBitmap(d, 360)?.asImageBitmap() }
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 96.dp, height = 72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = preview
            if (bmp != null) {
                Image(bmp, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.padding(6.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.fillMaxWidth()) {
            Text(row.label, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(row.appLabel, style = MaterialTheme.typography.bodyMedium, color = palette.accent, maxLines = 1)
            Text(row.sizeText, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.55f))
        }
    }
}

private fun scaledBitmap(d: Drawable, maxPx: Int): android.graphics.Bitmap? {
    val w = d.intrinsicWidth.takeIf { it > 0 } ?: maxPx
    val h = d.intrinsicHeight.takeIf { it > 0 } ?: maxPx
    val scale = minOf(1f, maxPx / maxOf(w, h).toFloat())
    val tw = (w * scale).toInt().coerceAtLeast(1)
    val th = (h * scale).toInt().coerceAtLeast(1)
    return try {
        d.toBitmap(tw, th)
    } catch (e: Exception) {
        null
    }
}
