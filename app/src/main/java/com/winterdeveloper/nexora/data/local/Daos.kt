package com.winterdeveloper.nexora.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LayoutDao {
    @Query("SELECT * FROM layout_items WHERE area = :area ORDER BY position ASC")
    fun observe(area: String): Flow<List<LayoutItemEntity>>

    @Query("SELECT * FROM layout_items WHERE area = :area ORDER BY position ASC")
    suspend fun get(area: String): List<LayoutItemEntity>

    @Query("SELECT * FROM layout_items ORDER BY area, position")
    suspend fun getAll(): List<LayoutItemEntity>

    @Query("SELECT COALESCE(MAX(position), -1) FROM layout_items WHERE area = :area")
    suspend fun maxPosition(area: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: LayoutItemEntity): Long

    @Query("UPDATE layout_items SET position = :position WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int)

    @Query("DELETE FROM layout_items WHERE area = :area AND appKey = :appKey")
    suspend fun delete(area: String, appKey: String)

    @Query("DELETE FROM layout_items WHERE area = :area")
    suspend fun clear(area: String)

    @Query("DELETE FROM layout_items")
    suspend fun clearAll()

    @Query("DELETE FROM layout_items WHERE appKey IN (:appKeys)")
    suspend fun deleteKeys(appKeys: List<String>)
}

@Dao
interface WidgetDao {
    @Query("SELECT * FROM widgets ORDER BY position ASC")
    fun observe(): Flow<List<WidgetEntity>>

    @Query("SELECT * FROM widgets ORDER BY position ASC")
    suspend fun getAll(): List<WidgetEntity>

    @Query("SELECT COALESCE(MAX(position), -1) FROM widgets")
    suspend fun maxPosition(): Int

    @Upsert
    suspend fun upsert(widget: WidgetEntity)

    @Query("UPDATE widgets SET heightDp = :heightDp WHERE appWidgetId = :id")
    suspend fun setHeight(id: Int, heightDp: Int)

    @Query("UPDATE widgets SET position = :position WHERE appWidgetId = :id")
    suspend fun setPosition(id: Int, position: Int)

    @Query("DELETE FROM widgets WHERE appWidgetId = :id")
    suspend fun delete(id: Int)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY createdAt ASC")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folder_apps ORDER BY folderId, position")
    fun observeFolderApps(): Flow<List<FolderAppEntity>>

    @Query("SELECT * FROM folders ORDER BY createdAt ASC")
    suspend fun getFolders(): List<FolderEntity>

    @Query("SELECT * FROM folder_apps ORDER BY folderId, position")
    suspend fun getFolderApps(): List<FolderAppEntity>

    @Insert
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("UPDATE folders SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: Long)

    @Query("DELETE FROM folders")
    suspend fun clearFolders()

    @Query("SELECT COALESCE(MAX(position), -1) FROM folder_apps WHERE folderId = :folderId")
    suspend fun maxPosition(folderId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApp(item: FolderAppEntity)

    @Query("DELETE FROM folder_apps WHERE folderId = :folderId AND appKey = :appKey")
    suspend fun removeApp(folderId: Long, appKey: String)

    @Query("DELETE FROM folder_apps WHERE appKey IN (:appKeys)")
    suspend fun deleteKeys(appKeys: List<String>)
}

@Dao
interface AppPrefsDao {
    @Query("SELECT appKey FROM hidden_apps")
    fun observeHidden(): Flow<List<String>>

    @Query("SELECT appKey FROM hidden_apps")
    suspend fun getHidden(): List<String>

    @Upsert
    suspend fun hide(item: HiddenAppEntity)

    @Query("DELETE FROM hidden_apps WHERE appKey = :appKey")
    suspend fun unhide(appKey: String)

    @Query("DELETE FROM hidden_apps")
    suspend fun clearHidden()

    @Query("SELECT * FROM app_renames")
    fun observeRenames(): Flow<List<AppRenameEntity>>

    @Query("SELECT * FROM app_renames")
    suspend fun getRenames(): List<AppRenameEntity>

    @Upsert
    suspend fun rename(item: AppRenameEntity)

    @Query("DELETE FROM app_renames WHERE appKey = :appKey")
    suspend fun clearRename(appKey: String)

    @Query("DELETE FROM app_renames")
    suspend fun clearRenames()

    @Query("DELETE FROM hidden_apps WHERE appKey IN (:appKeys)")
    suspend fun deleteHiddenKeys(appKeys: List<String>)

    @Query("DELETE FROM app_renames WHERE appKey IN (:appKeys)")
    suspend fun deleteRenameKeys(appKeys: List<String>)
}

@Dao
interface ThemeProfileDao {
    @Query("SELECT * FROM theme_profiles ORDER BY createdAt ASC")
    fun observe(): Flow<List<ThemeProfileEntity>>

    @Query("SELECT * FROM theme_profiles ORDER BY createdAt ASC")
    suspend fun getAll(): List<ThemeProfileEntity>

    @Insert
    suspend fun insert(profile: ThemeProfileEntity): Long

    @Query("DELETE FROM theme_profiles WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM theme_profiles")
    suspend fun clear()
}
