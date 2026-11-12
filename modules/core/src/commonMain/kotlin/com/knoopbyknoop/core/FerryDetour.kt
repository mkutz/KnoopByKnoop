package com.knoopbyknoop.core

/**
 * The question on Navigate about one ferry closed by the time the rider gets there, see
 * `knot-navigation` "Riding around a closed ferry" and "Detour on the map". With several closed
 * ferries there is one question per ferry, in riding order.
 *
 * @property ferry the ferry asked about, to keep it when the rider keeps it
 * @property number which question this is, from 1
 * @property count how many closed ferries there are to ask about, counting those asked before
 * @property warnings the ferry warning, then those of ferries closed on its way around, as
 *   navigation would say them
 * @property around the route for this navigation through temporary knots on the way around the
 *   ferry, `null` when there is no way around; the plan itself stays as it is
 * @property extraDistance how much longer the way around is, e.g. `6.3 km`, `null` without one
 * @property extraTime how much longer it takes at the usual pace, e.g. `24 min`
 * @property lines the way around, each stretch where the new route parts from the current one until
 *   it joins it again, in riding order; empty without a way around
 * @property bounds what the map shows while asking: the ferry and the way around it
 */
class FerryDetour(
  val ferry: Ferry,
  val number: Int,
  val count: Int,
  val warnings: List<String>,
  val around: PlanningSession?,
  val extraDistance: String?,
  val extraTime: String?,
  val lines: List<List<GeoPoint>>,
  val bounds: GeoBounds,
) {
  /** A ferry closed when the rider gets there: one crossing of it, the ferry and its warning. */
  private class Closed(val pair: Pair<Long, Long>, val ferry: Ferry, val warning: String)

  companion object {

    /** At most this many rounds of leaving out ferries closed on the way around. */
    private const val ROUNDS = 5

    /** How much longer than the way around the route through temporary knots may be. */
    private const val LONGER_SHARE = 0.01

    /**
     * The question about the first ferry on the route of [session] that is closed when the rider
     * gets there from [position] at [nowMillis], leaving out the [kept] ones, or `null` when there
     * is none.
     *
     * @param geometry the installed network's courses, for the line; straight lines without them
     * @param localTime the ferries' local time of an instant in epoch milliseconds
     * @param kept the ferries the rider chose to keep in earlier questions
     * @param asked how many questions came before this one
     */
    fun of(
      session: PlanningSession,
      geometry: NetworkGeometry?,
      position: GeoPoint,
      nowMillis: Long,
      localTime: (Long) -> LocalMoment,
      language: Language = Language.EN,
      kept: Set<Ferry> = setOf(),
      asked: Int = 0,
    ): FerryDetour? {
      val current = complete(session.route) ?: return null
      val graph = session.routeGraph
      val closedAt = { knots: List<Knot> ->
        closedFerries(graph, knots, position, nowMillis, localTime, language).filter {
          it.ferry !in kept
        }
      }
      // Each ferry once, also one with several crossings, see "A ferry with two crossings".
      val closedOnRoute = closedAt(current)
      val crossedBefore = closedOnRoute.groupingBy { it.ferry }.eachCount()
      val onRoute = closedOnRoute.distinctBy { it.ferry }
      val target = onRoute.firstOrNull() ?: return null
      val crossings = { ferry: Ferry ->
        graph.ferries
          .filterValues { it == ferry }
          .keys
          .mapTo(HashSet()) { pairOf(it.fromId, it.toId) }
      }
      val warnings = mutableListOf(target.warning)
      val leftOut = mutableSetOf(target.ferry)
      // The way around leaves out the ferry, and closed ferries the way around adds, also one that
      // is further along the route too, which then gets no question of its own.
      var avoided: Set<Pair<Long, Long>> = crossings(target.ferry)
      var aroundRoute: Route? = session.routeAvoiding(avoided)
      repeat(ROUNDS) {
        val knots = complete(aroundRoute) ?: return@repeat
        // Only ferries the way around adds: crossed more often than on the current route.
        val crossedThere = closedAt(knots).groupingBy { it.ferry }.eachCount()
        val more =
          closedAt(knots)
            .distinctBy { it.ferry }
            .filter {
              it.ferry !in leftOut &&
                crossedThere.getValue(it.ferry) > (crossedBefore[it.ferry] ?: 0)
            }
        if (more.isEmpty()) return@repeat
        warnings += more.map { it.warning }
        leftOut += more.map { it.ferry }
        avoided = avoided + more.flatMap { crossings(it.ferry) }
        aroundRoute = session.routeAvoiding(avoided)
      }
      val wayAround = aroundRoute?.takeIf { complete(it) != null }
      val aroundKnots = complete(wayAround)
      val stretches = aroundKnots?.let { differingStretches(current, it) }.orEmpty()
      val around = wayAround?.let { through(session, it, stretches, avoided) }
      val network = geometry?.withRoutePoints(graph)
      val course =
        current
          .zipWithNext()
          .firstOrNull { (from, to) -> pairOf(from.id, to.id) == target.pair }
          ?.let { (from, to) -> network?.courseOf(from, to) ?: listOf(pointOf(from), pointOf(to)) }
          .orEmpty()
      val stretchKnots = stretches.map { aroundKnots!!.subList(it.first, it.last + 1) }
      val lines = stretchKnots.map { stretch ->
        stretch.zipWithNext().flatMap { (a, b) ->
          network?.courseOf(a, b) ?: listOf(pointOf(a), pointOf(b))
        }
      }
      val ownLine =
        stretchKnots.indices
          .firstOrNull { index ->
            val ids = stretchKnots[index].map { it.id }.toSet()
            target.pair.first in ids || target.pair.second in ids
          }
          ?.let { lines[it] } ?: lines.firstOrNull().orEmpty()
      val pace = RouteProgress.assumedMetersPerSecond(session.network)
      val extraMeters =
        around?.let { lengthOf(it.route) - lengthOf(session.route) }?.coerceAtLeast(0)
      return FerryDetour(
        target.ferry,
        asked + 1,
        // Ferries further along that this way around leaves out too get no question of their own.
        asked + 1 + onRoute.drop(1).count { it.ferry !in leftOut },
        warnings,
        around,
        extraMeters?.let { MapFeatures.formatDistance(it, language) },
        extraMeters?.let { RouteProgress.formatDuration(it / pace) },
        if (around == null) listOf() else lines,
        boundsOf(course + ownLine),
      )
    }

    /**
     * The route of [session] for one navigation along [wayAround], with temporary knots on it: the
     * middle knot of each of [stretches], or all their knots where that is not enough, see
     * `knot-navigation` "Riding around a closed ferry". Its legs leave out the [avoided] ferries
     * too, as a knot alone does not keep a leg from going back over the ferry. `null` when it still
     * takes one of them.
     */
    private fun through(
      session: PlanningSession,
      wayAround: Route,
      stretches: List<IntRange>,
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
        val fits = !takesAvoided && lengthOf(candidate.route) <= wayLength * (1 + LONGER_SHARE)
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
     * with their warnings, in riding order.
     */
    private fun closedFerries(
      graph: NetworkGraph,
      knots: List<Knot>,
      position: GeoPoint,
      nowMillis: Long,
      localTime: (Long) -> LocalMoment,
      language: Language,
    ): List<Closed> {
      val route = RouteProgress.of(knots, graph, null, setOf(), knots.map(::pointOf))
      val pace = route.assumedMetersPerSecond
      // The way to the start counts too, straight, as navigation leads there first.
      val toStart = distanceMeters(position, pointOf(knots.first()))
      val startMillis = nowMillis + (toStart / pace * 1000).toLong()
      val (watch, notices) =
        FerryWatch.start(route, graph, localTime, 0.0, 0, startMillis, pace, language)
          ?: return listOf()
      val closed =
        knots.indices
          .drop(1)
          .map { it - 1 }
          .filter { watch.arrivalAt(it)?.state == FerryArrivalState.WARNING }
      // The warnings come nearer ferry first, as the closed positions.
      return closed.mapIndexedNotNull { index, at ->
        val ferry = graph.ferryBetween(knots[at], knots[at + 1]) ?: return@mapIndexedNotNull null
        Closed(pairOf(knots[at].id, knots[at + 1].id), ferry, notices.getOrNull(index)?.text ?: "")
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
