# NEXORA OS release rules.
# Room, DataStore, Compose and coroutines ship their own consumer rules.

# The notification listener is referenced from the manifest only.
-keep class com.winterdeveloper.nexora.notifications.BadgeListenerService { *; }

# Strip verbose/debug logging from release builds.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
