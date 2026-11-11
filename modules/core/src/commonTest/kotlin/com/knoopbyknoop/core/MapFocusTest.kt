package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Covers `knot-navigation` "Zoom while following", "Zoom at a knot", "Zoom out after passing a
 * knot" and "Zoom at a tricky spot" as far as the core decides them.
 *
 * The knots lie on a straight road from west to east, 1000 m apart; 46 is 150 m after 45. A zigzag
 * can be put on the course of 45-12 at [zigzagAt] m: 80° to the right, 40 m on 80° to the left.
 *
 * ```
 *   24 ------ 45 -46- ~~zigzag~~ ------ 12 ------ 31
 *   0 m      1000 m                     2000 m    3000 m
 * ```
 */
class MapFocusTest {

  private val knot24 = knot(24, "24", east = 0.0)
  private val knot45 = knot(45, "45", east = 1000.0)
  private val knot46 = knot(46, "46", east = 1150.0)
  private val knot12 = knot(12, "12", east = 2000.0)
  private val knot31 = knot(31, "31", east = 3000.0)

  private val graph =
    NetworkGraph(
      knots = listOf(knot24, knot45, knot46, knot12, knot31),
      connections =
        listOf(
          Connection(24, 45, 1000),
          Connection(45, 12, 1000),
          Connection(12, 31, 1000),
          Connection(45, 46, 150),
          Connection(46, 12, 850),
        ),
    )

  private val straight = NetworkGeometry.straightLines(graph)

  /** The network with a zigzag on the course of 45-12 starting [zigzagAt] m east. */
  private fun zigzag(zigzagAt: Double): NetworkGeometry {
    val courses = straight.courses.toMutableList()
    courses[1] =
      listOf(
        at(1000.0),
        at(zigzagAt),
        at(zigzagAt + 7.0, -40.0),
        at(zigzagAt + 47.0, -40.0),
        at(zigzagAt + 54.0),
        at(2000.0),
      )
    return NetworkGeometry(graph, courses, mapOf())
  }

  private var time = 0L

  private fun fix(east: Double, north: Double = 0.0): Fix {
    time += 5_000
    return Fix(latitude(north), longitude(east), 5.0, time)
  }

  private fun at(east: Double, north: Double = 0.0) = GeoPoint(latitude(north), longitude(east))

  private fun start(
    knots: List<Knot> = listOf(knot24, knot45, knot12, knot31),
    geometry: NetworkGeometry? = straight,
  ) = assertNotNull(Navigation.start(knots, graph, geometry, fix(0.0))).navigation

  /** Rides east along the road through [easts], returning the navigation and its last focus. */
  private fun Navigation.ride(vararg easts: Double): Pair<Navigation, MapFocus> {
    var navigation = this
    var last = MapFocus.BASE
    for (east in easts) {
      val fix = fix(east)
      navigation = navigation.update(fix).navigation
      last = navigation.mapFocus(fix)
    }
    return navigation to last
  }

  private fun near(expected: GeoPoint, actual: GeoPoint) {
    assertEquals(expected.latitude, actual.latitude, 1e-6)
    assertEquals(expected.longitude, actual.longitude, 1e-6)
  }

  // Requirement: Zoom while following

  @Test
  fun `riding between knots`() {
    val (_, focus) = start().ride(200.0, 300.0, 400.0)

    assertEquals(MapFocus.BASE, focus)
  }

  @Test
  fun `tricky spot at a knot`() {
    val navigation = start(geometry = zigzag(1050.0))

    val (atReminder, knotFocus) = navigation.ride(500.0, 820.0)
    val (_, trickyFocus) = atReminder.ride(940.0)

    assertEquals(MapFocusKind.KNOT, knotFocus.kind)
    assertEquals(MapFocusKind.TRICKY_SPOT, trickyFocus.kind)
  }

