package com.knoopbyknoop.core

private const val HEX = "0123456789ABCDEF"

/**
 * [value] percent-encoded as UTF-8, keeping only letters, digits and `-._~`, so it can go into any
 * part of a web address.
 */
fun percentEncoded(value: String): String = buildString {
  for (byte in value.encodeToByteArray()) {
    val char = byte.toInt().toChar()
    if (char.isLetterOrDigit() && byte >= 0 || char in "-._~") {
      append(char)
    } else {
      append('%')
      append(HEX[(byte.toInt() shr 4) and 0xF])
      append(HEX[byte.toInt() and 0xF])
    }
  }
}

/** [value] with its `%XX` escapes decoded as UTF-8, or `null` where an escape is broken. */
fun percentDecoded(value: String): String? {
  val bytes = ArrayList<Byte>(value.length)
  var index = 0
  while (index < value.length) {
    val char = value[index]
    if (char == '%') {
      if (index + 2 >= value.length) return null
      val high = HEX.indexOf(value[index + 1].uppercaseChar())
      val low = HEX.indexOf(value[index + 2].uppercaseChar())
      if (high < 0 || low < 0) return null
      bytes.add(((high shl 4) or low).toByte())
      index += 3
    } else {
      char.toString().encodeToByteArray().forEach { bytes.add(it) }
      index++
    }
  }
  return bytes.toByteArray().decodeToString()
}
