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
        fix(east = 20.0),
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
    val track = ride(fix(east = 0.0), fix(east = 20.0), fix(east = 40.0))
    assertEquals(listOf(0.0, 20.0, 40.0), track.points.map { east(it) })
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

  private fun ride(vararg fixes: Fix) = fixes.fold(RiddenTrack()) { track, fix -> track.add(fix) }

  /** A fix one second after the previous one. */
  private fun fix(east: Double, north: Double = 0.0, accuracy: Double = 5.0): Fix {
    seconds += 1
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
