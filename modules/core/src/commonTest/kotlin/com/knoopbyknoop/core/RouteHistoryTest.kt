package com.knoopbyknoop.core

import com.knoopbyknoop.core.TestNetwork.graph
import com.knoopbyknoop.core.TestNetwork.knot12
import com.knoopbyknoop.core.TestNetwork.knot30
import com.knoopbyknoop.core.TestNetwork.knot45
import com.knoopbyknoop.core.TestNetwork.knot7
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Covers `map-planning` "Undo route edits". */
class RouteHistoryTest {

  private val empty = PlanningSession.start(graph)

  /** The planner as the apps keep it: the shown route and the edits before it. */
  private data class Planner(val session: PlanningSession, val history: RouteHistory) {

    fun edit(change: (PlanningSession) -> PlanningSession): Planner {
      val after = change(session)
      return Planner(after, history.record(session, after))
    }

    fun undo(on: NetworkGraph = graph): Planner {
      val (restored, earlier) = assertNotNull(history.undo(on))
      return Planner(restored.session, earlier)
    }
  }

  private fun plan(vararg knots: Knot) =
    knots.fold(Planner(empty, RouteHistory())) { planner, knot -> planner.edit { it.tap(knot) } }

  @Test
  fun `Undo a tap`() {
    val tapped = plan(knot12, knot45).edit { it.tap(knot30) }
    assertEquals(listOf(knot12, knot30, knot45), tapped.session.waypoints)

    assertEquals(listOf(knot12, knot45), tapped.undo().session.waypoints)
  }

  @Test
  fun `Undo the tap after a choice in the note`() {
    // The note's choices replace the session without being kept, so they belong to the tap.
    val tapped = plan(knot12, knot45).edit { it.tap(knot30) }
    val chosen = tapped.copy(session = tapped.session.startAt(knot30))

    assertEquals(listOf(knot12, knot45), chosen.undo().session.waypoints)
  }

  @Test
  fun `Several steps back`() {
    val edited = plan(knot12, knot45, knot30).edit { it.removeWaypoint(0) }
    assertEquals(listOf(knot30, knot45), edited.session.waypoints)

    val once = edited.undo()
    val twice = once.undo()
    val thrice = twice.undo()

    assertEquals(listOf(knot12, knot30, knot45), once.session.waypoints)
    assertEquals(listOf(knot12, knot45), twice.session.waypoints)
    assertEquals(listOf(knot12), thrice.session.waypoints)
  }

  @Test
  fun `Undo a removal`() {
    val removed = plan(knot12, knot45, knot30).edit { it.removeWaypoint(1) }
    assertEquals(listOf(knot12, knot45), removed.session.waypoints)

    assertEquals(listOf(knot12, knot30, knot45), removed.undo().session.waypoints)
  }

  @Test
  fun `Undo a move`() {
    val planned =
      Planner(PlanningSession.start(graph, listOf(knot12, knot30, knot7, knot45)), RouteHistory())
    val moved = planned.edit { it.moveWaypoint(2, 1) }
    assertEquals(listOf(knot12, knot7, knot30, knot45), moved.session.waypoints)

    assertEquals(listOf(knot12, knot30, knot7, knot45), moved.undo().session.waypoints)
  }

  @Test
  fun `Undo Back to start`() {
    val roundTrip = plan(knot12, knot45).edit { it.returnToStart() }
    assertEquals(listOf(knot12, knot45, knot12), roundTrip.session.waypoints)

    val undone = roundTrip.undo()
    assertEquals(listOf(knot12, knot45), undone.session.waypoints)
    assertTrue(undone.session.canReturnToStart)
  }

  @Test
  fun `Undo Clear`() {
    val cleared = plan(knot12, knot30, knot45).edit { it.clear() }
    assertEquals(listOf(), cleared.session.waypoints)
    assertTrue(cleared.history.canUndo)

    assertEquals(listOf(knot12, knot30, knot45), cleared.undo().session.waypoints)
  }

  @Test
  fun `Nothing to undo`() {
    assertFalse(RouteHistory().canUndo)
    assertNull(RouteHistory().undo(graph))

    val undone = plan(knot12, knot45).undo().undo()
    assertFalse(undone.history.canUndo)
    assertNull(undone.history.undo(graph))
  }

  @Test
  fun `Nothing to undo after an edit that changed nothing`() {
    val planned = plan(knot12, knot45)
    val again = planned.edit { it.tap(knot45) }

    assertEquals(planned.history, again.history)
  }

  @Test
  fun `Undo after a restart`() {
    val tapped = plan(knot12, knot45).edit { it.tap(knot30) }

    val stored = RouteHistory.parse(tapped.history.toJson())
    val restarted = Planner(tapped.session, stored)

    assertEquals(listOf(knot12, knot45), restarted.undo().session.waypoints)
  }

  @Test
  fun `Undo after a restart with an unreadable history`() {
    assertEquals(RouteHistory(), RouteHistory.parse("{not json"))
  }

  @Test
  fun `Knot gone when undoing`() {
    val removed = plan(knot12, knot45, knot30).edit { it.removeWaypoint(1) }
    val updatedGraph =
      NetworkGraph(
        graph.knots.values.filter { it != knot30 },
        graph.connections.filter { knot30.id !in listOf(it.fromId, it.toId) },
      )

    val (restored, _) = assertNotNull(removed.history.undo(updatedGraph))

    assertEquals(listOf(knot12, knot45), restored.session.waypoints)
    assertEquals(listOf("30"), restored.removedWaypoints)
  }

  @Test
  fun `the last 50 edits are kept`() {
    val edited =
      (1..60).fold(plan(knot12, knot45)) { planner, index ->
        planner.edit { if (index % 2 == 1) it.tap(knot30) else it.removeWaypoint(1) }
      }

    assertEquals(RouteHistory.LIMIT, edited.history.before.size)
    val all = (1..RouteHistory.LIMIT).fold(edited) { planner, _ -> planner.undo() }
    assertFalse(all.history.canUndo)
    assertEquals(listOf(knot12, knot45), all.session.waypoints)
  }
}
