package com.knoopbyknoop.core

/**
 * Where a leg's way along paths to or from a route point leaves or joins the network's course, see
 * `knot-navigation` "Guidance to a route point off the network" and "Guidance from a route point
 * onto the network".
 *
 * @property meters how far along the leg's course the turn is
 * @property position where the way leaves or joins the course
 * @property direction the turn there, or `null` where the course or the way is too short to tell
 * @property towards the knot the course leads to in the riding direction, `null` if unknown
 * @property heading the riding direction just before the turn, clockwise from north
 */
internal class NetworkTurn(
  val meters: Double,
  val position: GeoPoint,
  val direction: TurnDirection?,
  val towards: Knot?,
  val heading: Double?,
)

/**
 * The turns of one leg: [turnOff] where its way to a route point leaves the course, [join] where
 * the way from a route point joins it, along a course of [courseMeters].
 */
internal class LegTurns(val turnOff: NetworkTurn?, val join: NetworkTurn?, val courseMeters: Double)

/**
 * The current leg of a navigation with a way off the network.
 *
 * @property leg the leg's position in the route
 * @property meters how far along the leg's course the rider is
 * @property pointPosition where the route point or knot the leg leads to is
 */
internal class NetworkLeg(
  val leg: Int,
  val meters: Double,
  val turns: LegTurns,
  val pointPosition: GeoPoint,
) {

  /** Where a guided way leads now, see [Navigation.offNetworkTarget]. */
  val target: GeoPoint?
    get() {
      val join = turns.join
      if (join != null && meters < join.meters) return join.position
      val turnOff = turns.turnOff
      if (turnOff != null && meters >= turnOff.meters - NETWORK_TURN_NOW_METERS)
        return pointPosition
      return null
    }
}

/** Within this distance of a turn-off or a join, the rider is told to turn now. */
internal const val NETWORK_TURN_NOW_METERS = 40.0

/** From this distance before a turn-off or a join, the rider is reminded of it. */
internal const val NETWORK_TURN_NOTICE_METERS = 100.0

/**
 * The [LegTurns] of each leg of the route through [knots] with their [courses]: a leg to a route
 * point with a way along paths has a turn-off, a leg from one a join; `null` for a leg with
 * neither.
 */
internal fun legTurnsOf(
  knots: List<Knot>,
  graph: NetworkGraph,
  courses: List<List<GeoPoint>>,
): List<LegTurns?> =
  knots.zipWithNext().mapIndexed { leg, (from, to) ->
    val course = courses.getOrNull(leg)?.takeIf { it.size >= 2 } ?: return@mapIndexed null
    val length = lengthOf(course)
    val into = to.routePoint?.takeIf { it.followsPaths }
    val out = from.routePoint?.takeIf { it.followsPaths }
    val turnOff = into?.let { routePoint ->
      val meters = length - lengthOf(routePoint.way)
      if (meters < 0) return@let null
      networkTurn(course, meters, towardsFrom(routePoint, from, graph))
    }
    val join = out?.let { routePoint ->
      val meters = lengthOf(routePoint.way)
      if (meters > length) return@let null
      networkTurn(course, meters, towardsTo(routePoint, to, graph))
    }
    if (turnOff == null && join == null) null else LegTurns(turnOff, join, length)
  }

/** Whether a route point's way runs along paths, so it can be guided. */
private val RoutePoint.followsPaths: Boolean
  get() = !straight && way.size >= 2

private fun networkTurn(course: List<GeoPoint>, meters: Double, towards: Knot?): NetworkTurn {
  val at = pointOn(course, meters)
  val before = pointOn(course, meters - Navigation.TURN_METERS)
  val after = pointOn(course, meters + Navigation.TURN_METERS)
  val beforeMeters = distanceMeters(before.latitude, before.longitude, at.latitude, at.longitude)
  val afterMeters = distanceMeters(at.latitude, at.longitude, after.latitude, after.longitude)
  val known =
    beforeMeters >= Navigation.TURN_MIN_METERS && afterMeters >= Navigation.TURN_MIN_METERS
  val direction = if (known) turnOf(signedAngle(heading(before, at), heading(at, after))) else null
  val riding = if (beforeMeters >= Navigation.TURN_MIN_METERS) heading(before, at) else null
  return NetworkTurn(meters, at, direction, towards, riding?.let { (it + 360) % 360 })
}

/** The knot the course of [routePoint] leads to, riding from [from] towards the point. */
private fun towardsFrom(routePoint: RoutePoint, from: Knot, graph: NetworkGraph): Knot? {
  val id =
    when {
      from.id == routePoint.fromId -> routePoint.toId
      from.id == routePoint.toId -> routePoint.fromId
      from.routePoint?.sharesConnectionWith(routePoint) == true ->
        if (from.routePoint.fractionFrom(routePoint.fromId) < routePoint.fraction) routePoint.toId
        else routePoint.fromId
      else -> return null
    }
  return graph.knots[id]
}

/** The knot the course of [routePoint] leads to, riding from the point towards [to]. */
private fun towardsTo(routePoint: RoutePoint, to: Knot, graph: NetworkGraph): Knot? {
  val id =
    when {
      to.id == routePoint.fromId || to.id == routePoint.toId -> to.id
      to.routePoint?.sharesConnectionWith(routePoint) == true ->
        if (to.routePoint.fractionFrom(routePoint.fromId) > routePoint.fraction) routePoint.toId
        else routePoint.fromId
      else -> return null
    }
  return graph.knots[id]
}

/** The point [meters] along [line], clamped to its ends. */
private fun pointOn(line: List<GeoPoint>, meters: Double): GeoPoint {
  if (meters <= 0) return line.first()
  var covered = 0.0
  for ((from, to) in line.zipWithNext()) {
    val step = distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)
    if (step > 0 && covered + step >= meters) {
      val share = (meters - covered) / step
      return GeoPoint(
        from.latitude + (to.latitude - from.latitude) * share,
        from.longitude + (to.longitude - from.longitude) * share,
      )
    }
    covered += step
  }
  return line.last()
}
