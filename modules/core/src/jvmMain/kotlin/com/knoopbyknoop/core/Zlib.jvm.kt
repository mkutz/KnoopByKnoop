package com.knoopbyknoop.core

import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

actual fun deflate(bytes: ByteArray): ByteArray {
  val deflater = Deflater(Deflater.BEST_COMPRESSION)
  try {
    deflater.setInput(bytes)
    deflater.finish()
    val output = ByteArrayOutputStream(bytes.size / 2 + 16)
    val buffer = ByteArray(16 * 1024)
    while (!deflater.finished()) output.write(buffer, 0, deflater.deflate(buffer))
    return output.toByteArray()
  } finally {
    deflater.end()
  }
}

actual fun inflate(bytes: ByteArray, size: Int): ByteArray {
  val inflater = Inflater()
  try {
    inflater.setInput(bytes)
    val result = ByteArray(size)
    var position = 0
    while (position < size) {
      val count = inflater.inflate(result, position, size - position)
      if (count == 0 && (inflater.finished() || inflater.needsInput())) break
      position += count
    }
    if (position != size) throw GraphFormatException("Unpacked $position bytes, expected $size")
    return result
  } catch (exception: DataFormatException) {
    throw GraphFormatException("Can't unpack: ${exception.message}")
  } finally {
    inflater.end()
  }
}
