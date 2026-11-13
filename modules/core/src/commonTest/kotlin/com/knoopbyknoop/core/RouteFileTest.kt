package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers `route-sharing` of `openspec/changes/share-routes`.
 *
 * A network along the Sieg: Siegburg 02 → 28 → 29 → 30 → 83 → 27 → 73 to the west, 2 km apart, and
 * back from 73 over 25 to 02. Decoys share numbers: a 28 1 km east of 02 not connected to it, and
 * Bonn's own 02 next to 73.
 */
class RouteFileTest {

  private val siegburg = Place("Siegburg")
  private val troisdorf = Place("Troisdorf")
  private val bonn = Place("Bonn")

  private fun knot(
    id: Long,
    ref: String,
    east: Double,
    north: Double = 0.0,
    place: Place? = null,
    network: NetworkType = NetworkType.CYCLING,
  ) =
    Knot(
      id,
      ref,
      network,
      LATITUDE + north / METERS_PER_DEGREE_LATITUDE,
      LONGITUDE + east / metersPerDegreeLongitude,
      place = place,
    )

  private fun knots(network: NetworkType) =
    listOf(
      knot(6364701, "02", 0.0, place = siegburg, network = network),
      knot(6364702, "28", -2_000.0, place = siegburg, network = network),
      knot(6364703, "29", -4_000.0, place = troisdorf, network = network),
      knot(6364704, "30", -6_000.0, place = troisdorf, network = network),
      knot(6364705, "83", -8_000.0, place = bonn, network = network),
      knot(6364706, "27", -9_000.0, north = -1_000.0, place = bonn, network = network),
      knot(6364707, "73", -10_000.0, north = -2_000.0, place = bonn, network = network),
      knot(6364708, "25", -5_000.0, north = -1_000.0, place = troisdorf, network = network),
      knot(6364790, "28", 1_000.0, place = siegburg, network = network),
      knot(6364791, "02", -10_500.0, north = -2_500.0, place = bonn, network = network),
    )

  private val connections =
    listOf(
      Connection(6364701, 6364702, 2_000),
      Connection(6364702, 6364703, 2_000),
      Connection(6364703, 6364704, 2_000),
      Connection(6364704, 6364705, 2_000),
      Connection(6364705, 6364706, 1_400),
      Connection(6364706, 6364707, 1_400),
      Connection(6364707, 6364708, 3_000),
      Connection(6364708, 6364701, 3_000),
      Connection(6364707, 6364791, 700),
    )

  private val graph = NetworkGraph(knots(NetworkType.CYCLING), connections)
  private val hikingGraph = NetworkGraph(knots(NetworkType.HIKING), connections)

  private val geometry =
    NetworkGeometry(
      graph,
      graph.connections.map { connection ->
        val from = graph.knots.getValue(connection.fromId)
        val to = graph.knots.getValue(connection.toId)
        listOf(GeoPoint(from.latitude, from.longitude), GeoPoint(to.latitude, to.longitude))
      },
    )

  private fun knotOf(id: Long) = graph.knots.getValue(id)

  private val k02 = knotOf(6364701)
  private val k28 = knotOf(6364702)
  private val k83 = knotOf(6364705)
  private val k73 = knotOf(6364707)

  private fun session(vararg waypoints: Knot, on: NetworkGraph = graph) =
    PlanningSession.start(on, waypoints.toList())

  private fun route(vararg waypoints: Knot) = session(*waypoints).route!!

  private fun open(
    text: String,
    on: NetworkGraph = graph,
  ): Pair<RouteFile.Resolved, PlanningSession> {
    val resolved = assertNotNull(RouteFile.resolve(RouteFile.parse(text), on))
    return resolved to resolved.snapshot.restore(on).session
  }

  /** A route point at [position] on the connection 25–02, near 02. */
  private fun flag(position: GeoPoint, name: String? = null): Knot =
    RouteSnapshot(
        IndexedNetworkType.CYCLING,
        listOf(
          RouteSnapshot.Waypoint(
            routePointIdOf(position),
            "",
            RouteSnapshot.StoredPoint(
              position.latitude,
              position.longitude,
              fromId = 6364708,
              toId = 6364701,
              fraction = 0.9,
              placeLatitude = k02.latitude,
              placeLongitude = k02.longitude,
              placeName = name,
            ),
          )
        ),
      )
      .restore(graph)
      .session
      .waypoints
      .single()

