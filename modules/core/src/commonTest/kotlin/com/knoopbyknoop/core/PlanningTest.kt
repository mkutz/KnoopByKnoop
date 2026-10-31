package com.knoopbyknoop.core

import com.knoopbyknoop.core.TestNetwork.graph
import com.knoopbyknoop.core.TestNetwork.knot12
import com.knoopbyknoop.core.TestNetwork.knot30
import com.knoopbyknoop.core.TestNetwork.knot45
import com.knoopbyknoop.core.TestNetwork.knot7
import com.knoopbyknoop.core.TestNetwork.knot99
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.TimeSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/** Covers the planning scenarios of `map-planning`, and the route scenarios of `network-sync`. */
class PlanningTest {

  private val session = PlanningSession.start(graph)

  private fun PlanningSession.tapAll(vararg knots: Knot) =
    knots.fold(this) { session, knot -> session.tap(knot) }

  private fun features(geoJson: String) =
    Json.parseToJsonElement(geoJson).jsonObject.getValue("features").jsonArray

  private fun coordinateCount(feature: kotlinx.serialization.json.JsonElement) =
    feature.jsonObject.getValue("geometry").jsonObject.getValue("coordinates").jsonArray.size

  private val route12To45 =
    listOf(
      RouteKnot(knot12, KnotRole.START),
      RouteKnot(knot7, KnotRole.PASSED),
      RouteKnot(knot45, KnotRole.END),
    )

  // Requirement: Show the network on the map

  @Test
  fun `the map shows every knot and connection`() {
    assertEquals(graph.knots.size, features(MapFeatures.knots(graph)).size)
    assertEquals(
      graph.connections.size,
      features(MapFeatures.connections(NetworkGeometry.straightLines(graph))).size,
    )
  }

  @Test
  fun `switching to hiking`() {
    val hikingGraph =
      NetworkGraph(
        graph.knots.values.map { it.copy(network = NetworkType.HIKING) },
        graph.connections,
      )
    val planned = session.tapAll(knot12, knot45)

    val switched = planned.switchTo(hikingGraph)

    assertEquals(NetworkType.HIKING, switched.network)
    assertEquals(listOf(), switched.waypoints)
  }

  @Test
  fun `without matching geometry`() {
    val straightLines = NetworkGeometry.straightLines(graph)
    val planned = session.tapAll(knot12, knot45)

    assertTrue(features(MapFeatures.connections(straightLines)).all { coordinateCount(it) == 2 })
    val route = features(MapFeatures.route(planned, straightLines)).single()
    assertEquals(3, coordinateCount(route), "12, 7 and 45")
    assertEquals(
      RouteDescription.Complete(route12To45, "3.5 km", duration = "about 13 min"),
      MapFeatures.describe(planned),
    )
  }

  @Test
  fun `routes follow the geometry of their connections`() {
    val bent =
      NetworkGeometry(
        graph,
        graph.connections.map { connection ->
          val from = graph.knots.getValue(connection.fromId)
          val to = graph.knots.getValue(connection.toId)
          listOf(
            GeoPoint(from.latitude, from.longitude),
            GeoPoint(
              (from.latitude + to.latitude) / 2 + 0.001,
              (from.longitude + to.longitude) / 2,
            ),
            GeoPoint(to.latitude, to.longitude),
          )
        },
      )

    val route = features(MapFeatures.route(session.tapAll(knot12, knot45), bent)).single()

    assertEquals(5, coordinateCount(route), "12, bend, 7, bend, 45")
  }

  // Requirement: Plan by tapping knots

  @Test
  fun `start and end`() {
    val planned = session.tapAll(knot12, knot45)

    assertEquals(listOf(knot12, knot45), planned.waypoints)
    assertEquals(RouteSummary.Complete(listOf(knot12, knot7, knot45), 3500), planned.route?.summary)
  }

  @Test
  fun `adding a stop`() {
    val planned = session.tapAll(knot12, knot45, knot30)

    assertEquals(listOf(knot12, knot30, knot45), planned.waypoints)
    assertTrue(knot30 in (planned.route?.summary as RouteSummary.Complete).knots)
  }

