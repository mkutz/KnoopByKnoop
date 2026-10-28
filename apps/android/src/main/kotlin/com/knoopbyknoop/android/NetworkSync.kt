package com.knoopbyknoop.android

import android.content.Context
import androidx.core.content.edit
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.knoopbyknoop.core.GraphFile
import com.knoopbyknoop.core.IndexedFileKind
import com.knoopbyknoop.core.IndexedNetworkType
import com.knoopbyknoop.core.InstallCheck
import com.knoopbyknoop.core.NetworkCatalog
import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.NetworkIndexEntry
import com.knoopbyknoop.core.NetworkInstaller
import com.knoopbyknoop.core.RejectionReason
import com.knoopbyknoop.core.UpdateCheck
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gets the networks onto the device without the user doing anything, see `network-sync`: installs
 * the graphs bundled into the app, and downloads the files the device still needs one at a time, so
 * a retry only fetches what is still missing.
 */
class NetworkSync(private val context: Context) {

  private val storage = NetworkStorage(context.filesDir)
  private val preferences = context.getSharedPreferences("network-sync", Context.MODE_PRIVATE)

  /** The networks whose last download failed. */
  val failed: Set<IndexedNetworkType>
    get() = networks(KEY_FAILED)

  /** The networks being downloaded right now. */
  val downloading: Set<IndexedNetworkType>
    get() = networks(KEY_DOWNLOADING)

  /** Whether a downloaded file needs a newer app version to be read. */
  val needsAppUpdate: Boolean
    get() = preferences.getBoolean(KEY_NEEDS_APP_UPDATE, false)

  /** Whether the sync should run: files are missing, or the daily update check is due. */
  fun isDue(): Boolean {
    val lastCheck = preferences.getLong(KEY_LAST_CHECK, 0).takeIf { it > 0 }
    val catalog = NetworkCatalog(index = null, storage.installed())
    return UpdateCheck.isDue(lastCheck, System.currentTimeMillis()) ||
      catalog.networks.any { !it.hasGeometry }
  }

  /**
   * Installs the bundled graphs of networks that have no installed graph, only an older one, or the
   * same data in an older format, and removes stale data, e.g. networks installed per region by
   * earlier app versions.
   */
  fun installBundled() {
    try {
      installBundledGraphs()
    } finally {
      storage.removeStaleData()
    }
  }

  private fun installBundledGraphs() {
    val bundled =
      try {
        context.assets.open("$BUNDLED_DIRECTORY/${NetworkIndex.BUNDLED_INDEX_FILE}").use {
          NetworkIndex.parse(it.readBytes().decodeToString())
        }
      } catch (exception: IOException) {
        return // A debug build without network access bundles nothing.
      }
    val graphs = bundled.files.filter { it.kind == IndexedFileKind.GRAPH }
    val installedVersions =
      graphs
        .mapNotNull { entry ->
          storage.graphFormatVersion(entry.network)?.let { entry.network to it }
        }
        .toMap()
    val bundledVersions =
      graphs
        .mapNotNull { entry ->
          val header =
            NetworkStorage.readHeader(context.assets.open("$BUNDLED_DIRECTORY/${entry.file}"))
          GraphFile.formatVersion(header)?.let { entry.network to it }
        }
        .toMap()
    val toInstall =
      NetworkCatalog(index = null, storage.installed())
        .bundledToInstall(bundled, installedVersions, bundledVersions)
    for (entry in toInstall) {
      val bytes = context.assets.open("$BUNDLED_DIRECTORY/${entry.file}").use { it.readBytes() }
      if (NetworkInstaller.checkGraph(entry, bytes) is InstallCheck.Valid) {
        storage.install(entry, bytes)
      }
    }
  }

