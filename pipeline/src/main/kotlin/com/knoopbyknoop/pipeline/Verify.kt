package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.GeoPoint
import com.knoopbyknoop.core.GraphFormatException
import com.knoopbyknoop.core.IndexedFileKind
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.InstallCheck
import com.knoopbyknoop.core.KnotLocator
import com.knoopbyknoop.core.NetworkGraph
import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.NetworkInstaller
import com.knoopbyknoop.core.PathFile
import com.knoopbyknoop.core.PlaceFile
import com.knoopbyknoop.core.ShortestPathRouter
import com.knoopbyknoop.core.distanceMeters
import com.knoopbyknoop.core.sha256Hex
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.math.roundToInt

/** The fewest knots and connections a network may have, see `network-data`. */
data class NetworkFloor(val knots: Int, val connections: Int)

/** The fewest squares and segments the path data may have, see `network-data`. */
data class PathFloor(val squares: Int, val segments: Long)

/** A route that must stay routable, between the knots nearest to two positions. */
data class KnownRoute(
  val name: String,
  val network: IndexedNetworkType,
  val from: GeoPoint,
  val to: GeoPoint,
)

/** What [verify] measured, line by line, and what failed. */
class Verification(val log: List<String>, val failures: List<String>) {
  val passed: Boolean
    get() = failures.isEmpty()
}

/** About 90 % of the counts of 2026-11-10, see the design of `gate-network-data-publish`. */
val NETWORK_FLOORS =
  mapOf(
    IndexedNetworkType.CYCLING to NetworkFloor(knots = 18_000, connections = 28_000),
    IndexedNetworkType.HIKING to NetworkFloor(knots = 45_000, connections = 67_000),
  )

val PATH_FLOOR = PathFloor(squares = 3_300, segments = 9_000_000)

/** The most a network may lose against the published one in one rebuild. */
const val MAX_DROP = 0.05

/** How far a known route's position may be from its knot. */
const val KNOWN_ROUTE_RADIUS_METERS = 1_000.0

/** How much longer than the straight line a known route may be. */
const val KNOWN_ROUTE_MAX_DETOUR = 2.0

/** Chosen at well-established knots, each routing on the data of 2026-11-10. */
val KNOWN_ROUTES =
  listOf(
    KnownRoute(
      "Breda → Antwerp",
      IndexedNetworkType.CYCLING,
      GeoPoint(51.5890, 4.7760),
      GeoPoint(51.2190, 4.4020),
    ),
    KnownRoute(
      "Hasselt → Aachen",
      IndexedNetworkType.CYCLING,
      GeoPoint(50.9300, 5.3370),
      GeoPoint(50.7760, 6.0840),
    ),
    KnownRoute(
      "Nijmegen → Kleve",
      IndexedNetworkType.CYCLING,
      GeoPoint(51.8430, 5.8580),
      GeoPoint(51.7880, 6.1380),
    ),
    KnownRoute(
      "Veluwe: Arnhem → Ede",
      IndexedNetworkType.HIKING,
      GeoPoint(51.9850, 5.8990),
      GeoPoint(52.0400, 5.6650),
    ),
    KnownRoute(
      "Utrecht → Amstelveen",
      IndexedNetworkType.HIKING,
      GeoPoint(52.1219, 5.1164),
      GeoPoint(52.2802, 4.9353),
    ),
    KnownRoute(
      "Kempen: Geel → Beringen",
      IndexedNetworkType.HIKING,
      GeoPoint(51.1509, 5.0685),
      GeoPoint(51.1661, 5.3382),
    ),
  )

/**
 * Checks the rebuilt network data in [directory] before it is published, see `network-data`
 * "Checked before publication": every file read back as the apps read it, the floors, the drop
 * against the data published at [publishedBase] unless [allowDrop], and the [knownRoutes].
 *
 * @param publishedBase where the published `index.json` is, or `null` to skip the drop check
 * @param pathFloor the floor of the path data, or `null` if there is none to check
 */
fun verify(
  directory: Path,
  publishedBase: String?,
  allowDrop: Boolean = false,
  networkFloors: Map<IndexedNetworkType, NetworkFloor> = NETWORK_FLOORS,
  pathFloor: PathFloor? = PATH_FLOOR,
  knownRoutes: List<KnownRoute> = KNOWN_ROUTES,
): Verification {
  val log = ArrayList<String>()
  val failures = ArrayList<String>()
  fun fail(message: String) {
    failures += message
    log += "FAILED: $message"
  }

  val indexFile = directory.resolve("index.json")
  val index =
    try {
      NetworkIndex.parse(indexFile.readText())
    } catch (exception: Exception) {
      fail("index.json cannot be read: ${exception.message}")
      return Verification(log, failures)
    }

  val graphs = readGraphs(directory, index, log, ::fail)
  for ((network, floor) in networkFloors) {
    val graph = graphs[network]
    val knots = graph?.knots?.size ?: 0
    val connections = graph?.connections?.size ?: 0
    val name = network.name.lowercase()
    if (knots < floor.knots || connections < floor.connections) {
      fail(
        "$name has $knots knots and $connections connections, " +
          "fewer than ${floor.knots} and ${floor.connections}"
      )
    } else {
      log +=
        "$name: $knots knots, $connections connections (floor ${floor.knots}, ${floor.connections})"
    }
  }

  if (pathFloor != null) checkPaths(directory, index, pathFloor, log, ::fail)
  checkPlaces(directory, index, log, ::fail)

  when {
    publishedBase == null -> log += "drop not checked: no published data given"
    allowDrop -> log += "drop not checked: allowed for this run"
    else -> checkDrop(publishedBase, graphs, log, ::fail)
  }

  for (route in knownRoutes) checkRoute(route, graphs[route.network], log, ::fail)

  return Verification(log, failures)
}

