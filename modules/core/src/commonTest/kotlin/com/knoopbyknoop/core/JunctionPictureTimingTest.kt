package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Covers `knot-navigation` "Picture in the app" and "Screen comes on at a junction" in the core.
 */
class JunctionPictureTimingTest {

  private val origin = GeoPoint(50.80, 7.20)

  private fun at(east: Double) = GeoPoint(origin.latitude, origin.longitude + east / 70_300.0)

  private fun fix(east: Double) = Fix(origin.latitude, origin.longitude + east / 70_300.0, 5.0, 0)

  private val picture =
    JunctionPicture(
      listOf(
        PictureLine(PictureLineKind.WAY_BEFORE, listOf(PicturePoint(0, -34), PicturePoint(0, 0)))
      )
    )

  private val crossing =
    DoubtfulJunction(1000.0, 1000.0, setOf(JunctionDoubt.MAIN_ROAD), picture = picture)

  /** 2 km to the east with a left turn at 1 km, doubtful or not. */
  private fun way(doubtful: DoubtfulJunction?): GuidedWay {
    val points = listOf(at(0.0), at(1000.0), at(2000.0))
    val meters = points.map {
      distanceMeters(origin.latitude, origin.longitude, it.latitude, it.longitude)
    }
    val turn = PathTurn(1000.0, TurnDirection.LEFT, "Weierstraße", doubtful = doubtful)
    return GuidedWay.of(PathWay(points, meters, listOf(turn), doubtful = listOfNotNull(doubtful)))
  }

  private fun GuidedWay.ride(vararg easts: Double) =
    easts.fold(this) { way, east -> way.update(fix(east), Language.EN, Language.EN).way }

  @Test
  fun `approaching a doubtful junction in the app`() {
    val far = way(crossing).ride(500.0, 800.0)
    val close = far.ride(860.0)

    assertNull(far.headerTurn(Language.EN).picture)
    val header = close.headerTurn(Language.EN)
    assertEquals(picture, header.picture)
    assertEquals("In 140 m, cross the main road, then turn left", header.pictureSentence)
  }

  @Test
  fun `after the junction`() {
    assertNull(way(crossing).ride(500.0, 860.0, 980.0, 1020.0).headerTurn(Language.EN).picture)
  }

  @Test
  fun `leaving the way before the junction`() {
    val left = way(crossing).ride(500.0, 860.0)
    val easy = GuidedWay.of(way(null).way, left).ride(870.0)
    val doubtfulAgain = GuidedWay.of(way(crossing).way, left).ride(870.0)

    assertNull(easy.headerTurn(Language.EN).picture)
    assertNotNull(doubtfulAgain.headerTurn(Language.EN).picture)
  }

  @Test
  fun `no picture on an easy turn`() {
    assertNull(way(null).ride(500.0, 900.0).headerTurn(Language.EN).picture)
  }

  @Test
  fun `screen comes on at a junction`() {
    // The first fix after the screen comes on is 80 m before the junction.
    assertNotNull(way(crossing).ride(920.0).headerTurn(Language.EN).picture)
  }
}
