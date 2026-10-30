package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers `knot-navigation` "Progress strip", "Distances and times ahead", "Rounded times" and the
 * progress line of "Navigation with the screen off".
 *
 * The knots lie on a straight road from west to east, as far apart as their connections are long,
 * with straight courses. The time is counted in minutes from midnight of Wednesday, 16 June 2027.
 */
class RouteProgressTest {

  private val day = CalendarDay(2027, 6, 16)

  private fun localTime(millis: Long): LocalMoment {
    val minutes = (millis / 60_000).toInt()
    return LocalMoment(day.plusDays(minutes / MINUTES_PER_DAY), minutes % MINUTES_PER_DAY)
  }

  private class Ride(val knots: List<Knot>, val graph: NetworkGraph, val stops: Set<Int>) {

    val offsets = knots.map { (it.longitude - BASE_LONGITUDE) * metersPerDegreeLongitude }

    /** A fix [meters] along the road, [north] of it, at [time], e.g. `18:00`. */
    fun fix(meters: Double, time: String, north: Double = 0.0): Fix {
      val (hours, minutes) = time.split(":").map { it.toLong() }
      return Fix(
        BASE_LATITUDE + north / METERS_PER_DEGREE_LATITUDE,
        BASE_LONGITUDE + meters / metersPerDegreeLongitude,
        5.0,
        (hours * 60 + minutes) * 60_000,
      )
    }
  }

  /**
   * A route through knots with the [refs], connected by [lengths] meters; [stops] are positions,
   * [ferryLegs] the legs with a ferry.
   */
  private fun ride(
    refs: List<String>,
    lengths: List<Int>,
    stops: Set<Int> = setOf(),
    ferryLegs: Set<Int> = setOf(),
    network: NetworkType = NetworkType.CYCLING,
  ): Ride {
    val offsets = lengths.runningFold(0) { sum, length -> sum + length }
    val knots = refs.mapIndexed { index, ref ->
      Knot(
        index + 1L,
        ref,
        network,
        BASE_LATITUDE,
        BASE_LONGITUDE + offsets[index] / metersPerDegreeLongitude,
      )
    }
    val connections = lengths.mapIndexed { index, length ->
      Connection(knots[index].id, knots[index + 1].id, length)
    }
    val ferries = ferryLegs.associate { connections[it] to Ferry(name = "Veer") }
    return Ride(knots, NetworkGraph(knots, connections, ferries), stops)
  }

  /** 24 → 12 (0.8 km) → the stop 30 (5.2 km) → 31 (12.4 km). */
  private val withStop = ride(listOf("24", "12", "30", "31"), listOf(800, 4400, 7200), setOf(2))

  private fun start(ride: Ride, fix: Fix): Navigation =
    assertNotNull(
        Navigation.start(
          ride.knots,
          ride.graph,
          NetworkGeometry.straightLines(ride.graph),
          fix,
          stops = ride.stops,
          localTime = ::localTime,
        )
      )
      .navigation

  private fun rows(navigation: Navigation, fix: Fix) =
    navigation.progress(fix).rows.map {
      listOfNotNull(it.label, it.knot, it.distance, it.time, it.arrival).joinToString(" · ")
    }

  // Requirement: Distances and times ahead

  @Test
  fun `ahead at the start`() {
    val fix = withStop.fix(0.0, "18:20")

    assertEquals(
      listOf(
        "Next knot · 12 · 800 m · 3 min",
        "Next stop · 30 · 5.2 km · 20 min · 18:40",
        "End · 31 · 12.4 km · 47 min · 19:05",
      ),
      rows(start(withStop, fix), fix),
    )
  }

  @Test
  fun `along the way`() {
    val navigation = start(withStop, withStop.fix(0.0, "18:20"))
    val halfway = withStop.fix(400.0, "18:21")

    val rows = rows(navigation.update(halfway).navigation, halfway)

    assertEquals("Next knot · 12 · 400 m · 2 min", rows[0])
    assertTrue(rows[1].startsWith("Next stop · 30 · 4.8 km"), rows[1])
    assertTrue(rows[2].startsWith("End · 31 · 12.0 km"), rows[2])
  }

