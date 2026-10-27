package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.IndexedFileKind
import com.knoopbyknoop.core.InstallCheck
import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.NetworkInstaller
import java.io.IOException
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

/** The published data cannot be bundled: it is unreachable, corrupted or of another format. */
class BundleException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Fetches the published graphs into [outputDirectory] to bundle them into the apps, together with a
 * [NetworkIndex.BUNDLED_INDEX_FILE] listing them, see `network-sync` "Bundled graphs". Every graph
 * is verified like a download before it is bundled.
 *
 * @param baseUrl where `index.json` and the files it lists are published
 * @param allowUnavailable whether to bundle no graphs instead of failing, for builds without
 *   network access; release builds must not allow it
 */
fun bundle(baseUrl: String, outputDirectory: Path, allowUnavailable: Boolean = false) {
  outputDirectory.createDirectories()
  outputDirectory.listDirectoryEntries().forEach { it.deleteIfExists() }
  val bundled =
    try {
      fetchVerifiedGraphs(URI(baseUrl.removeSuffix("/") + "/"), outputDirectory)
    } catch (exception: BundleException) {
      if (!allowUnavailable) throw exception
      System.err.println(
        "warning: bundling no graphs, the app downloads them: ${exception.message}"
      )
      outputDirectory.listDirectoryEntries().forEach { it.deleteIfExists() }
      NetworkIndex(formatVersion = 0, generatedAt = "", files = listOf())
    }
  outputDirectory.resolve(NetworkIndex.BUNDLED_INDEX_FILE).writeText(bundled.toJson())
  println("${NetworkIndex.BUNDLED_INDEX_FILE}: ${bundled.files.joinToString { it.file }}")
}

private fun fetchVerifiedGraphs(base: URI, outputDirectory: Path): NetworkIndex {
  val index =
    try {
      NetworkIndex.parse(fetch(base.resolve("index.json")).decodeToString())
    } catch (exception: IllegalArgumentException) {
      throw BundleException("index.json cannot be read", exception)
    }
  val graphs = index.files.filter { it.kind == IndexedFileKind.GRAPH }
  if (graphs.isEmpty()) throw BundleException("index.json lists no graphs")
  for (entry in graphs) {
    val bytes = fetch(base.resolve(entry.file))
    when (val check = NetworkInstaller.checkGraph(entry, bytes)) {
      is InstallCheck.Valid -> outputDirectory.resolve(entry.file).writeBytes(bytes)
      is InstallCheck.Rejected -> throw BundleException("${entry.file} is ${check.reason}")
    }
  }
  return index.copy(files = graphs)
}

private fun fetch(uri: URI): ByteArray =
  try {
    uri.toURL().openStream().use { it.readBytes() }
  } catch (exception: IOException) {
    throw BundleException("$uri cannot be fetched", exception)
  }
