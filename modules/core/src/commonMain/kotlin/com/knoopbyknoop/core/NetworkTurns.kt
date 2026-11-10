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
 * @property lead the leg's course from the turn to a little after it, to look for the way from its
 *   end: at the turn itself, a search could start on a path crossing above or below
 * @property restMeters how far the leg goes on after the turn
 */
internal class NetworkTurn(
  val meters: Double,
  val position: GeoPoint,
  val direction: TurnDirection?,
  val towards: Knot?,
  val lead: List<GeoPoint>,
  val restMeters: Double,
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

/** How far after a turn-off the way to the route point is looked for from. */
private const val WAY_SEARCH_AFTER_METERS = 40.0

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
  val line = Polyline(course)
  val at = line.pointAt(meters)
  val before = line.pointAt(meters - Navigation.TURN_METERS)
  val after = line.pointAt(meters + Navigation.TURN_METERS)
  val beforeMeters = distanceMeters(before.latitude, before.longitude, at.latitude, at.longitude)
  val afterMeters = distanceMeters(at.latitude, at.longitude, after.latitude, after.longitude)
  val known =
    beforeMeters >= Navigation.TURN_MIN_METERS && afterMeters >= Navigation.TURN_MIN_METERS
  val direction = if (known) turnOf(signedAngle(bearing(before, at), bearing(at, after))) else null
  val lead = line.stretch(meters, meters + WAY_SEARCH_AFTER_METERS)
  return NetworkTurn(meters, at, direction, towards, lead, line.length - meters)
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

/** [way], found from the end of [lead], with [lead] in front of it, so it starts at the turn. */
internal fun ledBy(lead: List<GeoPoint>, way: PathWay): PathWay {
  val points = lead + way.points
  val meters = Polyline(points).meters
  val offset = meters[lead.size]
  return PathWay(
    points,
    meters,
    way.turns.map {
      it.copy(
        atMeters = it.atMeters + offset,
        doubtful =
          it.doubtful?.let { doubt ->
            doubt.copy(
              startMeters = doubt.startMeters + offset,
              endMeters = doubt.endMeters + offset,
            )
          },
      )
    },
    startsBack = false,
    firstName = way.firstName,
    // Kept, so a way to a route point shows its junction pictures too, see `knot-navigation`
    // "Picture in the app".
    doubtful =
      way.doubtful.map {
        it.copy(startMeters = it.startMeters + offset, endMeters = it.endMeters + offset)
      },
    decisionMeters = way.decisionMeters.map { it + offset },
  )
}
