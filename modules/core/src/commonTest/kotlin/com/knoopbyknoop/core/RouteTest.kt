package com.knoopbyknoop.core

import com.knoopbyknoop.core.TestNetwork.graph
import com.knoopbyknoop.core.TestNetwork.knot12
import com.knoopbyknoop.core.TestNetwork.knot30
import com.knoopbyknoop.core.TestNetwork.knot45
import com.knoopbyknoop.core.TestNetwork.knot7
import com.knoopbyknoop.core.TestNetwork.knot99
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/** Covers the scenarios of `openspec/changes/add-waypoint-route-planning/specs/route-planning`. */
class RouteTest {

  private val router = ShortestPathRouter(graph)

  // Requirement: Route from waypoints

  @Test
  fun `start and end only`() {
    val route = Route.plan(listOf(knot12, knot45), router)

    assertEquals(RouteSummary.Complete(listOf(knot12, knot7, knot45), 3500), route.summary)
  }

  @Test
  fun `route through a stop`() {
    val route = Route.plan(listOf(knot12, knot30, knot45), router)

    assertEquals(RouteSummary.Complete(listOf(knot12, knot30, knot45), 5000), route.summary)
  }

  @Test
  fun `unreachable waypoint`() {
    val route = Route.plan(listOf(knot12, knot45, knot99), router)

    val summary = assertIs<RouteSummary.Incomplete>(route.summary)
    assertEquals(listOf(Leg(knot45, knot99, null)), summary.unreachableLegs)
  }

  @Test
  fun `waypoints of different networks`() {
    val hikingKnot = knot45.copy(network = NetworkType.HIKING)

    assertFailsWith<IllegalArgumentException> { Route.plan(listOf(knot12, hikingKnot), router) }
  }

  // Requirement: Insert a stop between start and end

  @Test
  fun `insert a stop at a given position`() {
    val route = Route.plan(listOf(knot12, knot45), router).insertStop(knot30, position = 1)

    assertEquals(listOf(knot12, knot30, knot45), route.waypoints)
  }

  @Test
  fun `stops cannot be inserted before the start or after the end`() {
    val route = Route.plan(listOf(knot12, knot45), router)

    assertFailsWith<IllegalArgumentException> { route.insertStop(knot30, position = 0) }
    assertFailsWith<IllegalArgumentException> { route.insertStop(knot30, position = 2) }
  }

  @Test
  fun `insert a stop into the best leg`() {
    // Adding 7 costs 1500 m more in leg 30→45 (2500 + 2000 - 3000) than 2000 m in leg 12→30.
    val route = Route.plan(listOf(knot12, knot30, knot45), router).insertStop(knot7)

    assertEquals(listOf(knot12, knot30, knot7, knot45), route.waypoints)
  }

  @Test
  fun `insert an unreachable stop without a position`() {
    val route = Route.plan(listOf(knot12, knot30, knot45), router).insertStop(knot99)

    assertEquals(listOf(knot12, knot30, knot99, knot45), route.waypoints)
    assertIs<RouteSummary.Incomplete>(route.summary)
  }

  // Requirement: Reorder and remove stops

  @Test
  fun `move a stop`() {
    val route = Route.plan(listOf(knot12, knot30, knot7, knot45), router).moveWaypoint(2, 1)

    assertEquals(listOf(knot12, knot7, knot30, knot45), route.waypoints)
    assertEquals(RouteSummary.Complete(listOf(knot12, knot7, knot30, knot45), 7000), route.summary)
  }

  @Test
  fun `remove a stop`() {
    val route = Route.plan(listOf(knot12, knot30, knot45), router).removeWaypoint(1)

    assertEquals(listOf(knot12, knot45), route.waypoints)
  }

  @Test
  fun `refuse to drop below two waypoints`() {
    val route = Route.plan(listOf(knot12, knot45), router)

    assertFailsWith<IllegalArgumentException> { route.removeWaypoint(1) }
    assertEquals(listOf(knot12, knot45), route.waypoints)
  }

  // Requirement: Round trips

  @Test
  fun `round trip through a stop`() {
    val route = Route.plan(listOf(knot12, knot30, knot12), router)

    assertEquals(RouteSummary.Complete(listOf(knot12, knot30, knot12), 4000), route.summary)
  }

  // Requirement: Route summary

  @Test
  fun `consecutive legs share their waypoint once`() {
    val summary =
      assertIs<RouteSummary.Complete>(Route.plan(listOf(knot12, knot30, knot45), router).summary)

    assertEquals(1, summary.knots.count { it == knot30 })
  }

  // Requirement: Waypoints are knots, not numbers

  @Test
  fun `choose between knots with equal numbers`() {
    // Knot 30 exists on both banks of a river; the banks are connected only by a 400 m ferry.
    val nearBank = knot30.copy(id = 301)
    val farBank = knot30.copy(id = 302, latitude = 52.104)
    val riverGraph =
      NetworkGraph(
        knots = listOf(knot12, nearBank, farBank),
        connections = listOf(Connection(12, 301, 2000), Connection(301, 302, 400)),
      )

    val route = Route.plan(listOf(knot12, nearBank), ShortestPathRouter(riverGraph))

    assertEquals(RouteSummary.Complete(listOf(knot12, nearBank), 2000), route.summary)
  }

  // Task 3.2: recompute only the legs next to a change

  @Test
  fun `edits only route the legs next to the change`() {
    val routedLegs = mutableListOf<Pair<Long, Long>>()
    val countingRouter = Router { from, to ->
      routedLegs.add(from.id to to.id)
      router.shortestPath(from, to)
    }
    val route = Route.plan(listOf(knot12, knot30, knot45), countingRouter)
    routedLegs.clear()

    route.insertStop(knot7, position = 2)

    assertEquals(listOf(30L to 7L, 7L to 45L), routedLegs)
  }
}
