# Manual checks

## How it was checked

On 2026-11-09 in the simulator (iPhone 18 Pro) and on the emulator, with mocked positions on the route 04 → 06 → 02 → 01 near Siegburg built from the installed courses. A throwaway core test, run against the simulator's installed path data, searched for off-route spots whose way back, found as the app finds it, has a doubtful junction with a picture close ahead: 18 such spots near the Sieg. The checks use one of them, south of the route at 50.7916, 7.2083: "In 90 m, keep left at the fork".

The style B drawing was first seen with a hand-made test picture behind a temporary launch switch, removed again ([light and dark](screenshots/ios-picture-demo-light-dark.png)).

## Scenarios

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| A similar exit close by, Crossing a main road, Leaving the road for a cycle path, A plain turn | unit test | unit test | `PathRouterTest`; 323 doubtful junctions with pictures turned up in ways around Siegburg |
| Crossing a main road and turning, Cycle path after a road, A similar exit | unit test | unit test | Texts in all four languages |
| Text on the Android lock screen | ✅ | — | The notification's text is the sentence: "In 90 m, keep left at the fork", in French "Dans 90 m, restez à gauche à la bifurcation" in two lines ([English](screenshots/android-notification.png), [French](screenshots/android-notification-french.png)). The German run did not count as off route on the emulator twice (flaky mocked fixes, also once in English); the German forms are as long as the French at most |
| Picture of a junction, Footways at a busy junction, Roads that can't be ridden | unit test | unit test | `JunctionPictureTest` |
| Picture with a screen reader | ✅ label | ✅ label | The picture with its sentence is one element labelled "In 90 m, keep left at the fork" in the accessibility tree; not yet heard |
| Approaching a doubtful junction in the app | ✅ light | ✅ light and dark | Below the header row the picture and the sentence, the row keeping its height ([iOS](screenshots/ios-fork-light.png), [iOS dark](screenshots/ios-fork-dark.png), [Android](screenshots/android-fork.png), [Android French](screenshots/android-fork-french.png)). Android dark not seen: on the dark run the app found another way, with the fork beyond 150 m |
| After the junction, Leaving the way before the junction, No picture on an easy turn | unit test | unit test | `JunctionPictureTimingTest` on the core's state |
| On the iPhone's lock screen, Dynamic Island at a doubtful junction, Back to text, Screen comes on at a junction | — | open | A test junction on the maintainer's lock screen, 96 pt next to the sentence ([simulator](screenshots/ios-live-activity-test-junction.png)): "might work but it's hard to tell without seeing the reality". On a ride |

## Found and fixed

- A way to a route point, which gets the network's course in front of it (`ledBy`), lost its doubtful junctions, so it never showed a picture. They are now kept, moved by the length put in front (`LedByTest`).
- The panel's smallest height while navigating keeps the highest it has been, so it does not jump near a knot; with a picture it would have stayed taller for the rest of the ride. The height now starts anew when a picture appears or goes, as when the text size changes.

## For the maintainer

1. **On a ride with path data, leave the route** where a way back crosses a main road, takes a cycle path or keeps left or right at a fork: below the header the picture and its sentence from about 150 m before the junction until past it. Note whether each picture helped and whether any was wrong.
2. **The Pixel's notification** at such a junction: the sentence instead of the usual line.
3. **Once with TalkBack or VoiceOver:** the picture is read as its sentence.
4. **Wet or in bright sun:** whether the picture can be read.
5. **iPhone locked at such a junction:** the lock screen shows the picture with its sentence in place of the header row, the Dynamic Island text only, and after the junction the row again. Whether 96 pt is big enough when compared with the real junction.

## Android in dark mode (task 4.1), 2026-11-11

A mocked ride on the emulator in dark mode: route 06 → 02 → 28 started in Siegburg-Dreesch, "Ride to 06", with the fixes of the recorded ride `dreesch-to-06` along the guided way.

- "In 120 m, keep left at the fork" at fix 83: the picture on a dark tile, the way in green, the other branch in grey ([screenshot](screenshots/android-dark-keep-left.png)).
- "In 120 m, take the cycle path ahead" at fix 112 ([screenshot](screenshots/android-dark-cycle-path.png)).
- Picture with a screen reader: the accessibility tree gives TalkBack the sentence, e.g. "In 90 m, take the cycle path ahead".

