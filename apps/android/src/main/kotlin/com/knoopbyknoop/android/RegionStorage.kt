package com.knoopbyknoop.android

import com.knoopbyknoop.core.GraphFile
import com.knoopbyknoop.core.GraphFormatException
import com.knoopbyknoop.core.IndexedFileKind
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.NetworkGeometry
import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.NetworkIndexEntry
import com.knoopbyknoop.core.RouteSnapshot
import java.io.File

/**
 * The installed regions and the current route in app-private storage: `regions/<region>/<file>`,
 * `regions/installed.json` (the installed index entries) and `route.json`. Files are written to a
 * temporary file and renamed, so they are never half-written.
 */
class RegionStorage(directory: File) {

  private val regionsDirectory = File(directory, "regions")
  private val installedFile = File(regionsDirectory, "installed.json")
  private val routeFile = File(directory, "route.json")

  fun installed(): List<NetworkIndexEntry> =
    if (installedFile.exists()) NetworkIndex.parse(installedFile.readText()).files else listOf()

  fun graph(region: String, network: IndexedNetworkType): GraphFile? =
    entry(region, network, IndexedFileKind.GRAPH)?.let { GraphFile.read(fileOf(it).readBytes()) }

  /** The installed geometry, or `null` if none belongs to the installed graph. */
  fun geometry(
    region: String,
    network: IndexedNetworkType,
    graphFile: GraphFile,
  ): NetworkGeometry? {
    val graphEntry = entry(region, network, IndexedFileKind.GRAPH) ?: return null
    val geometryEntry = entry(region, network, IndexedFileKind.GEOMETRY) ?: return null
    return try {
      NetworkGeometry.read(fileOf(geometryEntry).readBytes(), graphFile.graph, graphEntry.sha256)
    } catch (exception: GraphFormatException) {
      null
    }
  }

  /**
   * A map layer of an installed network as GeoJSON, built by [build] only once per installed data:
   * building it for hundreds of thousands of points takes seconds on a phone.
   */
  fun mapLayer(
    region: String,
    network: IndexedNetworkType,
    name: String,
    build: () -> String,
  ): String {
    val dataKey =
      listOf(IndexedFileKind.GRAPH, IndexedFileKind.GEOMETRY).joinToString("-") {
        entry(region, network, it)?.sha256?.take(12) ?: "none"
      }
    val prefix = "${network.name.lowercase()}-$name-"
    val directory = File(regionsDirectory, region)
    val file = File(directory, "$prefix$dataKey.geojson")
    if (file.exists()) return file.readText()
    directory.listFiles { candidate -> candidate.name.startsWith(prefix) }?.forEach { it.delete() }
    return build().also { writeAtomically(file, it.encodeToByteArray()) }
  }

  /**
   * Installs verified files, replacing the installed files of the same region, network and kind.
   */
  fun install(files: Map<NetworkIndexEntry, ByteArray>) {
    for ((entry, bytes) in files) writeAtomically(fileOf(entry), bytes)
    val replaced = files.keys.map { Triple(it.region, it.network, it.kind) }.toSet()
    saveInstalled(
      installed().filter { Triple(it.region, it.network, it.kind) !in replaced } + files.keys
    )
  }

  /** Removes a region's files, and the current route if it is planned on that region. */
  fun remove(region: String) {
    saveInstalled(installed().filter { it.region != region })
    File(regionsDirectory, region).deleteRecursively()
    if (route()?.isFor(region) == true) saveRoute(null)
  }

  fun route(): RouteSnapshot? =
    if (routeFile.exists()) runCatching { RouteSnapshot.parse(routeFile.readText()) }.getOrNull()
    else null

  fun saveRoute(snapshot: RouteSnapshot?) {
    if (snapshot == null) routeFile.delete()
    else writeAtomically(routeFile, snapshot.toJson().encodeToByteArray())
  }

  private fun entry(region: String, network: IndexedNetworkType, kind: IndexedFileKind) =
    installed().firstOrNull { it.region == region && it.network == network && it.kind == kind }

  private fun fileOf(entry: NetworkIndexEntry) =
    File(File(regionsDirectory, entry.region), entry.file)

  private fun saveInstalled(entries: List<NetworkIndexEntry>) {
    val index =
      NetworkIndex(formatVersion = GraphFile.FORMAT_VERSION, generatedAt = "", files = entries)
    writeAtomically(installedFile, index.toJson().encodeToByteArray())
  }

  private fun writeAtomically(file: File, bytes: ByteArray) {
    file.parentFile?.mkdirs()
    val temporary = File(file.parentFile, "${file.name}.tmp")
    temporary.writeBytes(bytes)
    check(temporary.renameTo(file)) { "Could not install ${file.name}" }
  }
}
