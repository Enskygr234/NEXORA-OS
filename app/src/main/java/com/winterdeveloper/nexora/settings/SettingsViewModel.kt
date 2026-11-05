package com.winterdeveloper.nexora.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.winterdeveloper.nexora.core.AppContainer
import com.winterdeveloper.nexora.data.backup.BackupManager
import com.winterdeveloper.nexora.domain.model.Appearance
import com.winterdeveloper.nexora.domain.model.CityResult
import com.winterdeveloper.nexora.domain.model.LauncherApp
import com.winterdeveloper.nexora.domain.model.LauncherSettings
import com.winterdeveloper.nexora.domain.model.LayoutArea
import com.winterdeveloper.nexora.domain.model.TemperatureUnit
import com.winterdeveloper.nexora.domain.model.ThemeProfile
import com.winterdeveloper.nexora.domain.usecase.ListOps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CitySearchState {
    data object Idle : CitySearchState
    data object Loading : CitySearchState
    data class Results(val cities: List<CityResult>) : CitySearchState
    data object Failed : CitySearchState
}

sealed interface BackupEvent {
    data class Exported(val summary: BackupManager.Summary) : BackupEvent
    data class Imported(val summary: BackupManager.Summary) : BackupEvent
    data object Failed : BackupEvent
}

class SettingsViewModel(private val c: AppContainer) : ViewModel() {

    val settings: StateFlow<LauncherSettings> = c.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, LauncherSettings())

    val profiles: StateFlow<List<ThemeProfile>> = c.themeProfileRepository.profiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Hidden apps that are still installed, so they can be shown again. */
    val hiddenApps: StateFlow<List<LauncherApp>> =
        combine(c.appsRepository.apps, c.layoutRepository.hidden) { apps, hidden ->
            ListOps.sortAlphabetically(apps.filter { it.key in hidden })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _citySearch = MutableStateFlow<CitySearchState>(CitySearchState.Idle)
    val citySearch: StateFlow<CitySearchState> = _citySearch.asStateFlow()

    private val _backupEvent = MutableStateFlow<BackupEvent?>(null)
    val backupEvent: StateFlow<BackupEvent?> = _backupEvent.asStateFlow()

    init {
        c.appsRepository.refresh()
    }

    fun updateAppearance(transform: (Appearance) -> Appearance) = viewModelScope.launch {
        c.settingsRepository.update { it.copy(appearance = transform(it.appearance)) }
    }

    fun applyProfile(profile: ThemeProfile) = updateAppearance { profile.appearance }

    fun saveProfile(name: String) = viewModelScope.launch {
        c.themeProfileRepository.save(name, settings.value.appearance)
    }

    fun deleteProfile(id: Long) = viewModelScope.launch { c.themeProfileRepository.delete(id) }

    fun setWeatherEnabled(enabled: Boolean) = viewModelScope.launch {
        c.settingsRepository.update { it.copy(weather = it.weather.copy(enabled = enabled)) }
        if (enabled) c.weatherRepository.refresh(force = true)
    }

    fun setTemperatureUnit(unit: TemperatureUnit) = viewModelScope.launch {
        c.settingsRepository.update { it.copy(weather = it.weather.copy(unit = unit)) }
    }

    fun searchCity(query: String) {
        if (query.isBlank()) return
        _citySearch.value = CitySearchState.Loading
        viewModelScope.launch {
            _citySearch.value = c.weatherRepository.searchCity(query).fold(
                onSuccess = { CitySearchState.Results(it) },
                onFailure = { CitySearchState.Failed },
            )
        }
    }

    fun chooseCity(city: CityResult) = viewModelScope.launch {
        c.settingsRepository.update {
            it.copy(
                weather = it.weather.copy(
                    enabled = true,
                    cityName = city.name,
                    latitude = city.latitude,
                    longitude = city.longitude,
                )
            )
        }
        _citySearch.value = CitySearchState.Idle
        c.weatherRepository.refresh(force = true)
    }

    fun clearCity() = viewModelScope.launch {
        c.settingsRepository.update {
            it.copy(weather = it.weather.copy(cityName = null, latitude = null, longitude = null))
        }
        c.weatherRepository.clear()
    }

    fun setBadgesEnabled(enabled: Boolean) = viewModelScope.launch {
        c.settingsRepository.update { it.copy(badgesEnabled = enabled) }
    }

    fun unhide(app: LauncherApp) = viewModelScope.launch { c.layoutRepository.setHidden(app.key, false) }

    fun resetHomeLayout() = viewModelScope.launch {
        c.layoutRepository.clear(LayoutArea.HOME)
        c.layoutRepository.clear(LayoutArea.FAVORITES)
    }

    fun exportBackup(resolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        _backupEvent.value = c.backupManager.export(resolver, uri).fold(
            onSuccess = { BackupEvent.Exported(it) },
            onFailure = { BackupEvent.Failed },
        )
    }

    fun importBackup(resolver: ContentResolver, uri: Uri) = viewModelScope.launch {
        _backupEvent.value = c.backupManager.import(resolver, uri).fold(
            onSuccess = { BackupEvent.Imported(it) },
            onFailure = { BackupEvent.Failed },
        )
    }

    fun consumeBackupEvent() {
        _backupEvent.value = null
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SettingsViewModel(container) }
        }
    }
}
