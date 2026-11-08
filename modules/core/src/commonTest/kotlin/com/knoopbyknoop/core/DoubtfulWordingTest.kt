package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals

/** Covers `knot-navigation` "Wording for doubtful junctions". */
class DoubtfulWordingTest {

  private val origin = GeoPoint(50.80, 7.20)

  private fun at(east: Double) = GeoPoint(origin.latitude, origin.longitude + east / 70_300.0)

  private fun fix(point: GeoPoint) = Fix(point.latitude, point.longitude, 5.0, 0)

  /** 2 km to the east, with [turn] at the doubtful junction at 1 km. */
  private fun guided(direction: TurnDirection, junction: DoubtfulJunction): GuidedWay {
    val points = listOf(at(0.0), at(1000.0), at(2000.0))
    val meters = points.map {
      distanceMeters(origin.latitude, origin.longitude, it.latitude, it.longitude)
    }
    val turn = PathTurn(1000.0, direction, null, doubtful = junction)
    return GuidedWay.of(PathWay(points, meters, listOf(turn), doubtful = listOf(junction)))
  }

  private fun junction(
    vararg doubts: JunctionDoubt,
    keep: TurnDirection? = null,
    offRoad: PathSwitch? = null,
  ) = DoubtfulJunction(1000.0, 1000.0, doubts.toSet(), keep, offRoad)

  private val mainRoad = junction(JunctionDoubt.MAIN_ROAD, JunctionDoubt.SIMILAR_EXIT)
  private val cyclePath = junction(JunctionDoubt.OFF_ROAD, offRoad = PathSwitch.CYCLE_PATH)
  private val fork = junction(JunctionDoubt.SIMILAR_EXIT, keep = TurnDirection.LEFT)

  private fun told(way: GuidedWay, east: Double, language: Language = Language.EN) =
    way.update(fix(at(east)), language, language).notice

  @Test
  fun `crossing a main road and turning`() {
    val way = guided(TurnDirection.LEFT, mainRoad)

    assertEquals("In 100 m, cross the main road, then turn left", told(way, 905.0)?.text)
    assertEquals(
      "In 100 meters, cross the main road, then turn left",
      told(way, 905.0)?.speech,
    )
    assertEquals("Now cross the main road, then turn left", told(way, 975.0)?.text)
  }

  @Test
  fun `crossing a main road straight on`() {
    assertEquals(
      "In 100 m, cross the main road",
      told(guided(TurnDirection.STRAIGHT, mainRoad), 905.0)?.text,
    )
  }

  @Test
  fun `cycle path after a road`() {
    assertEquals(
      "In 100 m, take the cycle path on the left",
      told(guided(TurnDirection.LEFT, cyclePath), 905.0)?.text,
    )
  }

  @Test
  fun `a similar exit`() {
    assertEquals(
      "In 100 m, keep left at the fork",
      told(guided(TurnDirection.SLIGHT_LEFT, fork), 905.0)?.text,
    )
  }

  @Test
  fun `all app languages`() {
    val sentences =
      listOf(Language.DE, Language.NL, Language.FR).map { language ->
        listOf(
            guided(TurnDirection.LEFT, mainRoad),
            guided(TurnDirection.LEFT, cyclePath),
            guided(TurnDirection.SLIGHT_LEFT, fork),
          )
          .map { told(it, 905.0, language)?.text }
      }

    assertEquals(
      listOf(
        listOf(
          "In 100 m die Hauptstraße überqueren, dann links abbiegen",
          "In 100 m den Radweg links nehmen",
          "In 100 m an der Gabelung links halten",
        ),
        listOf(
          "Over 100 m de hoofdweg oversteken, dan linksaf",
          "Over 100 m het fietspad links nemen",
          "Over 100 m bij de splitsing links aanhouden",
        ),
        listOf(
          "Dans 100 m, traversez la route principale, puis tournez à gauche",
          "Dans 100 m, prenez la piste cyclable à gauche",
          "Dans 100 m, restez à gauche à la bifurcation",
        ),
      ),
      sentences,
    )
  }
}
