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

  /** The distances and arrivals on the strip by the marks' labels, e.g. `31` to `12.4 km 19:05`. */
  private fun labels(navigation: Navigation, fix: Fix): Map<String, String> =
    navigation
      .progress(fix)
      .strip
      .marks
      .filter { it.distance != null }
      .associate { it.label to listOfNotNull(it.distance, it.arrival).joinToString(" ") }

  // Requirement: Distances and arrivals on the strip

  @Test
  fun `ahead at the start`() {
    val fix = withStop.fix(0.0, "18:20")

    assertEquals(
      mapOf("30" to "5.2 km 18:40", "31" to "12 km 19:05"),
      labels(start(withStop, fix), fix),
    )
  }

  @Test
  fun `along the way`() {
    val navigation = start(withStop, withStop.fix(0.0, "18:20"))
    val halfway = withStop.fix(400.0, "18:21")

    val labels = labels(navigation.update(halfway).navigation, halfway)

    assertTrue(labels.getValue("30").startsWith("4.8 km"), labels.toString())
    assertTrue(labels.getValue("31").startsWith("12 km"), labels.toString())
  }

  @Test
  fun `the rider's own pace`() {
    val ride = ride(listOf("10", "11", "12"), listOf(10_000, 10_000))
    val started = start(ride, ride.fix(0.0, "17:10"))
    val atKnot = ride.fix(10_000.0, "18:00")

    val navigation = started.update(atKnot).navigation

    assertEquals(mapOf("12" to "10 km 18:50"), labels(navigation, atKnot))
  }

  @Test
  fun `long time`() {
    val ride = ride(listOf("10", "11"), listOf(40_000))
    val fix = ride.fix(0.0, "12:00")

    assertEquals(mapOf("11" to "40 km 14:30"), labels(start(ride, fix), fix))
  }

  @Test
  fun `ferry ahead on the strip`() {
    val ride = ride(listOf("24", "12", "30", "31"), listOf(2900, 200, 1000), ferryLegs = setOf(1))
    val fix = ride.fix(0.0, "18:20")

    assertEquals("3.0 km 18:30", labels(start(ride, fix), fix)["⛴"])
  }

  @Test
  fun `stop passed`() {
    val started = start(withStop, withStop.fix(0.0, "18:20"))
    val atStop = withStop.fix(5200.0, "18:40")
    val passed = started.update(withStop.fix(800.0, "18:23")).navigation.update(atStop).navigation

    assertEquals(setOf("31"), labels(passed, atStop).keys)
  }

  @Test
  fun `no distance for the start and plain knots`() {
    val fix = withStop.fix(0.0, "18:20")
    val marks = start(withStop, fix).progress(fix).strip.marks

    assertNull(marks.single { it.label == "24" }.distance)
    assertNull(marks.single { it.label == "12" }.distance)
  }

  @Test
  fun `round trip`() {
    val ride = ride(listOf("24", "12", "30", "24"), listOf(1000, 2000, 3000))
    val fix = ride.fix(0.0, "10:00")

    val end = start(ride, fix).progress(fix).strip.marks.single { it.kind == ProgressMarkKind.END }

    assertEquals("24", end.label)
    assertEquals("6.0 km", end.distance)
  }

  @Test
  fun `off route on the strip ahead`() {
    val navigation = start(withStop, withStop.fix(0.0, "18:20"))
    val offRoute = withStop.fix(800.0, "18:24", north = 300.0)

    val update = navigation.update(offRoute).navigation
    val labels = labels(update, offRoute)

    assertTrue(update.offRoute)
    assertTrue(labels.getValue("30").startsWith("4.7 km"), "4.4 km from 12 plus 300 m")
    assertTrue(labels.getValue("31").startsWith("12 km"))
  }

  // Requirement: Ferries on the strip

  private val renkum = Ferry(name = "Renkumse Veer", openingHours = "Apr-Oct: Mo-Su 07:00-19:00")

  private fun ferryRide(): Ride {
    val ride = ride(listOf("24", "12", "30", "31"), listOf(2900, 200, 1000), setOf(2), setOf(1))
    val connection = ride.graph.connections[1]
    return Ride(
      ride.knots,
      NetworkGraph(ride.knots, ride.graph.connections, mapOf(connection to renkum)),
      ride.stops,
    )
  }

  private fun ferryMark(at: String): ProgressMark {
    val ride = ferryRide()
    val fix = ride.fix(0.0, at)
    return start(ride, fix).progress(fix).strip.marks.single { it.kind == ProgressMarkKind.FERRY }
  }

  @Test
  fun `closed ferry ahead`() {
    val ferry = ferryMark("18:55")

    assertEquals(1, ferry.position)
    assertEquals(FerryArrivalState.WARNING, ferry.state, "at 19:06, closed from 19:00")
  }

  @Test
  fun `open ferry ahead`() {
    val ferry = ferryMark("14:20")

    assertEquals(FerryArrivalState.OPEN, ferry.state)
    assertEquals("14:30", ferry.arrival)
  }

  @Test
  fun `passed ferry has no state`() {
    val ride = ferryRide()
    val started = start(ride, ride.fix(0.0, "14:20"))
    val after = ride.fix(3100.0, "14:35")
    val passed = started.update(ride.fix(2900.0, "14:30")).navigation.update(after).navigation

    val ferry = passed.progress(after).strip.marks.single { it.kind == ProgressMarkKind.FERRY }

    assertNull(ferry.state)
    assertNull(ferry.distance)
  }

  @Test
  fun `ferry with a screen reader`() {
    assertEquals(
      "at 21:10 closed until 1 May 10:00",
      FerryArrival("At 21:10: closed until 1 May 10:00", FerryArrivalState.WARNING)
        .accessibilityText,
    )
    assertEquals("at 14:30", FerryArrival("At 14:30", FerryArrivalState.UNKNOWN).accessibilityText)
  }

  // Requirement: Number to follow

  @Test
  fun `distance to the next knot`() {
    val fix = withStop.fix(0.0, "18:20")

    assertEquals("800 m", start(withStop, fix).progress(fix).nextKnotDistance)
  }

  @Test
  fun `distance off route`() {
    val navigation = start(withStop, withStop.fix(0.0, "18:20"))
    val offRoute = withStop.fix(800.0, "18:24", north = 300.0)

    assertEquals(
      "300 m",
      navigation.update(offRoute).navigation.progress(offRoute).nextKnotDistance,
    )
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
      strip.marks.map { it.copy(distance = null, arrival = null) },
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

    assertNull(navigation.progress(fix).strip.marks.last().arrival)
    assertEquals("800 m to 12", navigation.progressLine)
  }

  // Requirement: Rounded times and distances

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
  fun `short distance`() {
    assertEquals("800 m", MapFeatures.formatDistance(800))
  }

  @Test
  fun `middle distance`() {
    assertEquals("3.5 km", MapFeatures.formatDistance(3456))
  }

  @Test
  fun `long distance`() {
    assertEquals("200 km", MapFeatures.formatDistance(200_200))
    assertEquals("10 km", MapFeatures.formatDistance(9960))
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
