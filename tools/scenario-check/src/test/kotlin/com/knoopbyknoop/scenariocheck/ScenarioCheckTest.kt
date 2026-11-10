package com.knoopbyknoop.scenariocheck

import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers `spec-checks`. The specs, tests and lists are written by the tests, with scenario names no
 * real spec has, as the check reads this file too.
 */
class ScenarioCheckTest {

  private val root = createTempDirectory("scenario-check").toFile()

  @AfterTest fun deleteRepository() = root.deleteRecursively().let {}

  private fun write(path: String, text: String) =
    root.resolve(path).apply { parentFile.mkdirs() }.writeText(text.trimIndent())

  private fun spec(capability: String, vararg requirements: Pair<String, List<String>>) =
    write(
      "openspec/specs/$capability/spec.md",
      requirements.joinToString("\n\n") { (requirement, scenarios) ->
        "### Requirement: $requirement\nText.\n\n" +
          scenarios.joinToString("\n\n") { "#### Scenario: $it\n- **WHEN** a\n- **THEN** b" }
      },
    )

  private fun test(vararg lines: String) =
    write("modules/core/src/commonTest/kotlin/MoonTest.kt", lines.joinToString("\n"))

  private fun list(text: String) = write(LIST_PATH, text)

  private fun moonSpec() =
    spec(
      "moon-rides",
      "Riding on the moon" to listOf("Craters along the way", "Dust", "Low gravity at the knot"),
    )

  // Requirement: Tests tied to scenarios by name

  @Test
  fun `Test named after the scenario`() {
    assertTrue(
      TestNames.of("fun `Craters along the way are ridden around`() {}")
        .cover("Craters along the way")
    )
    assertTrue(TestNames.of("fun `craters, along the way`() {}").cover("Craters along the way"))
    assertFalse(TestNames.of("fun `Craters along the wayside`() {}").cover("Craters along the way"))
  }

  @Test
  fun `Test marked with the scenario`() {
    val tests = TestNames.of("  // Scenario: Craters along the way\n  fun `ride around`() {}")
    assertTrue(tests.cover("Craters along the way"))
    assertTrue(TestNames.of("# Scenario: Craters along the way\n").cover("Craters along the way"))
  }

  @Test
  fun `Short scenario name`() {
    assertFalse(TestNames.of("fun `dust on the tyres after the ride`() {}").cover("Dust"))
    assertTrue(TestNames.of("fun `dust`() {}").cover("Dust"))
    assertTrue(TestNames.of("// Scenario: Dust\nfun `tyres`() {}").cover("Dust"))
  }

  // Requirement: Every scenario has a test or a stated check

  @Test
  fun `Scenario without a test`() {
    moonSpec()
    test("fun `Craters along the way are ridden around`() {}", "fun `dust`() {}")

    assertEquals(
      listOf(
        "No test and not listed as checked otherwise: " +
          "moon-rides / Riding on the moon / Low gravity at the knot"
      ),
      checkRepository(root),
    )
  }

  @Test
  fun `Scenario checked on the devices`() {
    moonSpec()
    test("fun `Craters along the way are ridden around`() {}", "fun `dust`() {}")
    list(
      """
      # Scenarios checked otherwise

      ## moon-rides

      - Low gravity at the knot — on the devices
      """
    )

    assertEquals(listOf(), checkRepository(root))
  }

  @Test
  fun `Entry without a way of checking`() {
    moonSpec()
    test("fun `Craters along the way are ridden around`() {}", "fun `dust`() {}")
    list(
      """
      ## moon-rides

      - Low gravity at the knot
      """
    )

    assertEquals(
      listOf("Line 3 does not say how it is checked: Low gravity at the knot"),
      checkRepository(root),
    )
  }

  // Requirement: The list of scenarios checked otherwise stays true

  @Test
  fun `Scenario renamed`() {
    spec("moon-rides", "Riding on the moon" to listOf("Craters along the way", "Dust"))
    test("fun `Craters along the way are ridden around`() {}", "fun `dust`() {}")
    list(
      """
      ## moon-rides

      - Low gravity at the knot — on the devices
      """
    )

    assertEquals(
      listOf("Line 3 lists a scenario moon-rides does not have: Low gravity at the knot"),
      checkRepository(root),
    )
  }

  @Test
  fun `Scenario tested later`() {
    moonSpec()
    test(
      "fun `Craters along the way are ridden around`() {}",
      "fun `dust`() {}",
      "fun `Low gravity at the knot keeps the rider up`() {}",
    )
    list(
      """
      ## moon-rides

      - Low gravity at the knot — on the devices
      """
    )

    assertEquals(
      listOf("Line 3 lists a scenario that has a test: Low gravity at the knot"),
      checkRepository(root),
    )
  }

  // Requirement: Scenarios of a change checked before archiving

  @Test
  fun `Change with a scenario still untested`() {
    moonSpec()
    test("fun `Craters along the way are ridden around`() {}", "fun `dust`() {}")
    write(
      "openspec/changes/add-moon-dust/specs/moon-rides/spec.md",
      """
      ## ADDED Requirements

      ### Requirement: Dust storms
      Text.

      #### Scenario: Storm ahead of the rider
      - **WHEN** a
      - **THEN** b

      #### Scenario: Storm behind the rider
      - **WHEN** a
      - **THEN** b

      ## REMOVED Requirements

      ### Requirement: Riding on the moon
      #### Scenario: Gone with the storm
      """,
    )
    write(
      "modules/core/src/jvmTest/kotlin/StormTest.kt",
      "fun `Storm ahead of the rider is told`() {}",
    )

    assertEquals(
      listOf(
        "No test and not listed as checked otherwise: " +
          "moon-rides / Dust storms / Storm behind the rider"
      ),
      checkRepository(root, "add-moon-dust"),
    )
  }

  @Test
  fun `tests are read from test source sets and test scripts only`() {
    spec("moon-rides", "Riding on the moon" to listOf("Craters along the way"))
    write("modules/core/src/commonMain/kotlin/Moon.kt", "fun `Craters along the way`() {}")
    write("modules/core/build/src/test/Moon.kt", "fun `Craters along the way`() {}")
    assertEquals(1, checkRepository(root).size)

    write(".github/scripts/moon-test.sh", "# Scenario: Craters along the way\n")
    assertEquals(listOf(), checkRepository(root))
  }
}
