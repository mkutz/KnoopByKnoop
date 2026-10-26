package com.knoopbyknoop.core

import com.knoopbyknoop.core.TestNetwork.graph
import com.knoopbyknoop.core.TestNetwork.knot12
import com.knoopbyknoop.core.TestNetwork.knot30
import com.knoopbyknoop.core.TestNetwork.knot45
import com.knoopbyknoop.core.TestNetwork.knot7
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NetworkGraphTest {

  @Test
  fun `connections can be used in both directions`() {
    assertEquals(
      setOf(knot30 to 2000, knot45 to 4000, knot7 to 1500),
      graph.neighborsOf(knot12).toSet(),
    )
    assertEquals(
      setOf(knot12 to 2000, knot45 to 3000, knot7 to 2500),
      graph.neighborsOf(knot30).toSet(),
    )
  }

  @Test
  fun `connections to unknown knots are rejected`() {
    assertFailsWith<IllegalArgumentException> {
      NetworkGraph(listOf(knot12), listOf(Connection(12, 30, 2000)))
    }
  }

  @Test
  fun `knots of different networks are rejected`() {
    val hikingKnot = knot30.copy(network = NetworkType.HIKING)

    assertFailsWith<IllegalArgumentException> { NetworkGraph(listOf(knot12, hikingKnot), listOf()) }
  }
}
