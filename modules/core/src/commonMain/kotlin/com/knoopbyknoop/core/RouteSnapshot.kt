package com.knoopbyknoop.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The route being planned, as the apps store it to restore it after a restart, see `map-planning`.
 *
 * Waypoints are stored by knot ID, plus their number to tell the user which ones could not be
 * restored. Snapshots written while networks were installed per region also have a `region`, which
 * is ignored.
 */
@Serializable
data class RouteSnapshot(
  val network: IndexedNetworkType,
  val waypoints: List<Waypoint>,
) {

  /**
   * @property point for a route point, where it is and how it joins the network, see `map-planning`
   *   "Keeping route points"
   * @property label the waypoint's text from an opened route file, see `route-sharing` "Knot texts
   *   in a route file"
   */
  @Serializable
  data class Waypoint(
    val id: Long,
    val ref: String,
    val point: StoredPoint? = null,
    val label: String? = null,
  ) {

    internal fun toKnot(network: NetworkType): Knot? = point?.toKnot(id, network)
  }

  /** A route point as stored: everything needed to restore it without geometry or path data. */
  @Serializable
  data class StoredPoint(
    val latitude: Double,
    val longitude: Double,
    val fromId: Long,
    val toId: Long,
    val fraction: Double,
    val placeLatitude: Double,
    val placeLongitude: Double,
    val way: List<List<Double>> = listOf(),
    val straight: Boolean = false,
    /** See [RoutePoint.placeKind]; missing in routes saved before `route-points-from-places`. */
    val placeKind: String? = null,
    val placeName: String? = null,
  ) {

    internal fun toKnot(id: Long, network: NetworkType) =
      Knot(
        id,
        "",
        network,
        latitude,
        longitude,
        routePoint =
          RoutePoint(
            fromId,
            toId,
            fraction,
            GeoPoint(placeLatitude, placeLongitude),
            way.map { GeoPoint(it[0], it[1]) },
            straight,
            placeKind,
            placeName,
          ),
      )

    companion object {
      internal fun of(knot: Knot): StoredPoint? =
        knot.routePoint?.let {
          StoredPoint(
            knot.latitude,
            knot.longitude,
            it.fromId,
            it.toId,
            it.fraction,
            it.place.latitude,
            it.place.longitude,
            it.way.map { point -> listOf(point.latitude, point.longitude) },
            it.straight,
            it.placeKind,
            it.placeName,
          )
        }
    }
  }

  fun toJson(): String = json.encodeToString(this)

  /** Restores the route on [graph], the installed graph of [network]. */
  fun restore(graph: NetworkGraph): RestoredRoute {
    val network = requireNotNull(graph.network) { "Cannot restore on an empty network" }
    val (present, missing) = waypoints.partition { it.point != null || it.id in graph.knots }
    val knots = present.map { it.toKnot(network) ?: graph.knots.getValue(it.id) }
    val labels =
      present.mapNotNull { waypoint -> waypoint.label?.let { waypoint.id to it } }.toMap()
    return RestoredRoute(PlanningSession.start(graph, knots, labels), missing.map { it.ref })
  }

  companion object {

    private val json = Json { ignoreUnknownKeys = true }

    fun of(session: PlanningSession): RouteSnapshot =
      RouteSnapshot(
        network = IndexedNetworkType.of(session.network),
        waypoints =
          session.waypoints.map {
            Waypoint(it.id, it.ref, StoredPoint.of(it), session.labelOf(it))
          },
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
