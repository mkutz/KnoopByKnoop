package com.knoopbyknoop.core

import kotlin.concurrent.Volatile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** How far the squares for a route are on the device, see `network-sync`. */
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

/** The app-private files of a [SquareStore], written so they are never half-written. */
interface PathStoreFiles {

  /** The file called [name], or `null` if there is none. */
  fun read(name: String): ByteArray?

  fun write(name: String, bytes: ByteArray)

  fun delete(name: String)
}

/** Where square files come from: the published index and ranges of a file. */
interface PathDataOrigin {

  /** The text of `index.json`. */
  @Throws(Exception::class) fun index(): String

  /** Exactly the [size] bytes at [offset] of [file]. */
  @Throws(Exception::class) fun range(file: String, offset: Long, size: Int): ByteArray
}

/**
 * The squares of one [SquareFile] on the device, see `network-sync`. Read by the app and written by
 * [SquareDownloader], possibly at the same time: a square's file is written before the list names
 * it, and the list is replaced as a whole.
 */
open class SquareStore(private val files: PathStoreFiles, private val format: SquareFile) {

  @Volatile private var squares: List<InstalledSquare> = load()

  /** Goes up whenever squares change, so a reader knows to forget what it read before. */
  @Volatile
  var version: Int = 0
    private set

  /** The data date of the squares last seen, for the info sheet; `null` without any squares. */
  val dataDate: String?
    get() = if (squares.isEmpty()) null else cachedEntry()?.dataDate

  val installed: List<InstalledSquare>
    get() = squares

  /** The unpacked bytes of [square], `null` if it isn't installed or its file is broken. */
  protected fun rawBytes(square: PathSquare): ByteArray? {
    val entry = squares.firstOrNull { it.square == square } ?: return null
    val bytes = files.read(fileOf(square)) ?: return null
    // A broken file counts as not installed, and is requested again later.
    if (sha256Hex(bytes) != entry.sha256) {
      remove(listOf(square))
      return null
    }
    return inflate(bytes, entry.rawSize)
  }

  /**
   * The space the installed squares and the cached directory take, in bytes, for the info sheet.
   */
  fun installedBytes(): Long =
    if (squares.isEmpty()) 0
    else squares.sumOf { it.size.toLong() } + (files.read(DIRECTORY_FILE)?.size ?: 0)

  /** Removes everything, see the info sheet's delete buttons in `network-sync`. */
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

  internal fun cachedDirectory(entry: PathIndexEntry): SquareDirectory? {
    val bytes = files.read(DIRECTORY_FILE) ?: return null
    if (sha256Hex(bytes) != entry.directorySha256) return null
    return format.parseDirectory(bytes)
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
    val (usable, old) = listed.partition { it.formatVersion == format.formatVersion }
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
 * Brings squares of a [SquareFile] onto the device, see `network-sync`. Blocks, so call it off the
 * main thread.
 *
 * @param entryOf the file's entry in the index, `null` when it has none
 * @param maxStoreBytes how much the store keeps at most
 */
class SquareDownloader(
  private val store: SquareStore,
  private val origin: PathDataOrigin,
  private val format: SquareFile,
  private val entryOf: (NetworkIndex) -> PathIndexEntry?,
  private val maxStoreBytes: Long,
) {

  /**
   * Downloads what is missing or older than published of [wanted].
   *
   * @param keep the squares never removed to stay within the limit, e.g. the current route's
   * @param isCancelled asked between requests
   * @param onStatus told when it is known whether there is anything to download
   * @return what it came to; a failure leaves everything installed as it was
   */
  fun load(
    wanted: Set<PathSquare>,
    nowMillis: Long,
    keep: Set<PathSquare> = wanted,
    isCancelled: () -> Boolean = { false },
    onStatus: (PathDataStatus) -> Unit = {},
  ): PathDataStatus {
    var published = published(forceRefresh = false)
    var retried = false
    while (true) {
      val (entry, directory) = published ?: return statusWithout(wanted, null)
      val listed = directory.entries.filterKeys { it in wanted }
      store.markUsed(listed.keys, nowMillis)
      val missing = listed.filter { (square, info) ->
        store.installed.none { it.square == square && it.sha256 == info.sha256 }
      }
      onStatus(if (missing.isEmpty()) PathDataStatus.READY else PathDataStatus.LOADING)
      val outcome = fetch(entry, missing, nowMillis, isCancelled)
      if (isCancelled()) return statusWithout(wanted, directory)
      if (outcome == Outcome.CHANGED_FILE && !retried) {
        // The weekly publication replaced the file under the download: read its new list.
        retried = true
        published = published(forceRefresh = true)
        continue
      }
      keepWithinLimit(keep + wanted)
      return statusWithout(wanted, directory)
    }
  }

  private enum class Outcome {
    DONE,
    CHANGED_FILE,
    FAILED,
  }

  /** What was published, or the last known if that can't be found out now. */
  private fun published(forceRefresh: Boolean): Pair<PathIndexEntry, SquareDirectory>? {
    val cached = store.cachedEntry()
    val current =
      try {
        entryOf(NetworkIndex.parse(origin.index()))
      } catch (exception: Exception) {
        if (forceRefresh) return null
        return cached?.let { entry -> store.cachedDirectory(entry)?.let { entry to it } }
      }
    if (current == null || current.formatVersion != format.formatVersion) return null
    store.cachedDirectory(current)?.let {
      return current to it
    }
    return try {
      val bytes = origin.range(current.file, current.directoryOffset, current.directorySize)
      if (sha256Hex(bytes) != current.directorySha256) return null
      store.cacheDirectory(current, bytes)
      current to format.parseDirectory(bytes)
    } catch (exception: Exception) {
      cached?.let { entry -> store.cachedDirectory(entry)?.let { entry to it } }
    }
  }

  /** Requests squares lying next to each other in the file in one go, to save round trips. */
  private fun fetch(
    entry: PathIndexEntry,
    missing: Map<PathSquare, SquareEntry>,
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
              format.formatVersion,
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
  private fun statusWithout(wanted: Set<PathSquare>, directory: SquareDirectory?): PathDataStatus {
    val published =
      directory?.entries?.keys?.filter { it in wanted } ?: return PathDataStatus.UNAVAILABLE
    return if (published.all { square -> store.installed.any { it.square == square } }) {
      PathDataStatus.READY
    } else {
      PathDataStatus.UNAVAILABLE
    }
  }

  private fun keepWithinLimit(current: Set<PathSquare>) {
    var total = store.installed.sumOf { it.size.toLong() }
    if (total <= maxStoreBytes) return
    val removed = ArrayList<PathSquare>()
    for (square in
      store.installed.filter { it.square !in current }.sortedBy { it.lastUsedMillis }) {
      if (total <= maxStoreBytes) break
      removed.add(square.square)
      total -= square.size
    }
    store.remove(removed)
  }

  private companion object {
    const val MAX_REQUEST_BYTES = 2_000_000
  }
}
