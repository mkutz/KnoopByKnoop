package com.knoopbyknoop.core

/**
 * A language of the apps' texts, see `app-languages` "Language of the texts". The core writes its
 * texts, numbers and dates in it; [EN] wherever none is given.
 */
enum class Language {
  EN,
  DE,
  NL,
  FR;

  internal val texts: Texts
    get() =
      when (this) {
        EN -> EnglishTexts
        DE -> GermanTexts
        NL -> DutchTexts
        FR -> FrenchTexts
      }

  companion object {

    /** The language for a language code such as `de` or `nl-BE`; [EN] for any other. */
    fun of(code: String?): Language =
      when (code?.substringBefore('-')?.substringBefore('_')?.lowercase()) {
        "de" -> DE
        "nl" -> NL
        "fr" -> FR
        else -> EN
      }
  }
}

/** The texts the core writes, in one language. Distances, times and dates come in written. */
internal interface Texts {

  /** `.` in English, `,` in German, Dutch and French, see "Numbers, times and dates". */
  val decimalSeparator: Char

  /** A day of the year, e.g. `1 May`, `1. Mai`, `1 mei` or `1er mai`. */
  fun date(day: Int, month: Int): String

  /** The time a route takes at the assumed pace, e.g. `about 15 min`. */
  fun about(duration: String): String

  // Ferries, see `map-planning` "Ferries on the route".

  /** A ferry without a name. */
  val ferry: String
  val unknownTimes: String
  val seasonal: String

  /** E.g. `Seasonal (summer).` */
  fun seasonalIn(season: String): String

  fun fee(charge: String): String

  val free: String
  val feeCharged: String
  val open: String

  fun openUntil(time: String): String

  val closed: String

  fun closedUntil(time: String): String

  fun closedUntilTomorrow(time: String): String

  /** When the rider gets to a ferry, e.g. `At 19:15`. */
  fun atTime(time: String): String

  /** E.g. `ferry Renkumse Veer` or `ferry`, as part of a warning. */
  fun ferryTitle(name: String?): String

  /** E.g. `The ferry Renkumse Veer closes at 19:00, before you get there around 19:15`. */
  fun ferryWarning(title: String, warning: String): String

  fun closesShortlyAfter(closing: String, around: String): String

  fun closedWhenThere(around: String): String

  fun closesBefore(closing: String, around: String): String

  fun opensAt(time: String): String

  fun opensTomorrowAt(time: String): String

  fun opensOn(date: String, time: String): String

  // Navigation, see `knot-navigation`.

  /** E.g. `Next 45, then 12`, or `Next 45` before the end. */
  fun next(next: String, after: String?): String

  /** The short form of [next] for the one line outside the app, e.g. `Next 45, then 12`. */
  fun nextOutside(next: String, after: String): String

  /**
   * For screen readers, e.g. `Next 83, 1.2 km, then 30`, see `knot-navigation` "Number to follow".
   */
  fun nextAt(next: String, distance: String, after: String): String

  /** E.g. `45 ahead, then 12`, or `45 ahead, the end`. */
  fun ahead(next: String, after: String?): String

  fun offRoute(next: String, way: String): String

  /** Off route, the guidance, e.g. `Next 45, 1.7 km to the north-east`. */
  fun guidance(next: String, way: String): String

  fun backOnRoute(next: String): String

  /** E.g. `Wrong direction, turn around, next 45`, see `knot-navigation` "Riding the wrong way". */
  fun wrongWay(next: String): String

  /** The short form of [wrongWay] for the panel and outside the app. */
  fun wrongWayNext(next: String): String

  val arrived: String

  /** The eight compass directions from north, clockwise. */
  val compassDirections: List<String>

  /** E.g. `1.7 km to the north-east`. */
  fun way(distance: String, direction: String): String

  /** Off route, the line outside the app, e.g. `1.7 km north-east to 45`. */
  fun wayTo(distance: String, direction: String, next: String): String

  /** E.g. `At 45 follow 12`. */
  fun follow(next: String, follow: String): String

  /** E.g. `31 is the end`. */
  fun isTheEnd(next: String): String

  /** E.g. `Off route, next 45`. */
  fun offRouteNext(next: String): String

  // The guided way, see `knot-navigation` "Turn notices on the guided way".

  /** A distance as the voice says it, as it would read `m` as a letter, e.g. `528 meters`. */
  fun spokenDistance(meters: Int): String

  /** A distance after "in", e.g. `100 meters`, in German `100 Metern`. */
  fun spokenDistanceAhead(meters: Int): String

  /** On a guided way with no turn left. */
  val followTheWay: String

  /** The turn on its own, e.g. `Left`. */
  fun turnName(direction: TurnDirection): String

  /** E.g. `Left in 120 m`. */
  fun turnIn(turn: String, distance: String): String

  /** E.g. `In 100 m, turn left`. */
  fun turnAhead(direction: TurnDirection, distance: String): String

  /** E.g. `Turn left now`. */
  fun turnNow(direction: TurnDirection): String

  /** A switch without a turn on its own, e.g. `To the cycle path`. */
  fun switchName(switchTo: PathSwitch): String

  /** E.g. `In 100 m, switch to the cycle path`. */
  fun switchAhead(switchTo: PathSwitch, distance: String): String

  /** E.g. `Switch to the cycle path now`. */
  fun switchNow(switchTo: PathSwitch): String

  /** A turn [notice] with the switch, e.g. `In 100 m, turn right, onto the cycle path`. */
  fun onto(notice: String, switchTo: PathSwitch): String

  // Progress, see `knot-navigation` "Progress strip" and "Coming knots".

