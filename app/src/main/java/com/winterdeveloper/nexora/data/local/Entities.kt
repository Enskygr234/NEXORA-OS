package com.winterdeveloper.nexora.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "layout_items",
    indices = [Index(value = ["area", "appKey"], unique = true)],
)
data class LayoutItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val area: String,
    val appKey: String,
    val position: Int,
)

@Entity(tableName = "widgets")
data class WidgetEntity(
    @PrimaryKey val appWidgetId: Int,
    val position: Int,
    val heightDp: Int,
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(
    tableName = "folder_apps",
    primaryKeys = ["folderId", "appKey"],
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("folderId")],
)
data class FolderAppEntity(
    val folderId: Long,
    val appKey: String,
    val position: Int,
)

@Entity(tableName = "hidden_apps")
data class HiddenAppEntity(
    @PrimaryKey val appKey: String,
)

@Entity(tableName = "app_renames")
data class AppRenameEntity(
    @PrimaryKey val appKey: String,
    val label: String,
)

@Entity(tableName = "theme_profiles")
data class ThemeProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val appearanceJson: String,
    val createdAt: Long,
)
