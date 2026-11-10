# Manual checks

## How it was checked

Checked on 2026-11-10:

- **Cut-outs** (task 1.2): `pipeline cut` on the published data of 2026-11-08, 924 KB together (Titz 104 KB, Siegburg-Zange 204 KB, Rhine at Lülsdorf 536 KB, Veluwe 80 KB), almost all of it path squares. `RealRideTest` reads each one as the apps do: the graph, the courses against the graph's checksum, and every square around a knot against its checksum. Knot 43 turned out to be in the Veluwe near Apeldoorn, not in Arnhem; the area is named `veluwe-43`.
- **Titz against the full data** (task 3.1): the ride on the cut-out gives the same 6 notices and the same 5 way searches with the same turns as change 4's replay on the whole published data (`replay-after.txt`); two way lengths read 141 and 514 m where the replay printed 140 and 513, as that replay cut off the meters and this test rounds them.
- **Determinism:** every ride passes on repeated runs, the one with GPS noise included; the eight take 0.22 s together.
- **Scripts that first did not do what they said:** routing the detours of the two Siegburg rides with `path` between hand-picked points let the router run back along the route itself (a "wrong way" episode instead of a detour). Both now go by `line` through points 200 m beside the course, each within about 20 m of a mapped path; the way searches along the detour still use the real path data.

## What each ride shows

| Ride | Finding it names | What the recorded text shows |
|---|---|---|
| `titz-06-08` | guided way back, arriving | off route at fix 109, a way starting behind told "Turn back now", back on route at 164, arrived at 445 |
| `siegburg-through-02` | the signpost used at 02, the crossing at the L16 bridge (walk 2026-11-06) | "At 02 follow 28 straight on", 02 passed at its south-east node; the crossing itself gives no notice, so the ride shows it only through the route's line and distances |
| `siegburg-going-on` | the way back to where the rider left rather than ahead (walk 2026-11-09) | **shows it**: while the rider goes on 200 m beside the route, the guided way keeps starting behind, "Turn back now" four times, alternating with "Follow the way" as the searches change; back on route only where the rider rejoins at 2.25 km |
| `siegburg-way-behind` | a way behind told only as a turn ahead (ride 2026-11-04) | **no longer shows**: the way starting behind is told "Turn back now" (fixes 234, 310); riding on away from the route, the way is told as turns ahead ("Left in 50 m, Siegstraße") |
| `rhine-ferry` | the ferry on the strip and in the notices | at the start "The ferry Personenfähre Lülsdorf-Wesseling closes at 19:00, shortly after you get there around 18:55"; at 21 "Next 20" without naming the ferry, as the "Take the ferry to 20" idea of 2026-11-08 says |
| `rhine-83-twice` | the later visit of 83 taken for the first (ride 2026-11-08) | **no longer shows**: 83, 27, 83 and 30 in order, "At 27 follow 83 back" |
| `rhine-coarse-screen-off` | false "off route" from coarse fixes (ride 2026-11-08) | **no longer shows**: the fixes of 80 and 40 m accuracy 120 m off the course only switch to fine positions; no notice |
| `veluwe-43` | passing a Dutch knot on four nodes | 43 passed at the node the courses use; **new finding**: the reminder for 43 comes when the header still says 389 m along the route ("459 m to 43" in the line), as the course curves around to the signpost and the reminder goes by the straight distance |

The rides pin today's behavior; fixing `siegburg-going-on` or the reminder at 43 is for their own changes, and their recorded texts change with them.
