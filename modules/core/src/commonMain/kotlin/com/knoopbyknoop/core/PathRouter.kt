package com.knoopbyknoop.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/** Which way a turn goes. [STRAIGHT] is only given for a junction with several small turns. */
enum class TurnDirection {
  STRAIGHT,
  SLIGHT_LEFT,
  LEFT,
  SHARP_LEFT,
  SLIGHT_RIGHT,
  RIGHT,
  SHARP_RIGHT,
  BACK,
}

/**
 * A turn on a [PathWay], see `knot-navigation` "Turn notices on the guided way".
 *
 * @property atMeters where it is, along the way
 * @property name the way it turns into, if known
 */
data class PathTurn(val atMeters: Double, val direction: TurnDirection, val name: String?)

/**
 * A way found by [PathRouter].
 *
 * @property points its course from the given start to the given target
 * @property meters for each of [points], how far along the way it is
 * @property startsBack whether it starts against the riding direction, see `path-routing` "Starting
 *   in the riding direction"
 */
class PathWay(
  val points: List<GeoPoint>,
  val meters: List<Double>,
  val turns: List<PathTurn>,
  val startsBack: Boolean = false,
) {

  val lengthMeters: Double
    get() = meters.lastOrNull() ?: 0.0
}

/**
 * Finds ways for a bicycle from a [PathFile] on the device, see `path-routing`. Squares are loaded
 * as the search reaches them and stay loaded. Not safe for use from several threads at once.
 */
class PathRouter(private val file: PathFile) {

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

  /** How many squares are loaded, to measure the memory used. */
  val loadedSquares: Int
    get() = loaded.size

  /** How many steps the last search looked at, to measure the time used. */
  var visitedSteps: Int = 0
    private set

  /**
   * The way from [from] to [to], or `null` if the squares between them don't connect them, see
   * `path-routing` "No way found".
   *
   * @param heading the riding direction, clockwise from north, if known
   */
  fun way(from: GeoPoint, to: GeoPoint, heading: Double? = null): PathWay? {
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
    val queue = LabelHeap()
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
      queue.add(Label(start.step, cost, null, start), cost + estimate(start.step.end, to))
    }
    visitedSteps = 0
    while (true) {
      val entry = queue.poll() ?: break
      if (entry.priority >= resultCost) break
      val label = entry.label
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
        queue.add(Label(next, cost, label, null), cost + estimate(next.end, to))
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
    pieces.forEachIndexed { index, piece ->
      if (index > 0) {
        val previous = pieces[index - 1]
        junctions.add(
          Junction(
            meters.last(),
            piece.step.segment.name,
            exitsAt(piece.fromNode ?: 0L, previous.step),
            isPlainBend(points, piece.fromNode ?: 0L, previous.step, piece.step),
          )
        )
      }
      piece.points.forEach(::add)
    }
    add(to)
    return PathWay(
      points,
      meters,
      turns(points, meters, junctions),
      startsBack(points, first.start?.points?.firstOrNull(), heading),
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
    return (connection >= SHORT_START_METERS && isTurningBack(heading, heading(from, onPath))) ||
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
    val inbound = heading(pointAlong(arrived.asReversed()), arrived.last())
    fun turnOf(step: Step) = abs(signedAngle(inbound, headingAlong(step.points())))
    val takenTurn = turnOf(taken)
    return takenTurn < TURN_DEGREES &&
      stepsByNode[node].orEmpty().all {
        it.segment === arriving.segment ||
          it.segment === taken.segment ||
          turnOf(it) - takenTurn >= CLEAR_CHOICE_DEGREES
      }
  }

  /** The direction [points] set off in, over about [HEADING_METERS]. */
  private fun headingAlong(points: List<GeoPoint>) = heading(points.first(), pointAlong(points))

  /** The first of [points] about [HEADING_METERS] from the first, else the last. */
  private fun pointAlong(points: List<GeoPoint>): GeoPoint {
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
    fun at(position: Double): GeoPoint {
      val clamped = position.coerceIn(0.0, meters.last())
      val index = meters.indexOfFirst { it >= clamped }.coerceAtLeast(1)
      val length = meters[index] - meters[index - 1]
      val along = if (length == 0.0) 0.0 else (clamped - meters[index - 1]) / length
      val start = points[index - 1]
      val end = points[index]
      return GeoPoint(
        start.latitude + (end.latitude - start.latitude) * along,
        start.longitude + (end.longitude - start.longitude) * along,
      )
    }
    fun angle(fromMeters: Double, toMeters: Double): Double {
      val inbound = heading(at(fromMeters - HEADING_METERS), at(fromMeters))
      val outbound = heading(at(toMeters), at(toMeters + HEADING_METERS))
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
      val direction =
        if (index == last && decisions[index].plainBend) null else directionOf(angle, realTurns)
      if (direction != null) turns.add(PathTurn(firstMeters, direction, decisions[last].name))
      index = last + 1
    }
    return turns
  }

  private fun directionOf(angle: Double, realTurns: Int): TurnDirection? {
    val size = abs(angle)
    return when {
      size < STRAIGHT_DEGREES -> if (realTurns >= 2) TurnDirection.STRAIGHT else null
      size < SLIGHT_DEGREES ->
        if (angle < 0) TurnDirection.SLIGHT_LEFT else TurnDirection.SLIGHT_RIGHT
      size < TURN_DEGREES -> if (angle < 0) TurnDirection.LEFT else TurnDirection.RIGHT
      size < SHARP_DEGREES -> if (angle < 0) TurnDirection.SHARP_LEFT else TurnDirection.SHARP_RIGHT
      else -> TurnDirection.BACK
    }
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

  private fun lengthOf(points: List<GeoPoint>) =
    points.zipWithNext().sumOf { (a, b) ->
      distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
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
        heading(inbound[inbound.size - 2], inbound.last()),
        heading(outbound.first(), outbound[1]),
      )
    return if (abs(angle) > TURNING_BACK_DEGREES) TURNING_BACK_COST else 0.0
  }

  private fun estimate(from: GeoPoint, to: GeoPoint) =
    distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)