  /** E.g. `0.8 km to 45 · end around 19:05`. */
  fun toKnot(distance: String, next: String, endArrival: String?): String

  /** E.g. `0.8 km to the end, around 19:05`. */
  fun toTheEnd(distance: String, arrival: String?): String

  /** E.g. `4.0 of 12.4 km ridden, next knot 12`. */
  fun ridden(ridden: String, next: String): String

  /** E.g. `4.0 of 12.4 km` or `300 of 800 m`. */
  fun of(done: String, total: String, unit: String): String

  val start: String
  val stop: String
  val end: String

  /** E.g. `Knot 30`. */
  fun knot(ref: String): String

  /** E.g. `Ferry 12 → 31`. */
  fun ferryBetween(legs: String): String

  // Route points, see `knot-navigation` "Navigating with route points": where a sentence names a
  // knot by its number, a route point is named by its role.

  /** A route point with its article, e.g. `the stop`, e.g. after `then`. */
  fun point(role: WaypointRole): String

  /** E.g. `On to the stop, then 12`, or `On to the end`. */
  fun nextPoint(role: WaypointRole, after: String?): String

  /** E.g. `The stop ahead, then 12`, or `The end ahead`. */
  fun aheadPoint(role: WaypointRole, after: String?): String

  /** E.g. `Off route, on to the end, 300 m to the north`. */
  fun offRoutePoint(role: WaypointRole, way: String): String

  /** E.g. `On to the end, 300 m to the north`. */
  fun guidancePoint(role: WaypointRole, way: String): String

  /** E.g. `Back on route, on to the end`. */
  fun backOnRoutePoint(role: WaypointRole): String

  /** E.g. `Wrong direction, turn around, on to the end`. */
  fun wrongWayPoint(role: WaypointRole): String

  /** E.g. `Wrong direction, on to the end`. */
  fun wrongWayNextPoint(role: WaypointRole): String

  /** E.g. `Off route, on to the end`. */
  fun offRouteNextPoint(role: WaypointRole): String

  /** E.g. `300 m north to the end`. */
  fun wayToPoint(distance: String, direction: String, role: WaypointRole): String

  /** E.g. `0.8 km to the stop · end around 19:05`. */
  fun toPoint(distance: String, role: WaypointRole, endArrival: String?): String

  /** E.g. `4.0 of 12.4 km ridden, on to the stop`. */
  fun riddenPoint(ridden: String, role: WaypointRole): String
}

internal object EnglishTexts : Texts {
  override val decimalSeparator = '.'
  private val months =
    listOf(
      "January",
      "February",
      "March",
      "April",
      "May",
      "June",
      "July",
      "August",
      "September",
      "October",
      "November",
      "December",
    )

  override fun date(day: Int, month: Int) = "$day ${months[month - 1]}"

  override fun about(duration: String) = "about $duration"

  override val ferry = "Ferry"
  override val unknownTimes = "Operating times not known, check before you go"
  override val seasonal = "Seasonal."

  override fun seasonalIn(season: String) = "Seasonal ($season)."

  override fun fee(charge: String) = "Fee: $charge"

  override val free = "Free"
  override val feeCharged = "Fee charged"
  override val open = "Open"

  override fun openUntil(time: String) = "Open until $time"

  override val closed = "Closed"

  override fun closedUntil(time: String) = "Closed until $time"

  override fun closedUntilTomorrow(time: String) = "Closed until tomorrow $time"

  override fun atTime(time: String) = "At $time"

  override fun ferryTitle(name: String?) = name?.let { "ferry $it" } ?: "ferry"

  override fun ferryWarning(title: String, warning: String) = "The $title $warning"

  override fun closesShortlyAfter(closing: String, around: String) =
    "closes at $closing, shortly after you get there around $around"

  override fun closedWhenThere(around: String) = "is closed when you get there around $around"

  override fun closesBefore(closing: String, around: String) =
    "closes at $closing, before you get there around $around"

  override fun opensAt(time: String) = ", it opens at $time"

  override fun opensTomorrowAt(time: String) = ", it opens tomorrow at $time"

  override fun opensOn(date: String, time: String) = ", it opens on $date at $time"

  override fun next(next: String, after: String?) =
    if (after != null) "Next $next, then $after" else "Next $next"

  override fun nextOutside(next: String, after: String) = "Next $next, then $after"

  override fun nextAt(next: String, distance: String, after: String) =
    "Next $next, $distance, then $after"

  override fun ahead(next: String, after: String?) =
    if (after != null) "$next ahead, then $after" else "$next ahead, the end"

  override fun offRoute(next: String, way: String) = "Off route, next $next, $way"

  override fun guidance(next: String, way: String) = "Next $next, $way"

  override fun backOnRoute(next: String) = "Back on route, next $next"

  override fun wrongWay(next: String) = "Wrong direction, turn around, next $next"

  override fun wrongWayNext(next: String) = "Wrong direction, next $next"

  override val arrived = "You've arrived"
  override val compassDirections =
    listOf("north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west")

  override fun way(distance: String, direction: String) = "$distance to the $direction"

  override fun wayTo(distance: String, direction: String, next: String) =
    "$distance $direction to $next"

  override fun follow(next: String, follow: String) = "At $next follow $follow"

  override fun isTheEnd(next: String) = "$next is the end"

  override fun offRouteNext(next: String) = "Off route, next $next"

  override fun spokenDistance(meters: Int) =
    if (meters < 1000) "$meters meters"
    else "${MapFeatures.kilometers(meters, decimalSeparator)} kilometers"

  override fun spokenDistanceAhead(meters: Int) =
    if (meters < 1000) "$meters meters"
    else "${MapFeatures.kilometers(meters, decimalSeparator)} kilometers"

