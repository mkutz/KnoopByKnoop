package com.knoopbyknoop.core

import kotlin.concurrent.Volatile
import kotlin.math.max
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** How far the path data for a route is ready for the ride, see `network-sync`. */
enum class PathDataStatus {
  LOADING,
  READY,
  UNAVAILABLE,
}

/** A square on the device. */
@Serializable
data class InstalledSquare(
  val latitudeIndex: Int,
  val longitudeIndex: Int,
  val sha256: String,
  val size: Int,
  val rawSize: Int,
  val formatVersion: Int,
  val lastUsedMillis: Long,
) {
  val square: PathSquare
    get() = PathSquare(latitudeIndex, longitudeIndex)
}

/** The app-private files of a [PathStore], written so they are never half-written. */
interface PathStoreFiles {

  /** The file called [name], or `null` if there is none. */
  fun read(name: String): ByteArray?

  fun write(name: String, bytes: ByteArray)

  fun delete(name: String)
}

/** Where path data comes from: the published index and ranges of the path file. */
interface PathDataOrigin {

  /** The text of `index.json`. */
  @Throws(Exception::class) fun index(): String

  /** Exactly the [size] bytes at [offset] of [file]. */
  @Throws(Exception::class) fun range(file: String, offset: Long, size: Int): ByteArray
}

/** Which squares to request, see `network-sync` "Path data around the route". */
object PathSquares {

  /** How far from a route's course ways are downloaded. */
  private const val AROUND_METERS = 5000.0

  /** Course points further apart than this get points added between them. */
  private const val STEP_METERS = 2000.0

  /**
   * The course to download squares for, or `null` if nothing is to be downloaded: on a hiking
   * route, without a complete route, or for the course the ways were just deleted for, see
   * `network-sync` "Path data in the info sheet". It never depends on the rider's position.
   *
   * @param deletedFor the course of the route planned when the ways were deleted, until the route
   *   changes or navigation starts
   */
  fun courseToLoad(
    network: NetworkType?,
    course: List<GeoPoint>?,
    deletedFor: List<GeoPoint>? = null,
  ) = course?.takeIf {
    network == NetworkType.CYCLING && it.isNotEmpty() && it != deletedFor
  }

  /** The squares within [AROUND_METERS] of [course]. */
  fun near(course: List<GeoPoint>): Set<PathSquare> {
    val squares = LinkedHashSet<PathSquare>()
    fun addAround(point: GeoPoint) {
      val margins = marginsAt(point.latitude, AROUND_METERS)
      val latitudeMargin = margins.latitude
      val longitudeMargin = margins.longitude
      val low = PathSquare.of(point.latitude - latitudeMargin, point.longitude - longitudeMargin)
      val high = PathSquare.of(point.latitude + latitudeMargin, point.longitude + longitudeMargin)
      for (latitude in low.latitudeIndex..high.latitudeIndex) {
        for (longitude in low.longitudeIndex..high.longitudeIndex) {
          squares.add(PathSquare(latitude, longitude))
        }
      }
    }
    course.firstOrNull()?.let(::addAround)
    course.zipWithNext().forEach { (from, to) ->
      val meters = distanceMeters(from.latitude, from.longitude, to.latitude, to.longitude)
      val steps = max(1, (meters / STEP_METERS).toInt() + 1)
      for (step in 1..steps) {
        val along = step.toDouble() / steps
        addAround(
          GeoPoint(
            from.latitude + (to.latitude - from.latitude) * along,
            from.longitude + (to.longitude - from.longitude) * along,
          )
        )
      }
    }
    return squares
  }
}

/**
 * The squares on the device, see `network-sync` "Keeping path data". Read by the router and written
 * by [PathDownloader], possibly at the same time: a square's file is written before the list names
 * it, and the list is replaced as a whole.
 */
class PathStore(private val files: PathStoreFiles) : PathSquareSource {

  @Volatile private var squares: List<InstalledSquare> = load()

  /** Goes up whenever squares change, so a router knows to forget what it read before. */
  @Volatile
  var version: Int = 0
    private set

  /** The data date of the path data last seen, for the info sheet; `null` without any squares. */
  val dataDate: String?
    get() = if (squares.isEmpty()) null else cachedEntry()?.dataDate

  val installed: List<InstalledSquare>
    get() = squares

