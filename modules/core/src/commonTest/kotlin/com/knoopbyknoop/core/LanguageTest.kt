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

  // Requirement: Navigating with route points, scenario "All languages"

  private val stop = Spot("", WaypointRole.STOP)
  private val end = Spot("", WaypointRole.END)
  private val knot12 = Spot("12", null)

  private fun sentences(language: Language): List<String> {
    val texts = language.texts
    return listOf(
      texts.next(stop, knot12),
      texts.next(end, null),
      texts.next(knot12, end),
      texts.ahead(stop, knot12),
      texts.ahead(end, null),
      texts.offRoute(end, "300 m"),
      texts.guidance(end, "300 m"),
      texts.backOnRoute(stop),
      texts.wrongWay(end),
      texts.wrongWayNext(end),
      texts.offRouteNext(end),
      texts.wayTo("300 m", "N", Spot("", WaypointRole.START)),
      texts.toKnot("800 m", stop, "19:05"),
      texts.ridden("4.0 of 12.4 km", stop),
      texts.knot(end),
    )
  }

  @Test
  fun `route points in english`() {
    assertEquals(
      listOf(
        "On to the stop, then 12",
        "On to the end",
        "Next 12, then the end",
        "The stop ahead, then 12",
        "The end ahead",
        "Off route, on to the end, 300 m",
        "On to the end, 300 m",
        "Back on route, on to the stop",
        "Wrong direction, turn around, on to the end",
        "Wrong direction, on to the end",
        "Off route, on to the end",
        "300 m N to the start",
        "800 m to the stop · end around 19:05",
        "4.0 of 12.4 km ridden, on to the stop",
        "End",
      ),
      sentences(Language.EN),
    )
  }

  // Scenario: All languages
  @Test
  fun `route points in german`() {
    assertEquals(
      listOf(
        "Weiter zum Zwischenstopp, dann 12",
        "Weiter zum Ziel",
        "Nächster Knotenpunkt 12, dann das Ziel",
        "Zwischenstopp voraus, dann 12",
        "Ziel voraus",
        "Abseits der Route, weiter zum Ziel, 300 m",
        "Weiter zum Ziel, 300 m",
        "Zurück auf der Route, weiter zum Zwischenstopp",
        "Falsche Richtung, bitte wenden, weiter zum Ziel",
        "Falsche Richtung, weiter zum Ziel",
        "Abseits der Route, weiter zum Ziel",
        "300 m Richtung N bis zum Start",
        "800 m bis zum Zwischenstopp · Ende gegen 19:05",
        "4.0 of 12.4 km zurückgelegt, weiter zum Zwischenstopp",
        "Ende",
      ),
      sentences(Language.DE),
    )
  }

  @Test
  fun `route points in dutch`() {
    assertEquals(
      listOf(
        "Verder naar de tussenstop, dan 12",
        "Verder naar het einde",
        "Volgend knooppunt 12, dan het einde",
        "De tussenstop komt eraan, dan 12",
        "Het einde komt eraan",
        "Van de route af, verder naar het einde, 300 m",
        "Verder naar het einde, 300 m",
        "Terug op de route, verder naar de tussenstop",
        "Verkeerde richting, keer om, verder naar het einde",
        "Verkeerde richting, verder naar het einde",
        "Van de route af, verder naar het einde",
        "300 m richting N naar de start",
        "800 m naar de tussenstop · einde rond 19:05",
        "4.0 of 12.4 km afgelegd, verder naar de tussenstop",
        "Einde",
      ),
      sentences(Language.NL),
    )
  }

  @Test
  fun `route points in french`() {
    assertEquals(
      listOf(
        "Vers l'étape, puis 12",
        "Vers l'arrivée",
        "Prochain point-nœud 12, puis l'arrivée",
        "L'étape approche, puis 12",
        "L'arrivée approche",
        "Hors itinéraire, vers l'arrivée, 300 m",
        "Vers l'arrivée, 300 m",
        "De retour sur l'itinéraire, vers l'étape",
        "Mauvaise direction, faites demi-tour, vers l'arrivée",
        "Mauvaise direction, vers l'arrivée",
        "Hors itinéraire, vers l'arrivée",
        "300 m direction N jusqu'au départ",
        "800 m jusqu'à l'étape · arrivée vers 19:05",
        "4.0 of 12.4 km parcourus, vers l'étape",
        "Arrivée",
      ),
      sentences(Language.FR),
    )
  }

  @Test
  fun `a route point has no signs to follow`() {
    assertEquals("Next 12, then the stop", Language.EN.texts.follow(knot12, stop))
    assertEquals("At 12 follow 30", Language.EN.texts.follow(knot12, Spot("30", null)))
  }
}
