package com.knoopbyknoop.core

import com.knoopbyknoop.core.TestNetwork.graph
import com.knoopbyknoop.core.TestNetwork.knot12
import com.knoopbyknoop.core.TestNetwork.knot30
import com.knoopbyknoop.core.TestNetwork.knot7
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Covers the core part of `map-planning` "Route and bridges" and "Map content above and below
 * bridges" of `openspec/changes/show-route-on-bridges`: the lines come split at bridges.
 */
class BridgeLinesTest {

  /** Every connection runs through four points between its knots, six in all. */
  private val courses =
    graph.connections.map { connection ->
      val from = graph.knots.getValue(connection.fromId)
      val to = graph.knots.getValue(connection.toId)
      (0..5).map {
        GeoPoint(
          from.latitude + (to.latitude - from.latitude) * it / 5,
          from.longitude + (to.longitude - from.longitude) * it / 5,
        )
      }
    }

  /** 12–30 runs over a bridge between its third and fourth point; 30–7 has none. */
  private val withBridges =
    NetworkGeometry(graph, courses, bridges = mapOf(0 to listOf(Stretch(2, 3))))

  private fun features(geoJson: String): List<JsonObject> =
    Json.parseToJsonElement(geoJson).jsonObject.getValue("features").jsonArray.map {
      it.jsonObject
    }

  private fun bridgeFlags(geoJson: String) =
    features(geoJson).map {
      it.getValue("properties").jsonObject.getValue("bridge").jsonPrimitive.content.toBoolean()
    }

  private fun routeThrough(vararg knots: Knot) = PlanningSession.start(graph, knots.toList())

  @Test
  fun `route over a bridge`() {
    val route = MapFeatures.route(routeThrough(knot12, knot30), withBridges)

    assertEquals(listOf(false, true, false), bridgeFlags(route))
    val bridge = features(route)[1].getValue("geometry").jsonObject.getValue("coordinates")
    assertEquals(2, bridge.jsonArray.size, "the bridge piece runs from its start to its end")
  }

  @Test
  fun `over and under the same bridge`() {
    // Out over the bridge on 12–30, back the same way reversed, then on 30–7, which passes under.
    val route = MapFeatures.route(routeThrough(knot12, knot30, knot12, knot30, knot7), withBridges)

    assertEquals(
      listOf(false, true, false) + listOf(false, true, false) + listOf(false, true, false) + false,
      bridgeFlags(route),
    )
  }

  @Test
  fun `connections and bridges`() {
    val flags = bridgeFlags(MapFeatures.connections(withBridges))

    assertEquals(graph.connections.size + 2, flags.size, "12–30 in three pieces, the rest whole")
    assertEquals(1, flags.count { it })
  }

  @Test
  fun `line ahead while navigating`() {
    // The rider is halfway along the bridge: the line ahead starts on it.
    val course = courses[0]
    val rider =
      GeoPoint(
        (course[2].latitude + course[3].latitude) / 2,
        (course[2].longitude + course[3].longitude) / 2,
      )

    val ahead = MapFeatures.legAhead(listOf(rider) + course.drop(3), withBridges)

    assertEquals(listOf(true, false), bridgeFlags(ahead))
  }

  @Test
  fun `geometry without bridge stretches`() {
    val plain = NetworkGeometry(graph, courses)

    assertEquals(listOf(false), bridgeFlags(MapFeatures.route(routeThrough(knot12, knot30), plain)))
    assertEquals(graph.connections.size, bridgeFlags(MapFeatures.connections(plain)).size)
    assertEquals(false, plain.hasBridges)
    assertEquals(true, withBridges.hasBridges)
  }

  @Test
  fun `same transparency everywhere`() {
    // The pieces of a leg differ only in `bridge`, so the apps draw them with the same paint.
    val properties =
      features(MapFeatures.route(routeThrough(knot12, knot30), withBridges)).map {
        it.getValue("properties").jsonObject.filterKeys { key -> key != "bridge" }
      }

    assertEquals(1, properties.toSet().size)
  }
}
