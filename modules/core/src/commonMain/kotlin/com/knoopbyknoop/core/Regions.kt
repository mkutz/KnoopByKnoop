package com.knoopbyknoop.core

/** What the user can do with a region, see `region-management`. */
enum class RegionState {
  /** Published, not on the device. */
  AVAILABLE,

  /** Being downloaded. */
  DOWNLOADING,

  /** On the device and current. */
  INSTALLED,

  /** On the device, with newer data published. */
  UPDATABLE,
}

/**
 * A region as the region list shows it.
 *
 * @property sizeBytes the download size of all its files, or their installed size when offline
 * @property dataDate the date of the newest data, published or installed
 * @property files the files to download (published) or the installed files (offline)
 */
data class RegionStatus(
  val region: String,
  val name: String,
  val sizeBytes: Long,
  val dataDate: String,
  val state: RegionState,
  val files: List<NetworkIndexEntry>,
)

/**
 * Combines the published index with the regions installed on the device.
 *
 * @param index the published index, or `null` without network access
 * @param installed the index entries of all installed files
 * @param downloading the regions currently being downloaded
 */
class RegionCatalog(
  index: NetworkIndex?,
  installed: List<NetworkIndexEntry>,
  downloading: Set<String> = setOf(),
) {

  /** Without the published index, only installed regions can be listed. */
  val otherRegionsNeedNetworkAccess: Boolean = index == null

  val regions: List<RegionStatus> = run {
    val installedByRegion = installed.groupBy { it.region }
    val publishedByRegion = index?.files.orEmpty().groupBy { it.region }
    (publishedByRegion.keys + installedByRegion.keys)
      .filter { index != null || it in installedByRegion }
      .sortedBy { nameOf(it) }
      .map { region ->
        val installedFiles = installedByRegion[region].orEmpty()
        val publishedFiles = publishedByRegion[region] ?: installedFiles
        val state =
          when {
            region in downloading -> RegionState.DOWNLOADING
            installedFiles.isEmpty() -> RegionState.AVAILABLE
            publishedFiles.any { published -> isNewer(published, installedFiles) } ->
              RegionState.UPDATABLE
            else -> RegionState.INSTALLED
          }
        RegionStatus(
          region = region,
          name = nameOf(region),
          sizeBytes = publishedFiles.sumOf { it.sizeBytes },
          dataDate = publishedFiles.maxOf { it.dataDate },
          state = state,
          files = publishedFiles,
        )
      }
  }

  private fun isNewer(
    published: NetworkIndexEntry,
    installedFiles: List<NetworkIndexEntry>,
  ): Boolean {
    val installedFile =
      installedFiles.firstOrNull { it.network == published.network && it.kind == published.kind }
        ?: return true
    return published.dataDate > installedFile.dataDate
  }

  companion object {

    private val NAMES =
      mapOf(
        "belgium" to "Belgium",
        "netherlands" to "Netherlands",
        "nordrhein-westfalen" to "Nordrhein-Westfalen",
      )

    /** The display name of a region ID; unknown IDs are shown capitalized. */
    fun nameOf(region: String): String =
      NAMES[region]
        ?: region.split('-').joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
  }
}

/** Why downloaded bytes must not be installed. */
enum class RejectionReason {
  /** Size or checksum differ from the index: the download is corrupted or incomplete. */
  CORRUPTED,

  /** The file is newer than this app version can read; the app needs an update. */
  UNSUPPORTED_VERSION,

  /** The geometry file does not belong to the graph file it is installed with. */
  MISMATCHING_GEOMETRY,
}

/** The result of checking downloaded bytes before installing them. */
sealed interface InstallCheck<out T> {

  data class Valid<T>(val content: T) : InstallCheck<T>

  data class Rejected(val reason: RejectionReason) : InstallCheck<Nothing>
}

/** Checks downloaded files before an app installs them, see `region-management`. */
object RegionInstaller {

  fun checkGraph(entry: NetworkIndexEntry, bytes: ByteArray): InstallCheck<GraphFile> {
    if (!entry.matches(bytes)) return InstallCheck.Rejected(RejectionReason.CORRUPTED)
    return try {
      InstallCheck.Valid(GraphFile.read(bytes))
    } catch (exception: UnsupportedFormatVersionException) {
      InstallCheck.Rejected(RejectionReason.UNSUPPORTED_VERSION)
    } catch (exception: GraphFormatException) {
      InstallCheck.Rejected(RejectionReason.CORRUPTED)
    }
  }

  /** @param graphEntry the index entry of the graph file [graph] was read from */
  fun checkGeometry(
    entry: NetworkIndexEntry,
    bytes: ByteArray,
    graphEntry: NetworkIndexEntry,
    graph: NetworkGraph,
  ): InstallCheck<NetworkGeometry> {
    if (!entry.matches(bytes)) return InstallCheck.Rejected(RejectionReason.CORRUPTED)
    return try {
      InstallCheck.Valid(NetworkGeometry.read(bytes, graph, graphEntry.sha256))
    } catch (exception: UnsupportedFormatVersionException) {
      InstallCheck.Rejected(RejectionReason.UNSUPPORTED_VERSION)
    } catch (exception: GraphFormatException) {
      InstallCheck.Rejected(RejectionReason.MISMATCHING_GEOMETRY)
    }
  }
}
