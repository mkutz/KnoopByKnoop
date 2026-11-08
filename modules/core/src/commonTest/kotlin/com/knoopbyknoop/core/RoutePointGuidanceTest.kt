package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers `knot-navigation` "Guidance to a route point off the network" and "Guidance from a route
 * point onto the network".
 *
 * The knots lie on a straight road from west to east, 1000 m apart. The way to the end flag leaves
 * the road at 1500 m, on the course of 45-12, turning left to the north for 200 m, then left again
 * to the west for 150 m.
 *
 * ```
 *             E ------+
 *                     |
 *   24 ------ 45 ---- + ---- 12 ------ 31
 *   0 m      1000 m 1500 m  2000 m    3000 m
 * ```
 */
class RoutePointGuidanceTest {

  private val knot24 = knot(24, "24", east = 0.0)
  private val knot45 = knot(45, "45", east = 1000.0)
  private val knot12 = knot(12, "12", east = 2000.0)
  private val knot31 = knot(31, "31", east = 3000.0)

  private val graph =
    NetworkGraph(
      knots = listOf(knot24, knot45, knot12, knot31),
      connections =
        listOf(Connection(24, 45, 1000), Connection(45, 12, 1000), Connection(12, 31, 1000)),
    )

  private val geometry = NetworkGeometry.straightLines(graph)

  private var time = 0L

  private fun fix(east: Double, north: Double = 0.0): Fix {
    time += 5_000
    return Fix(latitude(north), longitude(east), 5.0, time)
  }

  private fun at(east: Double, north: Double = 0.0) = GeoPoint(latitude(north), longitude(east))

  /** A route point on 45-12 at 1500 m, reached by a way through [way], from the point on. */
  private fun point(vararg way: GeoPoint, straight: Boolean = false): Knot {
    val connection = assertNotNull(graph.connectionBetween(45, 12))
    val place = CoursePlace(connection, 0.5, at(1500.0), 0.0)
    return routePointKnot(way.first(), NetworkType.CYCLING, place, way.toList(), straight)
  }

  /** The end flag of the sketch. */
  private val end = point(at(1350.0, 200.0), at(1500.0, 200.0), at(1500.0))

  private val guide = WayGuide()

  private class Ride(
    val navigation: Navigation,
    val notices: List<Notice>,
    val search: WaySearch? = null,
  )

  private fun start(vararg knots: Knot, east: Double, north: Double = 0.0): Ride {
    val routeGraph = graph.withRoutePoints(knots.filter { it.isRoutePoint })
    val fix = fix(east, north)
    val update =
      assertNotNull(
        Navigation.start(knots.toList(), routeGraph, geometry.withRoutePoints(routeGraph), fix)
      )
    val step = guide.onFix(update.navigation, update.notices, fix, CYCLING, null, time, EN)
    return Ride(update.navigation, step.notices, step.search)
  }

  /** Rides through [fixes]; [found] answers each way search, as the apps do. */
  private fun Ride.through(vararg fixes: Fix, found: (WaySearch) -> PathWay? = { null }): Ride =
    fixes.fold(this) { ride, fix ->
      val update = ride.navigation.update(fix)
      val step = guide.onFix(update.navigation, update.notices, fix, CYCLING, null, time, EN)
      step.search?.let { guide.found(found(it), update.navigation) }
      Ride(update.navigation, ride.notices + step.notices, step.search ?: ride.search)
    }

  private fun Ride.along(
    from: Double,
    to: Double,
    north: Double = 0.0,
    found: (WaySearch) -> PathWay? = { null },
  ): Ride {
    val steps = (kotlin.math.abs(to - from) / 20).toInt()
    val sign = if (to >= from) 1 else -1
    return through(
      *(1..steps).map { fix(from + sign * it * 20.0, north) }.toTypedArray(),
      found = found,
    )
  }

