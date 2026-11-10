package com.knoopbyknoop.pipeline

import java.io.IOException
import java.net.URI

/**
 * The bytes at [uri], from the published network data or a local copy of it.
 *
 * @throws IOException if they cannot be fetched
 */
fun fetch(uri: URI): ByteArray = uri.toURL().openStream().use { it.readBytes() }

/** [base] as a directory, so files resolve inside it. */
fun directoryUri(base: String): URI = URI(base.removeSuffix("/") + "/")
