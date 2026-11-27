@file:JsModule("pako")
@file:JsNonModule

package com.knoopbyknoop.core

import org.khronos.webgl.Uint8Array

/** pako's zlib inflate, which throws on data it can't unpack. */
internal external fun inflate(data: Uint8Array): Uint8Array

/** pako's zlib deflate. */
internal external fun deflate(data: Uint8Array, options: dynamic): Uint8Array
