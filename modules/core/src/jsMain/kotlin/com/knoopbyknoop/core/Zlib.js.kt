package com.knoopbyknoop.core

import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array

private fun ByteArray.toUint8Array(): Uint8Array {
  val signed = unsafeCast<Int8Array>()
  return Uint8Array(signed.buffer, signed.byteOffset, signed.length)
}

private fun Uint8Array.toByteArray(): ByteArray =
  Int8Array(buffer, byteOffset, length).unsafeCast<ByteArray>()

actual fun deflate(bytes: ByteArray): ByteArray =
  deflate(bytes.toUint8Array(), js("({ level: 9 })")).toByteArray()

actual fun inflate(bytes: ByteArray, size: Int): ByteArray {
  val result =
    try {
      inflate(bytes.toUint8Array()).toByteArray()
    } catch (exception: Throwable) {
      throw GraphFormatException("Can't unpack: ${exception.message}")
    }
  if (result.size != size)
    throw GraphFormatException("Unpacked ${result.size} bytes, expected $size")
  return result
}
