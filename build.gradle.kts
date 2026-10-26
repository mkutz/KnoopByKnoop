plugins {
  base
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.spotless)
}

spotless {
  // The default derives line endings from .gitattributes, which intermittently fails to
  // fingerprint.
  lineEndings = com.diffplug.spotless.LineEnding.UNIX
  kotlin {
    target("**/*.kt")
    targetExclude("**/build/**")
    ktfmt(libs.versions.ktfmt.get()).googleStyle()
  }
  kotlinGradle {
    target("**/*.gradle.kts")
    targetExclude("**/build/**")
    ktfmt(libs.versions.ktfmt.get()).googleStyle()
  }
  toml {
    target("gradle/libs.versions.toml")
    versionCatalog()
  }
}
