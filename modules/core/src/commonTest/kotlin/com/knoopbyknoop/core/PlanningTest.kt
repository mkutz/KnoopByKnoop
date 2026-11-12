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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.TimeSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Covers the planning scenarios of `map-planning`, and the route scenarios of `network-sync`. */
class PlanningTest {

  private val session = PlanningSession.start(graph)

  private fun PlanningSession.tapAll(vararg knots: Knot) =
    knots.fold(this) { session, knot -> session.tap(knot) }

  // Requirement: Plan by tapping knots, where a tapped knot goes

  @Test
  fun `Beyond the end a tapped knot becomes the new end`() {
    // From 30, 45 is 3000 m on; as a stop between 12 and 30 it would add 4500 m.
    val extended = session.tapAll(knot12, knot30, knot45)

    assertEquals(listOf(knot12, knot30, knot45), extended.waypoints)
    assertEquals(WaypointRole.END, extended.roleOf(2))
    assertEquals(WaypointRole.STOP, extended.roleOf(1))
  }

  @Test
  fun `Extending step by step adds the knots in the order they are tapped`() {
    assertEquals(listOf(knot12, knot30, knot45), session.tapAll(knot12, knot30, knot45).waypoints)
  }

  @Test
  fun `a knot beside the route becomes a stop rather than the end`() {
    // Between 12 and 45, 30 adds 1500 m; after 45 it would add 3000 m.
    assertEquals(listOf(knot12, knot30, knot45), session.tapAll(knot12, knot45, knot30).waypoints)
  }

  @Test
  fun `Knot beyond the end of a round trip becomes a stop and the round trip stays one`() {
    val roundTrip = session.tapAll(knot12, knot45).returnToStart()

    val extended = roundTrip.tap(knot30)

    assertEquals(knot12, extended.waypoints.first())
    assertEquals(knot12, extended.waypoints.last())
    assertEquals(4, extended.waypoints.size)
    assertTrue(knot30 in extended.waypoints)
  }

  @Test
  fun `Tapping a knot that cannot be reached adds it before the end`() {
    val extended = session.tapAll(knot12, knot45).tap(knot99)

    assertEquals(listOf(knot12, knot99, knot45), extended.waypoints)
    val summary = assertNotNull(extended.route).summary
    assertTrue(summary is RouteSummary.Incomplete)
  }

  @Test
  fun `the first two taps are start and end`() {
    assertEquals(listOf(knot12), session.tap(knot12).waypoints)
    assertEquals(listOf(knot12, knot45), session.tapAll(knot12, knot45).waypoints)
  }

  @Test
  fun `tapping a waypoint of a route changes nothing`() {
    val route = session.tapAll(knot12, knot45)

    assertEquals(listOf(knot12, knot45), route.tap(knot12).waypoints)
    assertEquals(listOf(knot12, knot45), route.tap(knot45).waypoints)
  }

  // Requirement: Changing what a tapped knot became, on the session before the tap

  @Test
  fun `Make it the start moves the tapped knot to the start`() {
    val before = session.tapAll(knot12, knot45)

    assertEquals(listOf(knot30, knot12, knot45), before.startAt(knot30).waypoints)
  }

  @Test
  fun `Make it the end moves the tapped knot to the end`() {
    val before = session.tapAll(knot12, knot45)

    assertEquals(listOf(knot12, knot45, knot30), before.endAt(knot30).waypoints)
  }

  private fun KnotPreview.refs() = knots.map { it.knot.ref }

  @Test
  fun `Note after a tap offers start stop and end in this order with the stop selected`() {
    val before = session.tapAll(knot12, knot45)
    val options = assertNotNull(TapOptions.of(before, before.tap(knot30), knot30))

    assertEquals(WaypointRole.STOP, options.role)
    val (start, stop, end) = options.choices
    assertEquals(
      listOf(WaypointRole.START, WaypointRole.STOP, WaypointRole.END),
      options.choices.map { it.role },
    )
    assertEquals(listOf(false, true, false), options.choices.map { it.current })
    assertEquals(listOf("30", "12"), start.preview.refs())
    assertEquals(listOf("12", "30", "45"), stop.preview.refs())
    assertEquals(1, stop.preview.highlighted)
    assertEquals(listOf("45", "30"), end.preview.refs())
    assertEquals(listOf(knot30, knot12, knot45), start.session.waypoints)
    assertEquals(listOf(knot12, knot45, knot30), end.session.waypoints)
  }

