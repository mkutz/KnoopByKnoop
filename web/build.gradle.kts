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
