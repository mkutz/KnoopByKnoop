package com.knoopbyknoop.website

import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.Link
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.AttributeProvider
import org.commonmark.renderer.html.HtmlRenderer

fun escape(text: String) =
  text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

private val extensions = listOf(TablesExtension.create())
private val parser = Parser.builder().extensions(extensions).build()

/**
 * [markdown] as HTML for the page at [from] in [language]. Links to the repository's legal texts
 * (`PRIVACY.md`, `TERMS.md`) become links to the site's pages.
 */
fun markdownToHtml(markdown: String, language: Language, from: String): String {
  val pageLinks = mapOf("PRIVACY.md" to Page.PRIVACY, "TERMS.md" to Page.TERMS)
  val renderer =
    HtmlRenderer.builder()
      .extensions(extensions)
      .attributeProviderFactory {
        AttributeProvider { node, _, attributes ->
          if (node is Link) {
            pageLinks[node.destination]?.let { page ->
              attributes["href"] = relativeLink(from, page.path(language))
            }
          }
        }
      }
      .build()
  return renderer.render(parser.parse(markdown))
}

/** One page of the site: its frame (header, language switch, footer) around [main]. */
fun pageHtml(
  language: Language,
  page: Page,
  strings: Map<String, String>,
  siteName: String,
  title: String,
  description: String,
  main: String,
): String {
  val from = page.path(language)
  fun link(to: String) = relativeLink(from, to)
  fun text(key: String) = escape(strings.getValue(key))
  fun navLink(target: Page) =
    """<a href="${link(target.path(language))}"${if (target == page) " aria-current=\"page\"" else ""}>${text(target.nameKey)}</a>"""
  val alternates =
    Language.entries.joinToString("\n") {
      """  <link rel="alternate" hreflang="${it.code}" href="$SITE_URL${page.path(it)}">"""
    }
  val languageLinks =
    Language.entries.joinToString("\n") {
      val current = if (it == language) " aria-current=\"true\"" else ""
      """        <a href="${link(page.path(it))}" hreflang="${it.code}" lang="${it.code}"$current><abbr title="${it.ownName}">${it.code.uppercase()}</abbr></a>"""
    }
  val fullTitle = if (page == Page.HOME) "$siteName – $title" else "$title – $siteName"
  return """<!doctype html>
<html lang="${language.code}">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${escape(fullTitle)}</title>
  <meta name="description" content="${escape(description)}">
  <link rel="canonical" href="$SITE_URL$from">
$alternates
  <link rel="alternate" hreflang="x-default" href="$SITE_URL${page.path(Language.ENGLISH)}">
  <link rel="icon" href="${link("icon.svg")}" type="image/svg+xml">
  <link rel="stylesheet" href="${link("style.css")}">
</head>
<body>
  <a class="skip" href="#main">${text("skip")}</a>
  <header class="site-header">
    <div class="wrap header-row">
      <a class="brand" href="${link(Page.HOME.path(language))}">
        <img src="${link("icon.svg")}" alt="" width="40" height="40">
        <span>${escape(siteName)}</span>
      </a>
      <nav class="main-nav" aria-label="${text("nav.main")}">
        ${navLink(Page.HOME)}
        ${navLink(Page.HELP)}
      </nav>
      <nav class="languages" aria-label="${text("nav.language")}">
$languageLinks
      </nav>
    </div>
  </header>
  <main id="main">
$main
  </main>
  <footer class="site-footer">
    <div class="wrap footer-row">
      ${navLink(Page.HELP)}
      ${navLink(Page.PRIVACY)}
      ${navLink(Page.TERMS)}
      ${navLink(Page.LEGAL_NOTICE)}
      <a href="https://github.com/mkutz/knoopbyknoop">${text("footer.source")}</a>
      <p>${text("footer.osm")}</p>
    </div>
  </footer>
</body>
</html>
"""
}

