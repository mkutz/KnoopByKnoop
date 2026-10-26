package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkIndexTest {

  private val fileBytes = "graph".encodeToByteArray()

  private val entry =
    NetworkIndexEntry(
      region = "netherlands",
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
