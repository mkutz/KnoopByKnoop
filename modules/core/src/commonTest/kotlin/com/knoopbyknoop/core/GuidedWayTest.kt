package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GuidedWayTest {

  private val origin = GeoPoint(50.80, 7.20)

  private fun at(east: Double, north: Double = 0.0) =
    GeoPoint(origin.latitude + north / 111_195.0, origin.longitude + east / 70_300.0)

  private fun fix(point: GeoPoint, accuracy: Double = 5.0) =
    Fix(point.latitude, point.longitude, accuracy, 0)

  /** 2 km to the east, with a left turn into Weierstraße at 1 km. */
  private fun guided(): GuidedWay {
    val points = listOf(at(0.0), at(1000.0), at(2000.0))
    val meters =
      points
        .runningFold(0.0 to points.first()) { (total, previous), point ->
          total +
            distanceMeters(
              previous.latitude,
              previous.longitude,
              point.latitude,
              point.longitude,
            ) to point
        }
        .drop(1)
        .map { it.first }
    return GuidedWay.of(
      PathWay(points, meters, listOf(PathTurn(1000.0, TurnDirection.LEFT, "Weierstraße")))
    )
  }

  private fun GuidedWay.at(point: GeoPoint) = update(fix(point), Language.EN, Language.EN)

  /** 2 km to the east, with [turn] at 1 km. */
  private fun guided(turn: PathTurn): GuidedWay {
    val points = listOf(at(0.0), at(1000.0), at(2000.0))
    val meters = points.map {
      distanceMeters(origin.latitude, origin.longitude, it.latitude, it.longitude)
    }
    return GuidedWay.of(PathWay(points, meters, listOf(turn)))
  }

  @Test
  fun `A switch to the cycle path without a turn is said as a switch`() {
    val way = guided(PathTurn(1000.0, TurnDirection.STRAIGHT, null, PathSwitch.CYCLE_PATH))
    assertEquals("In 100 m, switch to the cycle path", way.at(at(905.0)).notice?.text)
    assertEquals("Switch to the cycle path now", way.at(at(975.0)).notice?.text)
  }

  @Test
  fun `A switch with a turn is said with the turn`() {
    val way = guided(PathTurn(1000.0, TurnDirection.RIGHT, null, PathSwitch.CYCLE_PATH))
    assertEquals("In 100 m, turn right, onto the cycle path", way.at(at(905.0)).notice?.text)
  }

  @Test
  fun `Switches back to the street and onto a path are named`() {
    assertEquals(
      "In 100 m, switch to the street",
      guided(PathTurn(1000.0, TurnDirection.STRAIGHT, null, PathSwitch.STREET))
        .at(at(905.0))
        .notice
        ?.text,
    )
    assertEquals(
      "In 100 m, switch to the path",
      guided(PathTurn(1000.0, TurnDirection.STRAIGHT, null, PathSwitch.PATH))
        .at(at(905.0))
        .notice
        ?.text,
    )
  }

  @Test
  fun `The header shows a switch without a turn`() {
    val way = guided(PathTurn(1000.0, TurnDirection.STRAIGHT, null, PathSwitch.CYCLE_PATH))
    assertEquals("To the cycle path in 100 m", way.at(at(905.0)).way.view(Language.EN).turn)
  }

  @Test
  fun `A switch is said in every app language`() {
    val turn = PathTurn(1000.0, TurnDirection.STRAIGHT, null, PathSwitch.CYCLE_PATH)
    val spoken =
      listOf(Language.DE, Language.NL, Language.FR).map {
        guided(turn).update(fix(at(905.0)), it, it).notice?.text
      }
    assertEquals(
      listOf(
        "In 100 m auf den Radweg wechseln",
        "Over 100 m naar het fietspad",
        "Dans 100 m, prenez la piste cyclable",
      ),
      spoken,
    )
  }

  @Test
  fun `A turn ahead is said once about 100 m before it`() {
    val far = guided().at(at(500.0))
    assertNull(far.notice)
    val close = far.way.at(at(905.0))
    assertEquals(NoticeKind.TURN, close.notice?.kind)
    assertEquals("In 100 m, turn left", close.notice?.text)
    assertNull(close.way.at(at(920.0)).notice)
  }

  @Test
  fun `A turn close to the start of the way says now`() {
    assertEquals("Turn left now", guided().at(at(975.0)).notice?.text)
  }

  @Test
  fun `The notice is spoken in the speech language`() {
    val update = guided().update(fix(at(905.0)), Language.DE, Language.EN)
    assertEquals("In 100 m links abbiegen", update.notice?.text)
    assertEquals("In 100 meters, turn left", update.notice?.speech)
  }

  @Test
  fun `The voice says the distance in words`() {
    val update = guided().update(fix(at(905.0)), Language.DE, Language.DE)
    assertEquals("In 100 m links abbiegen", update.notice?.text)
    assertEquals("In 100 Metern links abbiegen", update.notice?.speech)
  }

  @Test
  fun `The view shows the next turn and what is left of the way`() {
    val view = guided().at(at(880.0)).way.view(Language.EN)
    assertEquals("Left in 120 m", view.turn)
    assertEquals("Weierstraße", view.street)
    assertEquals("1.1 km", view.remaining)
    assertEquals("Left in 120 m · 1.1 km", view.line)
    assertEquals("Left in 120 m, Weierstraße, 1.1 km", view.description)
  }

  @Test
  fun `After the last turn the view says to follow the way`() {
    val view = guided().at(at(900.0)).way.at(at(1200.0)).way.view(Language.EN)
    assertEquals("Follow the way", view.turn)
    assertEquals("800 m", view.remaining)
  }

  @Test
  fun `Missing a turn is leaving the way after two fixes`() {
    val first = guided().at(at(500.0, 80.0))
    assertFalse(first.left)
    assertTrue(first.way.at(at(510.0, 90.0)).left)
  }

  @Test
  fun `A single inaccurate fix leaves the way as it is`() {
    val first = guided().at(at(500.0, 80.0))
    val back = first.way.at(at(510.0))
    assertFalse(back.left)
    assertFalse(back.way.at(at(520.0, 80.0)).left)
  }

  @Test
  fun `A coarse fix does not count`() {
    val update = guided().update(fix(at(900.0, 80.0), accuracy = 200.0), Language.EN, Language.EN)
    assertFalse(update.left)
    assertNull(update.notice)
  }

  /** 1 km to the east from the rider, reported as starting against their riding direction. */
  private fun wayBehind() =
    PathWay(
      listOf(at(0.0), at(1000.0)),
      listOf(
        0.0,
        distanceMeters(
          at(0.0).latitude,
          at(0.0).longitude,
          at(1000.0).latitude,
          at(1000.0).longitude,
        ),
      ),
      listOf(),
      startsBack = true,
    )

  @Test
  fun `A way behind the rider says to turn back and shows it until they follow it`() {
    val first = GuidedWay.of(wayBehind()).at(at(-5.0))
    assertEquals("Turn back now", first.notice?.text)
    assertEquals("Turn back", first.way.view(Language.EN).turn)
    val riding = first.way.at(at(-10.0))
    assertNull(riding.notice)
    assertEquals("Turn back", riding.way.view(Language.EN).turn)
    assertEquals("Follow the way", riding.way.at(at(20.0)).way.view(Language.EN).turn)
  }

  @Test
  fun `Turning back is not said again while the rider keeps riding away`() {
    val told = GuidedWay.of(wayBehind()).at(at(-5.0)).way
    val next = GuidedWay.of(wayBehind(), told).at(at(-40.0))
    assertNull(next.notice)
    assertEquals("Turn back", next.way.view(Language.EN).turn)
  }

  @Test
  fun `Turning back is said again for a new way behind after one ahead`() {
    val ahead = guided().at(at(10.0)).way
    assertEquals("Turn back now", GuidedWay.of(wayBehind(), ahead).at(at(-5.0)).notice?.text)
  }

  @Test
  fun `Riding away from the way counts in the distances and the line`() {
    val update = guided().at(at(0.0, 25.0))
    val away = update.way
    assertFalse(update.left)
    assertEquals("2.0 km", away.view(Language.EN).remaining)
    assertEquals(at(0.0, 25.0).latitude, away.pointsAhead().first().latitude, 0.00001)
    val riding = guided().at(at(0.0, 20.0)).way.at(at(500.0)).way
    assertEquals(at(500.0).longitude, riding.pointsAhead().first().longitude, 0.0001)
  }

  @Test
  fun `Close to the way the distances and the line stay on it`() {
    val onWay = guided().at(at(500.0, 10.0)).way
    assertEquals(1500.0, onWay.remainingMeters, 20.0)
    assertEquals(at(500.0).latitude, onWay.pointsAhead().first().latitude, 0.00001)
  }

  @Test
  fun `The distance left includes how far the rider is from the way`() {
    assertEquals(1525.0, guided().at(at(500.0)).way.at(at(500.0, 25.0)).way.remainingMeters, 20.0)
  }

  @Test
  fun `Points ahead start at the rider`() {
    val points = guided().at(at(500.0)).way.pointsAhead()
    assertEquals(3, points.size)
    assertEquals(at(500.0).longitude, points.first().longitude, 0.0001)
  }
}
