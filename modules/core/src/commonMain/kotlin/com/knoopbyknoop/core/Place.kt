package com.knoopbyknoop.core

/**
 * A village, town or city a knot lies in, see `network-data` "Place of each knot".
 *
 * @property name the name on its signpost, OpenStreetMap's `name`
 * @property names its names in the app languages where they differ from [name]
 */
data class Place(val name: String, val names: Map<Language, String> = mapOf())
