package com.knoopbyknoop.android

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the risk "Liberty renames its bridge layers" of `openspec/changes/show-route-on-bridges`:
 * the bundled dark style has the same bridge layers the app copies.
 */
class BridgeLayersTest {

  @Test
  fun `the bundled dark style's bridges are found`() {
    val style = File("../../map-styles/liberty-dark.json").readText()
    val ids = Regex(""""id"\s*:\s*"([^"]+)"""").findAll(style).map { it.groupValues[1] }.toList()

    val bridges = ids.filter(::isBaseMapBridgeId)

    assertEquals(20, bridges.size)
    assertEquals(
      setOf(
        "bridge_street",
        "bridge_path_pedestrian",
        "bridge_major_rail",
        "bridge_motorway_casing",
      ),
      bridges
        .toSet()
        .intersect(
          setOf(
            "bridge_street",
            "bridge_path_pedestrian",
            "bridge_major_rail",
            "bridge_motorway_casing",
          )
        ),
    )
  }
}