  // Scenario: Route as a file
  // Scenario: Towns in the file
  @Test
  fun `a route is written as its knots with towns where they change`() {
    val file = RouteFile.write(route(k02, k83, k73), "Siegburg Bonn and back")

    assertEquals(
      """
      # Siegburg Bonn and back

      - 02 6364701 Siegburg
        - 28
        - 29 Troisdorf
        - 30
      - 83 Bonn
        - 27
      - 73

      """
        .trimIndent(),
      file,
    )
  }

  // Scenario: Hiking route as a file
  @Test
  fun `a hiking route names its network below the title`() {
    val hiking =
      session(
        hikingGraph.knots.getValue(6364701),
        hikingGraph.knots.getValue(6364702),
        on = hikingGraph,
      )

    val lines = RouteFile.write(hiking.route!!, "Short walk").lines()

    assertEquals(listOf("# Short walk", "Network: hiking"), lines.take(2))
    assertEquals(NetworkType.HIKING, RouteFile.parse(lines.joinToString("\n")).network)
  }

  // Scenario: Route point as a file
  // Scenario: Home flag shared as it is
  @Test
  fun `a route point is written as its position and the next knot carries the id`() {
    val home = flag(GeoPoint(50.7821, 7.2154))

    val lines = RouteFile.write(route(home, k28), "Home to 28").lines()

    assertEquals("- 50.78210, 7.21540", lines[2])
    assertEquals("  - 02 6364701 Siegburg", lines[3])
    assertEquals("- 28", lines[4])
  }

  // Scenario: Route point from a place as a file
  @Test
  fun `a route point from a place is written with its name`() {
    val cafe = flag(GeoPoint(50.7821, 7.2154), "Eiscafé Riviera")

    val lines = RouteFile.write(route(k28, cafe), "To the café").lines()

    assertEquals("- 50.78210, 7.21540 Eiscafé Riviera", lines.last { it.isNotEmpty() })
  }

  // Scenario: Round trip as a file
  @Test
  fun `a round trip starts and ends with the same knot`() {
    val items =
      RouteFile.write(route(k02, k83, k02), "Round").lines().filter { it.startsWith("- ") }

    assertEquals("- 02 6364701 Siegburg", items.first())
    assertEquals("- 02", items.last())
  }

  // Scenario: Own text for a stop
  @Test
  fun `a stop's own text is shown instead of its town and kept`() {
    val (_, opened) =
      open(
        "# Lunch\n\n- 02 6364701 Siegburg\n  - 28\n  - 29\n  - 30\n- 83 lunch at the Fährhaus\n  - 27\n- 73"
      )

    assertEquals("lunch at the Fährhaus", opened.labelOf(k83))
    assertNull(opened.labelOf(k02))
    val restarted = RouteSnapshot.parse(RouteSnapshot.of(opened).toJson()).restore(graph).session
    assertEquals("lunch at the Fährhaus", restarted.labelOf(k83))
    val history = RouteHistory().record(opened, opened.removeWaypoint(2))
    val (undone, _) = assertNotNull(history.undo(graph))
    assertEquals("lunch at the Fährhaus", undone.session.labelOf(k83))
    assertTrue(
      "- 83 lunch at the Fährhaus" in RouteFile.write(opened.route!!, "Lunch", opened.labels)
    )
  }

  @Test
  fun `a route stored before texts existed is restored without them`() {
    val stored =
      RouteSnapshot.parse(
        """{"network":"cycling","waypoints":[{"id":6364701,"ref":"02"},{"id":6364705,"ref":"83"}]}"""
      )

    val restored = stored.restore(graph).session

    assertEquals(listOf(k02, k83), restored.waypoints)
    assertTrue(restored.labels.isEmpty())
  }

  // Scenario: Knot found as a neighbour
  @Test
  fun `a number is found among the knots connected to the one before`() {
    val (resolved, opened) = open("# A\n- 02 6364701 Siegburg\n  - 28\n- 29")

    assertEquals(listOf(6364701L, 6364702L, 6364703L), resolved.listed)
    assertEquals(listOf(k02, knotOf(6364703)), opened.waypoints)
  }