  override fun segments(square: PathSquare): List<PathSegment> {
    val entry = squares.firstOrNull { it.square == square } ?: return listOf()
    val bytes = files.read(fileOf(square)) ?: return listOf()
    // A broken file counts as not installed, and is requested again with the next route.
    if (sha256Hex(bytes) != entry.sha256) {
      remove(listOf(square))
      return listOf()
    }
    return PathFile.decode(bytes, entry.rawSize)
  }

  /**
   * The space the installed squares and the cached directory take, in bytes, see `network-sync`
   * "Path data in the info sheet".
   */
  fun installedBytes(): Long =
    if (squares.isEmpty()) 0
    else squares.sumOf { it.size.toLong() } + (files.read(DIRECTORY_FILE)?.size ?: 0)

  /** Removes everything, see `network-sync` "Deleting the ways". */
  fun clear() {
    squares.forEach { files.delete(fileOf(it.square)) }
    files.delete(DIRECTORY_FILE)
    files.delete(ENTRY_FILE)
    replaceSquares(listOf())
  }

  internal fun cachedEntry(): PathIndexEntry? =
    files.read(ENTRY_FILE)?.let { bytes ->
      try {
        json.decodeFromString<PathIndexEntry>(bytes.decodeToString())
      } catch (exception: IllegalArgumentException) {
        null
      }
    }

  internal fun cachedDirectory(entry: PathIndexEntry): PathFile.Directory? {
    val bytes = files.read(DIRECTORY_FILE) ?: return null
    if (sha256Hex(bytes) != entry.directorySha256) return null
    return PathFile.parseDirectory(bytes)
  }

  internal fun cacheDirectory(entry: PathIndexEntry, bytes: ByteArray) {
    files.write(DIRECTORY_FILE, bytes)
    files.write(ENTRY_FILE, json.encodeToString(entry).encodeToByteArray())
  }

  internal fun install(square: InstalledSquare, bytes: ByteArray) {
    files.write(fileOf(square.square), bytes)
    replaceSquares(squares.filterNot { it.square == square.square } + square)
  }

  internal fun markUsed(used: Set<PathSquare>, nowMillis: Long) {
    if (used.isEmpty()) return
    replaceSquares(
      squares.map { if (it.square in used) it.copy(lastUsedMillis = nowMillis) else it },
      changed = false,
    )
  }

  internal fun remove(removed: List<PathSquare>) {
    removed.forEach { files.delete(fileOf(it)) }
    replaceSquares(squares.filterNot { it.square in removed })
  }

  private fun replaceSquares(newSquares: List<InstalledSquare>, changed: Boolean = true) {
    squares = newSquares
    files.write(LIST_FILE, json.encodeToString(newSquares).encodeToByteArray())
    if (changed) version += 1
  }

  /** Reads the list; squares of a format this core doesn't read are as good as not there. */
  private fun load(): List<InstalledSquare> {
    val text = files.read(LIST_FILE)?.decodeToString() ?: return listOf()
    val listed =
      try {
        json.decodeFromString<List<InstalledSquare>>(text)
      } catch (exception: IllegalArgumentException) {
        return listOf()
      }
    val (usable, old) = listed.partition { it.formatVersion == PathFile.FORMAT_VERSION }
    old.forEach { files.delete(fileOf(it.square)) }
    return usable
  }

  private fun fileOf(square: PathSquare) = "square_${square.latitudeIndex}_${square.longitudeIndex}"

  private companion object {
    const val LIST_FILE = "squares.json"
    const val DIRECTORY_FILE = "directory"
    const val ENTRY_FILE = "entry.json"
    val json = Json { ignoreUnknownKeys = true }
  }
}

/**
 * Brings the squares around a course onto the device, see `network-sync`. Blocks, so call it off
 * the main thread.
 */
class PathDownloader(private val store: PathStore, private val origin: PathDataOrigin) {

  /**
   * Downloads what is missing or older than the published path data around [course].
   *
   * @param isCancelled asked between requests
   * @param onStatus told when it is known whether there is anything to download
   * @return what it came to; a failure leaves everything installed as it was
   */
  fun load(
    course: List<GeoPoint>,
    nowMillis: Long,
    isCancelled: () -> Boolean = { false },
    onStatus: (PathDataStatus) -> Unit = {},
  ): PathDataStatus {
    val near = PathSquares.near(course)
    var published = published(forceRefresh = false)
    var retried = false
    while (true) {
      val (entry, directory) = published ?: return statusWithout(near, null)
      val wanted = directory.entries.filterKeys { it in near }
      store.markUsed(wanted.keys, nowMillis)
      val missing = wanted.filter { (square, info) ->
        store.installed.none { it.square == square && it.sha256 == info.sha256 }
      }
      onStatus(if (missing.isEmpty()) PathDataStatus.READY else PathDataStatus.LOADING)
      val outcome = fetch(entry, missing, nowMillis, isCancelled)
      if (isCancelled()) return statusWithout(near, directory)
      if (outcome == Outcome.CHANGED_FILE && !retried) {
        // The weekly publication replaced the file under the download: read its new list.
        retried = true
        published = published(forceRefresh = true)
        continue
      }
      keepWithinLimit(near)
      return statusWithout(near, directory)
    }
  }

