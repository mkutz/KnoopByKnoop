package com.knoopbyknoop.website

/**
 * The website's languages, the apps' languages, see `project-website` "Pages in every app
 * language".
 */
enum class Language(
  val code: String,
  /** The locale folder of the store listing in `fastlane/`. */
  val storeLocale: String,
  /** The locale of Apple's App Store badge. */
  val appleBadgeLocale: String,
  /** The language's own name, for the language switch. */
  val ownName: String,
  /** The language's name in English, for the check's messages. */
  val englishName: String,
) {
  ENGLISH("en", "en-US", "en-us", "English", "English"),
  GERMAN("de", "de-DE", "de-de", "Deutsch", "German"),
  DUTCH("nl", "nl-NL", "nl-nl", "Nederlands", "Dutch"),
  FRENCH("fr", "fr-FR", "fr-fr", "Français", "French");

  /** The path of this language's pages: English at the root, the others in their own folder. */
  val prefix: String
    get() = if (this == ENGLISH) "" else "$code/"
}

/** The pages every language has. */
enum class Page(
  /** The page's folder under its language's prefix; empty for the home page. */
  val folder: String,
  /** The key of the page's name in a language's `site.properties`. */
  val nameKey: String,
  /** The page's name in English, for the check's messages. */
  val englishName: String,
) {
  HOME("", "page.home", "home page"),
  HELP("help/", "page.help", "help"),
  PRIVACY("privacy/", "page.privacy", "privacy policy"),
  TERMS("terms/", "page.terms", "terms of use"),
  LEGAL_NOTICE("legal-notice/", "page.legalNotice", "legal notice");

  /** The page's path from the site's root, e.g. `de/help/`. */
  fun path(language: Language) = language.prefix + folder

  /** The file the page is written to, e.g. `de/help/index.html`. */
  fun file(language: Language) = path(language) + "index.html"
}

/** The address the site is published at. */
const val SITE_URL = "https://knoopbyknoop.com/"

const val APP_STORE_URL = "https://apps.apple.com/app/id6831624583"

const val GOOGLE_PLAY_URL = "https://play.google.com/store/apps/details?id=com.knoopbyknoop"

/** [to] as a link relative to the page at [from], both paths from the site's root. */
fun relativeLink(from: String, to: String): String {
  val depth = from.count { it == '/' }
  return ("../".repeat(depth) + to).ifEmpty { "./" }
}
