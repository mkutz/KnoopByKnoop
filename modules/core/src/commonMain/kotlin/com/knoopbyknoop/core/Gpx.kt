package com.knoopbyknoop.core

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * A planned route as a GPX 1.1 file, see `map-planning` "Export the route as GPX" and the design of
 * `export-gpx`: every knot passed as a waypoint, then the route's course as one track.
 */
object Gpx {

  /** What a knot is on the route, written as the waypoint's `<type>`. */
  enum class Role {
    START,
    STOP,
    END,
    KNOT,
  }

  /** Whether [route] can be exported: complete, and with its [course] along the network known. */
  fun canExport(route: Route?, course: List<GeoPoint>?): Boolean =
    route?.summary is RouteSummary.Complete && !course.isNullOrEmpty()

  /** Every knot [route] passes in riding order with its role; empty for an incomplete route. */
  fun knotsWithRoles(route: Route): List<Pair<Knot, Role>> {
    if (route.summary !is RouteSummary.Complete) return emptyList()
    val knots = mutableListOf(route.waypoints.first() to Role.START)
    route.legs.forEachIndexed { index, leg ->
      val passed = leg.path!!.knots.drop(1)
      passed.forEachIndexed { position, knot ->
        val role =
          when {
            position < passed.lastIndex -> Role.KNOT
            index == route.legs.lastIndex -> Role.END
            else -> Role.STOP
          }
        knots += knot to role
      }
    }
    return knots
  }

  /** E.g. `knoop-by-knoop-cycling-12-34.gpx`. */
  fun fileName(route: Route): String {
    val network = route.waypoints.first().network.name.lowercase()
    val start = fileNamePart(route.waypoints.first().ref)
    val end = fileNamePart(route.waypoints.last().ref)
    return "knoop-by-knoop-$network-$start-$end.gpx"
  }

  /** The GPX file of [route] along [course], see [canExport]. */
  fun write(route: Route, course: List<GeoPoint>): String {
    require(canExport(route, course)) { "Only a complete route with its course can be exported" }
    val network =
      route.waypoints.first().network.name.lowercase().replaceFirstChar { it.uppercase() }
    val name = "$network ${route.waypoints.first().ref} → ${route.waypoints.last().ref}"
    return buildString {
      append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
      append(
          """<gpx version="1.1" creator="Knoop by Knoop" xmlns="http://www.topografix.com/GPX/1/1">"""
        )
        .append('\n')
      append("  <metadata><name>").append(escape(name)).append("</name></metadata>\n")
      for ((knot, role) in knotsWithRoles(route)) {
        append("  <wpt ").append(coordinates(knot.latitude, knot.longitude)).append(">")
        append("<name>").append(escape(knot.ref)).append("</name>")
        append("<type>").append(role.name.lowercase()).append("</type></wpt>\n")
      }
      append("  <trk>\n    <name>").append(escape(name)).append("</name>\n    <trkseg>\n")
      for (point in course) {
        append("      <trkpt ").append(coordinates(point.latitude, point.longitude)).append("/>\n")
      }
      append("    </trkseg>\n  </trk>\n</gpx>\n")
    }
  }

  private fun coordinates(latitude: Double, longitude: Double) =
    """lat="${decimal(latitude)}" lon="${decimal(longitude)}""""

  /** With 6 decimals and a point, whatever the device's locale. */
  internal fun decimal(value: Double): String {
    val micro = (value * 1_000_000).roundToLong()
    val sign = if (micro < 0) "-" else ""
    val whole = abs(micro) / 1_000_000
    val fraction = (abs(micro) % 1_000_000).toString().padStart(6, '0')
    return "$sign$whole.$fraction"
  }

  private fun escape(text: String) =
    text
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")

  private fun fileNamePart(ref: String) = ref.filter { it.isLetterOrDigit() }.ifEmpty { "x" }
}
