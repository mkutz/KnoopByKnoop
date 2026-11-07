package com.knoopbyknoop.core

/**
 * A knot in the navigation header, drawn like its signs, see `knot-navigation` "Number to follow".
 *
 * @property ref the knot's number, empty for a route point, which is drawn with a flag
 * @property plate the plate of the knot's sign region
 * @property filled whether the badge is a filled plate, as at the knot itself, or white with a
 *   ring, as on the Dutch arrow signs pointing to it
 * @property role how the knot takes part in the route, for a picked knot's corner symbol
 * @property onPlate whether the badge sits on a sign plate, as the number to follow close to a knot
 */
data class HeaderBadge(
  val ref: String,
  val plate: KnotPlate,
  val filled: Boolean,
  val role: KnotRole,
  val onPlate: Boolean = false,
)

/**
 * The next turn on a guided way in the header, see `knot-navigation` "Guided way in the header and
 * outside the app".
 *
 * @property turn the turn to take, or `null` to follow the way
 * @property turnDistance the distance to the turn, e.g. `120 m`
 * @property street the name of the way the turn goes into, if known
 * @property remaining the distance left along the way, e.g. `1.9 km`
 * @property description what screen readers say, e.g. `Left in 120 m, Weierstraße, 1.9 km`
 */
data class GuidedHeader(
  val turn: TurnDirection?,
  val turnDistance: String?,
  val street: String?,
  val remaining: String,
  val description: String,
)

/**
 * The row at the top of the panel while navigating, without words, see `knot-navigation` "Number to
 * follow". [spoken] is what screen readers say, in the app's language.
 */
sealed class HeaderRow {
  abstract val spoken: String

  /**
   * Close to [next], with [follow] on its sign plate, whose arrow shows the [turn] the route makes
   * at [next], or a plain → where it cannot be told, see `knot-navigation` "Turn at the next knot".
   */
  data class Close(
    val next: HeaderBadge,
    val distance: String,
    val follow: HeaderBadge,
    override val spoken: String,
    val turn: TurnDirection? = null,
  ) : HeaderRow()

  /** Farther from [next], with [follow] small after it. */
  data class Far(
    val next: HeaderBadge,
    val distance: String,
    val follow: HeaderBadge,
    override val spoken: String,
  ) : HeaderRow()

  /** The next knot is the end. */
  data class End(val next: HeaderBadge, val distance: String, override val spoken: String) :
    HeaderRow()

  /** Riding back along the route, see `knot-navigation` "Riding the wrong way". */
  data class WrongWay(val next: HeaderBadge, val distance: String, override val spoken: String) :
    HeaderRow()

  /**
   * The way back to the route: [offRoute] after the rider was on it, dark; else on the way to it
   * after starting away from it, see "Header on the way to the route".
   *
   * @property arrowDegrees where the place to get back onto the route lies, in degrees clockwise
   *   from the riding direction, or from north when the rider has not moved recently
   * @property distance the straight distance to that place
   * @property next the next knot, only while the rider is on a path of the knot network
   * @property guided the next turn, on a guided way
   */
  data class ToRoute(
    val offRoute: Boolean,
    val arrowDegrees: Double,
    val distance: String,
    val next: HeaderBadge?,
    val guided: GuidedHeader?,
    override val spoken: String,
  ) : HeaderRow()
}

/** [degrees] in 0 until 360. */
internal fun normalized(degrees: Double): Double = ((degrees % 360) + 360) % 360
