package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Covers `knot-navigation` "Ferry arrival estimate" and "Ferry warning".
 *
 * The knots lie 1000 m apart on a straight road from west to east; the lengths of the connections
 * between them are set per test, so the ferry is as far ahead as the scenario says. The time is
 * counted in minutes from midnight of the scenario's day, in the ferry's local time.
 */
class FerryWarningTest {

  private val seasonal = "Apr-Oct: Mo-Su 07:00-19:00"
  private val wednesday = CalendarDay(2027, 6, 16)

  private class Ride(val knots: List<Knot>, val graph: NetworkGraph, val day: CalendarDay) {

    fun localTime(millis: Long): LocalMoment {
      val minutes = (millis / 60_000).toInt()
      return LocalMoment(day.plusDays(minutes / MINUTES_PER_DAY), minutes % MINUTES_PER_DAY)
    }

    /** A fix at the knot at [index] at [time], e.g. `18:00`. */
    fun fixAt(index: Int, time: String): Fix {
      val knot = knots[index]
      val (hours, minutes) = time.split(":").map { it.toLong() }
      return Fix(knot.latitude, knot.longitude, 5.0, (hours * 60 + minutes) * 60_000)
    }

    fun start(time: String, at: Int = 0): NavigationUpdate =
      assertNotNull(Navigation.start(knots, graph, null, fixAt(at, time), localTime = ::localTime))
  }

  /**
   * A route over connections of [lengths] meters, with ferries on the legs [ferryLegs] (indices
   * into [lengths]).
   */
  private fun ride(
    vararg lengths: Int,
    ferryLegs: Map<Int, Ferry>,
    day: CalendarDay = wednesday,
    network: NetworkType = NetworkType.CYCLING,
  ): Ride {
    val knots =
      (0..lengths.size).map { index ->
        Knot(
          index.toLong() + 1,
          (index + 10).toString(),
          network,
          BASE_LATITUDE,
          BASE_LONGITUDE + index * 1000.0 / metersPerDegreeLongitude,
        )
      }
    val connections = lengths.mapIndexed { index, length ->
      Connection(knots[index].id, knots[index + 1].id, length)
    }
    val ferries = ferryLegs.mapKeys { (leg, _) -> connections[leg] }
    return Ride(knots, NetworkGraph(knots, connections, ferries), day)
  }

  /** A ferry [kilometers] ahead of the first knot, in the middle of a 200 m leg. */
  private fun ferryAhead(
    kilometers: Double,
    ferry: Ferry = broekhuizen,
    day: CalendarDay = wednesday,
    network: NetworkType = NetworkType.CYCLING,
  ) =
    ride(
      (kilometers * 1000).toInt() - 100,
      200,
      1000,
      ferryLegs = mapOf(1 to ferry),
      day = day,
      network = network,
    )

  private val broekhuizen = Ferry(name = "Veer Broekhuizen", openingHours = seasonal)

  private fun NavigationUpdate.ferryNotices() =
    notices.filter { it.kind == NoticeKind.FERRY }.map { it.text }

  // Requirement: Ferry warning

  @Test
  fun `closed when the rider gets there`() {
    assertEquals(
      listOf("The ferry Veer Broekhuizen closes at 19:00, before you get there around 19:15"),
      ferryAhead(20.0).start("18:00").ferryNotices(),
    )
  }

  @Test
  fun `closes shortly after the rider gets there`() {
    assertEquals(
      listOf(
        "The ferry Veer Broekhuizen closes at 19:00, shortly after you get there around 18:50"
      ),
      ferryAhead(12.0).start("18:05").ferryNotices(),
    )
  }

  @Test
  fun `not open yet when the rider gets there`() {
    assertEquals(
      listOf(
        "The ferry Veer Broekhuizen is closed when you get there around 06:15, it opens at 07:00"
      ),
      ferryAhead(8.0).start("05:45").ferryNotices(),
    )
  }

  @Test
  fun `closed for the season`() {
    assertEquals(
      listOf(
        "The ferry Veer Broekhuizen is closed when you get there around 12:30, " +
          "it opens on 1 April at 07:00"
      ),
      ferryAhead(8.0, day = CalendarDay(2026, 12, 2)).start("12:00").ferryNotices(),
    )
  }

  @Test
  fun `closed until tomorrow when the rider gets there`() {
    assertEquals(
      listOf(
        "The ferry Veer Broekhuizen is closed when you get there around 20:30, " +
          "it opens tomorrow at 07:00"
      ),
      ferryAhead(8.0).start("20:00").ferryNotices(),
    )
  }

  @Test
  fun `open when the rider gets there`() {
    val started = ferryAhead(8.0).start("14:00")

    assertEquals(listOf(), started.ferryNotices())
    assertEquals(
      FerryArrival("You get there around 14:30", warning = false),
      started.navigation.ferryArrival(1),
    )
  }

  @Test
  fun `the rider's own pace`() {
    // 10 km in 50 minutes, then 10 km to the ferry: 12 km/h instead of the assumed 16 km/h.
    val ride = ride(10_000, 9_900, 200, 1000, ferryLegs = mapOf(2 to broekhuizen))
    val started = ride.start("17:10")
    assertEquals(listOf(), started.ferryNotices(), "18:25 at the assumed pace")

    val update = started.navigation.update(ride.fixAt(1, "18:00"))

    assertEquals(
      listOf(
        "The ferry Veer Broekhuizen closes at 19:00, shortly after you get there around 18:50"
      ),
      update.ferryNotices(),
    )
  }