  override val followTheWay = "Follow the way"

  override fun turnName(direction: TurnDirection) =
    when (direction) {
      TurnDirection.STRAIGHT -> "Straight on"
      TurnDirection.SLIGHT_LEFT -> "Slight left"
      TurnDirection.LEFT -> "Left"
      TurnDirection.SHARP_LEFT -> "Sharp left"
      TurnDirection.SLIGHT_RIGHT -> "Slight right"
      TurnDirection.RIGHT -> "Right"
      TurnDirection.SHARP_RIGHT -> "Sharp right"
      TurnDirection.BACK -> "Turn back"
    }

  override fun turnIn(turn: String, distance: String) = "$turn in $distance"

  override fun turnAhead(direction: TurnDirection, distance: String) =
    when (direction) {
      TurnDirection.STRAIGHT -> "In $distance, go straight on"
      TurnDirection.SLIGHT_LEFT -> "In $distance, bear left"
      TurnDirection.LEFT -> "In $distance, turn left"
      TurnDirection.SHARP_LEFT -> "In $distance, turn sharp left"
      TurnDirection.SLIGHT_RIGHT -> "In $distance, bear right"
      TurnDirection.RIGHT -> "In $distance, turn right"
      TurnDirection.SHARP_RIGHT -> "In $distance, turn sharp right"
      TurnDirection.BACK -> "In $distance, turn back"
    }

  override fun turnNow(direction: TurnDirection) =
    when (direction) {
      TurnDirection.STRAIGHT -> "Go straight on now"
      TurnDirection.SLIGHT_LEFT -> "Bear left now"
      TurnDirection.LEFT -> "Turn left now"
      TurnDirection.SHARP_LEFT -> "Turn sharp left now"
      TurnDirection.SLIGHT_RIGHT -> "Bear right now"
      TurnDirection.RIGHT -> "Turn right now"
      TurnDirection.SHARP_RIGHT -> "Turn sharp right now"
      TurnDirection.BACK -> "Turn back now"
    }

  private fun way(switchTo: PathSwitch) =
    when (switchTo) {
      PathSwitch.STREET -> "the street"
      PathSwitch.CYCLE_PATH -> "the cycle path"
      PathSwitch.PATH -> "the path"
    }

  override fun switchName(switchTo: PathSwitch) = "To ${way(switchTo)}"

  override fun switchAhead(switchTo: PathSwitch, distance: String) =
    "In $distance, switch to ${way(switchTo)}"

  override fun switchNow(switchTo: PathSwitch) = "Switch to ${way(switchTo)} now"

  override fun onto(notice: String, switchTo: PathSwitch) = "$notice, onto ${way(switchTo)}"

  override fun toKnot(distance: String, next: String, endArrival: String?) =
    if (endArrival != null) "$distance to $next · end around $endArrival" else "$distance to $next"

  override fun toTheEnd(distance: String, arrival: String?) =
    if (arrival != null) "$distance to the end, around $arrival" else "$distance to the end"

  override fun ridden(ridden: String, next: String) = "$ridden ridden, next knot $next"

  override fun of(done: String, total: String, unit: String) = "$done of $total $unit"

  override val start = "start"
  override val stop = "stop"
  override val end = "end"

  override fun knot(ref: String) = "Knot $ref"

  override fun ferryBetween(legs: String) = "Ferry $legs"

  override fun point(role: WaypointRole) =
    when (role) {
      WaypointRole.START -> "the start"
      WaypointRole.STOP -> "the stop"
      WaypointRole.END -> "the end"
    }

  private fun onTo(role: WaypointRole) = "on to ${point(role)}"

  override fun nextPoint(role: WaypointRole, after: String?) =
    if (after != null) "On to ${point(role)}, then $after" else "On to ${point(role)}"

  override fun aheadPoint(role: WaypointRole, after: String?) =
    if (after != null) "${capitalized(point(role))} ahead, then $after"
    else "${capitalized(point(role))} ahead"

  override fun offRoutePoint(role: WaypointRole, way: String) = "Off route, ${onTo(role)}, $way"

  override fun guidancePoint(role: WaypointRole, way: String) = "On to ${point(role)}, $way"

  override fun backOnRoutePoint(role: WaypointRole) = "Back on route, ${onTo(role)}"

  override fun wrongWayPoint(role: WaypointRole) = "Wrong direction, turn around, ${onTo(role)}"

  override fun wrongWayNextPoint(role: WaypointRole) = "Wrong direction, ${onTo(role)}"

  override fun offRouteNextPoint(role: WaypointRole) = "Off route, ${onTo(role)}"

  override fun wayToPoint(distance: String, direction: String, role: WaypointRole) =
    "$distance $direction to ${point(role)}"

  override fun toPoint(distance: String, role: WaypointRole, endArrival: String?) =
    if (endArrival != null) "$distance to ${point(role)} · end around $endArrival"
    else "$distance to ${point(role)}"

  override fun riddenPoint(ridden: String, role: WaypointRole) = "$ridden ridden, ${onTo(role)}"
}

internal object GermanTexts : Texts {
  override val decimalSeparator = ','
  private val months =
    listOf(
      "Januar",
      "Februar",
      "März",
      "April",
      "Mai",
      "Juni",
      "Juli",
      "August",
      "September",
      "Oktober",
      "November",
      "Dezember",
    )

  override fun date(day: Int, month: Int) = "$day. ${months[month - 1]}"

  override fun about(duration: String) = "etwa $duration"

  override val ferry = "Fähre"
  override val unknownTimes = "Fahrzeiten unbekannt, vorher prüfen"
  override val seasonal = "Saisonal."

