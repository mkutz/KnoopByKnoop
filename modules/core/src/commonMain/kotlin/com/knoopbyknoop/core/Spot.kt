package com.knoopbyknoop.core

/**
 * What a sentence names where it would name a knot: a knot by its [ref], or a route point by its
 * [role], see `knot-navigation` "Navigating with route points".
 */
internal data class Spot(val ref: String, val role: WaypointRole?) {

  /** This spot as the voice says it: a knot's number read out, a route point unchanged. */
  fun spoken(): Spot = if (role == null) copy(ref = Navigation.spoken(ref)) else this

  companion object {

    /** The knot at [index] of a route's [knots]; a route point's role follows from its position. */
    fun at(knots: List<Knot>, index: Int): Spot {
      val knot = knots[index]
      if (!knot.isRoutePoint) return Spot(knot.ref, null)
      val role =
        when (index) {
          0 -> WaypointRole.START
          knots.lastIndex -> WaypointRole.END
          else -> WaypointRole.STOP
        }
      return Spot(knot.ref, role)
    }
  }
}

/** [spot] as it fits after e.g. `then`: a knot's number, or a route point with its article. */
internal fun Texts.name(spot: Spot): String = spot.role?.let(::point) ?: spot.ref

internal fun Texts.next(next: Spot, after: Spot?): String =
  next.role?.let { nextPoint(it, after?.let(::name)) } ?: next(next.ref, after?.let(::name))

internal fun Texts.nextOutside(next: Spot, after: Spot): String =
  next.role?.let { nextPoint(it, name(after)) } ?: nextOutside(next.ref, name(after))

internal fun Texts.ahead(next: Spot, after: Spot?): String =
  next.role?.let { aheadPoint(it, after?.let(::name)) } ?: ahead(next.ref, after?.let(::name))

internal fun Texts.offRoute(next: Spot, way: String): String =
  next.role?.let { offRoutePoint(it, way) } ?: offRoute(next.ref, way)

internal fun Texts.guidance(next: Spot, way: String): String =
  next.role?.let { guidancePoint(it, way) } ?: guidance(next.ref, way)

internal fun Texts.backOnRoute(next: Spot): String =
  next.role?.let(::backOnRoutePoint) ?: backOnRoute(next.ref)

internal fun Texts.wrongWay(next: Spot): String =
  next.role?.let(::wrongWayPoint) ?: wrongWay(next.ref)

internal fun Texts.wrongWayNext(next: Spot): String =
  next.role?.let(::wrongWayNextPoint) ?: wrongWayNext(next.ref)

internal fun Texts.offRouteNext(next: Spot): String =
  next.role?.let(::offRouteNextPoint) ?: offRouteNext(next.ref)

internal fun Texts.wayTo(distance: String, direction: String, next: Spot): String =
  next.role?.let { wayToPoint(distance, direction, it) } ?: wayTo(distance, direction, next.ref)

internal fun Texts.toKnot(distance: String, next: Spot, endArrival: String?): String =
  next.role?.let { toPoint(distance, it, endArrival) } ?: toKnot(distance, next.ref, endArrival)

internal fun Texts.ridden(ridden: String, next: Spot): String =
  next.role?.let { riddenPoint(ridden, it) } ?: ridden(ridden, next.ref)

/** A route point has no signs to follow, so `At 45 follow 12` becomes `Next 45, then the stop`. */
internal fun Texts.follow(next: Spot, follow: Spot): String =
  if (next.role == null && follow.role == null) follow(next.ref, follow.ref)
  else nextOutside(next, follow)

internal fun Texts.isTheEnd(next: Spot): String =
  next.role?.let { nextPoint(it, null) } ?: isTheEnd(next.ref)

/** E.g. `Knot 30`, or `Stop` for a route point. */
internal fun Texts.knot(spot: Spot): String =
  when (spot.role) {
    null -> knot(spot.ref)
    WaypointRole.START -> capitalized(start)
    WaypointRole.STOP -> capitalized(stop)
    WaypointRole.END -> capitalized(end)
  }