  @Test
  fun `Note for a new end selects the end and previews the stop where it fits`() {
    val before = session.tapAll(knot12, knot30)
    val options = assertNotNull(TapOptions.of(before, before.tap(knot45), knot45))

    assertEquals(WaypointRole.END, options.role)
    assertEquals(listOf(false, false, true), options.choices.map { it.current })
    assertEquals(listOf("30", "45"), options.choices[2].preview.refs())
    assertEquals(listOf("7", "45", "30"), options.choices[1].preview.refs())
  }

  @Test
  fun `Make it a stop puts the knot where it adds the least distance and the end back`() {
    val before = session.tapAll(knot12, knot30)
    val stop = assertNotNull(TapOptions.of(before, before.tap(knot45), knot45)).choices[1]

    assertEquals(listOf(knot12, knot45, knot30), stop.session.waypoints)
  }

  @Test
  fun `No note for the first two taps`() {
    val started = session.tap(knot12)
    assertNull(TapOptions.of(session, started, knot12))
    assertNull(TapOptions.of(started, started.tap(knot45), knot45))
  }

  @Test
  fun `Undo the tap gives the route before it`() {
    val before = session.tapAll(knot12, knot45)
    val after = before.tap(knot30)

    assertEquals(listOf(knot12, knot30, knot45), after.waypoints)
    assertEquals(listOf(knot12, knot45), before.waypoints)
  }

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
  fun `dutch phone`() {
    val description =
      MapFeatures.describe(session.tapAll(knot12, knot45), Language.NL) as RouteDescription.Complete

    assertEquals("3,5 km", description.distance)
    assertEquals("ongeveer 13 min", description.duration)
  }

