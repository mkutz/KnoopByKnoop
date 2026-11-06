package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.NetworkType
import com.knoopbyknoop.core.Stretch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers `network-data` "Bridge stretches of courses" of `openspec/changes/show-route-on-bridges`.
 *
 * A straight route north from knot 28 (node 1) to knot 29 (node 9), a node every 111 m (0.001°).
 */
class BridgeTest {

  private val nodeNetwork = mapOf("network:type" to "node_network")

  private fun node(id: Long, step: Int, ref: String? = null) =
    OsmNode(
      id,
      52.0 + step * 0.001,
      5.0,
      if (ref != null) nodeNetwork + ("rcn_ref" to ref) else mapOf(),
    )

  private val nodes =
    (1L..9L).map {
      node(it, (it - 1).toInt(), if (it == 1L) "28" else if (it == 9L) "29" else null)
    }

  private fun way(id: Long, vararg nodeIds: Long, bridge: String? = null) =
    OsmWay(id, nodeIds.toList(), if (bridge != null) mapOf("bridge" to bridge) else mapOf())

  private fun extract(
    vararg ways: OsmWay,
    extraNodes: List<OsmNode> = listOf(),
    relationWays: List<Long>? = null,
  ) =
    NetworkExtraction()
      .apply {
        acceptRelation(
          OsmRelation(
            10,
            nodeNetwork + mapOf("type" to "route", "network" to "rcn"),
            relationWays ?: ways.map { it.id },
          )
        )
        ways.forEach(::acceptWay)
        (nodes + extraNodes).forEach(::acceptNode)
      }
      .result()

  private fun bridgesOf(vararg ways: OsmWay) =
    extract(*ways).bridges[NetworkType.CYCLING].orEmpty().values.singleOrNull().orEmpty()

  @Test
  fun `course over a bridge`() {
    val bridges =
      bridgesOf(way(100, 1, 2, 3, 4), way(101, 4, 5, 6, bridge = "yes"), way(102, 6, 7, 8, 9))

    // From node 4 (index 3) to node 6 (index 5) of the course.
    assertEquals(listOf(Stretch(3, 5)), bridges)
  }

  @Test
  fun `course under a bridge`() {
    // The road over the route is not a way of the route; the route's own ways have no bridge tag.
    val result =
      extract(
        way(100, 1, 2, 3, 4, 5, 6, 7, 8, 9),
        way(200, 20, 21, bridge = "yes"),
        relationWays = listOf(100),
      )

    assertEquals(mapOf(), result.bridges[NetworkType.CYCLING].orEmpty())
  }

  @Test
  fun `several ways of one bridge`() {
    val bridges =
      bridgesOf(
        way(100, 1, 2, 3),
        way(101, 3, 4, 5, bridge = "yes"),
        way(102, 5, 6, bridge = "viaduct"),
        way(103, 6, 7, 8, 9),
      )

    assertEquals(listOf(Stretch(2, 5)), bridges)
  }

  @Test
  fun `bridge tagged no`() {
    assertEquals(
      listOf(),
      bridgesOf(way(100, 1, 2, 3, 4, 5), way(101, 5, 6, 7, 8, 9, bridge = "no")),
    )
  }

  @Test
  fun `stretch kept through simplification`() {
    // A straight course: simplifying alone would keep only its two ends.
    val course = (0..8).map { GeoPoint(52.0 + it * 0.001, 5.0) }
    val bridge = Stretch(3, 5)

    val (simplified, stretches) = simplifyKeeping(course, 5.0, listOf(bridge))

    assertEquals(listOf(course[0], course[3], course[5], course[8]), simplified)
    assertEquals(listOf(Stretch(1, 2)), stretches)
    stretches.single().let { (first, last) ->
      assertTrue(meters(simplified[first], course[bridge.first]) <= 5.0)
      assertTrue(meters(simplified[last], course[bridge.last]) <= 5.0)
    }
  }

  @Test
  fun `signpost course over a bridge`() {
    // Knot 28 is signposted on nodes 1 and 10, the second one 30 m to the east; a fork at node 2
    // leads to both. The branch to node 10 crosses a bridge.
    val signpost = OsmNode(10, 52.0, 5.00044, nodeNetwork + ("rcn_ref" to "28"))
    val result =
      extract(
        way(100, 2, 3, 4, 5, 6, 7, 8, 9),
        way(101, 2, 1),
        way(102, 2, 11, 10, bridge = "yes"),
        extraNodes = listOf(signpost, OsmNode(11, 52.0005, 5.00022)),
      )

    val signposts = result.signpostCourses.getValue(NetworkType.CYCLING).values.single()
    val toSignpost = signposts.single { it.course.any { point -> point.longitude > 5.0003 } }
    assertEquals(1, toSignpost.bridges.size, "its own bridge stretch")
    assertTrue(signposts.single { it != toSignpost }.bridges.isEmpty())
  }

  private fun meters(from: GeoPoint, to: GeoPoint) =
    hypot(
      (from.latitude - to.latitude) * 111_195.0,
      (from.longitude - to.longitude) * 111_195.0 * cos(Math.toRadians(from.latitude)),
    )
}
