package com.knoopbyknoop.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Which way a turn goes. [STRAIGHT] is only given for a junction with several small turns. */
enum class TurnDirection(
  /** How far an arrow for it turns from straight ahead, clockwise, as the apps draw it. */
  val degrees: Double
) {
  STRAIGHT(0.0),
  SLIGHT_LEFT(-45.0),
  LEFT(-90.0),
  SHARP_LEFT(-135.0),
  SLIGHT_RIGHT(45.0),
  RIGHT(90.0),
  SHARP_RIGHT(135.0),
  BACK(180.0),
}

/**
 * A turn on a [PathWay], see `knot-navigation` "Turn notices on the guided way".
 *
 * @property atMeters where it is, along the way
 * @property name the way it turns into, if known
 * @property switchTo the kind of way it switches to, see `knot-navigation` "Switching between
 *   street and path on the guided way"; [direction] is [TurnDirection.STRAIGHT] for a switch
 *   without a turn
 */
data class PathTurn(
  val atMeters: Double,
  val direction: TurnDirection,
  val name: String?,
  val switchTo: PathSwitch? = null,
  /** The doubtful junction here, if it is one, see `knot-navigation` "Doubtful junctions". */
  val doubtful: DoubtfulJunction? = null,
)

/** The kind of way a [PathTurn] switches to, between streets and paths. */
enum class PathSwitch {
  STREET,
  CYCLE_PATH,
  PATH;

  companion object {
    private fun isStreet(kind: PathKind) =
      kind == PathKind.MAJOR || kind == PathKind.MAIN || kind == PathKind.STREET

    /** The switch from a way of kind [from] to one of kind [to], if it is one. */
    fun of(from: PathKind, to: PathKind): PathSwitch? =
      when {
        isStreet(from) && !isStreet(to) -> if (to == PathKind.CYCLE) CYCLE_PATH else PATH
        !isStreet(from) && isStreet(to) -> STREET
        else -> null
      }
  }
}

/** Why a junction on a guided way is doubtful, see `knot-navigation` "Doubtful junctions". */
enum class JunctionDoubt {
  /** Another exit leaves within [PathRouter.SIMILAR_EXIT_DEGREES] of the one taken. */
  SIMILAR_EXIT,

  /** The way crosses a main road, without riding along it. */
  MAIN_ROAD,

  /** The way leaves a street or road for a cycle path or another path. */
  OFF_ROAD,
}

/**
 * A doubtful junction on a guided way, or several within [PathRouter.MERGE_METERS] of each other,
 * see `knot-navigation` "Doubtful junctions".
 *
 * @property startMeters where its first junction is, along the way
 * @property endMeters where its last junction is
 * @property keep at a similar exit, the side to keep to, [TurnDirection.LEFT] or
 *   [TurnDirection.RIGHT]
 * @property offRoad leaving the road, whether for a cycle path or another path
 */
data class DoubtfulJunction(
  val startMeters: Double,
  val endMeters: Double,
  val doubts: Set<JunctionDoubt>,
  val keep: TurnDirection? = null,
  val offRoad: PathSwitch? = null,
  /** Its drawing, see `knot-navigation` "Junction picture". */
  val picture: JunctionPicture? = null,
) {
  /** What the sentence for it is about: a main road before leaving the road before a fork. */
  val main: JunctionDoubt
    get() =
      listOf(JunctionDoubt.MAIN_ROAD, JunctionDoubt.OFF_ROAD, JunctionDoubt.SIMILAR_EXIT).first {
        it in doubts
      }
}

/**
 * A way found by [PathRouter].
 *
 * @property points its course from the given start to the given target
 * @property meters for each of [points], how far along the way it is
 * @property startsBack whether it starts against the riding direction, see `path-routing` "Starting
 *   in the riding direction"
 * @property firstName the name of the way it starts on, if known, e.g. for the turn off the network
 *   into it, see `knot-navigation` "Guidance to a route point off the network"
 * @property doubtful its doubtful junctions, see `knot-navigation` "Doubtful junctions"
 * @property decisionMeters where it passes a junction with a choice of ways, for measuring
 */
class PathWay(
  val points: List<GeoPoint>,
  val meters: List<Double>,
  val turns: List<PathTurn>,
  val startsBack: Boolean = false,
  val firstName: String? = null,
  val doubtful: List<DoubtfulJunction> = listOf(),
  internal val decisionMeters: List<Double> = listOf(),
) {

  val lengthMeters: Double
    get() = meters.lastOrNull() ?: 0.0
}

