package com.knoopbyknoop.core

/**
 * The question on Navigate about ferries closed by the time the rider gets there, see
 * `knot-navigation` "Riding around a closed ferry" and "Detour on the map".
 *
 * @property warnings the ferry warnings, one per closed ferry, as navigation would say them
 * @property around the route for this navigation through temporary knots on the way around the
 *   closed ferries, `null` when there is no way around; the plan itself stays as it is
 * @property extraDistance how much longer the way around is, e.g. `6.3 km`, `null` without one
 * @property extraTime how much longer it takes at the usual pace, e.g. `24 min`
 * @property lines the ways around, each where the new route parts from the planned one until it
 *   joins it again, in riding order; empty without a way around
 * @property bounds what the map shows while asking: the nearest closed ferry and the way around it
 */
class FerryDetour(
  val warnings: List<String>,
  val around: PlanningSession?,
  val extraDistance: String?,
  val extraTime: String?,
  val lines: List<List<GeoPoint>>,
  val bounds: GeoBounds,
) {
  companion object {

    /** At most this many rounds of leaving out ferries closed on the way around. */
    private const val ROUNDS = 5

    /** How much longer than the way around the route through temporary knots may be. */
    private const val LONGER_SHARE = 0.01

    /**
     * The question for the plan in [session] with the rider at [position] at [nowMillis], or `null`
     * when every ferry on the route is open when the rider gets there.
     *
     * @param geometry the installed network's courses, for the line; straight lines without them
     * @param localTime the ferries' local time of an instant in epoch milliseconds
     */
    fun of(
      session: PlanningSession,
      geometry: NetworkGeometry?,
      position: GeoPoint,
      nowMillis: Long,
      localTime: (Long) -> LocalMoment,
      language: Language = Language.EN,
    ): FerryDetour? {
      val planned = complete(session.route) ?: return null
      val graph = session.routeGraph
      val closedAt = { knots: List<Knot> ->
        closedFerries(graph, knots, position, nowMillis, localTime, language)
      }
      val first = closedAt(planned)
      if (first.isEmpty()) return null
      val warnings = first.values.toMutableList()
      val closedInOrder = first.keys.toMutableList()
      // The way around, found by leaving out the closed ferries, also those closed on the way
      // around.
      var avoided = first.keys
      var aroundRoute: Route? = session.routeAvoiding(avoided)
      repeat(ROUNDS) {
        val knots = complete(aroundRoute) ?: return@repeat
        val more = closedAt(knots)
        if (more.isEmpty()) return@repeat
        warnings += more.values
        closedInOrder += more.keys
        avoided = avoided + more.keys
        aroundRoute = session.routeAvoiding(avoided)
      }
      val wayAround = aroundRoute?.takeIf { complete(it) != null }
      val aroundKnots = complete(wayAround)
      val stretches = aroundKnots?.let { differingStretches(planned, it) }.orEmpty()
      val around = wayAround?.let { through(session, it, stretches, closedAt, avoided) }
      val network = geometry?.withRoutePoints(graph)
      // The map shows the nearest closed ferry with the way around it; further ones are drawn too.
      val nearest = closedInOrder.first()
      val nearestCourse =
        planned
          .zipWithNext()
          .firstOrNull { (from, to) -> pairOf(from.id, to.id) == nearest }
          ?.let { (from, to) -> network?.courseOf(from, to) ?: listOf(pointOf(from), pointOf(to)) }
          .orEmpty()
      val stretchKnots = stretches.map { aroundKnots!!.subList(it.first, it.last + 1) }
      val lines = stretchKnots.map { stretch ->
        stretch.zipWithNext().flatMap { (a, b) ->
          network?.courseOf(a, b) ?: listOf(pointOf(a), pointOf(b))
        }
      }
      val nearestLine =
        stretchKnots.indices
          .firstOrNull { index ->
            val ids = stretchKnots[index].map { it.id }.toSet()
            nearest.first in ids || nearest.second in ids
          }
          ?.let { lines[it] } ?: lines.firstOrNull().orEmpty()
      val pace = RouteProgress.assumedMetersPerSecond(session.network)
      val extraMeters =
        around?.let { lengthOf(it.route) - lengthOf(session.route) }?.coerceAtLeast(0)
      return FerryDetour(
        warnings.distinct(),
        around,
        extraMeters?.let { MapFeatures.formatDistance(it, language) },
        extraMeters?.let { RouteProgress.formatDuration(it / pace) },
        if (around == null) listOf() else lines,
        boundsOf(nearestCourse + nearestLine),
      )
    }

    /**
     * The route of [session] for one navigation along [wayAround], with temporary knots on it: the
     * middle knot of each of [stretches], or all their knots where that is not enough, see
     * `knot-navigation` "Riding around a closed ferry". Its legs leave out the [avoided] ferries
     * too, as a knot alone does not keep a leg from going back over the ferry. `null` when it still
     * takes a closed ferry.
     */
    private fun through(
      session: PlanningSession,
      wayAround: Route,
      stretches: List<IntRange>,
      closedAt: (List<Knot>) -> Map<Pair<Long, Long>, String>,
      avoided: Set<Pair<Long, Long>>,
    ): PlanningSession? {
      val knots = complete(wayAround) ?: return null
      val wayLength = lengthOf(wayAround)
      // Where the waypoints are on the way around: legs share their end and start knot.
      val waypointPositions = HashSet<Int>()
      var position = 0
      waypointPositions += 0
      for (leg in wayAround.legs) {
        position += leg.path!!.knots.size - 1
        waypointPositions += position
      }
      val inner = stretches.map { (it.first + 1 until it.last).toList() }.filter { it.isNotEmpty() }
      fun attempt(chosen: Set<Int>): PlanningSession? {
        val newWaypoints =
          knots.indices.filter { it in waypointPositions || it in chosen }.map { knots[it] }
        val candidate = session.through(newWaypoints, avoided)
        val candidateKnots = complete(candidate.route) ?: return null
        val takesAvoided =
          candidateKnots.zipWithNext().any { (from, to) -> pairOf(from.id, to.id) in avoided }
        val fits =
          !takesAvoided &&
            closedAt(candidateKnots).isEmpty() &&
            lengthOf(candidate.route) <= wayLength * (1 + LONGER_SHARE)
        return candidate.takeIf { fits }
      }
      return attempt(inner.mapTo(HashSet()) { it[it.size / 2] }) ?: attempt(inner.flatten().toSet())
    }

    private fun complete(route: Route?): List<Knot>? =
      (route?.summary as? RouteSummary.Complete)?.knots

    private fun lengthOf(route: Route?): Int =
      (route?.summary as? RouteSummary.Complete)?.lengthMeters ?: 0

    /**
     * The ferries on [knots] closed when the rider gets there from [position], at the usual pace,
     * with their warnings, by their knot pair.
     */
    private fun closedFerries(
      graph: NetworkGraph,
      knots: List<Knot>,
      position: GeoPoint,
      nowMillis: Long,
      localTime: (Long) -> LocalMoment,
      language: Language,
    ): Map<Pair<Long, Long>, String> {
      val route = RouteProgress.of(knots, graph, null, setOf(), knots.map(::pointOf))
      val pace = route.assumedMetersPerSecond
      // The way to the start counts too, straight, as navigation leads there first.
      val toStart = distanceMeters(position, pointOf(knots.first()))
      val startMillis = nowMillis + (toStart / pace * 1000).toLong()
      val (watch, notices) =
        FerryWatch.start(route, graph, localTime, 0.0, 0, startMillis, pace, language)
          ?: return mapOf()
      val closed =
        knots.indices
          .drop(1)
          .map { it - 1 }
          .filter { watch.arrivalAt(it)?.state == FerryArrivalState.WARNING }
      // The warnings come nearer ferry first, as the closed positions.
      return closed.withIndex().associate { (index, position) ->
        pairOf(knots[position].id, knots[position + 1].id) to (notices.getOrNull(index)?.text ?: "")
      }
    }

    /**
     * The stretches of [around] that differ from [planned], as positions in [around], each from the
     * last knot both share before it to the first both share after it.
     */
    private fun differingStretches(planned: List<Knot>, around: List<Knot>): List<IntRange> {
      val stretches = mutableListOf<IntRange>()
      var plannedIndex = 0
      var aroundIndex = 0
      while (aroundIndex < around.size && plannedIndex < planned.size) {
        if (around[aroundIndex].id == planned[plannedIndex].id) {
          aroundIndex++
          plannedIndex++
          continue
        }
        val start = (aroundIndex - 1).coerceAtLeast(0)
        // The first knot further on that the planned route reaches again after where they parted.
        var rejoin = aroundIndex
        var plannedRejoin = -1
        while (rejoin < around.size) {
          val id = around[rejoin].id
          val found = (plannedIndex until planned.size).firstOrNull { planned[it].id == id }
          if (found != null) {
            plannedRejoin = found
            break
          }
          rejoin++
        }
        if (plannedRejoin < 0) {
          stretches += start until around.size
          break
        }
        stretches += start..rejoin
        aroundIndex = rejoin
        plannedIndex = plannedRejoin
      }
      return stretches.filter { it.last > it.first }
    }

    private fun boundsOf(points: List<GeoPoint>): GeoBounds =
      GeoBounds(
        points.minOf { it.latitude },
        points.minOf { it.longitude },
        points.maxOf { it.latitude },
        points.maxOf { it.longitude },
      )

    private fun pointOf(knot: Knot) = GeoPoint(knot.latitude, knot.longitude)
  }
}
