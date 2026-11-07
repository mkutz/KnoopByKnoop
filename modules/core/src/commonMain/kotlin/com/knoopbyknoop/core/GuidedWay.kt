package com.knoopbyknoop.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * What the rider sees on a guided way, see `knot-navigation` "Guided way in the header and outside
 * the app".
 *
 * @property turn the next turn and how far it is, e.g. `Left in 120 m`
 * @property street the way it turns into, if known
 * @property remaining the distance left along the way, e.g. `1.9 km`
 */
data class GuidedWayView(val turn: String, val street: String?, val remaining: String) {

  /** The line outside the app, e.g. `Left in 120 m · 1.9 km`. */
  val line: String
    get() = "$turn · $remaining"

  /** What screen readers say after the next knot, e.g. `left in 120 m, Weierstraße, 1.9 km`. */
  val description: String
    get() = listOfNotNull(turn, street, remaining).joinToString(", ")
}

/** The result of a fix on a [GuidedWay]. */
data class GuidedWayUpdate(val way: GuidedWay, val notice: Notice?, val left: Boolean)

/**
 * Following a [PathWay] back to the route, see `knot-navigation` "Guided way back to the route".
 * Immutable: every fix returns a new one.
 *
 * @property place how far along the way the rider is
 */
class GuidedWay
private constructor(
  val way: PathWay,
  val place: Double,
  private val announced: Int,
  private val awayFixes: Int,
  /** How far the rider is from the way, when that is more than the inaccuracy of a fix. */
  private val awayMeters: Double,
  private val rider: GeoPoint?,
  private val turnBackTold: Boolean,
) {

  /** The rider still has to turn back, see `knot-navigation` "Way behind the rider". */
  private val turningBack: Boolean
    get() = way.startsBack && place < FOLLOWING_METERS

  /** How far it is along the way to its end, with the way there. */
  val remainingMeters: Double
    get() = way.lengthMeters - place + awayMeters

  /** Where the rider is on the way and what to tell them, after [fix]. */
  fun update(fix: Fix, language: Language, speechLanguage: Language): GuidedWayUpdate {
    if (fix.accuracyMeters > Navigation.PRECISE_ACCURACY_METERS) {
      return GuidedWayUpdate(this, null, left = false)
    }
    val (nearestPlace, distance) = nearest(fix)
    val position = GeoPoint(fix.latitude, fix.longitude)
    val away = if (distance > ON_WAY_METERS) distance else 0.0
    val turnBack =
      if (turningBack && !turnBackTold) {
        Notice(
          NoticeKind.TURN,
          language.texts.turnNow(TurnDirection.BACK),
          speechLanguage.texts.turnNow(TurnDirection.BACK),
        )
      } else null
    val told = turnBackTold || turnBack != null
    if (distance > AWAY_METERS) {
      val awayFixCount = awayFixes + 1
      return GuidedWayUpdate(
        GuidedWay(way, place, announced, awayFixCount, away, position, told),
        turnBack,
        left = awayFixCount >= AWAY_FIXES,
      )
    }
    val newPlace = max(place, nearestPlace)
    val index = way.turns.indexOfFirst { it.atMeters > newPlace }
    val turn = way.turns.getOrNull(index)
    var newAnnounced = announced
    var notice = turnBack
    if (
      notice == null &&
        turn != null &&
        index >= announced &&
        turn.atMeters - newPlace <= NOTICE_METERS
    ) {
      val meters = turn.atMeters - newPlace
      notice =
        Notice(
          NoticeKind.TURN,
          turnNotice(turn, meters, language),
          spokenTurnNotice(turn, meters, speechLanguage),
        )
      newAnnounced = index + 1
    }
    return GuidedWayUpdate(
      GuidedWay(way, newPlace, newAnnounced, 0, away, position, told),
      notice,
      left = false,
    )
  }

  /** What the rider sees now. */
  fun view(language: Language): GuidedWayView {
    val texts = language.texts
    val remaining = MapFeatures.formatDistance(rounded(remainingMeters), language)
    if (turningBack) return GuidedWayView(texts.turnName(TurnDirection.BACK), null, remaining)
    val turn = way.turns.firstOrNull { it.atMeters > place }
    return if (turn == null) {
      GuidedWayView(texts.followTheWay, null, remaining)
    } else {
      val distance =
        MapFeatures.formatDistance(rounded(turn.atMeters - place + awayMeters), language)
      val name =
        if (turn.switchTo != null && turn.direction == TurnDirection.STRAIGHT) {
          texts.switchName(turn.switchTo)
        } else texts.turnName(turn.direction)
      GuidedWayView(texts.turnIn(name, distance), turn.name, remaining)
    }
  }

  /**
   * The next turn for the header, see `knot-navigation` "Guided way in the header and outside the
   * app".
   */
  fun headerTurn(language: Language): GuidedHeader {
    val view = view(language)
    val remaining = view.remaining
    if (turningBack)
      return GuidedHeader(TurnDirection.BACK, null, null, remaining, view.description)
    val turn = way.turns.firstOrNull { it.atMeters > place }
    return if (turn == null) {
      GuidedHeader(null, null, null, remaining, view.description)
    } else {
      GuidedHeader(
        if (turn.switchTo != null && turn.direction == TurnDirection.STRAIGHT)
          TurnDirection.STRAIGHT
        else turn.direction,
        MapFeatures.formatDistance(rounded(turn.atMeters - place + awayMeters), language),
        turn.name,
        remaining,
        view.description,
      )
    }
  }

  /** The way ahead of the rider, starting at [place], for the map. */
  fun pointsAhead(): List<GeoPoint> {
    val points = pointsAheadOnWay()
    return if (awayMeters > 0.0 && rider != null) listOf(rider) + points else points
  }

  private fun pointsAheadOnWay(): List<GeoPoint> {
    val index = way.meters.indexOfFirst { it > place }
    if (index <= 0) return way.points
    val start = way.points[index - 1]
    val end = way.points[index]
    val length = way.meters[index] - way.meters[index - 1]
    val along = if (length == 0.0) 0.0 else (place - way.meters[index - 1]) / length
    val here =
      GeoPoint(
        start.latitude + (end.latitude - start.latitude) * along,
        start.longitude + (end.longitude - start.longitude) * along,
      )
    return listOf(here) + way.points.subList(index, way.points.size)
  }

  /**
   * The place on the way nearest to [fix], looking a little back and further ahead, and how far.
   */
  private fun nearest(fix: Fix): Pair<Double, Double> {
    val position = GeoPoint(fix.latitude, fix.longitude)
    var bestPlace = place
    var bestDistance = Double.MAX_VALUE
    for (index in 0 until way.points.size - 1) {
      val startMeters = way.meters[index]
      val endMeters = way.meters[index + 1]
      if (endMeters < place - LOOK_BACK_METERS) continue
      if (startMeters > place + LOOK_AHEAD_METERS) break
      val point = nearestOnLine(position, way.points[index], way.points[index + 1])
      val distance = distanceMeters(fix.latitude, fix.longitude, point.latitude, point.longitude)
      if (distance < bestDistance) {
        bestDistance = distance
        val along =
          distanceMeters(
            way.points[index].latitude,
            way.points[index].longitude,
            point.latitude,
            point.longitude,
          )
        bestPlace = min(startMeters + along, endMeters)
      }
    }
    return bestPlace to bestDistance
  }

  private fun turnNotice(turn: PathTurn, meters: Double, language: Language): String =
    notice(turn, meters, language.texts, MapFeatures.formatDistance(rounded(meters), language))

  /** As [turnNotice], with the distance in words, as the voice would read `m` as a letter. */
  private fun spokenTurnNotice(turn: PathTurn, meters: Double, language: Language): String =
    notice(turn, meters, language.texts, language.texts.spokenDistanceAhead(rounded(meters)))

  private fun notice(turn: PathTurn, meters: Double, texts: Texts, distance: String): String {
    val now = meters <= NOW_METERS
    val switchTo = turn.switchTo
    if (switchTo != null && turn.direction == TurnDirection.STRAIGHT) {
      return if (now) texts.switchNow(switchTo) else texts.switchAhead(switchTo, distance)
    }
    val notice =
      if (now) texts.turnNow(turn.direction) else texts.turnAhead(turn.direction, distance)
    return if (switchTo != null) texts.onto(notice, switchTo) else notice
  }

  private fun rounded(meters: Double): Int =
    (meters / ROUNDING_METERS).roundToInt() * ROUNDING_METERS

  companion object {

    /** Leaving the way is more than this far from it, see "Leaving the guided way". */
    private const val AWAY_METERS = 30.0
    private const val AWAY_FIXES = 2

    /** Closer to the way than this, the rider is on it, as far as a fix can tell. */
    private const val ON_WAY_METERS = 15.0

    /** Having followed the way this far, the rider has turned back, see "Way behind the rider". */
    private const val FOLLOWING_METERS = 15.0

    /** See `knot-navigation` "Turn notices on the guided way". */
    private const val NOTICE_METERS = 100.0
    private const val NOW_METERS = 40.0
    private const val ROUNDING_METERS = 10
    private const val LOOK_BACK_METERS = 50.0
    private const val LOOK_AHEAD_METERS = 300.0

    /**
     * Follows [way], found while on [previous] if any: turning back is only said once while the
     * rider keeps riding away, see "Still riding away".
     */
    fun of(way: PathWay, previous: GuidedWay? = null) =
      GuidedWay(
        way,
        0.0,
        0,
        0,
        0.0,
        null,
        turnBackTold = way.startsBack && previous?.way?.startsBack == true && previous.turnBackTold,
      )
  }
}
