package com.winterdeveloper.nexora.domain

import com.winterdeveloper.nexora.domain.model.AppCategory
import com.winterdeveloper.nexora.domain.model.AppKey
import com.winterdeveloper.nexora.domain.model.LauncherApp
import com.winterdeveloper.nexora.domain.usecase.AppSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchTest {

    private fun app(label: String, system: String = label) = LauncherApp(
        key = AppKey("pkg.${label.lowercase().replace(' ', '_')}", "Main", 0),
        label = label,
        systemLabel = system,
        category = AppCategory.OTHER,
        isWorkProfile = false,
    )

    private val apps = listOf(
        app("Instagram"),
        app("Google Maps"),
        app("WhatsApp"),
        app("Google Play Store"),
        app("Telegram"),
        app("Settings"),
        app("Çiçek Sepeti"),
    )

    @Test
    fun `prefix match ranks above substring match`() {
        assertTrue(AppSearch.score("Instagram", "ins") > AppSearch.score("WhatsApp", "app"))
    }

    @Test
    fun `word prefix finds second word`() {
        assertEquals("Google Maps", AppSearch.search(apps, "maps").first().label)
    }

    @Test
    fun `initials match`() {
        assertEquals("Google Play Store", AppSearch.search(apps, "gps").first().label)
    }

    @Test
    fun `subsequence match is last resort`() {
        val result = AppSearch.search(apps, "tlgm")
        assertEquals(listOf("Telegram"), result.map { it.label })
    }

    @Test
    fun `turkish dotted and dotless i are folded`() {
        assertEquals("Instagram", AppSearch.search(apps, "İnsta").first().label)
        assertEquals("Instagram", AppSearch.search(apps, "ınsta").first().label)
    }

    @Test
    fun `diacritics are ignored`() {
        assertEquals("Çiçek Sepeti", AppSearch.search(apps, "cicek").first().label)
    }

    @Test
    fun `blank query returns nothing`() {
        assertTrue(AppSearch.search(apps, "   ").isEmpty())
    }

    @Test
    fun `no match returns nothing`() {
        assertTrue(AppSearch.search(apps, "zzzz").isEmpty())
    }

    @Test
    fun `renamed app is still found by its original name`() {
        val renamed = app("Chat", system = "WhatsApp")
        assertEquals(listOf("Chat"), AppSearch.search(listOf(renamed), "whats").map { it.label })
    }
}