  /** Rides north at [east] from [from] to [to]. */
  private fun Ride.north(
    east: Double,
    from: Double,
    to: Double,
    found: (WaySearch) -> PathWay? = { null },
  ): Ride {
    val steps = (kotlin.math.abs(to - from) / 20).toInt()
    val sign = if (to >= from) 1 else -1
    return through(
      *(1..steps).map { fix(east, from + sign * it * 20.0) }.toTypedArray(),
      found = found,
    )
  }

  private fun Ride.texts(kind: NoticeKind) = notices.filter { it.kind == kind }.map { it.text }

  private fun wayOf(
    vararg points: GeoPoint,
    turns: List<PathTurn> = listOf(),
    firstName: String? = null,
  ): PathWay {
    var total = 0.0
    val meters = points.mapIndexed { index, place ->
      if (index > 0) {
        val before = points[index - 1]
        total += distanceMeters(before.latitude, before.longitude, place.latitude, place.longitude)
      }
      total
    }
    return PathWay(points.toList(), meters, turns, firstName = firstName)
  }

  /** The way from the turn-off to the flag, as the path data would have it. */
  private val toTheFlag =
    wayOf(
      at(1500.0),
      at(1500.0, 200.0),
      at(1350.0, 200.0),
      turns = listOf(PathTurn(200.0, TurnDirection.LEFT, "Kronprinzenstraße")),
      firstName = "Cecilienstraße",
    )

  // Requirement: Guidance to a route point off the network

  @Test
  fun `towards the next knot`() {
    val started = start(knot24, knot45, end, east = 0.0)

    val atFortyFive = started.along(0.0, 1000.0)

    assertEquals(
      listOf("Next 45, then towards 12", "Towards 12, then the end"),
      atFortyFive.texts(NoticeKind.NEXT),
    )
    assertEquals(
      "Towards 12, then the end",
      atFortyFive.notices.last { it.kind == NoticeKind.NEXT }.speech,
    )
    assertEquals(listOf("45 ahead, then towards 12"), atFortyFive.texts(NoticeKind.REMINDER))
  }

  @Test
  fun `towards the next knot in German`() {
    val routeGraph = graph.withRoutePoints(listOf(end))
    val started =
      assertNotNull(
        Navigation.start(
          listOf(knot24, knot45, end),
          routeGraph,
          geometry.withRoutePoints(routeGraph),
          fix(0.0),
          language = Language.DE,
        )
      )

    val atFortyFive = Ride(started.navigation, started.notices).along(0.0, 1000.0)

    assertEquals(
      listOf("Nächster Knotenpunkt 45, dann Richtung 12", "Richtung 12, dann das Ziel"),
      atFortyFive.texts(NoticeKind.NEXT),
    )
  }

  @Test
  fun `leaving the network`() {
    val ride = start(knot45, end, east = 1000.0).along(1000.0, 1500.0) { toTheFlag }

    assertEquals(
      listOf("In 100 m, turn left into Cecilienstraße", "Turn left now into Cecilienstraße"),
      ride.texts(NoticeKind.TURN),
    )
    assertEquals(
      "In 100 meters, turn left into Cecilienstraße",
      ride.notices.first { it.kind == NoticeKind.TURN }.speech,
    )
    assertFalse(ride.navigation.offRoute)
  }

  @Test
  fun `the way after the turn-off is looked for ahead of it`() {
    val ride = start(knot45, end, east = 1000.0).along(1000.0, 1220.0) { toTheFlag }

    val search = assertNotNull(ride.search)
    assertEquals(at(1500.0).latitude, search.from.latitude, 1e-6)
    assertEquals(at(1500.0).longitude, search.from.longitude, 1e-6)
    assertEquals(at(1350.0, 200.0), search.to)
    assertNull(guide.way)
  }

