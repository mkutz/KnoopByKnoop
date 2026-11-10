package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Scripted rides on cut-outs of the real network data, fix by fix through [NavigationSession] as
 * the apps run it, each way search answered at once with the cut-out's path data, see the change
 * `replay-rides-on-real-data`. Each ride is compared with the text recorded in
 * `src/jvmTest/resources/real-rides/<ride>.txt`; a missing text is recorded and the test fails
 * once. The texts pin today's behavior, findings of the rides included: after a deliberate change,
 * delete the text, run again and review the difference.
 */
class RealRideTest {

  private fun replay(name: String): String {
    val script = RideScript.read(REAL_RIDES.resolve("$name.ride"))
    val area = script.area
    val session =
      NavigationSession(
        NavigationRoute(script.knots, area.graph, area.geometry),
        script.localTime,
      )
    val lines = mutableListOf<String>()
    for ((index, rideFix) in script.fixes.withIndex()) {
      val fix = rideFix.fix
      val step = session.onFix(fix, rideFix.visible, fix.timeMillis)
      val parts = mutableListOf(place(index, fix, rideFix.visible))
      parts += step.notices.joinToString("; ") { "${it.kind} ${it.text}" }
      parts += "${step.precision}"
      step.search?.let { search ->
        val way = area.router.way(search.from, search.to, search.heading)
        parts += way?.let(::described) ?: "no way"
        session.onWay(way)
      }
      session.state.navigation?.let { navigation ->
        val guidedWay = session.state.guidedWay
        parts += navigation.header(fix, session.state.direction, guidedWay, area.geometry).spoken
        parts += navigation.progressLine
        guidedWay?.let { parts += "guided ${it.view(navigation.language).line}" }
      }
      if (step.end != SessionEnd.NONE) parts += "end ${step.end}"
      lines += parts.filter { it.isNotEmpty() }.joinToString(" | ")
      if (step.end != SessionEnd.NONE) break
    }
    return lines.joinToString("\n", postfix = "\n")
  }

  private fun place(index: Int, fix: Fix, visible: Boolean) =
    "%d %.5f,%.5f ±%.0f%s"
      .format(
        index,
        fix.latitude,
        fix.longitude,
        fix.accuracyMeters,
        if (visible) "" else " off",
      )

  private fun described(way: PathWay) =
    "way %.0f m%s, turns %s"
      .format(
        way.lengthMeters,
        if (way.startsBack) " starting back" else "",
        way.turns.joinToString { "${it.direction} at %.0f m".format(it.atMeters) },
      )

  private fun assertRide(name: String) {
    val actual = replay(name)
    val file = REAL_RIDES.resolve("$name.txt")
    if (!file.exists()) {
      file.writeText(actual)
      fail("Recorded $file, check it and run again")
    }
    val expected = file.readText()
    if (expected != actual) {
      val expectedLines = expected.lines()
      val actualLines = actual.lines()
      val first = expectedLines.indices.first { expectedLines[it] != actualLines.getOrNull(it) }
      assertEquals(expectedLines[first], actualLines.getOrNull(first), "$name, line ${first + 1}")
      assertEquals(expected, actual, name)
    }
  }

  @Test
  fun `every area reads as the apps read it`() {
    REAL_AREAS.listFiles()!!.filter { it.isDirectory }
      .forEach { directory ->
        val area = RealArea.load(directory.name)
        assertEquals("2026-11-08", area.paths.dataDate, directory.name)
        assertTrue(area.graph.connections.isNotEmpty(), directory.name)
        // Reading a square checks its checksum.
        area.graph.knots.values.forEach {
          area.paths.segments(PathSquare.of(it.latitude, it.longitude))
        }
      }
  }

  @Test fun `Titz 06 to 08 with a detour`() = assertRide("titz-06-08")

  @Test fun `Siegburg-Zange through 02`() = assertRide("siegburg-through-02")

  @Test fun `Siegburg-Zange going on beside the route`() = assertRide("siegburg-going-on")

  @Test
  fun `Siegburg-Zange with the way back behind the rider`() = assertRide("siegburg-way-behind")

  @Test fun `Rhine at Luelsdorf over the ferry`() = assertRide("rhine-ferry")

  @Test fun `Rhine at Luelsdorf visiting 83 twice`() = assertRide("rhine-83-twice")

  @Test
  fun `Rhine at Luelsdorf with coarse fixes and the screen off`() =
    assertRide("rhine-coarse-screen-off")

  @Test fun `Veluwe through the double knot 43`() = assertRide("veluwe-43")
}
