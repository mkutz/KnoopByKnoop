package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PathStoreTest {

  private class MemoryFiles : PathStoreFiles {
    val files = HashMap<String, ByteArray>()

    override fun read(name: String) = files[name]

    override fun write(name: String, bytes: ByteArray) {
      files[name] = bytes
    }

    override fun delete(name: String) {
      files.remove(name)
    }
  }

  /** The published index and file; requests are counted, and can fail or see another file. */
  private class FakeOrigin(var file: ByteArray, var withPaths: Boolean = true) : PathDataOrigin {
    var online = true
    val requests = ArrayList<Pair<Long, Int>>()
    var beforeRange: (Long, Int) -> Unit = { _, _ -> }
    var breakBytes: (Long, ByteArray) -> ByteArray = { _, bytes -> bytes }

    override fun index(): String {
      if (!online) throw IllegalStateException("offline")
      val info = PathFile.directoryInfo { offset, size ->
        file.copyOfRange(offset.toInt(), offset.toInt() + size)
      }
      val paths =
        PathIndexEntry(
          "cycling.paths",
          file.size.toLong(),
          info.dataDate,
          info.formatVersion,
          info.offset,
          info.size,
          info.sha256,
        )
      return NetworkIndex(2, "now", listOf(), paths.takeIf { withPaths }).toJson()
    }

    override fun range(file: String, offset: Long, size: Int): ByteArray {
      if (!online) throw IllegalStateException("offline")
      requests.add(offset to size)
      beforeRange(offset, size)
      return breakBytes(
        offset,
        this.file.copyOfRange(offset.toInt(), offset.toInt() + size),
      )
    }
  }

  private val origin = GeoPoint(50.80, 7.20)

  /** A point [east] and [north] kilometers from [origin]. */
  private fun at(east: Double, north: Double = 0.0) =
    GeoPoint(origin.latitude + north * 1000 / 111_195.0, origin.longitude + east * 1000 / 70_300.0)

  private fun segment(id: Long, from: GeoPoint, to: GeoPoint, name: String? = null) =
    PathSegment(id, id + 1, PathKind.CYCLE, true, true, 1.0, name, listOf(from, to))

  /**
   * A way east every 1 km for [kilometers], with [marker] in the names so another build differs.
   */
  private fun fileOf(
    kilometers: Int,
    marker: String = "a",
    date: String = "2026-11-02",
  ): ByteArray {
    val squares = HashMap<PathSquare, MutableList<PathSegment>>()
    for (index in 0 until kilometers) {
      val piece = segment(index.toLong(), at(index.toDouble()), at(index + 1.0), "$marker$index")
      listOf(piece.points.first(), piece.points.last())
        .map { PathSquare.of(it.latitude, it.longitude) }
        .distinct()
        .forEach { squares.getOrPut(it) { mutableListOf() }.add(piece) }
    }
    return PathFile.toBytes(squares, date, "OSM")
  }

  private val files = MemoryFiles()
  private val store = PathStore(files)

  private fun course(fromKilometer: Double, toKilometer: Double) =
    listOf(at(fromKilometer), at(toKilometer))

  @Test
  fun `Off by default no course is loaded`() {
    val course = course(0.0, 1.0)
    assertNull(PathSquares.courseToLoad(false, NetworkType.CYCLING, course))
    assertEquals(course, PathSquares.courseToLoad(true, NetworkType.CYCLING, course))
  }

  @Test
  fun `A hiking route loads nothing`() {
    assertNull(PathSquares.courseToLoad(true, NetworkType.HIKING, course(0.0, 1.0)))
  }

  @Test
  fun `An incomplete route loads nothing`() {
    assertNull(PathSquares.courseToLoad(true, NetworkType.CYCLING, null))
  }

  @Test
  fun `Squares within 5 km of the course are near also between far points`() {
    val near = PathSquares.near(listOf(at(0.0), at(60.0)))
    assertTrue(PathSquare.of(at(30.0).latitude, at(30.0).longitude) in near)
    assertTrue(PathSquare.of(at(30.0, 4.9).latitude, at(30.0).longitude) in near)
    assertFalse(PathSquare.of(at(30.0, 20.0).latitude, at(30.0).longitude) in near)
  }

  @Test
  fun `Route planned at home then navigated without network access`() {
    val source = FakeOrigin(fileOf(40))
    val status = PathDownloader(store, source).load(course(0.0, 8.0), 1)
    assertEquals(PathDataStatus.READY, status)
    source.online = false
    val way = PathRouter(store).way(at(1.1), at(6.9))
    assertEquals(5.8, assertNotNull(way).lengthMeters / 1000, 0.1)
  }

  @Test
  fun `Only squares around the route are requested`() {
    val source = FakeOrigin(fileOf(100))
    PathDownloader(store, source).load(course(0.0, 2.0), 1)
    val far = at(90.0)
    assertTrue(store.installed.none { it.square == PathSquare.of(far.latitude, far.longitude) })
    assertTrue(store.installed.isNotEmpty())
  }

  @Test
  fun `Changing the route requests installed squares no more`() {
    val source = FakeOrigin(fileOf(20))
    val downloader = PathDownloader(store, source)
    downloader.load(course(0.0, 2.0), 1)
    val first = source.requests.size
    val installed = store.installed.size
    downloader.load(course(0.5, 2.5), 2)
    assertEquals(first, source.requests.size)
    assertEquals(installed, store.installed.size)
  }

  @Test
  fun `Squares next to each other in the file come in one request`() {
    val source = FakeOrigin(fileOf(40))
    PathDownloader(store, source).load(course(0.0, 40.0), 1)
    // The directory, and fewer ranges than squares.
    assertTrue(source.requests.size - 1 < store.installed.size)
    assertTrue(store.installed.size > 3)
  }

  @Test
  fun `It says it is loading before it is ready`() {
    val source = FakeOrigin(fileOf(10))
    val statuses = ArrayList<PathDataStatus>()
    val downloader = PathDownloader(store, source)
    val result = downloader.load(course(0.0, 2.0), 1, onStatus = { statuses.add(it) })
    assertEquals(listOf(PathDataStatus.LOADING), statuses)
    assertEquals(PathDataStatus.READY, result)
    statuses.clear()
    downloader.load(course(0.0, 2.0), 2, onStatus = { statuses.add(it) })
    assertEquals(listOf(PathDataStatus.READY), statuses)
  }

  @Test
  fun `Without network access it is not available and is tried again`() {
    val source = FakeOrigin(fileOf(10))
    source.online = false
    val downloader = PathDownloader(store, source)
    assertEquals(PathDataStatus.UNAVAILABLE, downloader.load(course(0.0, 2.0), 1))
    assertEquals(listOf(), store.installed)
    source.online = true
    assertEquals(PathDataStatus.READY, downloader.load(course(0.0, 2.0), 2))
  }

  @Test
  fun `A download breaking off keeps what is in`() {
    val source = FakeOrigin(fileOf(40))
    var ranges = 0
    source.beforeRange = { _, _ ->
      ranges++
      if (ranges == 1) source.online = false
    }
    val status =
      try {
        PathDownloader(store, source).load(course(0.0, 40.0), 1)
      } finally {
        source.online = true
      }
    assertEquals(PathDataStatus.UNAVAILABLE, status)
    source.beforeRange = { _, _ -> }
    assertEquals(PathDataStatus.READY, PathDownloader(store, source).load(course(0.0, 40.0), 2))
  }

  @Test
  fun `A corrupted square is discarded and the rest keep working`() {
    val source = FakeOrigin(fileOf(40))
    val downloader = PathDownloader(store, source)
    val directorySize = source.index().let { NetworkIndex.parse(it).paths!!.directorySize }
    source.breakBytes = { offset, bytes ->
      if (offset > 9L + directorySize - 1) bytes.also { it[0] = (it[0] + 1).toByte() } else bytes
    }
    assertEquals(PathDataStatus.UNAVAILABLE, downloader.load(course(0.0, 40.0), 1))
    assertTrue(store.installed.size < PathSquares.near(course(0.0, 40.0)).size)
    source.breakBytes = { _, bytes -> bytes }
    assertEquals(PathDataStatus.READY, downloader.load(course(0.0, 40.0), 2))
  }

  @Test
  fun `A file replaced during a download is read anew`() {
    val source = FakeOrigin(fileOf(30, marker = "old"))
    val replacement = fileOf(30, marker = "new", date = "2026-11-09")
    var replaced = false
    source.beforeRange = { _, _ ->
      // After the directory, the first square's request hits the new file.
      if (source.requests.size == 2 && !replaced) {
        replaced = true
        source.file = replacement
      }
    }
    val status = PathDownloader(store, source).load(course(0.0, 30.0), 1)
    assertEquals(PathDataStatus.READY, status)
    assertEquals("2026-11-09", store.dataDate)
    assertTrue(replaced)
    assertNotNull(PathRouter(store).way(at(1.0), at(25.0)))
  }

  @Test
  fun `Newer path data replaces squares of a new route only`() {
    val source = FakeOrigin(fileOf(60))
    val downloader = PathDownloader(store, source)
    downloader.load(course(0.0, 2.0), 1)
    downloader.load(course(50.0, 52.0), 2)
    val before = store.installed.associateBy { it.square }
    source.file = fileOf(60, marker = "new", date = "2026-11-09")
    downloader.load(course(0.0, 2.0), 3)
    val near = PathSquares.near(course(0.0, 2.0))
    store.installed.forEach { square ->
      if (square.square in near) {
        assertTrue(square.sha256 != before[square.square]?.sha256 || square.size == 0)
      } else {
        assertEquals(before[square.square]?.sha256, square.sha256)
      }
    }
  }

  @Test
  fun `Older squares serve until their newer version is in`() {
    val source = FakeOrigin(fileOf(10))
    val downloader = PathDownloader(store, source)
    downloader.load(course(0.0, 2.0), 1)
    source.file = fileOf(10, marker = "new", date = "2026-11-09")
    source.beforeRange = { _, size -> if (size > 0) throw IllegalStateException("down") }
    assertEquals(PathDataStatus.READY, downloader.load(course(0.0, 2.0), 2))
    assertNotNull(PathRouter(store).way(at(0.5), at(1.5)))
  }

  @Test
  fun `An unsupported format is not used and an index without path data neither`() {
    val source = FakeOrigin(fileOf(10), withPaths = false)
    assertEquals(
      PathDataStatus.UNAVAILABLE,
      PathDownloader(store, source).load(course(0.0, 2.0), 1),
    )
    assertEquals(0, source.requests.size)
    assertEquals(listOf(), store.installed)
  }

  @Test
  fun `Squares of an older format count as not installed after a restart`() {
    files.write(
      "squares.json",
      """[{"latitudeIndex":812,"longitudeIndex":115,"sha256":"a","size":1,"rawSize":1,
        "formatVersion":1,"lastUsedMillis":1}]"""
        .encodeToByteArray(),
    )
    files.write("square_812_115", byteArrayOf(1))
    val restarted = PathStore(files)
    assertEquals(listOf(), restarted.installed)
    assertNull(files.read("square_812_115"))
  }

  @Test
  fun `Turning it off removes everything`() {
    val source = FakeOrigin(fileOf(10))
    PathDownloader(store, source).load(course(0.0, 2.0), 1)
    assertTrue(files.files.size > 2)
    store.clear()
    assertEquals(listOf(), store.installed)
    assertEquals(setOf("squares.json"), files.files.keys)
    assertNull(store.dataDate)
    assertNull(PathRouter(store).way(at(0.5), at(1.5)))
  }

  @Test
  fun `Cancelling stops between requests`() {
    val source = FakeOrigin(fileOf(40))
    var asked = 0
    PathDownloader(store, source).load(course(0.0, 40.0), 1, isCancelled = { ++asked > 1 })
    assertTrue(source.requests.size <= 2)
  }

  @Test
  fun `The list survives a restart`() {
    val source = FakeOrigin(fileOf(10))
    PathDownloader(store, source).load(course(0.0, 2.0), 1)
    val restarted = PathStore(files)
    assertEquals(store.installed, restarted.installed)
    assertNotNull(PathRouter(restarted).way(at(0.5), at(1.5)))
  }

  @Test
  fun `A broken file counts as not installed`() {
    val source = FakeOrigin(fileOf(10))
    PathDownloader(store, source).load(course(0.0, 2.0), 1)
    val square = store.installed.first().square
    files.write("square_${square.latitudeIndex}_${square.longitudeIndex}", byteArrayOf(1, 2))
    assertEquals(listOf(), store.segments(square))
    assertTrue(store.installed.none { it.square == square })
  }

  @Test
  fun `The router reads newly arrived squares after forgetting`() {
    val source = FakeOrigin(fileOf(10))
    val router = PathRouter(store)
    assertNull(router.way(at(0.5), at(1.5)))
    val before = store.version
    PathDownloader(store, source).load(course(0.0, 2.0), 1)
    assertTrue(store.version > before)
    router.forgetSquares()
    assertNotNull(router.way(at(0.5), at(1.5)))
  }

  @Test
  fun `The storage limit removes the squares unused for the longest`() {
    assertEquals(50_000_000L, PathDownloader.MAX_STORE_BYTES)
    val big = InstalledSquare(1, 1, "x", 30_000_000, 1, PathFile.FORMAT_VERSION, 1)
    val newer = InstalledSquare(2, 2, "y", 30_000_000, 1, PathFile.FORMAT_VERSION, 5)
    files.write(
      "squares.json",
      kotlinx.serialization.json.Json.encodeToString(listOf(big, newer)).encodeToByteArray(),
    )
    files.write("square_1_1", byteArrayOf(1))
    files.write("square_2_2", byteArrayOf(1))
    val full = PathStore(files)
    PathDownloader(full, FakeOrigin(fileOf(10))).load(course(0.0, 2.0), 9)
    assertTrue(full.installed.none { it.square == big.square })
    assertTrue(full.installed.any { it.square == newer.square })
    assertTrue(full.installed.any { it.square !in setOf(big.square, newer.square) })
  }
}
