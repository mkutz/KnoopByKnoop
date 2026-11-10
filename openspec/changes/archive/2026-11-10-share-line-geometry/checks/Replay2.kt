import com.knoopbyknoop.core.*
import java.io.File
import java.io.RandomAccessFile

// Replays the same fixes through the old app loop and through NavigationSession, answering each
// way search at once with the real path data; prints what each said, for a diff.
fun main(args: Array<String>) {
  val dir = File(args[0])
  val graphBytes = File(dir, "cycling.graph").readBytes()
  val graph = GraphFile.read(graphBytes).graph
  val geometry = NetworkGeometry.read(File(dir, "cycling.geometry").readBytes(), graph, sha256Hex(graphBytes))
  val access = RandomAccessFile(File(dir, "cycling.paths"), "r")
  val paths = PathFile.open { offset, size -> access.seek(offset); ByteArray(size).also(access::readFully) }
  val knots = listOf(graph.knots.getValue(427779798), graph.knots.getValue(2518992736))
  val fixes = File(args[1]).readLines().mapIndexed { index, line ->
    val (lon, lat) = line.trim().split(" ").map { it.toDouble() }
    Fix(lat, lon, 5.0, 1_794_300_000_000L + index * 1000L)
  }
  val mode = args[2]
  val out = ArrayList<String>()
  if (mode == "old") {
    // As NavigationService did before the change.
    val router = PathRouter(paths)
    val guide = WayGuide()
    var navigation: Navigation? = null
    var track = RiddenTrack()
    var stopping = false
    for (fix in fixes) {
      if (stopping) break
      val update = navigation?.update(fix, true) ?: Navigation.start(knots, graph, geometry, fix, true)!!
      navigation = update.navigation
      val step = guide.onFix(update.navigation, update.notices, fix, graph.network, track.directionAt(fix), fix.timeMillis, Language.EN)
      step.notices.forEach { out += "notice: ${it.text}" }
      graph.network?.takeIf { it != track.network }?.let { track = track.copy(network = it) }
      track = track.add(fix)
      if (update.navigation.arrived) { stopping = true; out += "end after speech"; continue }
      out += "precision: ${update.precision}"
      step.search?.let { search ->
        val way = router.way(search.from, search.to, search.heading)
        out += "way: ${way?.lengthMeters?.toInt()} m, ${way?.turns?.size} turns"
        val latest = navigation
        guide.found(way, latest)
        if (latest.offRoute && !latest.arrived) out += "shown: ${guide.way?.let { "way" }}"
      }
    }
  } else {
    val finder = PathRouter(paths)
    val session = NavigationSession(NavigationRoute(knots, graph, geometry))
    for (fix in fixes) {
      val step = session.onFix(fix, true, fix.timeMillis)
      step.notices.forEach { out += "notice: ${it.text}" }
      step.state?.navigation?.let { navigation ->
        val state = step.state!!
        val strip = navigation.progress(fix, state.guidedWay).strip
        out += "line: ${navigation.progressLine} | ${navigation.display.text} | rider %.3f | way %s".format(
          strip.rider, state.guidedWay?.view(navigation.language)?.line ?: "-")
      }
      if (step.end == SessionEnd.AFTER_SPEECH) { out += "end after speech"; break }
      out += "precision: ${step.precision}"
      step.search?.let { search ->
        val way = finder.way(search.from, search.to, search.heading)
        out += "way: ${way?.lengthMeters?.toInt()} m, ${way?.turns?.size} turns"
        val shown = session.onWay(way)
        if (shown.state != null) out += "shown: ${shown.state!!.guidedWay?.let { "way" }}"
      }
    }
  }
  out.forEach(::println)
}
