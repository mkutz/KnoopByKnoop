package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class KnotTest {

  @Test
  fun `knots with the same ref in different places are different knots`() {
    val utrecht = Knot(id = 1, ref = "12", network = NetworkType.CYCLING, 52.09, 5.12)
    val groningen = Knot(id = 2, ref = "12", network = NetworkType.CYCLING, 53.22, 6.57)

    assertNotEquals(utrecht, groningen)
  }

  @Test
  fun `network types map to their OpenStreetMap ref tags`() {
    assertEquals("rcn_ref", NetworkType.CYCLING.osmRefTag)
    assertEquals("rwn_ref", NetworkType.HIKING.osmRefTag)
  }
}
