package com.knoopbyknoop.web

import com.knoopbyknoop.core.Connection
import com.knoopbyknoop.core.Ferry
import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.Language
import com.knoopbyknoop.core.NetworkGeometry
import com.knoopbyknoop.core.NetworkGraph
import com.knoopbyknoop.core.NetworkType
import com.knoopbyknoop.core.PathDataOrigin
import com.knoopbyknoop.core.Place
import kotlinx.coroutines.CoroutineScope

/**
 * A network along the Sieg like the core's route file tests: Siegburg 02 → 28 → 29 → 30 → 83 → 27 →
 * 73 westwards, 2 km apart, a ferry without known times between 29 and 30 and one running every day
 * from 7 to 19 between 83 and 27; and 12 → 40 → 34 three kilometres north, 12 joined to 02.
 */
object TestNetwork {

  private const val LATITUDE = 50.8
  private const val LONGITUDE = 7.2
  private const val METERS_PER_DEGREE_LATITUDE = 111_320.0
  private val metersPerDegreeLongitude =
    111_320.0 * kotlin.math.cos(LATITUDE * kotlin.math.PI / 180)

  private val siegburg = Place("Siegburg")
  private val bonn = Place("Bonn")

  fun position(east: Double, north: Double = 0.0) =
    GeoPoint(
      LATITUDE + north / METERS_PER_DEGREE_LATITUDE,
      LONGITUDE + east / metersPerDegreeLongitude,
    )

  private fun knot(id: Long, ref: String, east: Double, north: Double, network: NetworkType) =
    position(east, north).let {
      Knot(
        id,
        ref,
        network,
        it.latitude,
        it.longitude,
        place = if (east < -7_000) bonn else siegburg,
      )
    }

  const val K02 = 6364701L
  const val K28 = 6364702L
  const val K29 = 6364703L
  const val K30 = 6364704L
  const val K83 = 6364705L
  const val K27 = 6364706L
  const val K73 = 6364707L
  const val K12 = 6364708L
  const val K40 = 6364709L
  const val K34 = 6364710L

  private fun knots(network: NetworkType) =
    listOf(
      knot(K02, "02", 0.0, 0.0, network),
      knot(K28, "28", -2_000.0, 0.0, network),
      knot(K29, "29", -4_000.0, 0.0, network),
      knot(K30, "30", -6_000.0, 0.0, network),
      knot(K83, "83", -8_000.0, 0.0, network),
      knot(K27, "27", -9_000.0, -1_000.0, network),
      knot(K73, "73", -10_000.0, -2_000.0, network),
      knot(K12, "12", 0.0, 3_000.0, network),
      knot(K40, "40", -2_000.0, 3_000.0, network),
      knot(K34, "34", -4_000.0, 3_000.0, network),
    )

  private val connections =
    listOf(
      Connection(K02, K28, 2_000),
      Connection(K28, K29, 2_000),
      Connection(K29, K30, 2_000),
      Connection(K30, K83, 2_000),
      Connection(K83, K27, 1_400),
      Connection(K27, K73, 1_400),
      Connection(K12, K40, 2_000),
      Connection(K40, K34, 2_000),
      Connection(K12, K02, 3_000),
    )

  private val ferries =
    mapOf(
      Connection(K29, K30, 2_000) to Ferry(name = "Fähre Mondorf"),
      Connection(K83, K27, 1_400) to Ferry(name = "Rheinfähre", openingHours = "Mo-Su 07:00-19:00"),
    )

  fun graph(network: NetworkType = NetworkType.CYCLING) =
    NetworkGraph(knots(network), connections, ferries)

  fun geometry(graph: NetworkGraph) =
    NetworkGeometry(
      graph,
      graph.connections.map { connection ->
        val from = graph.knots.getValue(connection.fromId)
        val to = graph.knots.getValue(connection.toId)
        listOf(GeoPoint(from.latitude, from.longitude), GeoPoint(to.latitude, to.longitude))
      },
    )

  /** The networks of this test, loaded at once, without path data. */
  class Source : NetworkSource {
    val graphs = IndexedNetworkType.entries.associateWith { graph(it.network) }
    var squareLoads = 0

    override suspend fun graph(network: IndexedNetworkType) =
      NetworkSource.Loaded(
        LoadedNetwork(
          network,
          graphs.getValue(network),
          NetworkGeometry.straightLines(graphs.getValue(network)),
          false,
        ),
        "",
      )

    override suspend fun courses(loaded: NetworkSource.Loaded) =
      LoadedNetwork(
        loaded.network.network,
        loaded.network.graph,
        geometry(loaded.network.graph),
        true,
      )

    override suspend fun <T> withSquares(load: (PathDataOrigin) -> T): T? {
      squareLoads++
      return null
    }
  }

  /** A browser's storage, in memory. */
  class Storage : Kept {
    val values = mutableMapOf<String, String>()

    override fun read(key: String) = values[key]

    override fun write(key: String, value: String) {
      values[key] = value
    }
  }

  val texts =
    mapOf(
      "routeFileNone" to "This file holds no route the planner can open.",
      "routeFileLeftOut" to "Left out: {0}",
      "routeFileDiffers" to "The route differs from the file's list.",
      "waypointRemoved" to "Removed: {0}",
      "noCyclingKnotsWithin" to "No cycling knots within {0} km",
      "noHikingKnotsWithin" to "No hiking knots within {0} km",
    )

  fun planner(
    scope: CoroutineScope,
    storage: Kept = Storage(),
    source: NetworkSource = Source(),
    language: Language = Language.EN,
  ) = Planner(source, storage, language, texts, scope, nowMillis = { 0L }).also { it.start() }
}
