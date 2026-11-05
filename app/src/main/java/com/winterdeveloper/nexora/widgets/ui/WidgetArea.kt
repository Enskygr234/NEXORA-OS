package com.winterdeveloper.nexora.widgets.ui

import android.appwidget.AppWidgetHostView
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.winterdeveloper.nexora.R
import com.winterdeveloper.nexora.container
import com.winterdeveloper.nexora.core.designsystem.LocalPalette
import com.winterdeveloper.nexora.core.designsystem.glass
import com.winterdeveloper.nexora.domain.model.LayoutLimits
import com.winterdeveloper.nexora.domain.model.WidgetEntry

@Composable
fun WidgetArea(
    widgets: List<WidgetEntry>,
    editMode: Boolean,
    onRemove: (WidgetEntry) -> Unit,
    onResize: (WidgetEntry, Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (widgets.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        widgets.forEachIndexed { index, entry ->
            androidx.compose.runtime.key(entry.appWidgetId) {
                WidgetCard(
                    entry = entry,
                    editMode = editMode,
                    canMoveUp = index > 0,
                    canMoveDown = index < widgets.lastIndex,
                    onRemove = { onRemove(entry) },
                    onGrow = { onResize(entry, LayoutLimits.WIDGET_HEIGHT_STEP_DP) },
                    onShrink = { onResize(entry, -LayoutLimits.WIDGET_HEIGHT_STEP_DP) },
                    onMoveUp = { onMove(index, index - 1) },
                    onMoveDown = { onMove(index, index + 1) },
                )
            }
        }
    }
}

@Composable
private fun WidgetCard(
    entry: WidgetEntry,
    editMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRemove: () -> Unit,
    onGrow: () -> Unit,
    onShrink: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val context = LocalContext.current
    val hostManager = context.container.widgetHost
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(24.dp)
    val available = remember(entry.appWidgetId) { hostManager.info(entry.appWidgetId) != null }

    Column(Modifier.fillMaxWidth()) {
        if (editMode) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .glass(RoundedCornerShape(16.dp), palette, strong = true)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.widget_height, entry.heightDp),
                    modifier = Modifier.padding(start = 12.dp).weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
                IconButton(onClick = onShrink, enabled = entry.heightDp > LayoutLimits.MIN_WIDGET_HEIGHT_DP) {
                    Icon(Icons.Rounded.Remove, stringResource(R.string.widget_shrink))
                }
                IconButton(onClick = onGrow, enabled = entry.heightDp < LayoutLimits.MAX_WIDGET_HEIGHT_DP) {
                    Icon(Icons.Rounded.Add, stringResource(R.string.widget_grow))
                }
                IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                    Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.widget_move_up))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                    Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.widget_move_down))
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Rounded.Delete, stringResource(R.string.widget_remove), tint = Color(0xFFFF5470))
                }
            }
        }

        if (!available) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(entry.heightDp.dp)
                    .glass(shape, palette)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.widget_unavailable),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            return@Column
        }

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(entry.heightDp.dp)
                .clip(shape)
        ) {
            val widthDp = maxWidth.value.toInt()
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    hostManager.createView(ctx, entry.appWidgetId) ?: FrameLayout(ctx)
                },
                update = { view ->
                    if (view is AppWidgetHostView) {
                        hostManager.updateSize(view, widthDp, entry.heightDp)
                    }
                },
            )
        }
    }
}