/**
 * Finds ways for a bicycle from a [PathFile] on the device, see `path-routing`. Squares are loaded
 * as the search reaches them and stay loaded until there are too many, see "Memory on a long ride".
 * Not safe for use from several threads at once.
 */
class PathRouter(private val file: PathSquareSource) {

  /** A [segment] ridden from its start, or from its end when [reversed]. */
  private class Step(val segment: PathSegment, val reversed: Boolean) {
    val toId: Long = if (reversed) segment.fromId else segment.toId
    val cost: Double = segment.lengthMeters * segment.costFactor
    val end: GeoPoint = if (reversed) segment.points.first() else segment.points.last()

    fun points(): List<GeoPoint> = if (reversed) segment.points.asReversed() else segment.points
  }

  /**
   * Reaching [step] for [cost], coming from [previous]; a label is only ever improved by a new one.
   */
  private class Label(val step: Step, val cost: Double, val previous: Label?, val start: Partial?)

  /** The part of a segment from or to a place on it, with what it costs. */
  private class Partial(
    val points: List<GeoPoint>,
    val cost: Double,
    val step: Step,
    /** Where the place is on [step], in meters from the start of the step. */
    val along: Double = 0.0,
    val place: GeoPoint? = null,
    val offsetCost: Double = 0.0,
  )

  private val loaded = HashSet<PathSquare>()
  private val segmentsBySquare = HashMap<PathSquare, List<PathSegment>>()
  private val stepsByNode = HashMap<Long, ArrayList<Step>>()

  /** Every segment at a node, also those only for drawing, e.g. a motorway being crossed. */
  private val segmentsByNode = HashMap<Long, ArrayList<PathSegment>>()

  /** How many squares are loaded, to measure the memory used. */
  val loadedSquares: Int
    get() = loaded.size

  /** How many steps the last search looked at, to measure the time used. */
  var visitedSteps: Int = 0
    private set

  /**
   * The way from [from] to [to], or `null` if the squares between them don't connect them or the
   * search fails, see `path-routing` "No way found". Never throws, as an exception would end the
   * app on iOS.
   *
   * @param heading the riding direction, clockwise from north, if known
   */
  fun way(from: GeoPoint, to: GeoPoint, heading: Double? = null): PathWay? =
    try {
      searchWay(from, to, heading)
    } catch (exception: Exception) {
      // Some squares may be half loaded; they are read again with the next search, see
      // `path-routing` "Broken path data".
      forgetSquares()
      null
    }

  private fun searchWay(from: GeoPoint, to: GeoPoint, heading: Double?): PathWay? {
    if (loaded.size > MAX_KEPT_SQUARES) forgetSquares()
    val starts = candidates(from, leaving = true)
    val targets = candidates(to, leaving = false)
    if (starts.isEmpty() || targets.isEmpty()) return null
    // On a path, the way starts there; further ones are for when it is a stray piece, see
    // `path-routing` "Connecting to the paths".
    val closest = starts.minOf { it.offsetCost }
    val close = starts.filter { it.offsetCost <= closest + CLOSE_STARTS_METERS * OFFSET_COST }
    return search(from, to, heading, close, targets)
      ?: if (close.size < starts.size) search(from, to, heading, starts, targets) else null
  }

  private fun search(
    from: GeoPoint,
    to: GeoPoint,
    heading: Double?,
    starts: List<Partial>,
    targets: List<Partial>,
  ): PathWay? {
    val bounds = SearchBounds(from, to)
    val targetsByNode = HashMap<Long, MutableList<Partial>>()
    targets.forEach { target ->
      val node = if (target.step.reversed) target.step.segment.toId else target.step.segment.fromId
      targetsByNode.getOrPut(node) { mutableListOf() }.add(target)
    }

    val best = HashMap<Step, Double>()
    val queue = MinHeap<Label>()
    var result: Pair<Label?, Partial?>? = null
    var resultCost = Double.MAX_VALUE
    // On the same segment as the target and riding towards it: no junction in between.
    starts.forEach { start ->
      targets.forEach { target ->
        val direct = direct(start, target)
        val directCost = direct?.let { it.cost + startingCost(from, start, heading) }
        if (direct != null && directCost != null && directCost < resultCost) {
          resultCost = directCost
          result = Label(start.step, directCost, null, direct) to null
        }
      }
    }
    starts.forEach { start ->
      val cost = start.cost + startingCost(from, start, heading)
      queue.add(cost + estimate(start.step.end, to), Label(start.step, cost, null, start))
    }
    visitedSteps = 0
    while (true) {
      val entry = queue.poll() ?: break
      if (entry.priority >= resultCost) break
      val label = entry.value
      val known = best[label.step]
      if (known != null && known <= label.cost) continue
      best[label.step] = label.cost
      visitedSteps++
      if (visitedSteps > MAX_VISITED_STEPS) break
      val node = label.step.toId
      load(label.step.end)
      targetsByNode[node]?.forEach { target ->
        val cost = label.cost + target.cost + turnCost(from, label, target.step, heading)
        if (cost < resultCost) {
          resultCost = cost
          result = label to target
        }
      }
      for (next in stepsByNode[node].orEmpty()) {
        if (next.segment === label.step.segment) continue
        if (!bounds.contains(next.end)) continue
        val cost = label.cost + next.cost + turnCost(from, label, next, heading)
        val nextKnown = best[next]
        if (nextKnown != null && nextKnown <= cost) continue
        queue.add(cost + estimate(next.end, to), Label(next, cost, label, null))
      }
    }
    val (lastLabel, target) = result ?: return null
    return assemble(from, to, lastLabel!!, target, heading)
  }

