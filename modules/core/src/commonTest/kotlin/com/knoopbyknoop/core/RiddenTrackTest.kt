package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Covers the riding direction and the ridden track of `knot-navigation`, decided in the core. */
class RiddenTrackTest {

  private var seconds = 0L

  @Test
  fun `riding east for 20 m points east`() {
    val track = ride(fix(east = 0.0), fix(east = 10.0), fix(east = 20.0))
    assertDirection(90.0, track.directionAt(track.points.last()))
  }

  @Test
  fun `after a curve only the most recent movement counts`() {
    val track =
      ride(
        fix(east = 0.0),
        fix(east = 20.0, after = 2),
        fix(east = 20.0, north = 6.0),
        fix(east = 20.0, north = 12.0),
      )
    assertDirection(0.0, track.directionAt(track.points.last()))
  }

  @Test
  fun `standing still has no direction`() {
    val track = ride(fix(east = 0.0), fix(east = 10.0), fix(east = 20.0))
    seconds += 11
    assertNull(track.directionAt(fix(east = 21.0)))
  }

  @Test
  fun `a track too short has no direction`() {
    val track = ride(fix(east = 0.0), fix(east = 6.0))
    assertNull(track.directionAt(track.points.last()))
  }

  @Test
  fun `movement older than 30 s gives no direction`() {
    val first = fix(east = 0.0)
    seconds += 40
    val track = ride(first, fix(east = 6.0), fix(east = 12.0))
    assertNull(track.directionAt(track.points.last()))
  }

  @Test
  fun `fixes are kept in the order they were ridden`() {
    val track = ride(fix(east = 0.0), fix(east = 12.0), fix(east = 24.0))
    assertEquals(listOf(0.0, 12.0, 24.0), track.points.map { east(it) })
  }

  @Test
  fun `an inaccurate fix is not added to the track`() {
    val track = ride(fix(east = 0.0), fix(east = 20.0, accuracy = 60.0))
    assertEquals(listOf(0.0), track.points.map { east(it) })
  }

  @Test
  fun `a fix accurate to 50 m is added to the track`() {
    assertEquals(1, ride(fix(east = 0.0, accuracy = 50.0)).points.size)
  }

  @Test
  fun `standing still adds nothing to the track`() {
    val track = ride(fix(east = 0.0), fix(east = 2.0), fix(east = 4.0), fix(east = 3.0))
    assertEquals(listOf(0.0), track.points.map { east(it) })
  }

  // `smooth-ridden-track`: scatter and spikes, see `knot-navigation` "Ridden track".

  @Test
  fun `standing with scattered fixes`() {
    // Two minutes, a fix every 10 s, scattered up to 20 m around where the hiker stands.
    val corners =
      listOf(20.0 to 0.0, 0.0 to 20.0, -20.0 to 0.0, 0.0 to -20.0, 14.0 to 14.0, -14.0 to -14.0)
    val track =
      ride(
        NetworkType.HIKING,
        *(0 until 12)
          .map {
            corners[it % corners.size].let { (east, north) -> fix(east, north, 20.0, after = 10) }
          }
          .toTypedArray(),
      )

    assertEquals(1, track.points.size)
  }

  @Test
  fun `scatter while walking slowly`() {
    // 200 m east at 1.3 m/s, a fix every 10 s accurate to 30 m, 30 m to either side in turn.
    val track =
      ride(
        NetworkType.HIKING,
        *(0..15)
          .map { fix(it * 13.0, if (it % 2 == 0) 30.0 else -30.0, 30.0, after = 10) }
          .toTypedArray(),
      )

    val easts = track.points.map { east(it) }
    track.points.zipWithNext().forEach { (from, to) ->
      assertTrue(
        distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude) >= 60.0,
        "points at least 60 m apart",
      )
    }
    assertEquals(easts.sorted(), easts, "none west of the one before")
  }

  @Test
  fun `impossible jump`() {
    val track = ride(NetworkType.HIKING, fix(0.0, after = 10), fix(120.0, after = 10))

    assertEquals(listOf(0.0), track.points.map { east(it) })
  }

  @Test
  fun `spike out and back`() {
    // Riding east at 5 m/s with a fix every 10 s; one lands 80 m north.
    val track =
      ride(
        fix(0.0, after = 10),
        fix(50.0, after = 10),
        fix(100.0, north = 80.0, after = 10),
        fix(150.0, after = 10),
        fix(200.0, after = 10),
      )

    assertTrue(track.points.none { it.latitude > ORIGIN_LATITUDE + 40 / METERS_PER_DEGREE })
    assertEquals(listOf(0.0, 50.0, 150.0, 200.0), track.points.map { east(it) })
  }

  @Test
  fun `a right-angle corner stays`() {
    val track =
      ride(fix(0.0, after = 10), fix(50.0, after = 10), fix(50.0, north = 50.0, after = 10))

    assertEquals(3, track.points.size)
  }

  @Test
  fun `riding fast with good fixes`() {
    val track = ride(*(0..60).map { fix(it * 8.0) }.toTypedArray())

    track.points.zipWithNext().forEach { (from, to) -> assertTrue(east(to) - east(from) <= 16.0) }
    assertEquals(480.0, east(track.points.last()))
  }

  @Test
  fun `out and back on the same path`() {
    // 100 m north and back on the same path at 5 m/s, a fix every 10 s, then on east.
    val fixes = buildList {
      add(fix(0.0, after = 10))
      add(fix(50.0, after = 10))
      add(fix(100.0, after = 10))
      add(fix(100.0, north = 50.0, after = 10))
      add(fix(100.0, north = 100.0, after = 10))
      add(fix(100.0, north = 50.0, after = 10))
      add(fix(100.0, after = 10))
      add(fix(150.0, after = 10))
    }
    val track = ride(*fixes.toTypedArray())

    assertTrue(
      track.points.any { it.latitude > ORIGIN_LATITUDE + 90 / METERS_PER_DEGREE },
      "the far end",
    )
    assertEquals(150.0, east(track.points.last()))
  }

  private fun ride(vararg fixes: Fix) = ride(NetworkType.CYCLING, *fixes)

  private fun ride(network: NetworkType, vararg fixes: Fix) =
    fixes.fold(RiddenTrack(network = network)) { track, fix -> track.add(fix) }

  /** A fix [after] seconds after the previous one. */
  private fun fix(east: Double, north: Double = 0.0, accuracy: Double = 5.0, after: Long = 1): Fix {
    seconds += after
    return Fix(
      ORIGIN_LATITUDE + north / METERS_PER_DEGREE,
      ORIGIN_LONGITUDE + east / METERS_PER_DEGREE_LONGITUDE,
      accuracy,
      seconds * 1000,
    )
  }

  private fun east(fix: Fix) =
    round((fix.longitude - ORIGIN_LONGITUDE) * METERS_PER_DEGREE_LONGITUDE)

  private fun assertDirection(expected: Double, actual: Double?) {
    assertTrue(actual != null && abs(actual - expected) < 1.0, "expected $expected, was $actual")
  }

  private companion object {
    const val ORIGIN_LATITUDE = 52.2
    const val ORIGIN_LONGITUDE = 4.5
    const val METERS_PER_DEGREE = 6_371_008.8 * PI / 180
    val METERS_PER_DEGREE_LONGITUDE = METERS_PER_DEGREE * cos(ORIGIN_LATITUDE * PI / 180)
  }
}
