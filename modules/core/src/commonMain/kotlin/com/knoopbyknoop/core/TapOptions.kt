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
 * What a tapped knot became in a route and what else it can be, see `map-planning` "Changing what a
 * tapped knot became".
 *
 * @property role what the knot became
 * @property preview where it went
 * @property choices the other roles it can take, each with its route and preview
 */
class TapOptions(
  val knot: Knot,
  val role: WaypointRole,
  val preview: KnotPreview,
  val choices: List<TapChoice>,
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
      val preview = KnotPreview.of(after, knot) ?: return null
      val choices =
        listOf(WaypointRole.START, WaypointRole.END, WaypointRole.STOP)
          .filter { it != role }
          .mapNotNull { other ->
            val session =
              when (other) {
                WaypointRole.START -> before.startAt(knot)
                WaypointRole.END -> before.endAt(knot)
                WaypointRole.STOP -> before.stopAt(knot)
              }
            KnotPreview.of(session, knot)?.let { TapChoice(other, session, it) }
          }
      return TapOptions(knot, role, preview, choices)
    }
  }
}

/** One other role a tapped knot can take, with the route it gives and its preview. */
class TapChoice(val role: WaypointRole, val session: PlanningSession, val preview: KnotPreview)
