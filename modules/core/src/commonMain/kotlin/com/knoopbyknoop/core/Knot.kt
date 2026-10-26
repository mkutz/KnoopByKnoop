package com.knoopbyknoop.core

/**
 * A numbered junction ("knooppunt") of a node network.
 *
 * @property id the OpenStreetMap node ID, which is globally unique
 * @property ref the number on the signpost, which is only unique within its region
 * @property network the type of node network this knot belongs to
 */
data class Knot(
  val id: Long,
  val ref: String,
  val network: NetworkType,
  val latitude: Double,
  val longitude: Double,
)

/** The node network types known from OpenStreetMap (`network:type=node_network`). */
enum class NetworkType(val osmRefTag: String) {
  CYCLING("rcn_ref"),
  HIKING("rwn_ref"),
}
