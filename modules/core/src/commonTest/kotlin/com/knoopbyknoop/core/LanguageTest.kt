package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers `app-languages` "Language of the texts" and "Numbers, times and dates in the language".
 */
class LanguageTest {

  @Test
  fun `languages by their codes`() {
    assertEquals(Language.DE, Language.of("de"))
    assertEquals(Language.NL, Language.of("nl-BE"))
    assertEquals(Language.FR, Language.of("fr_FR"))
    assertEquals(Language.EN, Language.of("en-GB"))
  }

  @Test
  fun `other language`() {
    assertEquals(Language.EN, Language.of("es"))
    assertEquals(Language.EN, Language.of(null))
  }

  @Test
  fun `decimal comma in all but english`() {
    assertEquals("5.2 km", MapFeatures.formatDistance(5200))
    assertEquals("5,2 km", MapFeatures.formatDistance(5200, Language.DE))
    assertEquals("5,2 km", MapFeatures.formatDistance(5200, Language.NL))
    assertEquals("5,2 km", MapFeatures.formatDistance(5200, Language.FR))
  }

  @Test
  fun `dates in each language`() {
    val day = LocalMoment(CalendarDay(2027, 5, 1), 600)

    assertEquals("1 May", RouteFerry.date(day))
    assertEquals("1. Mai", RouteFerry.date(day, Language.DE))
    assertEquals("1 mei", RouteFerry.date(day, Language.NL))
    assertEquals("1er mai", RouteFerry.date(day, Language.FR))
  }

  @Test
  fun `every language has eight compass directions`() {
    Language.entries.forEach { assertEquals(8, it.texts.compassDirections.size, it.name) }
  }
}
