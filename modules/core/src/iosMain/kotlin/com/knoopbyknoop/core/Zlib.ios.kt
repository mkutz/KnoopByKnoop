package com.knoopbyknoop.core

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.zlib.Z_OK
import platform.zlib.compress2
import platform.zlib.compressBound
import platform.zlib.uLongfVar
import platform.zlib.uncompress

private const val BEST_COMPRESSION = 9

@OptIn(ExperimentalForeignApi::class)
actual fun deflate(bytes: ByteArray): ByteArray {
  val output = ByteArray(compressBound(bytes.size.convert()).toInt())
  memScoped {
    val length = alloc<uLongfVar>()
    length.value = output.size.convert()
    val status = output.usePinned { out ->
      bytes.usePinned { input ->
        compress2(
          out.addressOf(0).reinterpret(),
          length.ptr,
          if (bytes.isEmpty()) null else input.addressOf(0).reinterpret(),
          bytes.size.convert(),
          BEST_COMPRESSION,
        )
      }
    }
    if (status != Z_OK) throw GraphFormatException("Can't pack: $status")
    return output.copyOf(length.value.toInt())
  }
}

@OptIn(ExperimentalForeignApi::class)
actual fun inflate(bytes: ByteArray, size: Int): ByteArray {
  val result = ByteArray(size)
  if (size == 0) return result
  if (bytes.isEmpty()) throw GraphFormatException("Nothing to unpack")
  memScoped {
    val length = alloc<uLongfVar>()
    length.value = size.convert()
    val status = result.usePinned { out ->
      bytes.usePinned { input ->
        uncompress(
          out.addressOf(0).reinterpret(),
          length.ptr,
          input.addressOf(0).reinterpret(),
          bytes.size.convert(),
        )
      }
    }
    if (status != Z_OK || length.value.toInt() != size) {
      throw GraphFormatException("Can't unpack: $status")
    }
  }
  return result
}
