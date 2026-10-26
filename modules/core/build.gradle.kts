plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  jvmToolchain(21)

  // The JVM target is consumed by the Android app and the data pipeline.
  jvm { testRuns["test"].executionTask.configure { useJUnitPlatform() } }

  // The iOS app links this framework, see apps/ios/project.yml.
  listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
    target.binaries.framework {
      baseName = "KnoopByKnoopCore"
      isStatic = true
    }
  }

  sourceSets {
    commonMain.dependencies { implementation(libs.kotlinx.serialization.json) }
    commonTest.dependencies { implementation(libs.kotlin.test) }
  }
}
