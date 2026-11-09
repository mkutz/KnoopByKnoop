package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Covers `knot-navigation` "Picture in the app" for ways to a route point, led from the network.
 */
class LedByTest {

  private fun at(east: Double, north: Double) =
    GeoPoint(
      52.0 + north / METERS_PER_DEGREE,
      5.0 + east / (METERS_PER_DEGREE * cos(52.0 * PI / 180)),
    )

  @Test
  fun `a way to a route point keeps its doubtful junctions`() {
    val junction =
      DoubtfulJunction(50.0, 60.0, setOf(JunctionDoubt.SIMILAR_EXIT), keep = TurnDirection.LEFT)
    val way =
      PathWay(
        listOf(at(100.0, 0.0), at(200.0, 0.0)),
        listOf(0.0, 100.0),
        listOf(PathTurn(50.0, TurnDirection.LEFT, null, doubtful = junction)),
        doubtful = listOf(junction),
      )

    // 100 m of the network's course in front of it.
    val led = ledBy(listOf(at(0.0, 0.0)), way)

    val moved = led.doubtful.single()
    assertEquals(150.0, moved.startMeters, 1.0)
    assertEquals(160.0, moved.endMeters, 1.0)
    // The turn still names the same junction, for its sentence.
    assertEquals(moved, assertNotNull(led.turns.single().doubtful))
  }

  private companion object {
    const val METERS_PER_DEGREE = 6_371_008.8 * PI / 180
  }
}
