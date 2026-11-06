package com.knoopbyknoop.pipeline

/** The parts of OpenStreetMap elements the pipeline needs, independent of the file format. */
data class OsmNode(
  val id: Long,
  val latitude: Double,
  val longitude: Double,
  val tags: Map<String, String> = mapOf(),
)

data class OsmWay(val id: Long, val nodeIds: List<Long>, val tags: Map<String, String> = mapOf())

/**
 * @property memberRoles the role of each of [memberWayIds], e.g. `forward`; empty if none, see
 *   `network-data` "Course in each riding direction"
 */
data class OsmRelation(
  val id: Long,
  val tags: Map<String, String>,
  val memberWayIds: List<Long>,
  val memberRoles: List<String> = memberWayIds.map { "" },
)
