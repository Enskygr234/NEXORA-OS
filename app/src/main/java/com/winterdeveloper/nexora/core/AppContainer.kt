package com.winterdeveloper.nexora.core

import android.content.Context
import com.winterdeveloper.nexora.data.apps.AppsRepositoryImpl
import com.winterdeveloper.nexora.data.apps.IconLoader
import com.winterdeveloper.nexora.data.backup.BackupManager
import com.winterdeveloper.nexora.data.layout.LayoutRepositoryImpl
import com.winterdeveloper.nexora.data.local.NexoraDatabase
import com.winterdeveloper.nexora.data.settings.SettingsRepositoryImpl
import com.winterdeveloper.nexora.data.settings.ThemeProfileRepositoryImpl
import com.winterdeveloper.nexora.data.settings.settingsDataStore
import com.winterdeveloper.nexora.data.weather.WeatherRepositoryImpl
import com.winterdeveloper.nexora.domain.repository.LayoutRepository
import com.winterdeveloper.nexora.domain.repository.SettingsRepository
import com.winterdeveloper.nexora.domain.repository.ThemeProfileRepository
import com.winterdeveloper.nexora.domain.repository.WeatherRepository
import com.winterdeveloper.nexora.widgets.WidgetHostManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container. One instance per process, created by [com.winterdeveloper.nexora.NexoraApp].
 * Kept deliberately small: a launcher should start fast and a DI framework adds little here.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Process-wide scope for work that must outlive a screen (app list refresh, pruning). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: NexoraDatabase = NexoraDatabase.create(appContext)

    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(appContext.settingsDataStore)
    val layoutRepository: LayoutRepository = LayoutRepositoryImpl(database)
    val themeProfileRepository: ThemeProfileRepository = ThemeProfileRepositoryImpl(database.themeProfileDao())
    val weatherRepository: WeatherRepository = WeatherRepositoryImpl(appContext, settingsRepository)

    val appsRepository: AppsRepositoryImpl = AppsRepositoryImpl(appContext, appScope, layoutRepository.renames)
    val iconLoader: IconLoader = IconLoader(appContext, appsRepository)
    val widgetHost: WidgetHostManager = WidgetHostManager(appContext)
    val backupManager: BackupManager = BackupManager(database, settingsRepository)
}