  @Test
  fun `tapping next to the network`() {
    val locator = KnotLocator(graph.knots.values)

    assertNull(locator.nearest(52.0, 5.0, radiusMeters = 60.0))
  }

  @Test
  fun `the nearest knot within the radius is found`() {
    val locator = KnotLocator(graph.knots.values)

    assertEquals(
      knot12,
      locator.nearest(knot12.latitude + 0.0003, knot12.longitude, radiusMeters = 60.0),
    )
    assertNull(locator.nearest(knot12.latitude + 0.0006, knot12.longitude, radiusMeters = 60.0))
  }

  @Test
  fun `knots with equal numbers`() {
    // Knot 30 on both banks of a river, 110 m apart.
    val nearBank = knot30.copy(id = 301)
    val farBank = knot30.copy(id = 302, latitude = knot30.latitude + 0.001)
    val locator = KnotLocator(listOf(nearBank, farBank))

    val tapped = locator.nearest(farBank.latitude - 0.0001, farBank.longitude, radiusMeters = 60.0)

    assertEquals(302, tapped?.id)
  }

  // Requirement: Show the route

  @Test
  fun `route summary`() {
    assertEquals(
      RouteDescription.Complete(route12To45, "3.5 km", duration = "about 13 min"),
      MapFeatures.describe(session.tapAll(knot12, knot45)),
    )
  }

  @Test
  fun `descriptions before there is a route`() {
    assertEquals(RouteDescription.Empty, MapFeatures.describe(session))
    assertEquals(RouteDescription.StartOnly("12"), MapFeatures.describe(session.tap(knot12)))
  }

  @Test
  fun `unreachable leg`() {
    val planned = session.tapAll(knot12, knot99)

    assertEquals(RouteDescription.Unreachable(listOf("12 → 99")), MapFeatures.describe(planned))
    val leg = features(MapFeatures.route(planned, NetworkGeometry.straightLines(graph))).single()
    assertEquals(
      "false",
      leg.jsonObject.getValue("properties").jsonObject.getValue("reachable").toString(),
    )
  }

  @Test
  fun `distances below and above one kilometer`() {
    assertEquals("800 m", MapFeatures.formatDistance(800))
    assertEquals("3.5 km", MapFeatures.formatDistance(3456))
    assertEquals("12 km", MapFeatures.formatDistance(11_960))
  }

  // Requirement: Edit the waypoint list

  @Test
  fun `reorder`() {
    val planned = PlanningSession.start(graph, listOf(knot12, knot30, knot7, knot45))

    val reordered = planned.moveWaypoint(2, 1)

    assertEquals(listOf(knot12, knot7, knot30, knot45), reordered.waypoints)
    assertEquals(
      listOf(knot12, knot7, knot30, knot45),
      (reordered.route?.summary as RouteSummary.Complete).knots,
    )
  }

  @Test
  fun `remove`() {
    val planned = session.tapAll(knot12, knot45, knot30)

    assertEquals(listOf(knot12, knot45), planned.removeWaypoint(1).waypoints)
  }

  @Test
  fun `removing one of two waypoints`() {
    val planned = session.tapAll(knot12, knot45)

    val removed = planned.removeWaypoint(1)

    assertTrue(planned.canRemoveWaypoints)
    assertEquals(listOf(knot12), removed.waypoints)
    assertNull(removed.route)
    assertEquals(RouteDescription.StartOnly("12"), MapFeatures.describe(removed))
    assertEquals(listOf(knot12, knot30), removed.tap(knot30).waypoints, "the next tap sets the end")
  }

  @Test
  fun `removing the start of two waypoints`() {
    assertEquals(listOf(knot45), session.tapAll(knot12, knot45).removeWaypoint(0).waypoints)
  }

  @Test
  fun `removing the only waypoint`() {
    assertEquals(listOf(), session.tapAll(knot12).removeWaypoint(0).waypoints)
  }

  @Test
  fun `start over`() {
    val cleared = session.tapAll(knot12, knot30, knot45).clear()

    assertEquals(listOf(), cleared.waypoints)
    assertEquals(listOf(knot7), cleared.tap(knot7).waypoints, "the next tap sets a new start")
  }