  override fun seasonalIn(season: String) = "Saisonal ($season)."

  override fun fee(charge: String) = "Gebühr: $charge"

  override val free = "Kostenlos"
  override val feeCharged = "Kostenpflichtig"
  override val open = "Geöffnet"

  override fun openUntil(time: String) = "Geöffnet bis $time"

  override val closed = "Geschlossen"

  override fun closedUntil(time: String) = "Geschlossen bis $time"

  override fun closedUntilTomorrow(time: String) = "Geschlossen bis morgen $time"

  override fun atTime(time: String) = "Um $time"

  override fun ferryTitle(name: String?) = name?.let { "Fähre $it" } ?: "Fähre"

  override fun ferryWarning(title: String, warning: String) = "Die $title $warning"

  override fun closesShortlyAfter(closing: String, around: String) =
    "schließt um $closing, kurz nach der Ankunft gegen $around"

  override fun closedWhenThere(around: String) = "ist bei der Ankunft gegen $around geschlossen"

  override fun closesBefore(closing: String, around: String) =
    "schließt um $closing, vor der Ankunft gegen $around"

  override fun opensAt(time: String) = ", sie öffnet um $time"

  override fun opensTomorrowAt(time: String) = ", sie öffnet morgen um $time"

  override fun opensOn(date: String, time: String) = ", sie öffnet am $date um $time"

  override fun next(next: String, after: String?) =
    if (after != null) "Nächster Knotenpunkt $next, dann $after" else "Nächster Knotenpunkt $next"

  override fun nextOutside(next: String, after: String) = "Nächster $next, dann $after"

  override fun nextAt(next: String, distance: String, after: String) =
    "Nächster $next, $distance, dann $after"

  override fun ahead(next: String, after: String?) =
    if (after != null) "$next voraus, dann $after" else "$next voraus, das Ende"

  override fun offRoute(next: String, way: String) =
    "Abseits der Route, nächster Knotenpunkt $next, $way"

  override fun guidance(next: String, way: String) = "Nächster Knotenpunkt $next, $way"

  override fun backOnRoute(next: String) = "Zurück auf der Route, nächster Knotenpunkt $next"

  override fun wrongWay(next: String) = "Falsche Richtung, bitte wenden, nächster Knotenpunkt $next"

  override fun wrongWayNext(next: String) = "Falsche Richtung, nächster $next"

  override val arrived = "Ziel erreicht"
  override val compassDirections =
    listOf("Nord", "Nordost", "Ost", "Südost", "Süd", "Südwest", "West", "Nordwest")

  override fun way(distance: String, direction: String) = "$distance Richtung $direction"

  override fun wayTo(distance: String, direction: String, next: String) =
    "$distance Richtung $direction bis $next"

  override fun follow(next: String, follow: String) = "Bei $next folge $follow"

  override fun isTheEnd(next: String) = "$next ist das Ende"

  override fun offRouteNext(next: String) = "Abseits der Route, nächster $next"

  override fun spokenDistance(meters: Int) =
    if (meters < 1000) "$meters Meter"
    else "${MapFeatures.kilometers(meters, decimalSeparator)} Kilometer"

  override fun spokenDistanceAhead(meters: Int) =
    if (meters < 1000) "$meters Metern"
    else "${MapFeatures.kilometers(meters, decimalSeparator)} Kilometer"

  override val followTheWay = "Dem Weg folgen"

  override fun turnName(direction: TurnDirection) =
    when (direction) {
      TurnDirection.STRAIGHT -> "Geradeaus"
      TurnDirection.SLIGHT_LEFT -> "Leicht links"
      TurnDirection.LEFT -> "Links"
      TurnDirection.SHARP_LEFT -> "Scharf links"
      TurnDirection.SLIGHT_RIGHT -> "Leicht rechts"
      TurnDirection.RIGHT -> "Rechts"
      TurnDirection.SHARP_RIGHT -> "Scharf rechts"
      TurnDirection.BACK -> "Umkehren"
    }

  override fun turnIn(turn: String, distance: String) = "$turn in $distance"

  override fun turnAhead(direction: TurnDirection, distance: String) =
    when (direction) {
      TurnDirection.STRAIGHT -> "In $distance geradeaus"
      TurnDirection.SLIGHT_LEFT -> "In $distance leicht links"
      TurnDirection.LEFT -> "In $distance links abbiegen"
      TurnDirection.SHARP_LEFT -> "In $distance scharf links"
      TurnDirection.SLIGHT_RIGHT -> "In $distance leicht rechts"
      TurnDirection.RIGHT -> "In $distance rechts abbiegen"
      TurnDirection.SHARP_RIGHT -> "In $distance scharf rechts"
      TurnDirection.BACK -> "In $distance umkehren"
    }

  override fun turnNow(direction: TurnDirection) =
    when (direction) {
      TurnDirection.STRAIGHT -> "Jetzt geradeaus"
      TurnDirection.SLIGHT_LEFT -> "Jetzt leicht links"
      TurnDirection.LEFT -> "Jetzt links abbiegen"
      TurnDirection.SHARP_LEFT -> "Jetzt scharf links"
      TurnDirection.SLIGHT_RIGHT -> "Jetzt leicht rechts"
      TurnDirection.RIGHT -> "Jetzt rechts abbiegen"
      TurnDirection.SHARP_RIGHT -> "Jetzt scharf rechts"
      TurnDirection.BACK -> "Jetzt umkehren"
    }

  private fun way(switchTo: PathSwitch) =
    when (switchTo) {
      PathSwitch.STREET -> "die Straße"
      PathSwitch.CYCLE_PATH -> "den Radweg"
      PathSwitch.PATH -> "den Weg"
    }

