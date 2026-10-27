package com.knoopbyknoop.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The route being planned, as the apps store it to restore it after a restart, see `map-planning`.
 *
 * Waypoints are stored by knot ID, plus their number to tell the user which ones could not be
 * restored.
 */
@Serializable
data class RouteSnapshot(
  val region: String,
  val network: IndexedNetworkType,
  val waypoints: List<Waypoint>,
) {

  @Serializable data class Waypoint(val id: Long, val ref: String)

  fun toJson(): String = json.encodeToString(this)

  /** Whether this route is planned on [region], so removing that region discards it. */
  fun isFor(region: String): Boolean = this.region == region

  /** Restores the route on [graph], the installed graph of [region] and [network]. */
  fun restore(graph: NetworkGraph): RestoredRoute {
    val (present, missing) = waypoints.partition { it.id in graph.knots }
    val session = PlanningSession.start(region, graph, present.map { graph.knots.getValue(it.id) })
    return RestoredRoute(session, missing.map { it.ref })
  }

  companion object {

    private val json = Json { ignoreUnknownKeys = true }

    fun of(session: PlanningSession): RouteSnapshot =
      RouteSnapshot(
        region = session.region,
        network = IndexedNetworkType.of(session.network),
        waypoints = session.waypoints.map { Waypoint(it.id, it.ref) },
      )

    @Throws(IllegalArgumentException::class)
    fun parse(text: String): RouteSnapshot = json.decodeFromString(text)
  }
}

/**
 * @property removedWaypoints the numbers of the waypoints whose knots no longer exist, e.g. after a
 *   data update
 */
data class RestoredRoute(val session: PlanningSession, val removedWaypoints: List<String>)
