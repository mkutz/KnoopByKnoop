package com.knoopbyknoop.core

/**
 * A few knots of a route around one of them, for a preview with badges, see `map-planning`
 * "Changing what a tapped knot became".
 *
 * @property knots up to three knots of the route with their roles, in riding order
 * @property highlighted the position of the knot the preview is about in [knots]
 * @property moreBefore whether the route has knots before [knots]
 * @property moreAfter whether the route has knots after [knots]
 */
data class KnotPreview(
  val knots: List<RouteKnot>,
  val highlighted: Int,
  val moreBefore: Boolean,
  val moreAfter: Boolean,
) {
  companion object {

    /** At most this many knots in a preview. */
    const val SIZE = 3

    /** The preview of [knot] in the route of [session], or `null` if the route does not have it. */
    fun of(session: PlanningSession, knot: Knot): KnotPreview? {
      val knots = MapFeatures.routeKnots(session)
      // The waypoint, not a place where the route only passes the knot.
      val index =
        knots
          .indexOfFirst { it.knot.id == knot.id && it.role != KnotRole.PASSED }
          .takeIf { it >= 0 }
          ?: knots.indexOfFirst { it.knot.id == knot.id }.takeIf { it >= 0 }
          ?: return null
      val from = (index - 1).coerceAtMost(knots.size - SIZE).coerceAtLeast(0)
      val to = (from + SIZE).coerceAtMost(knots.size)
      return KnotPreview(knots.subList(from, to), index - from, from > 0, to < knots.size)
    }
  }
}

/**
 * What a tapped knot became in a route, and the three roles it can take in a fixed order, see
 * `map-planning` "Changing what a tapped knot became"; or the same for a point not yet added, see
 * "Choose a route point on the map".
 *
 * @property role what the knot became, or for a point not yet added where it would fit best
 * @property choices the start, the stop and the end, in this order; after a tap, the one of [role]
 *   is [TapChoice.current]
 * @property suggested for a point not yet added, the role where it adds the least distance; `null`
 *   after a tap
 */
class TapOptions(
  val knot: Knot,
  val role: WaypointRole,
  val choices: List<TapChoice>,
  val suggested: WaypointRole? = null,
) {
  companion object {

    /**
     * The options after [knot] was tapped on the route of [before] and gave [after], or `null` when
     * the tap added nothing to an existing route.
     */
    fun of(before: PlanningSession, after: PlanningSession, knot: Knot): TapOptions? {
      if (before.route == null || after === before) return null
      val position =
        after.waypoints.indexOfFirst { it.id == knot.id }.takeIf { it >= 0 } ?: return null
      val role = after.roleOf(position)
      return TapOptions(knot, role, choices(before, after, knot, role))
    }

    /**
     * The options for [point], a route point not yet in the route of [session], with the role where
     * it adds the least distance as the suggestion, or `null` without a route.
     */
    fun forPoint(session: PlanningSession, point: Knot): TapOptions? {
      if (session.route == null) return null
      val placed = session.placed(point)
      val position =
        placed.waypoints.indexOfFirst { it.id == point.id }.takeIf { it >= 0 } ?: return null
      val suggested = placed.roleOf(position)
      return TapOptions(point, suggested, choices(session, null, point, null), suggested)
    }

    /**
     * The start, the stop and the end for [knot] on the route of [before]; the one of [current] is
     * [after].
     */
    private fun choices(
      before: PlanningSession,
      after: PlanningSession?,
      knot: Knot,
      current: WaypointRole?,
    ): List<TapChoice> {
      val start = before.waypoints.first()
      val end = before.waypoints.last()
      return listOf(WaypointRole.START, WaypointRole.STOP, WaypointRole.END).mapNotNull { choice ->
        val isCurrent = choice == current
        val session =
          when {
            isCurrent && after != null -> after
            choice == WaypointRole.START -> before.startAt(knot)
            choice == WaypointRole.END -> before.endAt(knot)
            else -> before.stopAt(knot)
          }
        // Start and end show only what changes: the knot next to the start or end it takes over,
        // which becomes a stop.
        val preview =
          when (choice) {
            WaypointRole.START ->
              KnotPreview(
                listOf(RouteKnot(knot, KnotRole.START), RouteKnot(start, KnotRole.STOP)),
                0,
                moreBefore = false,
                moreAfter = false,
              )
            WaypointRole.END ->
              KnotPreview(
                listOf(RouteKnot(end, KnotRole.STOP), RouteKnot(knot, KnotRole.END)),
                1,
                moreBefore = false,
                moreAfter = false,
              )
            WaypointRole.STOP -> KnotPreview.of(session, knot) ?: return@mapNotNull null
          }
        TapChoice(choice, session, preview, isCurrent)
      }
    }
  }
}

/**
 * One role a knot can take, with the route it gives and its preview; [current] for the role a
 * tapped knot got.
 */
class TapChoice(
  val role: WaypointRole,
  val session: PlanningSession,
  val preview: KnotPreview,
  val current: Boolean,
)
