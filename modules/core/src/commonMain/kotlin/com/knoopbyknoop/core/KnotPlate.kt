package com.knoopbyknoop.core

/**
 * How a knot's badge looks like its signs: the plate's colour, and whether a white ring sits inside
 * a filled plate, see `map-planning` "Route knots stand out".
 *
 * @property colour the plate's colour as ARGB
 * @property innerRing whether a filled plate has a white ring inside, as on the signs in North
 *   Rhine-Westphalia and Belgium; Dutch plates have none
 */
data class KnotPlate(val colour: Long, val innerRing: Boolean) {

  /** The plate's name in map image names and map feature properties. */
  val key: String
    get() = PLATES.entries.firstOrNull { it.value == this }?.key?.key ?: NONE_KEY

  companion object {
    /** The app's green, for knots without a sign region. */
    val NONE = KnotPlate(0xFF2E9E5B, innerRing = true)

    /** The key of [NONE]. */
    const val NONE_KEY = "none"

    private val PLATES =
      mapOf(
        SignRegion.NETHERLANDS to KnotPlate(0xFF12663A, innerRing = false),
        SignRegion.BELGIUM to KnotPlate(0xFF1A5FB4, innerRing = true),
        SignRegion.NORTH_RHINE_WESTPHALIA to KnotPlate(0xFFC8102E, innerRing = true),
      )

    /** The plate of a knot in [region], [NONE] without one. */
    fun of(region: SignRegion?): KnotPlate = region?.let { PLATES[it] } ?: NONE

    /** The plate named [key], [NONE] for an unknown one. */
    fun ofKey(key: String?): KnotPlate =
      SignRegion.entries.firstOrNull { it.key == key }?.let { of(it) } ?: NONE
  }
}
