@file:OptIn(ExperimentalFoundationApi::class)

package com.winterdeveloper.nexora.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.winterdeveloper.nexora.R
import com.winterdeveloper.nexora.core.designsystem.LocalPalette
import com.winterdeveloper.nexora.domain.model.Appearance
import com.winterdeveloper.nexora.domain.model.LauncherApp
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A fixed (non-lazy) grid of apps. In edit mode, long-press and drag an icon to a new cell;
 * the red badge removes it. Outside edit mode icons open on tap and show actions on long-press.
 */
@Composable
fun ReorderableAppGrid(
    apps: List<LauncherApp>,
    columns: Int,
    appearance: Appearance,
    showLabels: Boolean,
    badges: Map<String, Int>,
    editMode: Boolean,
    onClick: (LauncherApp) -> Unit,
    onLongClick: (LauncherApp) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (LauncherApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (apps.isEmpty() || columns <= 0) return
    val palette = LocalPalette.current
    val density = LocalDensity.current
    val currentOnMove by rememberUpdatedState(onMove)
    val rows = ceil(apps.size / columns.toFloat()).toInt()
    val cellHeight = (appearance.iconSizeDp + if (showLabels) 36 else 14).dp

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cellWidth = maxWidth / columns
        val cellWpx = with(density) { cellWidth.toPx() }
        val cellHpx = with(density) { cellHeight.toPx() }

        var dragIndex by remember { mutableIntStateOf(-1) }
        var dragOffset by remember { mutableStateOf(Offset.Zero) }

        fun targetFor(index: Int, offset: Offset): Int {
            val cx = (index % columns) * cellWpx + cellWpx / 2f + offset.x
            val cy = (index / columns) * cellHpx + cellHpx / 2f + offset.y
            val col = floor(cx / cellWpx).toInt().coerceIn(0, columns - 1)
            val row = floor(cy / cellHpx).toInt().coerceIn(0, rows - 1)
            return (row * columns + col).coerceIn(0, apps.lastIndex)
        }

        Box(Modifier.fillMaxWidth().height(cellHeight * rows)) {
            // Drop target highlight while dragging.
            if (dragIndex >= 0) {
                val t = targetFor(dragIndex, dragOffset)
                Box(
                    Modifier
                        .offset(x = cellWidth * (t % columns), y = cellHeight * (t / columns))
                        .size(cellWidth, cellHeight)
                        .padding(4.dp)
                        .border(1.dp, palette.accent.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                        .background(palette.accent.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                )
            }

            apps.forEachIndexed { index, app ->
                key(app.key) {
                    val dragged = index == dragIndex
                    val interaction = if (editMode) {
                        Modifier.pointerInput(app.key, index, columns, rows) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragIndex = index
                                    dragOffset = Offset.Zero
                                },
                                onDragEnd = {
                                    val target = targetFor(index, dragOffset)
                                    dragIndex = -1
                                    dragOffset = Offset.Zero
                                    if (target != index) currentOnMove(index, target)
                                },
                                onDragCancel = {
                                    dragIndex = -1
                                    dragOffset = Offset.Zero
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount
                                },
                            )
                        }
                    } else {
                        Modifier.combinedClickable(
                            onClick = { onClick(app) },
                            onLongClick = { onLongClick(app) },
                        )
                    }
                    Box(
                        Modifier
                            .offset(x = cellWidth * (index % columns), y = cellHeight * (index / columns))
                            .size(cellWidth, cellHeight)
                            .zIndex(if (dragged) 1f else 0f)
                            .graphicsLayer {
                                if (dragged) {
                                    translationX = dragOffset.x
                                    translationY = dragOffset.y
                                    scaleX = 1.12f
                                    scaleY = 1.12f
                                }
                            }
                            .then(interaction),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        AppTile(
                            app = app,
                            sizeDp = appearance.iconSizeDp,
                            shape = appearance.iconShape,
                            showLabel = showLabels,
                            badgeCount = badges[app.key.packageName] ?: 0,
                        )
                        if (editMode && !dragged) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 2.dp, end = 6.dp)
                                    .size(22.dp)
                                    .background(Color(0xFFFF3D5A), CircleShape)
                                    .clickable { onRemove(app) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.action_remove_from_home),
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
