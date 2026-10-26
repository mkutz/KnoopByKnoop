package com.knoopbyknoop.pipeline

import com.google.protobuf.ByteString
import crosby.binary.Osmformat
import crosby.binary.file.BlockOutputStream
import crosby.binary.file.FileBlock
import java.nio.file.Path
import kotlin.io.path.outputStream
import kotlin.math.roundToLong

/** Writes small PBF files for tests, with dense nodes like real extracts use. */
object PbfWriter {

  fun write(
    path: Path,
    replicationTimestamp: Long,
    nodes: List<OsmNode>,
    ways: List<OsmWay>,
    relations: List<OsmRelation>,
  ) {
    val strings = StringTable()
    val denseNodes = Osmformat.DenseNodes.newBuilder()
    var previousId = 0L
    var previousLatitude = 0L
    var previousLongitude = 0L
    for (node in nodes) {
      val latitude = (node.latitude * 10_000_000).roundToLong()
      val longitude = (node.longitude * 10_000_000).roundToLong()
      denseNodes.addId(node.id - previousId)
      denseNodes.addLat(latitude - previousLatitude)
      denseNodes.addLon(longitude - previousLongitude)
      node.tags.forEach { (key, value) ->
        denseNodes.addKeysVals(strings.id(key)).addKeysVals(strings.id(value))
      }
      denseNodes.addKeysVals(0)
      previousId = node.id
      previousLatitude = latitude
      previousLongitude = longitude
    }

    val wayGroup = Osmformat.PrimitiveGroup.newBuilder()
    for (way in ways) {
      var previousNodeId = 0L
      wayGroup.addWays(
        Osmformat.Way.newBuilder()
          .setId(way.id)
          .addAllRefs(way.nodeIds.map { (it - previousNodeId).also { _ -> previousNodeId = it } })
      )
    }

    val relationGroup = Osmformat.PrimitiveGroup.newBuilder()
    for (relation in relations) {
      var previousMemberId = 0L
      val builder = Osmformat.Relation.newBuilder().setId(relation.id)
      relation.tags.forEach { (key, value) ->
        builder.addKeys(strings.id(key)).addVals(strings.id(value))
      }
      for (wayId in relation.memberWayIds) {
        builder
          .addMemids(wayId - previousMemberId)
          .addTypes(Osmformat.Relation.MemberType.WAY)
          .addRolesSid(strings.id(""))
        previousMemberId = wayId
      }
      relationGroup.addRelations(builder)
    }

    val header =
      Osmformat.HeaderBlock.newBuilder()
        .addRequiredFeatures("OsmSchema-V0.6")
        .addRequiredFeatures("DenseNodes")
        .setOsmosisReplicationTimestamp(replicationTimestamp)
        .build()
    val data =
      Osmformat.PrimitiveBlock.newBuilder()
        .addPrimitivegroup(Osmformat.PrimitiveGroup.newBuilder().setDense(denseNodes))
        .addPrimitivegroup(wayGroup)
        .addPrimitivegroup(relationGroup)
        .setStringtable(strings.build())
        .build()

    path.outputStream().use { output ->
      BlockOutputStream(output).apply {
        write(FileBlock.newInstance("OSMHeader", header.toByteString(), null))
        write(FileBlock.newInstance("OSMData", data.toByteString(), null))
        flush()
      }
    }
  }

  private class StringTable {
    // Index 0 is reserved, it separates the tags of dense nodes.
    private val strings = mutableListOf("")

    fun id(string: String): Int =
      strings.indexOf(string).takeIf { it > 0 } ?: strings.size.also { strings.add(string) }

    fun build(): Osmformat.StringTable =
      Osmformat.StringTable.newBuilder()
        .addAllS(strings.map { ByteString.copyFromUtf8(it) })
        .build()
  }
}
