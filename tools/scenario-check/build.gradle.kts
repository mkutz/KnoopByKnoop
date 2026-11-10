plugins {
  application
  alias(libs.plugins.kotlin.jvm)
}

kotlin { jvmToolchain(21) }

application { mainClass = "com.knoopbyknoop.scenariocheck.MainKt" }

dependencies { testImplementation(libs.kotlin.test) }

tasks.test { useJUnitPlatform() }

// Every scenario has a test or is listed as checked otherwise, see `spec-checks`; with
// -Pchange=<name> the scenarios of that change's spec delta.
val checkScenarios by
  tasks.registering(JavaExec::class) {
    description = "Checks that every spec scenario has a test or is listed as checked otherwise"
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args(listOfNotNull(rootDir.absolutePath, providers.gradleProperty("change").orNull))
  }

tasks.check { dependsOn(checkScenarios) }
