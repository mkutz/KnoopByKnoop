package com.knoopbyknoop.android

import android.content.Context
import com.knoopbyknoop.core.NetworkIndex
import com.knoopbyknoop.core.PathDataOrigin
import com.knoopbyknoop.core.PathStore
import com.knoopbyknoop.core.PathStoreFiles
import com.knoopbyknoop.core.PlaceStore
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** The squares of path data on the device, shared by planning and navigation. */
object PathData {

  @Volatile private var store: PathStore? = null

  fun store(context: Context): PathStore =
    store
      ?: synchronized(this) {
        store
          ?: PathStore(FilePathStoreFiles(File(context.applicationContext.filesDir, "paths")))
            .also {
              store = it
            }
      }
}

/** The squares of place details on the device, see `network-sync` "Keeping place details". */
object PlaceDetailsData {

  @Volatile private var store: PlaceStore? = null

  fun store(context: Context): PlaceStore =
    store
      ?: synchronized(this) {
        store
          ?: PlaceStore(FilePathStoreFiles(File(context.applicationContext.filesDir, "places")))
            .also { store = it }
      }
}

/** Files in [directory], written to a temporary file and renamed, so never half-written. */
private class FilePathStoreFiles(private val directory: File) : PathStoreFiles {

  override fun read(name: String): ByteArray? =
    File(directory, name).takeIf { it.exists() }?.readBytes()

  override fun write(name: String, bytes: ByteArray) {
    directory.mkdirs()
    val temporary = File.createTempFile(name, ".tmp", directory)
    temporary.writeBytes(bytes)
    if (!temporary.renameTo(File(directory, name))) {
      temporary.delete()
      throw IOException("Can't write $name")
    }
  }

  override fun delete(name: String) {
    File(directory, name).delete()
  }
}

/** The published path data, requested by range, see `network-sync` "Path data around the route". */
class HttpPathDataOrigin : PathDataOrigin {

  override fun index(): String =
    connect("index.json").use(HttpURLConnection.HTTP_OK) { it.readBytes() }.decodeToString()

  override fun range(file: String, offset: Long, size: Int): ByteArray =
    connect(file)
      .apply { setRequestProperty("Range", "bytes=$offset-${offset + size - 1}") }
      .use(HttpURLConnection.HTTP_PARTIAL) { it.readBytes() }

  private fun connect(file: String) =
    (URL(NetworkIndex.PUBLISHED_BASE_URL + file).openConnection() as HttpURLConnection).apply {
      connectTimeout = 15_000
      readTimeout = 30_000
    }

  private fun <T> HttpURLConnection.use(expected: Int, read: (java.io.InputStream) -> T): T =
    try {
      if (responseCode != expected) throw IOException("$url: HTTP $responseCode")
      inputStream.use(read)
    } finally {
      disconnect()
    }
}
