package com.winterdeveloper.nexora.domain

import com.winterdeveloper.nexora.domain.model.AppCategory
import com.winterdeveloper.nexora.domain.model.AppKey
import com.winterdeveloper.nexora.domain.model.Appearance
import com.winterdeveloper.nexora.domain.model.LauncherApp
import com.winterdeveloper.nexora.domain.model.WeatherCondition
import com.winterdeveloper.nexora.domain.usecase.ListOps
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DomainModelTest {

    @Test
    fun `app key survives flatten and parse`() {
        val key = AppKey("com.example.app", "com.example.app.MainActivity", 10)
        assertEquals(key, AppKey.parse(key.flatten()))
    }

    @Test
    fun `malformed app keys are rejected`() {
        assertNull(AppKey.parse(""))
        assertNull(AppKey.parse("noslash#1"))
        assertNull(AppKey.parse("pkg/cls"))
        assertNull(AppKey.parse("pkg/cls#notanumber"))
        assertNull(AppKey.parse("pkg/#1"))
        assertNull(AppKey.parse("pkg/cls#"))
    }

    @Test
    fun `move reorders and clamps`() {
        val list = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "c", "a", "d"), ListOps.move(list, 0, 2))
        assertEquals(listOf("d", "a", "b", "c"), ListOps.move(list, 3, 0))
        assertEquals(listOf("b", "c", "d", "a"), ListOps.move(list, 0, 99))
        assertEquals(list, ListOps.move(list, 7, 0))
        assertEquals(emptyList<String>(), ListOps.move(emptyList<String>(), 0, 1))
    }

    @Test
    fun `alphabetical sort is locale aware`() {
        fun app(label: String) = LauncherApp(AppKey("p.$label", "C", 0), label, label, AppCategory.OTHER, false)
        val sorted = ListOps.sortAlphabetically(listOf(app("Zeta"), app("Çay"), app("Cami"), app("Dergi")), Locale("tr", "TR"))
        assertEquals(listOf("Cami", "Çay", "Dergi", "Zeta"), sorted.map { it.label })
    }

    @Test
    fun `section letters`() {
        assertEquals("#", ListOps.sectionLetter("9GAG"))
        assertEquals("İ", ListOps.sectionLetter("ilaç", Locale("tr", "TR")))
        assertEquals("#", ListOps.sectionLetter("   "))
    }

    @Test
    fun `wmo codes map to conditions`() {
        assertEquals(WeatherCondition.CLEAR, WeatherCondition.fromWmoCode(0))
        assertEquals(WeatherCondition.PARTLY_CLOUDY, WeatherCondition.fromWmoCode(2))
        assertEquals(WeatherCondition.FOG, WeatherCondition.fromWmoCode(45))
        assertEquals(WeatherCondition.RAIN, WeatherCondition.fromWmoCode(63))
        assertEquals(WeatherCondition.SNOW, WeatherCondition.fromWmoCode(75))
        assertEquals(WeatherCondition.THUNDERSTORM, WeatherCondition.fromWmoCode(95))
        assertEquals(WeatherCondition.UNKNOWN, WeatherCondition.fromWmoCode(-1))
    }

    @Test
    fun `appearance values are clamped`() {
        val a = Appearance(iconSizeDp = 500, gridColumns = 1, wallpaperDim = 3f).sanitized()
        assertEquals(Appearance.MAX_ICON_DP, a.iconSizeDp)
        assertEquals(Appearance.MIN_COLUMNS, a.gridColumns)
        assertEquals(0.8f, a.wallpaperDim, 0.0001f)
    }
}
