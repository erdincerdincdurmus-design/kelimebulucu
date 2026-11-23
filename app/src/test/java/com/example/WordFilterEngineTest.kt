package com.example

import com.example.domain.FilterCriteria
import com.example.domain.WordFilterEngine
import java.nio.charset.Charset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordFilterEngineTest {

  private val sampleWords =
    WordFilterEngine.sortTurkish(
      listOf(
        "ak",
        "aklı",
        "akıllı",
        "ayak",
        "ayva",
        "aylık",
        "kalem",
        "kelam",
        "kalım",
        "kemal",
        "malik",
        "kara",
        "mart",
        "arı",
        "arama",
        "mamak",
        "elektroensefalografi",
      )
    )

  @Test
  fun turkishLowercaseHandlesIAndDottedI() {
    assertEquals("ışık", WordFilterEngine.normalizeTurkish("IŞIK"))
    assertEquals("istanbul", WordFilterEngine.normalizeTurkish("İSTANBUL"))
  }

  @Test
  fun decodeWordsHandlesUtf8AndWindows1254Fallback() {
    val utf8Bytes = "Işık\nİstanbul\n\nışık\n".toByteArray(Charsets.UTF_8)
    val utf8Decoded = WordFilterEngine.decodeWords(utf8Bytes)
    assertEquals(listOf("ışık", "istanbul"), utf8Decoded)

    val win1254Bytes = "Şehir\r\nĞümüş\r\nÇiçek\r\n".toByteArray(Charset.forName("windows-1254"))
    val winDecoded = WordFilterEngine.decodeWords(win1254Bytes)
    assertEquals(listOf("şehir", "ğümüş", "çiçek"), winDecoded)
  }

  @Test
  fun startsWithAndEndsWithRequiresAtLeastOneMiddleChar() {
    val result =
      WordFilterEngine.filterWords(
        sampleWords,
        FilterCriteria(startsWith = "ak", endsWith = "lı"),
      )
    // "aklı" has length 4 (2 + 2), so excluded; "akıllı" has length 6 >= 5, so included
    assertEquals(listOf("akıllı"), result)
  }

  @Test
  fun unorderedLettersRespectsMultiplicity() {
    val result =
      WordFilterEngine.filterWords(
        sampleWords,
        FilterCriteria(unorderedLetters = "l k m"),
      )
    assertEquals(listOf("kalem", "kalım", "kelam", "kemal", "malik"), result)

    val repeatedLetter =
      WordFilterEngine.filterWords(
        sampleWords,
        FilterCriteria(unorderedLetters = "a a a"),
      )
    assertEquals(listOf("arama"), repeatedLetter)
  }

  @Test
  fun patternWildcardsWorkForAllSupportedSymbols() {
    val result =
      WordFilterEngine.filterWords(
        sampleWords,
        FilterCriteria(pattern = "k*l?m"),
      )
    assertEquals(listOf("kalem", "kalım", "kelam"), result)

    val underscoreAndDot =
      WordFilterEngine.filterWords(
        sampleWords,
        FilterCriteria(pattern = "a_.."),
      )
    assertTrue("ayak" in underscoreAndDot)
    assertTrue("ayva" in underscoreAndDot)
  }

  @Test
  fun lengthChipsFilterExactAnd16Plus() {
    val fiveLetters =
      WordFilterEngine.filterWords(
        sampleWords,
        FilterCriteria(selectedLengths = setOf(5)),
      )
    assertTrue(fiveLetters.all { it.length == 5 })
    assertTrue("kalem" in fiveLetters)

    val sixteenPlus =
      WordFilterEngine.filterWords(
        sampleWords,
        FilterCriteria(selectedLengths = setOf(16)),
      )
    assertEquals(listOf("elektroensefalografi"), sixteenPlus)
  }
}
