package com.knoopbyknoop.scenariocheck

import java.io.File

/** A scenario of a spec, see `spec-checks`. */
data class Scenario(val capability: String, val requirement: String, val name: String) {
  override fun toString() = "$capability / $requirement / $name"
}

/** A line of the list of scenarios checked otherwise: its scenario and how it is checked. */
data class CheckedOtherwise(
  val capability: String,
  val name: String,
  val how: String,
  val line: Int,
)

/** The scenarios of a main spec of [capability]. */
fun specScenarios(capability: String, text: String): List<Scenario> =
  scenariosIn(capability, text) { true }

/** The scenarios a spec delta of [capability] adds or modifies. */
fun deltaScenarios(capability: String, text: String): List<Scenario> {
  var section = ""
  return scenariosIn(capability, text) { line ->
    if (line.startsWith("## ")) section = line.removePrefix("## ").trim()
    section == "ADDED Requirements" || section == "MODIFIED Requirements"
  }
}

private fun scenariosIn(
  capability: String,
  text: String,
  counts: (String) -> Boolean,
): List<Scenario> {
  var requirement = ""
  val scenarios = mutableListOf<Scenario>()
  for (line in text.lines()) {
    val counted = counts(line)
    when {
      line.startsWith("### Requirement:") ->
        requirement = line.removePrefix("### Requirement:").trim()
      line.startsWith("#### Scenario:") && counted ->
        scenarios += Scenario(capability, requirement, line.removePrefix("#### Scenario:").trim())
    }
  }
  return scenarios
}

/** The entries of the list of scenarios checked otherwise, see `openspec/checked-by-hand.md`. */
fun checkedOtherwise(text: String): List<CheckedOtherwise> {
  var capability = ""
  return text.lines().mapIndexedNotNull { index, line ->
    when {
      line.startsWith("## ") -> {
        capability = line.removePrefix("## ").trim()
        null
      }
      line.startsWith("- ") -> {
        val entry = line.removePrefix("- ")
        val separator = entry.indexOf(" — ")
        if (separator < 0) CheckedOtherwise(capability, entry.trim(), "", index + 1)
        else
          CheckedOtherwise(
            capability,
            entry.substring(0, separator).trim(),
            entry.substring(separator + 3).trim(),
            index + 1,
          )
      }
      else -> null
    }
  }
}

/** The names of the tests and the scenarios they are marked with, in a test source. */
class TestNames(val names: Set<String>, val markers: Set<String>) {

  operator fun plus(other: TestNames) = TestNames(names + other.names, markers + other.markers)

  /**
   * Whether a test counts for a scenario of [name], see `spec-checks` "Tests tied to scenarios by
   * name": a test of that name, one whose name starts with it if it has three words or more, or a
   * test marked with it.
   */
  fun cover(name: String): Boolean {
    val scenario = normalized(name)
    if (scenario in markers || scenario in names) return true
    return scenario.split(' ').size >= 3 && names.any { it.startsWith("$scenario ") }
  }

  companion object {
    val NONE = TestNames(setOf(), setOf())

    private val testName = Regex("""fun\s+`([^`]+)`""")
    private val marker = Regex("""^\s*(?://|#)\s*Scenario:\s*(.+?)\s*$""", RegexOption.MULTILINE)

    /** The tests of a Kotlin or Swift test source, or the markers of a shell test script. */
    fun of(text: String) =
      TestNames(
        testName.findAll(text).map { normalized(it.groupValues[1]) }.toSet(),
        marker.findAll(text).map { normalized(it.groupValues[1]) }.toSet(),
      )
  }
}

/** [text] in lower case with anything but letters and digits as single spaces. */
fun normalized(text: String): String =
  text.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

/**
 * What is wrong with [scenarios] given [tests] and the [list] of scenarios checked otherwise: each
 * scenario without either, see `spec-checks` "Every scenario has a test or a stated check", and,
 * with [allScenarios] as the specs' scenarios, every entry of the list no longer true, see "The
 * list of scenarios checked otherwise stays true"; empty if nothing is.
 */
fun problems(
  scenarios: List<Scenario>,
  tests: TestNames,
  list: List<CheckedOtherwise>,
  allScenarios: List<Scenario>? = null,
): List<String> {
  val listed = list.map { it.capability to normalized(it.name) }.toSet()
  val missing =
    scenarios
      .filter { !tests.cover(it.name) && (it.capability to normalized(it.name)) !in listed }
      .map { "No test and not listed as checked otherwise: $it" }
  val withoutWay =
    list
      .filter { it.how.isEmpty() }
      .map { "Line ${it.line} does not say how it is checked: ${it.name}" }
  val stale =
    if (allScenarios == null) listOf()
    else {
      val existing = allScenarios.map { it.capability to normalized(it.name) }.toSet()
      list.mapNotNull {
        when {
          (it.capability to normalized(it.name)) !in existing ->
            "Line ${it.line} lists a scenario ${it.capability} does not have: ${it.name}"
          tests.cover(it.name) -> "Line ${it.line} lists a scenario that has a test: ${it.name}"
          else -> null
        }
      }
    }
  return missing + withoutWay + stale
}

/** The file listing the scenarios checked otherwise, relative to the repository. */
const val LIST_PATH = "openspec/checked-by-hand.md"

/**
 * The problems of the repository at [root]: of its specs, or with [change] of that change's spec
 * delta.
 */
fun checkRepository(root: File, change: String? = null): List<String> {
  val all =
    specFiles(root.resolve("openspec/specs")).flatMap { (capability, text) ->
      specScenarios(capability, text)
    }
  val scenarios =
    if (change == null) all
    else {
      val delta = root.resolve("openspec/changes/$change/specs")
      require(delta.isDirectory) { "No spec delta at $delta" }
      specFiles(delta).flatMap { (capability, text) -> deltaScenarios(capability, text) }
    }
  val listFile = root.resolve(LIST_PATH)
  val list = if (listFile.exists()) checkedOtherwise(listFile.readText()) else listOf()
  return problems(scenarios, testsIn(root), list, all.takeIf { change == null })
}

private fun specFiles(directory: File): List<Pair<String, String>> =
  directory
    .listFiles()
    .orEmpty()
    .filter { it.resolve("spec.md").exists() }
    .sortedBy { it.name }
    .map { it.name to it.resolve("spec.md").readText() }

private val skippedDirectories = setOf("build", ".gradle", ".git", ".kotlin", "node_modules")

/**
 * The tests of the repository: Kotlin and Swift sources in test source sets (a directory of `src`
 * whose name contains "test"), and the shell test scripts of `.github/scripts`.
 */
fun testsIn(root: File): TestNames {
  val sources =
    root
      .walkTopDown()
      .onEnter { it.name !in skippedDirectories }
      .filter { it.isFile && (it.extension == "kt" || it.extension == "swift") }
      .filter { file ->
        val parts = file.relativeTo(root).invariantSeparatorsPath.split('/')
        parts.zipWithNext().any { (parent, child) ->
          parent == "src" && child.contains("test", ignoreCase = true)
        }
      }
  val scripts =
    root.resolve(".github/scripts").listFiles().orEmpty().filter { it.name.endsWith("-test.sh") }
  return (sources + scripts).fold(TestNames.NONE) { tests, file ->
    tests + TestNames.of(file.readText())
  }
}
