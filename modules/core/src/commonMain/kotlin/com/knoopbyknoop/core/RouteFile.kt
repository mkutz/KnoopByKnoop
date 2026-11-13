package com.knoopbyknoop.core

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * A route as a Markdown list of knots, the file people share, see `route-sharing` "Route file": the
 * waypoints as top-level items and the knots passed between them indented below. Only the first
 * knot carries its node ID; every further knot is found from the one before it.
 */
object RouteFile {

  const val EXTENSION = "knoop"

  /** How far from the knot before a knot with the listed number is looked for, see "Reading". */
  private const val SEARCH_METERS = 5_000.0

  /** Node IDs have at least this many digits, so a number in a text isn't taken for one. */
  private const val NODE_ID_DIGITS = 5

  /**
   * One list item of a file.
   *
   * @property waypoint whether it is a top-level item, a waypoint, rather than a knot passed
   * @property ref the knot's number, `null` for a route point
   * @property position the route point's position, `null` for a knot
   * @property nodeId the knot's OpenStreetMap node ID, where the file gives it
   * @property text the text after the number, ID or position
   */
  data class Item(
    val waypoint: Boolean,
    val ref: String? = null,
    val position: GeoPoint? = null,
    val nodeId: Long? = null,
    val text: String? = null,
  )

  /** A file as read, before its knots are found in a network. */
  data class Parsed(val title: String?, val network: NetworkType, val items: List<Item>)

  /**
   * A file's route found in a network.
   *
   * @property snapshot the waypoints, restored like a stored route
   * @property leftOut the numbers of the knots that could not be found
   * @property listed the IDs of every knot found, waypoints and knots passed, in riding order
   */
  data class Resolved(
    val snapshot: RouteSnapshot,
    val leftOut: List<String>,
    val listed: List<Long>,
  )

  /** Whether [route] can be shared: complete, as the file lists every knot it passes. */
  fun canShare(route: Route?): Boolean = route?.summary is RouteSummary.Complete

  /**
   * The file of [route] with [title]; [labels] are the waypoints' own texts by knot ID, written in
   * place of their town, see `route-sharing` "Knot texts in a route file".
   */
  fun write(route: Route, title: String, labels: Map<Long, String> = mapOf()): String {
    require(canShare(route)) { "Only a complete route can be shared" }
    return buildString {
      append("# ").append(title.lineSequence().first().trim()).append('\n')
      if (route.waypoints.first().network == NetworkType.HIKING) append("Network: hiking\n")
      append('\n')
      var anchored = false
      var lastTown: String? = null
      for ((knot, role) in Gpx.knotsWithRoles(route)) {
        append(if (role == Gpx.Role.KNOT) "  - " else "- ")
        val point = knot.routePoint
        if (point != null) {
          append(decimal(knot.latitude)).append(", ").append(decimal(knot.longitude))
          point.placeName?.let { append(' ').append(oneLine(it)) }
          lastTown = null
        } else {
          append(knot.ref)
          if (!anchored) append(' ').append(knot.id)
          anchored = true
          val town = knot.place?.name
          val label = labels[knot.id]?.takeIf { role != Gpx.Role.KNOT }
          when {
            label != null -> append(' ').append(oneLine(label))
            town != null && town != lastTown -> append(' ').append(town)
          }
          lastTown = town
        }
        append('\n')
      }
    }
  }

  /** Reads [text]; never fails, lines it doesn't understand are skipped. */
  fun parse(text: String): Parsed {
    var title: String? = null
    var network = NetworkType.CYCLING
    var baseIndent: Int? = null
    val items = mutableListOf<Item>()
    for (line in text.lineSequence()) {
      val trimmed = line.trim()
      when {
        title == null && trimmed.startsWith("# ") -> title = trimmed.removePrefix("# ").trim()
        trimmed.lowercase().startsWith("network:") ->
          if (trimmed.substringAfter(':').trim().lowercase() == "hiking")
            network = NetworkType.HIKING
        else -> {
          val match = ITEM.matchEntire(line.trimEnd()) ?: continue
          val indent = match.groupValues[1].replace("\t", "    ").length
          val base = baseIndent ?: indent.also { baseIndent = it }
          itemOf(match.groupValues[2], waypoint = indent <= base)?.let { items += it }
        }
      }
    }
    return Parsed(title, network, items)
  }

  private val ITEM = Regex("""^(\s*)[-*+]\s+(.+)$""")
  private val POSITION =
    Regex("""^(-?\d{1,2}(?:\.\d+)?)\s*,\s*(-?\d{1,3}(?:\.\d+)?)(?:\s+(.*))?$""")

