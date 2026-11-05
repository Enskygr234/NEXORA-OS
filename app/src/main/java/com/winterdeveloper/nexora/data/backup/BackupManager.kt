package com.winterdeveloper.nexora.data.backup

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.winterdeveloper.nexora.data.local.AppRenameEntity
import com.winterdeveloper.nexora.data.local.FolderAppEntity
import com.winterdeveloper.nexora.data.local.FolderEntity
import com.winterdeveloper.nexora.data.local.HiddenAppEntity
import com.winterdeveloper.nexora.data.local.LayoutItemEntity
import com.winterdeveloper.nexora.data.local.NexoraDatabase
import com.winterdeveloper.nexora.data.local.ThemeProfileEntity
import com.winterdeveloper.nexora.data.settings.AppearanceJson
import com.winterdeveloper.nexora.data.settings.enumOrDefault
import com.winterdeveloper.nexora.domain.model.LayoutArea
import com.winterdeveloper.nexora.domain.model.TemperatureUnit
import com.winterdeveloper.nexora.domain.model.WeatherSettings
import com.winterdeveloper.nexora.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Exports and restores the launcher setup as a JSON file the user picks with the system file picker.
 *
 * Included: appearance and weather settings, home and favourite layout, folders, hidden apps,
 * renamed apps and theme profiles. Not included: widgets, because widget IDs are issued by
 * Android per installation and cannot be moved to another install.
 */