  private fun assemble(
    from: GeoPoint,
    to: GeoPoint,
    last: Label,
    target: Partial?,
    heading: Double?,
  ): PathWay {
    val labels = generateSequence(last) { it.previous }.toList().asReversed()
    val first = labels.first()
    val pieces = ArrayList<Piece>()
    if (first.start != null) pieces.add(Piece(first.start.points, first.step, null))
    labels.drop(1).forEach {
      pieces.add(Piece(it.step.points(), it.step, it.step.segment.fromOf(it.step)))
    }
    if (target != null) {
      pieces.add(Piece(target.points, target.step, target.step.segment.fromOf(target.step)))
    }
    val points = ArrayList<GeoPoint>()
    val meters = ArrayList<Double>()
    fun add(point: GeoPoint) {
      val previous = points.lastOrNull()
      if (previous == point) return
      val before = meters.lastOrNull() ?: 0.0
      meters.add(
        before +
          (previous?.let {
            distanceMeters(it.latitude, it.longitude, point.latitude, point.longitude)
          } ?: 0.0)
      )
      points.add(point)
    }
    add(from)
    val junctions = ArrayList<Junction>()
    val doubts = ArrayList<DoubtfulJunction>()
    pieces.forEachIndexed { index, piece ->
      if (index > 0) {
        val previous = pieces[index - 1]
        val node = piece.fromNode ?: 0L
        doubtsAt(meters.last(), node, previous.step, piece.step)?.let { doubts.add(it) }
        junctions.add(
          Junction(
            meters.last(),
            piece.step.segment.name,
            exitsAt(piece.fromNode ?: 0L, previous.step),
            isPlainBend(points, piece.fromNode ?: 0L, previous.step, piece.step),
            previous.step.segment.kind,
            piece.step.segment.kind,
          )
        )
      }
      piece.points.forEach(::add)
    }
    add(to)
    val waySegments = pieces.map { it.step.segment }
    val doubtful =
      merged(doubts).map { it.copy(picture = pictureOf(it, points, meters, waySegments)) }
    return PathWay(
      points,
      meters,
      withDoubts(turns(points, meters, junctions), doubtful),
      startsBack(points, first.start?.points?.firstOrNull(), heading),
      pieces.firstOrNull()?.step?.segment?.name,
      doubtful,
      junctions.filter { it.exits >= 2 && it.atMeters > 0.0 }.map { it.atMeters },
    )
  }

  /** Whether the way along [points] starts against [heading], joining the paths at [onPaths]. */
  private fun startsBack(points: List<GeoPoint>, onPaths: GeoPoint?, heading: Double?): Boolean {
    if (heading == null || points.size < 2) return false
    val along = points.drop(points.indexOf(onPaths).coerceAtLeast(0))
    return along.size >= 2 && setsOffBack(points.first(), along, heading)
  }

  /**
   * Whether going from [from] to the first of [along], or on along them, goes against [heading]. A
   * path close by counts by its own direction: the way there is only the inaccuracy of the fix.
   */
  private fun setsOffBack(from: GeoPoint, along: List<GeoPoint>, heading: Double): Boolean {
    val onPath = along.first()
    val connection =
      distanceMeters(from.latitude, from.longitude, onPath.latitude, onPath.longitude)
    return (connection >= SHORT_START_METERS && isTurningBack(heading, bearing(from, onPath))) ||
      (lengthOf(along) >= SHORT_START_METERS && isTurningBack(heading, headingAlong(along)))
  }

