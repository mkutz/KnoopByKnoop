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
 * `map-planning` "Changing what a tapped knot became"; a knot of the network or a route point from
 * a spot or a place.
 *
 * @property role what the knot became
 * @property choices the start, the stop and the end, in this order; the one of [role] is
 *   [TapChoice.current]
 */
class TapOptions(val knot: Knot, val role: WaypointRole, val choices: List<TapChoice>) {

  /**
   * The note after [choice] was used: the same choices with that one selected, see `map-planning`
   * "Changing what a tapped knot became".
   */
  fun chosen(choice: TapChoice): TapOptions =
    TapOptions(
      knot,
      choice.role,
      choices.map { TapChoice(it.role, it.session, it.preview, it.role == choice.role) },
    )

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
     * The start, the stop and the end for [knot] on the route of [before]; the one of [current] is
     * [after].
     */
    private fun choices(
      before: PlanningSession,
      after: PlanningSession,
      knot: Knot,
      current: WaypointRole,
    ): List<TapChoice> {
      val start = before.waypoints.first()
      val end = before.waypoints.last()
      return listOf(WaypointRole.START, WaypointRole.STOP, WaypointRole.END).mapNotNull { choice ->
        val isCurrent = choice == current
        val session =
          when {
            isCurrent -> after
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
