package com.knoopbyknoop.core

/**
 * The question on Navigate about ferries closed by the time the rider gets there, see
 * `knot-navigation` "Riding around a closed ferry" and "Detour on the map".
 *
 * @property warnings the ferry warnings, one per closed ferry, as navigation would say them
 * @property around the plan without the closed ferries, `null` when there is no way around
 * @property extraDistance how much longer the way around is, e.g. `6.3 km`, `null` without one
 * @property extraTime how much longer it takes at the usual pace, e.g. `25 min`
 * @property line the way around where it differs from the planned route, empty without one
 * @property bounds what the map shows while asking: the closed ferries and [line]
 */
class FerryDetour(
  val warnings: List<String>,
  val around: PlanningSession?,
  val extraDistance: String?,
  val extraTime: String?,
  val line: List<GeoPoint>,
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
        avoided = avoided + more.keys
        around = session.avoidingFerries(avoided)
      }
      val aroundKnots = around?.let(::complete)
      if (aroundKnots == null) around = null
      val network = geometry?.withRoutePoints(around?.routeGraph ?: session.routeGraph)
      val ferryLines =
        planned
          .zipWithNext()
          .filter { (from, to) -> pairOf(from.id, to.id) in avoided }
          .map { (from, to) ->
            network?.courseOf(from, to) ?: listOf(pointOf(from), pointOf(to))
          }
      val line = aroundKnots?.let { differingLine(planned, it, network) }.orEmpty()
      val pace = RouteProgress.assumedMetersPerSecond(session.network)
      val extraMeters =
        aroundKnots?.let { lengthOf(around!!) - lengthOf(session) }?.coerceAtLeast(0)
      return FerryDetour(
        warnings,
        around,
        extraMeters?.let { MapFeatures.formatDistance(it, language) },
        extraMeters?.let { RouteProgress.formatDuration(it / pace) },
        line,
        boundsOf(ferryLines.flatten() + line),
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
     * The course of [around] from the last knot it shares with [planned] before they part to the
     * first after.
     */
    private fun differingLine(
      planned: List<Knot>,
      around: List<Knot>,
      network: NetworkGeometry?,
    ): List<GeoPoint> {
      val start = planned.zip(around).takeWhile { (a, b) -> a.id == b.id }.size
      val end =
        planned.asReversed().zip(around.asReversed()).takeWhile { (a, b) -> a.id == b.id }.size
      val from = (start - 1).coerceAtLeast(0)
      val to = (around.size - end).coerceAtMost(around.lastIndex)
      if (from >= to) return listOf()
      return around.subList(from, to + 1).zipWithNext().flatMap { (a, b) ->
        network?.courseOf(a, b) ?: listOf(pointOf(a), pointOf(b))
      }
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
