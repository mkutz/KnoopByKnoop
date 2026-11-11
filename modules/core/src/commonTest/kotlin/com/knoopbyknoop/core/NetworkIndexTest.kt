package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class NetworkIndexTest {

  private val fileBytes = "graph".encodeToByteArray()

  private val entry =
    NetworkIndexEntry(
      area = "netherlands",
      network = IndexedNetworkType.CYCLING,
      file = "netherlands-cycling.graph",
      sizeBytes = fileBytes.size.toLong(),
      sha256 = sha256Hex(fileBytes),
      dataDate = "2026-10-25",
    )

  @Test
  fun `an index reads back the same`() {
    val index = NetworkIndex(formatVersion = 1, generatedAt = "2026-10-26T03:00:00Z", listOf(entry))

    assertEquals(index, NetworkIndex.parse(index.toJson()))
  }

  @Test
  fun `network types are written in lowercase`() {
    val json = NetworkIndex(1, "2026-10-26T03:00:00Z", listOf(entry)).toJson()

    assertTrue("\"network\": \"cycling\"" in json, json)
  }

  private val paths = PathIndexEntry("cycling.paths", 1000, "2026-11-02", 2, 9, 100, "abc")

  /** The index as released apps knew it, with their parser's rules. */
  @Serializable
  private data class EarlierIndex(
    val formatVersion: Int,
    val generatedAt: String,
    val files: List<NetworkIndexEntry>,
  )

  @Test
  fun `An earlier app reads the new index`() {
    val json = NetworkIndex(1, "2026-11-02T03:00:00Z", listOf(entry), paths).toJson()

    val earlier = Json { ignoreUnknownKeys = true }.decodeFromString<EarlierIndex>(json)

    assertEquals(listOf(entry), earlier.files)
  }

  @Test
  fun `An index with path data reads back the same`() {
    val index = NetworkIndex(1, "2026-11-02T03:00:00Z", listOf(entry), paths)

    assertEquals(index, NetworkIndex.parse(index.toJson()))
  }

  @Test
  fun `An index without path data has no entry for it`() {
    val index = NetworkIndex(1, "2026-11-02T03:00:00Z", listOf(entry))

    assertFalse("paths" in index.toJson())
    assertEquals(null, NetworkIndex.parse(index.toJson()).paths)
  }

  private val places = PathIndexEntry("places.places", 500, "2026-11-10", 1, 9, 50, "def")

  /** The index as the apps knew it before place details, with their parser's rules. */
  @Serializable
  private data class IndexWithPaths(
    val formatVersion: Int,
    val generatedAt: String,
    val files: List<NetworkIndexEntry>,
    val paths: PathIndexEntry? = null,
  )

  @Test
  fun `An earlier app reads the index with place details`() {
    val json = NetworkIndex(1, "2026-11-10T03:00:00Z", listOf(entry), paths, places).toJson()

    val earlier = Json { ignoreUnknownKeys = true }.decodeFromString<IndexWithPaths>(json)

    assertEquals(listOf(entry), earlier.files)
    assertEquals(paths, earlier.paths)
    assertEquals(places, NetworkIndex.parse(json).places)
  }

  @Test
  fun `An index without place details has no entry for them`() {
    val index = NetworkIndex(1, "2026-11-10T03:00:00Z", listOf(entry), paths)

    assertFalse("places" in index.toJson())
    assertEquals(null, NetworkIndex.parse(index.toJson()).places)
  }

  @Test
  fun `The directory info describes the file`() {
    val square = PathSquare(800, 100)
    val point = GeoPoint(50.0, 6.25)
    val segment =
      PathSegment(1, 2, PathKind.CYCLE, true, true, 1.0, null, listOf(point, GeoPoint(50.01, 6.26)))
    val bytes = PathFile.toBytes(mapOf(square to listOf(segment)), "2026-11-02", "OSM")
    val read = { offset: Long, size: Int ->
      bytes.copyOfRange(offset.toInt(), offset.toInt() + size)
    }

    val info = PathFile.directoryInfo(read)

    assertEquals(PathFile.FORMAT_VERSION, info.formatVersion)
    assertEquals("2026-11-02", info.dataDate)
    assertEquals(sha256Hex(read(info.offset, info.size)), info.sha256)
  }

  @Test
  fun `unknown fields of newer indexes are ignored`() {
    val json =
      """{"formatVersion": 1, "generatedAt": "2026-10-26T03:00:00Z", "files": [], "mirrors": []}"""

    assertEquals(NetworkIndex(1, "2026-10-26T03:00:00Z", listOf()), NetworkIndex.parse(json))
  }

  @Test
  fun `downloaded bytes match their entry`() {
    assertTrue(entry.matches(fileBytes))
  }

  @Test
  fun `corrupted or incomplete downloads do not match their entry`() {
    assertFalse(entry.matches("grapf".encodeToByteArray()))
    assertFalse(entry.matches("grap".encodeToByteArray()))
  }

  @Test
  fun `sha256 of known inputs`() {
    assertEquals(
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
      sha256Hex("abc".encodeToByteArray()),
    )
    assertEquals(
      "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
      sha256Hex(ByteArray(0)),
    )
  }

  @Test
  fun `index lists geometry`() {
    val geometryEntry =
      entry.copy(file = "netherlands-cycling.geometry", kind = IndexedFileKind.GEOMETRY)
    val index = NetworkIndex(1, "2026-10-26T03:00:00Z", listOf(entry, geometryEntry))

    val json = index.toJson()

    assertTrue("\"kind\": \"geometry\"" in json, json)
    assertTrue("\"kind\": \"graph\"" in json, json)
    assertTrue("\"region\": \"netherlands\"" in json, json)
    assertEquals(index, NetworkIndex.parse(json))
  }

  @Test
  fun `an index without kinds lists graph files`() {
    val json =
      """{"formatVersion": 1, "generatedAt": "2026-10-26T03:00:00Z", "files": [
        {"region": "netherlands", "network": "cycling", "file": "netherlands-cycling.graph",
         "sizeBytes": 5, "sha256": "${entry.sha256}", "dataDate": "2026-10-25"}]}"""

    assertEquals(IndexedFileKind.GRAPH, NetworkIndex.parse(json).files.single().kind)
  }
}