class BackupManager(
    private val db: NexoraDatabase,
    private val settings: SettingsRepository,
) {

    data class Summary(val homeApps: Int, val favorites: Int, val folders: Int, val profiles: Int)

    suspend fun export(resolver: ContentResolver, uri: Uri): Result<Summary> = withContext(Dispatchers.IO) {
        runCatching {
            val root = buildJson()
            val out = resolver.openOutputStream(uri, "wt") ?: error("Cannot open file")
            out.bufferedWriter().use { it.write(root.toString(2)) }
            summarize(root)
        }
    }

    suspend fun import(resolver: ContentResolver, uri: Uri): Result<Summary> = withContext(Dispatchers.IO) {
        runCatching {
            val text = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Cannot open file")
            require(text.length < MAX_BYTES) { "File too large" }
            val root = JSONObject(text)
            require(root.optString("format") == FORMAT) { "Not a NEXORA OS backup" }
            restore(root)
            summarize(root)
        }
    }

    private suspend fun buildJson(): JSONObject {
        val s = settings.settings.first()
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("createdAt", System.currentTimeMillis())

        root.put("settings", JSONObject().apply {
            put("appearance", AppearanceJson.encode(s.appearance))
            put("badgesEnabled", s.badgesEnabled)
            put("weather", JSONObject().apply {
                put("enabled", s.weather.enabled)
                put("unit", s.weather.unit.name)
                if (s.weather.hasLocation) {
                    put("cityName", s.weather.cityName ?: "")
                    put("latitude", s.weather.latitude ?: 0.0)
                    put("longitude", s.weather.longitude ?: 0.0)
                }
            })
        })

        val layout = JSONObject()
        LayoutArea.entries.forEach { area ->
            layout.put(area.name, JSONArray(db.layoutDao().get(area.name).map { it.appKey }))
        }
        root.put("layout", layout)

        val folderApps = db.folderDao().getFolderApps().groupBy { it.folderId }
        root.put("folders", JSONArray().apply {
            db.folderDao().getFolders().forEach { f ->
                put(JSONObject().apply {
                    put("name", f.name)
                    put("apps", JSONArray(folderApps[f.id].orEmpty().map { it.appKey }))
                })
            }
        })

        root.put("hidden", JSONArray(db.appPrefsDao().getHidden()))
        root.put("renames", JSONObject().apply {
            db.appPrefsDao().getRenames().forEach { put(it.appKey, it.label) }
        })
        root.put("themeProfiles", JSONArray().apply {
            db.themeProfileDao().getAll().forEach { p ->
                put(JSONObject().apply {
                    put("name", p.name)
                    put("appearance", JSONObject(p.appearanceJson))
                })
            }
        })
        return root
    }

    private suspend fun restore(root: JSONObject) {
        val settingsJson = root.optJSONObject("settings")
        val now = System.currentTimeMillis()

        db.withTransaction {
            val layoutDao = db.layoutDao()
            layoutDao.clearAll()
            val layout = root.optJSONObject("layout")
            LayoutArea.entries.forEach { area ->
                val keys = layout?.optJSONArray(area.name).strings()
                keys.forEachIndexed { index, key ->
                    layoutDao.insert(LayoutItemEntity(area = area.name, appKey = key, position = index))
                }
            }

            val folderDao = db.folderDao()
            folderDao.clearFolders()
            val folders = root.optJSONArray("folders")
            if (folders != null) {
                for (i in 0 until folders.length()) {
                    val f = folders.optJSONObject(i) ?: continue
                    val name = f.optString("name").trim()
                    if (name.isEmpty()) continue
                    val id = folderDao.insertFolder(FolderEntity(name = name, createdAt = now + i))
                    f.optJSONArray("apps").strings().forEachIndexed { index, key ->
                        folderDao.insertApp(FolderAppEntity(id, key, index))
                    }
                }
            }

            val prefsDao = db.appPrefsDao()
            prefsDao.clearHidden()
            root.optJSONArray("hidden").strings().forEach { prefsDao.hide(HiddenAppEntity(it)) }
            prefsDao.clearRenames()
            root.optJSONObject("renames")?.let { renames ->
                renames.keys().forEach { key ->
                    val label = renames.optString(key).trim()
                    if (label.isNotEmpty()) prefsDao.rename(AppRenameEntity(key, label.take(40)))
                }
            }

            val profileDao = db.themeProfileDao()
            profileDao.clear()
            val profiles = root.optJSONArray("themeProfiles")
            if (profiles != null) {
                for (i in 0 until profiles.length()) {
                    val p = profiles.optJSONObject(i) ?: continue
                    val appearance = AppearanceJson.decode(p.optJSONObject("appearance") ?: JSONObject())
                    profileDao.insert(
                        ThemeProfileEntity(
                            name = p.optString("name", "Profile").take(32),
                            appearanceJson = AppearanceJson.encode(appearance).toString(),
                            createdAt = now + i,
                        )
                    )
                }
            }
        }

        if (settingsJson != null) {
            val appearance = AppearanceJson.decode(settingsJson.optJSONObject("appearance") ?: JSONObject())
            val w = settingsJson.optJSONObject("weather")
            val weather = if (w == null) {
                WeatherSettings()
            } else {
                val hasCity = w.has("latitude") && w.has("longitude") && w.optString("cityName").isNotBlank()
                WeatherSettings(
                    enabled = w.optBoolean("enabled", true),
                    unit = enumOrDefault(w.optString("unit"), TemperatureUnit.CELSIUS),
                    cityName = if (hasCity) w.optString("cityName") else null,
                    latitude = if (hasCity) w.optDouble("latitude") else null,
                    longitude = if (hasCity) w.optDouble("longitude") else null,
                )
            }
            val badges = settingsJson.optBoolean("badgesEnabled", false)
            settings.update { it.copy(appearance = appearance, weather = weather, badgesEnabled = badges) }
        }
    }

    private fun summarize(root: JSONObject): Summary {
        val layout = root.optJSONObject("layout")
        return Summary(
            homeApps = layout?.optJSONArray(LayoutArea.HOME.name)?.length() ?: 0,
            favorites = layout?.optJSONArray(LayoutArea.FAVORITES.name)?.length() ?: 0,
            folders = root.optJSONArray("folders")?.length() ?: 0,
            profiles = root.optJSONArray("themeProfiles")?.length() ?: 0,
        )
    }

    private fun JSONArray?.strings(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { i -> optString(i).takeIf { it.isNotBlank() } }
    }

    companion object {
        const val FORMAT = "nexora-os-backup"
        const val VERSION = 1
        private const val MAX_BYTES = 2_000_000
        const val MIME = "application/json"
    }
}
