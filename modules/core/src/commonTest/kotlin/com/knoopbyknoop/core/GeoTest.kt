package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Covers the shared line geometry the core measures with. */
class GeoTest {

  private val origin = GeoPoint(52.0, 5.0)

  /** A point [east] and [north] meters from [origin]. */
  private fun at(east: Double, north: Double = 0.0) =
    GeoPoint(
      origin.latitude + north / METERS_PER_DEGREE,
      origin.longitude + east / (METERS_PER_DEGREE * kotlin.math.cos(52.0 * kotlin.math.PI / 180)),
    )

  private fun assertNear(expected: GeoPoint, actual: GeoPoint, meters: Double = 0.01) =
    assertEquals(0.0, distanceMeters(expected, actual), meters, "$actual is not at $expected")

  /** East 100 m, then north 100 m. */
  private val corner = Polyline(listOf(at(0.0), at(100.0), at(100.0, 100.0)))

  @Test
  fun `meters along add up the segments`() {
    assertEquals(listOf(0.0, 100.0, 200.0), corner.meters.map { kotlin.math.round(it * 100) / 100 })
    assertEquals(200.0, corner.length, 0.01)
  }

  @Test
  fun `a point along is clamped to the ends`() {
    assertNear(at(50.0), corner.pointAt(50.0))
    assertNear(at(100.0, 50.0), corner.pointAt(150.0))
    assertNear(at(0.0), corner.pointAt(-10.0))
    assertNear(at(100.0, 100.0), corner.pointAt(500.0))
  }

  @Test
  fun `a zero-length segment is passed over`() {
    val doubled = Polyline(listOf(at(0.0), at(100.0), at(100.0), at(200.0)))

    assertNear(at(100.0), doubled.pointAt(100.0))
    assertNear(at(150.0), doubled.pointAt(150.0))
  }

  @Test
  fun `a stretch has its ends and the points between`() {
    val stretch = corner.stretch(50.0, 150.0)

    assertEquals(3, stretch.size)
    assertNear(at(50.0), stretch[0])
    assertNear(at(100.0), stretch[1])
    assertNear(at(100.0, 50.0), stretch[2])
  }

  @Test
  fun `a stretch backwards is reversed and clamped`() {
    val stretch = corner.stretch(300.0, 150.0)

    assertEquals(2, stretch.size)
    assertNear(at(100.0, 100.0), stretch[0])
    assertNear(at(100.0, 50.0), stretch[1])
  }

  @Test
  fun `the nearest place beside a segment`() {
    val onLine = corner.nearest(at(40.0, -30.0))

    assertNear(at(40.0), onLine.place)
    assertEquals(40.0, onLine.meters, 0.01)
    assertEquals(0.2, onLine.fraction, 0.0001)
    assertEquals(30.0, onLine.distance, 0.01)
  }

  @Test
  fun `the nearest place before the start and past the end`() {
    assertNear(at(0.0), corner.nearest(at(-50.0)).place)
    assertEquals(200.0, corner.nearest(at(100.0, 180.0)).meters, 0.01)
  }

  @Test
  fun `the nearest place outside a corner is the corner`() {
    val onLine = corner.nearest(at(130.0, -40.0))

    assertNear(at(100.0), onLine.place)
    assertEquals(50.0, onLine.distance, 0.01)
    assertEquals(50.0, corner.distanceTo(at(130.0, -40.0)), 0.01)
  }

  @Test
  fun `a line of one point`() {
    val point = Polyline(listOf(at(0.0)))

    assertNear(at(0.0), point.pointAt(10.0))
    assertEquals(20.0, point.nearest(at(20.0)).distance, 0.01)
    assertFailsWith<IllegalArgumentException> { Polyline(listOf()) }
  }

  @Test
  fun `bearings in all directions`() {
    assertEquals(0.0, bearing(origin, at(0.0, 100.0)), 0.01)
    assertEquals(90.0, bearing(origin, at(100.0)), 0.01)
    assertEquals(180.0, bearing(origin, at(0.0, -100.0)), 0.01)
    assertEquals(270.0, bearing(origin, at(-100.0)), 0.01)
    assertEquals(315.0, bearing(origin, at(-100.0, 100.0)), 0.1)
  }

  @Test
  fun `turns between bearings across north`() {
    assertEquals(20.0, signedAngle(350.0, 10.0), 0.0001)
    assertEquals(-20.0, signedAngle(10.0, 350.0), 0.0001)
    assertEquals(180.0, angleBetween(0.0, 180.0), 0.0001)
    assertEquals(170.0, angleBetween(-175.0, 175.0 + 180.0), 0.0001)
  }

  @Test
  fun `a box around a point`() {
    val box = boxAround(origin, 1000.0)

    assertEquals(1000.0, distanceMeters(origin, GeoPoint(box.maxLatitude, origin.longitude)), 0.01)
    assertEquals(1000.0, distanceMeters(origin, GeoPoint(origin.latitude, box.maxLongitude)), 1.0)
  }
}
