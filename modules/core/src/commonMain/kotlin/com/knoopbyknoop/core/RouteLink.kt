package com.knoopbyknoop.core

/**
 * A route as a link, see `route-sharing` "Route link": the website's route page with the text of
 * the route file after `#`, which browsers keep to themselves.
 */
object RouteLink {

  /** The route page, which the apps open as links and the website sends on to the planner. */
  const val PAGE = "https://knoopbyknoop.com/route/"

  /** The link carrying the route file [text], see [RouteFile.write]. */
  fun of(text: String): String = PAGE + "#" + percentEncoded(text)

  /**
   * The route file text carried by [link], or `null` when [link] is no route link or its text can't
   * be decoded. `http`, `www.`, a missing slash before `#` and parameters before `#` (such as
   * `?fbclid=…`) are accepted, as people and messengers change links.
   */
  fun textOf(link: String): String? {
    val match = LINK.matchEntire(link.trim()) ?: return null
    return percentDecoded(match.groupValues[1])?.takeIf { it.isNotBlank() }
  }

  private val LINK = Regex("""https?://(?:www\.)?knoopbyknoop\.com/route/?(?:\?[^#]*)?#(.*)""")
}
