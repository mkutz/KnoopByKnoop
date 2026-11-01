package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers `knot-navigation` "Off route" (the directions), "Direction to the route" and "Direction
 * outside the app".
 *
 * The route runs from west to east on a straight road, its knots 1000 m apart; the rider leaves it
 * to the north.
 *
 * ```
 *              rider, 2000 m north of 45
 *
 *   24 ------ 45 ------ 12 ------ 31
 *   0 m      1000 m    2000 m    3000 m
 * ```
 */
class GuidanceTest {

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

  private var time = 0L

  private fun fix(east: Double, north: Double = 0.0): Fix {
    time += 5_000
    return Fix(latitude(north), longitude(east), 5.0, time)
  }

  /** Rides along the fixes, collecting every notice. */
  private fun Navigation.ride(vararg fixes: Fix): Pair<Navigation, List<Notice>> =
    fixes.fold(this to listOf()) { (navigation, notices), fix ->
      val update = navigation.update(fix)
      update.navigation to notices + update.notices
    }

  /** Started at 24, ridden to 300 m and then 2000 m north of 45. */
  private fun offRoute(): Pair<Navigation, List<Notice>> {
    val started =
      assertNotNull(
          Navigation.start(
            listOf(knot24, knot45, knot12, knot31),
            graph,
            NetworkGeometry.straightLines(graph),
            fix(0.0),
          )
        )
        .navigation
    return started.ride(fix(300.0), fix(1000.0, north = 2000.0))
  }

  // Requirement: Off route

  @Test
  fun `the off-route notice tells the way`() {
    val (navigation, notices) = offRoute()

    assertTrue(navigation.offRoute)
    assertEquals(listOf("Off route, next 45, 2.0 km to the south"), notices.map { it.text })
  }

  @Test
  fun `getting closer`() {
    val (navigation, _) = offRoute()

    val (_, notices) = navigation.ride(fix(1000.0, north = 1600.0), fix(1000.0, north = 1400.0))

    assertEquals(listOf("Next 45, 1.4 km to the south"), notices.map { it.text })
    assertEquals(NoticeKind.GUIDANCE, notices.single().kind)
  }

  @Test
  fun `direction changes`() {
    val (navigation, _) = offRoute()

    val (_, notices) = navigation.ride(fix(2500.0, north = 1400.0))

    assertEquals(listOf("Next 45, 2.1 km to the south-west"), notices.map { it.text })
  }

  @Test
  fun `riding around without getting closer`() {
    val (navigation, _) = offRoute()

    val (_, notices) =
      navigation.ride(
        fix(1100.0, north = 1900.0),
        fix(900.0, north = 1900.0),
        fix(1000.0, north = 2000.0),
      )

    assertEquals(listOf(), notices)
  }

  @Test
  fun `off route is announced once`() {
    val (navigation, first) = offRoute()

    val (_, notices) =
      navigation.ride(
        fix(1000.0, north = 1400.0),
        fix(1000.0, north = 800.0),
        fix(1000.0, north = 200.0),
      )

    assertEquals(1, (first + notices).count { it.kind == NoticeKind.OFF_ROUTE })
    assertTrue(notices.all { it.kind == NoticeKind.GUIDANCE })
  }

  // Requirement: Direction to the route

  @Test
  fun `direction in the header`() {
    val (navigation, _) = offRoute()

    val guidance = assertNotNull(navigation.guidance(fix(1000.0, north = 2000.0)))

    assertEquals("2.0 km", guidance.distance)
    assertEquals(180.0, guidance.bearing, 0.5)
    assertEquals("south", guidance.direction)
    assertEquals("2.0 km to the south", guidance.text)
  }

  @Test
  fun `no guidance on route`() {
    val started =
      assertNotNull(Navigation.start(listOf(knot24, knot45), graph, null, fix(0.0))).navigation

    assertNull(started.guidance(fix(100.0)))
  }

  @Test
  fun `compass directions`() {
    assertEquals(
      listOf(
        "north",
        "north-east",
        "east",
        "south-east",
        "south",
        "south-west",
        "west",
        "north-west",
        "north",
        "north",
        "north-east",
      ),
      listOf(0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0, 350.0, 22.0, 23.0).map(::compass),
    )
  }

  // Requirement: Direction outside the app

  @Test
  fun `lock screen off route`() {
    val (navigation, _) = offRoute()

    assertEquals("2.0 km south to 45", navigation.progressLine)
  }

  @Test
  fun `updated on the way`() {
    val (navigation, _) = offRoute()

    val (closer, _) = navigation.ride(fix(1000.0, north = 1950.0))
    assertEquals("2.0 km south to 45", closer.progressLine, "only 50 m closer")

    val (closerStill, _) = closer.ride(fix(1000.0, north = 1880.0))
    assertEquals("1.9 km south to 45", closerStill.progressLine)
  }

  @Test
  fun `back on route outside the app`() {
    val (navigation, _) = offRoute()

    val (back, notices) = navigation.ride(fix(900.0, north = 20.0))

    assertFalse(back.offRoute)
    assertTrue("Back on route, next 45" in notices.map { it.text }, notices.toString())
    assertEquals("100 m to 45", back.progressLine)
  }

  private companion object {
    const val ORIGIN_LATITUDE = 52.0
    const val ORIGIN_LONGITUDE = 5.0
    const val METERS_PER_DEGREE = 6_371_008.8 * PI / 180

    fun latitude(north: Double) = ORIGIN_LATITUDE + north / METERS_PER_DEGREE

    fun longitude(east: Double) =
      ORIGIN_LONGITUDE + east / (METERS_PER_DEGREE * cos(ORIGIN_LATITUDE * PI / 180))

    fun knot(id: Long, ref: String, east: Double, north: Double = 0.0) =
      Knot(id, ref, NetworkType.CYCLING, latitude(north), longitude(east))
  }
}
