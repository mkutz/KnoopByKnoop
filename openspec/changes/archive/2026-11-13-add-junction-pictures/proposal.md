# Proposal

## Why

The maintainer finds that left, right and straight don't work well at awkward junctions, such as "cross the road, skip the street, take the cycle path". Sketches from real Köln data on 2026-11-04 showed that one arrow can get these wrong: the L 16 at Sankt Augustin is nearly "straight on" overall, while the real way zigzags across it. The maintainer chose the simple style, on the lock screen, only at doubtful junctions, and text only on the Pixel for now.

## What Changes

- **Doubtful junctions:** on a guided way, a junction counts as doubtful when another similar exit is close by, the way crosses a main road, or the way leaves a road for a separate cycle path.
- **Style B picture:** a small drawing of the junction, with the riding direction up, the way in bold, main roads wide and other exits short, without footways, text or map detail.
- **In the app:** both platforms show the picture when approaching a doubtful junction.
- **iPhone lock screen:** the Live Activity shows the picture on the lock screen. The Dynamic Island stays text.
- **Pixel lock screen:** the notification stays text only, as its image slot looked too small to read in the mock-ups.
- **Wording:** doubtful junctions get a sentence describing them, e.g. "Cross the main road, then turn left". It's on screen and spoken, so the Pixel and riders with the phone in a pocket get it too.
- Easy turns keep the short notice from `add-offline-paths`.

## Out of Scope

- **Pictures on the route itself:** on route, the rider follows the signposts.
- **Style A,** the detailed drawing with crossings and footways.
- **A picture on the Pixel's lock screen,** through its large icon or an app screen over the lock screen. It can be tried later.
- **Pictures in the Dynamic Island or on Android's status chip.**
- **Lanes, traffic lights and crossings in the picture.**
- **Hiking.**

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: new requirements for doubtful junctions, their wording and their pictures in the app and on the iPhone lock screen.

## Impact

- **Depends on:** `add-offline-paths`, for the guided way and the path data, including roads that can't be ridden. Pictures and sentences only appear on a guided way, so only for users who turned that feature on.
- **Core:** finding doubtful junctions, building a picture as lines in local coordinates, and new wording in `Texts` and the speech texts in all app languages.
- **Android:** a drawing in the planner's header. `NavigationService` shows the sentence, with no picture.
- **iOS:** a drawing in the planner's header, and in the Live Activity from data in its content state, in light and dark.
- **Reference:** the sketches of style B are in `sketches/`.
