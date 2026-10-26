package com.knoopbyknoop.pipeline

import com.knoopbyknoop.core.NetworkType

/**
 * Turns OpenStreetMap node network data into the compact graph files the apps download.
 *
 * The extraction itself is specified in `openspec/changes/add-network-graph-pipeline`.
 */
fun main(args: Array<String>) {
  println("knoopbyknoop pipeline")
  println("Supported networks: ${NetworkType.entries.joinToString { it.name.lowercase() }}")
  if (args.isEmpty()) println("Usage: pipeline <osm-extract.pbf> <output-dir>")
}