  @Test
  fun `waypoint markers know their role`() {
    val markers = features(MapFeatures.waypoints(session.tapAll(knot12, knot45, knot30)))

    assertEquals(
      listOf("\"start\"", "\"stop\"", "\"end\""),
      markers.map { it.jsonObject.getValue("properties").jsonObject.getValue("role").toString() },
    )
  }

  // Requirement: Keep the current route

  @Test
  fun `restart`() {
    val planned = session.tapAll(knot12, knot45, knot30)

    val restored = RouteSnapshot.parse(RouteSnapshot.of(planned).toJson()).restore(graph)

    assertEquals(planned.waypoints, restored.session.waypoints)
    assertEquals(planned.route?.summary, restored.session.route?.summary)
    assertEquals(listOf(), restored.removedWaypoints)
  }

  @Test
  fun `knot gone after an update`() {
    val snapshot = RouteSnapshot.of(session.tapAll(knot12, knot45, knot30))
    val updatedGraph =
      NetworkGraph(
        graph.knots.values.filter { it != knot30 },
        graph.connections.filter { knot30.id !in listOf(it.fromId, it.toId) },
      )

    val restored = snapshot.restore(updatedGraph)

    assertEquals(listOf(knot12, knot45), restored.session.waypoints)
    assertEquals(listOf("30"), restored.removedWaypoints)
  }

  @Test
  fun `snapshots written per region still restore`() {
    val json =
      """{"region": "utrecht", "network": "cycling", "waypoints": [
      {"id": ${knot12.id}, "ref": "12"}, {"id": ${knot45.id}, "ref": "45"}]}"""

    val restored = RouteSnapshot.parse(json).restore(graph)

    assertEquals(listOf(knot12, knot45), restored.session.waypoints)
  }

  @Test
  fun `route across an update`() {
    val planned = session.tapAll(knot12, knot45, knot30)
    val updatedGraph =
      NetworkGraph(
        graph.knots.values.filter { it != knot30 },
        graph.connections.filter { knot30.id !in listOf(it.fromId, it.toId) },
      )

    val restored = RouteSnapshot.of(planned).restore(updatedGraph)

    assertEquals(listOf(knot12, knot45), restored.session.waypoints)
    assertEquals(listOf("30"), restored.removedWaypoints)
    assertSame(updatedGraph, restored.session.graph)
  }

  // improve-planner-map: Plan by tapping knots

  @Test
  fun `tapping the last waypoint again`() {
    val started = session.tapAll(knot12, knot12)

    assertEquals(listOf(knot12), started.waypoints)
    assertEquals(listOf(knot12, knot45), started.tap(knot45).waypoints, "the next knot is the end")
    assertEquals(listOf(knot12, knot45), session.tapAll(knot12, knot45, knot45).waypoints)
  }

  @Test
  fun `tapping the start of a route again`() {
    val planned = session.tapAll(knot12, knot45, knot12, knot12)

    assertEquals(listOf(knot12, knot45), planned.waypoints)
    assertEquals(listOf(knot12, knot30, knot45), planned.tapAll(knot30, knot30, knot12).waypoints)
  }

  // improve-planner-map: Return to the start

  @Test
  fun `back to start`() {
    val planned = session.tapAll(knot12, knot45, knot30)

    val roundTrip = planned.returnToStart()

    assertEquals(listOf(knot12, knot30, knot45, knot12), roundTrip.waypoints)
    assertFalse(roundTrip.canReturnToStart, "it already ends at its start")
    assertFalse(session.tap(knot12).canReturnToStart, "a start alone is no route")
  }

  // improve-planner-map: Route knots stand out

  @Test
  fun `passed knots`() {
    val routeKnots = MapFeatures.routeKnots(session.tapAll(knot12, knot45))

    assertEquals(route12To45, routeKnots)
    assertTrue(routeKnots.none { it.knot == knot30 }, "knots off the route are not highlighted")
  }

