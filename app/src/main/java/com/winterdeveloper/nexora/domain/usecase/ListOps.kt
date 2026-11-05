package com.winterdeveloper.nexora.domain.usecase

import com.winterdeveloper.nexora.domain.model.LauncherApp
import java.text.Collator
import java.util.Locale

object ListOps {

    /** Returns a copy of [list] with the element at [from] moved to [to]. Out-of-range indices are clamped. */
    fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
        if (list.isEmpty() || from !in list.indices) return list
        val target = to.coerceIn(0, list.lastIndex)
        if (from == target) return list
        val result = list.toMutableList()
        val item = result.removeAt(from)
        result.add(target, item)
        return result
    }

    /** Locale-aware alphabetical sort (so Turkish "Ç", "Ş", "Ü" sort where users expect them). */
    fun sortAlphabetically(apps: List<LauncherApp>, locale: Locale = Locale.getDefault()): List<LauncherApp> {
        val collator = Collator.getInstance(locale).apply { strength = Collator.PRIMARY }
        return apps.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    /** First letter used for the drawer section headers; digits and symbols go under "#". */
    fun sectionLetter(label: String, locale: Locale = Locale.getDefault()): String {
        val first = label.trim().firstOrNull() ?: return "#"
        return if (first.isLetter()) first.toString().uppercase(locale) else "#"
    }
}
