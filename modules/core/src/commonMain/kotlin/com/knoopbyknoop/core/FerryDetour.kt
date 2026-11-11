package com.knoopbyknoop.core

/**
 * The question on Navigate about ferries closed by the time the rider gets there, see
 * `knot-navigation` "Riding around a closed ferry" and "Detour on the map".
 *
 * @property warnings the ferry warnings, one per closed ferry, as navigation would say them
 * @property around the plan without the closed ferries, `null` when there is no way around
 * @property extraDistance how much longer the way around is, e.g. `6.3 km`, `null` without one
 * @property extraTime how much longer it takes at the usual pace, e.g. `25 min`
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
      val planned = complete(session) ?: return null
      val first = closedFerries(session, planned, position, nowMillis, localTime, language)
      if (first.isEmpty()) return null
      val warnings = first.values.toMutableList()
      val closedInOrder = first.keys.toMutableList()
      var avoided = first.keys
      var around: PlanningSession? = session.avoidingFerries(avoided)
      repeat(ROUNDS) {
        val current = around ?: return@repeat
        val knots = complete(current)
        if (knots == null) {
          around = null
          return@repeat
        }
        val more = closedFerries(current, knots, position, nowMillis, localTime, language)
        if (more.isEmpty()) return@repeat
        warnings += more.values
        closedInOrder += more.keys
        avoided = avoided + more.keys
        around = session.avoidingFerries(avoided)
      }
      val aroundKnots = around?.let(::complete)
      if (aroundKnots == null) around = null
      val network = geometry?.withRoutePoints(around?.routeGraph ?: session.routeGraph)
      // The map shows the nearest closed ferry with the way around it; further ones are drawn too.
      val nearest = closedInOrder.first()
      val nearestCourse =
        planned
          .zipWithNext()
          .firstOrNull { (from, to) -> pairOf(from.id, to.id) == nearest }
          ?.let { (from, to) -> network?.courseOf(from, to) ?: listOf(pointOf(from), pointOf(to)) }
          .orEmpty()
      val stretches = aroundKnots?.let { differingStretches(planned, it) }.orEmpty()
      val lines = stretches.map { stretch ->
        stretch.zipWithNext().flatMap { (a, b) ->
          network?.courseOf(a, b) ?: listOf(pointOf(a), pointOf(b))
        }
      }
      val nearestLine =
        stretches.indices
          .firstOrNull { index ->
            val ids = stretches[index].map { it.id }.toSet()
            nearest.first in ids || nearest.second in ids
          }
          ?.let { lines[it] } ?: lines.firstOrNull().orEmpty()
      val pace = RouteProgress.assumedMetersPerSecond(session.network)
      val extraMeters =
        aroundKnots?.let { lengthOf(around!!) - lengthOf(session) }?.coerceAtLeast(0)
      return FerryDetour(
        warnings.distinct(),
        around,
        extraMeters?.let { MapFeatures.formatDistance(it, language) },
        extraMeters?.let { RouteProgress.formatDuration(it / pace) },
        lines,
        boundsOf(nearestCourse + nearestLine),
      )
    }

    private fun complete(session: PlanningSession): List<Knot>? =
      (session.route?.summary as? RouteSummary.Complete)?.knots

    private fun lengthOf(session: PlanningSession): Int =
      (session.route?.summary as? RouteSummary.Complete)?.lengthMeters ?: 0

    /**
     * The ferries on [knots] closed when the rider gets there from [position], at the usual pace,
     * with their warnings, by their knot pair.
     */
    private fun closedFerries(
      session: PlanningSession,
      knots: List<Knot>,
      position: GeoPoint,
      nowMillis: Long,
      localTime: (Long) -> LocalMoment,
      language: Language,
    ): Map<Pair<Long, Long>, String> {
      val graph = session.routeGraph
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
     * The stretches of [around] that differ from [planned], each from the last knot both share
     * before it to the first both share after it.
     */
    private fun differingStretches(planned: List<Knot>, around: List<Knot>): List<List<Knot>> {
      val stretches = mutableListOf<List<Knot>>()
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
          stretches += around.subList(start, around.size)
          break
        }
        stretches += around.subList(start, rejoin + 1)
        aroundIndex = rejoin
        plannedIndex = plannedRejoin
      }
      return stretches.filter { it.size >= 2 }
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
