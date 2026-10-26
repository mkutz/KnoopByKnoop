plugins {
  application
  alias(libs.plugins.kotlin.jvm)
}

kotlin { jvmToolchain(21) }

application { mainClass = "com.knoopbyknoop.pipeline.MainKt" }

dependencies {
  implementation(project(":modules:core"))
  testImplementation(libs.kotlin.test)
}

tasks.test { useJUnitPlatform() }
