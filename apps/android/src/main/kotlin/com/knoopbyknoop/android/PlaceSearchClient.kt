package com.knoopbyknoop.android

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Sends the place search's requests to Photon, see `place-search`. */
object PlaceSearchClient {

  /** Photon asks to be told who is calling. */
  private const val USER_AGENT = "knoopbyknoop-android (https://github.com/mkutz/knoopbyknoop)"

  /** @throws IOException when Photon does not answer, or answers with an error */
  suspend fun fetch(url: String): String =
    withContext(Dispatchers.IO) {
      val connection = URL(url).openConnection() as HttpURLConnection
      connection.connectTimeout = 10_000
      connection.readTimeout = 10_000
      connection.setRequestProperty("User-Agent", USER_AGENT)
      try {
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
          throw IOException("Photon answered HTTP ${connection.responseCode}")
        }
        connection.inputStream.use { it.readBytes().decodeToString() }
      } finally {
        connection.disconnect()
      }
    }
}
