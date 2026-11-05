package com.winterdeveloper.nexora.data.layout

import androidx.room.withTransaction
import com.winterdeveloper.nexora.data.local.AppRenameEntity
import com.winterdeveloper.nexora.data.local.FolderAppEntity
import com.winterdeveloper.nexora.data.local.FolderEntity
import com.winterdeveloper.nexora.data.local.HiddenAppEntity
import com.winterdeveloper.nexora.data.local.LayoutItemEntity
import com.winterdeveloper.nexora.data.local.NexoraDatabase
import com.winterdeveloper.nexora.data.local.WidgetEntity
import com.winterdeveloper.nexora.domain.model.AppFolder
import com.winterdeveloper.nexora.domain.model.AppKey
import com.winterdeveloper.nexora.domain.model.LayoutArea
import com.winterdeveloper.nexora.domain.model.LayoutEntry
import com.winterdeveloper.nexora.domain.model.LayoutLimits
import com.winterdeveloper.nexora.domain.model.WidgetEntry
import com.winterdeveloper.nexora.domain.repository.LayoutRepository
import com.winterdeveloper.nexora.domain.usecase.ListOps
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class LayoutRepositoryImpl(private val db: NexoraDatabase) : LayoutRepository {

    private val layoutDao = db.layoutDao()
    private val widgetDao = db.widgetDao()
    private val folderDao = db.folderDao()
    private val prefsDao = db.appPrefsDao()

    override fun entries(area: LayoutArea): Flow<List<LayoutEntry>> =
        layoutDao.observe(area.name).map { rows ->
            rows.mapNotNull { row ->
                AppKey.parse(row.appKey)?.let { LayoutEntry(area, it, row.position) }
            }
        }

    override val widgets: Flow<List<WidgetEntry>> =
        widgetDao.observe().map { rows -> rows.map { WidgetEntry(it.appWidgetId, it.position, it.heightDp) } }

    override val folders: Flow<List<AppFolder>> =
        combine(folderDao.observeFolders(), folderDao.observeFolderApps()) { folders, apps ->
            val byFolder = apps.groupBy { it.folderId }
            folders.map { folder ->
                AppFolder(
                    id = folder.id,
                    name = folder.name,
                    apps = byFolder[folder.id].orEmpty().mapNotNull { AppKey.parse(it.appKey) },
                )
            }
        }

    override val hidden: Flow<Set<AppKey>> =
        prefsDao.observeHidden().map { keys -> keys.mapNotNull { AppKey.parse(it) }.toSet() }

    override val renames: Flow<Map<AppKey, String>> =
        prefsDao.observeRenames().map { rows ->
            rows.mapNotNull { row -> AppKey.parse(row.appKey)?.let { it to row.label } }.toMap()
        }

    override suspend fun add(area: LayoutArea, key: AppKey) {
        db.withTransaction {
            val next = layoutDao.maxPosition(area.name) + 1
            layoutDao.insert(LayoutItemEntity(area = area.name, appKey = key.flatten(), position = next))
        }
    }

    override suspend fun remove(area: LayoutArea, key: AppKey) {
        db.withTransaction {
            layoutDao.delete(area.name, key.flatten())
            compact(area)
        }
    }

    override suspend fun move(area: LayoutArea, from: Int, to: Int) {
        db.withTransaction {
            val rows = layoutDao.get(area.name)
            ListOps.move(rows, from, to).forEachIndexed { index, row ->
                if (row.position != index) layoutDao.setPosition(row.id, index)
            }
        }
    }

    override suspend fun clear(area: LayoutArea) {
        layoutDao.clear(area.name)
    }

    private suspend fun compact(area: LayoutArea) {
        layoutDao.get(area.name).forEachIndexed { index, row ->
            if (row.position != index) layoutDao.setPosition(row.id, index)
        }
    }

    override suspend fun addWidget(appWidgetId: Int, heightDp: Int) {
        db.withTransaction {
            val next = widgetDao.maxPosition() + 1
            widgetDao.upsert(WidgetEntity(appWidgetId, next, clampHeight(heightDp)))
        }
    }

    override suspend fun removeWidget(appWidgetId: Int) {
        db.withTransaction {
            widgetDao.delete(appWidgetId)
            widgetDao.getAll().forEachIndexed { index, w ->
                if (w.position != index) widgetDao.setPosition(w.appWidgetId, index)
            }
        }
    }

    override suspend fun resizeWidget(appWidgetId: Int, heightDp: Int) {
        widgetDao.setHeight(appWidgetId, clampHeight(heightDp))
    }

    override suspend fun moveWidget(from: Int, to: Int) {
        db.withTransaction {
            ListOps.move(widgetDao.getAll(), from, to).forEachIndexed { index, w ->
                if (w.position != index) widgetDao.setPosition(w.appWidgetId, index)
            }
        }
    }

    private fun clampHeight(heightDp: Int) =
        heightDp.coerceIn(LayoutLimits.MIN_WIDGET_HEIGHT_DP, LayoutLimits.MAX_WIDGET_HEIGHT_DP)

    override suspend fun createFolder(name: String): Long =
        folderDao.insertFolder(FolderEntity(name = name.trim(), createdAt = System.currentTimeMillis()))

    override suspend fun renameFolder(id: Long, name: String) {
        folderDao.rename(id, name.trim())
    }

    override suspend fun deleteFolder(id: Long) {
        folderDao.deleteFolder(id)
    }

    override suspend fun addToFolder(folderId: Long, key: AppKey) {
        db.withTransaction {
            val next = folderDao.maxPosition(folderId) + 1
            folderDao.insertApp(FolderAppEntity(folderId, key.flatten(), next))
        }
    }

    override suspend fun removeFromFolder(folderId: Long, key: AppKey) {
        folderDao.removeApp(folderId, key.flatten())
    }

    override suspend fun setHidden(key: AppKey, hidden: Boolean) {
        if (hidden) {
            db.withTransaction {
                prefsDao.hide(HiddenAppEntity(key.flatten()))
                // A hidden app should not stay visible on the home screen or dock.
                LayoutArea.entries.forEach { area ->
                    layoutDao.delete(area.name, key.flatten())
                    compact(area)
                }
            }
        } else {
            prefsDao.unhide(key.flatten())
        }
    }

    override suspend fun rename(key: AppKey, label: String?) {
        val clean = label?.trim().orEmpty()
        if (clean.isEmpty()) prefsDao.clearRename(key.flatten())
        else prefsDao.rename(AppRenameEntity(key.flatten(), clean.take(MAX_LABEL)))
    }

    override suspend fun prune(installed: Set<AppKey>, activeUserSerials: Set<Long>) {
        val installedKeys = installed.map { it.flatten() }.toSet()
        // Only judge apps of profiles we can currently see. A locked work profile hides its apps
        // temporarily; those pins must survive.
        fun isStale(raw: String): Boolean {
            val key = AppKey.parse(raw) ?: return true
            return key.userSerial in activeUserSerials && raw !in installedKeys
        }
        db.withTransaction {
            val stale = buildSet {
                layoutDao.getAll().forEach { if (isStale(it.appKey)) add(it.appKey) }
                folderDao.getFolderApps().forEach { if (isStale(it.appKey)) add(it.appKey) }
            }.toList()
            if (stale.isEmpty()) return@withTransaction
            layoutDao.deleteKeys(stale)
            folderDao.deleteKeys(stale)
            LayoutArea.entries.forEach { compact(it) }
        }
    }

    private companion object {
        const val MAX_LABEL = 40
    }
}