  private fun load(place: GeoPoint) = load(PathSquare.of(place.latitude, place.longitude))

  private fun load(square: PathSquare) {
    if (!loaded.add(square)) return
    val segments = file.segments(square)
    segmentsBySquare[square] = segments
    segments.forEach { segment ->
      if (segment.forward) addStep(segment, reversed = false)
      if (segment.backward) addStep(segment, reversed = true)
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
    val latitudeMargin = CONNECT_METERS / METERS_PER_DEGREE
    val longitudeMargin = latitudeMargin / cos(place.latitude * PI / 180)
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

  private class HeapEntry(val label: Label, val priority: Double)

  /** The labels to look at, cheapest estimate first. */
  private class LabelHeap {
    private val entries = ArrayList<HeapEntry>()

    fun add(label: Label, priority: Double) {
      entries.add(HeapEntry(label, priority))
      var index = entries.lastIndex
      while (index > 0) {
        val parent = (index - 1) / 2
        if (entries[parent].priority <= entries[index].priority) break
        swap(parent, index)
        index = parent
      }
    }

    fun poll(): HeapEntry? {
      if (entries.isEmpty()) return null
      val top = entries[0]
      val last = entries.removeAt(entries.lastIndex)
      if (entries.isNotEmpty()) {
        entries[0] = last
        var index = 0
        while (true) {
          val left = index * 2 + 1
          val right = left + 1
          var smallest = index
          if (left < entries.size && entries[left].priority < entries[smallest].priority) {
            smallest = left
          }
          if (right < entries.size && entries[right].priority < entries[smallest].priority) {
            smallest = right
          }
          if (smallest == index) break
          swap(index, smallest)
          index = smallest
        }
      }
      return top
    }

    private fun swap(first: Int, second: Int) {
      val entry = entries[first]
      entries[first] = entries[second]
      entries[second] = entry
    }
  }

  private companion object {
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
    const val MARGIN_DEGREES = 0.09
    const val METERS_PER_DEGREE = 111_195.0
    const val STEPS_PER_NODE = 3

    /** Turns this close together are one, see "Turn notices on the guided way". */
    const val MERGE_METERS = 40.0

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
 * The direction from [from] to [to] in degrees clockwise from north, in a local flat projection.
 */
internal fun heading(from: GeoPoint, to: GeoPoint): Double {
  val x = (to.longitude - from.longitude) * cos(from.latitude * PI / 180)
  val y = to.latitude - from.latitude
  return atan2(x, y) * 180 / PI
}

/** From [from] to [to] in degrees: negative to the left, positive to the right. */
internal fun signedAngle(from: Double, to: Double): Double {
  var angle = (to - from) % 360
  if (angle > 180) angle -= 360
  if (angle < -180) angle += 360
  return angle
}

/** The point on the line from [from] to [to] nearest to [place], in a local flat projection. */
internal fun nearestOnLine(place: GeoPoint, from: GeoPoint, to: GeoPoint): GeoPoint {
  val scale = cos(place.latitude * PI / 180)
  val deltaX = (to.longitude - from.longitude) * scale
  val deltaY = to.latitude - from.latitude
  val lengthSquared = deltaX * deltaX + deltaY * deltaY
  if (lengthSquared == 0.0) return from
  val along =
    (((place.longitude - from.longitude) * scale * deltaX +
        (place.latitude - from.latitude) * deltaY) / lengthSquared)
      .coerceIn(0.0, 1.0)
  return GeoPoint(
    from.latitude + (to.latitude - from.latitude) * along,
    from.longitude + (to.longitude - from.longitude) * along,
  )
}
