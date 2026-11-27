package com.knoopbyknoop.web

/**
 * [text] as an SVG QR code, or `null` when it is too long for one, see `web-planner` "Route link
 * from the planner". The library throws a plain string when the text doesn't fit, which Kotlin's
 * `catch (Throwable)` lets through, so it is called inside JavaScript's own try.
 */
fun qrSvg(text: String): String? {
  val factory: dynamic = js("require('qrcode-generator')")
  val svg: dynamic =
    js(
      "(function () { try { var code = factory(0, 'L'); code.addData(text, 'Byte'); code.make(); return code.createSvgTag({ cellSize: 3, margin: 3, scalable: true }); } catch (e) { return null; } })()"
    )
  return svg?.unsafeCast<String>()
}
