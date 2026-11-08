# Measurements

## Share of doubtful junctions (task 1.2)

Measured 2026-11-08 with `PathRouter` and the rules of task 1.1 on the published `cycling.paths` (release `network-data`, 102.8 MB), with no requests to outside services. The eight trips of `add-offline-paths` are kept only as names; they were rebuilt from public coordinates of their starting places, to the knot with the given number whose way comes closest to the length in that design's table. All came within 7 % of it.

| Trip | km (table) | Decision points | Doubtful |
|---|---|---|---|
| Siegburg Markt to 28 | 4.00 (4.14) | 15 | 9 |
| Troisdorf Bf to 28 | 3.04 (3.22) | 17 | 4 |
| Sankt Augustin Zentrum to 27 | 7.27 (7.19) | 44 | 8 |
| Bonn Hbf to 81 | 2.22 (2.41) | 16 | 3 |
| Beuel Bf to 80 | 1.12 (1.24) | 10 | 1 |
| Hennef Bf to 02 south-east | 7.01 (6.64) | 41 | 10 |
| Lohmar Markt to 02 north-west | 6.90 (7.08) | 42 | 12 |
| Niederkassel Rathaus to 28 | 10.76 (11.45) | 59 | 10 |
| **Total** | **42.3** | **244** | **57: 23 %, 1.35 per km, one every 740 m** |

A decision point is a junction with a choice of ways, those within 40 m counted once, as for turns; this counts more of them than the rough test (94 on 44.6 km), so the share is lower while doubtful junctions per km are higher than the rough test's "almost one per km".

By rule (a junction can have several): crossing a main road 23, leaving the road for a cycle path or path 22, a similar exit 21. Narrower sets:

| Rules | Doubtful | One every |
|---|---|---|
| All three (the design) | 57, 23 % | 740 m |
| Main road and similar exit | 40, 16 % | 1.1 km |
| Main road and leaving the road | 43, 18 % | 980 m |
| Similar exit and leaving the road | 39, 16 % | 1.1 km |
| Main road only | 23, 9 % | 1.8 km |
| Two or more of the rules at once | 8, 3 % | 5.3 km |

The guided way is only shown off route and to and from route points, so on a ride these come up far less often than per kilometre of the network.

**Decided by the maintainer, 2026-11-08:** keep all three rules.
