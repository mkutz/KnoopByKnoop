package com.knoopbyknoop.web

import com.knoopbyknoop.core.CalendarDay
import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.Language
import com.knoopbyknoop.core.LocalMoment
import com.knoopbyknoop.core.MapPlace
import com.knoopbyknoop.core.NetworkGeometry
import com.knoopbyknoop.core.NetworkGraph
import com.knoopbyknoop.core.NetworkType
import com.knoopbyknoop.core.PlaceDetails
import com.knoopbyknoop.core.PlaceKind
import com.knoopbyknoop.core.RouteDescription
import com.knoopbyknoop.web.TestNetwork.K02
import com.knoopbyknoop.web.TestNetwork.K27
import com.knoopbyknoop.web.TestNetwork.K28
import com.knoopbyknoop.web.TestNetwork.K73
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class PlacesAndFerriesTest {

  /** A network with one knot at Utrecht Centraal, for searches there. */
  private val utrecht =
    NetworkGraph(listOf(Knot(1_000_001, "10", NetworkType.CYCLING, 52.0894, 5.1100)), listOf())
      .let {
        LoadedNetwork(IndexedNetworkType.CYCLING, it, NetworkGeometry.straightLines(it), true)
      }

  @Test
  fun `Station found in the planner - Utrecht Centraal as one hit`() = runTest {
    val searcher = PlaceSearcher { STATION }

    val result = searcher.search("Utrecht Centraal", GeoPoint(52.09, 5.11), utrecht, Language.NL)

    val hits = assertIs<SearchResult.Hits>(result).hits
    assertEquals("Utrecht Centraal", hits.first().name)
    assertEquals(52.089, hits.first().latitude, 0.001)
  }

  @Test
  fun `Search unavailable in the planner - no answer from the search service`() = runTest {
    val searcher = PlaceSearcher { null }

    assertEquals(
      SearchResult.Unavailable,
      searcher.search("Utrecht Centraal", null, utrecht, Language.EN),
    )
  }

  @Test
  fun `Café on the card - name, kind and opening hours`() {
    val cafe = MapPlace(GeoPoint(50.8, 7.2), "Eiscafé Riviera", PlaceKind.FOOD, "cafe")
    val details =
      PlaceDetails(website = "riviera.example", phone = null, openingHours = "Mo-Su 10:00-20:00")
    val noon = LocalMoment(CalendarDay(2027, 5, 1), 12 * 60)

    val card =
      PlaceCard.of(cafe, details, noon, Language.EN) { key ->
        mapOf("place.food" to "Food and drink")[key] ?: key
      }

    assertEquals("Eiscafé Riviera", card.title)
    assertEquals("Food and drink", card.kind)
    assertEquals("Mo-Su 10:00-20:00", card.hours)
    assertEquals(true, card.status?.open)
    assertEquals("https://riviera.example", card.website)
  }

  @Test
  fun `Place added to the route - a route point named after the café`() = runTest {
    val planner = TestNetwork.planner(this)
    advanceUntilIdle()
    val start = TestNetwork.graph().knots.getValue(K02)
    planner.click(start.latitude, start.longitude, 30.0, null)
    val at = TestNetwork.position(-1_000.0, 200.0)
    val cafe = MapPlace(at, "Eiscafé Riviera", PlaceKind.FOOD, "cafe")

    val offer = assertIs<Clicked.Offer>(planner.click(at.latitude, at.longitude, 30.0, cafe))
    planner.add(offer.offer)

    val point = planner.session!!.waypoints.last()
    assertEquals("Eiscafé Riviera", point.routePoint?.placeName)
  }

  private suspend fun kotlinx.coroutines.test.TestScope.routeOver(
    vararg ids: Long
  ): RouteDescription.Complete {
    val planner = TestNetwork.planner(this)
    advanceUntilIdle()
    val graph = TestNetwork.graph()
    ids.forEach { id ->
      graph.knots.getValue(id).let { planner.click(it.latitude, it.longitude, 30.0, null) }
    }
    return assertIs<RouteDescription.Complete>(planner.description)
  }

  @Test
  fun `Ferry on the route - open now and until when`() = runTest {
    val description = routeOver(K28, K73)
    val ferry = description.ferries.single { it.title == "Rheinfähre" }

    val status = assertNotNull(ferry.statusAt(LocalMoment(CalendarDay(2027, 5, 1), 12 * 60)))

    assertEquals(true, status.open)
    assertEquals("Open until 19:00", status.text)
    assertEquals("83 → 27", ferry.place)
  }

  @Test
  fun `Ferry listed without operating times - so the visitor checks them`() = runTest {
    val description = routeOver(K02, K27)
    val ferry = description.ferries.single { it.title == "Fähre Mondorf" }

    assertEquals(false, ferry.timesKnown)
    assertEquals(null, ferry.statusAt(LocalMoment(CalendarDay(2027, 5, 1), 12 * 60)))
  }

  private companion object {
    const val STATION =
      """{"type": "FeatureCollection", "features": [{"type": "Feature", "geometry": {"type": "Point", "coordinates": [5.1096021, 52.0893825]}, "properties": {"osm_key": "building", "osm_value": "train_station", "type": "house", "name": "Utrecht Centraal", "street": "Stationshal", "housenumber": "12", "city": "Utrecht", "state": "Utrecht", "country": "Netherlands"}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [5.1112243, 52.0890443]}, "properties": {"osm_key": "building", "osm_value": "yes", "type": "house", "name": "Utrecht Centraal", "street": "Stationshal", "city": "Utrecht", "state": "Utrecht", "country": "Netherlands"}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [5.109821, 52.0893925]}, "properties": {"osm_key": "railway", "osm_value": "station", "type": "house", "name": "Utrecht Centraal", "street": "Stationshal", "housenumber": "12", "city": "Utrecht", "state": "Utrecht", "country": "Netherlands"}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [5.1089728, 52.0895085]}, "properties": {"osm_key": "railway", "osm_value": "stop", "type": "house", "name": "Utrecht Centraal", "street": "Stationshal", "housenumber": "12", "city": "Utrecht", "state": "Utrecht", "country": "Netherlands"}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [5.1098192, 52.0897617]}, "properties": {"osm_key": "railway", "osm_value": "stop", "type": "house", "name": "Utrecht Centraal", "street": "Stationshal", "housenumber": "12", "city": "Utrecht", "state": "Utrecht", "country": "Netherlands"}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [5.1101619, 52.0898821]}, "properties": {"osm_key": "railway", "osm_value": "stop", "type": "house", "name": "Utrecht Centraal", "street": "Stationshal", "housenumber": "12", "city": "Utrecht", "state": "Utrecht", "country": "Netherlands"}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [5.1095873, 52.0896802]}, "properties": {"osm_key": "railway", "osm_value": "stop", "type": "house", "name": "Utrecht Centraal", "street": "Stationshal", "housenumber": "12", "city": "Utrecht", "state": "Utrecht", "country": "Netherlands"}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [5.1098758, 52.0897816]}, "properties": {"osm_key": "railway", "osm_value": "stop", "type": "house", "name": "Utrecht Centraal", "street": "Stationshal", "housenumber": "12", "city": "Utrecht", "state": "Utrecht", "country": "Netherlands"}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [5.1095284, 52.0896595]}, "properties": {"osm_key": "railway", "osm_value": "stop", "type": "house", "name": "Utrecht Centraal", "street": "Stationshal", "housenumber": "12", "city": "Utrecht", "state": "Utrecht", "country": "Netherlands"}}, {"type": "Feature", "geometry": {"type": "Point", "coordinates": [5.1091981, 52.0895954]}, "properties": {"osm_key": "railway", "osm_value": "stop", "type": "house", "name": "Utrecht Centraal", "street": "Stationshal", "housenumber": "12", "city": "Utrecht", "state": "Utrecht", "country": "Netherlands"}}]}"""
  }
}
