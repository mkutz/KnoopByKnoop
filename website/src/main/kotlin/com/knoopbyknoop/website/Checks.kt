package com.knoopbyknoop.website

import java.time.LocalDate

/**
 * The problems of the site's sources that keep it from being published, see `project-website`:
 * missing pages, stale translations of the legal texts, and contact details that differ between the
 * legal texts.
 */
fun sourceProblems(sources: Sources): List<String> =
  missingPages(sources).ifEmpty { staleTranslations(sources) + differingContactDetails(sources) }

/** A page missing in a language, named with its source file; the page frame's texts too. */
fun missingPages(sources: Sources): List<String> =
  Language.entries.flatMap { language ->
    val pages =
      Page.entries.mapNotNull { page ->
        val file = sources.file(language, page)
        if (file.isFile) null
        else "${language.englishName} ${page.englishName} missing: ${file.relativeTo(sources.root)}"
      }
    val strings = sources.content.resolve("${language.code}/site.properties")
    pages +
      listOfNotNull(
        if (strings.isFile) null
        else "${language.englishName} page texts missing: ${strings.relativeTo(sources.root)}"
      )
  }

/**
 * A translated privacy policy or terms of use whose `translates:` date is not the English text's
 * "Last updated:" date, see "Legal texts in every language, English binding".
 */
fun staleTranslations(sources: Sources): List<String> =
  listOf(Page.PRIVACY, Page.TERMS).flatMap { page ->
    val english = lastUpdated(sources.markdown(Language.ENGLISH, page).markdown)
    Language.entries
      .filter { it != Language.ENGLISH }
      .mapNotNull { language ->
        val translation = sources.markdown(language, page)
        val translates =
          translation.frontMatter["translates"]?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
          }
        if (translates == english) null
        else
          "${language.englishName} ${page.englishName} translates the English one of " +
            "${translates ?: "no date"}, the English one is of $english: " +
            "${sources.file(language, page).relativeTo(sources.root)}"
      }
  }

/**
 * The lines naming who is responsible, from the English privacy policy's "Who is responsible": the
 * legal notice and the other legal texts must carry them unchanged, see "Legal notice".
 */
fun contactDetails(sources: Sources): List<String> =
  sources
    .markdown(Language.ENGLISH, Page.PRIVACY)
    .markdown
    .substringAfter("## Who is responsible", "")
    .substringBefore("\n## ")
    .lines()
    // A trailing backslash is Markdown's line break.
    .map { it.trim().removeSuffix("\\").trim() }
    .filter { it.isNotEmpty() }

/** A legal text in any language that lacks a line of [contactDetails]. */
fun differingContactDetails(sources: Sources): List<String> {
  val details = contactDetails(sources)
  if (details.isEmpty()) return listOf("PRIVACY.md: no \"## Who is responsible\" section")
  return Language.entries.flatMap { language ->
    listOf(Page.LEGAL_NOTICE, Page.PRIVACY, Page.TERMS).flatMap { page ->
      val markdown = sources.markdown(language, page).markdown
      details
        .filter { it !in markdown }
        .map {
          "${language.englishName} ${page.englishName} lacks \"$it\" of the privacy policy's " +
            "\"Who is responsible\": ${sources.file(language, page).relativeTo(sources.root)}"
        }
    }
  }
}

private val loadingAttributes =
  listOf(
    // src and srcset on any element
    Regex("""\b(?:src|srcset)\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
    // href on <link>, which loads stylesheets, icons and fonts; <a href> only links
    Regex("""<link\b[^>]*\bhref\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
    // CSS
    Regex("""@import\s+(?:url\()?\s*["']?([^"')\s;]+)""", RegexOption.IGNORE_CASE),
    Regex("""url\(\s*["']?([^"')\s]+)""", RegexOption.IGNORE_CASE),
  )

/** Hosts a page may load from: the site's own. */
private val ownHosts = setOf("knoopbyknoop.com", "www.knoopbyknoop.com")

/**
 * Everything a generated page or stylesheet loads from another site, named with the file and the
 * address, see "No tracking and nothing from other sites". `<link rel="alternate">` and
 * `rel="canonical"` name the site's own pages and load nothing.
 */
fun foreignResources(files: Map<String, String>): List<String> = files.flatMap { (path, text) ->
  val withoutPageLinks =
    text.replace(
      Regex(
        """<link\b[^>]*\brel\s*=\s*["'](?:alternate|canonical)["'][^>]*>""",
        RegexOption.IGNORE_CASE,
      ),
      "",
    )
  loadingAttributes
    .flatMap { pattern -> pattern.findAll(withoutPageLinks).map { it.groupValues[1] } }
    .flatMap { value -> value.split(',').map { it.trim().substringBefore(' ') } }
    .filter { address -> isForeign(address) }
    .distinct()
    .map { "$path loads $it from another site" }
}

private fun isForeign(address: String): Boolean {
  val lower = address.lowercase()
  val host =
    when {
        lower.startsWith("//") -> lower.removePrefix("//")
        Regex("^[a-z][a-z0-9+.-]*:").containsMatchIn(lower) ->
          if (lower.startsWith("http://") || lower.startsWith("https://"))
            lower.substringAfter("://")
          else return !lower.startsWith("data:")
        else -> return false
      }
      .substringBefore('/')
      .substringBefore(':')
  return host !in ownHosts
}
