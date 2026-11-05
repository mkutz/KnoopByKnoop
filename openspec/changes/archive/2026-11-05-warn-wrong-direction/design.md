# Design

## Context

- `Navigation.update` handles a precise fix: passing a knot first, then off route by `distanceToCourse`, then the place on the route.
- `RouteProgress.placeOf(fix, nextIndex)` gives the place along the current leg only. So legs of an out-and-back route do not mix, and the place already moves backwards when the rider does. Nothing looks at that.
- `distanceToCourse` includes the neighboring legs and the courses to other signposts. `placeOf` projects on the leg's main course only, so a rider on a branch to another signpost gets a place that can move oddly.
- `updateCoarse` ignores inaccurate fixes, and they should stay out of this too.
- `Pace.at` already ignores steps backwards, so riding back does not spoil the pace.
- On the ride, the warning would have helped from 4:55 to 8:20: a drop of 150 m is reached about 30 s after turning.

## Goals / Non-Goals

**Goals:**
- A warning within about 30 s of riding back along the route, by voice and on screen.
- No false alarms from GPS scatter, hairpins or branches.
- No extra location sampling while riding normally.

**Non-Goals:**
- Knowing the rider's intent. A deliberate turn past 150 m gets one notice, which is cheap.
- Detecting it while off the course or on a parallel path.

## Decisions

**Compare with the furthest place on the leg.** `Navigation` keeps `furthest`, the highest place reached on the current leg and the fix there. It resets on passing a knot and when off route.
- Wrong way when: `place <= furthest.place - 150 m`, the fix is at least 100 m in a straight line from `furthest`'s fix, and it is within 30 m of the leg's main course.
- The straight-line condition filters jumps of the nearest point: at a hairpin, the place can drop a long way while the rider barely moves.
- A step of the place of more than 3 times the distance moved plus 50 m is a jump, and the comparison starts anew from there. Found while implementing: without this, a jump forward raises the furthest place, and riding on from the real place looks like riding back.
- `furthest` only moves within the 30 m as well, so a place projected from a branch does not count.
- The 30 m is the same as "back on route". It leaves out branches to other signposts and parallel paths.
- Alternative: the rider's heading against the course's direction. Headings are noisy at low speed and on winding paths, and a few wrong fixes would need smoothing anyway. The place on the route is already there.
- Alternative: treating the distance to the next knot as growing. It grows on any winding path that bends away, so it is no signal on its own.

**150 m.** More than GPS scatter and a turn back for a dropped glove, less than a rider wants to waste. At 18 km/h that is 30 s. To be judged on a ride, as a constant.

**A state of its own, not off route.** `wrongWay` is separate from `offRoute`, as off route freezes the place, draws the line, shows the straight distance, and ends within 30 m of the course, where this rider already is.
- While `wrongWay`: the place keeps moving, so the distance along the route grows honestly. `wayToRoute` stays `null`, so no line and normal following. Precision is `FINE`, as off route.
- Off route wins: more than 100 m from the course sets `offRoute`, clears `wrongWay`, and gives the off-route notice.
- `NavigationDisplay` gets `wrongWay` and `wrongWayText`. `progress` reports the red dot as for off route.

**Ending it.** With `wrongWay`, the app keeps the lowest place reached. At 50 m above it: the existing back-on-route notice, and `furthest` starts from that place. So the rider needs another 150 m back to hear it again, and scatter cannot make it flip. Passing a knot clears it through the usual path.

**Wording.** The spoken notice asks to turn around; the display is short, in the form of the off-route header:

| Language | Spoken and notice | Display |
|---|---|---|
| English | Wrong direction, turn around, next 28 | Wrong direction, next 28 |
| German | Falsche Richtung, bitte wenden, nächster Knotenpunkt 28 | Falsche Richtung, nächster 28 |
| Dutch | Verkeerde richting, keer om, volgend knooppunt 28 | Verkeerde richting, volgende 28 |
| French | Mauvaise direction, faites demi-tour, prochain point-nœud 28 | Mauvaise direction, suivant 28 |

**Apps.** The header, the notification's title and the Live Activity branch on `wrongWay` beside `offRoute`, in the same red. The compact Dynamic Island shows the warning sign and the next knot, its minimal form the next knot. On Android, the chip and the large badge show the next knot.

## Risks / Trade-offs

- **Winding courses:** where a course runs back beside itself within 30 m for a long way, a rider could look as if riding back. The 100 m straight-line condition helps but does not rule out a long hairpin. Tests cover a short one; the ride will show the rest.
- **Coarse sampling with the screen off:** fixes can be about 30 s apart, so the warning can come a little later. Sampling stays as it is until the warning stands.
- **Deliberately going back:** a rider who turns back on purpose hears it once. There is no way to turn it off; if this annoys on rides, it is a matter for the maintainer's notes.
- **`Navigation` grows:** two more fields on a large class. Kept small by putting the comparison in one private function.