  @Test
  fun `the rider's own pace`() {
    val ride = ride(listOf("10", "11", "12"), listOf(10_000, 10_000))
    val started = start(ride, ride.fix(0.0, "17:10"))
    val atKnot = ride.fix(10_000.0, "18:00")

    val navigation = started.update(atKnot).navigation

    assertEquals("End · 12 · 10.0 km · 50 min · 18:50", rows(navigation, atKnot).last())
  }

  @Test
  fun `long time`() {
    val ride = ride(listOf("10", "11"), listOf(40_000))
    val fix = ride.fix(0.0, "12:00")

    assertEquals(
      listOf("Next knot · 11 · 40.0 km · 2 h 30 min", "End · 11 · 40.0 km · 2 h 30 min · 14:30"),
      rows(start(ride, fix), fix),
    )
  }

  @Test
  fun `no stop left`() {
    val started = start(withStop, withStop.fix(0.0, "18:20"))
    val atStop = withStop.fix(5200.0, "18:40")
    val passed = started.update(withStop.fix(800.0, "18:23")).navigation.update(atStop).navigation

    assertEquals(listOf("Next knot", "End"), passed.progress(atStop).rows.map { it.label })
  }

  @Test
  fun `next knot is the next stop`() {
    val ride = ride(listOf("24", "30", "31"), listOf(2000, 3000), setOf(1))
    val fix = ride.fix(0.0, "10:00")

    val rows = start(ride, fix).progress(fix).rows

    assertEquals(listOf("30", "30", "31"), rows.map { it.knot })
    assertEquals(rows[0].distance, rows[1].distance)
  }

  @Test
  fun `round trip`() {
    val ride = ride(listOf("24", "12", "30", "24"), listOf(1000, 2000, 3000))
    val fix = ride.fix(0.0, "10:00")

    val end = start(ride, fix).progress(fix).rows.last()

    assertEquals("24", end.knot)
    assertEquals("6.0 km", end.distance)
  }

  @Test
  fun `off route ahead`() {
    val navigation = start(withStop, withStop.fix(0.0, "18:20"))
    val offRoute = withStop.fix(800.0, "18:24", north = 300.0)

    val update = navigation.update(offRoute).navigation
    val rows = update.progress(offRoute).rows

    assertTrue(update.offRoute)
    assertEquals(ProgressRow("Next knot", "12", "300 m", null, null), rows[0])
    assertEquals("4.7 km", rows[1].distance, "4.4 km from 12 plus 300 m")
    assertEquals("11.9 km", rows[2].distance)
  }

  // Requirement: Progress strip

  @Test
  fun `strip at the start`() {
    val fix = withStop.fix(0.0, "18:20")

    val strip = start(withStop, fix).progress(fix).strip

    assertEquals(
      listOf(
        ProgressMark(ProgressMarkKind.START, "24", 0.0),
        ProgressMark(ProgressMarkKind.KNOT, "12", 800.0 / 12_400),
        ProgressMark(ProgressMarkKind.STOP, "30", 5200.0 / 12_400),
        ProgressMark(ProgressMarkKind.END, "31", 1.0),
      ),
      strip.marks,
    )
    assertEquals(0.0, strip.rider)
  }

  @Test
  fun `moving along`() {
    val started = start(withStop, withStop.fix(0.0, "18:20"))
    val fix = withStop.fix(4000.0, "18:35")

    val navigation = started.update(withStop.fix(800.0, "18:23")).navigation.update(fix).navigation

    assertEquals(4000.0 / 12_400, navigation.progress(fix).strip.rider, 0.001)
  }

  @Test
  fun `ferry on the strip`() {
    val ride = ride(listOf("24", "12", "30", "31"), listOf(800, 4400, 7200), setOf(2), setOf(1))
    val fix = ride.fix(0.0, "18:20")

    val ferry =
      start(ride, fix).progress(fix).strip.marks.single { it.kind == ProgressMarkKind.FERRY }

    assertEquals("⛴", ferry.label)
    assertEquals(3000.0 / 12_400, ferry.fraction, 0.001)
  }

  @Test
  fun `long route`() {
    val refs = (1..82).map { "$it" }
    val ride = ride(refs, List(81) { 500 }, setOf(27, 54))
    val fix = ride.fix(0.0, "10:00")

    val kinds = start(ride, fix).progress(fix).strip.marks.groupingBy { it.kind }.eachCount()

    assertEquals(
      mapOf(
        ProgressMarkKind.START to 1,
        ProgressMarkKind.KNOT to 78,
        ProgressMarkKind.STOP to 2,
        ProgressMarkKind.END to 1,
      ),
      kinds,
    )
  }

