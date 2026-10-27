package com.knoopbyknoop.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The published list of all available graph files (`index.json`).
 *
 * @property generatedAt when the pipeline wrote this index, as ISO instant
 */
@Serializable
data class NetworkIndex(
  val formatVersion: Int,
  val generatedAt: String,
  val files: List<NetworkIndexEntry>,
) {

  fun toJson(): String = json.encodeToString(this)

  companion object {

    private val json = Json {
      encodeDefaults = true
      prettyPrint = true
      ignoreUnknownKeys = true
    }

    @Throws(IllegalArgumentException::class)
    fun parse(text: String): NetworkIndex = json.decodeFromString(text)
  }
}

/**
 * One downloadable graph file.
 *
 * @property file the file name, relative to the index
 * @property sha256 the lowercase hex SHA-256 of the file
 * @property kind what the file contains; indexes written before geometry files existed have none
 */
@Serializable
data class NetworkIndexEntry(
  val region: String,
  val network: IndexedNetworkType,
  val file: String,
  val sizeBytes: Long,
  val sha256: String,
  val dataDate: String,
  val kind: IndexedFileKind = IndexedFileKind.GRAPH,
) {

  /** Whether [bytes] are exactly the file this entry describes, so they are safe to install. */
  fun matches(bytes: ByteArray): Boolean =
    bytes.size.toLong() == sizeBytes && sha256Hex(bytes) == sha256
}

/** The kinds of files a region and network type is published as. */
@Serializable
enum class IndexedFileKind {
  /** A [GraphFile], needed for routing. */
  @SerialName("graph") GRAPH,

  /** A [NetworkGeometry] file, needed to draw connections along their actual course. */
  @SerialName("geometry") GEOMETRY,
}

/** [NetworkType] as it is written to the index. */
@Serializable
enum class IndexedNetworkType(val network: NetworkType) {
  @SerialName("cycling") CYCLING(NetworkType.CYCLING),
  @SerialName("hiking") HIKING(NetworkType.HIKING);

  companion object {
    fun of(network: NetworkType): IndexedNetworkType = entries.first { it.network == network }
  }
}

/** The lowercase hex SHA-256 digest of [bytes]. */
expect fun sha256Hex(bytes: ByteArray): String
