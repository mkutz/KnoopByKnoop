package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Covers the ride scripts of [RealRideTest], on the Siegburg-Zange cut-out. */
class RideScriptTest {

  private val header =
    """
    area siegburg-zange
    route 81468719 59469043 1336813155
    """

  private fun ride(commands: String) = RideScript.parse(header + commands.trimIndent())

  private fun Fix.point() = GeoPoint(latitude, longitude)

  private fun assertSteps(fixes: List<RideFix>) =
    fixes.zipWithNext().forEach { (from, to) ->
      val meters = distanceMeters(from.fix.point(), to.fix.point())
      assertTrue(meters in 5.0..RideScript.STEP_METERS + 0.5, "$meters m between fixes")
      assertEquals(RideScript.SECONDS_PER_STEP * 1000, to.fix.timeMillis - from.fix.timeMillis)
    }

  @Test
  fun `along follows the drawn courses from knot to knot`() {
    val ride = ride("along 0 2")
    val courses = ride.area.geometry.courses(ride.knots)

    val fixes = ride.fixes
    val line = Polyline(courses.flatten())
    fixes.forEach { assertTrue(line.distanceTo(it.fix.point()) < 1.0, "on the courses") }
    assertSteps(fixes)
    assertTrue(distanceMeters(fixes.last().fix.point(), courses.last().last()) < 1.0)
  }

  @Test
  fun `leg goes along part of a leg and backwards`() {
    val forward = ride("leg 0 0 300").fixes
    val back = ride("leg 0 0 300\nleg 0 300 100").fixes.drop(forward.size)
    val course = Polyline(ride("").area.geometry.courses(ride("").knots)[0])

    assertEquals(300.0, course.nearest(forward.last().fix.point()).meters, 1.0)
    assertEquals(100.0, course.nearest(back.last().fix.point()).meters, 1.0)
    assertTrue(
      back.zipWithNext().all { (from, to) ->
        course.nearest(to.fix.point()).meters < course.nearest(from.fix.point()).meters
      }
    )
  }

  @Test
  fun `path follows the way the path router finds`() {
    val target = GeoPoint(50.7861, 7.1953)
    val ride = ride("leg 0 0 300\npath ${target.latitude} ${target.longitude}")
    val from = ride.fixes[29].fix.point()
    val way = Polyline(ride.area.router.way(from, target)!!.points)

    ride.fixes.drop(30).forEach { assertTrue(way.distanceTo(it.fix.point()) < 1.0, "on the way") }
    assertTrue(distanceMeters(ride.fixes.last().fix.point(), way.points.last()) < 1.0)
  }

  @Test
  fun `line goes straight and still stays`() {
    val ride = ride("start 50.7800 7.2200\nline 50.7810 7.2200\nstill 10")

    assertSteps(ride.fixes.take(12).drop(1))
    val last = ride.fixes.takeLast(5).map { it.fix.point() }
    assertEquals(1, last.distinct().size, "standing still")
    assertEquals(GeoPoint(50.7810, 7.2200), last.first())
  }

  @Test
  fun `coarse gives one inaccurate fix and the rider goes on from where they were`() {
    val ride = ride("start 50.7800 7.2200\ncoarse 50.7850 7.2300\nline 50.7801 7.2200")

    assertEquals(RideScript.COARSE_ACCURACY, ride.fixes[1].fix.accuracyMeters)
    assertEquals(GeoPoint(50.7850, 7.2300), ride.fixes[1].fix.point())
    assertTrue(distanceMeters(ride.fixes[2].fix.point(), GeoPoint(50.7801, 7.22)) < 1.0)
  }

  @Test
  fun `noise scatters the fixes the same way every time`() {
    val script = "start 50.7800 7.2200\nnoise 4\nline 50.7850 7.2200\nscreen off\nstill 4"
    val first = ride(script).fixes
    val second = ride(script).fixes

    assertEquals(first.map { it.fix }, second.map { it.fix })
    val straight = ride("start 50.7800 7.2200\nline 50.7850 7.2200").fixes
    val offsets =
      first.drop(1).zip(straight.drop(1)).map { (scattered, plain) ->
        distanceMeters(scattered.fix.point(), plain.fix.point())
      }
    assertTrue(offsets.all { it <= 4.01 } && offsets.any { it > 1.0 }, "$offsets")
    assertEquals(listOf(false, false), first.takeLast(2).map { it.visible })
  }
}
