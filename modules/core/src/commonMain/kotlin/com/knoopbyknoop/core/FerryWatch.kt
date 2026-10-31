package com.knoopbyknoop.core

import com.knoopbyknoop.core.RouteFerry.Companion.clock
import com.knoopbyknoop.core.RouteFerry.Companion.date
import com.knoopbyknoop.core.RouteFerry.Companion.endOfDayBefore
import com.knoopbyknoop.core.RouteFerry.Companion.minutesBetween
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Whether a ferry runs when the rider gets there, see `knot-navigation` "Ferry arrival estimate".
 */
enum class FerryArrivalState {
  /** Open then, and not closing shortly after. */
  OPEN,

  /** Closed then, or closing shortly after, see `knot-navigation` "Ferry warning". */
  WARNING,

  /** The operating times do not tell. */
  UNKNOWN,
}

/**
 * When the rider gets to a ferry ahead and whether it runs then, the line the ferry list shows in
 * place of the status for now, see `knot-navigation` "Ferry arrival estimate".
 *
 * @property text e.g. `At 14:30: open until 19:00` or `At 19:15: closed until tomorrow 07:00`
 */
data class FerryArrival(val text: String, val state: FerryArrivalState) {

  /** What screen readers say after the ferry's place, e.g. `at 21:10 closed until 1 May 10:00`. */
  val accessibilityText: String
    get() = text.replaceFirst(": ", " ").replaceFirstChar { it.lowercase() }
}

/**
 * The ferries ahead on a navigated route, with when the rider gets there and the warnings given,
 * see `knot-navigation` "Ferry arrival estimate" and "Ferry warning". Immutable like [Navigation];
 * the distances along the route come from [RouteProgress], the pace from [Navigation].
 */
