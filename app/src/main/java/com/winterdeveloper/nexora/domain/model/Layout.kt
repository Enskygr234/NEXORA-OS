package com.winterdeveloper.nexora.domain.model

/** Where an app is pinned. HOME = home screen grid, FAVORITES = dock + favourites tab. */
enum class LayoutArea { HOME, FAVORITES }

data class LayoutEntry(
    val area: LayoutArea,
    val key: AppKey,
    val position: Int,
)

data class WidgetEntry(
    val appWidgetId: Int,
    val position: Int,
    val heightDp: Int,
)

data class AppFolder(
    val id: Long,
    val name: String,
    val apps: List<AppKey>,
)

object LayoutLimits {
    const val DOCK_SIZE = 5
    const val MIN_WIDGET_HEIGHT_DP = 72
    const val MAX_WIDGET_HEIGHT_DP = 480
    const val WIDGET_HEIGHT_STEP_DP = 40
}
