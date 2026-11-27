plugins {
  application
  alias(libs.plugins.kotlin.jvm)
}

kotlin { jvmToolchain(21) }

application { mainClass = "com.knoopbyknoop.website.MainKt" }

dependencies {
  implementation(project(":modules:core"))
  implementation(libs.commonmark)
  implementation(libs.commonmark.ext.gfm.tables)
  testImplementation(libs.kotlin.test)
}

// The tests build the site from the real repository content, see `project-website`.
tasks.test {
  useJUnitPlatform()
  systemProperty("repositoryRoot", rootDir.absolutePath)
  inputs.dir("content")
  inputs.dir("static")
  inputs.dir(rootDir.resolve("fastlane"))
  inputs.files(
    rootDir.resolve("PRIVACY.md"),
    rootDir.resolve("TERMS.md"),
    rootDir.resolve("design/icon/icon.svg"),
  )
}

// The published site, built from the repository into build/site, see `project-website`.
tasks.register<JavaExec>("site") {
  description = "Builds the website into build/site"
  group = "build"
  val output = layout.buildDirectory.dir("site")
  outputs.dir(output)
  inputs.dir("content")
  inputs.dir("static")
  inputs.dir(rootDir.resolve("fastlane"))
  inputs.files(
    rootDir.resolve("PRIVACY.md"),
    rootDir.resolve("TERMS.md"),
    rootDir.resolve("design/icon/icon.svg"),
  )
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass = application.mainClass
  // The published network data for the planner, e.g. from `gh release download network-data`; the
  // site builds without it, but the planner then finds no data.
  val networkData = providers.gradleProperty("networkData").orNull?.let { file(it).absolutePath }
  networkData?.let { inputs.dir(it) }
  args(listOfNotNull(rootDir.absolutePath, output.get().asFile.absolutePath, networkData))
}
