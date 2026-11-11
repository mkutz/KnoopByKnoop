package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlaceFileTest {

  private val renesse = PathSquare.of(51.73, 3.77)

  private val grol = PlaceFile.wayFeatureId(1333572075)
  private val fletcher = PlaceFile.nodeFeatureId(2807330910)
  private val wapen = PlaceFile.nodeFeatureId(2807324298)

  private val squares =
    mapOf(
      renesse to
        mapOf(
          grol to PlaceDetails("https://manegegrol.nl/voorbeeld-pagina/", "+31 111 461 623", null),
          fletcher to
            PlaceDetails("https://www.fletcherstrandhotelrenesse.nl/", "+31 111 462 510", null),
          wapen to PlaceDetails(null, null, "Mo-Su 11:30-14:00,17:30-22:00"),
        ),
      PathSquare(renesse.latitudeIndex + 1, renesse.longitudeIndex) to
        mapOf(PlaceFile.nodeFeatureId(1) to PlaceDetails(null, "+31 1", null)),
    )

  private fun open(bytes: ByteArray) = PlaceFile.open { offset, size ->
    bytes.copyOfRange(offset.toInt(), offset.toInt() + size)
  }

  @Test
  fun `the feature ids of nodes and ways are those of the base map's tiles`() {
    // As checked on the tile 14/8363/5431 near Renesse, see BaseMapPlacesTest.
    assertEquals(28073309101, fletcher)
    assertEquals(13335720752, grol)
  }

  @Test
  fun `the details of each square come back as written`() {
    val file = open(PlaceFile.toBytes(squares, "2026-11-10", "OSM"))
    assertEquals("2026-11-10", file.dataDate)
    for ((square, details) in squares) assertEquals(details, file.details(square))
    assertEquals(mapOf(), file.details(PathSquare(0, 0)))
  }

  // Scenario: Unsupported place details format
  @Test
  fun `a place details file of an unknown format is not read`() {
    val bytes = PlaceFile.toBytes(squares, "2026-11-10", "OSM")
    bytes[4] = (PlaceFile.FORMAT_VERSION + 1).toByte()
    assertFailsWith<UnsupportedFormatVersionException> { open(bytes) }
  }

  @Test
  fun `a path file is no place details file`() {
    val paths = PathFile.toBytes(mapOf(), "2026-11-10", "OSM")
    assertFailsWith<GraphFormatException> { open(paths) }
    assertTrue(PlaceFile.SQUARES.formatVersion == PlaceFile.FORMAT_VERSION)
  }
}
