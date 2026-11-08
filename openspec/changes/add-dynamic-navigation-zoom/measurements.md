# Measurements

## How often tricky stretches occur (task 1.2)

Measured 2026-11-08 with `trickyStretches` on every course of the published networks of 2026-11-06 (`nl-be-nrw`), as installed on the emulator: each connection's course once, by length of the network. The eight public trips of `add-offline-paths` are kept only as names in its design and run along paths from public places, not along the network, so the whole network stands in for them; it covers their areas around Siegburg, Bonn and Troisdorf.

| Limits | Cycling, 78,117 km | Hiking, 68,582 km |
|---|---|---|
| **60° within 100 m (the design)** | **1.16 per 10 km, one every 8.7 km**; 19 % have 3+ turns | **2.59 per 10 km, one every 3.9 km**; 26 % have 3+ turns |
| 60° within 50 m | 0.63 per 10 km, every 15.8 km | 1.47 per 10 km, every 6.8 km |
| 75° within 100 m | 0.64 per 10 km, every 15.6 km | 1.60 per 10 km, every 6.3 km |
| 75° within 50 m | 0.33 per 10 km, every 30.2 km | 0.84 per 10 km, every 11.9 km |
| 90° within 100 m | 0.17 per 10 km, every 60.6 km | 0.43 per 10 km, every 23.1 km |

With the design's limits, a cycling route of 40 km has about 5 tricky spots, a hiking route of 15 km about 4. The risk in the design, that the rule triggers every few hundred meters, does not show: the map would be at the navigation zoom almost all the time.

Turns are measured between the 10 m before and after each point of a course; points closer than 20 m count as one turn, so a corner drawn with several points is one turn and the small zigzags of the data are none (see `TrickyStretchesTest`).

Examples with the design's limits around Siegburg, to look at on the map:

| Where | Turns |
|---|---|
| 50.84052, 7.20346 (by 06 at Lohmarer Brücke) | 2 over 76 m |
| 50.80793, 7.17394 | 2 over 78 m |
| 50.84910, 7.19579 | 2 over 24 m |
| 50.77521, 7.28599 | 2 over 41 m |
| 50.79360, 7.14661 | 2 over 47 m |

**For the maintainer to decide before any app work:** keep 60° within 100 m, or narrow it.
