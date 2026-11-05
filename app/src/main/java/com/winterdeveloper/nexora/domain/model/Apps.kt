package com.winterdeveloper.nexora.domain.model

/**
 * Stable identity of a launchable activity for one Android user profile.
 * [userSerial] comes from UserManager.getSerialNumberForUser and survives reboots.
 */
data class AppKey(
    val packageName: String,
    val className: String,
    val userSerial: Long,
) {
    fun flatten(): String = "$packageName/$className#$userSerial"

    companion object {
        /** Parses the output of [flatten]; returns null for malformed input. */
        fun parse(value: String): AppKey? {
            val slash = value.indexOf('/')
            val hash = value.lastIndexOf('#')
            if (slash <= 0 || hash <= slash + 1 || hash == value.lastIndex) return null
            val serial = value.substring(hash + 1).toLongOrNull() ?: return null
            return AppKey(
                packageName = value.substring(0, slash),
                className = value.substring(slash + 1, hash),
                userSerial = serial,
            )
        }
    }
}

enum class AppCategory {
    GAMES, AUDIO, VIDEO, IMAGE, SOCIAL, NEWS, MAPS, PRODUCTIVITY, ACCESSIBILITY, OTHER
}

/** A launchable app as shown in the launcher. [label] already includes a user rename, if any. */
data class LauncherApp(
    val key: AppKey,
    val label: String,
    val systemLabel: String,
    val category: AppCategory,
    val isWorkProfile: Boolean,
)

/** An app shortcut published by another app (long-press menu). */
data class AppShortcut(
    val id: String,
    val packageName: String,
    val label: String,
    val userSerial: Long,
)
