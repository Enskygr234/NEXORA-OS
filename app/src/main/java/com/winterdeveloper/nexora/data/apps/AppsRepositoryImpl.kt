package com.winterdeveloper.nexora.data.apps

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import com.winterdeveloper.nexora.domain.model.AppCategory
import com.winterdeveloper.nexora.domain.model.AppKey
import com.winterdeveloper.nexora.domain.model.AppShortcut
import com.winterdeveloper.nexora.domain.model.LauncherApp
import com.winterdeveloper.nexora.domain.repository.AppsRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Reads launchable activities through [LauncherApps], the official API for home apps.
 * Covers every user profile the launcher may see (personal + work profile).
 * Re-reads automatically when apps are installed, updated or removed.
 */
class AppsRepositoryImpl(
    private val context: Context,
    private val scope: CoroutineScope,
    renames: Flow<Map<AppKey, String>>,
) : AppsRepository {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    private val _apps = MutableStateFlow<List<LauncherApp>>(emptyList())
    override val apps: StateFlow<List<LauncherApp>> = _apps.asStateFlow()

    private val _activeUserSerials = MutableStateFlow<Set<Long>>(emptySet())
    /** Serials of profiles whose apps were readable at the last refresh. */
    val activeUserSerials: StateFlow<Set<Long>> = _activeUserSerials.asStateFlow()

    /** Raw activity info, kept for icon loading. */
    private val activities = ConcurrentHashMap<AppKey, LauncherActivityInfo>()
    private val shortcutCache = ConcurrentHashMap<String, ShortcutInfo>()

    @Volatile private var currentRenames: Map<AppKey, String> = emptyMap()
    private var refreshJob: Job? = null

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        scope.launch {
            renames.collectLatest {
                currentRenames = it
                refresh()
            }
        }
    }

    fun activityInfo(key: AppKey): LauncherActivityInfo? = activities[key]

    fun userFor(serial: Long): UserHandle? = userManager.getUserForSerialNumber(serial)

    override fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            val own = context.packageName
            val mySerial = userManager.getSerialNumberForUser(Process.myUserHandle())
            val found = HashMap<AppKey, LauncherActivityInfo>()
            val serials = HashSet<Long>()
            val result = ArrayList<LauncherApp>()

            for (profile in launcherApps.profiles) {
                val serial = userManager.getSerialNumberForUser(profile)
                val list = try {
                    launcherApps.getActivityList(null, profile)
                } catch (e: SecurityException) {
                    Log.w(TAG, "Profile not readable")
                    continue
                }
                // A profile in quiet mode (work apps paused) returns an empty list.
                if (list.isEmpty() && serial != mySerial) continue
                serials += serial
                for (info in list) {
                    val component = info.componentName
                    if (component.packageName == own) continue
                    val key = AppKey(component.packageName, component.className, serial)
                    val systemLabel = info.label?.toString()?.trim().orEmpty().ifEmpty { component.packageName }
                    found[key] = info
                    result += LauncherApp(
                        key = key,
                        label = currentRenames[key] ?: systemLabel,
                        systemLabel = systemLabel,
                        category = categoryOf(info.applicationInfo),
                        isWorkProfile = serial != mySerial,
                    )
                }
            }
            activities.keys.retainAll(found.keys)
            activities.putAll(found)
            _activeUserSerials.value = serials
            _apps.value = result
        }
    }

    override fun launch(key: AppKey): Boolean {
        val user = userFor(key.userSerial) ?: return false
        return try {
            launcherApps.startMainActivity(ComponentName(key.packageName, key.className), user, null, null)
            true
        } catch (e: ActivityNotFoundException) {
            refresh()
            false
        } catch (e: SecurityException) {
            false
        }
    }

    override fun openAppInfo(key: AppKey): Boolean {
        val user = userFor(key.userSerial) ?: return false
        return try {
            launcherApps.startAppDetailsActivity(ComponentName(key.packageName, key.className), user, null, null)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    override fun hasShortcutAccess(): Boolean =
        try {
            launcherApps.hasShortcutHostPermission()
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalStateException) {
            // Thrown while the user is locked (direct boot).
            false
        }

    override fun shortcuts(key: AppKey): List<AppShortcut> {
        if (!hasShortcutAccess()) return emptyList()
        val user = userFor(key.userSerial) ?: return emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(key.packageName)
            .setActivity(ComponentName(key.packageName, key.className))
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        val infos = try {
            launcherApps.getShortcuts(query, user).orEmpty()
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: IllegalStateException) {
            emptyList()
        }
        return infos
            .filter { it.isEnabled }
            .sortedBy { it.rank }
            .take(MAX_SHORTCUTS)
            .map { info ->
                shortcutCache[cacheKey(info.`package`, info.id, key.userSerial)] = info
                AppShortcut(
                    id = info.id,
                    packageName = info.`package`,
                    label = (info.shortLabel ?: info.longLabel ?: info.id).toString(),
                    userSerial = key.userSerial,
                )
            }
    }

    fun shortcutInfo(shortcut: AppShortcut): ShortcutInfo? =
        shortcutCache[cacheKey(shortcut.packageName, shortcut.id, shortcut.userSerial)]

    override fun launchShortcut(shortcut: AppShortcut): Boolean {
        val user = userFor(shortcut.userSerial) ?: return false
        return try {
            launcherApps.startShortcut(shortcut.packageName, shortcut.id, null, null, user)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        } catch (e: IllegalStateException) {
            false
        }
    }

    private fun cacheKey(pkg: String, id: String, serial: Long) = "$pkg|$id|$serial"

    private fun categoryOf(info: ApplicationInfo): AppCategory {
        if (info.flags and ApplicationInfo.FLAG_IS_GAME != 0) return AppCategory.GAMES
        return when (info.category) {
            ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
            ApplicationInfo.CATEGORY_AUDIO -> AppCategory.AUDIO
            ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
            ApplicationInfo.CATEGORY_IMAGE -> AppCategory.IMAGE
            ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
            ApplicationInfo.CATEGORY_MAPS -> AppCategory.MAPS
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
            CATEGORY_ACCESSIBILITY -> AppCategory.ACCESSIBILITY
            else -> AppCategory.OTHER
        }
    }

    private companion object {
        const val TAG = "NexoraApps"
        const val MAX_SHORTCUTS = 5
        /** ApplicationInfo.CATEGORY_ACCESSIBILITY, added in API 31. Same value on every version. */
        const val CATEGORY_ACCESSIBILITY = 8
    }
}