/** Graph and geometry files of [index], checked as an app checks a download. */
private fun readGraphs(
  directory: Path,
  index: NetworkIndex,
  log: MutableList<String>,
  fail: (String) -> Unit,
): Map<IndexedNetworkType, NetworkGraph> {
  val graphs = HashMap<IndexedNetworkType, NetworkGraph>()
  for (graphEntry in index.files.filter { it.kind == IndexedFileKind.GRAPH }) {
    val bytes = directory.resolve(graphEntry.file).takeIf { it.exists() }?.readBytes()
    if (bytes == null) {
      fail("${graphEntry.file} is missing")
      continue
    }
    val graph =
      when (val check = NetworkInstaller.checkGraph(graphEntry, bytes)) {
        is InstallCheck.Valid -> check.content.graph
        is InstallCheck.Rejected -> {
          fail("${graphEntry.file} is ${check.reason}")
          continue
        }
      }
    log += "${graphEntry.file}: read back"
    val geometryEntry =
      index.files.find { it.network == graphEntry.network && it.kind == IndexedFileKind.GEOMETRY }
    val geometryBytes =
      geometryEntry?.let { directory.resolve(it.file) }?.takeIf { it.exists() }?.readBytes()
    if (geometryEntry == null || geometryBytes == null) {
      fail("the geometry of ${graphEntry.file} is missing")
    } else {
      when (
        val check = NetworkInstaller.checkGeometry(geometryEntry, geometryBytes, graphEntry, graph)
      ) {
        is InstallCheck.Valid -> log += "${geometryEntry.file}: read back"
        is InstallCheck.Rejected -> fail("${geometryEntry.file} is ${check.reason}")
      }
    }
    graphs[graphEntry.network] = graph
  }
  return graphs
}

/** Every square of the path data unpacked and checked against its checksum. */
private fun checkPaths(
  directory: Path,
  index: NetworkIndex,
  floor: PathFloor,
  log: MutableList<String>,
  fail: (String) -> Unit,
) {
  val entry = index.paths
  val file = entry?.let { directory.resolve(it.file) }
  if (entry == null || file == null || !file.exists()) {
    fail("the path data is missing")
    return
  }
  try {
    RandomAccessFile(file.toFile(), "r").use { access ->
      val read = { offset: Long, size: Int ->
        access.seek(offset)
        ByteArray(size).also(access::readFully)
      }
      val directoryBytes = read(entry.directoryOffset, entry.directorySize)
      if (
        access.length() != entry.sizeBytes || sha256Hex(directoryBytes) != entry.directorySha256
      ) {
        fail("${entry.file} does not match index.json")
        return
      }
      val pathFile = PathFile.open(read)
      val squares = PathFile.parseDirectory(directoryBytes).entries.keys
      var segments = 0L
      val broken = ArrayList<String>()
      for (square in squares) {
        try {
          segments += pathFile.segments(square).size
        } catch (exception: GraphFormatException) {
          broken += "${square.latitudeIndex}/${square.longitudeIndex}"
        }
      }
      if (broken.isNotEmpty()) {
        fail("${entry.file} has ${broken.size} broken squares: ${broken.take(5).joinToString()}")
      }
      if (squares.size < floor.squares || segments < floor.segments) {
        fail(
          "${entry.file} has ${squares.size} squares and $segments segments, " +
            "fewer than ${floor.squares} and ${floor.segments}"
        )
      } else {
        log +=
          "${entry.file}: ${squares.size} squares, $segments segments " +
            "(floor ${floor.squares}, ${floor.segments})"
      }
    }
  } catch (exception: GraphFormatException) {
    fail("${entry.file} cannot be read: ${exception.message}")
  }
}

/**
 * Every square of the place details unpacked and checked against its checksum, if they are
 * published; a failed places build leaves them out of the index, see `network-data` "Failed places
 * build".
 */
