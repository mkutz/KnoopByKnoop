package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Covers `knot-navigation` "Tricky spots on the route". */
class TrickyStretchesTest {

  /**
   * A course from the origin through [legs] of the given meters, each turning by the angle before
   * it, in degrees, positive to the right; it starts to the east.
   */
  private fun course(vararg legs: Pair<Double, Double>): List<GeoPoint> {
    var heading = 90.0
    var east = 0.0
    var north = 0.0
    val points = mutableListOf(at(east, north))
    for ((turn, length) in legs) {
      heading += turn
      east += length * sin(heading * PI / 180)
      north += length * cos(heading * PI / 180)
      points += at(east, north)
    }
    return points
  }

  @Test
  fun `zigzag`() {
    val zigzag = course(0.0 to 200.0, 80.0 to 40.0, -90.0 to 200.0)

    val stretches = trickyStretches(zigzag)

    assertEquals(1, stretches.size)
    assertEquals(2, stretches.single().turns)
    assertEquals(200.0, stretches.single().startMeters, 1.0)
    assertEquals(240.0, stretches.single().endMeters, 1.0)
  }

  @Test
  fun `one sharp turn`() {
    assertTrue(trickyStretches(course(0.0 to 200.0, 90.0 to 300.0)).isEmpty())
  }

  @Test
  fun `two sharp turns far apart`() {
    assertTrue(trickyStretches(course(0.0 to 200.0, 90.0 to 150.0, -90.0 to 200.0)).isEmpty())
  }

  @Test
  fun `gentle bends`() {
    val bends = course(0.0 to 100.0, 30.0 to 25.0, 30.0 to 25.0, -30.0 to 25.0, -30.0 to 100.0)

    assertTrue(trickyStretches(bends).isEmpty())
  }

  @Test
  fun `a sharp turn drawn with several points counts once`() {
    // 90° to the right in three steps of 30° over 6 m, as the data often draws a corner.
    val corner = course(0.0 to 200.0, 30.0 to 3.0, 30.0 to 3.0, 30.0 to 200.0)

    assertTrue(trickyStretches(corner).isEmpty())
  }

  @Test
  fun `many short vertices with small zigzags`() {
    // Straight east, with a point every 3 m alternating 1.5 m north and south.
    val jittery = (0..200).map { at(it * 3.0, if (it % 2 == 0) 1.5 else -1.5) }

    assertTrue(trickyStretches(jittery).isEmpty())
  }

  @Test
  fun `without courses`() {
    assertEquals(listOf(listOf(), listOf()), trickyStretchesOf(null, legs = 2))
  }

  private companion object {
    const val METERS_PER_DEGREE = 6_371_008.8 * PI / 180

    fun at(east: Double, north: Double) =
      GeoPoint(
        52.0 + north / METERS_PER_DEGREE,
        5.0 + east / (METERS_PER_DEGREE * cos(52.0 * PI / 180)),
      )
  }
}
