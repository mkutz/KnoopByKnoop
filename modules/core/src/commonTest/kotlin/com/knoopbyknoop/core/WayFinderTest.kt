package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Covers `knot-navigation` "Path data arrives on the way" for the way finder of both apps. */
class WayFinderTest {

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

  private val start = GeoPoint(50.80, 7.20)
  private val end = GeoPoint(50.80, 7.20 + 1000 / 70_300.0)
  private val search = WaySearch(start, end, null)

  private val store = PathStore(MemoryFiles())

  /** Installs the square with a cycle path from [start] to [end]. */
  private fun installPath() {
    val square = PathSquare.of(start.latitude, start.longitude)
    val segment = PathSegment(1, 2, PathKind.CYCLE, true, true, 1.0, null, listOf(start, end))
    val encoded = PathFile.encode(square, listOf(segment))
    store.install(
      InstalledSquare(
        square.latitudeIndex,
        square.longitudeIndex,
        sha256Hex(encoded.bytes),
        encoded.bytes.size,
        encoded.rawSize,
        PathFile.FORMAT_VERSION,
        0,
      ),
      encoded.bytes,
    )
  }

  @Test
  fun `squares arriving after a search are read at the next one`() {
    val finder = WayFinder(store)
    assertNull(finder.way(search))

    installPath()

    assertNotNull(finder.way(search))
  }
}