  /**
   * Whether the way at [node] just continues: all others turn clearly more, so there is nothing to
   * tell the rider, see `knot-navigation` "Turn notices on the guided way".
   */
  private fun isPlainBend(
    arrived: List<GeoPoint>,
    node: Long,
    arriving: Step,
    taken: Step,
  ): Boolean {
    val inbound = bearing(firstPointBeyond(arrived.asReversed()), arrived.last())
    fun turnOf(step: Step) = abs(signedAngle(inbound, headingAlong(step.points())))
    val takenTurn = turnOf(taken)
    return takenTurn < TURN_DEGREES &&
      stepsByNode[node].orEmpty().all {
        it.segment === arriving.segment ||
          it.segment === taken.segment ||
          turnOf(it) - takenTurn >= CLEAR_CHOICE_DEGREES
      }
  }

  /**
   * Why the junction at [node] is doubtful when arriving on [arriving] and taking [taken], see
   * `knot-navigation` "Doubtful junctions"; empty for a plain one.
   */
  private fun doubtsAt(at: Double, node: Long, arriving: Step, taken: Step): DoubtfulJunction? {
    val doubts = mutableSetOf<JunctionDoubt>()
    val takenHeading = headingAlong(taken.points())
    val others =
      stepsByNode[node].orEmpty().filter {
        it.segment !== arriving.segment && it.segment !== taken.segment
      }
    val similar =
      others
        .map { signedAngle(takenHeading, headingAlong(it.points())) }
        .filter { abs(it) <= SIMILAR_EXIT_DEGREES }
        .minByOrNull { abs(it) }
    // The other exit to the right means keeping left.
    val keep = similar?.let { if (it > 0) TurnDirection.LEFT else TurnDirection.RIGHT }
    if (similar != null) doubts += JunctionDoubt.SIMILAR_EXIT
    fun isMainRoad(kind: PathKind) = kind == PathKind.MAJOR || kind == PathKind.MAIN
    val mainRoads =
      segmentsByNode[node].orEmpty().count {
        isMainRoad(it.kind) && it !== arriving.segment && it !== taken.segment
      }
    // A main road passing through, which the way neither comes along nor goes on.
    if (mainRoads >= 2 && !isMainRoad(arriving.segment.kind) && !isMainRoad(taken.segment.kind)) {
      doubts += JunctionDoubt.MAIN_ROAD
    }
    val switch = PathSwitch.of(arriving.segment.kind, taken.segment.kind)
    val offRoad = switch?.takeIf { it != PathSwitch.STREET }
    if (offRoad != null) doubts += JunctionDoubt.OFF_ROAD
    return if (doubts.isEmpty()) null else DoubtfulJunction(at, at, doubts, keep, offRoad)
  }

  /**
   * [turns] with the doubtful junctions on them; a doubtful junction without a turn, e.g. crossing
   * a main road straight on, becomes one.
   */
  private fun withDoubts(
    turns: List<PathTurn>,
    doubtful: List<DoubtfulJunction>,
  ): List<PathTurn> {
    val result = turns.toMutableList()
    for (junction in doubtful) {
      val index = result.indexOfFirst {
        it.atMeters >= junction.startMeters - 1 && it.atMeters <= junction.endMeters + MERGE_METERS
      }
      if (index >= 0) {
        result[index] = result[index].copy(doubtful = junction)
      } else {
        result += PathTurn(junction.startMeters, TurnDirection.STRAIGHT, null, doubtful = junction)
      }
    }
    return result.sortedBy { it.atMeters }
  }

