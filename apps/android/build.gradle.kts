plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.knoopbyknoop.android"
  compileSdk = 37

  defaultConfig {
    applicationId = "com.knoopbyknoop"
    minSdk = 26
    targetSdk = 37
    versionCode = 1
    versionName = "0.1.0"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  buildFeatures { compose = true }

  buildTypes {
    release {
      // Until add-release-pipelines brings an upload key: signed with the debug key, so release
      // builds can be installed, e.g. to measure startup times (`faster-startup`).
      signingConfig = signingConfigs.getByName("debug")
    }
  }
}

/** Copies the graphs bundled by `:pipeline` into the app's assets, see `network-sync`. */
abstract class CopyBundledNetworks : DefaultTask() {
  @get:InputFiles abstract val bundledNetworks: ConfigurableFileCollection

  @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

  @TaskAction
  fun copy() {
    val target = outputDirectory.get().dir("bundled-networks").asFile
    target.deleteRecursively()
    target.mkdirs()
    bundledNetworks.asFileTree.forEach { it.copyTo(target.resolve(it.name)) }
  }
}

androidComponents {
  onVariants { variant ->
    val isRelease = variant.buildType == "release"
    val bundleTask =
      if (isRelease) ":pipeline:bundleNetworkGraphsForRelease" else ":pipeline:bundleNetworkGraphs"
    val bundled =
      project(":pipeline")
        .layout
        .buildDirectory
        .dir("bundled-networks/${if (isRelease) "release" else "debug"}")
    val copyTask =
      tasks.register<CopyBundledNetworks>(
        "copy${variant.name.replaceFirstChar { it.uppercase() }}BundledNetworks"
      ) {
        bundledNetworks.from(bundled)
        dependsOn(bundleTask)
      }
    variant.sources.assets?.addGeneratedSourceDirectory(
      copyTask,
      CopyBundledNetworks::outputDirectory,
    )
  }
}

dependencies {
  implementation(project(":modules:core"))
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.work.runtime)
  implementation(libs.maplibre.android)
  implementation(libs.play.services.location)
}
