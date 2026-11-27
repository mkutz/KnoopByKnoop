package com.knoopbyknoop.website

import java.io.File
import kotlin.system.exitProcess

/** Builds the website: `<repository root> <output directory>`. */
fun main(arguments: Array<String>) {
  require(arguments.size == 2) { "Usage: website <repository root> <output directory>" }
  val files =
    try {
      buildSite(Sources(File(arguments[0])))
    } catch (problems: SiteProblems) {
      System.err.println(problems.message)
      exitProcess(1)
    }
  writeSite(files, File(arguments[1]))
  println("Website: ${files.size} files in ${arguments[1]}")
}