  /**
   * The drawing of [junction] on the way through [points], see `knot-navigation` "Junction
   * picture": the way before and after it, the main roads, and the other streets and cycle paths
   * close by, with the riding direction up.
   */
  private fun pictureOf(
    junction: DoubtfulJunction,
    points: List<GeoPoint>,
    meters: List<Double>,
    waySegments: List<PathSegment>,
  ): JunctionPicture {
    val start = junction.startMeters
    val line = Polyline(points, meters)
    val center = line.pointAt(start)
    val from = line.pointAt(start - JunctionPicture.DIRECTION_FROM_METERS)
    val to = line.pointAt(start - JunctionPicture.DIRECTION_TO_METERS)
    val ahead = if (from == to) line.pointAt(start + HEADING_METERS) else to
    val heading = bearing(if (from == to) center else from, ahead)
    val frame = PictureFrame(center, heading)
    val view = PICTURE_VIEW_METERS
    val lines = mutableListOf<PictureLine>()
    frame.lines(line.stretch(start - JunctionPicture.BEFORE_METERS, start), view).forEach {
      lines += PictureLine(PictureLineKind.WAY_BEFORE, it)
    }
    frame
      .lines(
        line.stretch(start, junction.endMeters + JunctionPicture.AFTER_METERS),
        view,
      )
      .forEach { lines += PictureLine(PictureLineKind.WAY_AFTER, it) }
    val square = PathSquare.of(center.latitude, center.longitude)
    // Only segments with a point near the junction, checked cheaply first: the squares hold
    // thousands of them, and comparing them all with each other took minutes.
    val reach = marginsAt(center.latitude, PICTURE_REACH_METERS)
    val latitudeReach = reach.latitude
    val longitudeReach = reach.longitude
    fun isNear(segment: PathSegment) =
      segment.points.any {
        abs(it.latitude - center.latitude) <= latitudeReach &&
          abs(it.longitude - center.longitude) <= longitudeReach
      } || crossesNear(segment, center, latitudeReach, longitudeReach)
    val nearby = mutableListOf<PathSegment>()
    (squaresAround(center) + square).forEach { around ->
      segmentsBySquare[around].orEmpty().forEach { segment ->
        if (
          isNear(segment) &&
            nearby.none { it.isSameAs(segment) } &&
            waySegments.none { it.isSameAs(segment) }
        ) {
          nearby += segment
        }
      }
    }
    for (segment in nearby) {
      val (kind, radius) =
        when (segment.kind) {
          PathKind.MAJOR,
          PathKind.MAIN -> PictureLineKind.MAIN_ROAD to view
          PathKind.STREET -> PictureLineKind.STREET to JunctionPicture.EXIT_METERS
          PathKind.CYCLE -> PictureLineKind.CYCLE_PATH to JunctionPicture.EXIT_METERS
          PathKind.PATH -> continue
        }
      frame.lines(segment.points, radius).forEach { lines += PictureLine(kind, it) }
    }
    // Roads first, so the way is drawn over them.
    val order =
      listOf(
        PictureLineKind.MAIN_ROAD,
        PictureLineKind.STREET,
        PictureLineKind.CYCLE_PATH,
        PictureLineKind.WAY_BEFORE,
        PictureLineKind.WAY_AFTER,
      )
    return JunctionPicture(lines.sortedBy { order.indexOf(it.kind) })
  }

  /**
   * Whether a straight piece of [segment] passes the box around [center], for long segments whose
   * points are all outside it, e.g. a motorway crossed between two far points.
   */
  private fun crossesNear(
    segment: PathSegment,
    center: GeoPoint,
    latitudeReach: Double,
    longitudeReach: Double,
  ): Boolean =
    segment.points.zipWithNext().any { (from, to) ->
      val minLatitude = minOf(from.latitude, to.latitude) - latitudeReach
      val maxLatitude = maxOf(from.latitude, to.latitude) + latitudeReach
      val minLongitude = minOf(from.longitude, to.longitude) - longitudeReach
      val maxLongitude = maxOf(from.longitude, to.longitude) + longitudeReach
      center.latitude in minLatitude..maxLatitude &&
        center.longitude in minLongitude..maxLongitude &&
        distanceMeters(
          center.latitude,
          center.longitude,
          nearestOnLine(center, from, to).latitude,
          nearestOnLine(center, from, to).longitude,
        ) <= PICTURE_REACH_METERS
    }

  /** [doubts] at junctions along the way, those within [MERGE_METERS] of each other as one. */
  private fun merged(doubts: List<DoubtfulJunction>): List<DoubtfulJunction> {
    val result = ArrayList<DoubtfulJunction>()
    for (junction in doubts) {
      val last = result.lastOrNull()
      if (last != null && junction.startMeters - last.endMeters <= MERGE_METERS) {
        result[result.lastIndex] =
          last.copy(
            endMeters = junction.endMeters,
            doubts = last.doubts + junction.doubts,
            keep = last.keep ?: junction.keep,
            offRoad = last.offRoad ?: junction.offRoad,
          )
      } else {
        result += junction
      }
    }
    return result
  }

  /** The direction [points] set off in, over about [HEADING_METERS]. */
  private fun headingAlong(points: List<GeoPoint>) =
    bearing(points.first(), firstPointBeyond(points))

