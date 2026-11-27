package com.knoopbyknoop.website

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SiteTest {

  private val repository = File(System.getProperty("repositoryRoot"))

  /** The site built from the real repository content, once for all tests. */
  private val site: Map<String, String> by lazy {
    buildSite(Sources(repository))
      .filterKeys { it.endsWith(".html") }
      .mapValues { String(it.value) }
  }

  /**
   * A copy of the site's sources in a temporary folder, without the screenshots, to change for a
   * test.
   */
  private fun copyOfSources(): File {
    val copy = Files.createTempDirectory("website-sources").toFile()
    listOf("fastlane/metadata/ios", "website/content", "website/static", "design/icon").forEach {
      repository.resolve(it).copyRecursively(copy.resolve(it))
    }
    listOf("PRIVACY.md", "TERMS.md").forEach { repository.resolve(it).copyTo(copy.resolve(it)) }
    return copy
  }

  @Test
  fun `the repository's site has no problems and every page in every language`() {
    assertEquals(emptyList(), sourceProblems(Sources(repository)))
    for (language in Language.entries) {
      for (page in Page.entries) {
        assertTrue(page.file(language) in site, "${page.file(language)} built")
      }
    }
  }

  // Scenario: Language switch
  @Test
  fun `Language switch - the German help page links to the help page in the other languages`() {
    val germanHelp = site.getValue("de/help/index.html")

    assertContains(germanHelp, """<a href="../../nl/help/" hreflang="nl" lang="nl">""")
    assertContains(germanHelp, """<a href="../../help/" hreflang="en" lang="en">""")
    assertContains(germanHelp, """<a href="../../fr/help/" hreflang="fr" lang="fr">""")
    assertContains(
      germanHelp,
      """<a href="../../de/help/" hreflang="de" lang="de" aria-current="true">""",
    )
  }

  @Test
  fun `Missing page in a language - fails naming the French terms of use`() {
    val sources = copyOfSources()
    sources.resolve("website/content/fr/terms.md").delete()

    val problems = sourceProblems(Sources(sources))

    assertEquals(listOf("French terms of use missing: website/content/fr/terms.md"), problems)
    assertFailsWith<SiteProblems> { buildSite(Sources(sources)) }
  }

  @Test
  fun `Store description changes - the Dutch home page shows the changed description`() {
    val sources = copyOfSources()
    val description = sources.resolve("fastlane/metadata/ios/nl-NL/description.txt")
    description.writeText(
      description.readText().replaceFirst("Knoop by Knoop plant", "Knoop by Knoop plant nu ook")
    )

    val dutchHome = String(buildSite(Sources(sources)).getValue("nl/index.html"))

    assertContains(dutchHome, "Knoop by Knoop plant nu ook")
  }

  @Test
  fun `Links to the stores - the badges open the app in the App Store and Google Play`() {
    val englishHome = site.getValue("index.html")

    assertContains(englishHome, """<a href="$APP_STORE_URL">""")
    assertContains(englishHome, """<a href="$GOOGLE_PLAY_URL&amp;hl=en">""")
    assertContains(site.getValue("de/index.html"), """<a href="$GOOGLE_PLAY_URL&amp;hl=de">""")
  }

  @Test
  fun `No knots on the map - every help page names the areas with networks`() {
    val areas =
      mapOf(
        Language.ENGLISH to listOf("Netherlands", "Belgium", "North Rhine-Westphalia"),
        Language.GERMAN to listOf("Niederlanden", "Belgien", "Nordrhein-Westfalen"),
        Language.DUTCH to listOf("Nederland", "België", "Noordrijn-Westfalen"),
        Language.FRENCH to listOf("Pays-Bas", "Belgique", "Rhénanie-du-Nord-Westphalie"),
      )
    for ((language, names) in areas) {
      val help = site.getValue(Page.HELP.file(language))
      names.forEach { assertContains(help, it, message = "${language.code} help names $it") }
    }
  }

  @Test
  fun `English text updated, translation not - fails naming the German privacy policy`() {
    val sources = copyOfSources()
    val privacy = sources.resolve("PRIVACY.md")
    privacy.writeText(
      privacy.readText().replace(Regex("Last updated: .+"), "Last updated: 1 January 2027")
    )

    val problems = sourceProblems(Sources(sources))

    assertTrue(
      problems.any {
        it.startsWith("German privacy policy translates") &&
          it.endsWith("website/content/de/privacy.md")
      },
      problems.joinToString("\n"),
    )
    assertEquals(3, problems.size, problems.joinToString("\n"))
  }

  @Test
  fun `Binding version named - the French terms of use link to the binding English ones`() {
    val frenchTerms = site.getValue("fr/terms/index.html")

    assertContains(
      frenchTerms,
      """<p class="translation-note" role="note">Ceci est une traduction. Seule la <a href="../../terms/" hreflang="en">version anglaise</a> fait foi.</p>""",
    )
    assertTrue(
      frenchTerms.indexOf("translation-note") < frenchTerms.indexOf("<h2>"),
      "the note is right below the title",
    )
  }

  @Test
  fun `Who runs the site - every legal notice names the person, postal and mail address`() {
    val details = contactDetails(Sources(repository))
    assertEquals(3, details.size, details.toString())
    for (language in Language.entries) {
      val notice = site.getValue(Page.LEGAL_NOTICE.file(language))
      details.forEach { assertContains(notice, escape(it)) }
    }
  }

  @Test
  fun `Who runs the site - fails when a legal notice differs from the privacy policy`() {
    val sources = copyOfSources()
    val notice = sources.resolve("website/content/nl/legal-notice.md")
    notice.writeText(notice.readText().replace("53721 Siegburg", "53721 Bonn"))

    val problems = sourceProblems(Sources(sources))

    assertEquals(1, problems.size, problems.joinToString("\n"))
    assertTrue(problems.single().startsWith("Dutch legal notice lacks"), problems.single())
  }

  @Test
  fun `Font from another site - fails naming the page and the address`() {
    val problems =
      foreignResources(
        mapOf(
          "de/index.html" to
            """<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Inter">""",
          "style.css" to """body { background: url("//cdn.example.com/a.png"); }""",
        )
      )

    assertEquals(
      listOf(
        "de/index.html loads https://fonts.googleapis.com/css2?family=Inter from another site",
        "style.css loads //cdn.example.com/a.png from another site",
      ),
      problems,
    )
  }

  @Test
  fun `links to other sites and the site's own addresses load nothing foreign`() {
    val problems =
      foreignResources(
        mapOf(
          "index.html" to
            """<link rel="alternate" hreflang="de" href="https://knoopbyknoop.com/de/">""" +
              """<a href="https://github.com/mkutz/knoopbyknoop">GitHub</a>""" +
              """<img src="images/en-1.jpg" alt=""><img src="https://knoopbyknoop.com/icon.svg" alt="">"""
        )
      )

    assertEquals(emptyList(), problems)
  }

  @Test
  fun `the built site loads nothing from other sites`() {
    assertEquals(emptyList(), foreignResources(site))
  }
}