  @Test
  fun `off route`() {
    val (_, focus) =
      start().ride(300.0, 400.0).first.ride(450.0).first.let { navigation ->
        val away = fix(500.0, 300.0)
        val off = navigation.update(away).navigation
        off to off.mapFocus(away)
      }

    assertEquals(MapFocusKind.OFF_ROUTE, focus.kind)
    assertEquals(1, focus.points.size)
  }

  // Requirement: Zoom at a knot

  @Test
  fun `approaching a knot`() {
    val (_, focus) = start().ride(500.0, 820.0)

    assertEquals(MapFocusKind.KNOT, focus.kind)
    assertEquals(MapFocus.KNOT_CLOSEST_METERS, focus.closestMeters)
    assertEquals(2, focus.points.size)
    near(at(1000.0), focus.points[0])
    near(at(1100.0), focus.points[1])
  }

  @Test
  fun `coming closer`() {
    val (atReminder, first) = start().ride(500.0, 820.0)

    val (_, closer) = atReminder.ride(900.0, 950.0)

    assertEquals(first, closer)
  }

  @Test
  fun `knot at the end of the route`() {
    val (_, focus) = start().ride(500.0, 1000.0, 1500.0, 2000.0, 2500.0, 2820.0)

    assertEquals(MapFocusKind.KNOT, focus.kind)
    assertEquals(1, focus.points.size)
    near(at(3000.0), focus.points.single())
  }

  @Test
  fun `without courses the map zooms at knots only`() {
    val (_, focus) = start(geometry = null).ride(500.0, 820.0)

    assertEquals(MapFocusKind.KNOT, focus.kind)
  }

  // Requirement: Zoom out after passing a knot

  @Test
  fun `next knot 800 m ahead`() {
    val (passed, focus) = start().ride(500.0, 820.0, 1010.0)
    // Three fixes of 5 s later, past the 10 s, and short of 46, which is not on this route.
    val (_, later) = passed.ride(1050.0, 1080.0, 1100.0)

    assertEquals(MapFocusKind.AFTER_KNOT, focus.kind)
    near(at(2000.0), focus.points.single())
    assertEquals(MapFocus.AFTER_KNOT_FARTHEST_METERS, focus.farthestMeters)
    assertEquals(MapFocus.BASE, later)
  }

  @Test
  fun `next knot close`() {
    val (_, focus) =
      start(knots = listOf(knot24, knot45, knot46, knot12, knot31)).ride(500.0, 820.0, 1010.0)

    assertEquals(MapFocusKind.KNOT, focus.kind)
    assertEquals(2, focus.key)
    near(at(1150.0), focus.points.first())
  }

  @Test
  fun `tricky spot right after the knot`() {
    val (_, focus) = start(geometry = zigzag(1060.0)).ride(500.0, 820.0, 1010.0)

    assertEquals(MapFocusKind.TRICKY_SPOT, focus.kind)
  }

  @Test
  fun `last knot passed`() {
    val (_, focus) = start().ride(500.0, 1000.0, 1500.0, 2000.0, 2500.0, 2820.0, 3000.0)

    assertEquals(MapFocus.BASE, focus)
  }

  // Scenario: Next knot far ahead
  @Test
  fun `the view spans twice the farthest point within the limits`() {
    val rider = Fix(latitude(0.0), longitude(0.0), 5.0, 0)
    val knot = MapFocus(MapFocusKind.KNOT, 1, listOf(at(100.0), at(250.0)), 300.0, null)
    val near = MapFocus(MapFocusKind.KNOT, 1, listOf(at(50.0)), 300.0, null)
    val after = MapFocus(MapFocusKind.AFTER_KNOT, 1, listOf(at(6000.0)), null, 3000.0)

    assertEquals(500.0, assertNotNull(knot.acrossMeters(rider)), 1.0)
    assertEquals(300.0, near.acrossMeters(rider))
    assertEquals(3000.0, after.acrossMeters(rider))
    assertEquals(null, MapFocus.BASE.acrossMeters(rider))
    val view = assertNotNull(knot.viewAround(rider))
    assertEquals(500.0, (view.maxLatitude - view.minLatitude) * METERS_PER_DEGREE, 1.0)
  }

