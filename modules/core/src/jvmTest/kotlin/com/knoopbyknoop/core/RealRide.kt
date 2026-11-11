package com.knoopbyknoop.core

import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/** Where the cut-outs and the rides of [RealRideTest] are, relative to `modules/core`. */
internal val REAL_AREAS = File("src/jvmTest/resources/real-areas")
internal val REAL_RIDES = File("src/jvmTest/resources/real-rides")

/**
 * A cut-out of the published cycling data, read as the apps read it: the graph, the courses
 * belonging to it by its checksum, and the path data whose squares are checked when read.
 */
internal class RealArea(
  val graph: NetworkGraph,
  val geometry: NetworkGeometry,
  val paths: PathFile,
) {

  val router = PathRouter(paths)

  fun knot(id: Long): Knot = graph.knots[id] ?: error("No knot $id in the area")

  companion object {
    fun load(name: String): RealArea {
      val directory = REAL_AREAS.resolve(name)
      val graphBytes = directory.resolve("cycling.graph").readBytes()
      val graph = GraphFile.read(graphBytes).graph
      val geometry =
        NetworkGeometry.read(
          directory.resolve("cycling.geometry").readBytes(),
          graph,
          sha256Hex(graphBytes),
        )
      val pathBytes = directory.resolve("cycling.paths").readBytes()
      val paths = PathFile.open { offset, size ->
        pathBytes.copyOfRange(offset.toInt(), offset.toInt() + size)
      }
      return RealArea(graph, geometry, paths)
    }
  }
}

/** A fix of a scripted ride, and whether the app is on screen then. */
internal class RideFix(val fix: Fix, val visible: Boolean)

/**
 * A scripted ride: the route's knots and the fixes of the rider, see the change
 * `replay-rides-on-real-data`. One command per line, `#` starts a comment:
 * - `area <name>`: the cut-out, see [RealArea]
 * - `route <knot id or latitude,longitude>...`: the route; a position is a route point there, made
 *   and joined to the network by a way along paths as planning does
 * - `start <latitude> <longitude>`: where the rider starts, the first knot if not given
 * - `along <from> <to>`: along the drawn courses from the route's knot at position `from` to the
 *   one at `to`
 * - `leg <leg> <from meters> <to meters>`: along the drawn course of leg `leg`, backwards if `from`
 *   is beyond `to`
 * - `path <latitude> <longitude>`: along the way the path router finds to there; `path start` to
 *   the start of the route
 * - `line <latitude> <longitude>`: straight to there
 * - `still <seconds>`: standing still
 * - `coarse <latitude> <longitude> [<accuracy>]`: one fix of [COARSE_ACCURACY] or the accuracy
 *   given there, the rider staying where they are
 * - `noise <meters>`: from here on, fixes scattered by up to that much, from a fixed seed
 * - `screen on|off`: whether the app is on screen, on at the start
 * - `fixes <file>`: the fixes in a file of `<longitude> <latitude>` lines, one second apart
 * - `nearby <latitude> <longitude>`: the plan started on the route nearby from there, as the apps
 *   cut it when the rider chooses to, see `knot-navigation` "Start on the route nearby"
 * - `clock <yyyy-mm-dd> <hh:mm>`: the local time at the first fix, for the ferries' operating times
 *   and the arrivals; without it, there are none
 */
