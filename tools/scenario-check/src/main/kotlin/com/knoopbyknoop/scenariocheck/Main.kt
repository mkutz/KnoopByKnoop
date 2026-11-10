package com.knoopbyknoop.scenariocheck

import java.io.File
import kotlin.system.exitProcess

/**
 * Checks that every scenario has a test or is listed as checked otherwise, see `spec-checks`.
 *
 * Usage: `scenario-check <repository> [<change>]`
 */
fun main(args: Array<String>) {
  if (args.isEmpty() || args.size > 2) {
    System.err.println("Usage: scenario-check <repository> [<change>]")
    exitProcess(2)
  }
  val change = args.getOrNull(1)
  val problems = checkRepository(File(args[0]), change)
  val what = if (change == null) "the specs" else "the change $change"
  if (problems.isEmpty()) {
    println("Every scenario of $what has a test or is listed in $LIST_PATH.")
    return
  }
  problems.forEach { System.err.println(it) }
  System.err.println(
    "${problems.size} ${if (problems.size == 1) "problem" else "problems"} in $what. Name a test after its scenario, mark it with " +
      "`// Scenario: <name>`, or list the scenario in $LIST_PATH with how it is checked."
  )
  exitProcess(1)
}