  // Requirement: Zoom at a tricky spot

  @Test
  fun `approaching a zigzag`() {
    val (_, focus) = start(geometry = zigzag(1500.0)).ride(500.0, 1000.0, 1200.0, 1360.0)

    assertEquals(MapFocusKind.TRICKY_SPOT, focus.kind)
    assertEquals(MapFocus.TRICKY_CLOSEST_METERS, focus.closestMeters)
    // Both turns and 30 m after the second.
    near(at(1500.0), focus.points.first())
    assertEquals(true, focus.points.any { it == at(1507.0, -40.0) })
    assertEquals(true, focus.points.any { it == at(1547.0, -40.0) })
  }

  @Test
  fun `past the spot`() {
    val zigzagged = start(geometry = zigzag(1500.0)).ride(500.0, 1000.0, 1200.0, 1360.0).first

    val fix = fix(1640.0)
    val past = zigzagged.update(fix).navigation

    assertEquals(MapFocus.BASE, past.mapFocus(fix))
  }

  // Doubtful junctions on the guided way

  /** Off route 300 m north of the road at 500 m, after riding it to 450 m. */
  private fun offRouteNorth(): Pair<Navigation, Fix> {
    val (onRoad, _) = start().ride(200.0, 300.0, 400.0, 450.0)
    var navigation = onRoad
    var last = fix(500.0, 150.0)
    for (north in listOf(150.0, 300.0, 300.0)) {
      last = fix(500.0, north)
      navigation = navigation.update(last).navigation
    }
    return navigation to last
  }

  /**
   * A guided way from the rider at 500 m, 300 m north, east to 1500 m and then down to the road,
   * 1.3 km long, with a doubtful junction at [junctionAt] m along it.
   */
  private fun guidedWay(junctionAt: Double): GuidedWay {
    val points = listOf(at(500.0, 300.0), at(1500.0, 300.0), at(1500.0, 0.0))
    val meters = listOf(0.0, 1000.0, 1300.0)
    val junction = DoubtfulJunction(junctionAt, junctionAt, setOf(JunctionDoubt.SIMILAR_EXIT))
    return GuidedWay.of(PathWay(points, meters, listOf(), doubtful = listOf(junction)))
  }

  @Test
  fun `doubtful junction off route`() {
    val (navigation, fix) = offRouteNorth()
    assertEquals(true, navigation.offRoute)

    val focus = navigation.mapFocus(fix, guidedWay(junctionAt = 120.0))

    assertEquals(MapFocusKind.TRICKY_SPOT, focus.kind)
    assertEquals(MapFocus.TRICKY_CLOSEST_METERS, focus.closestMeters)
    near(at(620.0, 300.0), focus.points.first())
  }

  @Test
  fun `doubtful junction on the way back`() {
    val (navigation, fix) = offRouteNorth()

    val far = navigation.mapFocus(fix, guidedWay(junctionAt = 400.0))
    val close = navigation.mapFocus(fix, guidedWay(junctionAt = 140.0))

    // 400 m ahead it is not yet in view: the rider and the end of the way are.
    assertEquals(MapFocusKind.OFF_ROUTE, far.kind)
    // Within 150 m, the junction wins, and the end of the way at 1500 m may leave the view.
    assertEquals(MapFocusKind.TRICKY_SPOT, close.kind)
    assertEquals(true, close.points.none { it.longitude > longitude(1000.0) })
  }

  private companion object {
    const val METERS_PER_DEGREE = 6_371_008.8 * PI / 180

    fun latitude(north: Double) = 52.0 + north / METERS_PER_DEGREE

    fun longitude(east: Double) = 5.0 + east / (METERS_PER_DEGREE * cos(52.0 * PI / 180))

    fun knot(id: Long, ref: String, east: Double) =
      Knot(id, ref, NetworkType.CYCLING, latitude(0.0), longitude(east))
  }
}
