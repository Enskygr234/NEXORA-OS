package com.winterdeveloper.nexora.launcher

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.winterdeveloper.nexora.R
import com.winterdeveloper.nexora.container
import com.winterdeveloper.nexora.core.designsystem.NexoraPalettes
import com.winterdeveloper.nexora.core.designsystem.NexoraTheme
import com.winterdeveloper.nexora.domain.model.LayoutLimits
import com.winterdeveloper.nexora.launcher.ui.LauncherBridge
import com.winterdeveloper.nexora.launcher.ui.LauncherScreen
import com.winterdeveloper.nexora.settings.SettingsActivity

class MainActivity : ComponentActivity(), LauncherBridge {

    private val vm: LauncherViewModel by viewModels { LauncherViewModel.factory(container) }
    private val widgetHost by lazy { container.widgetHost }

    /** Incremented when Home is pressed while the launcher is already in front. */
    private var homeSignal by mutableIntStateOf(0)

    /** Widget ID waiting for the bind permission dialog or the widget's configuration screen. */
    private var pendingWidgetId = -1

    private val bindWidget = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pendingWidgetId
        pendingWidgetId = -1
        if (id == -1) return@registerForActivityResult
        if (result.resultCode == Activity.RESULT_OK) configureOrAdd(id) else widgetHost.deleteId(id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingWidgetId = savedInstanceState?.getInt(KEY_PENDING_WIDGET, -1) ?: -1

        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            NexoraTheme(
                palette = NexoraPalettes.byId(settings.appearance.themeId),
                animationsEnabled = settings.appearance.animationsEnabled,
            ) {
                LauncherScreen(vm = vm, bridge = this, homeSignal = homeSignal)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PENDING_WIDGET, pendingWidgetId)
    }

    override fun onStart() {
        super.onStart()
        widgetHost.startListening()
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    override fun onStop() {
        super.onStop()
        widgetHost.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (Intent.ACTION_MAIN == intent.action && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homeSignal++
        }
    }

    // ---- LauncherBridge ----

    override fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun openAlarms() {
        if (!tryStart(Intent(AlarmClock.ACTION_SHOW_ALARMS))) showMessage(getString(R.string.no_app_for_action))
    }

    override fun openCalendar() {
        val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
        if (!tryStart(intent)) showMessage(getString(R.string.no_app_for_action))
    }

    override fun showMessage(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    override fun startAddWidget(info: AppWidgetProviderInfo) {
        val id = widgetHost.allocateId()
        if (widgetHost.bindIfAllowed(id, info)) {
            configureOrAdd(id)
            return
        }
        // Android asks the user once per provider whether NEXORA OS may host it.
        pendingWidgetId = id
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
        try {
            bindWidget.launch(intent)
        } catch (e: ActivityNotFoundException) {
            pendingWidgetId = -1
            widgetHost.deleteId(id)
            showMessage(getString(R.string.widget_add_failed))
        }
    }

    private fun configureOrAdd(id: Int) {
        val info = widgetHost.info(id)
        if (info == null) {
            widgetHost.deleteId(id)
            showMessage(getString(R.string.widget_add_failed))
            return
        }
        if (info.configure != null) {
            pendingWidgetId = id
            try {
                widgetHost.host.startAppWidgetConfigureActivityForResult(this, id, 0, REQUEST_CONFIGURE, null)
                return
            } catch (e: ActivityNotFoundException) {
                pendingWidgetId = -1
            } catch (e: SecurityException) {
                pendingWidgetId = -1
            }
        }
        addWidget(id, info)
    }

    @Deprecated("Widget configuration can only be started through AppWidgetHost, which uses this callback.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CONFIGURE) return
        val id = pendingWidgetId
        pendingWidgetId = -1
        if (id == -1) return
        val info = widgetHost.info(id)
        if (resultCode == Activity.RESULT_OK && info != null) addWidget(id, info) else widgetHost.deleteId(id)
    }

    private fun addWidget(id: Int, info: AppWidgetProviderInfo) {
        val density = resources.displayMetrics.density
        val minDp = (info.minHeight / density).toInt()
        val height = maxOf(minDp + 16, DEFAULT_WIDGET_HEIGHT_DP)
            .coerceIn(LayoutLimits.MIN_WIDGET_HEIGHT_DP, LayoutLimits.MAX_WIDGET_HEIGHT_DP)
        vm.addWidget(id, height)
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

    private companion object {
        const val KEY_PENDING_WIDGET = "pending_widget"
        const val REQUEST_CONFIGURE = 0x4E01
        const val DEFAULT_WIDGET_HEIGHT_DP = 120
    }
}
