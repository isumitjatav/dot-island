package com.nothing.island.ai.ui.components

import java.text.BreakIterator

/**
 * Pure JVM text helpers for grapheme-safe avatar initials and fallback glyphs
 * across emoji sequences, Devanagari/non-Latin scripts, and unbreakable strings.
 */
object TextBreakUtils {

    /**
     * Extracts up to [maxInitials] intact grapheme clusters from [rawName].
     * Avoids splitting UTF-16 surrogate pairs (emoji) or combining marks (Hindi/Devanagari).
     */
    fun extractAvatarInitials(
        rawName: String?,
        maxInitials: Int = 2,
        fallback: String = "C",
    ): String {
        val trimmed = rawName?.trim().orEmpty()
        if (trimmed.isEmpty()) return fallback

        val words = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val letterOrDigitWords = words.mapNotNull { firstLetterOrDigitGrapheme(it) }
        if (letterOrDigitWords.isNotEmpty()) {
            return letterOrDigitWords.take(maxInitials).joinToString("")
        }

        // Emoji-only or punctuation-only names: return the first intact grapheme cluster.
        return firstGraphemeCluster(trimmed)?.takeIf { it.isNotBlank() } ?: fallback
    }

    /**
     * Returns a single safe BMP fallback character (never an unpaired UTF-16 surrogate).
     */
    fun extractSafeFallbackChar(rawText: String?, fallback: Char = 'N'): Char {
        val text = rawText ?: return fallback
        var idx = 0
        while (idx < text.length) {
            val cp = text.codePointAt(idx)
            val charCount = Character.charCount(cp)
            if (Character.isLetterOrDigit(cp) && Character.isBmpCodePoint(cp)) {
                return cp.toChar().uppercaseChar()
            }
            idx += charCount
        }
        return fallback
    }

    private fun firstLetterOrDigitGrapheme(word: String): String? {
        val it = BreakIterator.getCharacterInstance()
        it.setText(word)
        var start = it.first()
        var end = it.next()
        while (end != BreakIterator.DONE) {
            val cluster = word.substring(start, end)
            val cp = cluster.codePointAt(0)
            if (Character.isLetterOrDigit(cp)) {
                return cluster.uppercase()
            }
            start = end
            end = it.next()
        }
        return null
    }

    private fun firstGraphemeCluster(text: String): String? {
        val it = BreakIterator.getCharacterInstance()
        it.setText(text)
        val start = it.first()
        val end = it.next()
        return if (end != BreakIterator.DONE && end > start) text.substring(start, end) else null
    }
}
