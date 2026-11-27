package com.knoopbyknoop.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RouteLinkTest {

  private val file =
    """
    # Siegburg 02 – Bonn 73

    - 02 6364701 Siegburg
      - 28
    - 83 Bonn
    - 73

    """
      .trimIndent()

  @Test
  fun `Link of a route - the route page with the file text after the hash`() {
    val link = RouteLink.of(file)

    assertTrue(link.startsWith("https://knoopbyknoop.com/route/#"), link)
    assertEquals(file, RouteLink.textOf(link))
  }

  @Test
  fun `Route not sent to the server - the route is only after the hash`() {
    val link = RouteLink.of(file)
    val hash = link.indexOf('#')

    assertEquals("https://knoopbyknoop.com/route/", link.substring(0, hash))
    assertEquals(1, link.count { it == '#' }, "no second # in the encoded route")
    assertTrue(link.substring(hash + 1).all { it.isLetterOrDigit() || it in "%-._~" }, link)
  }

  @Test
  fun `links changed by people and messengers are still read`() {
    val encoded = RouteLink.of(file).substringAfter('#')

    assertEquals(file, RouteLink.textOf("http://www.knoopbyknoop.com/route#$encoded"))
    assertEquals(file, RouteLink.textOf("  https://knoopbyknoop.com/route/#$encoded\n"))
  }

  @Test
  fun `other links and empty routes and broken escapes are no route links`() {
    assertNull(RouteLink.textOf("https://knoopbyknoop.com/plan/#abc"))
    assertNull(RouteLink.textOf("https://example.com/route/#abc"))
    assertNull(RouteLink.textOf("https://knoopbyknoop.com/route/#"))
    assertNull(RouteLink.textOf("https://knoopbyknoop.com/route/#%E"))
    assertNull(RouteLink.textOf("https://knoopbyknoop.com/route/#%ZZ"))
  }

  @Test
  fun `umlauts and accents survive the link`() {
    val text = "# Köln – Liège\n\n- 12 Liège\n- 34 Köln\n"

    assertEquals(text, RouteLink.textOf(RouteLink.of(text)))
  }
}