  // Scenario: Round trip back to the start
  @Test
  fun `the last 02 is the one connected to the knot before and not the nearest`() {
    val (_, opened) = open("# Back\n- 73 6364707 Bonn\n  - 25\n- 02 Siegburg")

    assertEquals(listOf(k73, k02), opened.waypoints)
  }

  // Scenario: Hand-written file without an id
  @Test
  fun `a hand-written file without an id is found by number and town`() {
    val (resolved, opened) = open("# Along the Sieg\n- 02 Siegburg\n- 28")

    assertEquals(IndexedNetworkType.CYCLING, resolved.snapshot.network)
    assertEquals(listOf(k02, k28), opened.waypoints)
  }

  // Scenario: Knot skipped in a hand-written list
  @Test
  fun `a knot not connected to the one before is found nearby`() {
    val (_, opened) = open("# Skip\n- 02 Siegburg\n- 29")

    assertEquals(listOf(k02, knotOf(6364703)), opened.waypoints)
  }

  // Scenario: Knot not found
  @Test
  fun `a knot that cannot be found is left out and the next found from the one before`() {
    val (resolved, opened) = open("# Gap\n- 02 Siegburg\n  - 28\n  - 29\n  - 30\n  - 45\n- 83")

    assertEquals(listOf("45"), resolved.leftOut)
    assertEquals(listOf(k02, k83), opened.waypoints)
  }

  // Scenario: Route different from the list
  @Test
  fun `a route different from the file's list is told`() {
    val (resolved, opened) = open("# Other way\n- 02 Siegburg\n  - 25\n  - 73\n  - 27\n- 83")

    assertTrue(RouteFile.differsFrom(opened.route!!, resolved.listed))
  }

  @Test
  fun `a written route is read back as the same route`() {
    val original = route(k02, k83, k02)

    val (resolved, opened) = open(RouteFile.write(original, "Round"))

    assertEquals(original.waypoints, opened.waypoints)
    assertFalse(RouteFile.differsFrom(opened.route!!, resolved.listed))
    assertTrue(resolved.leftOut.isEmpty())
  }

  // Scenario: Other lines skipped
  @Test
  fun `notes and unknown lines are skipped`() {
    val (_, opened) =
      open(
        "# Notes\n\nA nice ride along the river.\nDate: 2027-05-01\n\n- 02 6364701 Siegburg\n- 28"
      )

    assertEquals(listOf(k02, k28), opened.waypoints)
  }

  // Scenario: Not a route file
  @Test
  fun `a file without two waypoints that can be found holds no route`() {
    assertNull(RouteFile.resolve(RouteFile.parse("Hello, this is a letter."), graph))
    assertNull(RouteFile.resolve(RouteFile.parse("# Nowhere\n- 45\n- 99"), graph))
  }

  // Scenario: Route point opened
  @Test
  fun `a route point from a file joins the nearest course with its name`() {
    val position = GeoPoint(LATITUDE + 0.001, LONGITUDE + -3_000.0 / metersPerDegreeLongitude)
    val text =
      "# Café\n- 02 6364701 Siegburg\n- ${position.latitude}, ${position.longitude} Eiscafé Riviera"

    val (_, opened) = open(text)
    val joined = opened.reattached(geometry)

    val point = assertNotNull(joined.waypoints.last().routePoint)
    assertEquals("Eiscafé Riviera", point.placeName)
    assertEquals(setOf(6364702L, 6364703L), setOf(point.fromId, point.toId))
    assertNotNull(joined.route)
  }

  // Scenario: Share a route
  @Test
  fun `the file is named like the GPX export with its own extension`() {
    val route = route(k02, k83, k73)

    assertEquals("knoop-by-knoop-cycling-02-73.knoop", RouteFile.fileName(route))
    assertEquals("02 Siegburg → 73 Bonn", RouteFile.titleOf(route))
  }

  private companion object {
    const val LATITUDE = 50.79562
    const val LONGITUDE = 7.20311
    const val METERS_PER_DEGREE_LATITUDE = 6_371_008.8 * PI / 180
    val metersPerDegreeLongitude = METERS_PER_DEGREE_LATITUDE * cos(LATITUDE * PI / 180)
  }
}
