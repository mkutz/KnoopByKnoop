package com.knoopbyknoop.website

import java.io.File
import kotlin.system.exitProcess

/**
 * Builds the website: `<repository root> <output directory> [<network data directory>]`. With the
 * published network data, the planner's data is checked and copied in too.
 */
fun main(arguments: Array<String>) {
  require(arguments.size in 2..3) {
    "Usage: website <repository root> <output directory> [<network data directory>]"
  }
  val data = arguments.getOrNull(2)?.let { NetworkData(File(it)) }
  val files =
    try {
      data?.let { networkDataProblems(it) }?.let { if (it.isNotEmpty()) throw SiteProblems(it) }
      buildSite(Sources(File(arguments[0])))
    } catch (problems: SiteProblems) {
      System.err.println(problems.message)
      exitProcess(1)
    }
  val output = File(arguments[1])
  writeSite(files, output)
  data?.let { writeNetworkData(it, output) }
  println(
    "Website: ${files.size} files in ${arguments[1]}" + (data?.let { ", data ${it.folder}" } ?: "")
  )
}
