package com.winterdeveloper.nexora.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.winterdeveloper.nexora.R
import com.winterdeveloper.nexora.core.designsystem.LocalPalette
import com.winterdeveloper.nexora.core.designsystem.glass
import com.winterdeveloper.nexora.domain.model.LauncherApp
import com.winterdeveloper.nexora.domain.model.LauncherSettings
import com.winterdeveloper.nexora.domain.model.LayoutLimits
import com.winterdeveloper.nexora.domain.model.WeatherReport
import com.winterdeveloper.nexora.domain.model.WidgetEntry
import com.winterdeveloper.nexora.launcher.LauncherUiState

class HomeActions(
    val onAppClick: (LauncherApp) -> Unit,
    val onAppLongClick: (LauncherApp) -> Unit,
    val onOpenDrawer: (focusSearch: Boolean) -> Unit,
    val onOpenSettings: () -> Unit,
    val onAddWidget: () -> Unit,
    val onEditModeChange: (Boolean) -> Unit,
    val onMoveHome: (Int, Int) -> Unit,
    val onRemoveHome: (LauncherApp) -> Unit,
    val onMoveFavorite: (Int, Int) -> Unit,
    val onRemoveFavorite: (LauncherApp) -> Unit,
    val onRemoveWidget: (WidgetEntry) -> Unit,
    val onResizeWidget: (WidgetEntry, Int) -> Unit,
    val onMoveWidget: (Int, Int) -> Unit,
    val onWeatherClick: () -> Unit,
    val onClockClick: () -> Unit,
    val onDateClick: () -> Unit,
)

@Composable
fun HomeScreen(
    state: LauncherUiState,
    settings: LauncherSettings,
    weather: WeatherReport?,
    widgets: List<WidgetEntry>,
    badges: Map<String, Int>,
    editMode: Boolean,
    actions: HomeActions,
) {
    val appearance = settings.appearance
    val density = LocalDensity.current
    val swipeThreshold = with(density) { 90.dp.toPx() }
    val openDrawer by rememberUpdatedState(actions.onOpenDrawer)
    val editModeNow by rememberUpdatedState(editMode)
    val setEditMode by rememberUpdatedState(actions.onEditModeChange)

    // Swipe up at the end of the scrollable area opens the drawer.
    val swipeUpConnection = remember(swipeThreshold) {
        object : NestedScrollConnection {
            var accumulated = 0f
            var flinging = false
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!flinging && !editModeNow && available.y < 0f) {
                    accumulated += available.y
                    if (accumulated < -swipeThreshold) {
                        accumulated = 0f
                        openDrawer(false)
                    }
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                flinging = true
                accumulated = 0f
                return Velocity.Zero
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                flinging = false
                return Velocity.Zero
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .nestedScroll(swipeUpConnection)
            .pointerInput(swipeThreshold) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { change, amount ->
                        total += amount
                        if (total < -swipeThreshold && !editModeNow) {
                            change.consume()
                            total = 0f
                            openDrawer(false)
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { setEditMode(true) })
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (editMode) {
                EditBar(
                    onAddWidget = actions.onAddWidget,
                    onOpenSettings = actions.onOpenSettings,
                    onDone = { actions.onEditModeChange(false) },
                )
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(if (editMode) 8.dp else 28.dp))
                HomeClock(
                    appearance = appearance,
                    onClockClick = actions.onClockClick,
                    onDateClick = actions.onDateClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (settings.weather.enabled) {
                    WeatherChip(
                        report = weather,
                        unit = settings.weather.unit,
                        hasCity = settings.weather.hasLocation,
                        onClick = actions.onWeatherClick,
                        modifier = Modifier.align(
                            if (appearance.clockStyle == com.winterdeveloper.nexora.domain.model.ClockStyle.MINIMAL) Alignment.Start
                            else Alignment.CenterHorizontally
                        ),
                    )
                }
                SearchPill(onClick = { actions.onOpenDrawer(true) })

                com.winterdeveloper.nexora.widgets.ui.WidgetArea(
                    widgets = widgets,
                    editMode = editMode,
                    onRemove = actions.onRemoveWidget,
                    onResize = actions.onResizeWidget,
                    onMove = actions.onMoveWidget,
                )

                if (state.home.isEmpty() && state.loaded) {
                    Text(
                        text = stringResource(R.string.home_empty_hint),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                } else {
                    ReorderableAppGrid(
                        apps = state.home,
                        columns = appearance.gridColumns,
                        appearance = appearance,
                        showLabels = appearance.showLabels,
                        badges = badges,
                        editMode = editMode,
                        onClick = actions.onAppClick,
                        onLongClick = actions.onAppLongClick,
                        onMove = actions.onMoveHome,
                        onRemove = actions.onRemoveHome,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Dock(
                favorites = state.favorites.take(LayoutLimits.DOCK_SIZE),
                settings = settings,
                badges = badges,
                editMode = editMode,
                actions = actions,
            )
        }
    }
}

@Composable
private fun EditBar(onAddWidget: () -> Unit, onOpenSettings: () -> Unit, onDone: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .glass(RoundedCornerShape(20.dp), palette, strong = true)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.edit_home_title),
            modifier = Modifier.padding(start = 8.dp).weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        IconButton(onClick = onAddWidget) {
            Icon(Icons.Rounded.Widgets, stringResource(R.string.action_add_widget), tint = palette.accent)
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Rounded.Settings, stringResource(R.string.settings_title), tint = Color.White)
        }
        IconButton(onClick = onDone) {
            Icon(Icons.Rounded.Check, stringResource(R.string.action_done), tint = palette.accent)
        }
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(50), palette)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.accent)
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.search_apps_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.65f),
        )
    }
}

@Composable
private fun Dock(
    favorites: List<LauncherApp>,
    settings: LauncherSettings,
    badges: Map<String, Int>,
    editMode: Boolean,
    actions: HomeActions,
) {
    val palette = LocalPalette.current
    val appearance = settings.appearance
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .glass(RoundedCornerShape(28.dp), palette)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (favorites.isEmpty()) {
                Text(
                    stringResource(R.string.dock_empty_hint),
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.55f),
                )
            } else {
                ReorderableAppGrid(
                    apps = favorites,
                    columns = LayoutLimits.DOCK_SIZE,
                    appearance = appearance.copy(iconSizeDp = appearance.iconSizeDp.coerceAtMost(56)),
                    showLabels = false,
                    badges = badges,
                    editMode = editMode,
                    onClick = actions.onAppClick,
                    onLongClick = actions.onAppLongClick,
                    onMove = actions.onMoveFavorite,
                    onRemove = actions.onRemoveFavorite,
                )
            }
        }
        Box(
            Modifier
                .padding(start = 4.dp, end = 4.dp)
                .size(52.dp)
                .glass(CircleShape, palette, strong = true)
                .clickable { actions.onOpenDrawer(false) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Apps, stringResource(R.string.open_app_drawer), tint = palette.accent)
        }
    }
}
