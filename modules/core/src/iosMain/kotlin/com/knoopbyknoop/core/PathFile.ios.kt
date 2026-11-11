package com.knoopbyknoop.core

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.Foundation.writeToFile
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait

/** The squares of path data in the app-private [directory]. */
fun openPathStore(directory: String): PathStore = PathStore(DirectoryPathStoreFiles(directory))

/** Loads squares into [store] from the published path data, by range. */
fun pathDownloader(store: PathStore): PathDownloader = PathDownloader(store, HttpPathDataOrigin())

/** The squares of place details in the app-private [directory]. */
fun openPlaceStore(directory: String): PlaceStore = PlaceStore(DirectoryPathStoreFiles(directory))

/** Loads squares into [store] from the published place details, by range. */
fun placeDownloader(store: PlaceStore): PlaceDownloader =
  PlaceDownloader(store, HttpPathDataOrigin())

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class DirectoryPathStoreFiles(private val directory: String) : PathStoreFiles {

  private val manager = NSFileManager.defaultManager

  override fun read(name: String): ByteArray? =
    NSData.dataWithContentsOfFile("$directory/$name")?.let(::byteArrayOf)

  /** Atomically, so a file is never half-written. */
  override fun write(name: String, bytes: ByteArray) {
    manager.createDirectoryAtPath(directory, true, null, null)
    val data =
      if (bytes.isEmpty()) {
        NSData()
      } else {
        bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.convert()) }
      }
    if (!data.writeToFile("$directory/$name", atomically = true)) {
      throw GraphFormatException("Can't write $name")
    }
  }

  override fun delete(name: String) {
    manager.removeItemAtPath("$directory/$name", null)
  }
}

/** Waits for each answer, as [PathDownloader] runs on a background queue of its own. */
@OptIn(ExperimentalForeignApi::class)
private class HttpPathDataOrigin : PathDataOrigin {

  private val session =
    NSURLSession.sessionWithConfiguration(
      NSURLSessionConfiguration.defaultSessionConfiguration.apply {
        timeoutIntervalForRequest = 30.0
      }
    )

  override fun index(): String = fetch("index.json", null, 200).decodeToString()

  override fun range(file: String, offset: Long, size: Int): ByteArray =
    fetch(file, "bytes=$offset-${offset + size - 1}", 206)

  private fun fetch(file: String, range: String?, expected: Long): ByteArray {
    val url = NSURL(string = NetworkIndex.PUBLISHED_BASE_URL + file)
    val request = NSMutableURLRequest(uRL = url)
    request.setHTTPMethod("GET")
    range?.let { request.setValue(it, forHTTPHeaderField = "Range") }
    val done = dispatch_semaphore_create(0)
    var result: ByteArray? = null
    var status = 0L
    session
      .dataTaskWithRequest(request) { data, response, _ ->
        status = (response as? NSHTTPURLResponse)?.statusCode ?: 0L
        result = data?.let(::byteArrayOf)
        dispatch_semaphore_signal(done)
      }
      .resume()
    dispatch_semaphore_wait(done, DISPATCH_TIME_FOREVER)
    if (status != expected) throw IllegalStateException("$file: HTTP $status")
    return result ?: throw IllegalStateException("$file: no answer")
  }
}
