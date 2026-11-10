package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals

class KnotPlateTest {

  // Scenario: Route in North Rhine-Westphalia
  @Test
  fun `knots in North Rhine-Westphalia are red with a ring inside`() {
    assertEquals(
      KnotPlate(0xFFC8102E, innerRing = true),
      KnotPlate.of(SignRegion.NORTH_RHINE_WESTPHALIA),
    )
  }

  // Scenario: Route in the Netherlands
  @Test
  fun `Dutch knots are dark green without a ring inside`() {
    assertEquals(KnotPlate(0xFF12663A, innerRing = false), KnotPlate.of(SignRegion.NETHERLANDS))
  }

  // Scenario: Route in Belgium
  @Test
  fun `Belgian knots are blue with a ring inside`() {
    assertEquals(KnotPlate(0xFF1A5FB4, innerRing = true), KnotPlate.of(SignRegion.BELGIUM))
  }

  // Scenario: Knot without a region
  @Test
  fun `knots without a region take the app's green`() {
    assertEquals(KnotPlate(0xFF2E9E5B, innerRing = true), KnotPlate.of(null))
  }

  @Test
  fun `plates are named by their region's key`() {
    assertEquals("nrw", KnotPlate.of(SignRegion.NORTH_RHINE_WESTPHALIA).key)
    assertEquals("none", KnotPlate.of(null).key)
    assertEquals(KnotPlate.of(SignRegion.BELGIUM), KnotPlate.ofKey("be"))
    assertEquals(KnotPlate.NONE, KnotPlate.ofKey("unknown"))
  }
}
