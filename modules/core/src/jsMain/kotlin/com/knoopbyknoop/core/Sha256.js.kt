package com.knoopbyknoop.core

/** The SHA-256 round constants. */
private val ROUND_CONSTANTS =
  intArrayOf(
    0x428a2f98,
    0x71374491,
    -0x4a3f0431,
    -0x164a245b,
    0x3956c25b,
    0x59f111f1,
    -0x6dc07d5c,
    -0x54e3a12b,
    -0x27f85568,
    0x12835b01,
    0x243185be,
    0x550c7dc3,
    0x72be5d74,
    -0x7f214e02,
    -0x6423f959,
    -0x3e640e8c,
    -0x1b64963f,
    -0x1041b87a,
    0x0fc19dc6,
    0x240ca1cc,
    0x2de92c6f,
    0x4a7484aa,
    0x5cb0a9dc,
    0x76f988da,
    -0x67c1aeae,
    -0x57ce3993,
    -0x4ffcd838,
    -0x40a68039,
    -0x391ff40d,
    -0x2a586eb9,
    0x06ca6351,
    0x14292967,
    0x27b70a85,
    0x2e1b2138,
    0x4d2c6dfc,
    0x53380d13,
    0x650a7354,
    0x766a0abb,
    -0x7e3d36d2,
    -0x6d8dd37b,
    -0x5d40175f,
    -0x57e599b5,
    -0x3db47490,
    -0x3893ae5d,
    -0x2e6d17e7,
    -0x2966f9dc,
    -0xbf1ca7b,
    0x106aa070,
    0x19a4c116,
    0x1e376c08,
    0x2748774c,
    0x34b0bcb5,
    0x391c0cb3,
    0x4ed8aa4a,
    0x5b9cca4f,
    0x682e6ff3,
    0x748f82ee,
    0x78a5636f,
    -0x7b3787ec,
    -0x7338fdf8,
    -0x6f410006,
    -0x5baf9315,
    -0x41065c09,
    -0x398e870e,
  )

/**
 * SHA-256 written out, since the browser's own is asynchronous and the core's callers expect an
 * answer right away.
 */
actual fun sha256Hex(bytes: ByteArray): String {
  val hash =
    intArrayOf(
      0x6a09e667,
      -0x4498517b,
      0x3c6ef372,
      -0x5ab00ac6,
      0x510e527f,
      -0x64fa9774,
      0x1f83d9ab,
      0x5be0cd19,
    )
  val bitLength = bytes.size.toLong() * 8
  val paddedSize = ((bytes.size + 9 + 63) / 64) * 64
  val message = ByteArray(paddedSize)
  bytes.copyInto(message)
  message[bytes.size] = 0x80.toByte()
  for (index in 0 until 8) {
    message[paddedSize - 1 - index] = (bitLength ushr (8 * index)).toByte()
  }
  val words = IntArray(64)
  for (block in 0 until paddedSize step 64) {
    for (index in 0 until 16) {
      val offset = block + index * 4
      words[index] =
        (message[offset].toInt() and 0xff shl 24) or
          (message[offset + 1].toInt() and 0xff shl 16) or
          (message[offset + 2].toInt() and 0xff shl 8) or
          (message[offset + 3].toInt() and 0xff)
    }
    for (index in 16 until 64) {
      val before15 = words[index - 15]
      val before2 = words[index - 2]
      val sigma0 = before15.rotateRight(7) xor before15.rotateRight(18) xor (before15 ushr 3)
      val sigma1 = before2.rotateRight(17) xor before2.rotateRight(19) xor (before2 ushr 10)
      words[index] = words[index - 16] + sigma0 + words[index - 7] + sigma1
    }
    var a = hash[0]
    var b = hash[1]
    var c = hash[2]
    var d = hash[3]
    var e = hash[4]
    var f = hash[5]
    var g = hash[6]
    var h = hash[7]
    for (index in 0 until 64) {
      val sum1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
      val choice = (e and f) xor (e.inv() and g)
      val temporary1 = h + sum1 + choice + ROUND_CONSTANTS[index] + words[index]
      val sum0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
      val majority = (a and b) xor (a and c) xor (b and c)
      val temporary2 = sum0 + majority
      h = g
      g = f
      f = e
      e = d + temporary1
      d = c
      c = b
      b = a
      a = temporary1 + temporary2
    }
    hash[0] += a
    hash[1] += b
    hash[2] += c
    hash[3] += d
    hash[4] += e
    hash[5] += f
    hash[6] += g
    hash[7] += h
  }
  return hash.joinToString("") { it.toUInt().toString(16).padStart(8, '0') }
}
