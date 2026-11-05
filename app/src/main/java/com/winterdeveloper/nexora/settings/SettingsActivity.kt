package com.winterdeveloper.nexora.settings

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.winterdeveloper.nexora.R
import com.winterdeveloper.nexora.container
import com.winterdeveloper.nexora.core.designsystem.NexoraPalettes
import com.winterdeveloper.nexora.core.designsystem.NexoraTheme
import com.winterdeveloper.nexora.data.backup.BackupManager
import com.winterdeveloper.nexora.domain.model.BackgroundMode
import com.winterdeveloper.nexora.launcher.DefaultLauncher
import com.winterdeveloper.nexora.notifications.BadgeStore
import com.winterdeveloper.nexora.settings.ui.SettingsBridge
import com.winterdeveloper.nexora.settings.ui.SettingsScreen
import java.time.LocalDate

class SettingsActivity : ComponentActivity(), SettingsBridge {

    private val vm: SettingsViewModel by viewModels { SettingsViewModel.factory(container) }

    private var isDefaultLauncher by mutableStateOf(false)
    private var badgeAccess by mutableStateOf(false)

    private val requestDefault = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshStatus()
        if (!isDefaultLauncher) DefaultLauncher.openHomeSettings(this)
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument(BackupManager.MIME)) { uri ->
        if (uri != null) vm.exportBackup(contentResolver, uri)
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importBackup(contentResolver, uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            NexoraTheme(
                palette = NexoraPalettes.byId(settings.appearance.themeId),
                animationsEnabled = settings.appearance.animationsEnabled,
            ) {
                SettingsScreen(
                    vm = vm,
                    isDefaultLauncher = isDefaultLauncher,
                    badgeAccessGranted = badgeAccess,
                    bridge = this,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        isDefaultLauncher = DefaultLauncher.isDefault(this)
        badgeAccess = BadgeStore.isAccessGranted(this)
    }

    // ---- SettingsBridge ----

    override fun close() = finish()

    override fun requestDefaultLauncher() {
        try {
            requestDefault.launch(DefaultLauncher.requestIntent(this))
        } catch (e: ActivityNotFoundException) {
            DefaultLauncher.openHomeSettings(this)
        }
    }

    override fun openNotificationAccess() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        if (!tryStart(intent)) toast(getString(R.string.no_app_for_action))
    }

    override fun pickSystemWallpaper() {
        val chooser = Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), getString(R.string.wallpaper_choose))
        if (tryStart(chooser)) {
            vm.updateAppearance { it.copy(background = BackgroundMode.SYSTEM_WALLPAPER) }
        } else {
            toast(getString(R.string.no_app_for_action))
        }
    }

    override fun pickLiveWallpaper() {
        if (tryStart(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))) {
            vm.updateAppearance { it.copy(background = BackgroundMode.SYSTEM_WALLPAPER) }
        } else {
            toast(getString(R.string.live_wallpaper_unsupported))
        }
    }

    override fun exportBackup() {
        try {
            exportLauncher.launch("nexora-backup-${LocalDate.now()}.json")
        } catch (e: ActivityNotFoundException) {
            toast(getString(R.string.no_app_for_action))
        }
    }

    override fun importBackup() {
        try {
            importLauncher.launch(arrayOf(BackupManager.MIME, "text/plain", "application/octet-stream"))
        } catch (e: ActivityNotFoundException) {
            toast(getString(R.string.no_app_for_action))
        }
    }

    override fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun tryStart(intent: Intent): Boolean =
        try {
            startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
}
