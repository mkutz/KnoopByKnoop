package com.knoopbyknoop.core

/**
 * A numbered junction ("knooppunt") of a node network.
 *
 * @property id the OpenStreetMap node ID, which is globally unique
 * @property ref the number on the signpost, which is only unique within its region
 * @property network the type of node network this knot belongs to
 * @property otherNodes the positions of the other nodes the knot is tagged on, if mappers tagged
 *   the junction several times, see `network-data` "One knot per signposted junction"
 * @property routePoint where a route point, a chosen place that is no knot, joins the network, see
 *   `route-planning` "Route from waypoints"; `null` for a knot. A route point has an empty [ref]
 *   and a negative [id], and is only part of a session's route graph, never of an installed
 *   network.
 */
data class Knot(
  val id: Long,
  val ref: String,
  val network: NetworkType,
  val latitude: Double,
  val longitude: Double,
  val otherNodes: List<GeoPoint> = listOf(),
  val routePoint: RoutePoint? = null,
) {

  /** Whether this is a route point rather than a numbered knot. */
  val isRoutePoint: Boolean
    get() = routePoint != null

  /** The knot's own position first, then those of its [otherNodes]. */
  val positions: List<GeoPoint>
    get() = listOf(GeoPoint(latitude, longitude)) + otherNodes
}

/** The node network types known from OpenStreetMap (`network:type=node_network`). */
enum class NetworkType(val osmRefTag: String) {
  CYCLING("rcn_ref"),
  HIKING("rwn_ref"),
}
