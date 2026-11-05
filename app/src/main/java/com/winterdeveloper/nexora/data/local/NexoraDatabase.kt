package com.winterdeveloper.nexora.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LayoutItemEntity::class,
        WidgetEntity::class,
        FolderEntity::class,
        FolderAppEntity::class,
        HiddenAppEntity::class,
        AppRenameEntity::class,
        ThemeProfileEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class NexoraDatabase : RoomDatabase() {
    abstract fun layoutDao(): LayoutDao
    abstract fun widgetDao(): WidgetDao
    abstract fun folderDao(): FolderDao
    abstract fun appPrefsDao(): AppPrefsDao
    abstract fun themeProfileDao(): ThemeProfileDao

    companion object {
        fun create(context: Context): NexoraDatabase =
            Room.databaseBuilder(context.applicationContext, NexoraDatabase::class.java, "nexora.db")
                .build()

        fun inMemory(context: Context): NexoraDatabase =
            Room.inMemoryDatabaseBuilder(context.applicationContext, NexoraDatabase::class.java)
                .build()
    }
}
