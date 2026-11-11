package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.Language
import com.knoopbyknoop.core.Place
import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Covers `network-data` "Place of each knot" in the pipeline. */
class PlaceNamingTest {

  private val metersPerDegree = 6_371_008.8 * PI / 180

  private fun latitude(north: Double) = 50.8 + north / metersPerDegree

  private fun longitude(east: Double) = 7.2 + east / (metersPerDegree * cos(50.8 * PI / 180))

  private fun point(name: String, size: PlaceSize, east: Double, north: Double = 0.0) =
    LocatedPlace(Place(name), size, latitude(north), longitude(east))

  @Test
  fun `Knot in a town`() {
    val finder = PlaceFinder(listOf(point("Lohmar", PlaceSize.TOWN, 0.0)))

    assertEquals("Lohmar", finder.placeAt(latitude(0.0), longitude(1_500.0))?.name)
  }

  @Test
  fun `Knot in the open`() {
    val finder = PlaceFinder(listOf(point("Inger", PlaceSize.VILLAGE, 0.0)))

    assertNull(finder.placeAt(latitude(0.0), longitude(3_000.0)))
  }

  @Test
  fun `Only a hamlet nearby`() {
    // A hamlet is no place that counts, so its tags give none.
    assertNull(placeOfTags(mapOf("place" to "hamlet", "name" to "Höffen")))
    assertNull(placeOfTags(mapOf("place" to "suburb", "name" to "Kaldauen")))
    val finder = PlaceFinder(listOf(point("Wahlscheid", PlaceSize.VILLAGE, 5_000.0)))

    assertNull(finder.placeAt(latitude(0.0), longitude(0.0)))
  }

  @Test
  fun `Names in other languages`() {
    val (place, size) =
      placeOfTags(
        mapOf(
          "place" to "city",
          "name" to "Liège",
          "name:de" to "Lüttich",
          "name:nl" to "Luik",
          "name:fr" to "Liège",
        )
      )!!

    assertEquals(PlaceSize.CITY, size)
    assertEquals("Liège", place.name)
    assertEquals(mapOf(Language.DE to "Lüttich", Language.NL to "Luik"), place.names)
  }

  @Test
  fun `the village wins at the edge of a city`() {
    val finder =
      PlaceFinder(
        listOf(
          point("Siegburg", PlaceSize.CITY, 0.0),
          point("Kaldauen", PlaceSize.VILLAGE, 2_500.0),
        )
      )

    // 2.3 km from the city's point (0.58 of its reach), 200 m from the village's (0.2).
    assertEquals("Kaldauen", finder.placeAt(latitude(0.0), longitude(2_300.0))?.name)
  }

  @Test
  fun `inside a mapped area`() {
    val ring =
      listOf(-500.0 to -500.0, -500.0 to 500.0, 500.0 to 500.0, 500.0 to -500.0, -500.0 to -500.0)
        .map { (north, east) -> latitude(north) to longitude(east) }
    val finder =
      PlaceFinder(
        listOf(
          LocatedPlace(Place("Birk"), PlaceSize.VILLAGE, latitude(0.0), longitude(0.0), ring),
          point("Lohmar", PlaceSize.TOWN, 900.0),
        )
      )

    assertEquals("Birk", finder.placeAt(latitude(0.0), longitude(400.0))?.name)
  }
}