  /**
   * The first of [points] at least [HEADING_METERS] from the first, else the last: a point of the
   * way itself, not one between two of them, unlike [Polyline.pointAt].
   */
  private fun firstPointBeyond(points: List<GeoPoint>): GeoPoint {
    var meters = 0.0
    points.zipWithNext().forEach { (from, to) ->
      meters += distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)
      if (meters >= HEADING_METERS) return to
    }
    return points.last()
  }

  private fun PathSegment.fromOf(step: Step): Long = if (step.reversed) toId else fromId

  private class Piece(val points: List<GeoPoint>, val step: Step, val fromNode: Long?)

  private class Junction(
    val atMeters: Double,
    val name: String?,
    val exits: Int,
    val plainBend: Boolean,
    val fromKind: PathKind,
    val toKind: PathKind,
  )

  /** The ways on at a junction other than back along [arriving], for telling turns from bends. */
  private fun exitsAt(node: Long, arriving: Step): Int =
    stepsByNode[node].orEmpty().count { it.segment !== arriving.segment }

  /** The turns at [junctions], see `knot-navigation` "Turn notices on the guided way". */
  private fun turns(
    points: List<GeoPoint>,
    meters: List<Double>,
    junctions: List<Junction>,
  ): List<PathTurn> {
    val line = Polyline(points, meters)
    fun at(position: Double) = line.pointAt(position)
    fun angle(fromMeters: Double, toMeters: Double): Double {
      val inbound = bearing(at(fromMeters - HEADING_METERS), at(fromMeters))
      val outbound = bearing(at(toMeters), at(toMeters + HEADING_METERS))
      return signedAngle(inbound, outbound)
    }
    val decisions = junctions.filter { it.exits >= 2 && it.atMeters > 0.0 }
    val turns = ArrayList<PathTurn>()
    var index = 0
    while (index < decisions.size) {
      var last = index
      while (
        last + 1 < decisions.size &&
          decisions[last + 1].atMeters - decisions[last].atMeters <= MERGE_METERS
      ) {
        last++
      }
      val firstMeters = decisions[index].atMeters
      val lastMeters = decisions[last].atMeters
      val angle = angle(firstMeters, lastMeters)
      val realTurns =
        (index..last).count {
          abs(angle(decisions[it].atMeters, decisions[it].atMeters)) >= STRAIGHT_DEGREES
        }
      val switchTo = PathSwitch.of(decisions[index].fromKind, decisions[last].toKind)
      val turn =
        if (index == last && decisions[index].plainBend) null else directionOf(angle, realTurns)
      // A switch is said even where the way goes on without a turn, see "Switching between street
      // and path on the guided way".
      val direction = turn ?: if (switchTo != null) TurnDirection.STRAIGHT else null
      if (direction != null) {
        turns.add(PathTurn(firstMeters, direction, decisions[last].name, switchTo))
      }
      index = last + 1
    }
    return turns
  }

  private fun directionOf(angle: Double, realTurns: Int): TurnDirection? {
    val direction = turnOf(angle)
    return if (direction == TurnDirection.STRAIGHT && realTurns < 2) null else direction
  }

  /** The way from [start] to [target] when both lie on one segment in the right order. */
  private fun direct(start: Partial, target: Partial): Partial? {
    if (start.step !== target.step || target.along < start.along) return null
    val startPlace = start.place ?: return null
    val targetPlace = target.place ?: return null
    // The course's points between them come from what's left after the start and before the target.
    val between = start.points.drop(1).filter { point -> target.points.any { it == point } }
    val points = listOf(startPlace) + between + targetPlace
    val length = target.along - start.along
    return Partial(
      points,
      length * start.step.segment.costFactor + start.offsetCost + target.offsetCost,
      start.step,
    )
  }

  /**
   * The ways near [place] to start on or end on, each as the part between [place] and one of the
   * segment's ends. Several, so a stray piece of path without a connection doesn't strand the
   * search, see `path-routing` "Connecting to the paths".
   */
  private fun candidates(place: GeoPoint, leaving: Boolean): List<Partial> {
    load(place)
    val nearby = ArrayList<Pair<Double, PathSegment>>()
    val seen = HashSet<PathSegment>()
    squaresAround(place).forEach { square ->
      load(square)
      segmentsBySquare[square].orEmpty().forEach { segment ->
        if ((segment.forward || segment.backward) && seen.add(segment)) {
          val distance = nearestOn(segment, place).distance
          if (distance <= CONNECT_METERS) nearby.add(distance to segment)
        }
      }
    }
    return nearby
      .sortedBy { it.first }
      .take(MAX_CANDIDATES)
      .flatMap { (_, segment) ->
        val nearest = nearestOn(segment, place)
        listOfNotNull(
          partial(segment, nearest, reversed = false, leaving),
          partial(segment, nearest, reversed = true, leaving),
        )
      }
  }

  private fun partial(
    segment: PathSegment,
    nearest: Nearest,
    reversed: Boolean,
    leaving: Boolean,
  ): Partial? {
    if (if (reversed) !segment.backward else !segment.forward) return null
    val step = stepsOf(segment, reversed) ?: return null
    val course = step.points()
    val nearestIndex = if (reversed) course.size - 2 - nearest.index else nearest.index
    val before = course.subList(0, nearestIndex + 1) + nearest.point
    val along = lengthOf(before)
    val points =
      if (leaving) listOf(nearest.point) + course.subList(nearestIndex + 1, course.size) else before
    val offsetCost = nearest.distance * OFFSET_COST
    return Partial(
      points,
      lengthOf(points) * segment.costFactor + offsetCost,
      step,
      along,
      nearest.point,
      offsetCost,
    )
  }

  private fun stepsOf(segment: PathSegment, reversed: Boolean): Step? {
    val node = if (reversed) segment.toId else segment.fromId
    return stepsByNode[node]?.firstOrNull { it.segment === segment && it.reversed == reversed }
  }

  private class Nearest(val index: Int, val point: GeoPoint, val distance: Double)

  private fun nearestOn(segment: PathSegment, place: GeoPoint): Nearest {
    var best: Nearest? = null
    for (index in 0 until segment.points.size - 1) {
      val point = nearestOnLine(place, segment.points[index], segment.points[index + 1])
      val distance =
        distanceMeters(place.latitude, place.longitude, point.latitude, point.longitude)
      if (best == null || distance < best.distance) best = Nearest(index, point, distance)
    }
    return best!!
  }

  /**
   * The cost of setting off along [start], see `path-routing` "Starting in the riding direction".
   */
  private fun startingCost(from: GeoPoint, start: Partial, heading: Double?): Double {
    if (heading == null) return 0.0
    return if (setsOffBack(from, start.points, heading)) TURNING_BACK_COST else 0.0
  }

  /** The cost of going on from [label] along [to], from the riding direction right at the start. */
  private fun turnCost(from: GeoPoint, label: Label, to: Step, heading: Double?): Double {
    val start = label.start
    if (start != null && heading != null && lengthOf(start.points) < SHORT_START_METERS) {
      return if (setsOffBack(from, listOf(start.points.first()) + to.points(), heading)) {
        TURNING_BACK_COST
      } else 0.0
    }
    return turnCost(label.step, to)
  }

  private fun isTurningBack(from: Double, to: Double) =
    abs(signedAngle(from, to)) > TURNING_BACK_DEGREES

  /** The cost of turning back on oneself, see `path-routing` "No needless turning back". */
  private fun turnCost(from: Step, to: Step): Double {
    val inbound = from.points()
    val outbound = to.points()
    if (inbound.size < 2 || outbound.size < 2) return 0.0
    val angle =
      signedAngle(
        bearing(inbound[inbound.size - 2], inbound.last()),
        bearing(outbound.first(), outbound[1]),
      )
    return if (abs(angle) > TURNING_BACK_DEGREES) TURNING_BACK_COST else 0.0
  }

  private fun estimate(from: GeoPoint, to: GeoPoint) =
    distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)

  /** Frees the loaded squares; they are read again when needed. */
  fun forgetSquares() {
    loaded.clear()
    segmentsBySquare.clear()
    stepsByNode.clear()
    segmentsByNode.clear()
  }

  private fun load(place: GeoPoint) = load(PathSquare.of(place.latitude, place.longitude))

  private fun load(square: PathSquare) {
    if (!loaded.add(square)) return
    val segments = file.segments(square)
    segmentsBySquare[square] = segments
    segments.forEach { segment ->
      if (segment.forward) addStep(segment, reversed = false)
      if (segment.backward) addStep(segment, reversed = true)
      for (node in listOf(segment.fromId, segment.toId)) {
        val atNode = segmentsByNode.getOrPut(node) { ArrayList(STEPS_PER_NODE) }
        if (atNode.none { it.isSameAs(segment) }) atNode.add(segment)
      }
    }
  }

  /** A segment comes with the squares of both of its ends, so it's only added once. */
  private fun addStep(segment: PathSegment, reversed: Boolean) {
    val node = if (reversed) segment.toId else segment.fromId
    val steps = stepsByNode.getOrPut(node) { ArrayList(STEPS_PER_NODE) }
    if (steps.none { it.segment.isSameAs(segment) && it.reversed == reversed }) {
      steps.add(Step(segment, reversed))
    }
  }

  private fun PathSegment.isSameAs(other: PathSegment) =
    fromId == other.fromId && toId == other.toId && points == other.points

  private fun squaresAround(place: GeoPoint): Set<PathSquare> {
    val margins = marginsAt(place.latitude, CONNECT_METERS)
    val latitudeMargin = margins.latitude
    val longitudeMargin = margins.longitude
    return listOf(-1, 1)
      .flatMap { latitudeSign ->
        listOf(-1, 1).map { longitudeSign ->
          PathSquare.of(
            place.latitude + latitudeSign * latitudeMargin,
            place.longitude + longitudeSign * longitudeMargin,
          )
        }
      }
      .toSet()
  }

  /** How far from the straight line a search may go, so a dead end doesn't load the whole file. */
  private class SearchBounds(from: GeoPoint, to: GeoPoint) {
    private val minimumLatitude = min(from.latitude, to.latitude) - MARGIN_DEGREES
    private val maximumLatitude = max(from.latitude, to.latitude) + MARGIN_DEGREES
    private val minimumLongitude = min(from.longitude, to.longitude) - MARGIN_DEGREES * 1.6
    private val maximumLongitude = max(from.longitude, to.longitude) + MARGIN_DEGREES * 1.6

    fun contains(point: GeoPoint) =
      point.latitude in minimumLatitude..maximumLatitude &&
        point.longitude in minimumLongitude..maximumLongitude
  }

  internal companion object {
    const val CONNECT_METERS = 200.0

    /** Paths this much further away than the closest one are only used if it leads nowhere. */
    const val CLOSE_STARTS_METERS = 50.0
    const val MAX_CANDIDATES = 8

    /** Getting to a path off the paths costs this much per meter. */
    const val OFFSET_COST = 2.0

    /** See `path-routing` "No needless turning back". */
    const val TURNING_BACK_DEGREES = 135.0
    const val TURNING_BACK_COST = 200.0

    /** A start this close to a junction takes its direction from the way after the junction. */
    const val SHORT_START_METERS = 10.0

    const val MAX_VISITED_STEPS = 400_000

    /** See `path-routing` "Memory on a long ride": about 50 MB in a region with many ways. */
    const val MAX_KEPT_SQUARES = 24
    const val MARGIN_DEGREES = 0.09
    const val STEPS_PER_NODE = 3

    /** Turns this close together are one, see "Turn notices on the guided way". */
    const val MERGE_METERS = 40.0

    /**
     * Lines of a junction picture reach this far from the junction, past the corners of its view.
     */
    const val PICTURE_VIEW_METERS = 70.0

    /** Segments with no part this close to a junction are left out of its picture. */
    const val PICTURE_REACH_METERS = 80.0

    /** Another exit within this of the one taken makes a junction doubtful. */
    const val SIMILAR_EXIT_DEGREES = 35.0

    /** The direction of a way at a junction is taken over this length. */
    const val HEADING_METERS = 15.0
    const val STRAIGHT_DEGREES = 25.0
    const val SLIGHT_DEGREES = 60.0
    const val TURN_DEGREES = 120.0
    const val SHARP_DEGREES = 160.0

    /** A bend is plain when all other ways turn this much more. */
    const val CLEAR_CHOICE_DEGREES = 30.0
  }
}

/**
 * The turn of [angle] degrees, negative to the left, with the limits of the guided way's turns, see
 * `knot-navigation` "Turn notices on the guided way" and "Turn at the next knot".
 */
internal fun turnOf(angle: Double): TurnDirection {
  val size = abs(angle)
  return when {
    size < PathRouter.STRAIGHT_DEGREES -> TurnDirection.STRAIGHT
    size < PathRouter.SLIGHT_DEGREES ->
      if (angle < 0) TurnDirection.SLIGHT_LEFT else TurnDirection.SLIGHT_RIGHT
    size < PathRouter.TURN_DEGREES -> if (angle < 0) TurnDirection.LEFT else TurnDirection.RIGHT
    size < PathRouter.SHARP_DEGREES ->
      if (angle < 0) TurnDirection.SHARP_LEFT else TurnDirection.SHARP_RIGHT
    else -> TurnDirection.BACK
  }
}
