package com.example.domain

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.text.Collator
import java.text.NumberFormat
import java.util.Locale

data class FilterCriteria(
  val startsWith: String = "",
  val endsWith: String = "",
  val contains: String = "",
  val unorderedLetters: String = "",
  val pattern: String = "",
  val selectedLengths: Set<Int> = emptySet(),
) {
  val isEmpty: Boolean
    get() =
      startsWith.isBlank() &&
        endsWith.isBlank() &&
        contains.isBlank() &&
        unorderedLetters.isBlank() &&
        pattern.isBlank() &&
        selectedLengths.isEmpty()
}

object WordFilterEngine {
  val TURKISH_LOCALE: Locale = Locale.Builder().setLanguage("tr").setRegion("TR").build()

  private val turkishCollator: Collator
    get() = Collator.getInstance(TURKISH_LOCALE).apply { strength = Collator.TERTIARY }

  private val numberFormat: NumberFormat
    get() = NumberFormat.getIntegerInstance(TURKISH_LOCALE)

  fun normalizeTurkish(input: String): String {
    return input.lowercase(TURKISH_LOCALE).trim()
  }

  fun formatNumber(value: Int): String = numberFormat.format(value)

  /**
   * Decodes raw file bytes as strict UTF-8 first, falling back to Windows-1254 (Turkish)
   * if invalid UTF-8 byte sequences are encountered — matching the original HTML decoder logic.
   */
  fun decodeWords(bytes: ByteArray): List<String> {
    if (bytes.isEmpty()) return emptyList()
    val text =
      try {
        val decoder =
          StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        decoder.decode(ByteBuffer.wrap(bytes)).toString()
      } catch (_: Exception) {
        String(bytes, Charset.forName("windows-1254"))
      }

    // Strip UTF-8 BOM if present on the first line
    val cleaned = text.removePrefix("\uFEFF")
    val unique = LinkedHashSet<String>()
    cleaned.lineSequence().forEach { line ->
      val normalized = normalizeTurkish(line)
      if (normalized.isNotEmpty()) {
        unique.add(normalized)
      }
    }
    return unique.toList()
  }

  fun sortTurkish(words: Iterable<String>): List<String> {
    val collator = turkishCollator
    return words.sortedWith { a, b -> collator.compare(a, b) }
  }

  fun buildPatternRegex(rawPattern: String): Regex? {
    val cleaned = normalizeTurkish(rawPattern).replace(Regex("\\s+"), "")
    if (cleaned.isEmpty()) return null
    val sb = StringBuilder("^")
    for (ch in cleaned) {
      if (ch == '?' || ch == '_' || ch == '.' || ch == '*') {
        sb.append('.')
      } else {
        sb.append(Regex.escape(ch.toString()))
      }
    }
    sb.append('$')
    return Regex(sb.toString())
  }

  fun filterWords(words: List<String>, criteria: FilterCriteria): List<String> {
    if (words.isEmpty()) return emptyList()

    val s = normalizeTurkish(criteria.startsWith)
    val e = normalizeTurkish(criteria.endsWith)
    val c = normalizeTurkish(criteria.contains)
    val rx = buildPatternRegex(criteria.pattern)

    val needCounts = HashMap<Char, Int>()
    for (ch in normalizeTurkish(criteria.unorderedLetters)) {
      if (ch.isLetter()) {
        needCounts[ch] = (needCounts[ch] ?: 0) + 1
      }
    }
    val needs = needCounts.entries.toList()
    val lengths = criteria.selectedLengths
    val hasLengths = lengths.isNotEmpty()
    val accept16Plus = 16 in lengths

    val result = ArrayList<String>()
    for (w in words) {
      if (s.isNotEmpty() && !w.startsWith(s)) continue
      if (e.isNotEmpty() && !w.endsWith(e)) continue
      if (s.isNotEmpty() && e.isNotEmpty() && w.length < s.length + e.length + 1) continue
      if (c.isNotEmpty() && !w.contains(c)) continue
      if (rx != null && !rx.matches(w)) continue

      if (needs.isNotEmpty()) {
        var allPresent = true
        for ((reqChar, reqCount) in needs) {
          var count = 0
          for (wChar in w) {
            if (wChar == reqChar) {
              count++
              if (count >= reqCount) break
            }
          }
          if (count < reqCount) {
            allPresent = false
            break
          }
        }
        if (!allPresent) continue
      }

      if (hasLengths) {
        val len = w.length
        val matchesLen = if (accept16Plus && len >= 16) true else len in lengths
        if (!matchesLen) continue
      }

      result.add(w)
    }
    return result
  }
}
