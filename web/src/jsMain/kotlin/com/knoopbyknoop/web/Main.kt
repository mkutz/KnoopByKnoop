package com.knoopbyknoop.web

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

/**
 * Starts the planner on the planner page, or sends a route link on to the planner in the browser's
 * language on the route page, see `web-planner` "Open a route in the planner".
 */
fun main() {
  document.getElementById("route-forward")?.let { forward ->
    val root = forward.getAttribute("data-root") ?: "/"
    val languages =
      (window.navigator.asDynamic().languages as Array<String>?)?.toList()
        ?: listOf(window.navigator.language)
    window.location.replace(root + plannerPathFor(languages) + window.location.hash)
    return
  }
  (document.getElementById("planner") as HTMLElement?)?.let { PlannerPage(it) }
}