  override fun switchName(switchTo: PathSwitch) = "Auf ${way(switchTo)}"

  override fun switchAhead(switchTo: PathSwitch, distance: String) =
    "In $distance auf ${way(switchTo)} wechseln"

  override fun switchNow(switchTo: PathSwitch) = "Jetzt auf ${way(switchTo)} wechseln"

  override fun onto(notice: String, switchTo: PathSwitch) = "$notice, auf ${way(switchTo)}"

  override fun toKnot(distance: String, next: String, endArrival: String?) =
    if (endArrival != null) "$distance bis $next · Ende gegen $endArrival"
    else "$distance bis $next"

  override fun toTheEnd(distance: String, arrival: String?) =
    if (arrival != null) "$distance bis zum Ende, gegen $arrival" else "$distance bis zum Ende"

  override fun ridden(ridden: String, next: String) =
    "$ridden zurückgelegt, nächster Knotenpunkt $next"

  override fun of(done: String, total: String, unit: String) = "$done von $total $unit"

  override val start = "Start"
  override val stop = "Zwischenstopp"
  override val end = "Ende"

  override fun knot(ref: String) = "Knotenpunkt $ref"

  override fun ferryBetween(legs: String) = "Fähre $legs"

  override fun point(role: WaypointRole) =
    when (role) {
      WaypointRole.START -> "der Start"
      WaypointRole.STOP -> "der Zwischenstopp"
      WaypointRole.END -> "das Ziel"
    }

  /** E.g. `zum Ziel`. */
  private fun toThe(role: WaypointRole) =
    when (role) {
      WaypointRole.START -> "zum Start"
      WaypointRole.STOP -> "zum Zwischenstopp"
      WaypointRole.END -> "zum Ziel"
    }

  /** Without article, e.g. `Ziel voraus`. */
  private fun bare(role: WaypointRole) =
    when (role) {
      WaypointRole.START -> "Start"
      WaypointRole.STOP -> "Zwischenstopp"
      WaypointRole.END -> "Ziel"
    }

  override fun nextPoint(role: WaypointRole, after: String?) =
    if (after != null) "Weiter ${toThe(role)}, dann $after" else "Weiter ${toThe(role)}"

  override fun aheadPoint(role: WaypointRole, after: String?) =
    if (after != null) "${bare(role)} voraus, dann $after" else "${bare(role)} voraus"

  override fun offRoutePoint(role: WaypointRole, way: String) =
    "Abseits der Route, weiter ${toThe(role)}, $way"

  override fun guidancePoint(role: WaypointRole, way: String) = "Weiter ${toThe(role)}, $way"

  override fun backOnRoutePoint(role: WaypointRole) = "Zurück auf der Route, weiter ${toThe(role)}"

  override fun wrongWayPoint(role: WaypointRole) =
    "Falsche Richtung, bitte wenden, weiter ${toThe(role)}"

  override fun wrongWayNextPoint(role: WaypointRole) = "Falsche Richtung, weiter ${toThe(role)}"

  override fun offRouteNextPoint(role: WaypointRole) = "Abseits der Route, weiter ${toThe(role)}"

  override fun wayToPoint(distance: String, direction: String, role: WaypointRole) =
    "$distance Richtung $direction bis ${toThe(role)}"

  override fun toPoint(distance: String, role: WaypointRole, endArrival: String?) =
    if (endArrival != null) "$distance bis ${toThe(role)} · Ende gegen $endArrival"
    else "$distance bis ${toThe(role)}"

  override fun riddenPoint(ridden: String, role: WaypointRole) =
    "$ridden zurückgelegt, weiter ${toThe(role)}"
}

internal object DutchTexts : Texts {
  override val decimalSeparator = ','
  private val months =
    listOf(
      "januari",
      "februari",
      "maart",
      "april",
      "mei",
      "juni",
      "juli",
      "augustus",
      "september",
      "oktober",
      "november",
      "december",
    )

  override fun date(day: Int, month: Int) = "$day ${months[month - 1]}"

  override fun about(duration: String) = "ongeveer $duration"

  override val ferry = "Pont"
  override val unknownTimes = "Vaartijden onbekend, controleer ze vooraf"
  override val seasonal = "Seizoensgebonden."

  override fun seasonalIn(season: String) = "Seizoensgebonden ($season)."

  override fun fee(charge: String) = "Tarief: $charge"

  override val free = "Gratis"
  override val feeCharged = "Betaald"
  override val open = "Open"

  override fun openUntil(time: String) = "Open tot $time"

  override val closed = "Gesloten"

  override fun closedUntil(time: String) = "Gesloten tot $time"

  override fun closedUntilTomorrow(time: String) = "Gesloten tot morgen $time"

  override fun atTime(time: String) = "Om $time"

  override fun ferryTitle(name: String?) = name?.let { "pont $it" } ?: "pont"

  override fun ferryWarning(title: String, warning: String) = "De $title $warning"

  override fun closesShortlyAfter(closing: String, around: String) =
    "sluit om $closing, kort na aankomst rond $around"

  override fun closedWhenThere(around: String) = "is gesloten bij aankomst rond $around"

  override fun closesBefore(closing: String, around: String) =
    "sluit om $closing, vóór aankomst rond $around"

  override fun opensAt(time: String) = ", opent om $time"

  override fun opensTomorrowAt(time: String) = ", opent morgen om $time"

  override fun opensOn(date: String, time: String) = ", opent op $date om $time"

  override fun next(next: String, after: String?) =
    if (after != null) "Volgend knooppunt $next, dan $after" else "Volgend knooppunt $next"

