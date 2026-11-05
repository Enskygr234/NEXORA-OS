package com.winterdeveloper.nexora.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Bundle
import android.util.Log

/**
 * Thin wrapper around the platform [AppWidgetHost]. Lives for the whole process so widget views
 * survive configuration changes.
 */
class WidgetHostManager(context: Context) {

    private val appContext = context.applicationContext
    val manager: AppWidgetManager = AppWidgetManager.getInstance(appContext)
    val host: AppWidgetHost = AppWidgetHost(appContext, HOST_ID)

    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)

    fun startListening() {
        try {
            host.startListening()
        } catch (e: RuntimeException) {
            // Can fail while the device is still in direct-boot mode.
            Log.w(TAG, "startListening failed")
        }
    }

    fun stopListening() {
        try {
            host.stopListening()
        } catch (e: RuntimeException) {
            Log.w(TAG, "stopListening failed")
        }
    }

    /** Widget providers for every visible user profile, sorted by app then widget name. */
    fun providers(): List<AppWidgetProviderInfo> {
        val pm = appContext.packageManager
        return launcherApps.profiles
            .flatMap { profile -> manager.getInstalledProvidersForProfile(profile) }
            .sortedWith(
                compareBy<AppWidgetProviderInfo>(
                    { appLabel(it).lowercase() },
                    { it.loadLabel(pm).lowercase() },
                )
            )
    }

    fun appLabel(info: AppWidgetProviderInfo): String {
        val pm = appContext.packageManager
        return try {
            pm.getApplicationInfo(info.provider.packageName, 0).loadLabel(pm).toString()
        } catch (e: Exception) {
            info.provider.packageName
        }
    }

    fun allocateId(): Int = host.allocateAppWidgetId()

    fun deleteId(appWidgetId: Int) {
        try {
            host.deleteAppWidgetId(appWidgetId)
        } catch (e: RuntimeException) {
            Log.w(TAG, "deleteAppWidgetId failed")
        }
    }

    /** Returns true if Android let us bind without asking the user. */
    fun bindIfAllowed(appWidgetId: Int, info: AppWidgetProviderInfo): Boolean =
        try {
            manager.bindAppWidgetIdIfAllowed(appWidgetId, info.profile, info.provider, null)
        } catch (e: RuntimeException) {
            false
        }

    fun info(appWidgetId: Int): AppWidgetProviderInfo? = manager.getAppWidgetInfo(appWidgetId)

    fun createView(context: Context, appWidgetId: Int): AppWidgetHostView? {
        val info = info(appWidgetId) ?: return null
        return try {
            host.createView(context, appWidgetId, info)
        } catch (e: RuntimeException) {
            null
        }
    }

    /** Tells the widget how much space it has, so it can choose a layout. */
    @Suppress("DEPRECATION")
    fun updateSize(view: AppWidgetHostView, widthDp: Int, heightDp: Int) {
        try {
            view.updateAppWidgetSize(Bundle(), widthDp, heightDp, widthDp, heightDp)
        } catch (e: RuntimeException) {
            Log.w(TAG, "updateAppWidgetSize failed")
        }
    }

    companion object {
        const val HOST_ID = 0x4E58
        private const val TAG = "NexoraWidgets"
    }
}
