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

  /** E.g. `45 ahead, then 12`, or `45 ahead, the end`. */
  fun ahead(next: String, after: String?): String

  fun offRoute(next: String, way: String): String

  /** Off route, the guidance, e.g. `Next 45, 1.7 km to the north-east`. */
  fun guidance(next: String, way: String): String

  fun backOnRoute(next: String): String

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

  override fun ahead(next: String, after: String?) =
    if (after != null) "$next ahead, then $after" else "$next ahead, the end"

  override fun offRoute(next: String, way: String) = "Off route, next $next, $way"

  override fun guidance(next: String, way: String) = "Next $next, $way"

  override fun backOnRoute(next: String) = "Back on route, next $next"

  override val arrived = "You've arrived"
  override val compassDirections =
    listOf("north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west")

  override fun way(distance: String, direction: String) = "$distance to the $direction"

  override fun wayTo(distance: String, direction: String, next: String) =
    "$distance $direction to $next"

  override fun follow(next: String, follow: String) = "At $next follow $follow"

  override fun isTheEnd(next: String) = "$next is the end"

  override fun offRouteNext(next: String) = "Off route, next $next"

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

  override fun ahead(next: String, after: String?) =
    if (after != null) "$next voraus, dann $after" else "$next voraus, das Ende"

  override fun offRoute(next: String, way: String) =
    "Abseits der Route, nächster Knotenpunkt $next, $way"

  override fun guidance(next: String, way: String) = "Nächster Knotenpunkt $next, $way"

  override fun backOnRoute(next: String) = "Zurück auf der Route, nächster Knotenpunkt $next"

  override val arrived = "Ziel erreicht"
  override val compassDirections =
    listOf("Nord", "Nordost", "Ost", "Südost", "Süd", "Südwest", "West", "Nordwest")

  override fun way(distance: String, direction: String) = "$distance Richtung $direction"

  override fun wayTo(distance: String, direction: String, next: String) =
    "$distance Richtung $direction bis $next"

  override fun follow(next: String, follow: String) = "Bei $next folge $follow"

  override fun isTheEnd(next: String) = "$next ist das Ende"

  override fun offRouteNext(next: String) = "Abseits der Route, nächster $next"

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

  override fun ahead(next: String, after: String?) =
    if (after != null) "$next komt eraan, dan $after" else "$next komt eraan, het einde"

  override fun offRoute(next: String, way: String) =
    "Van de route af, volgend knooppunt $next, $way"

  override fun guidance(next: String, way: String) = "Volgend knooppunt $next, $way"

  override fun backOnRoute(next: String) = "Terug op de route, volgend knooppunt $next"

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

  override fun ahead(next: String, after: String?) =
    if (after != null) "$next approche, puis $after" else "$next approche, l'arrivée"

  override fun offRoute(next: String, way: String) =
    "Hors itinéraire, prochain point-nœud $next, $way"

  override fun guidance(next: String, way: String) = "Prochain point-nœud $next, $way"

  override fun backOnRoute(next: String) = "De retour sur l'itinéraire, prochain point-nœud $next"

  override val arrived = "Vous êtes arrivé"
  override val compassDirections =
    listOf("nord", "nord-est", "est", "sud-est", "sud", "sud-ouest", "ouest", "nord-ouest")

  override fun way(distance: String, direction: String) = "$distance direction $direction"

  override fun wayTo(distance: String, direction: String, next: String) =
    "$distance direction $direction jusqu'à $next"

  override fun follow(next: String, follow: String) = "Au $next suivre $follow"

  override fun isTheEnd(next: String) = "$next est l'arrivée"

  override fun offRouteNext(next: String) = "Hors itinéraire, suivant $next"

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
}