  @Test
  fun `descriptions before there is a route`() {
    assertEquals(RouteDescription.Empty, MapFeatures.describe(session))
    assertEquals(
      RouteDescription.StartOnly("12"),
      MapFeatures.describe(session.tap(knot12)),
    )
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
    assertEquals(
      listOf(knot12, knot30),
      removed.tap(knot30).waypoints,
      "the next tap sets the end",
    )
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
    assertEquals(
      listOf(knot7),
      cleared.tap(knot7).waypoints,
      "the next tap sets a new start",
    )
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
    assertEquals(
      listOf(knot12, knot45),
      started.tap(knot45).waypoints,
      "the next knot is the end",
    )
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

  // Requirement: Route knots stand out, plates by sign region

  private val dutchKnot =
    Knot(101, "95", NetworkType.CYCLING, 51.32, 6.0, signRegion = SignRegion.NETHERLANDS)
  private val belgianKnot =
    Knot(102, "150", NetworkType.CYCLING, 51.31, 6.0, signRegion = SignRegion.BELGIUM)
  private val unknownKnot = Knot(103, "12", NetworkType.CYCLING, 51.30, 6.0)
  private val borderGraph =
    NetworkGraph(
      listOf(dutchKnot, belgianKnot, unknownKnot),
      listOf(Connection(101, 102, 1200), Connection(102, 103, 1100)),
    )

  private fun platesOf(session: PlanningSession) =
    features(MapFeatures.routeKnotsLayer(session)).map {
      it.jsonObject.getValue("properties").jsonObject.getValue("plate").toString()
    }

  @Test
  fun `route across the border`() {
    val route = PlanningSession.start(borderGraph, listOf(dutchKnot, belgianKnot))

    assertEquals(listOf("\"nl\"", "\"be\""), platesOf(route))
  }

  @Test
  fun `data from before sign regions`() {
    val withoutRegions =
      NetworkGraph(
        borderGraph.knots.values.map { it.copy(signRegion = null) },
        borderGraph.connections,
      )
    val route =
      PlanningSession.start(
        withoutRegions,
        listOf(withoutRegions.knots.getValue(101), withoutRegions.knots.getValue(103)),
      )

    assertEquals(listOf("\"none\"", "\"none\"", "\"none\""), platesOf(route))
  }

  @Test
  fun `a round trip marker keeps its plate`() {
    val roundTrip = PlanningSession.start(borderGraph, listOf(dutchKnot, belgianKnot, dutchKnot))

    assertEquals(listOf("\"nl\"", "\"be\""), platesOf(roundTrip))
  }

  // Requirement: Knots signposted at several places

  private val knot02 =
    Knot(2, "02", NetworkType.CYCLING, 52.1, 5.1, listOf(GeoPoint(52.1012, 5.0985)))
  private val knot05 = Knot(5, "05", NetworkType.CYCLING, 52.12, 5.1)
  private val signpostGraph = NetworkGraph(listOf(knot02, knot05), listOf(Connection(2, 5, 2000)))

  private fun coordinatesOf(feature: kotlinx.serialization.json.JsonElement) =
    feature.jsonObject.getValue("geometry").jsonObject.getValue("coordinates").jsonArray.let {
      GeoPoint(it[1].toString().toDouble(), it[0].toString().toDouble())
    }

  @Test
  fun `badges at both signposts`() {
    val features = features(MapFeatures.knots(signpostGraph))
    val badges02 = features.filter {
      it.jsonObject.getValue("properties").jsonObject.getValue("ref").toString() == "\"02\""
    }

    assertEquals(3, features.size)
    assertEquals(knot02.positions, badges02.map(::coordinatesOf))
    assertEquals(
      1,
      badges02
        .map { it.jsonObject.getValue("properties").jsonObject.getValue("id") }
        .distinct()
        .size,
    )
  }

  // Requirement: Nearest knots

  @Test
  fun `no knots in the area`() {
    val locator = KnotLocator(graph.knots.values)

    assertTrue(locator.noKnotsIn(GeoBounds(50.0, 7.0, 50.1, 7.1)))
  }

  @Test
  fun `zoomed out too far`() {
    assertTrue(KnotLocator(graph.knots.values).noKnotsIn(null))
  }

  @Test
  fun `knots in view`() {
    val locator = KnotLocator(graph.knots.values)
    val around =
      GeoBounds(
        knot12.latitude - 0.001,
        knot12.longitude - 0.001,
        knot12.latitude + 0.001,
        knot12.longitude + 0.001,
      )

    assertFalse(locator.noKnotsIn(around))
  }

  @Test
  fun `only the other signpost in view counts as knots in view`() {
    val locator = KnotLocator(signpostGraph.knots.values)

    assertFalse(locator.noKnotsIn(GeoBounds(52.1011, 5.0984, 52.1013, 5.0986)))
  }

  @Test
  fun `a network without knots has nothing to show`() {
    assertFalse(KnotLocator(emptyList()).noKnotsIn(null))
    assertNull(KnotLocator(emptyList()).nearestPosition(52.0, 5.0))
  }

  @Test
  fun `the nearest knot however far`() {
    val nearest = KnotLocator(graph.knots.values).nearestPosition(40.0, 5.0)

    val positions = graph.knots.values.flatMap { it.positions }
    val southernmost = positions.minBy { it.latitude }
    assertEquals(southernmost, nearest)
  }

  @Test
  fun `the nearest place a knot is signposted`() {
    val nearest = KnotLocator(signpostGraph.knots.values).nearestPosition(52.1013, 5.0985)

    assertEquals(GeoPoint(52.1012, 5.0985), nearest)
  }

  @Test
  fun `other network`() {
    val hiking = KnotLocator(listOf(knot05))

    assertEquals(
      knot05.positions.first(),
      hiking.nearestPosition(knot02.latitude, knot02.longitude),
    )
  }

  @Test
  fun `tapping the other signpost`() {
    val locator = KnotLocator(signpostGraph.knots.values)

    val tapped = assertNotNull(locator.nearestAtAnyNode(52.1012, 5.0985, 20.0))
    val started = PlanningSession.start(signpostGraph).tap(tapped)

    assertSame(knot02, tapped)
    assertEquals(listOf(knot02), started.waypoints)
  }

  @Test
  fun `route through a knot with two signposts`() {
    val route = PlanningSession.start(signpostGraph).tapAll(knot02, knot05)

    val features = features(MapFeatures.routeKnotsLayer(route))

    assertEquals(
      listOf("\"start\"", "\"start\"", "\"end\""),
      features.map { it.jsonObject.getValue("properties").jsonObject.getValue("role").toString() },
    )
    assertEquals(
      knot02.positions + GeoPoint(knot05.latitude, knot05.longitude),
      features.map(::coordinatesOf),
    )
    assertEquals(
      listOf(knot02, knot05),
      MapFeatures.routeKnots(route).map { it.knot },
      "once in the sheet",
    )
  }

  @Test
  fun `knots with equal numbers on both banks`() {
    val north = Knot(31, "30", NetworkType.CYCLING, 52.2, 5.1)
    val south = Knot(32, "30", NetworkType.CYCLING, 52.198, 5.1)
    val banks = NetworkGraph(listOf(north, south), listOf(Connection(31, 32, 300)))

    assertEquals(2, features(MapFeatures.knots(banks)).size)
    assertSame(south, KnotLocator(banks.knots.values).nearestAtAnyNode(52.198, 5.1, 20.0))
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

  // Requirement: What a chosen route point does

  private val home = TestNetwork.pointOn(knot12, knot30, 0.25)

  @Test
  fun `first waypoint`() {
    val planned = session.startAt(home).tap(knot45)

    assertEquals(listOf(home, knot45), planned.waypoints)
    assertTrue(planned.route?.summary is RouteSummary.Complete)
  }

  @Test
  fun `end before the start`() {
    val endOnly = session.endAt(home)

    assertEquals(WaypointRole.END, endOnly.roleOf(0))
    assertNull(endOnly.route)
    val planned = endOnly.tap(knot45)
    assertEquals(listOf(knot45, home), planned.waypoints)
    assertEquals(WaypointRole.START, planned.roleOf(0))
  }

  @Test
  fun `before an existing start`() {
    val planned = session.tapAll(knot12, knot45).startAt(home)

    assertEquals(listOf(home, knot12, knot45), planned.waypoints)
    assertEquals(WaypointRole.STOP, planned.roleOf(1))
  }

  @Test
  fun `after an existing end`() {
    val planned = session.tapAll(knot45, knot12).endAt(home)

    assertEquals(listOf(knot45, knot12, home), planned.waypoints)
    assertEquals(WaypointRole.STOP, planned.roleOf(1))
    assertEquals(WaypointRole.END, planned.roleOf(2))
  }

  @Test
  fun `an earlier route point`() {
    val other = TestNetwork.pointOn(knot30, knot45, 0.5)
    val planned = session.startAt(home).tap(knot45).startAt(other)

    assertEquals(listOf(other, home, knot45), planned.waypoints)
    assertEquals(listOf(other, knot45), planned.removeWaypoint(1).waypoints)
  }

  @Test
  fun `a round trip stops being one`() {
    val roundTrip = session.tapAll(knot12, knot45).returnToStart()

    val planned = roundTrip.endAt(home)

    assertEquals(listOf(knot12, knot45, knot12, home), planned.waypoints)
  }

  @Test
  fun `removing the route point`() {
    val planned = session.startAt(home).tap(knot12).tap(knot45)

    val removed = planned.removeWaypoint(0)

    assertEquals(listOf(knot12, knot45), removed.waypoints)
    assertNull(removed.routeGraph.knots[home.id])
  }

  @Test
  fun `out and back from home`() {
    val planned = session.startAt(home).tap(knot45).returnToStart()

    assertEquals(listOf(home, knot45, home), planned.waypoints)
    val summary = assertIs<RouteSummary.Complete>(planned.route?.summary)
    assertEquals(8000, summary.lengthMeters)
  }

  @Test
  fun `a stop at a place`() {
    val stop = TestNetwork.pointOn(knot30, knot45, 0.5)

    val planned = session.tapAll(knot12, knot45).addStopAt(stop)

    assertEquals(listOf(knot12, stop, knot45), planned.waypoints)
    assertEquals(WaypointRole.STOP, planned.roleOf(1))
  }

  @Test
  fun `route points are only in the route graph`() {
    val planned = session.startAt(home).tap(knot45)

    assertNull(planned.graph.knots[home.id])
    assertEquals(home, planned.routeGraph.knots[home.id])
  }

  @Test
  fun `a found way replaces the straight line`() {
    val straight = TestNetwork.pointOn(knot12, knot30, 0.25, wayMeters = 320)
    val planned = session.startAt(straight).tap(knot45)
    val way = straight.routePoint!!.way
    val found =
      straight.copy(
        routePoint =
          straight.routePoint!!.copy(
            way =
              listOf(
                way.first(),
                GeoPoint(way.first().latitude, way.first().longitude + 0.003),
                way.last(),
              ),
            straight = false,
          )
      )

    val replaced = planned.withRoutePoint(found)

    assertEquals(listOf(found, knot45), replaced.waypoints)
    val before = assertIs<RouteSummary.Complete>(planned.route?.summary).lengthMeters
    val after = assertIs<RouteSummary.Complete>(replaced.route?.summary).lengthMeters
    assertTrue(after > before, "$after > $before")
  }

  // Requirement: Route points from a place

  /** [home] as made from the café "Eiscafé Riviera". */
  private val cafe =
    home.copy(
      routePoint = home.routePoint!!.copy(placeKind = "food", placeName = "Eiscafé Riviera")
    )

  @Test
  fun `start that becomes a stop`() {
    val other = TestNetwork.pointOn(knot30, knot45, 0.5)
    val planned = session.startAt(cafe).tap(knot45).startAt(other)

    val stop = planned.waypoints[1].routePoint!!
    assertEquals("food", stop.placeKind)
    assertEquals("Eiscafé Riviera", stop.placeName)
  }

  @Test
  fun `kept after a restart`() {
    val planned = session.startAt(cafe).tap(knot45)

    val restored = RouteSnapshot.parse(RouteSnapshot.of(planned).toJson()).restore(graph)

    val start = restored.session.waypoints.first().routePoint!!
    assertEquals("food", start.placeKind)
    assertEquals("Eiscafé Riviera", start.placeName)
  }

  @Test
  fun `route saved before`() {
    val json =
      RouteSnapshot.of(session.startAt(home).tap(knot45))
        .toJson()
        .replace(Regex(""",\s*"place(Kind|Name)":\s*(null|"[^"]*")"""), "")
    assertFalse(json.contains("placeKind"))

    val start = RouteSnapshot.parse(json).restore(graph).session.waypoints.first().routePoint!!
    assertNull(start.placeKind)
    assertNull(start.placeName)
  }

  @Test
  fun `the place on the map`() {
    val features = features(MapFeatures.routeKnotsLayer(session.startAt(cafe).tap(knot45)))
    val places = features.map {
      it.jsonObject["properties"]!!.jsonObject["place"]!!.jsonPrimitive.content
    }
    // The café first, then the knots, which have none.
    assertEquals("food", places.first())
    assertTrue(places.drop(1).all { it.isEmpty() }, "$places")
  }

  // Requirement: Keeping route points

  @Test
  fun `after a restart`() {
    val away = TestNetwork.pointOn(knot12, knot30, 0.25, wayMeters = 320)
    val found = away.copy(routePoint = away.routePoint!!.copy(straight = false))
    val planned = session.startAt(found).tap(knot45)

    // Restored from the graph alone: no geometry, no path data, no internet access.
    val restored = RouteSnapshot.parse(RouteSnapshot.of(planned).toJson()).restore(graph)

    assertEquals(planned.waypoints, restored.session.waypoints)
    assertEquals(listOf(), restored.removedWaypoints)
    assertEquals(
      assertIs<RouteSummary.Complete>(planned.route?.summary).lengthMeters,
      assertIs<RouteSummary.Complete>(restored.session.route?.summary).lengthMeters,
    )
  }

  @Test
  fun `connection gone`() {
    val point = TestNetwork.pointOn(knot12, knot30, 0.25, wayMeters = 320)
    val snapshot = RouteSnapshot.of(session.startAt(point).tap(knot45))
    // An update removed 12-30.
    val updated =
      NetworkGraph(
        graph.knots.values,
        graph.connections.filterNot { it.fromId == 12L && it.toId == 30L },
      )
    val geometry = NetworkGeometry.straightLines(updated)

    val restored = snapshot.restore(updated).session

    assertEquals(2, restored.waypoints.size, "the point stays in the route")
    val reattached = restored.reattached(geometry)
    val routePoint = assertNotNull(reattached.routePoints.single().routePoint)
    assertTrue(updated.connectionBetween(routePoint.fromId, routePoint.toId) != null)
    assertTrue(reattached.route?.summary is RouteSummary.Complete)
  }

  @Test
  fun `older route`() {
    val json = """{"network":"cycling","waypoints":[{"id":12,"ref":"12"},{"id":45,"ref":"45"}]}"""

    assertEquals(listOf(knot12, knot45), RouteSnapshot.parse(json).restore(graph).session.waypoints)
  }
}
