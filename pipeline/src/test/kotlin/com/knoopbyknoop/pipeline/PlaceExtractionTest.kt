package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.PathSquare
import com.knoopbyknoop.core.PlaceDetails
import com.knoopbyknoop.core.PlaceFile
import com.knoopbyknoop.core.PlaceKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaceExtractionTest {

  private fun extraction(ways: List<OsmWay> = listOf(), nodes: List<OsmNode>) =
    PlaceExtraction().apply {
      ways.forEach(::acceptWay)
      nodes.forEach(::acceptNode)
    }

  private fun all(extraction: PlaceExtraction) =
    extraction.squares().values.flatMap { it.entries }.associate { it.key to it.value }

  @Test
  fun `Campsite with contacts is in the file under its node`() {
    val campsite =
      OsmNode(
        42,
        51.73,
        3.77,
        mapOf(
          "tourism" to "camp_site",
          "website" to "https://duinhoeve.nl",
          "phone" to "+31 111 1",
        ),
      )
    val squares = extraction(nodes = listOf(campsite)).squares()
    assertEquals(
      mapOf(
        PathSquare.of(51.73, 3.77) to
          mapOf(
            PlaceFile.nodeFeatureId(42) to PlaceDetails("https://duinhoeve.nl", "+31 111 1", null)
          )
      ),
      squares,
    )
  }

  // Scenario: Contact keys
  @Test
  fun `Contact keys count as website and phone`() {
    val hotel =
      OsmNode(
        7,
        51.73,
        3.77,
        mapOf(
          "tourism" to "hotel",
          "contact:website" to "https://hotel.nl",
          "contact:phone" to "+31 2",
        ),
      )
    assertEquals(
      PlaceDetails("https://hotel.nl", "+31 2", null),
      all(extraction(nodes = listOf(hotel)))[PlaceFile.nodeFeatureId(7)],
    )
  }

  @Test
  fun `Place drawn from a building is in every square its outline touches`() {
    // A restaurant on the border of two squares, mapped as a building with its tags on the way.
    val border = (PathSquare.of(51.73, 3.77).longitudeIndex + 1) / 16.0
    val corners =
      listOf(
        OsmNode(1, 51.73, border - 0.0001),
        OsmNode(2, 51.73, border + 0.0001),
        OsmNode(3, 51.7301, border + 0.0001),
        OsmNode(4, 51.7301, border - 0.0001),
      )
    val building =
      OsmWay(
        1333572075,
        listOf(1, 2, 3, 4, 1),
        mapOf("building" to "yes", "amenity" to "restaurant", "website" to "https://r.nl"),
      )
    val squares = extraction(listOf(building), corners).squares()
    val id = PlaceFile.wayFeatureId(1333572075)
    assertEquals(2, squares.size)
    assertTrue(squares.values.all { it[id] == PlaceDetails("https://r.nl", null, null) })
  }

  @Test
  fun `Café with opening hours only is kept`() {
    val cafe =
      OsmNode(9, 51.73, 3.77, mapOf("amenity" to "cafe", "opening_hours" to "Mo-Su 08:00-18:00"))
    val extraction = extraction(nodes = listOf(cafe))
    assertEquals(
      PlaceDetails(null, null, "Mo-Su 08:00-18:00"),
      all(extraction)[PlaceFile.nodeFeatureId(9)],
    )
    assertEquals(1, extraction.openingHoursCount)
    assertEquals(1, extraction.readableOpeningHoursCount)
  }

  @Test
  fun `Place without contacts is not in the file`() {
    val water = OsmNode(5, 51.73, 3.77, mapOf("amenity" to "drinking_water"))
    assertEquals(mapOf(), extraction(nodes = listOf(water)).squares())
  }

  @Test
  fun `Places that are not highlighted are not in the file`() {
    val pharmacy = OsmNode(5, 51.73, 3.77, mapOf("amenity" to "pharmacy", "website" to "a.nl"))
    val chalet =
      OsmWay(6, listOf(5), mapOf("tourism" to "chalet", "website" to "https://chalet.nl"))
    assertEquals(mapOf(), extraction(listOf(chalet), listOf(pharmacy)).squares())
  }

  @Test
  fun `the places are counted per kind`() {
    val nodes =
      listOf(
        OsmNode(1, 51.73, 3.77, mapOf("tourism" to "camp_site", "phone" to "+31 1")),
        OsmNode(2, 51.73, 3.77, mapOf("tourism" to "guest_house", "phone" to "+31 2")),
        OsmNode(3, 51.73, 3.77, mapOf("amenity" to "cafe", "opening_hours" to "ask inside")),
      )
    val extraction = extraction(nodes = nodes)
    assertEquals(
      mapOf(PlaceKind.FOOD to 1, PlaceKind.CAMPSITE to 1, PlaceKind.LODGING to 1),
      extraction.counts,
    )
    assertEquals(0, extraction.readableOpeningHoursCount)
  }
}
