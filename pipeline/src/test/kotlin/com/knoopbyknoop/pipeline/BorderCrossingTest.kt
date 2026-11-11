package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.Country
import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.Language
import com.knoopbyknoop.core.Region
import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers `network-data` "Borders between knots" in the pipeline. Regions are squares side by side,
 * 2 km wide, along a course running east.
 */
class BorderCrossingTest {

  private val metersPerDegree = 6_371_008.8 * PI / 180

  private fun latitude(north: Double) = 51.0 + north / metersPerDegree

  private fun longitude(east: Double) = 6.0 + east / (metersPerDegree * cos(51.0 * PI / 180))

  private val dutchLimburg = Region("NL-LI", Country.NETHERLANDS, "Limburg")
  private val belgianLimburg =
    Region("BE-VLI", Country.BELGIUM, "Limburg", mapOf(Language.FR to "Limbourg"))
  private val gelderland = Region("NL-GE", Country.NETHERLANDS, "Gelderland")
  private val overijssel = Region("NL-OV", Country.NETHERLANDS, "Overijssel")

  /** A square region from [west] to [west] + 2 km, 2 km north and south of the course. */
  private fun square(region: Region, west: Double) =
    Border(
      region,
      listOf(
        listOf(
            -2_000.0 to west,
            -2_000.0 to west + 2_000.0,
            2_000.0 to west + 2_000.0,
            2_000.0 to west,
            -2_000.0 to west,
          )
          .map { (north, east) -> latitude(north) to longitude(east) }
      ),
    )

  private fun course(from: Double, to: Double) =
    listOf(GeoPoint(latitude(0.0), longitude(from)), GeoPoint(latitude(0.0), longitude(to)))

  @Test
  fun `Crossing into Belgium`() {
    val borders = listOf(square(dutchLimburg, 0.0), square(belgianLimburg, 2_000.0))

    val crossings = crossingsAlong(course(800.0, 3_000.0), borders)

    assertEquals(1, crossings.size)
    val crossing = crossings.single()
    assertTrue(crossing.atMeters in 1_195..1_205, "${crossing.atMeters} m")
    assertEquals(dutchLimburg, crossing.from)
    assertEquals(belgianLimburg, crossing.to)
    assertEquals(Country.BELGIUM, crossing.to.country)
  }

  @Test
  fun `Crossing a province border`() {
    val borders = listOf(square(gelderland, 0.0), square(overijssel, 2_000.0))

    val crossing = crossingsAlong(course(1_000.0, 3_000.0), borders).single()

    assertEquals(overijssel, crossing.to)
    assertEquals(crossing.from.country, crossing.to.country)
  }

  @Test
  fun `Within one province`() {
    assertEquals(listOf(), crossingsAlong(course(200.0, 1_800.0), listOf(square(gelderland, 0.0))))
  }

  @Test
  fun `Beyond the covered area`() {
    // Leaving the regions, and coming back into the same one, crosses no border between two.
    assertEquals(
      listOf(),
      crossingsAlong(course(1_000.0, 3_000.0), listOf(square(gelderland, 0.0))),
    )
    // A gap outside all regions between two of them still gives the crossing.
    val gap = listOf(square(gelderland, 0.0), square(overijssel, 2_300.0))
    assertEquals(overijssel, crossingsAlong(course(1_000.0, 3_500.0), gap).single().to)
  }

  @Test
  fun `Incomplete border`() {
    val failure =
      assertFailsWith<IncompleteBorderException> {
        Border.of(gelderland, 47_554, "Gelderland", listOf(1L, 2L), { null }) { null }
      }

    assertTrue("Gelderland" in failure.message.orEmpty(), failure.message)
  }

  @Test
  fun `regions from their boundary tags`() {
    fun tags(level: String, key: String) =
      mapOf(
        "boundary" to "administrative",
        "type" to "boundary",
        "admin_level" to level,
        "ISO3166-2" to key,
        "name" to key,
        "name:de" to "Name $key",
      )

    assertEquals(Country.NETHERLANDS, regionOfBorder(tags("4", "NL-LI"))?.country)
    assertEquals(Country.BELGIUM, regionOfBorder(tags("6", "BE-VLI"))?.country)
    assertEquals(Country.BELGIUM, regionOfBorder(tags("4", "BE-BRU"))?.country)
    assertEquals(Country.GERMANY, regionOfBorder(tags("4", "DE-NW"))?.country)
    assertEquals("Name DE-NW", regionOfBorder(tags("4", "DE-NW"))?.names?.get(Language.DE))
    // Flanders is a level 4 region above the provinces; Lower Saxony is not whole in the extracts.
    assertNull(regionOfBorder(tags("4", "BE-VLG")))
    assertNull(regionOfBorder(tags("4", "DE-NI")))
  }
}
