package com.knoopbyknoop.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The route as it was before each of the user's last edits, oldest first, to take them back one by
 * one, see `map-planning` "Undo route edits". The apps store it beside the route, so it is kept
 * across a restart.
 */
@Serializable
data class RouteHistory(val before: List<RouteSnapshot> = listOf()) {

  /** Whether there is an edit to take back. */
  val canUndo: Boolean
    get() = before.isNotEmpty()

  /**
   * Keeps [before], the route before an edit that made it [after]; an edit that changed no waypoint
   * is not kept. Only the last [LIMIT] edits are kept.
   */
  fun record(before: PlanningSession, after: PlanningSession): RouteHistory =
    if (after.waypoints == before.waypoints) this
    else RouteHistory((this.before + RouteSnapshot.of(before)).takeLast(LIMIT))

  /**
   * Takes back the last edit: the route before it, restored on [graph], the current graph of its
   * network, and the history without it; `null` if there is nothing to take back.
   */
  fun undo(graph: NetworkGraph): Pair<RestoredRoute, RouteHistory>? {
    val last = before.lastOrNull() ?: return null
    return last.restore(graph) to RouteHistory(before.dropLast(1))
  }

  fun toJson(): String = json.encodeToString(this)

  companion object {

    /** How many edits are kept at least, see "Undo route edits". */
    const val LIMIT = 50

    private val json = Json { ignoreUnknownKeys = true }

    /** The stored history, or none if [text] cannot be read. */
    fun parse(text: String): RouteHistory = runCatching {
      json.decodeFromString<RouteHistory>(text)
    }
      .getOrDefault(RouteHistory())
  }
}