  private fun itemOf(content: String, waypoint: Boolean): Item? {
    POSITION.matchEntire(content.trim())?.let { match ->
      val latitude = match.groupValues[1].toDouble()
      val longitude = match.groupValues[2].toDouble()
      if (abs(latitude) > 90 || abs(longitude) > 180) return null
      return Item(
        waypoint = true,
        position = GeoPoint(latitude, longitude),
        text = match.groupValues[3].trim().ifEmpty { null },
      )
    }
    val words = content.trim().split(Regex("""\s+"""), limit = 3)
    val ref = words[0]
    val idWord = words.getOrNull(1)?.takeIf { it.length >= NODE_ID_DIGITS && it.all(Char::isDigit) }
    val text = (if (idWord != null) words.drop(2) else words.drop(1)).joinToString(" ").trim()
    return Item(waypoint, ref, nodeId = idWord?.toLongOrNull(), text = text.ifEmpty { null })
  }

  /**
   * The route of [parsed] found in [graph], the installed network of the file's network, see
   * `route-sharing` "Reading a route file"; `null` if fewer than two waypoints can be found.
   */
  fun resolve(parsed: Parsed, graph: NetworkGraph): Resolved? {
    val network = requireNotNull(graph.network) { "Cannot read a route on an empty network" }
    val waypoints = mutableListOf<RouteSnapshot.Waypoint>()
    val leftOut = mutableListOf<String>()
    val listed = mutableListOf<Long>()
    var previous: Knot? = null
    var previousPosition: GeoPoint? = null
    for (item in parsed.items) {
      val position = item.position
      if (position != null) {
        waypoints += pointWaypoint(position, item.text)
        previous = null
        previousPosition = position
        continue
      }
      val ref = item.ref ?: continue
      val knot = find(item, ref, graph, previous, previousPosition)
      if (knot == null) {
        leftOut += ref
        continue
      }
      if (item.waypoint) {
        val label = item.text?.takeIf { it != knot.place?.name }
        waypoints += RouteSnapshot.Waypoint(knot.id, knot.ref, label = label)
      }
      listed += knot.id
      previous = knot
      previousPosition = null
    }
    if (waypoints.size < 2) return null
    return Resolved(RouteSnapshot(IndexedNetworkType.of(network), waypoints), leftOut, listed)
  }

  private fun find(
    item: Item,
    ref: String,
    graph: NetworkGraph,
    previous: Knot?,
    previousPosition: GeoPoint?,
  ): Knot? {
    item.nodeId?.let { id ->
      graph.knots[id]?.let {
        return it
      }
    }
    val from =
      previous?.let { GeoPoint(it.latitude, it.longitude) }
        ?: previousPosition
        ?: return graph.knots.values
          .filter { it.ref == ref && item.text != null && it.place?.name == item.text }
          .minByOrNull { it.id }
    if (previous != null) {
      val neighbors = graph.neighborsOf(previous).map { it.first }.filter { it.ref == ref }
      if (neighbors.isNotEmpty())
        return neighbors.firstOrNull { item.text != null && it.place?.name == item.text }
          ?: neighbors.first()
    }
    return graph.knots.values
      .filter { it.ref == ref }
      .map { it to distanceMeters(from, GeoPoint(it.latitude, it.longitude)) }
      .filter { it.second <= SEARCH_METERS }
      .minByOrNull { it.second }
      ?.first
  }

  /** A route point at [position], not yet joined to the network; `reattached` joins it. */
  private fun pointWaypoint(position: GeoPoint, name: String?) =
    RouteSnapshot.Waypoint(
      routePointIdOf(position),
      "",
      RouteSnapshot.StoredPoint(
        position.latitude,
        position.longitude,
        fromId = 0,
        toId = 0,
        fraction = 0.0,
        placeLatitude = position.latitude,
        placeLongitude = position.longitude,
        placeName = name,
      ),
    )

  /**
   * Whether the knots [route] passes differ from those the file [listed], see "Route different from
   * the list".
   */
  fun differsFrom(route: Route, listed: List<Long>): Boolean {
    val summary = route.summary as? RouteSummary.Complete ?: return true
    return summary.knots.filterNot { it.isRoutePoint }.map { it.id } != listed
  }

  /** E.g. `knoop-by-knoop-cycling-12-34.knoop`, the GPX export's name with its own extension. */
  fun fileName(route: Route): String = Gpx.fileName(route).removeSuffix(".gpx") + ".$EXTENSION"

  /** The start's and the end's number and town, e.g. `02 Siegburg → 73 Bonn`. */
  fun titleOf(route: Route): String =
    "${nameOf(route.waypoints.first(), "Start")} → ${nameOf(route.waypoints.last(), "End")}"

  private fun nameOf(knot: Knot, role: String): String =
    if (knot.isRoutePoint) knot.routePoint?.placeName ?: role
    else listOfNotNull(knot.ref, knot.place?.name).joinToString(" ")

  private fun oneLine(text: String) = text.lineSequence().joinToString(" ") { it.trim() }.trim()

  /** With 5 decimals and a point, whatever the device's locale. */
  private fun decimal(value: Double): String {
    val scaled = (value * 100_000).roundToLong()
    val sign = if (scaled < 0) "-" else ""
    val whole = abs(scaled) / 100_000
    val fraction = (abs(scaled) % 100_000).toString().padStart(5, '0')
    return "$sign$whole.$fraction"
  }
}
