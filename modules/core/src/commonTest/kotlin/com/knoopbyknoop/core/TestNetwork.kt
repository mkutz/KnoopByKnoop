package com.knoopbyknoop.core

/**
 * A small cycling network to reason about by hand. Knot IDs equal their refs.
 *
 * ```
 *        2000        3000
 *   12 -------- 30 -------- 45
 *    \          |          /|
 *  1500\    2500|   2000 /  | 4000 (12-45 direct)
 *        \      |      /
 *           --- 7 ---
 *
 *   99 -- 98   (separate, unreachable from the rest)
 * ```
 *
 * Shortest paths: 12→45 is 12, 7, 45 (3500 m, the direct connection is 4000 m), 12→30 is direct
 * (2000 m), 30→45 is direct (3000 m).
 */
object TestNetwork {

  val knot12 = cyclingKnot(12, 52.090, 5.120)
  val knot30 = cyclingKnot(30, 52.100, 5.150)
  val knot45 = cyclingKnot(45, 52.110, 5.190)
  val knot7 = cyclingKnot(7, 52.080, 5.160)
  val knot99 = cyclingKnot(99, 52.300, 5.500)
  val knot98 = cyclingKnot(98, 52.310, 5.510)

  val graph =
    NetworkGraph(
      knots = listOf(knot12, knot30, knot45, knot7, knot99, knot98),
      connections =
        listOf(
          Connection(12, 30, 2000),
          Connection(30, 45, 3000),
          Connection(12, 45, 4000),
          Connection(12, 7, 1500),
          Connection(7, 45, 2000),
          Connection(30, 7, 2500),
          Connection(99, 98, 1000),
        ),
    )

  private fun cyclingKnot(ref: Long, latitude: Double, longitude: Double) =
    Knot(id = ref, ref = "$ref", network = NetworkType.CYCLING, latitude, longitude)
}