  @Test
  fun `start stop and end`() {
    val routeKnots = MapFeatures.routeKnots(session.tapAll(knot12, knot45, knot30))

    assertEquals(
      listOf(KnotRole.START, KnotRole.STOP, KnotRole.END),
      routeKnots.filter { it.role != KnotRole.PASSED }.map { it.role },
    )
    assertEquals(
      listOf(knot12, knot30, knot45),
      routeKnots.filter { it.role != KnotRole.PASSED }.map { it.knot },
    )
  }

  @Test
  fun `round trips end where they start`() {
    val roles = MapFeatures.routeKnots(PlanningSession.start(graph, listOf(knot12, knot30, knot12)))

    assertEquals(
      listOf(
        RouteKnot(knot12, KnotRole.START),
        RouteKnot(knot30, KnotRole.STOP),
        RouteKnot(knot12, KnotRole.END),
      ),
      roles,
    )
  }

  @Test
  fun `round trip on the map`() {
    val roundTrip = PlanningSession.start(graph, listOf(knot12, knot30, knot12))

    assertEquals(
      listOf(RouteKnot(knot12, KnotRole.ROUND_TRIP), RouteKnot(knot30, KnotRole.STOP)),
      MapFeatures.routeKnotMarkers(roundTrip),
    )
    assertEquals(2, features(MapFeatures.routeKnotsLayer(roundTrip)).size, "one marker for 12")
    assertEquals(
      listOf(KnotRole.START, KnotRole.STOP, KnotRole.END),
      MapFeatures.routeKnots(roundTrip).map { it.role },
      "the sheet still shows start and end",
    )
  }

  @Test
  fun `other routes keep separate start and end markers`() {
    val planned = session.tapAll(knot12, knot45)

    assertEquals(MapFeatures.routeKnots(planned), MapFeatures.routeKnotMarkers(planned))
  }

  @Test
  fun `an incomplete route shows its waypoints`() {
    val roles = MapFeatures.routeKnots(session.tapAll(knot12, knot99))

    assertEquals(listOf(RouteKnot(knot12, KnotRole.START), RouteKnot(knot99, KnotRole.END)), roles)
  }

  @Test
  fun `route knots on the map know their role`() {
    val features = features(MapFeatures.routeKnotsLayer(session.tapAll(knot12, knot45)))

    assertEquals(
      listOf("\"start\"", "\"passed\"", "\"end\""),
      features.map { it.jsonObject.getValue("properties").jsonObject.getValue("role").toString() },
    )
  }

  // improve-planner-map: Waypoint list with badges

  @Test
  fun `listing waypoints`() {
    assertEquals(
      listOf(
        RouteKnot(knot12, KnotRole.START),
        RouteKnot(knot30, KnotRole.STOP),
        RouteKnot(knot45, KnotRole.END),
      ),
      MapFeatures.waypointKnots(session.tapAll(knot12, knot45, knot30)),
    )
  }

  // Task 2.6

  @Test
  fun `the knot locator is fast on the Dutch hiking network`() {
    val random = Random(42)
    val knots =
      List(38_262) {
        Knot(
          it.toLong(),
          "${it % 100}",
          NetworkType.HIKING,
          50.75 + random.nextDouble() * 2.8,
          3.35 + random.nextDouble() * 3.9,
        )
      }
    val locator = KnotLocator(knots)
    val taps = List(1000) { 50.75 + random.nextDouble() * 2.8 to 3.35 + random.nextDouble() * 3.9 }

    val start = TimeSource.Monotonic.markNow()
    val found = taps.map { (latitude, longitude) -> locator.nearest(latitude, longitude, 200.0) }
    val averageMilliseconds = start.elapsedNow().inWholeMicroseconds / 1000.0 / taps.size

    assertTrue(averageMilliseconds < 5, "average lookup took $averageMilliseconds ms")
    // The grid finds the same knot as measuring every knot would.
    taps.take(50).zip(found).forEach { (tap, knot) ->
      val nearest =
        knots
          .map { it to distanceMeters(tap.first, tap.second, it.latitude, it.longitude) }
          .filter { it.second <= 200.0 }
          .minByOrNull { it.second }
          ?.first
      assertSame(nearest, knot)
    }
  }
}