internal class FerryWatch
private constructor(
  private val ferries: List<FerryAhead>,
  private val offsets: List<Double>,
  private val localTime: (Long) -> LocalMoment,
  private val arrivals: Map<Int, FerryArrival>,
  private val warned: Set<Int>,
) {

  /** A ferry on the leg from the knot at [fromPosition] to the next one. */
  private class FerryAhead(val fromPosition: Int, val title: String, val hours: OpeningHours?)

  /** When the rider gets to the ferry from the knot at [fromPosition], or `null` once passed. */
  fun arrivalAt(fromPosition: Int): FerryArrival? = arrivals[fromPosition]

  /** Renews the estimates with the rider at the knot at [index] at [timeMillis]. */
  fun atKnot(
    index: Int,
    timeMillis: Long,
    metersPerSecond: Double,
  ): Pair<FerryWatch, List<Notice>> =
    renew(offsets[index], timeMillis, nextIndex = index + 1, metersPerSecond)

  /**
   * Renews the estimates with the rider [offset] meters along the route at [timeMillis], before the
   * knot at [nextIndex], at [metersPerSecond]; returns the warnings due, the nearer ferry first.
   */
  private fun renew(
    offset: Double,
    timeMillis: Long,
    nextIndex: Int,
    metersPerSecond: Double,
  ): Pair<FerryWatch, List<Notice>> {
    val now = localTime(timeMillis)
    val arrivals = mutableMapOf<Int, FerryArrival>()
    val notices = mutableListOf<Notice>()
    val warned = warned.toMutableSet()
    for (ferry in ferries.filter { it.fromPosition + 1 >= nextIndex }) {
      val position = ferry.fromPosition
      val ferryOffset = (offsets[position] + offsets[position + 1]) / 2
      val seconds = max(0.0, ferryOffset - offset) / metersPerSecond
      val arrival = localTime(timeMillis + (seconds * 1000).roundToLong())
      val around = clock(roundedToFiveMinutes(arrival))
      val status = ferry.hours?.statusAt(arrival)
      val warning = status?.let { warning(ferry.hours, it, now, arrival, around) }
      arrivals[position] =
        if (status == null) FerryArrival("At $around", FerryArrivalState.UNKNOWN)
        else
          FerryArrival(
            "At $around: ${RouteFerry.statusText(status, arrival).replaceFirstChar { it.lowercase() }}",
            when {
              warning != null -> FerryArrivalState.WARNING
              else -> FerryArrivalState.OPEN
            },
          )
      if (warning != null && warned.add(position)) {
        val text = "The ${ferry.title} $warning"
        notices += Notice(NoticeKind.FERRY, text, text)
      }
    }
    return FerryWatch(ferries, offsets, localTime, arrivals, warned) to notices
  }

  companion object {

    /** A ferry closing this soon after the arrival is warned about too. */
    private const val SHORTLY_AFTER_MINUTES = 15

    /** At most this many changes are walked to find the last closing before the arrival. */
    private const val MAXIMUM_CHANGES = 20

    /**
     * Starts watching the ferries on [route], with the rider [offset] meters along it before the
     * knot at [nextIndex] at [timeMillis]. `null` without ferries.
     *
     * @param localTime the ferries' local time of an instant in epoch milliseconds
     */
    fun start(
      route: RouteProgress,
      graph: NetworkGraph,
      localTime: (Long) -> LocalMoment,
      offset: Double,
      nextIndex: Int,
      timeMillis: Long,
      metersPerSecond: Double,
    ): Pair<FerryWatch, List<Notice>>? {
      val ferries =
        route.knots.zipWithNext().mapIndexedNotNull { position, (from, to) ->
          graph.ferryBetween(from, to)?.let { ferry ->
            val title = ferry.name?.let { "ferry $it" } ?: "ferry"
            FerryAhead(position, title, ferry.openingHours?.let(OpeningHours::parse))
          }
        }
      if (ferries.isEmpty()) return null
      val watch = FerryWatch(ferries, route.offsets, localTime, mapOf(), setOf())
      return watch.renew(offset, timeMillis, nextIndex, metersPerSecond)
    }

    private fun roundedToFiveMinutes(moment: LocalMoment): LocalMoment {
      val minute = (moment.minute + 2) / 5 * 5
      return if (minute >= MINUTES_PER_DAY) LocalMoment(moment.day.plusDays(1), 0)
      else LocalMoment(moment.day, minute)
    }

    /**
     * What to warn about a ferry with [hours] and [status] at [arrival] ([around] rounded), e.g.
     * `closes at 19:00, before you get there around 19:15`, or `null` without a warning.
     */
    private fun warning(
      hours: OpeningHours,
      status: OpeningStatus,
      now: LocalMoment,
      arrival: LocalMoment,
      around: String,
    ): String? {
      val next = status.next
      if (status.open) {
        if (next == null || minutesBetween(arrival, next) > SHORTLY_AFTER_MINUTES) return null
        return "closes at ${clock(endOfDayBefore(next))}, shortly after you get there around $around"
      }
      val closed = "is closed when you get there around $around"
      if (next != null && next.day == arrival.day) return "$closed, it opens at ${clock(next)}"
      val closing = lastClosing(hours, now, arrival)?.let(::endOfDayBefore)
      if (closing != null && closing.day == arrival.day) {
        return "closes at ${clock(closing)}, before you get there around $around"
      }
      return when {
        next == null -> closed
        next.day == now.day.plusDays(1) -> "$closed, it opens tomorrow at ${clock(next)}"
        else -> "$closed, it opens on ${date(next)} at ${clock(next)}"
      }
    }

    /** The last certain closing after [now] and before [arrival], or `null`. */
    private fun lastClosing(
      hours: OpeningHours,
      now: LocalMoment,
      arrival: LocalMoment,
    ): LocalMoment? {
      var moment = now
      var closing: LocalMoment? = null
      repeat(MAXIMUM_CHANGES) {
        val status = hours.statusAt(moment) ?: return closing
        val next = status.next ?: return closing
        if (minutesBetween(next, arrival) < 0) return closing
        if (status.open) closing = next
        moment = next
      }
      return closing
    }

    private const val MINUTES_PER_DAY = 24 * 60
  }
}
