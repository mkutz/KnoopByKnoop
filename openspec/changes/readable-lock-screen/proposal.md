# Proposal

## Why

The maintainer's note on the test ride of 2026-11-01 (Bonn towards Troisdorf, Pixel 7, video 2 at 21:33) about the notification with the screen off: "Not super readable IMHO. Can we do something cooler here?"

- **On Android it is small text:** "Bei 29 folge 25", "1,4 km bis 29 · Ende gegen 15:25", without badges. The iOS Live Activity already has them.
- **Its distance is out of date:** the video shows "1,4 km bis 29" on the lock screen, and 96 m in the app opened seconds later. By design, that line is only renewed when a knot is passed.
- **The words differ from the app's:** since `follow-number-when-close`, the app shows the next knot first and the number to follow when close.

## What Changes

- **Android, a Live Update (Android 16 and up):** the system's style for rides and deliveries, on the lock screen and promoted over other apps:
  - the words as in the app, as the title;
  - the distance and the arrival;
  - a progress bar along the route with the rider and marks for stops and ferries, like the strip;
  - the knot to look for as a large badge, and as a chip in the status bar.
- **Older Android versions** keep the plain notification, with the new words and the badge.
- **iOS:** the Live Activity shows the far and close wording with its badges, as the app does, and gets the same bar along the route, on the lock screen and in the Dynamic Island's expanded form.
- **The distance stays current on both:** the progress line is renewed about every 100 m of the route, on the fixes navigation already gets. So is its arrival, with the rider's current pace.
- **Stop** stays in both.

## Out of Scope

- **More location fixes** with the screen off: the distance is only as fresh as the fixes.
- **A custom layout on Android:** the maintainer chose the Live Update over a card with our own badges. Custom layouts cannot be promoted.
- **The Dynamic Island's compact form**, which keeps its next knot and number to follow, for lack of room.
- **The other ride notes:** out-and-back legs with a fork, and the language switch during navigation.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: "Navigation with the screen off" gets the app's wording, a distance renewed on the way, and the Android Live Update.

## Impact

- **Core:**
  - `NavigationDisplay.text` gives the far wording.
  - `Navigation` renews `progressLine` per 100 m on the route.
  - Tests are in `RouteProgressTest` and `NavigationTest`.
- **Android:** `NavigationService.notification`, `POST_PROMOTED_NOTIFICATIONS` in the manifest, and the badge as a bitmap from `KnotBadges`.
- **iOS:** `NavigationActivityAttributes.ContentState` gets `close`; `NavigationLiveActivity.swift` and `Navigator.swift`.
- **Archive order:** after `follow-number-when-close`, whose `display.close` this uses.
