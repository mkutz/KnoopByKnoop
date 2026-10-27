package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Covers the scenarios of `place-search`, with responses recorded from Photon. */
class PlaceSearchTest {

  /** A network with knots at the given places, standing in for the covered area. */
  private fun networkAt(vararg places: Pair<Double, Double>) =
    KnotLocator(
      places.mapIndexed { index, (latitude, longitude) ->
        Knot(index.toLong(), "$index", NetworkType.CYCLING, latitude, longitude)
      }
    )

  private val utrecht = 52.0907 to 5.1214
  private val ghent = 51.0543 to 3.7174
  private val venlo = 51.3704 to 6.1724
  private val assenede = 51.2300 to 3.7100
  private val eeklo = 51.1850 to 3.5650
  private val sluis = 51.3084 to 3.3874
  private val helmond = 51.4660 to 5.6625
  private val borgloon = 50.8103 to 5.3418

  private val area = GeoBounds(49.5, 2.5, 53.6, 9.5)

  // Requirement: Search while typing

  @Test
  fun `station`() {
    val hits = PlaceSearch.parse(PhotonResponses.STATION, networkAt(utrecht))

    assertEquals(
      PlaceHit("Utrecht Centraal", "Train station, Utrecht", 52.0893825, 5.1096021),
      hits.first(),
    )
    assertEquals(1, hits.count { it.name == "Utrecht Centraal" }, "duplicates are merged")
  }

  @Test
  fun `address`() {
    val hits = PlaceSearch.parse(PhotonResponses.ADDRESS, networkAt(utrecht))

    assertEquals("Utrecht", hits.first { it.name == "Oudegracht 158B" }.description)
    assertEquals(
      "Oudegracht 158, Utrecht",
      hits.first { it.name == "Winkel van Sinkel" }.description,
    )
  }

  @Test
  fun `business`() {
    val hits = PlaceSearch.parse(PhotonResponses.BUSINESS, networkAt(utrecht))

    val olivier = hits.first()
    assertEquals("Olivier", olivier.name)
    assertTrue(olivier.description.endsWith(", Utrecht"), olivier.description)
  }

  @Test
  fun `streets are described as streets`() {
    val hits = PlaceSearch.parse(PhotonResponses.BUSINESS, networkAt(utrecht, 52.1260 to 5.0400))

    assertEquals("Street, Maarssen", hits.first { it.name == "Olivier van Noortlaan" }.description)
  }

  @Test
  fun `no hits`() {
    assertEquals(listOf(), PlaceSearch.parse(PhotonResponses.NO_HITS, networkAt(utrecht)))
  }

  @Test
  fun `towns are described by their region`() {
    val hits =
      PlaceSearch.parse(
        """{"type": "FeatureCollection", "features": [{"type": "Feature",
          "geometry": {"type": "Point", "coordinates": [5.1214, 52.0907]},
          "properties": {"osm_key": "place", "osm_value": "city", "name": "Utrecht",
            "state": "Utrecht", "country": "Netherlands"}}]}""",
        networkAt(utrecht),
      )

    assertEquals("Utrecht, Netherlands", hits.single().description)
  }

  @Test
  fun `a response that is not from Photon`() {
    assertFailsWith<IllegalArgumentException> { PlaceSearch.parse("<html>", networkAt(utrecht)) }
  }

  // Requirement: Hits in the covered area

  @Test
  fun `same name in several towns`() {
    val hits = PlaceSearch.parse(PhotonResponses.STATIONSSTRAAT, networkAt(assenede, eeklo))

    assertEquals(
      listOf("Assenede", "Eeklo"),
      hits.map { it.description.substringAfterLast(", ") }.distinct(),
    )
  }

  @Test
  fun `towns of that name come before nearby hits`() {
    val hits =
      PlaceSearch.merge(
        PhotonResponses.SLUIS_PROMINENT,
        PhotonResponses.SLUIS_NEARBY,
        networkAt(sluis, helmond),
      )

    assertEquals("Sluis" to "Zeeland, Netherlands", hits.first().let { it.name to it.description })
    assertEquals(3.3874159, hits.first().longitude, 0.0001, "the village, not the municipality")
    assertEquals(1, hits.count { it.description == "Zeeland, Netherlands" })
    assertEquals(
      "Bridge, Helmond",
      hits.first { it.name == "Sluis 8" }.description,
      "nearby hits follow",
    )
  }

  @Test
  fun `nearby streets come before streets of that name far away`() {
    val hits =
      PlaceSearch.merge(
        PhotonResponses.STATIONSSTRAAT_PROMINENT,
        PhotonResponses.STATIONSSTRAAT_NEARBY,
        networkAt(assenede, eeklo, borgloon),
      )

    assertTrue(hits.first().description.endsWith("Assenede"), "$hits")
  }

  @Test
  fun `one failed request still gives hits`() {
    val hits = PlaceSearch.merge(null, PhotonResponses.STATIONSSTRAAT_NEARBY, networkAt(assenede))

    assertTrue(hits.isNotEmpty())
    assertFailsWith<IllegalArgumentException> {
      PlaceSearch.merge(null, "<html>", networkAt(assenede))
    }
  }

  @Test
  fun `the prominent request has a wide location bias`() {
    val requests = PlaceSearch.requests("Sluis", 51.48, 5.66, area, "en")

    assertTrue(requests.prominent.endsWith("&zoom=10"), requests.prominent)
    assertTrue("zoom" !in requests.nearby, requests.nearby)
  }

  @Test
  fun `place outside the covered area`() {
    assertEquals(
      listOf(),
      PlaceSearch.parse(PhotonResponses.PARIS, networkAt(utrecht, ghent, venlo)),
    )
  }

  @Test
  fun `the covered area is the extent of the network`() {
    assertEquals(GeoBounds(52.080, 5.120, 52.310, 5.510), GeoBounds.of(TestNetwork.graph))
  }

  @Test
  fun `request`() {
    val url = PlaceSearch.requestUrl("Utrecht Centraal", 52.1, 5.1, area, "en")

    assertEquals(
      "https://photon.komoot.io/api/?q=Utrecht%20Centraal&limit=10&lang=en&lat=52.1&lon=5.1" +
        "&bbox=2.5%2C49.5%2C9.5%2C53.6",
      url,
    )
  }

  @Test
  fun `request in a language Photon does not know`() {
    val url = PlaceSearch.requestUrl("Café", 52.1, 5.1, area, "nl")

    assertTrue("lang=default" in url, url)
    assertTrue("q=Caf%C3%A9" in url, url)
  }
}
