package com.knoopbyknoop.core

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
actual fun sha256Hex(bytes: ByteArray): String {
  val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
  // Pinning needs at least one element, so an empty input is hashed from a one-byte buffer.
  val input = if (bytes.isEmpty()) ByteArray(1) else bytes
  input.usePinned { pinnedInput ->
    digest.usePinned { pinnedDigest ->
      CC_SHA256(pinnedInput.addressOf(0), bytes.size.convert(), pinnedDigest.addressOf(0))
    }
  }
  return digest.joinToString("") { it.toString(16).padStart(2, '0') }
}