  private enum class Outcome {
    DONE,
    CHANGED_FILE,
    FAILED,
  }

  /** What was published, or the last known if that can't be found out now. */
  private fun published(forceRefresh: Boolean): Pair<PathIndexEntry, PathFile.Directory>? {
    val cached = store.cachedEntry()
    val current =
      try {
        NetworkIndex.parse(origin.index()).paths
      } catch (exception: Exception) {
        if (forceRefresh) return null
        return cached?.let { entry -> store.cachedDirectory(entry)?.let { entry to it } }
      }
    if (current == null || current.formatVersion != PathFile.FORMAT_VERSION) return null
    store.cachedDirectory(current)?.let {
      return current to it
    }
    return try {
      val bytes = origin.range(current.file, current.directoryOffset, current.directorySize)
      if (sha256Hex(bytes) != current.directorySha256) return null
      store.cacheDirectory(current, bytes)
      current to PathFile.parseDirectory(bytes)
    } catch (exception: Exception) {
      cached?.let { entry -> store.cachedDirectory(entry)?.let { entry to it } }
    }
  }

  /** Requests squares lying next to each other in the file in one go, to save round trips. */
  private fun fetch(
    entry: PathIndexEntry,
    missing: Map<PathSquare, PathFile.Entry>,
    nowMillis: Long,
    isCancelled: () -> Boolean,
  ): Outcome {
    val dataOffset = entry.directoryOffset + entry.directorySize
    val sorted = missing.entries.sortedBy { it.value.offset }
    var outcome = Outcome.DONE
    var position = 0
    while (position < sorted.size) {
      if (isCancelled()) return outcome
      var last = position
      var size = sorted[position].value.size
      while (
        last + 1 < sorted.size &&
          sorted[last + 1].value.offset == sorted[last].value.offset + sorted[last].value.size &&
          size + sorted[last + 1].value.size <= MAX_REQUEST_BYTES
      ) {
        last++
        size += sorted[last].value.size
      }
      val first = sorted[position].value
      val bytes =
        try {
          origin.range(entry.file, dataOffset + first.offset, size)
        } catch (exception: Exception) {
          return Outcome.FAILED
        }
      if (bytes.size != size) return Outcome.FAILED
      for (index in position..last) {
        val (square, info) = sorted[index]
        val start = (info.offset - first.offset).toInt()
        val part = bytes.copyOfRange(start, start + info.size)
        if (sha256Hex(part) == info.sha256) {
          store.install(
            InstalledSquare(
              square.latitudeIndex,
              square.longitudeIndex,
              info.sha256,
              info.size,
              info.rawSize,
              PathFile.FORMAT_VERSION,
              nowMillis,
            ),
            part,
          )
        } else {
          outcome = Outcome.CHANGED_FILE
        }
      }
      position = last + 1
    }
    return outcome
  }

  /** A square kept from older data still serves until its newer version is in. */
  private fun statusWithout(near: Set<PathSquare>, directory: PathFile.Directory?): PathDataStatus {
    val wanted =
      directory?.entries?.keys?.filter { it in near } ?: return PathDataStatus.UNAVAILABLE
    return if (wanted.all { square -> store.installed.any { it.square == square } }) {
      PathDataStatus.READY
    } else {
      PathDataStatus.UNAVAILABLE
    }
  }

  private fun keepWithinLimit(current: Set<PathSquare>) {
    var total = store.installed.sumOf { it.size.toLong() }
    if (total <= MAX_STORE_BYTES) return
    val removed = ArrayList<PathSquare>()
    for (square in
      store.installed.filter { it.square !in current }.sortedBy { it.lastUsedMillis }) {
      if (total <= MAX_STORE_BYTES) break
      removed.add(square.square)
      total -= square.size
    }
    store.remove(removed)
  }

  companion object {
    /** See `network-sync` "Keeping path data". */
    const val MAX_STORE_BYTES = 50_000_000L

    private const val MAX_REQUEST_BYTES = 2_000_000
  }
}