  @Test
  fun `turn by turn to the flag`() {
    val atTurnOff = start(knot45, end, east = 1000.0).along(1000.0, 1500.0) { toTheFlag }

    // 95 m before the turn into Kronprinzenstraße at 105 m.
    val ride =
      atTurnOff.through(
        *listOf(20.0, 45.0, 70.0, 105.0, 130.0, 165.0, 180.0).map { fix(1500.0, it) }.toTypedArray()
      ) {
        toTheFlag
      }

    assertEquals(
      listOf(
        "In 100 m, turn left into Cecilienstraße",
        "Turn left now into Cecilienstraße",
        "In 100 m, turn left",
      ),
      ride.texts(NoticeKind.TURN),
    )
    assertFalse(ride.navigation.offRoute)
    val header =
      assertIs<HeaderRow.ToRoute>(ride.navigation.header(fix(1500.0, 120.0), null, guide.way))
    assertFalse(header.offRoute)
    assertEquals(TurnDirection.LEFT, header.guided?.turn)
    assertEquals("Kronprinzenstraße", header.guided?.street)
  }

  @Test
  fun `missing the turn-off`() {
    val pastTurnOff = start(knot45, end, east = 1000.0).along(1000.0, 1500.0) { toTheFlag }

    val ride = pastTurnOff.along(1500.0, 1600.0) { toTheFlag }

    val search = assertNotNull(ride.search)
    assertEquals(at(1600.0).latitude, search.from.latitude, 1e-6)
    assertEquals(at(1350.0, 200.0), search.to)
  }

  @Test
  fun `a straight line`() {
    val straight = point(at(1500.0, 300.0), at(1500.0), straight = true)
    val ride = start(knot24, knot45, straight, east = 0.0).along(0.0, 1500.0)

    assertEquals(listOf("Next 45, then the end", "On to the end"), ride.texts(NoticeKind.NEXT))
    assertTrue(ride.texts(NoticeKind.TURN).isEmpty())
  }

  @Test
  fun `without path data`() {
    val ride = start(knot45, end, east = 1000.0).along(1000.0, 1500.0) { null }

    assertEquals(
      listOf("In 100 m, turn left", "Turn left now, 228 m to the north-west"),
      ride.texts(NoticeKind.TURN),
    )
    assertNull(guide.way)
  }

  // Requirement: Guidance from a route point onto the network

  @Test
  fun `from the start flag onto the network`() {
    val home = point(at(1500.0, 300.0), at(1500.0))
    val started = start(home, knot12, knot31, east = 1500.0, north = 300.0)
    val search = assertNotNull(started.search)
    assertEquals(at(1500.0), search.to)
    guide.found(
      wayOf(
        at(1500.0, 300.0),
        at(1500.0),
        turns = listOf(PathTurn(155.0, TurnDirection.SLIGHT_RIGHT, "Am Bach")),
      ),
      started.navigation,
    )

    val ride = started.north(1500.0, 300.0, 0.0)

    assertEquals(listOf("Next 12, then 31"), started.notices.map { it.text })
    assertEquals(
      listOf(
        "In 100 m, bear right",
        "In 100 m, turn left onto the route towards 12",
        "Turn left now onto the route towards 12",
      ),
      ride.texts(NoticeKind.TURN),
    )
    assertFalse(ride.navigation.offRoute)
  }

  @Test
  fun `after the join the route goes on as usual`() {
    val home = point(at(1500.0, 300.0), at(1500.0))
    val started = start(home, knot12, knot31, east = 1500.0, north = 300.0)

    val ride = started.north(1500.0, 300.0, 0.0).along(1500.0, 2000.0)

    assertNull(guide.way)
    assertEquals(listOf("Next 12, then 31", "Next 31"), ride.texts(NoticeKind.NEXT))
    assertIs<HeaderRow.End>(ride.navigation.header(fix(2100.0)))
  }

  private companion object {
    val CYCLING = NetworkType.CYCLING
    val EN = Language.EN
    const val METERS_PER_DEGREE = 6_371_008.8 * PI / 180

    fun latitude(north: Double) = 52.0 + north / METERS_PER_DEGREE

    fun longitude(east: Double) = 5.0 + east / (METERS_PER_DEGREE * cos(52.0 * PI / 180))

    fun knot(id: Long, ref: String, east: Double) =
      Knot(id, ref, NetworkType.CYCLING, latitude(0.0), longitude(east))
  }
}
