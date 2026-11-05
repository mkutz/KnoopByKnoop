package com.knoopbyknoop.core

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.posix.SEEK_SET
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek

/**
 * A router over the path file at [path], or `null` if there is none or it can't be read: read in
 * the core, as copying bytes from Swift is slow.
 */
@OptIn(ExperimentalForeignApi::class)
fun openPathRouter(path: String): PathRouter? {
  val file = fopen(path, "rb") ?: return null
  return try {
    PathRouter(
      PathFile.open { offset, size ->
        val bytes = ByteArray(size)
        fseek(file, offset.convert(), SEEK_SET)
        if (size > 0) {
          bytes.usePinned { fread(it.addressOf(0), 1.convert(), size.convert(), file) }
        }
        bytes
      }
    )
  } catch (exception: GraphFormatException) {
    null
  }
}
