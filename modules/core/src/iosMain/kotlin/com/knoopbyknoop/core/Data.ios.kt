package com.knoopbyknoop.core

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.posix.memcpy

/** The bytes of [data], copied in one go: element-wise copying from Swift is slow for megabytes. */
@OptIn(ExperimentalForeignApi::class)
fun byteArrayOf(data: NSData): ByteArray {
  val bytes = ByteArray(data.length.toInt())
  if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
  return bytes
}
