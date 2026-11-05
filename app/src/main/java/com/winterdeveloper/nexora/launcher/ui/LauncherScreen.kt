package com.winterdeveloper.nexora.launcher.ui

import android.appwidget.AppWidgetProviderInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.winterdeveloper.nexora.R
import com.winterdeveloper.nexora.core.designsystem.nexoraTween
import com.winterdeveloper.nexora.domain.model.AppShortcut
import com.winterdeveloper.nexora.domain.model.LauncherApp
import com.winterdeveloper.nexora.domain.model.LayoutArea
import com.winterdeveloper.nexora.launcher.LauncherViewModel
import com.winterdeveloper.nexora.widgets.ui.WidgetPickerSheet

/** Platform actions the screen asks the activity to perform. */
interface LauncherBridge {
    fun openSettings()
    fun startAddWidget(info: AppWidgetProviderInfo)
    fun openAlarms()
    fun openCalendar()
    fun showMessage(text: String)
}

@Composable
fun LauncherScreen(
    vm: LauncherViewModel,
    bridge: LauncherBridge,
    homeSignal: Int,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val widgets by vm.widgets.collectAsStateWithLifecycle()
    val badges by vm.badges.collectAsStateWithLifecycle()
    val appearance = settings.appearance

    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var drawerFocusSearch by remember { mutableStateOf(false) }
    var editMode by rememberSaveable { mutableStateOf(false) }
    var widgetPicker by remember { mutableStateOf(false) }

    var actionsFor by remember { mutableStateOf<LauncherApp?>(null) }
    var actionsShortcuts by remember { mutableStateOf<List<AppShortcut>>(emptyList()) }
    var renameFor by remember { mutableStateOf<LauncherApp?>(null) }
    var folderChooserFor by remember { mutableStateOf<LauncherApp?>(null) }
    var newFolderFor by remember { mutableStateOf<LauncherApp?>(null) }
    var newFolderOpen by remember { mutableStateOf(false) }
    var openFolderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var renameFolderId by remember { mutableStateOf<Long?>(null) }
    var deleteFolderId by remember { mutableStateOf<Long?>(null) }

    val launchFailed = stringResource(R.string.launch_failed)

    // Pressing Home while already on the home screen returns to a clean state.
    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            drawerOpen = false
            editMode = false
            actionsFor = null
            openFolderId = null
        }
    }

    fun launch(app: LauncherApp) {
        if (vm.launch(app)) {
            drawerOpen = false
            openFolderId = null
        } else {
            bridge.showMessage(launchFailed)
        }
    }

    fun showActions(app: LauncherApp) {
        actionsShortcuts = vm.shortcutsFor(app)
        actionsFor = app
    }

    BackHandler(enabled = true) {
        when {
            openFolderId != null -> openFolderId = null
            drawerOpen -> drawerOpen = false
            editMode -> editMode = false
            // On the home screen Back does nothing: a launcher must not close itself.
            else -> Unit
        }
    }

    Box(Modifier.fillMaxSize()) {
        LauncherBackground(appearance)

        HomeScreen(
            state = state,
            settings = settings,
            weather = weather,
            widgets = widgets,
            badges = badges,
            editMode = editMode,
            actions = HomeActions(
                onAppClick = ::launch,
                onAppLongClick = ::showActions,
                onOpenDrawer = { focus ->
                    drawerFocusSearch = focus
                    drawerOpen = true
                },
                onOpenSettings = bridge::openSettings,
                onAddWidget = { widgetPicker = true },
                onEditModeChange = { editMode = it },
                onMoveHome = { from, to -> vm.move(LayoutArea.HOME, from, to) },
                onRemoveHome = { vm.setPinned(LayoutArea.HOME, it, false) },
                onMoveFavorite = { from, to -> vm.move(LayoutArea.FAVORITES, from, to) },
                onRemoveFavorite = { vm.setPinned(LayoutArea.FAVORITES, it, false) },
                onRemoveWidget = { vm.removeWidget(it.appWidgetId) },
                onResizeWidget = { entry, delta -> vm.resizeWidget(entry, delta) },
                onMoveWidget = { from, to -> vm.moveWidget(from, to) },
                onWeatherClick = {
                    if (settings.weather.hasLocation) vm.refreshWeather(force = true) else bridge.openSettings()
                },
                onClockClick = bridge::openAlarms,
                onDateClick = bridge::openCalendar,
            ),
        )

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically(nexoraTween(320)) { it / 3 } + fadeIn(nexoraTween(240)),
            exit = slideOutVertically(nexoraTween(260)) { it / 3 } + fadeOut(nexoraTween(200)),
        ) {
            AppDrawer(
                state = state,
                appearance = appearance,
                badges = badges,
                focusSearch = drawerFocusSearch,
                onClose = { drawerOpen = false },
                onAppClick = ::launch,
                onAppLongClick = ::showActions,
                onOpenSettings = bridge::openSettings,
                onOpenFolder = { openFolderId = it.id },
                onCreateFolder = {
                    newFolderFor = null
                    newFolderOpen = true
                },
            )
        }
    }

    actionsFor?.let { app ->
        val isHome = app.key in state.homeKeys
        val isFavorite = app.key in state.favoriteKeys
        AppActionsSheet(
            app = app,
            appearance = appearance,
            isHome = isHome,
            isFavorite = isFavorite,
            shortcuts = actionsShortcuts,
            shortcutsNeedDefault = !vm.hasShortcutAccess(),
            onOpen = {
                actionsFor = null
                launch(app)
            },
            onShortcut = {
                actionsFor = null
                if (vm.launchShortcut(it)) drawerOpen = false else bridge.showMessage(launchFailed)
            },
            onToggleHome = {
                actionsFor = null
                vm.setPinned(LayoutArea.HOME, app, !isHome)
            },
            onToggleFavorite = {
                actionsFor = null
                vm.setPinned(LayoutArea.FAVORITES, app, !isFavorite)
            },
            onAddToFolder = {
                actionsFor = null
                folderChooserFor = app
            },
            onRename = {
                actionsFor = null
                renameFor = app
            },
            onHide = {
                actionsFor = null
                vm.hide(app)
            },
            onAppInfo = {
                actionsFor = null
                vm.openAppInfo(app)
            },
            onEditHome = if (isHome && !drawerOpen) {
                {
                    actionsFor = null
                    editMode = true
                }
            } else {
                null
            },
            onDismiss = { actionsFor = null },
        )
    }

    renameFor?.let { app ->
        TextInputDialog(
            title = stringResource(R.string.action_rename),
            initial = app.label,
            confirmLabel = stringResource(R.string.action_save),
            allowEmpty = true,
            onConfirm = {
                // An empty name restores the app's own label.
                vm.rename(app, it.takeIf { name -> name.isNotBlank() && name != app.systemLabel })
                renameFor = null
            },
            onDismiss = { renameFor = null },
        )
    }

    folderChooserFor?.let { app ->
        FolderChooserDialog(
            folders = state.folders,
            onPick = {
                vm.addToFolder(it.id, app)
                folderChooserFor = null
            },
            onNew = {
                newFolderFor = app
                newFolderOpen = true
                folderChooserFor = null
            },
            onDismiss = { folderChooserFor = null },
        )
    }

    if (newFolderOpen) {
        TextInputDialog(
            title = stringResource(R.string.folder_new),
            initial = "",
            confirmLabel = stringResource(R.string.action_create),
            allowEmpty = false,
            onConfirm = {
                vm.createFolderWith(it, newFolderFor)
                newFolderOpen = false
                newFolderFor = null
            },
            onDismiss = {
                newFolderOpen = false
                newFolderFor = null
            },
        )
    }

    val openFolder = state.folders.firstOrNull { it.id == openFolderId }
    if (openFolder != null) {
        FolderContentDialog(
            folder = openFolder,
            appearance = appearance,
            badges = badges,
            onAppClick = ::launch,
            onRemoveApp = { vm.removeFromFolder(openFolder.id, it) },
            onRename = { renameFolderId = openFolder.id },
            onDelete = { deleteFolderId = openFolder.id },
            onDismiss = { openFolderId = null },
        )
    }

    state.folders.firstOrNull { it.id == renameFolderId }?.let { folder ->
        TextInputDialog(
            title = stringResource(R.string.folder_rename),
            initial = folder.name,
            confirmLabel = stringResource(R.string.action_save),
            allowEmpty = false,
            onConfirm = {
                vm.renameFolder(folder.id, it)
                renameFolderId = null
            },
            onDismiss = { renameFolderId = null },
        )
    }

    state.folders.firstOrNull { it.id == deleteFolderId }?.let { folder ->
        ConfirmDialog(
            title = stringResource(R.string.folder_delete),
            message = stringResource(R.string.folder_delete_message, folder.name),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                vm.deleteFolder(folder.id)
                deleteFolderId = null
                openFolderId = null
            },
            onDismiss = { deleteFolderId = null },
        )
    }

    if (widgetPicker) {
        WidgetPickerSheet(
            onPick = {
                widgetPicker = false
                bridge.startAddWidget(it)
            },
            onDismiss = { widgetPicker = false },
        )
    }
}
