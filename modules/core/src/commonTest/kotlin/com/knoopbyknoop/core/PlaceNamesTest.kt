package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the core of `map-planning` "Waypoint list with badges" and `knot-navigation` "Coming
 * knots" with places and borders. The knots lie on a road east, as far apart as their connections
 * are long; the time is minutes from midnight.
 */
class PlaceNamesTest {

  private val dutchLimburg = Region("NL-LI", Country.NETHERLANDS, "Limburg")
  private val belgianLimburg =
    Region("BE-VLI", Country.BELGIUM, "Limburg", mapOf(Language.FR to "Limbourg"))
  private val gelderland = Region("NL-GE", Country.NETHERLANDS, "Gelderland")
  private val overijssel = Region("NL-OV", Country.NETHERLANDS, "Overijssel")

  private fun knot(id: Long, ref: String, meters: Double, place: Place? = null) =
    Knot(id, ref, NetworkType.CYCLING, LATITUDE, LONGITUDE + meters / metersPerDegreeLongitude)
      .copy(place = place)

  private fun fix(meters: Double) =
    Fix(
      LATITUDE,
      LONGITUDE + meters / metersPerDegreeLongitude,
      5.0,
      18 * 60 * 60_000L + 20 * 60_000L,
    )

  private fun localTime(millis: Long) =
    LocalMoment(CalendarDay(2027, 6, 16), (millis / 60_000).toInt() % MINUTES_PER_DAY)

  /**
   * The coming knots and borders at the start of [knots], with [crossings] on their connections.
   */
  private fun coming(
    knots: List<Knot>,
    crossings: (List<Connection>) -> Map<Connection, List<Crossing>> = { mapOf() },
    language: Language = Language.EN,
  ): List<ComingKnot> {
    val connections = knots.zipWithNext { from, to ->
      Connection(
        from.id,
        to.id,
        ((to.longitude - from.longitude) * metersPerDegreeLongitude).toInt(),
      )
    }
    val graph = NetworkGraph(knots, connections, crossings = crossings(connections))
    val start = fix(0.0)
    val navigation =
      Navigation.start(
          knots,
          graph,
          NetworkGeometry.straightLines(graph),
          start,
          stops = setOf(),
          localTime = ::localTime,
          language = language,
        )!!
        .navigation
    return navigation.progress(start).coming
  }

  @Test
  fun `Place name in the app's language`() {
    val liege = Place("Liège", mapOf(Language.DE to "Lüttich", Language.NL to "Luik"))

    assertEquals("Liège (Lüttich)", liege.nameIn(Language.DE))
    assertEquals("Liège (Luik)", liege.nameIn(Language.NL))
    assertEquals("Liège", liege.nameIn(Language.EN))
    assertEquals("Lohmar", Place("Lohmar").nameIn(Language.FR))
  }

  @Test
  fun `Places in the list`() {
    val list =
      coming(
        listOf(
          knot(6, "06", 0.0, Place("Lohmar")),
          knot(2, "02", 2_000.0),
          knot(28, "28", 5_200.0, Place("Sankt Augustin")),
        )
      )

    assertEquals(listOf(null, "Sankt Augustin"), list.map { it.placeName })
  }

  @Test
  fun `Place and border with a screen reader`() {
    val knots =
      listOf(
        knot(95, "95", 0.0),
        knot(66, "66", 2_000.0),
        knot(28, "28", 5_200.0, Place("Sankt Augustin")),
      )

    val list =
      coming(knots, { mapOf(it[0] to listOf(Crossing(1_200, dutchLimburg, belgianLimburg))) })

    assertEquals("Knot 28, Sankt Augustin, end, 5.2 km, 18:40", list.last().description)
    assertEquals("Border, Belgium, Limburg", list.first().description)
  }

  @Test
  fun `Crossing into another country`() {
    val knots = listOf(knot(95, "95", 0.0), knot(66, "66", 2_000.0), knot(12, "12", 4_000.0))
    val crossings = { connections: List<Connection> ->
      mapOf(connections[0] to listOf(Crossing(1_200, dutchLimburg, belgianLimburg)))
    }

    val english = coming(knots, crossings)
    val german = coming(knots, crossings, Language.DE)

    assertEquals(listOf("", "66", "12"), english.map { it.label })
    assertEquals(ProgressMarkKind.BORDER, english.first().kind)
    assertEquals("Belgium, Limburg", english.first().border)
    assertEquals("1.2 km", english.first().distance)
    assertEquals("Belgien, Limburg", german.first().border)
    assertEquals("Belgique, Limbourg", coming(knots, crossings, Language.FR).first().border)
  }

  @Test
  fun `Crossing a province border`() {
    val knots = listOf(knot(12, "12", 0.0), knot(31, "31", 3_000.0))

    val list = coming(knots, { mapOf(it[0] to listOf(Crossing(1_000, gelderland, overijssel))) })

    assertEquals("Overijssel", list.first().border)
  }

  @Test
  fun `a connection ridden backwards`() {
    // The connection is stored from 12 to 31; the route rides it from 31 to 12.
    val knots = listOf(knot(31, "31", 0.0), knot(12, "12", 3_000.0))
    val stored = Connection(12, 31, 3_000)
    val graph =
      NetworkGraph(
        knots,
        listOf(stored),
        crossings = mapOf(stored to listOf(Crossing(1_000, gelderland, overijssel))),
      )

    val back = graph.crossingsBetween(knots[0], knots[1]).single()

    assertEquals(2_000, back.atMeters)
    assertEquals(overijssel, back.from)
    assertEquals(gelderland, back.to)
  }

  @Test
  fun `back and forth along a border`() {
    val knots = listOf(knot(12, "12", 0.0), knot(31, "31", 5_000.0))
    val stored = Connection(12, 31, 5_000)
    fun crossingsOf(vararg found: Crossing) =
      NetworkGraph(knots, listOf(stored), crossings = mapOf(stored to found.toList()))
        .crossingsBetween(knots[0], knots[1])

    // Over and back within 500 m: no line.
    assertEquals(
      listOf(),
      crossingsOf(Crossing(1_000, gelderland, overijssel), Crossing(1_300, overijssel, gelderland)),
    )
    // Over, back and over again within 500 m of each other: one line.
    assertEquals(
      listOf(Crossing(1_000, gelderland, overijssel)),
      crossingsOf(
        Crossing(1_000, gelderland, overijssel),
        Crossing(1_300, overijssel, gelderland),
        Crossing(1_600, gelderland, overijssel),
      ),
    )
    // Far apart, both count.
    assertEquals(
      2,
      crossingsOf(Crossing(1_000, gelderland, overijssel), Crossing(3_000, overijssel, gelderland))
        .size,
    )
  }

  private companion object {
    const val LATITUDE = 51.5
    const val LONGITUDE = 6.0
    const val METERS_PER_DEGREE_LATITUDE = 6_371_008.8 * PI / 180
    val metersPerDegreeLongitude = METERS_PER_DEGREE_LATITUDE * cos(LATITUDE * PI / 180)
  }
}
