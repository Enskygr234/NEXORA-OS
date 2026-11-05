package com.winterdeveloper.nexora.domain.usecase

import com.winterdeveloper.nexora.domain.model.LauncherApp
import java.text.Normalizer
import java.util.Locale

/**
 * Ranks apps for a query. Pure Kotlin so it can be unit tested.
 *
 * Order: exact label > label prefix > word prefix > substring > initials > subsequence.
 * Diacritics are ignored, and Turkish dotted/dotless i is folded, so "instagram",
 * "İnstagram" and "ınstagram" all match.
 */
object AppSearch {

    fun normalize(text: String): String {
        val lowered = text
            .replace('İ', 'i').replace('I', 'i').replace('ı', 'i')
            .lowercase(Locale.ROOT)
        val decomposed = Normalizer.normalize(lowered, Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{M}+"), "").trim()
    }

    fun score(label: String, query: String): Int {
        val l = normalize(label)
        val q = normalize(query)
        if (q.isEmpty()) return 0
        if (l == q) return 1000
        if (l.startsWith(q)) return 800 - (l.length - q.length).coerceAtMost(100)
        val words = l.split(' ', '-', '_', '.').filter { it.isNotEmpty() }
        if (words.any { it.startsWith(q) }) return 600
        if (l.contains(q)) return 400
        val initials = words.joinToString("") { it.take(1) }
        if (initials.startsWith(q)) return 300
        if (isSubsequence(q, l)) return 100
        return 0
    }

    fun search(apps: List<LauncherApp>, query: String): List<LauncherApp> {
        if (normalize(query).isEmpty()) return emptyList()
        return apps
            .map { it to maxOf(score(it.label, query), score(it.systemLabel, query) - 1) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<LauncherApp, Int>> { it.second }.thenBy { normalize(it.first.label) })
            .map { it.first }
    }

    private fun isSubsequence(needle: String, haystack: String): Boolean {
        var i = 0
        for (c in haystack) {
            if (i < needle.length && needle[i] == c) i++
        }
        return i == needle.length
    }
}
