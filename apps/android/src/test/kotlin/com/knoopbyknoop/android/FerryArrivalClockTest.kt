package com.knoopbyknoop.android

import com.knoopbyknoop.core.Connection
import com.knoopbyknoop.core.Ferry
import com.knoopbyknoop.core.Fix
import com.knoopbyknoop.core.Knot
import com.knoopbyknoop.core.Navigation
import com.knoopbyknoop.core.NetworkGraph
import com.knoopbyknoop.core.NetworkType
import com.knoopbyknoop.core.NoticeKind
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Covers `knot-navigation` "Ferry arrival estimate": "Phone in another time zone". */
class FerryArrivalClockTest {

  @Test
  fun `phone in another time zone`() {
    // 20 km to the middle of the ferry's leg: 75 minutes at the assumed 16 km/h.
    val knots =
      (0..2).map { Knot(it + 1L, "${it + 10}", NetworkType.CYCLING, 51.5, 6.0 + it / 100.0) }
    val connections = listOf(Connection(1, 2, 19_900), Connection(2, 3, 200))
    val ferry = Ferry(name = "Veer Broekhuizen", openingHours = "Apr-Oct: Mo-Su 07:00-19:00")
    val graph = NetworkGraph(knots, connections, mapOf(connections[1] to ferry))
    val london = ZonedDateTime.of(2027, 6, 16, 17, 0, 0, 0, ZoneId.of("Europe/London"))
    val fix = Fix(51.5, 6.0, 5.0, london.toInstant().toEpochMilli())

    val started =
      assertNotNull(
        Navigation.start(knots, graph, null, fix) { ferryMoment(Instant.ofEpochMilli(it)) }
      )

    assertEquals(
      "You get there around 19:15",
      started.navigation.ferryArrival(1)?.text,
      "18:15 in London",
    )
    assertEquals(
      listOf("The ferry Veer Broekhuizen closes at 19:00, before you get there around 19:15"),
      started.notices.filter { it.kind == NoticeKind.FERRY }.map { it.text },
    )
  }
}
