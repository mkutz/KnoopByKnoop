package com.knoopbyknoop.website

import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Properties

/**
 * A language's App Store texts, the source of its home page, see "Home page from the store
 * listing".
 */
data class StoreListing(
  val name: String,
  val subtitle: String,
  val promotionalText: String,
  val description: List<DescriptionBlock>,
)

/** A part of a store description: a paragraph, or a section under a heading in capitals. */
sealed interface DescriptionBlock {
  data class Paragraph(val text: String) : DescriptionBlock

  data class Section(val heading: String, val text: String) : DescriptionBlock
}

/** Splits a store description into its paragraphs and its sections under headings in capitals. */
fun parseDescription(description: String): List<DescriptionBlock> =
  description.trim().split(Regex("\\n\\s*\\n")).map { block ->
    val lines = block.trim().lines()
    val heading = lines.first()
    if (lines.size > 1 && heading == heading.uppercase() && heading.any { it.isLetter() })
      DescriptionBlock.Section(heading, lines.drop(1).joinToString(" "))
    else DescriptionBlock.Paragraph(lines.joinToString(" "))
  }

/** A Markdown page with the values of its front matter, the `key: value` lines between `---`. */
data class MarkdownPage(val markdown: String, val frontMatter: Map<String, String>)

fun parseMarkdownPage(text: String): MarkdownPage {
  val match = Regex("\\A---\\n(.*?)\\n---\\n", RegexOption.DOT_MATCHES_ALL).find(text)
  if (match == null) return MarkdownPage(text, emptyMap())
  val frontMatter =
    match.groupValues[1]
      .lines()
      .filter { ':' in it }
      .associate { line ->
        line.substringBefore(':').trim() to line.substringAfter(':').trim()
      }
  return MarkdownPage(text.substring(match.range.last + 1), frontMatter)
}

private val englishDate = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)

/** The date of the "Last updated:" line of an English legal text, or null without one. */
fun lastUpdated(markdown: String): LocalDate? =
  Regex("^Last updated: (.+)$", RegexOption.MULTILINE).find(markdown)?.let {
    LocalDate.parse(it.groupValues[1].trim(), englishDate)
  }

/** Where the site's sources are in the repository at [root]. */
class Sources(val root: File) {
  val content = root.resolve("website/content")
  val static = root.resolve("website/static")
  val icon = root.resolve("design/icon/icon.svg")

  fun storeListing(language: Language): StoreListing {
    val folder = root.resolve("fastlane/metadata/ios/${language.storeLocale}")
    fun text(name: String) = folder.resolve("$name.txt").readText().trim()
    return StoreListing(
      name = text("name"),
      subtitle = text("subtitle"),
      promotionalText = text("promotional_text"),
      description = parseDescription(text("description")),
    )
  }

  fun screenshots(language: Language): List<File> =
    root
      .resolve("fastlane/screenshots/ios/${language.storeLocale}")
      .listFiles { file -> file.extension == "png" }
      .orEmpty()
      .sortedBy { it.name }

  /**
   * The source file of [page] in [language]: the English legal texts are the repository's own, all
   * else is in `website/content/<language>/`. The home page's source is the store listing.
   */
  fun file(language: Language, page: Page): File =
    when {
      page == Page.HOME ->
        root.resolve("fastlane/metadata/ios/${language.storeLocale}/description.txt")
      page == Page.PLAN -> content.resolve("${language.code}/planner.properties")
      page == Page.PRIVACY && language == Language.ENGLISH -> root.resolve("PRIVACY.md")
      page == Page.TERMS && language == Language.ENGLISH -> root.resolve("TERMS.md")
      else ->
        content.resolve(
          "${language.code}/" +
            when (page) {
              Page.HELP -> "help.md"
              Page.PRIVACY -> "privacy.md"
              Page.TERMS -> "terms.md"
              Page.LEGAL_NOTICE -> "legal-notice.md"
              Page.HOME,
              Page.PLAN -> error("unreachable")
            }
        )
    }

  fun markdown(language: Language, page: Page) = parseMarkdownPage(file(language, page).readText())

  /** The planner's files, built by `./gradlew :web:plannerFiles`, see `web-planner`. */
  val planner = root.resolve("web/build/planner")

  /** The apps' dark base map, which the planner uses too. */
  val darkMapStyle = root.resolve("map-styles/liberty-dark.json")

  /** The planner's texts in [language], see "Planner page in every language". */
  fun plannerTexts(language: Language): Map<String, String> = properties(file(language, Page.PLAN))

  /** The language's texts of the page frame: page names, the language note, the badges' text. */
  fun strings(language: Language): Map<String, String> =
    properties(content.resolve("${language.code}/site.properties"))

  private fun properties(file: File): Map<String, String> {
    val properties = Properties()
    file.reader(Charsets.UTF_8).use { properties.load(it) }
    return properties.stringPropertyNames().associateWith { properties.getProperty(it) }
  }
}
