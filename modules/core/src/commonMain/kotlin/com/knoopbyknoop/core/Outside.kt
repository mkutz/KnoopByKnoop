package com.knoopbyknoop.core

import kotlin.math.roundToInt

/**
 * What navigation shows outside the app, the same in every language, see `knot-navigation`
 * "Navigation with the screen off".
 *
 * @property title the Android notification's title of numbers, arrow characters and distances, e.g.
 *   `45 → 12` or `83 · 1.2 km › 30`
 * @property line the notification's second line: the distance close to a knot and the end arrival
 *   after ■, e.g. `150 m · ■ 19:05`
 * @property chip the status chip's text, at most 7 characters, e.g. `→12`
 * @property inverted whether the iPhone's card takes the opposite of its usual appearance, off
 *   route and riding the wrong way
 */
data class OutsideView(
  val title: String,
  val line: String,
  val chip: String,
  val inverted: Boolean,
)

/** This row outside the app, with the [endArrival], e.g. `19:05`, if known. */
fun HeaderRow.outside(endArrival: String?): OutsideView {
  val end = endArrival?.let { "■ $it" }
  return when (this) {
    is HeaderRow.Close ->
      OutsideView(
        "${next.label} → ${turn?.let(::turnCharacter).orEmpty()}${follow.label}",
        listOfNotNull(distance, end).joinToString(" · "),
        "→${follow.label}",
        inverted = false,
      )
    is HeaderRow.Far ->
      OutsideView(
        "${next.label} · $distance › ${follow.label}",
        end.orEmpty(),
        next.label,
        inverted = false,
      )
    is HeaderRow.End ->
      OutsideView("${next.label} · $distance", end.orEmpty(), "■${next.label}", inverted = false)
    is HeaderRow.WrongWay ->
      OutsideView(
        "↩ ${next.label} · $distance",
        end.orEmpty(),
        "↩${next.label}",
        inverted = true,
      )
    is HeaderRow.ToRoute -> {
      val after = next?.let { " › ${it.label}" }.orEmpty()
      val guided = guided
      if (guided == null) {
        val arrow = directionCharacter(arrowDegrees)
        OutsideView("$arrow $distance$after", end.orEmpty(), "$arrow$distance", offRoute)
      } else {
        val turn = turnCharacter(guided.turn)
        val title =
          listOfNotNull(
              listOfNotNull(turn, guided.turnDistance).joinToString(" "),
              guided.remaining,
            )
            .joinToString(" · ")
        OutsideView("$title$after", end.orEmpty(), "$turn${guided.remaining}", offRoute)
      }
    }
  }
}

/** A knot's number, or a flag for a route point, which has none. */
private val HeaderBadge.label: String
  get() = ref.ifEmpty { "⚑" }

private val DIRECTION_CHARACTERS = listOf("↑", "↗", "→", "↘", "↓", "↙", "←", "↖")

/** The arrow character nearest to [degrees] clockwise from straight ahead, in steps of 45°. */
internal fun directionCharacter(degrees: Double): String =
  DIRECTION_CHARACTERS[(normalized(degrees) / 45).roundToInt() % DIRECTION_CHARACTERS.size]

/** The arrow character of a turn on the guided way; following the way is straight on. */
internal fun turnCharacter(turn: TurnDirection?): String =
  when (turn) {
    null,
    TurnDirection.STRAIGHT -> "↑"
    TurnDirection.SLIGHT_LEFT -> "↖"
    TurnDirection.LEFT -> "↰"
    TurnDirection.SHARP_LEFT -> "↙"
    TurnDirection.SLIGHT_RIGHT -> "↗"
    TurnDirection.RIGHT -> "↱"
    TurnDirection.SHARP_RIGHT -> "↘"
    TurnDirection.BACK -> "↩"
  }
