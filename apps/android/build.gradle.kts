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
    // Set by the release workflow, see `release-delivery`.
    versionCode = providers.gradleProperty("versionCode").map(String::toInt).getOrElse(1)
    versionName = providers.gradleProperty("versionName").getOrElse("0.1.0")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  buildFeatures { compose = true }

  // The upload key, only on CI in the release workflow.
  val uploadKeystore = providers.environmentVariable("KNOOPBYKNOOP_UPLOAD_KEYSTORE").orNull
  if (uploadKeystore != null) {
    signingConfigs {
      create("upload") {
        storeFile = file(uploadKeystore)
        storePassword = providers.environmentVariable("KNOOPBYKNOOP_UPLOAD_KEYSTORE_PASSWORD").get()
        keyAlias = providers.environmentVariable("KNOOPBYKNOOP_UPLOAD_KEY_ALIAS").get()
        keyPassword = providers.environmentVariable("KNOOPBYKNOOP_UPLOAD_KEY_PASSWORD").get()
      }
    }
  }

  buildTypes {
    release {
      // Without the upload key, signed with the debug key, so local release builds can be
      // installed, e.g. to measure startup times (`faster-startup`).
      signingConfig = signingConfigs.getByName(if (uploadKeystore != null) "upload" else "debug")
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
  testImplementation(libs.kotlin.test.junit)
  implementation(libs.play.services.location)
}
