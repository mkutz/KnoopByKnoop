package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class PlaceStoreTest {

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

  /** The published index with the place details file; requests are counted, and can fail. */
  private class FakeOrigin(var file: ByteArray) : PathDataOrigin {
    var online = true
    val requests = ArrayList<Pair<Long, Int>>()
    var breakBytes: (Long, ByteArray) -> ByteArray = { _, bytes -> bytes }

    override fun index(): String {
      if (!online) throw IllegalStateException("offline")
      val info =
        PlaceFile.SQUARES.directoryInfo { offset, size ->
          file.copyOfRange(offset.toInt(), offset.toInt() + size)
        }
      val places =
        PathIndexEntry(
          "places.places",
          file.size.toLong(),
          info.dataDate,
          info.formatVersion,
          info.offset,
          info.size,
          info.sha256,
        )
      return NetworkIndex(2, "now", listOf(), places = places).toJson()
    }

    override fun range(file: String, offset: Long, size: Int): ByteArray {
      if (!online) throw IllegalStateException("offline")
      requests.add(offset to size)
      return breakBytes(offset, this.file.copyOfRange(offset.toInt(), offset.toInt() + size))
    }
  }

  private val origin = GeoPoint(51.70, 3.75)

  /** A point [east] and [north] kilometers from [origin]. */
  private fun at(east: Double, north: Double = 0.0) =
    GeoPoint(origin.latitude + north * 1000 / 111_195.0, origin.longitude + east * 1000 / 69_000.0)

  private fun squareAt(point: GeoPoint) = PathSquare.of(point.latitude, point.longitude)

  /** A café every 1 km east for [kilometers], feature id = kilometer + 1, website with [marker]. */
  private fun fileOf(kilometers: Int, marker: String = "a"): ByteArray {
    val squares = HashMap<PathSquare, MutableMap<Long, PlaceDetails>>()
    for (index in 0 until kilometers) {
      squares.getOrPut(squareAt(at(index.toDouble()))) { mutableMapOf() }[index + 1L] =
        PlaceDetails("https://$marker$index.nl", null, "Mo-Su 08:00-18:00")
    }
    return PlaceFile.toBytes(squares, "2026-11-10", "OSM")
  }

  private val files = MemoryFiles()
  private val store = PlaceStore(files)

  private fun course(fromKilometer: Double, toKilometer: Double) =
    listOf(at(fromKilometer), at(toKilometer))

  @Test
  fun `Cycling route planned at home shows contacts later without network access`() {
    val source = FakeOrigin(fileOf(40))
    val course = assertNotNull(PlaceSquares.courseToLoad(course(0.0, 8.0)))
    assertEquals(PathDataStatus.READY, PlaceDownloader(store, source).loadAround(course, 1))
    source.online = false
    assertEquals("https://a4.nl", store.details(squareAt(at(4.0)))[5]?.website)
  }

  // Scenario: Hiking route
  @Test
  fun `Hiking route loads place details as well`() {
    // Unlike path data, which only cycling routes load.
    assertNull(PathSquares.courseToLoad(NetworkType.HIKING, course(0.0, 1.0)))
    assertEquals(course(0.0, 1.0), PlaceSquares.courseToLoad(course(0.0, 1.0)))
  }

  @Test
  fun `Changing the route requests installed squares no more`() {
    val source = FakeOrigin(fileOf(20))
    val downloader = PlaceDownloader(store, source)
    downloader.loadAround(course(0.0, 2.0), 1)
    val first = source.requests.size
    val installed = store.installed.size
    downloader.loadAround(course(0.5, 2.5), 2)
    assertEquals(first, source.requests.size)
    assertEquals(installed, store.installed.size)
  }

  @Test
  fun `Navigating away from the route requests nothing for a tapped café`() {
    assertNull(PlaceSquares.squareToLoad(at(30.0, 8.0), navigating = true, store))
  }

  @Test
  fun `Without network access nothing is installed and it is tried again`() {
    val source = FakeOrigin(fileOf(10))
    source.online = false
    val downloader = PlaceDownloader(store, source)
    assertEquals(PathDataStatus.UNAVAILABLE, downloader.loadAround(course(0.0, 2.0), 1))
    assertEquals(listOf(), store.installed)
    assertEquals(mapOf(), store.details(squareAt(at(1.0))))
    source.online = true
    assertEquals(PathDataStatus.READY, downloader.loadAround(course(0.0, 2.0), 2))
  }

  @Test
  fun `Café far from the route gets its square while the route's squares stay`() {
    val source = FakeOrigin(fileOf(40))
    val downloader = PlaceDownloader(store, source)
    val route = course(0.0, 2.0)
    downloader.loadAround(route, 1)
    val routeSquares = store.installed.map { it.square }.toSet()
    val cafe = at(30.0)
    val square = assertNotNull(PlaceSquares.squareToLoad(cafe, navigating = false, store))
    assertEquals(squareAt(cafe), square)
    assertEquals(PathDataStatus.READY, downloader.loadSquare(square, route, 2))
    assertEquals(routeSquares + square, store.installed.map { it.square }.toSet())
    assertEquals("https://a30.nl", store.details(square)[31]?.website)
  }

  @Test
  fun `Square already installed needs no request`() {
    val source = FakeOrigin(fileOf(10))
    PlaceDownloader(store, source).loadAround(course(0.0, 2.0), 1)
    assertNull(PlaceSquares.squareToLoad(at(1.0), navigating = false, store))
  }

  @Test
  fun `Tap while navigating requests nothing`() {
    assertNull(PlaceSquares.squareToLoad(at(1.0), navigating = true, store))
  }

  @Test
  fun `Corrupted square of place details is discarded and the rest keep working`() {
    val source = FakeOrigin(fileOf(40))
    val downloader = PlaceDownloader(store, source)
    val directorySize = NetworkIndex.parse(source.index()).places!!.directorySize
    source.breakBytes = { offset, bytes ->
      if (offset > 9L + directorySize - 1) bytes.also { it[0] = (it[0] + 1).toByte() } else bytes
    }
    assertEquals(PathDataStatus.UNAVAILABLE, downloader.loadAround(course(0.0, 40.0), 1))
    source.breakBytes = { _, bytes -> bytes }
    assertEquals(PathDataStatus.READY, downloader.loadAround(course(0.0, 40.0), 2))
  }

  @Test
  fun `Newer place details published replace the squares of a new route only`() {
    val source = FakeOrigin(fileOf(40, marker = "old"))
    val downloader = PlaceDownloader(store, source)
    downloader.loadAround(course(0.0, 2.0), 1)
    downloader.loadAround(course(30.0, 32.0), 1)
    source.file = fileOf(40, marker = "new")
    downloader.loadAround(course(0.0, 2.0), 2)
    assertEquals("https://new1.nl", store.details(squareAt(at(1.0)))[2]?.website)
    assertEquals("https://old31.nl", store.details(squareAt(at(31.0)))[32]?.website)
  }

  @Test
  fun `Storage limit for place details removes the squares unused for the longest`() {
    assertEquals(10_000_000L, PlaceDownloader.MAX_STORE_BYTES)
    val big = InstalledSquare(1, 1, "x", 6_000_000, 1, PlaceFile.FORMAT_VERSION, 1)
    val newer = InstalledSquare(2, 2, "y", 6_000_000, 1, PlaceFile.FORMAT_VERSION, 5)
    files.write("squares.json", Json.encodeToString(listOf(big, newer)).encodeToByteArray())
    files.write("square_1_1", byteArrayOf(1))
    files.write("square_2_2", byteArrayOf(1))
    val full = PlaceStore(files)
    PlaceDownloader(full, FakeOrigin(fileOf(10))).loadAround(course(0.0, 2.0), 9)
    assertTrue(full.installed.none { it.square == big.square })
    assertTrue(full.installed.any { it.square == newer.square })
    assertTrue(full.installed.any { it.square !in setOf(big.square, newer.square) })
  }

  @Test
  fun `Deleting the place details removes them and nothing else`() {
    files.write("cycling.graph", byteArrayOf(1))
    PlaceDownloader(store, FakeOrigin(fileOf(10))).loadAround(course(0.0, 2.0), 1)
    assertTrue(store.installedBytes() > 0)
    store.clear()
    assertEquals(listOf(), store.installed)
    assertEquals(0, store.installedBytes())
    assertEquals(setOf("squares.json", "cycling.graph"), files.files.keys)
    assertNull(store.dataDate)
  }

  @Test
  fun `Deleting place details with a route planned loads nothing until it changes`() {
    val route = course(0.0, 1.0)
    assertNull(PlaceSquares.courseToLoad(route, deletedFor = course(0.0, 1.0)))
    assertEquals(course(0.0, 2.0), PlaceSquares.courseToLoad(course(0.0, 2.0), deletedFor = route))
    // A place tapped while planning still gets its square.
    assertNotNull(PlaceSquares.squareToLoad(at(0.5), navigating = false, store))
  }

  @Test
  fun `An incomplete route loads no place details`() {
    assertNull(PlaceSquares.courseToLoad(null))
    assertNull(PlaceSquares.courseToLoad(listOf()))
  }

  private val campsite = at(3.0)
  private val grol = PlaceFile.wayFeatureId(1333572075)
  private val cafe = PlaceFile.nodeFeatureId(7)

  private fun installed(details: Map<Long, PlaceDetails>): PlaceStore {
    val file = PlaceFile.toBytes(mapOf(squareAt(campsite) to details), "2026-11-10", "OSM")
    PlaceDownloader(store, FakeOrigin(file)).loadAround(listOf(campsite), 1)
    return store
  }

  private fun tapped(id: Long, kind: PlaceKind = PlaceKind.CAMPSITE) =
    MapPlace(campsite, "Minicamping Manege Grol", kind, kind.key, id)

  @Test
  fun `Campsite with website and phone has both on its card`() {
    val details = PlaceDetails("https://manegegrol.nl/voorbeeld-pagina/", "+31 111 461 623", null)
    assertEquals(details, installed(mapOf(grol to details)).detailsOf(tapped(grol)))
  }

  @Test
  fun `Only a phone number comes without a website`() {
    val details = PlaceDetails(null, "+31 111 461 210", null)
    val found = assertNotNull(installed(mapOf(cafe to details)).detailsOf(tapped(cafe)))
    assertNull(found.websiteUri)
    assertEquals("tel:+31111461210", found.dialUri)
  }

  // Scenario: No contacts
  @Test
  fun `No contacts for a place the file leaves out`() {
    val store = installed(mapOf(cafe to PlaceDetails(null, "+31 1", null)))
    assertNull(store.detailsOf(tapped(PlaceFile.nodeFeatureId(8), PlaceKind.WATER)))
  }

  @Test
  fun `Details not installed give no contacts`() {
    assertNull(store.detailsOf(tapped(grol)))
  }

  @Test
  fun `a place without a feature id has no details`() {
    val store = installed(mapOf(grol to PlaceDetails("https://a.nl", null, null)))
    assertNull(store.detailsOf(tapped(grol).copy(featureId = null)))
  }

  @Test
  fun `Contacts while navigating come from the installed squares as while planning`() {
    // The lookup reads only what is installed, so it is the same while navigating.
    val details = PlaceDetails("https://www.hetwapenvanzeeland.nl", null, null)
    assertEquals(details, installed(mapOf(cafe to details)).detailsOf(tapped(cafe, PlaceKind.FOOD)))
  }
}