  /**
   * Fetches the published index and installs every file the device still needs, cycling first.
   *
   * @param onInstalled called after each installed file
   * @return whether a file was rejected as corrupted, so the sync should be retried later
   * @throws IOException without network access or when a download breaks off
   */
  suspend fun run(onInstalled: suspend (NetworkIndexEntry) -> Unit): Boolean {
    val index = NetworkIndex.parse(fetch("index.json").decodeToString())
    preferences.edit { putLong(KEY_LAST_CHECK, System.currentTimeMillis()) }
    val toFetch = NetworkCatalog(index, storage.installed()).filesToFetch
    var retry = false
    setNetworks(KEY_DOWNLOADING, toFetch.map { it.network }.toSet())
    try {
      for (entry in toFetch) {
        val bytes =
          try {
            fetch(entry.file)
          } catch (exception: IOException) {
            setNetworks(KEY_FAILED, failed + entry.network)
            throw exception
          }
        when (val check = verify(entry, bytes)) {
          is InstallCheck.Valid -> {
            storage.install(entry, bytes)
            setNetworks(KEY_FAILED, failed - entry.network)
            storage.removeStaleData()
            onInstalled(entry)
          }
          is InstallCheck.Rejected -> {
            setNetworks(KEY_FAILED, failed + entry.network)
            if (check.reason == RejectionReason.UNSUPPORTED_VERSION) {
              preferences.edit { putBoolean(KEY_NEEDS_APP_UPDATE, true) }
            } else {
              retry = true
            }
          }
        }
      }
    } finally {
      setNetworks(KEY_DOWNLOADING, setOf())
    }
    if (!retry) preferences.edit { putBoolean(KEY_NEEDS_APP_UPDATE, false) }
    return retry
  }

  /** A graph is checked on its own; a geometry against the graph installed before it. */
  private fun verify(entry: NetworkIndexEntry, bytes: ByteArray): InstallCheck<*> =
    when (entry.kind) {
      IndexedFileKind.GRAPH -> NetworkInstaller.checkGraph(entry, bytes)
      IndexedFileKind.GEOMETRY -> {
        val graphEntry = storage.graphEntry(entry.network)
        val graphFile = storage.graph(entry.network)
        if (graphEntry == null || graphFile == null) {
          InstallCheck.Rejected(RejectionReason.MISMATCHING_GEOMETRY)
        } else {
          NetworkInstaller.checkGeometry(entry, bytes, graphEntry, graphFile.graph)
        }
      }
    }

  private suspend fun fetch(file: String): ByteArray =
    withContext(Dispatchers.IO) {
      val connection =
        URL(NetworkIndex.PUBLISHED_BASE_URL + file).openConnection() as HttpURLConnection
      connection.connectTimeout = 15_000
      connection.readTimeout = 30_000
      try {
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
          throw IOException("$file: HTTP ${connection.responseCode}")
        }
        connection.inputStream.use { it.readBytes() }
      } finally {
        connection.disconnect()
      }
    }

  private fun networks(key: String): Set<IndexedNetworkType> =
    preferences.getStringSet(key, setOf()).orEmpty().map { IndexedNetworkType.valueOf(it) }.toSet()

  private fun setNetworks(key: String, networks: Set<IndexedNetworkType>) {
    preferences.edit { putStringSet(key, networks.map { it.name }.toSet()) }
  }

  companion object {
    private const val BUNDLED_DIRECTORY = "bundled-networks"
    private const val KEY_LAST_CHECK = "last-check"
    private const val KEY_FAILED = "failed"
    private const val KEY_DOWNLOADING = "downloading"
    private const val KEY_NEEDS_APP_UPDATE = "needs-app-update"
  }
}

/**
 * Runs [NetworkSync] in the background. It waits for network access by itself and retries with a
 * growing delay, so the download starts and continues without the user doing anything.
 */
class NetworkSyncWorker(context: Context, parameters: WorkerParameters) :
  CoroutineWorker(context, parameters) {

  override suspend fun doWork(): Result =
    try {
      val retry =
        NetworkSync(applicationContext).run { entry ->
          setProgress(workDataOf(KEY_INSTALLED to entry.file))
        }
      if (retry) Result.retry() else Result.success()
    } catch (exception: IOException) {
      Result.retry()
    }

  companion object {
    const val NAME = "network-sync"
    const val KEY_INSTALLED = "installed"

    /**
     * @param restart whether to restart a sync that is waiting to retry, e.g. on the user's wish
     */
    fun enqueue(context: Context, restart: Boolean = false) {
      val request =
        OneTimeWorkRequestBuilder<NetworkSyncWorker>()
          .setConstraints(
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
          )
          .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
          .build()
      WorkManager.getInstance(context)
        .enqueueUniqueWork(
          NAME,
          if (restart) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
          request,
        )
    }
  }
}
