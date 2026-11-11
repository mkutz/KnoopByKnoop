package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Covers `network-sync` "Base map around the route" and "Base map requests" in the core. */
class BaseMapAreaTest {

  private val metersPerDegree = 6_371_008.8 * PI / 180

  /** A straight course east from 52° N 5° E, [kilometers] long. */
  private fun east(kilometers: Double, north: Double = 0.0): List<GeoPoint> {
    val latitude = 52.0 + north / metersPerDegree
    val degrees = kilometers * 1000 / (metersPerDegree * cos(latitude * PI / 180))
    return (0..100).map { GeoPoint(latitude, 5.0 + degrees * it / 100) }
  }

  @Test
  fun `Route planned at home`() {
    val area = assertNotNull(BaseMapArea.of(east(50.0)))

    assertEquals(14, area.detail.maxZoom)
    assertEquals(10, area.detail.minZoom)
    assertEquals(5, area.overview.minZoom)
    assertEquals(9, area.overview.maxZoom)
    assertTrue(area.tiles in 60..400, "${area.tiles} tiles")
    // A tile at zoom 14 is about 1.5 km wide here: the 1 km corridor takes one or two rows.
    assertTrue(area.detail.rings.size in 34..70, "${area.detail.rings.size} tiles at zoom 14")
  }

  @Test
  fun `A long route`() {
    val area = assertNotNull(BaseMapArea.of(east(3_000.0)))

    assertTrue(area.detail.maxZoom < 14, "zoom 14 left out")
    assertTrue(area.tiles <= BaseMapArea.MAX_TILES, "${area.tiles} tiles")
  }

  @Test
  fun `Starting away from the route`() {
    // The area depends only on the course: nothing else goes in.
    val first = assertNotNull(BaseMapArea.of(east(20.0)))
    val again = assertNotNull(BaseMapArea.of(east(20.0)))

    assertEquals(first, again)
    assertNotEquals(first.key, assertNotNull(BaseMapArea.of(east(20.0, north = 3000.0))).key)
  }

  @Test
  fun `the shape as GeoJSON`() {
    val area = assertNotNull(BaseMapArea.of(east(5.0)))

    val json = area.overview.geoJson
    assertTrue(json.startsWith("""{"type":"MultiPolygon","coordinates":[[[["""), json)
    assertTrue(json.endsWith("]]]]}"), json)
  }

  @Test
  fun `no area without a course`() {
    assertNull(BaseMapArea.of(listOf()))
  }

  @Test
  fun `A hiking route`() {
    // Unlike path data, any course is loaded, whatever the network.
    val course = east(10.0)

    assertEquals(course, BaseMapSquares.courseToLoad(course))
  }

  @Test
  fun `Deleting the base map with a route planned`() {
    val course = east(10.0)

    assertNull(BaseMapSquares.courseToLoad(course, deletedFor = course))
    assertEquals(east(12.0), BaseMapSquares.courseToLoad(east(12.0), deletedFor = course))
  }
}
