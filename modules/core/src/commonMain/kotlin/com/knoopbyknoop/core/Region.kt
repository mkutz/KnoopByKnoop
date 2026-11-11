package com.knoopbyknoop.core

/** The countries of the covered area, see `network-data` "Borders between knots". */
enum class Country(val key: String) {
  NETHERLANDS("NL"),
  BELGIUM("BE"),
  GERMANY("DE"),
}

/**
 * A province, Brussels or a state, see `network-data` "Borders between knots".
 *
 * @property key its ISO 3166-2 code, e.g. `NL-LI`
 * @property name its name in OpenStreetMap
 * @property names its names in the app languages where mapped
 */
data class Region(
  val key: String,
  val country: Country,
  val name: String,
  val names: Map<Language, String> = mapOf(),
)

/**
 * Where a connection's course, ridden from its `from` knot, crosses a border.
 *
 * @property atMeters how far along the course
 * @property from the region before
 * @property to the region beyond
 */
data class Crossing(val atMeters: Int, val from: Region, val to: Region)