  @Test
  fun `off route on the strip`() {
    val started = start(withStop, withStop.fix(0.0, "18:20"))
    val onRoute = withStop.fix(400.0, "18:21")
    val offRoute = withStop.fix(600.0, "18:22", north = 300.0)

    val navigation = started.update(onRoute).navigation.update(offRoute).navigation
    val strip = navigation.progress(offRoute).strip

    assertTrue(strip.offRoute)
    assertEquals(400.0 / 12_400, strip.rider, 0.001, "the last place on the route")
  }

  @Test
  fun `progress with a screen reader`() {
    val started = start(withStop, withStop.fix(0.0, "18:20"))
    val fix = withStop.fix(4000.0, "18:35")

    val navigation = started.update(withStop.fix(800.0, "18:23")).navigation.update(fix).navigation

    assertEquals("4.0 of 12.4 km ridden, next knot 30", navigation.progress(fix).strip.description)
  }

  // Requirement: Navigation with the screen off

  @Test
  fun `progress outside the app`() {
    val started = start(withStop, withStop.fix(0.0, "18:20"))
    assertEquals("800 m to 12 · end around 19:05", started.progressLine)

    val atKnot = started.update(withStop.fix(800.0, "18:23")).navigation

    assertEquals("4.4 km to 30 · end around 19:05", atKnot.progressLine)
  }

  @Test
  fun `the progress line does not change between knots`() {
    val started = start(withStop, withStop.fix(0.0, "18:20"))

    val moved = started.update(withStop.fix(400.0, "18:22")).navigation

    assertEquals(started.progressLine, moved.progressLine)
  }

  @Test
  fun `progress line before the end`() {
    val ride = ride(listOf("10", "11"), listOf(800))

    assertEquals("800 m to the end, around 10:05", start(ride, ride.fix(0.0, "10:00")).progressLine)
  }

  @Test
  fun `no arrivals without the local time`() {
    val fix = withStop.fix(0.0, "18:20")
    val navigation =
      assertNotNull(
          Navigation.start(withStop.knots, withStop.graph, null, fix, stops = withStop.stops)
        )
        .navigation

    assertNull(navigation.progress(fix).rows.last().arrival)
    assertEquals("800 m to 12", navigation.progressLine)
  }

  // Requirement: Rounded times

  @Test
  fun `short time`() {
    assertEquals("20 min", RouteProgress.formatDuration(19 * 60 + 30.0))
  }

  @Test
  fun `very short time`() {
    assertEquals("1 min", RouteProgress.formatDuration(20.0))
  }

  @Test
  fun `long time rounded`() {
    assertEquals("2 h 35 min", RouteProgress.formatDuration(2 * 3600 + 33 * 60.0))
  }

  @Test
  fun `arrival rounded`() {
    val now = (19 * 60 + 6) * 60_000L

    assertEquals("19:05", RouteProgress.arrivalText(now, 30.0, ::localTime))
  }

  // map-planning, Requirement: Show the route

  @Test
  fun `hiking time`() {
    assertEquals(
      "47 min",
      RouteProgress.formatDuration(3500 / RouteProgress.assumedMetersPerSecond(NetworkType.HIKING)),
    )
  }

  @Test
  fun `long route time`() {
    assertEquals(
      "2 h 30 min",
      RouteProgress.formatDuration(
        40_000 / RouteProgress.assumedMetersPerSecond(NetworkType.CYCLING)
      ),
    )
  }

  @Test
  fun `stop positions of a route`() {
    val knots =
      listOf(KnotRole.START, KnotRole.PASSED, KnotRole.STOP, KnotRole.END).mapIndexed { index, role
        ->
        RouteKnot(withStop.knots[index], role)
      }

    assertEquals(setOf(2), RouteDescription.Complete(knots, "12.4 km").stopPositions)
  }

  private companion object {
    const val MINUTES_PER_DAY = 24 * 60
    const val BASE_LATITUDE = 51.5
    const val BASE_LONGITUDE = 6.0
    const val METERS_PER_DEGREE_LATITUDE = 6_371_008.8 * PI / 180
    val metersPerDegreeLongitude = METERS_PER_DEGREE_LATITUDE * cos(BASE_LATITUDE * PI / 180)
  }
}
