package com.knoopbyknoop.pipeline

import crosby.binary.BinaryParser
import crosby.binary.Osmformat
import crosby.binary.file.BlockInputStream
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.io.path.inputStream

/** An OpenStreetMap extract in the PBF format, e.g. from download.geofabrik.de. */
class PbfExtract(private val path: Path) {

  /**
   * Reads the extract in three passes (relations, ways, nodes) into [extraction].
   *
   * @return the date of the data, from the replication timestamp in the file's header
   */
  fun readInto(extraction: NetworkExtraction): LocalDate? {
    var dataDate: LocalDate? = null
    read(
      object : ElementParser() {
        override fun parse(header: Osmformat.HeaderBlock) {
          if (header.hasOsmosisReplicationTimestamp()) {
            dataDate =
              Instant.ofEpochSecond(header.osmosisReplicationTimestamp)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
          }
        }

        override fun parseRelations(relations: List<Osmformat.Relation>) {
          relations.forEach { extraction.acceptRelation(toOsmRelation(it)) }
        }
      }
    )
    read(
      object : ElementParser() {
        override fun parseWays(ways: List<Osmformat.Way>) {
          ways.forEach { extraction.acceptWay(toOsmWay(it)) }
        }
      }
    )
    read(
      object : ElementParser() {
        override fun parseNodes(nodes: List<Osmformat.Node>) {
          nodes.forEach { extraction.acceptNode(toOsmNode(it)) }
        }

        override fun parseDense(nodes: Osmformat.DenseNodes) {
          forEachDenseNode(nodes) { extraction.acceptNode(it) }
        }
      }
    )
    return dataDate
  }

  private fun read(parser: ElementParser) {
    path.inputStream().buffered().use { BlockInputStream(it, parser).process() }
  }

  /** A parser ignoring everything; each pass overrides only what it needs. */
  private abstract class ElementParser : BinaryParser() {

    override fun parse(header: Osmformat.HeaderBlock) {}

    override fun parseRelations(relations: List<Osmformat.Relation>) {}

    override fun parseWays(ways: List<Osmformat.Way>) {}

    override fun parseNodes(nodes: List<Osmformat.Node>) {}

    override fun parseDense(nodes: Osmformat.DenseNodes) {}

    override fun complete() {}

    fun toOsmRelation(relation: Osmformat.Relation): OsmRelation {
      var memberId = 0L
      val memberWayIds = mutableListOf<Long>()
      for (index in 0 until relation.memidsCount) {
        memberId += relation.getMemids(index)
        if (relation.getTypes(index) == Osmformat.Relation.MemberType.WAY)
          memberWayIds.add(memberId)
      }
      return OsmRelation(relation.id, tags(relation.keysList, relation.valsList), memberWayIds)
    }

    fun toOsmWay(way: Osmformat.Way): OsmWay {
      var nodeId = 0L
      return OsmWay(
        way.id,
        way.refsList.map { delta -> (nodeId + delta).also { nodeId = it } },
        tags(way.keysList, way.valsList),
      )
    }

    fun toOsmNode(node: Osmformat.Node): OsmNode =
      OsmNode(node.id, parseLat(node.lat), parseLon(node.lon), tags(node.keysList, node.valsList))

    /** Dense nodes store IDs and coordinates as deltas, and tags as one zero-separated list. */
    fun forEachDenseNode(nodes: Osmformat.DenseNodes, action: (OsmNode) -> Unit) {
      var id = 0L
      var latitude = 0L
      var longitude = 0L
      var keyValueIndex = 0
      for (index in 0 until nodes.idCount) {
        id += nodes.getId(index)
        latitude += nodes.getLat(index)
        longitude += nodes.getLon(index)
        var tags = mapOf<String, String>()
        if (nodes.keysValsCount > 0) {
          val nodeTags = HashMap<String, String>()
          while (nodes.getKeysVals(keyValueIndex) != 0) {
            nodeTags[getStringById(nodes.getKeysVals(keyValueIndex))] =
              getStringById(nodes.getKeysVals(keyValueIndex + 1))
            keyValueIndex += 2
          }
          keyValueIndex++
          tags = nodeTags
        }
        action(OsmNode(id, parseLat(latitude), parseLon(longitude), tags))
      }
    }

    private fun tags(keys: List<Int>, values: List<Int>): Map<String, String> =
      keys.indices.associate { getStringById(keys[it]) to getStringById(values[it]) }
  }
}
