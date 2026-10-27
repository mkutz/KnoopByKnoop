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
 * The installed networks and the current route in app-private storage: `networks/<file>`,
 * `networks/installed.json` (the installed index entries) and `route.json`. Files are written to a
 * temporary file and renamed, so they are never half-written.
 */
class NetworkStorage(private val directory: File) {

  private val networksDirectory = File(directory, "networks")
  private val installedFile = File(networksDirectory, "installed.json")
  private val routeFile = File(directory, "route.json")

  fun installed(): List<NetworkIndexEntry> =
    if (installedFile.exists()) NetworkIndex.parse(installedFile.readText()).files else listOf()

  fun graphEntry(network: IndexedNetworkType): NetworkIndexEntry? =
    entry(network, IndexedFileKind.GRAPH)

  fun graph(network: IndexedNetworkType): GraphFile? =
    graphEntry(network)?.let { GraphFile.read(fileOf(it).readBytes()) }

  /** The installed geometry, or `null` if none belongs to the installed graph. */
  fun geometry(network: IndexedNetworkType, graphFile: GraphFile): NetworkGeometry? {
    val graphEntry = graphEntry(network) ?: return null
    val geometryEntry = entry(network, IndexedFileKind.GEOMETRY) ?: return null
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
  fun mapLayer(network: IndexedNetworkType, name: String, build: () -> String): String {
    val dataKey =
      listOf(IndexedFileKind.GRAPH, IndexedFileKind.GEOMETRY).joinToString("-") {
        entry(network, it)?.sha256?.take(12) ?: "none"
      }
    val prefix = "${network.name.lowercase()}-$name-"
    val file = File(networksDirectory, "$prefix$dataKey.geojson")
    if (file.exists()) return file.readText()
    networksDirectory
      .listFiles { candidate -> candidate.name.startsWith(prefix) }
      ?.forEach {
        it.delete()
      }
    return build().also { writeAtomically(file, it.encodeToByteArray()) }
  }

  /** Installs a verified file, replacing the installed file of the same network and kind. */
  fun install(entry: NetworkIndexEntry, bytes: ByteArray) {
    writeAtomically(fileOf(entry), bytes)
    saveInstalled(
      installed().filter { it.network != entry.network || it.kind != entry.kind } + entry
    )
  }

  /**
   * Deletes data that is no longer installed: the networks installed per region by earlier app
   * versions, see `network-sync`, and files replaced by files of another name. The saved route
   * stays and is restored on the new networks.
   */
  fun removeStaleData() {
    File(directory, "regions").deleteRecursively()
    val installedFiles = installed().map { it.file }.toSet()
    networksDirectory
      .listFiles { file ->
        (file.name.endsWith(".graph") || file.name.endsWith(".geometry")) &&
          file.name !in installedFiles
      }
      ?.forEach { it.delete() }
  }

  fun route(): RouteSnapshot? =
    if (routeFile.exists()) runCatching { RouteSnapshot.parse(routeFile.readText()) }.getOrNull()
    else null

  fun saveRoute(snapshot: RouteSnapshot?) {
    if (snapshot == null) routeFile.delete()
    else writeAtomically(routeFile, snapshot.toJson().encodeToByteArray())
  }

  private fun entry(network: IndexedNetworkType, kind: IndexedFileKind) =
    installed().firstOrNull { it.network == network && it.kind == kind }

  private fun fileOf(entry: NetworkIndexEntry) = File(networksDirectory, entry.file)

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