  override fun nextOutside(next: String, after: String) = "Volgende $next, dan $after"

  override fun nextAt(next: String, distance: String, after: String) =
    "Volgende $next, $distance, dan $after"

  override fun ahead(next: String, after: String?) =
    if (after != null) "$next komt eraan, dan $after" else "$next komt eraan, het einde"

  override fun offRoute(next: String, way: String) =
    "Van de route af, volgend knooppunt $next, $way"

  override fun guidance(next: String, way: String) = "Volgend knooppunt $next, $way"

  override fun backOnRoute(next: String) = "Terug op de route, volgend knooppunt $next"

  override fun wrongWay(next: String) = "Verkeerde richting, keer om, volgend knooppunt $next"

  override fun wrongWayNext(next: String) = "Verkeerde richting, volgende $next"

  override val arrived = "Je bent aangekomen"
  override val compassDirections =
    listOf(
      "noord",
      "noordoost",
      "oost",
      "zuidoost",
      "zuid",
      "zuidwest",
      "west",
      "noordwest",
    )

  override fun way(distance: String, direction: String) = "$distance richting $direction"

  override fun wayTo(distance: String, direction: String, next: String) =
    "$distance richting $direction naar $next"

  override fun follow(next: String, follow: String) = "Bij $next volg $follow"

  override fun isTheEnd(next: String) = "$next is het einde"

  override fun offRouteNext(next: String) = "Van de route af, volgende $next"

  override fun spokenDistance(meters: Int) =
    if (meters < 1000) "$meters meter"
    else "${MapFeatures.kilometers(meters, decimalSeparator)} kilometer"

  override fun spokenDistanceAhead(meters: Int) =
    if (meters < 1000) "$meters meter"
    else "${MapFeatures.kilometers(meters, decimalSeparator)} kilometer"

  override val followTheWay = "Volg de weg"

  override fun turnName(direction: TurnDirection) =
    when (direction) {
      TurnDirection.STRAIGHT -> "Rechtdoor"
      TurnDirection.SLIGHT_LEFT -> "Flauw links"
      TurnDirection.LEFT -> "Links"
      TurnDirection.SHARP_LEFT -> "Scherp links"
      TurnDirection.SLIGHT_RIGHT -> "Flauw rechts"
      TurnDirection.RIGHT -> "Rechts"
      TurnDirection.SHARP_RIGHT -> "Scherp rechts"
      TurnDirection.BACK -> "Omkeren"
    }

  override fun turnIn(turn: String, distance: String) = "$turn over $distance"

  override fun turnAhead(direction: TurnDirection, distance: String) =
    when (direction) {
      TurnDirection.STRAIGHT -> "Over $distance rechtdoor"
      TurnDirection.SLIGHT_LEFT -> "Over $distance flauw links"
      TurnDirection.LEFT -> "Over $distance linksaf"
      TurnDirection.SHARP_LEFT -> "Over $distance scherp links"
      TurnDirection.SLIGHT_RIGHT -> "Over $distance flauw rechts"
      TurnDirection.RIGHT -> "Over $distance rechtsaf"
      TurnDirection.SHARP_RIGHT -> "Over $distance scherp rechts"
      TurnDirection.BACK -> "Over $distance omkeren"
    }

  override fun turnNow(direction: TurnDirection) =
    when (direction) {
      TurnDirection.STRAIGHT -> "Nu rechtdoor"
      TurnDirection.SLIGHT_LEFT -> "Nu flauw links"
      TurnDirection.LEFT -> "Nu linksaf"
      TurnDirection.SHARP_LEFT -> "Nu scherp links"
      TurnDirection.SLIGHT_RIGHT -> "Nu flauw rechts"
      TurnDirection.RIGHT -> "Nu rechtsaf"
      TurnDirection.SHARP_RIGHT -> "Nu scherp rechts"
      TurnDirection.BACK -> "Nu omkeren"
    }

  private fun way(switchTo: PathSwitch) =
    when (switchTo) {
      PathSwitch.STREET -> "de weg"
      PathSwitch.CYCLE_PATH -> "het fietspad"
      PathSwitch.PATH -> "het pad"
    }

  override fun switchName(switchTo: PathSwitch) = "Naar ${way(switchTo)}"

  override fun switchAhead(switchTo: PathSwitch, distance: String) =
    "Over $distance naar ${way(switchTo)}"

  override fun switchNow(switchTo: PathSwitch) = "Nu naar ${way(switchTo)}"

  override fun onto(notice: String, switchTo: PathSwitch) = "$notice, naar ${way(switchTo)}"

  override fun toKnot(distance: String, next: String, endArrival: String?) =
    if (endArrival != null) "$distance naar $next · einde rond $endArrival"
    else "$distance naar $next"

  override fun toTheEnd(distance: String, arrival: String?) =
    if (arrival != null) "$distance tot het einde, rond $arrival" else "$distance tot het einde"

  override fun ridden(ridden: String, next: String) = "$ridden afgelegd, volgend knooppunt $next"

  override fun of(done: String, total: String, unit: String) = "$done van $total $unit"

  override val start = "start"
  override val stop = "tussenstop"
  override val end = "einde"

  override fun knot(ref: String) = "Knooppunt $ref"

  override fun ferryBetween(legs: String) = "Pont $legs"

  override fun point(role: WaypointRole) =
    when (role) {
      WaypointRole.START -> "de start"
      WaypointRole.STOP -> "de tussenstop"
      WaypointRole.END -> "het einde"
    }

