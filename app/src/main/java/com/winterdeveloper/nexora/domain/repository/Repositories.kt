package com.winterdeveloper.nexora.domain.repository

import com.winterdeveloper.nexora.domain.model.AppFolder
import com.winterdeveloper.nexora.domain.model.AppKey
import com.winterdeveloper.nexora.domain.model.AppShortcut
import com.winterdeveloper.nexora.domain.model.Appearance
import com.winterdeveloper.nexora.domain.model.CityResult
import com.winterdeveloper.nexora.domain.model.LauncherApp
import com.winterdeveloper.nexora.domain.model.LauncherSettings
import com.winterdeveloper.nexora.domain.model.LayoutArea
import com.winterdeveloper.nexora.domain.model.LayoutEntry
import com.winterdeveloper.nexora.domain.model.ThemeProfile
import com.winterdeveloper.nexora.domain.model.WeatherReport
import com.winterdeveloper.nexora.domain.model.WidgetEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AppsRepository {
    /** Every launchable activity on the device for all user profiles, with user renames applied. */
    val apps: StateFlow<List<LauncherApp>>

    fun refresh()
    fun launch(key: AppKey): Boolean
    fun openAppInfo(key: AppKey): Boolean

    /** True only while NEXORA OS is the default home app (an Android requirement for shortcuts). */
    fun hasShortcutAccess(): Boolean
    fun shortcuts(key: AppKey): List<AppShortcut>
    fun launchShortcut(shortcut: AppShortcut): Boolean
}

interface LayoutRepository {
    fun entries(area: LayoutArea): Flow<List<LayoutEntry>>
    val widgets: Flow<List<WidgetEntry>>
    val folders: Flow<List<AppFolder>>
    val hidden: Flow<Set<AppKey>>
    val renames: Flow<Map<AppKey, String>>

    suspend fun add(area: LayoutArea, key: AppKey)
    suspend fun remove(area: LayoutArea, key: AppKey)
    suspend fun move(area: LayoutArea, from: Int, to: Int)
    suspend fun clear(area: LayoutArea)

    suspend fun addWidget(appWidgetId: Int, heightDp: Int)
    suspend fun removeWidget(appWidgetId: Int)
    suspend fun resizeWidget(appWidgetId: Int, heightDp: Int)
    suspend fun moveWidget(from: Int, to: Int)

    suspend fun createFolder(name: String): Long
    suspend fun renameFolder(id: Long, name: String)
    suspend fun deleteFolder(id: Long)
    suspend fun addToFolder(folderId: Long, key: AppKey)
    suspend fun removeFromFolder(folderId: Long, key: AppKey)

    suspend fun setHidden(key: AppKey, hidden: Boolean)
    suspend fun rename(key: AppKey, label: String?)

    /**
     * Removes pins of apps that are no longer installed. Only apps belonging to
     * [activeUserSerials] are considered, so a locked work profile keeps its pins.
     */
    suspend fun prune(installed: Set<AppKey>, activeUserSerials: Set<Long>)
}

interface SettingsRepository {
    val settings: Flow<LauncherSettings>
    suspend fun update(transform: (LauncherSettings) -> LauncherSettings)
}

interface ThemeProfileRepository {
    val profiles: Flow<List<ThemeProfile>>
    suspend fun save(name: String, appearance: Appearance): Long
    suspend fun delete(id: Long)
}

interface WeatherRepository {
    val report: Flow<WeatherReport?>
    suspend fun searchCity(query: String): Result<List<CityResult>>

    /** Fetches current weather for the saved city. Skips the network if the cache is fresh. */
    suspend fun refresh(force: Boolean): Result<WeatherReport?>
    suspend fun clear()
}