  @Test
  fun `the assumed pace until 2 km are covered`() {
    // 1.5 km in 30 minutes would be 3 km/h, but the assumed 16 km/h still applies.
    val ride = ride(1_500, 8_400, 200, 1000, ferryLegs = mapOf(2 to broekhuizen))
    val update = ride.start("17:00").navigation.update(ride.fixAt(1, "17:30"))

    assertEquals(
      FerryArrival("You get there around 18:00", warning = false),
      update.navigation.ferryArrival(2),
    )
  }

  @Test
  fun `hiking`() {
    assertEquals(
      listOf("The ferry Veer Broekhuizen closes at 19:00, before you get there around 19:10"),
      ferryAhead(3.0, network = NetworkType.HIKING).start("18:30").ferryNotices(),
    )
  }

  @Test
  fun `falling behind`() {
    val ride = ride(4_000, 11_900, 200, 1000, ferryLegs = mapOf(2 to broekhuizen))
    val started = ride.start("17:30")
    assertEquals(listOf(), started.ferryNotices(), "18:30 at the assumed pace")

    val update = started.navigation.update(ride.fixAt(1, "18:10"))

    assertEquals(
      listOf("The ferry Veer Broekhuizen closes at 19:00, before you get there around 20:10"),
      update.ferryNotices(),
    )
  }

  @Test
  fun `warned once`() {
    val ride = ride(10_000, 9_900, 200, 1000, ferryLegs = mapOf(2 to broekhuizen))
    val started = ride.start("18:00")
    assertEquals(1, started.ferryNotices().size)

    val update = started.navigation.update(ride.fixAt(1, "18:40"))

    assertEquals(listOf(), update.ferryNotices())
    assertEquals(true, update.navigation.ferryArrival(2)?.warning)
  }

  @Test
  fun `ferry without a name`() {
    assertEquals(
      listOf("The ferry closes at 19:00, before you get there around 19:15"),
      ferryAhead(20.0, Ferry(openingHours = seasonal)).start("18:00").ferryNotices(),
    )
  }

  @Test
  fun `times that cannot be told`() {
    val holidays = Ferry(name = "Veer", openingHours = "Mo-Sa 06:00-19:00; PH,Su 09:00-19:00")
    val goodFriday = ferryAhead(8.0, holidays, day = CalendarDay(2027, 3, 26)).start("07:30")
    val unknown = ferryAhead(20.0, Ferry(name = "Veer")).start("18:00")

    assertEquals(listOf(), goodFriday.ferryNotices())
    assertEquals(listOf(), unknown.ferryNotices())
    assertEquals(
      FerryArrival("You get there around 19:15", warning = false),
      unknown.navigation.ferryArrival(1),
    )
  }

  @Test
  fun `several ferries`() {
    val other = Ferry(name = "Veer Arcen", openingHours = "Mo-Su 07:00-18:00")
    val ride = ride(7_900, 200, 11_800, 200, 1000, ferryLegs = mapOf(1 to broekhuizen, 3 to other))

    assertEquals(
      listOf(
        "The ferry Veer Broekhuizen is closed when you get there around 19:30, " +
          "it opens tomorrow at 07:00",
        "The ferry Veer Arcen is closed when you get there around 20:15, it opens tomorrow at 07:00",
      ),
      ride.start("19:00").ferryNotices(),
    )
  }

  @Test
  fun `warning in the ferry list`() {
    assertEquals(
      FerryArrival("You get there around 19:15", warning = true),
      ferryAhead(20.0).start("18:00").navigation.ferryArrival(1),
    )
  }

  // Requirement: Ferry arrival estimate

  @Test
  fun `arrival in the ferry list`() {
    val ride = ride(10_000, 9_900, 200, 1000, ferryLegs = mapOf(2 to broekhuizen))
    val started = ride.start("17:00")
    assertEquals(
      FerryArrival("You get there around 18:15", warning = false),
      started.navigation.ferryArrival(2),
    )

    val update = started.navigation.update(ride.fixAt(1, "17:30"))

    assertEquals(
      FerryArrival("You get there around 18:00", warning = false),
      update.navigation.ferryArrival(2),
      "10 km in 30 minutes, renewed at the knot",
    )
  }

  @Test
  fun `ferry passed`() {
    val ride = ride(1000, 200, 1000, 1000, ferryLegs = mapOf(1 to broekhuizen))
    val started = ride.start("18:50")
    assertNotNull(started.navigation.ferryArrival(1))

    val passed =
      started.navigation.update(ride.fixAt(1, "18:54")).navigation.update(ride.fixAt(2, "19:05"))

    assertNull(passed.navigation.ferryArrival(1))
    assertEquals(listOf(), passed.ferryNotices())
  }

  @Test
  fun `no estimate without the local time`() {
    val ride = ferryAhead(20.0)
    val started =
      assertNotNull(Navigation.start(ride.knots, ride.graph, null, ride.fixAt(0, "18:00")))

    assertEquals(listOf(), started.ferryNotices())
    assertNull(started.navigation.ferryArrival(1))
  }

  @Test
  fun `routes without ferries have no estimates`() {
    val started = ride(1000, 1000, ferryLegs = mapOf()).start("18:00")

    assertEquals(listOf(), started.ferryNotices())
    assertNull(started.navigation.ferryArrival(0))
  }

  @Test
  fun `the warning comes after the notice of the knot`() {
    val notices = ferryAhead(20.0).start("18:00").notices.map { it.kind }

    assertEquals(listOf(NoticeKind.NEXT, NoticeKind.FERRY), notices)
  }

  private companion object {
    const val MINUTES_PER_DAY = 24 * 60
    const val BASE_LATITUDE = 51.5
    const val BASE_LONGITUDE = 6.0
    val metersPerDegreeLongitude = 6_371_008.8 * PI / 180 * cos(BASE_LATITUDE * PI / 180)
  }
}
