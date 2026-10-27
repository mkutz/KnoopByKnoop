package com.knoopbyknoop.core

/** How far a network is on the device, see `network-sync`. */
enum class NetworkState {
  /** No graph installed: nothing to plan on yet. */
  MISSING,

  /** Files of this network are being downloaded. */
  DOWNLOADING,

  /** Installed, and nothing newer published. */
  INSTALLED,

  /** Installed, with newer files published that are not downloaded yet. */
  UPDATABLE,
}

/**
 * One network type as the apps show it, e.g. in the info sheet.
 *
 * @property dataDate the date of the installed graph's data, or `null` when no graph is installed
 * @property hasGeometry whether the courses of the connections are installed; without them,
 *   connections are drawn as straight lines
 * @property lastAttemptFailed whether the last download of this network failed
 */
data class NetworkStatus(
  val network: IndexedNetworkType,
  val state: NetworkState,
  val dataDate: String?,
  val hasGeometry: Boolean,
  val lastAttemptFailed: Boolean,
)

/**
 * Combines the published index with the networks installed on the device, see `network-sync`.
 *
 * @param index the published index, or `null` before it could be fetched
 * @param installed the index entries of all installed files
 * @param downloading the networks currently being downloaded
 * @param failed the networks whose last download failed
 */
class NetworkCatalog(
  index: NetworkIndex?,
  private val installed: List<NetworkIndexEntry>,
  downloading: Set<IndexedNetworkType> = setOf(),
  failed: Set<IndexedNetworkType> = setOf(),
) {

  /**
   * The published files the device still needs, cycling first and each graph before its geometry:
   * files of a network and kind that are not installed, or that differ from the installed file and
   * are not older than it. Installed files of another area, e.g. of one region from before the
   * networks were merged, cannot be compared by date and are always replaced.
   */
  val filesToFetch: List<NetworkIndexEntry> =
    index?.files.orEmpty().filter { isNeeded(it) }.sortedWith(FETCH_ORDER)

  /** Cycling first, then hiking. */
  val networks: List<NetworkStatus> =
    IndexedNetworkType.entries.map { network ->
      val graph = installedFile(network, IndexedFileKind.GRAPH)
      val geometry = installedFile(network, IndexedFileKind.GEOMETRY)
      NetworkStatus(
        network = network,
        state =
          when {
            network in downloading -> NetworkState.DOWNLOADING
            graph == null -> NetworkState.MISSING
            filesToFetch.any { it.network == network } -> NetworkState.UPDATABLE
            else -> NetworkState.INSTALLED
          },
        dataDate = graph?.dataDate,
        // Geometry is only usable with the graph it was built with; a newer graph makes it stale.
        hasGeometry = graph != null && geometry != null && geometry.dataDate == graph.dataDate,
        lastAttemptFailed = network in failed,
      )
    }

  fun status(network: IndexedNetworkType): NetworkStatus = networks.first { it.network == network }

  /**
   * The bundled graphs to install on start, see `network-sync` "Bundled graphs": those whose
   * network has no installed graph yet, only an older one, or one of another area.
   */
  fun bundledToInstall(bundled: NetworkIndex): List<NetworkIndexEntry> =
    bundled.files
      .filter { it.kind == IndexedFileKind.GRAPH }
      .filter { entry ->
        val installedGraph = installedFile(entry.network, IndexedFileKind.GRAPH)
        installedGraph == null ||
          installedGraph.area != entry.area ||
          installedGraph.dataDate < entry.dataDate
      }
      .sortedWith(FETCH_ORDER)

  private fun installedFile(network: IndexedNetworkType, kind: IndexedFileKind) =
    installed.firstOrNull {
      it.network == network && it.kind == kind
    }

  private fun isNeeded(published: NetworkIndexEntry): Boolean {
    val installedFile = installedFile(published.network, published.kind) ?: return true
    if (installedFile.area != published.area) return true
    return installedFile.sha256 != published.sha256 && published.dataDate >= installedFile.dataDate
  }

  private companion object {
    val FETCH_ORDER = compareBy<NetworkIndexEntry>({ it.network.ordinal }, { it.kind.ordinal })
  }
}

/**
 * When the apps check the published index for newer data, see `network-sync` "Keep the networks
 * current": on start, at most once a day.
 */
object UpdateCheck {

  const val INTERVAL_MILLIS: Long = 24 * 60 * 60 * 1000

  /** @param lastCheckEpochMillis when the index was last fetched, or `null` if never */
  fun isDue(lastCheckEpochMillis: Long?, nowEpochMillis: Long): Boolean =
    lastCheckEpochMillis == null || nowEpochMillis - lastCheckEpochMillis >= INTERVAL_MILLIS
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

/** Checks downloaded and bundled files before an app installs them, see `network-sync`. */
object NetworkInstaller {

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
