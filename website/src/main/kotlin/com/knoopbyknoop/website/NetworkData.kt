package com.knoopbyknoop.website

import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.PathIndexEntry
import com.knoopbyknoop.core.sha256Hex
import java.io.File
import java.io.RandomAccessFile

/**
 * The folder under `data/` that names which publication the planner reads, see [writeNetworkData].
 */
const val CURRENT_DATA_FILE = "data/index.json"

/**
 * The published network data in [directory], as `gh release download network-data` writes it: its
 * index and every file it names, see `web-planner` "Network data from the website".
 */
class NetworkData(val directory: File) {

  val index: NetworkIndex by lazy { NetworkIndex.parse(directory.resolve("index.json").readText()) }

  /** The publication's own folder, from the time it was generated, e.g. `20261123T155202Z`. */
  val folder: String
    get() = index.generatedAt.filter { it.isLetterOrDigit() }

  /** Every file of the publication, the index first. */
  val files: List<String>
    get() =
      listOf("index.json") +
        index.files.map { it.file } +
        listOfNotNull(index.paths?.file, index.places?.file)
}

/**
 * What is wrong with the published data in [data]: the index missing or unreadable, or a file it
 * names missing or not matching it. The planner would refuse these files, so the site isn't
 * published with them.
 */
fun networkDataProblems(data: NetworkData): List<String> {
  val indexFile = data.directory.resolve("index.json")
  if (!indexFile.isFile) return listOf("Network data has no index: $indexFile")
  val index =
    try {
      data.index
    } catch (exception: Exception) {
      return listOf("Network data index can't be read: ${exception.message}")
    }
  val problems = mutableListOf<String>()
  for (entry in index.files) {
    val file = data.directory.resolve(entry.file)
    when {
      !file.isFile -> problems += "Network data file missing: ${entry.file}"
      !entry.matches(file.readBytes()) ->
        problems += "Network data file doesn't match the index: ${entry.file}"
    }
  }
  for (entry in listOfNotNull(index.paths, index.places)) {
    squareFileProblem(data.directory, entry)?.let { problems += it }
  }
  return problems
}

/** What is wrong with a file of squares: missing, of another size, or a broken directory. */
private fun squareFileProblem(directory: File, entry: PathIndexEntry): String? {
  val file = directory.resolve(entry.file)
  if (!file.isFile) return "Network data file missing: ${entry.file}"
  if (file.length() != entry.sizeBytes)
    return "Network data file doesn't match the index: ${entry.file}"
  val bytes = ByteArray(entry.directorySize)
  RandomAccessFile(file, "r").use {
    it.seek(entry.directoryOffset)
    it.readFully(bytes)
  }
  return if (sha256Hex(bytes) == entry.directorySha256) null
  else "Network data file doesn't match the index: ${entry.file}"
}

/**
 * Copies [data] into `data/<folder>/` of the site in [siteDirectory] and writes [CURRENT_DATA_FILE]
 * naming that folder, so a page never mixes files of two publications.
 */
fun writeNetworkData(data: NetworkData, siteDirectory: File) {
  val target = siteDirectory.resolve("data/${data.folder}")
  target.mkdirs()
  for (name in data.files) data.directory
    .resolve(name)
    .copyTo(target.resolve(name), overwrite = true)
  siteDirectory.resolve(CURRENT_DATA_FILE).writeText("""{"folder":"${data.folder}/"}""" + "\n")
}
