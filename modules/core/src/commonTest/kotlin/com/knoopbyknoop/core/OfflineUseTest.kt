package com.knoopbyknoop.core

import com.knoopbyknoop.core.TestNetwork.knot12
import com.knoopbyknoop.core.TestNetwork.knot45
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the scenario "Airplane mode" of `network-data`: the core itself never uses the network.
 */
class OfflineUseTest {

  @Test
  fun `airplane mode`() {
    val downloadedBytes =
      GraphFile(NetworkType.CYCLING, "utrecht", "2026-10-25", "", TestNetwork.graph).toBytes()

    val graph = GraphFile.read(downloadedBytes).graph
    val start = graph.knots.getValue(knot12.id)
    val end = graph.knots.getValue(knot45.id)
    val route = Route.plan(listOf(start, end), ShortestPathRouter(graph))

    assertEquals(3500, (route.summary as RouteSummary.Complete).lengthMeters)
  }
}
