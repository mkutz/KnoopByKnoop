package com.knoopbyknoop.android

import com.knoopbyknoop.core.NetworkIndex
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Fetches the published index of regions. */
object NetworkIndexClient {

  /** The published index, or `null` without network access. */
  suspend fun fetch(): NetworkIndex? =
    withContext(Dispatchers.IO) {
      try {
        val connection =
          URL(RegionDownloads.BASE_URL + "index.json").openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.inputStream.use { NetworkIndex.parse(it.readBytes().decodeToString()) }
      } catch (exception: IOException) {
        null
      }
    }
}