internal class RideScript(
  val area: RealArea,
  val knots: List<Knot>,
  val fixes: List<RideFix>,
  val localTime: ((Long) -> LocalMoment)? = null,
  /** The network with the route's route points, as navigation gets it. */
  val graph: NetworkGraph = area.graph,
  /** The courses with the route's route points. */
  val geometry: NetworkGeometry = area.geometry,
) {

  companion object {
    /** The rider moves this far between fixes, see [SECONDS_PER_STEP]. */
    const val STEP_METERS = 10.0

    /** 10 m in 2 s: 18 km/h. */
    const val SECONDS_PER_STEP = 2L

    const val COARSE_ACCURACY = 80.0

    const val START_MILLIS = 1_794_300_000_000L

    fun read(file: File): RideScript = parse(file.readText(), file.parentFile)

    fun parse(text: String, directory: File = REAL_RIDES): RideScript {
      val commands =
        text
          .lines()
          .map { it.substringBefore('#').trim() }
          .filter { it.isNotEmpty() }
          .map { it.split(Regex("\\s+")) }
      val area = RealArea.load(commands.first { it[0] == "area" }[1])
      val nearby =
        commands
          .firstOrNull { it[0] == "nearby" }
          ?.let { GeoPoint(it[1].toDouble(), it[2].toDouble()) }
      val (knots, graph, geometry) =
        route(area, commands.first { it[0] == "route" }.drop(1), nearby)
      val clock =
        commands
          .firstOrNull { it[0] == "clock" }
          ?.let { (_, date, time) ->
            val (year, month, day) = date.split("-").map { it.toInt() }
            val (hours, minutes) = time.split(":").map { it.toInt() }
            CalendarDay(year, month, day) to hours * 60 + minutes
          }
      val localTime = clock?.let { (day, startMinute) ->
        { millis: Long ->
          val minutes = startMinute + ((millis - START_MILLIS) / 60_000).toInt()
          LocalMoment(day.plusDays(minutes / MINUTES_PER_DAY), minutes % MINUTES_PER_DAY)
        }
      }
      val fixes = Generator(area, knots, geometry, directory).run(commands)
      return RideScript(area, knots, fixes, localTime, graph, geometry)
    }

    /**
     * The route through [entries], knot ids or `latitude,longitude` of route points, planned as the
     * apps plan it, with the network and courses it is navigated on.
     */
    private fun route(
      area: RealArea,
      entries: List<String>,
      nearby: GeoPoint? = null,
    ): Triple<List<Knot>, NetworkGraph, NetworkGeometry> {
      if (entries.none { ',' in it } && nearby == null) {
        return Triple(entries.map { area.knot(it.toLong()) }, area.graph, area.geometry)
      }
      val empty = PlanningSession.start(area.graph)
      val waypoints = entries.map { entry ->
        if (',' !in entry) area.knot(entry.toLong())
        else {
          val (latitude, longitude) = entry.split(",").map { it.toDouble() }
          val offer =
            empty.tapAt(GeoPoint(latitude, longitude), 30.0, null, area.geometry) as MapTap.Offer
          empty.routePointFor(offer, area.geometry) ?: error("No route point at $entry")
        }
      }
      val session = PlanningSession.start(area.graph, waypoints)
      val cut =
        if (nearby == null) session
        else {
          val choice =
            StartChoice.of(session, area.geometry, nearby) ?: error("Not asked at $nearby")
          session.startOnRouteAt(choice.nearby)
        }
      val planned = cut.withWaysFrom(RoutePointWays(area.router, area.geometry), navigating = false)
      val knots = (planned.route!!.summary as RouteSummary.Complete).knots
      return Triple(knots, planned.routeGraph, area.geometry.withRoutePoints(planned.routeGraph))
    }
  }

  private class Generator(
    val area: RealArea,
    val knots: List<Knot>,
    val geometry: NetworkGeometry,
    val directory: File,
  ) {
    val fixes = mutableListOf<RideFix>()
    var position = GeoPoint(knots.first().latitude, knots.first().longitude)
    var time = START_MILLIS
    var noise = 0.0
    var visible = true
    val random = Random(42)
    val drawn by lazy { geometry.courses(knots) }

    fun run(commands: List<List<String>>): List<RideFix> {
      for (command in commands) {
        fun double(index: Int) = command[index].toDouble()
        fun point() = GeoPoint(double(1), double(2))
        when (command[0]) {
          "area",
          "route",
          "nearby",
          "clock" -> {}
          "start" -> {
            position = point()
            emit(position)
          }
          "along" ->
            (command[1].toInt() until command[2].toInt()).forEach { leg ->
              follow(drawn[leg])
            }
          "leg" -> {
            val course = Polyline(drawn[command[1].toInt()])
            follow(course.stretch(double(2), double(3)))
          }
          "path" -> {
            // `path start`: to the start of the route, e.g. a start point on the course.
            val to =
              if (command[1] == "start") GeoPoint(knots.first().latitude, knots.first().longitude)
              else point()
            val way =
              area.router.way(position, to) ?: error("No way from $position to $to in the area")
            follow(way.points)
          }
          "line" -> follow(listOf(position, point()))
          "still" -> repeat((command[1].toLong() / SECONDS_PER_STEP).toInt()) { emit(position) }
          "coarse" ->
            emit(
              point(),
              accuracy = command.getOrNull(3)?.toDouble() ?: COARSE_ACCURACY,
              scatter = false,
            )
          "noise" -> noise = double(1)
          "screen" -> visible = command[1] == "on"
          "fixes" ->
            directory.resolve(command[1]).readLines().forEach { line ->
              val (longitude, latitude) = line.trim().split(" ").map { it.toDouble() }
              position = GeoPoint(latitude, longitude)
              time += 1000
              fixes += RideFix(Fix(latitude, longitude, 5.0, time - 1000), visible)
            }
          else -> error("Unknown command: ${command.joinToString(" ")}")
        }
      }
      return fixes
    }

    /** Fixes every [STEP_METERS] along [points], from the first, which is the rider's position. */
    fun follow(points: List<GeoPoint>) {
      val line = Polyline(listOf(position) + points)
      val steps = max(1, (line.length / STEP_METERS).toInt())
      for (step in 1..steps) emit(line.pointAt(line.length * step / steps))
    }

    fun emit(point: GeoPoint, accuracy: Double = 5.0, scatter: Boolean = true) {
      if (accuracy <= 5.0) position = point
      val shown =
        if (scatter && noise > 0) {
          val meters = random.nextDouble() * noise
          val angle = random.nextDouble() * 2 * PI
          val margins = marginsAt(point.latitude, meters)
          GeoPoint(
            point.latitude + margins.latitude * sin(angle),
            point.longitude + margins.longitude * cos(angle),
          )
        } else point
      fixes += RideFix(Fix(shown.latitude, shown.longitude, accuracy, time), visible)
      time += SECONDS_PER_STEP * 1000
    }
  }
}
