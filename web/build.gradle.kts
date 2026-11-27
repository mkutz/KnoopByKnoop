plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
}

// The route planner in the browser, see `web-planner`. The website generator copies the bundle
// into the planner pages.
kotlin {
  js {
    browser {
      commonWebpackConfig { outputFileName = "planner.js" }
      // The planner's state is tested on Node, without a browser.
      testTask { enabled = false }
    }
    nodejs()
    binaries.executable()
  }

  sourceSets {
    jsMain.dependencies {
      implementation(project(":modules:core"))
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.serialization.json)
      implementation(npm("maplibre-gl", libs.versions.maplibre.gl.js.get()))
      implementation(npm("qrcode-generator", libs.versions.qrcode.generator.get()))
    }
    jsTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.kotlinx.coroutines.test)
    }
  }
}

// What the planner pages load: the bundle, and MapLibre's worker and stylesheet, which it can't
// bundle. The worker gets a .js name, which every host serves as JavaScript.
val plannerFiles by
  tasks.registering(Sync::class) {
    description = "Collects the planner's files for the website"
    group = "build"
    dependsOn("jsBrowserProductionWebpack")
    from(layout.buildDirectory.dir("kotlin-webpack/js/productionExecutable")) {
      include("planner.js")
    }
    from(rootProject.layout.buildDirectory.dir("js/node_modules/maplibre-gl/dist")) {
      include("maplibre-gl-worker.mjs", "maplibre-gl.css", "LICENSE.txt")
      rename("maplibre-gl-worker.mjs", "maplibre-gl-worker.js")
    }
    into(layout.buildDirectory.dir("planner"))
  }