  override fun nextPoint(role: WaypointRole, after: String?) =
    if (after != null) "Verder naar ${point(role)}, dan $after" else "Verder naar ${point(role)}"

  override fun aheadPoint(role: WaypointRole, after: String?) =
    if (after != null) "${capitalized(point(role))} komt eraan, dan $after"
    else "${capitalized(point(role))} komt eraan"

  override fun offRoutePoint(role: WaypointRole, way: String) =
    "Van de route af, verder naar ${point(role)}, $way"

  override fun guidancePoint(role: WaypointRole, way: String) = "Verder naar ${point(role)}, $way"

  override fun backOnRoutePoint(role: WaypointRole) =
    "Terug op de route, verder naar ${point(role)}"

  override fun wrongWayPoint(role: WaypointRole) =
    "Verkeerde richting, keer om, verder naar ${point(role)}"

  override fun wrongWayNextPoint(role: WaypointRole) =
    "Verkeerde richting, verder naar ${point(role)}"

  override fun offRouteNextPoint(role: WaypointRole) = "Van de route af, verder naar ${point(role)}"

  override fun wayToPoint(distance: String, direction: String, role: WaypointRole) =
    "$distance richting $direction naar ${point(role)}"

  override fun toPoint(distance: String, role: WaypointRole, endArrival: String?) =
    if (endArrival != null) "$distance naar ${point(role)} · einde rond $endArrival"
    else "$distance naar ${point(role)}"

  override fun riddenPoint(ridden: String, role: WaypointRole) =
    "$ridden afgelegd, verder naar ${point(role)}"
}

internal object FrenchTexts : Texts {
  override val decimalSeparator = ','
  private val months =
    listOf(
      "janvier",
      "février",
      "mars",
      "avril",
      "mai",
      "juin",
      "juillet",
      "août",
      "septembre",
      "octobre",
      "novembre",
      "décembre",
    )

  override fun date(day: Int, month: Int) =
    "${if (day == 1) "1er" else day.toString()} ${months[month - 1]}"

  override fun about(duration: String) = "environ $duration"

  override val ferry = "Bac"
  override val unknownTimes = "Horaires inconnus, vérifiez avant de partir"
  override val seasonal = "Saisonnier."

  override fun seasonalIn(season: String) = "Saisonnier ($season)."

  override fun fee(charge: String) = "Tarif : $charge"

  override val free = "Gratuit"
  override val feeCharged = "Payant"
  override val open = "Ouvert"

  override fun openUntil(time: String) = "Ouvert jusqu'à $time"

  override val closed = "Fermé"

  override fun closedUntil(time: String) = "Fermé jusqu'à $time"

  override fun closedUntilTomorrow(time: String) = "Fermé jusqu'à demain $time"

  override fun atTime(time: String) = "À $time"

  override fun ferryTitle(name: String?) = name?.let { "bac $it" } ?: "bac"

  override fun ferryWarning(title: String, warning: String) = "Le $title $warning"

  override fun closesShortlyAfter(closing: String, around: String) =
    "ferme à $closing, peu après l'arrivée vers $around"

  override fun closedWhenThere(around: String) = "est fermé à l'arrivée vers $around"

  override fun closesBefore(closing: String, around: String) =
    "ferme à $closing, avant l'arrivée vers $around"

  override fun opensAt(time: String) = ", il ouvre à $time"

  override fun opensTomorrowAt(time: String) = ", il ouvre demain à $time"

  override fun opensOn(date: String, time: String) = ", il ouvre le $date à $time"

  override fun next(next: String, after: String?) =
    if (after != null) "Prochain point-nœud $next, puis $after" else "Prochain point-nœud $next"

  override fun nextOutside(next: String, after: String) = "Prochain $next, puis $after"

  override fun nextAt(next: String, distance: String, after: String) =
    "Prochain $next, $distance, puis $after"

  override fun ahead(next: String, after: String?) =
    if (after != null) "$next approche, puis $after" else "$next approche, l'arrivée"

  override fun offRoute(next: String, way: String) =
    "Hors itinéraire, prochain point-nœud $next, $way"

  override fun guidance(next: String, way: String) = "Prochain point-nœud $next, $way"

  override fun backOnRoute(next: String) = "De retour sur l'itinéraire, prochain point-nœud $next"

  override fun wrongWay(next: String) =
    "Mauvaise direction, faites demi-tour, prochain point-nœud $next"

  override fun wrongWayNext(next: String) = "Mauvaise direction, suivant $next"

  override val arrived = "Vous êtes arrivé"
  override val compassDirections =
    listOf("nord", "nord-est", "est", "sud-est", "sud", "sud-ouest", "ouest", "nord-ouest")

  override fun way(distance: String, direction: String) = "$distance direction $direction"

  override fun wayTo(distance: String, direction: String, next: String) =
    "$distance direction $direction jusqu'à $next"

  override fun follow(next: String, follow: String) = "Au $next suivre $follow"

  override fun isTheEnd(next: String) = "$next est l'arrivée"

  override fun offRouteNext(next: String) = "Hors itinéraire, suivant $next"

  override fun spokenDistance(meters: Int) =
    if (meters < 1000) "$meters mètres"
    else "${MapFeatures.kilometers(meters, decimalSeparator)} kilomètres"

  override fun spokenDistanceAhead(meters: Int) =
    if (meters < 1000) "$meters mètres"
    else "${MapFeatures.kilometers(meters, decimalSeparator)} kilomètres"

  override val followTheWay = "Suivez le chemin"

