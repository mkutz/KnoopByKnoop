# Design

## Context

See `proposal.md` for the motivation. The maintainer chose style B from a web page of sketches of real junctions. Those sketches are in `sketches/`, turned so the riding direction points up. They're mock-ups, drawn with a throwaway script, and haven't been seen on a phone.

The numbers below come from the same test as `add-offline-paths`: 94 decision points on nine trips over 44.6 km in the Köln region, measured by a test router.

| Rule | Decision points | Share |
|---|---|---|
| Crosses a main road | 26 | 28 % |
| A similar exit close by | 18 | 19 % |
| Leaves a street or main road for a cycle path or path | 14 | 15 % |
| **Any of them** | **40** | **43 %** |

That's almost one picture per kilometre, more than "only at doubtful junctions" suggests. The test's rules were rough, so task 1.2 measures this again before any drawing work, and the maintainer decides whether to narrow them.

## Goals / Non-Goals

**Goals:**
- A picture that can be read at a glance on a bike.
- Text that does the same job where there's no picture.

**Non-Goals:**
- A general map renderer for junctions.
- Making images on a server or storing them. Pictures are drawn on the device from data.

## Decisions

### The picture is data from the core
The core builds a `JunctionPicture` of lines with a kind (way before, way after, main road, street, cycle path) and points in local coordinates. Each platform draws it: Compose on Android and SwiftUI `Path` on iOS. So both platforms show the same content, and it's easy to test without a screen.

Alternative: rendering an image in the app and sending it to the Live Activity. Images in a Live Activity must fit tight size limits, and rendering in the background may not be allowed. This hasn't been tested, and drawing from data avoids the question.

### Geometry of style B
From the sketches: about 95 m across, with the junction's start at two thirds of the height. The way shows 34 m before and 40 m after, with an arrow at its end. Main roads (primary, secondary, tertiary, motorway, trunk) are wide. Streets and cycle paths only appear within about 50 m of the center, and footways and other paths not at all unless the way uses them. The riding direction comes from the way 34 to 10 m before the junction. A couple of the sketches still don't enter exactly from the bottom, so this may need adjusting.

### Doubtful junctions are found when the way is built
This is done once per way in the core, not on every fix. Similar exits are within 35°, as in the test.

### Live Activity content
The picture goes into the content state with the other values, simplified and with short coordinates, at most 1 KB. **Checked 2026-11-09:** Apple documents a 4 KB limit for a Live Activity's data (`ActivityAuthorizationError.attributesTooLarge`); a developer reports that it counts after the JSON is re-serialized, so the 1 KB picture keeps the state well below it.
- **On the lock screen** the picture, 96 pt, takes the row's place next to the sentence and the distance left, so the card stays within the Live Activity's height. The Dynamic Island stays as it is. The state changes when the picture appears and when it goes, so it adds only two updates per junction.

### Sentences have a fixed set of forms
"Cross the main road, then…", "Take the cycle path on the…", and "Keep … at the fork", each with left, right or straight on, in every app language. This is simple enough to translate and speak. The first matching one wins, in that order.

### Android stays text
Live Updates allow no custom layouts, and the large icon was the only image slot found, at 48 to 64 pt in the mock-ups. So `NavigationService` shows only the sentence.

## Risks / Trade-offs

- **[Risk] Too many junctions count as doubtful.** 43 % in the test. → Task 1.2 measures it and asks the maintainer before any drawing work.
- **[Risk] The picture isn't readable on the real lock screen.** The size of about 118 pt is an assumption from the mock-up. → Task 3.2 checks it on the iPhone early, before the rest of the work on iOS. If it's unreadable, iOS follows the Pixel, with text only on the lock screen.
- **[Risk] It can't be read at a glance on a bike.** Untested. → Manual checks on a real ride, and it can be turned off by removing the state's picture.
- **[Risk] A picture makes a bad way look certain.** The doubtful turn-back in `sketches/uturn-schem.svg` looks just as clear. → `add-offline-paths` adds a cost for turning back, and the check ride should note any wrong pictures.
- **[Trade-off] The sentence can be long for a Live Update.** → Check German and French on the Pixel.
- **[Risk] Mapping mistakes.** A missing crossing or a wrong class gives a wrong picture. → Accepted; it comes from the same data as the way.

## Open Questions

- Can the picture be read while wet or in bright sun? This doesn't change the design, but it'll be noted on the ride.
