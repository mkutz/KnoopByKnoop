package com.knoopbyknoop.android

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.NetworkIndexEntry
import com.knoopbyknoop.core.RegionStatus
import java.io.File

/**
 * Downloads a region's files with the system's [DownloadManager], which keeps downloading when the
 * app is in the background and resumes after connection drops. Started downloads are remembered, so
 * they are picked up again after the app was closed.
 */
class RegionDownloads(private val context: Context) {

  private val downloadManager = context.getSystemService(DownloadManager::class.java)
  private val preferences = context.getSharedPreferences("downloads", Context.MODE_PRIVATE)

  /** The state of a region's download. */
  sealed interface Progress {
    data class Running(val fraction: Float) : Progress

    data class Finished(val files: Map<NetworkIndexEntry, File>) : Progress

    data object Failed : Progress
  }

  private data class Pending(val entry: NetworkIndexEntry, val downloadId: Long)

  /** The regions with started downloads, finished or not. */
  fun regions(): Set<String> = preferences.all.keys

  fun start(region: RegionStatus) {
    cancel(region.region)
    val pending =
      region.files.map { entry ->
        val request =
          DownloadManager.Request(Uri.parse(BASE_URL + entry.file))
            .setTitle("Knoop by Knoop: ${region.name}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(
              context,
              null,
              "downloads/${entry.region}/${entry.file}",
            )
        Pending(entry, downloadManager.enqueue(request))
      }
    preferences.edit { putStringSet(region.region, pending.map(::encode).toSet()) }
  }

  fun progress(region: String): Progress? {
    val pending = preferences.getStringSet(region, null)?.map(::decode) ?: return null
    var downloadedBytes = 0L
    var allSuccessful = true
    val files = mutableMapOf<NetworkIndexEntry, File>()
    for (download in pending) {
      downloadManager.query(DownloadManager.Query().setFilterById(download.downloadId)).use { cursor
        ->
        if (!cursor.moveToFirst()) return Progress.Failed
        when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
          DownloadManager.STATUS_FAILED -> return Progress.Failed
          DownloadManager.STATUS_SUCCESSFUL -> {
            downloadedBytes += download.entry.sizeBytes
            // The download manager may pick another file name, e.g. if the file already exists.
            val localUri =
              cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            files[download.entry] = File(requireNotNull(Uri.parse(localUri).path))
          }
          else -> {
            allSuccessful = false
            downloadedBytes +=
              cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
              )
          }
        }
      }
    }
    if (allSuccessful) return Progress.Finished(files)
    val totalBytes = pending.sumOf { it.entry.sizeBytes }.coerceAtLeast(1)
    return Progress.Running(downloadedBytes.toFloat() / totalBytes)
  }

  /** Forgets a region's download and deletes its downloaded files. */
  fun cancel(region: String) {
    val pending = preferences.getStringSet(region, null)?.map(::decode).orEmpty()
    if (pending.isNotEmpty()) downloadManager.remove(*pending.map { it.downloadId }.toLongArray())
    File(context.getExternalFilesDir(null), "downloads/$region").deleteRecursively()
    preferences.edit { remove(region) }
  }

  // The index entry is stored as its JSON in a one-entry index, so no own format is needed.
  private fun encode(pending: Pending) =
    "${pending.downloadId} ${NetworkIndex(1, "", listOf(pending.entry)).toJson().replace('\n', ' ')}"

  private fun decode(text: String): Pending {
    val (downloadId, json) = text.split(' ', limit = 2)
    return Pending(NetworkIndex.parse(json).files.single(), downloadId.toLong())
  }

  companion object {
    const val BASE_URL = "https://github.com/mkutz/knoopbyknoop/releases/download/network-data/"
  }
}
