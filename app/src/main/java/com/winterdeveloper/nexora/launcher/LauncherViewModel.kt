package com.winterdeveloper.nexora.launcher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.winterdeveloper.nexora.core.AppContainer
import com.winterdeveloper.nexora.domain.model.AppCategory
import com.winterdeveloper.nexora.domain.model.AppKey
import com.winterdeveloper.nexora.domain.model.AppShortcut
import com.winterdeveloper.nexora.domain.model.LauncherApp
import com.winterdeveloper.nexora.domain.model.LauncherSettings
import com.winterdeveloper.nexora.domain.model.LayoutArea
import com.winterdeveloper.nexora.domain.model.WeatherReport
import com.winterdeveloper.nexora.domain.model.WidgetEntry
import com.winterdeveloper.nexora.domain.usecase.ListOps
import com.winterdeveloper.nexora.notifications.BadgeStore
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FolderUi(val id: Long, val name: String, val apps: List<LauncherApp>)

data class LauncherUiState(
    val loaded: Boolean = false,
    /** All visible (not hidden) apps, sorted alphabetically. */
    val apps: List<LauncherApp> = emptyList(),
    val home: List<LauncherApp> = emptyList(),
    val favorites: List<LauncherApp> = emptyList(),
    val folders: List<FolderUi> = emptyList(),
    val hiddenCount: Int = 0,
) {
    val homeKeys: Set<AppKey> by lazy { home.map { it.key }.toSet() }
    val favoriteKeys: Set<AppKey> by lazy { favorites.map { it.key }.toSet() }
    val categories: List<AppCategory> by lazy { apps.map { it.category }.distinct().sortedBy { it.ordinal } }
}

@OptIn(FlowPreview::class)
class LauncherViewModel(private val c: AppContainer) : ViewModel() {

    private val layout = c.layoutRepository
    private val appsRepo = c.appsRepository

    val settings: StateFlow<LauncherSettings> = c.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, LauncherSettings())

    val weather: StateFlow<WeatherReport?> = c.weatherRepository.report
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val widgets: StateFlow<List<WidgetEntry>> = layout.widgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val badges: StateFlow<Map<String, Int>> = combine(settings, BadgeStore.counts) { s, counts ->
        if (s.badgesEnabled) counts else emptyMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val state: StateFlow<LauncherUiState> = combine(
        appsRepo.apps,
        layout.entries(LayoutArea.HOME),
        layout.entries(LayoutArea.FAVORITES),
        layout.folders,
        layout.hidden,
    ) { apps, home, favorites, folders, hidden ->
        val byKey = apps.associateBy { it.key }
        val visible = ListOps.sortAlphabetically(apps.filter { it.key !in hidden })
        LauncherUiState(
            loaded = apps.isNotEmpty(),
            apps = visible,
            home = home.mapNotNull { byKey[it.key] }.filter { it.key !in hidden },
            favorites = favorites.mapNotNull { byKey[it.key] }.filter { it.key !in hidden },
            folders = folders.map { f ->
                FolderUi(f.id, f.name, f.apps.mapNotNull { byKey[it] }.filter { it.key !in hidden })
            },
            hiddenCount = hidden.size,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherUiState())

    init {
        appsRepo.refresh()
        // Drop pins of uninstalled apps once the app list has settled.
        viewModelScope.launch {
            combine(appsRepo.apps, appsRepo.activeUserSerials) { apps, serials -> apps to serials }
                .filter { (apps, _) -> apps.isNotEmpty() }
                .debounce(3_000)
                .collect { (apps, serials) ->
                    layout.prune(apps.map { it.key }.toSet(), serials)
                }
        }
    }

    fun onResume() {
        appsRepo.refresh()
        refreshWeather(force = false)
    }

    fun launch(app: LauncherApp): Boolean = appsRepo.launch(app.key)
    fun openAppInfo(app: LauncherApp): Boolean = appsRepo.openAppInfo(app.key)

    fun shortcutsFor(app: LauncherApp): List<AppShortcut> = appsRepo.shortcuts(app.key)
    fun hasShortcutAccess(): Boolean = appsRepo.hasShortcutAccess()
    fun launchShortcut(shortcut: AppShortcut): Boolean = appsRepo.launchShortcut(shortcut)

    fun setPinned(area: LayoutArea, app: LauncherApp, pinned: Boolean) = viewModelScope.launch {
        if (pinned) layout.add(area, app.key) else layout.remove(area, app.key)
    }

    fun move(area: LayoutArea, from: Int, to: Int) = viewModelScope.launch { layout.move(area, from, to) }

    fun hide(app: LauncherApp) = viewModelScope.launch { layout.setHidden(app.key, true) }

    fun rename(app: LauncherApp, label: String?) = viewModelScope.launch { layout.rename(app.key, label) }

    fun createFolderWith(name: String, app: LauncherApp?) = viewModelScope.launch {
        val id = layout.createFolder(name)
        if (app != null) layout.addToFolder(id, app.key)
    }

    fun addToFolder(folderId: Long, app: LauncherApp) = viewModelScope.launch { layout.addToFolder(folderId, app.key) }
    fun removeFromFolder(folderId: Long, app: LauncherApp) = viewModelScope.launch { layout.removeFromFolder(folderId, app.key) }
    fun renameFolder(folderId: Long, name: String) = viewModelScope.launch { layout.renameFolder(folderId, name) }
    fun deleteFolder(folderId: Long) = viewModelScope.launch { layout.deleteFolder(folderId) }

    fun addWidget(appWidgetId: Int, heightDp: Int) = viewModelScope.launch { layout.addWidget(appWidgetId, heightDp) }

    fun removeWidget(appWidgetId: Int) = viewModelScope.launch {
        layout.removeWidget(appWidgetId)
        c.widgetHost.deleteId(appWidgetId)
    }

    fun resizeWidget(entry: WidgetEntry, deltaDp: Int) =
        viewModelScope.launch { layout.resizeWidget(entry.appWidgetId, entry.heightDp + deltaDp) }

    fun moveWidget(from: Int, to: Int) = viewModelScope.launch { layout.moveWidget(from, to) }

    fun refreshWeather(force: Boolean) = viewModelScope.launch { c.weatherRepository.refresh(force) }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { LauncherViewModel(container) }
        }
    }
}
