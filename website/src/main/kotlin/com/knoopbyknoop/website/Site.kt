package com.knoopbyknoop.website

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/** The site's sources have problems, see [sourceProblems] and [foreignResources]. */
class SiteProblems(val problems: List<String>) :
  Exception("The website can't be built:\n" + problems.joinToString("\n") { "- $it" })

/** Width the screenshots are scaled to, so a page stays small. */
const val SCREENSHOT_WIDTH = 600

/**
 * The whole site built from [sources]: each file's path from the site's root and its content.
 * Throws [SiteProblems] instead when a check fails, so nothing broken is published.
 */
fun buildSite(sources: Sources): Map<String, ByteArray> {
  sourceProblems(sources).let { if (it.isNotEmpty()) throw SiteProblems(it) }
  val files = linkedMapOf<String, ByteArray>()
  val screenshotCache = mutableMapOf<File, String>()
  for (language in Language.entries) {
    val strings = sources.strings(language)
    val listing = sources.storeListing(language)
    val screenshots =
      sources.screenshots(language).mapIndexed { index, file ->
        screenshotCache.getOrPut(file) {
          val path = "images/${language.code}-${index + 1}.jpg"
          files[path] = scaledJpeg(file)
          path
        }
      }
    files[Page.HOME.file(language)] =
      pageHtml(
          language,
          Page.HOME,
          strings,
          listing.name,
          listing.subtitle,
          listing.promotionalText,
          homeMain(language, listing, strings, screenshots),
        )
        .toByteArray()
    val plannerTexts = sources.plannerTexts(language)
    files[Page.PLAN.file(language)] =
      pageHtml(
          language,
          Page.PLAN,
          strings,
          listing.name,
          plannerTexts.getValue("title"),
          plannerTexts.getValue("description"),
          plannerMain(language, strings, plannerTexts),
          plannerHead(language),
        )
        .toByteArray()
    for (page in Page.entries - Page.HOME - Page.PLAN) {
      val markdown = sources.markdown(language, page).markdown
      val title = titleOf(markdown, strings.getValue(page.nameKey))
      val isTranslation =
        language != Language.ENGLISH && (page == Page.PRIVACY || page == Page.TERMS)
      val note = if (isTranslation) translationNote(language, page, strings) else null
      val html = markdownToHtml(markdown, language, page.path(language))
      files[page.file(language)] =
        pageHtml(
            language,
            page,
            strings,
            listing.name,
            title,
            listing.promotionalText,
            articleMain(html, note),
          )
          .toByteArray()
    }
  }
  files["icon.svg"] = sources.icon.readBytes()
  files["route/index.html"] =
    routePage(sources.strings(Language.ENGLISH), sources.plannerTexts(Language.ENGLISH))
      .toByteArray()
  PLANNER_FILES.forEach { files[it] = sources.planner.resolve(it).readBytes() }
  files["map-styles/liberty-dark.json"] = sources.darkMapStyle.readBytes()
  sources.static
    .walkTopDown()
    .filter { it.isFile }
    .forEach { files[it.relativeTo(sources.static).invariantSeparatorsPath] = it.readBytes() }
  val texts =
    files
      .filterKeys { it.endsWith(".html") || it.endsWith(".css") || it.endsWith(".svg") }
      .mapValues { String(it.value) }
  foreignResources(texts).let { if (it.isNotEmpty()) throw SiteProblems(it) }
  return files
}

/** [file], a PNG screenshot, scaled to [SCREENSHOT_WIDTH] and written as a JPEG. */
fun scaledJpeg(file: File): ByteArray {
  val original = ImageIO.read(file)
  val height = original.height * SCREENSHOT_WIDTH / original.width
  val scaled = BufferedImage(SCREENSHOT_WIDTH, height, BufferedImage.TYPE_INT_RGB)
  val graphics = scaled.createGraphics()
  graphics.setRenderingHint(
    RenderingHints.KEY_INTERPOLATION,
    RenderingHints.VALUE_INTERPOLATION_BICUBIC,
  )
  graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
  graphics.drawImage(original, 0, 0, SCREENSHOT_WIDTH, height, null)
  graphics.dispose()
  val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
  val parameters =
    writer.defaultWriteParam.apply {
      compressionMode = ImageWriteParam.MODE_EXPLICIT
      compressionQuality = 0.85f
    }
  val bytes = ByteArrayOutputStream()
  ImageIO.createImageOutputStream(bytes).use { output ->
    writer.output = output
    writer.write(null, IIOImage(scaled, null, null), parameters)
  }
  writer.dispose()
  return bytes.toByteArray()
}

/** Writes [files] into [directory], replacing what was there. */
fun writeSite(files: Map<String, ByteArray>, directory: File) {
  directory.deleteRecursively()
  for ((path, content) in files) {
    val file = directory.resolve(path)
    file.parentFile.mkdirs()
    file.writeBytes(content)
  }
}
