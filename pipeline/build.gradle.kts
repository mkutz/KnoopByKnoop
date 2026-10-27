plugins {
  application
  alias(libs.plugins.kotlin.jvm)
}

kotlin { jvmToolchain(21) }

application { mainClass = "com.knoopbyknoop.pipeline.MainKt" }

dependencies {
  implementation(project(":modules:core"))
  implementation(libs.osmpbf)
  testImplementation(libs.kotlin.test)
}

tasks.test { useJUnitPlatform() }

// The published graphs, verified and ready to bundle into the apps, see `network-sync` "Bundled
// graphs". Debug builds bundle nothing when the data is unreachable; release builds fail instead.
val networkDataBaseUrl =
  providers
    .gradleProperty("networkDataBaseUrl")
    // NetworkIndex.PUBLISHED_BASE_URL
    .orElse("https://github.com/mkutz/knoopbyknoop/releases/download/network-data/")

fun registerBundleTask(name: String, buildType: String, allowUnavailable: Boolean) =
  tasks.register<JavaExec>(name) {
    description = "Fetches and verifies the published graphs to bundle into $buildType app builds"
    val outputDirectory = layout.buildDirectory.dir("bundled-networks/$buildType")
    outputs.dir(outputDirectory)
    // Bundled data is refreshed by a clean build; an empty bundle is retried on every build.
    outputs.upToDateWhen {
      val bundledIndex = outputDirectory.get().file("bundled-index.json").asFile
      bundledIndex.exists() && bundledIndex.readText().contains("\"file\"")
    }
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    args(
      listOfNotNull(
        "bundle",
        "--allow-unavailable".takeIf { allowUnavailable },
        networkDataBaseUrl.get(),
        outputDirectory.get().asFile.absolutePath,
      )
    )
  }

registerBundleTask("bundleNetworkGraphs", "debug", allowUnavailable = true)

registerBundleTask("bundleNetworkGraphsForRelease", "release", allowUnavailable = false)
