package com.knoopbyknoop.core

import com.knoopbyknoop.core.TestNetwork.graph
import com.knoopbyknoop.core.TestNetwork.knot12
import com.knoopbyknoop.core.TestNetwork.knot30
import com.knoopbyknoop.core.TestNetwork.knot45
import com.knoopbyknoop.core.TestNetwork.knot7
import com.knoopbyknoop.core.TestNetwork.knot99
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShortestPathRouterTest {

  private val router = ShortestPathRouter(graph)

  @Test
  fun `takes the shortest path even when a direct connection exists`() {
    assertEquals(Path(listOf(knot12, knot7, knot45), 3500), router.shortestPath(knot12, knot45))
  }

  @Test
  fun `paths are the same in both directions`() {
    assertEquals(Path(listOf(knot45, knot7, knot12), 3500), router.shortestPath(knot45, knot12))
  }

  @Test
  fun `the path from a knot to itself is just that knot`() {
    assertEquals(Path(listOf(knot30), 0), router.shortestPath(knot30, knot30))
  }

  @Test
  fun `knots in another part of the network are unreachable`() {
    assertNull(router.shortestPath(knot12, knot99))
  }

  @Test
  fun `knots outside the network are unreachable`() {
    val unknownKnot = knot12.copy(id = 1234)

    assertNull(router.shortestPath(knot12, unknownKnot))
  }
}
