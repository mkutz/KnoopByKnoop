package com.knoopbyknoop.core

/**
 * A village, town or city a knot lies in, see `network-data` "Place of each knot".
 *
 * @property name the name on its signpost, OpenStreetMap's `name`
 * @property names its names in the app languages where they differ from [name]
 */
data class Place(val name: String, val names: Map<Language, String> = mapOf())

/**
 * The name as on the signpost, followed by the name in [language] in brackets where that differs,
 * e.g. `Liège (Lüttich)`, see `map-planning` "Waypoint list with badges".
 */
fun Place.nameIn(language: Language): String =
  names[language]?.takeIf { it != name }?.let { "$name ($it)" } ?: name