  override fun turnName(direction: TurnDirection) =
    when (direction) {
      TurnDirection.STRAIGHT -> "Tout droit"
      TurnDirection.SLIGHT_LEFT -> "Légèrement à gauche"
      TurnDirection.LEFT -> "À gauche"
      TurnDirection.SHARP_LEFT -> "Fortement à gauche"
      TurnDirection.SLIGHT_RIGHT -> "Légèrement à droite"
      TurnDirection.RIGHT -> "À droite"
      TurnDirection.SHARP_RIGHT -> "Fortement à droite"
      TurnDirection.BACK -> "Demi-tour"
    }

  override fun turnIn(turn: String, distance: String) = "$turn dans $distance"

  override fun turnAhead(direction: TurnDirection, distance: String) =
    when (direction) {
      TurnDirection.STRAIGHT -> "Dans $distance, tout droit"
      TurnDirection.SLIGHT_LEFT -> "Dans $distance, légèrement à gauche"
      TurnDirection.LEFT -> "Dans $distance, tournez à gauche"
      TurnDirection.SHARP_LEFT -> "Dans $distance, fortement à gauche"
      TurnDirection.SLIGHT_RIGHT -> "Dans $distance, légèrement à droite"
      TurnDirection.RIGHT -> "Dans $distance, tournez à droite"
      TurnDirection.SHARP_RIGHT -> "Dans $distance, fortement à droite"
      TurnDirection.BACK -> "Dans $distance, faites demi-tour"
    }

  override fun turnNow(direction: TurnDirection) =
    when (direction) {
      TurnDirection.STRAIGHT -> "Tout droit maintenant"
      TurnDirection.SLIGHT_LEFT -> "Légèrement à gauche maintenant"
      TurnDirection.LEFT -> "Tournez à gauche maintenant"
      TurnDirection.SHARP_LEFT -> "Fortement à gauche maintenant"
      TurnDirection.SLIGHT_RIGHT -> "Légèrement à droite maintenant"
      TurnDirection.RIGHT -> "Tournez à droite maintenant"
      TurnDirection.SHARP_RIGHT -> "Fortement à droite maintenant"
      TurnDirection.BACK -> "Faites demi-tour maintenant"
    }

  private fun way(switchTo: PathSwitch) =
    when (switchTo) {
      PathSwitch.STREET -> "la route"
      PathSwitch.CYCLE_PATH -> "la piste cyclable"
      PathSwitch.PATH -> "le chemin"
    }

  override fun switchName(switchTo: PathSwitch) = "Vers ${way(switchTo)}"

  override fun switchAhead(switchTo: PathSwitch, distance: String) =
    "Dans $distance, prenez ${way(switchTo)}"

  override fun switchNow(switchTo: PathSwitch) = "Prenez ${way(switchTo)} maintenant"

  override fun onto(notice: String, switchTo: PathSwitch) = "$notice, sur ${way(switchTo)}"

  override fun toKnot(distance: String, next: String, endArrival: String?) =
    if (endArrival != null) "$distance jusqu'à $next · arrivée vers $endArrival"
    else "$distance jusqu'à $next"

  override fun toTheEnd(distance: String, arrival: String?) =
    if (arrival != null) "$distance jusqu'à l'arrivée, vers $arrival"
    else "$distance jusqu'à l'arrivée"

  override fun ridden(ridden: String, next: String) = "$ridden parcourus, prochain point-nœud $next"

  override fun of(done: String, total: String, unit: String) = "$done sur $total $unit"

  override val start = "départ"
  override val stop = "étape"
  override val end = "arrivée"

  override fun knot(ref: String) = "Point-nœud $ref"

  override fun ferryBetween(legs: String) = "Bac $legs"

  override fun point(role: WaypointRole) =
    when (role) {
      WaypointRole.START -> "le départ"
      WaypointRole.STOP -> "l'étape"
      WaypointRole.END -> "l'arrivée"
    }

  /** E.g. `jusqu'au départ` or `jusqu'à l'arrivée`. */
  private fun upTo(role: WaypointRole) =
    if (role == WaypointRole.START) "jusqu'au départ" else "jusqu'à ${point(role)}"

  override fun nextPoint(role: WaypointRole, after: String?) =
    if (after != null) "Vers ${point(role)}, puis $after" else "Vers ${point(role)}"

  override fun aheadPoint(role: WaypointRole, after: String?) =
    if (after != null) "${capitalized(point(role))} approche, puis $after"
    else "${capitalized(point(role))} approche"

  override fun offRoutePoint(role: WaypointRole, way: String) =
    "Hors itinéraire, vers ${point(role)}, $way"

  override fun guidancePoint(role: WaypointRole, way: String) = "Vers ${point(role)}, $way"

  override fun backOnRoutePoint(role: WaypointRole) =
    "De retour sur l'itinéraire, vers ${point(role)}"

  override fun wrongWayPoint(role: WaypointRole) =
    "Mauvaise direction, faites demi-tour, vers ${point(role)}"

  override fun wrongWayNextPoint(role: WaypointRole) = "Mauvaise direction, vers ${point(role)}"

  override fun offRouteNextPoint(role: WaypointRole) = "Hors itinéraire, vers ${point(role)}"

  override fun wayToPoint(distance: String, direction: String, role: WaypointRole) =
    "$distance direction $direction ${upTo(role)}"

  override fun toPoint(distance: String, role: WaypointRole, endArrival: String?) =
    if (endArrival != null) "$distance ${upTo(role)} · arrivée vers $endArrival"
    else "$distance ${upTo(role)}"

  override fun riddenPoint(ridden: String, role: WaypointRole) =
    "$ridden parcourus, vers ${point(role)}"
}

/** [text] with its first letter upper case, e.g. `The stop`. */
internal fun capitalized(text: String): String = text.replaceFirstChar { it.uppercase() }
