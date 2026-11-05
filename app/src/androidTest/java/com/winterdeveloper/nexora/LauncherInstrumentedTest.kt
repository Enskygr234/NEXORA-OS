package com.winterdeveloper.nexora

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.winterdeveloper.nexora.data.apps.AppsRepositoryImpl
import com.winterdeveloper.nexora.data.layout.LayoutRepositoryImpl
import com.winterdeveloper.nexora.data.local.NexoraDatabase
import com.winterdeveloper.nexora.data.settings.SettingsRepositoryImpl
import com.winterdeveloper.nexora.domain.model.AppKey
import com.winterdeveloper.nexora.domain.model.BackgroundMode
import com.winterdeveloper.nexora.domain.model.LayoutArea
import com.winterdeveloper.nexora.launcher.MainActivity
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: NexoraDatabase

    @Before
    fun setUp() {
        db = NexoraDatabase.inMemory(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun mainActivityIsAHomeCandidate() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val matches = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        assertTrue(
            "MainActivity must be selectable as the default home app",
            matches.any {
                it.activityInfo.packageName == context.packageName &&
                    it.activityInfo.name == MainActivity::class.java.name
            },
        )
    }

    @Test
    fun appListLoadsLaunchableApps(): Unit = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val repo = AppsRepositoryImpl(context, scope, flowOf(emptyMap()))
            repo.refresh()
            val apps = withTimeout(10_000) { repo.apps.first { it.isNotEmpty() } }
            assertTrue(apps.isNotEmpty())
            assertFalse("The launcher must not list itself", apps.any { it.key.packageName == context.packageName })
            assertTrue(apps.all { it.label.isNotBlank() })
        } finally {
            scope.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()
        }
    }

    @Test
    fun homeLayoutIsSavedAndReordered(): Unit = runBlocking {
        val repo = LayoutRepositoryImpl(db)
        val a = AppKey("a.pkg", "A", 0)
        val b = AppKey("b.pkg", "B", 0)
        val c = AppKey("c.pkg", "C", 0)
        repo.add(LayoutArea.HOME, a)
        repo.add(LayoutArea.HOME, b)
        repo.add(LayoutArea.HOME, c)
        repo.add(LayoutArea.HOME, a) // duplicate is ignored
        assertEquals(listOf(a, b, c), repo.entries(LayoutArea.HOME).first().map { it.key })

        repo.move(LayoutArea.HOME, 0, 2)
        assertEquals(listOf(b, c, a), repo.entries(LayoutArea.HOME).first().map { it.key })

        repo.remove(LayoutArea.HOME, c)
        val after = repo.entries(LayoutArea.HOME).first()
        assertEquals(listOf(b, a), after.map { it.key })
        assertEquals(listOf(0, 1), after.map { it.position })
    }

    @Test
    fun hidingAnAppRemovesItsPins(): Unit = runBlocking {
        val repo = LayoutRepositoryImpl(db)
        val a = AppKey("a.pkg", "A", 0)
        repo.add(LayoutArea.HOME, a)
        repo.add(LayoutArea.FAVORITES, a)
        repo.setHidden(a, true)
        assertTrue(repo.entries(LayoutArea.HOME).first().isEmpty())
        assertTrue(repo.entries(LayoutArea.FAVORITES).first().isEmpty())
        assertEquals(setOf(a), repo.hidden.first())
        repo.setHidden(a, false)
        assertTrue(repo.hidden.first().isEmpty())
    }

    @Test
    fun pruneKeepsAppsOfInactiveProfiles(): Unit = runBlocking {
        val repo = LayoutRepositoryImpl(db)
        val installed = AppKey("a.pkg", "A", 0)
        val uninstalled = AppKey("gone.pkg", "G", 0)
        val workApp = AppKey("w.pkg", "W", 10)
        repo.add(LayoutArea.HOME, installed)
        repo.add(LayoutArea.HOME, uninstalled)
        repo.add(LayoutArea.HOME, workApp)
        // Profile 10 is locked: its apps are not in the list but must not be removed.
        repo.prune(setOf(installed), activeUserSerials = setOf(0L))
        assertEquals(listOf(installed, workApp), repo.entries(LayoutArea.HOME).first().map { it.key })
    }

    @Test
    fun foldersAndRenames(): Unit = runBlocking {
        val repo = LayoutRepositoryImpl(db)
        val a = AppKey("a.pkg", "A", 0)
        val id = repo.createFolder("  Tools ")
        repo.addToFolder(id, a)
        repo.addToFolder(id, a)
        val folder = repo.folders.first().single()
        assertEquals("Tools", folder.name)
        assertEquals(listOf(a), folder.apps)

        repo.rename(a, "My App")
        assertEquals("My App", repo.renames.first()[a])
        repo.rename(a, "   ")
        assertTrue(repo.renames.first().isEmpty())

        repo.deleteFolder(id)
        assertTrue(repo.folders.first().isEmpty())
    }

    @Test
    fun settingsSurviveRestart(): Unit = runBlocking {
        val file = File(context.cacheDir, "settings-test-${System.nanoTime()}.preferences_pb")
        try {
            val scope1 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val store1 = PreferenceDataStoreFactory.create(scope = scope1, produceFile = { file })
            SettingsRepositoryImpl(store1).update {
                it.copy(appearance = it.appearance.copy(gridColumns = 5, background = BackgroundMode.OLED_BLACK))
            }
            scope1.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()

            // A new DataStore on the same file simulates a process restart.
            val scope2 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val store2 = PreferenceDataStoreFactory.create(scope = scope2, produceFile = { file })
            val restored = SettingsRepositoryImpl(store2).settings.first()
            assertEquals(5, restored.appearance.gridColumns)
            assertEquals(BackgroundMode.OLED_BLACK, restored.appearance.background)
            scope2.coroutineContext[kotlinx.coroutines.Job]?.cancelAndJoin()
        } finally {
            file.delete()
        }
    }
}