/** The note on a translated legal text that the English one is binding, linking to it. */
fun translationNote(language: Language, page: Page, strings: Map<String, String>): String {
  val note = escape(strings.getValue("translation.note"))
  val link = relativeLink(page.path(language), page.path(Language.ENGLISH))
  return """<p class="translation-note" role="note">""" +
    note.replace("[", """<a href="$link" hreflang="en">""").replace("]", "</a>") +
    "</p>"
}

/** The chain of knot plates above the home page's headline; decoration only. */
private val knotChain =
  """<div class="knots" aria-hidden="true"><span>21</span><i></i><span>05</span><i></i><span>43</span><i></i><span class="end">72</span></div>"""

/**
 * The home page's main content from [listing] with the [screenshots] (paths from the site's root)
 * and their descriptions from the page texts.
 */
fun homeMain(
  language: Language,
  listing: StoreListing,
  strings: Map<String, String>,
  screenshots: List<String>,
): String {
  val from = Page.HOME.path(language)
  fun text(key: String) = escape(strings.getValue(key))
  fun screenshotAlt(index: Int) = strings["screenshot.${index + 1}"]?.let(::escape) ?: ""
  val paragraphs = listing.description.filterIsInstance<DescriptionBlock.Paragraph>()
  val sections = listing.description.filterIsInstance<DescriptionBlock.Section>()
  val intro = paragraphs.firstOrNull()
  val closing = paragraphs.drop(1)
  val hero = screenshots.firstOrNull()
  val googlePlay = "$GOOGLE_PLAY_URL&amp;hl=${language.code}"
  return buildString {
    append(
      """
    <section class="hero wrap">
      <div class="hero-text">
        $knotChain
        <h1>${escape(listing.subtitle)}</h1>
        <p class="lead">${escape(listing.promotionalText)}</p>
        <div class="badges">
          <a href="$APP_STORE_URL"><img src="${relativeLink(from, "badges/app-store-${language.code}.svg")}" alt="${text("badge.appStore")}" height="56"></a>
          <a href="$googlePlay"><img src="${relativeLink(from, "badges/google-play-${language.code}.png")}" alt="${text("badge.googlePlay")}" height="83"></a>
        </div>
      </div>
"""
    )
    if (hero != null)
      append(
        """      <img class="phone hero-shot" src="${relativeLink(from, hero)}" alt="${screenshotAlt(0)}" width="300">
"""
      )
    append("    </section>\n")
    append("""    <section class="band"><div class="wrap">""")
    if (intro != null) append("""<p class="intro">${escape(intro.text)}</p>""")
    append("""<div class="features">""")
    for (section in sections) append(
      """<div class="feature"><h2>${escape(section.heading)}</h2><p>${escape(section.text)}</p></div>"""
    )
    append("</div></div></section>\n")
    if (screenshots.isNotEmpty()) {
      append(
        """    <section class="wrap shots"><h2>${text("home.screenshots")}</h2><div class="shot-row">"""
      )
      screenshots.forEachIndexed { index, path ->
        append(
          """<img class="phone" src="${relativeLink(from, path)}" alt="${screenshotAlt(index)}" width="200" loading="lazy">"""
        )
      }
      append("</div></section>\n")
    }
    if (closing.isNotEmpty()) {
      append("""    <section class="wrap closing">""")
      closing.forEach { append("<p>${escape(it.text)}</p>") }
      append("</section>\n")
    }
  }
}

/**
 * A Markdown page's main content: its HTML, after the translation note for translated legal texts.
 */
fun articleMain(html: String, note: String?): String {
  if (note == null) return """    <article class="wrap prose">$html</article>"""
  // The note goes right below the title.
  val titleEnd = html.indexOf("</h1>")
  val withNote =
    if (titleEnd < 0) note + html
    else html.substring(0, titleEnd + 5) + "\n" + note + html.substring(titleEnd + 5)
  return """    <article class="wrap prose">$withNote</article>"""
}

/** The page's title: its first heading, else its name. */
fun titleOf(markdown: String, fallback: String) =
  Regex("^# (.+)$", RegexOption.MULTILINE).find(markdown)?.groupValues?.get(1)?.trim() ?: fallback