private fun checkPlaces(
  directory: Path,
  index: NetworkIndex,
  log: MutableList<String>,
  fail: (String) -> Unit,
) {
  val entry = index.places
  if (entry == null) {
    log += "place details: not published in this run"
    return
  }
  val file = directory.resolve(entry.file)
  if (!file.exists()) {
    fail("${entry.file} is in index.json but missing")
    return
  }
  try {
    RandomAccessFile(file.toFile(), "r").use { access ->
      val read = { offset: Long, size: Int ->
        access.seek(offset)
        ByteArray(size).also(access::readFully)
      }
      val directoryBytes = read(entry.directoryOffset, entry.directorySize)
      if (
        access.length() != entry.sizeBytes || sha256Hex(directoryBytes) != entry.directorySha256
      ) {
        fail("${entry.file} does not match index.json")
        return
      }
      val places = PlaceFile.open(read)
      val squares = PlaceFile.SQUARES.parseDirectory(directoryBytes).entries.keys
      var entries = 0L
      val broken = ArrayList<String>()
      for (square in squares) {
        try {
          entries += places.details(square).size
        } catch (exception: GraphFormatException) {
          broken += "${square.latitudeIndex}/${square.longitudeIndex}"
        }
      }
      if (broken.isNotEmpty()) {
        fail("${entry.file} has ${broken.size} broken squares: ${broken.take(5).joinToString()}")
      } else {
        log += "${entry.file}: ${squares.size} squares, $entries places"
      }
    }
  } catch (exception: GraphFormatException) {
    fail("${entry.file} cannot be read: ${exception.message}")
  }
}

/** Fails when a network loses more than [MAX_DROP] of its knots or connections. */
private fun checkDrop(
  publishedBase: String,
  graphs: Map<IndexedNetworkType, NetworkGraph>,
  log: MutableList<String>,
  fail: (String) -> Unit,
) {
  val base = directoryUri(publishedBase)
  val published =
    try {
      NetworkIndex.parse(fetch(base.resolve("index.json")).decodeToString())
    } catch (exception: IOException) {
      log += "WARNING: drop not checked, the published index.json cannot be fetched"
      return
    } catch (exception: IllegalArgumentException) {
      log += "WARNING: drop not checked, the published index.json cannot be read"
      return
    }
  for (entry in published.files.filter { it.kind == IndexedFileKind.GRAPH }) {
    val name = entry.network.name.lowercase()
    val check =
      try {
        NetworkInstaller.checkGraph(entry, fetch(base.resolve(entry.file)))
      } catch (exception: IOException) {
        log += "WARNING: $name drop not checked, the published ${entry.file} cannot be fetched"
        continue
      }
    val before =
      when (check) {
        is InstallCheck.Valid -> check.content.graph
        is InstallCheck.Rejected -> {
          log += "WARNING: $name drop not checked, the published ${entry.file} is ${check.reason}"
          continue
        }
      }
    // A network missing from the rebuild fails its floor already.
    val after = graphs[entry.network] ?: continue
    for ((what, beforeCount, afterCount) in
      listOf(
        Triple("knots", before.knots.size, after.knots.size),
        Triple("connections", before.connections.size, after.connections.size),
      )) {
      val drop = if (beforeCount == 0) 0.0 else (beforeCount - afterCount).toDouble() / beforeCount
      val change = "$beforeCount → $afterCount $what (${percent(-drop)})"
      if (drop > MAX_DROP) {
        fail("$name dropped from $change, more than ${percent(-MAX_DROP)}")
      } else {
        log += "$name: $change"
      }
    }
  }
}

private fun checkRoute(
  route: KnownRoute,
  graph: NetworkGraph?,
  log: MutableList<String>,
  fail: (String) -> Unit,
) {
  if (graph == null) {
    fail("${route.name}: no ${route.network.name.lowercase()} network")
    return
  }
  val locator = KnotLocator(graph.knots.values)
  val from = locator.nearest(route.from.latitude, route.from.longitude, KNOWN_ROUTE_RADIUS_METERS)
  val to = locator.nearest(route.to.latitude, route.to.longitude, KNOWN_ROUTE_RADIUS_METERS)
  if (from == null || to == null) {
    val position = if (from == null) route.from else route.to
    fail(
      "${route.name}: no knot within ${KNOWN_ROUTE_RADIUS_METERS.roundToInt()} m of " +
        "${position.latitude}, ${position.longitude}"
    )
    return
  }
  val path = ShortestPathRouter(graph).shortestPath(from, to)
  val straight = distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)
  val name = "${route.name} (${from.ref} → ${to.ref})"
  when {
    path == null -> fail("$name: no route")
    path.lengthMeters > straight * KNOWN_ROUTE_MAX_DETOUR ->
      fail(
        "$name: ${path.lengthMeters} m, more than $KNOWN_ROUTE_MAX_DETOUR times " +
          "the straight line of ${straight.roundToInt()} m"
      )
    else -> log += "$name: ${path.lengthMeters} m, straight line ${straight.roundToInt()} m"
  }
}

private fun percent(share: Double) = "%+.1f %%".format(share * 100)
