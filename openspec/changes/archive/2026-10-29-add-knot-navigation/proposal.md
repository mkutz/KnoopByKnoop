# Proposal

## Why

The apps can plan a route but not ride it. On a knooppunt network, the signs at every junction show the way to the neighboring knots, so a rider only needs to know which number to follow next. The README promises exactly that: "the app only tells you the next knot, so the screen can stay off and the battery lasts". Today the rider has to remember the numbers or write them on a strip of paper.

## What Changes

- A planned route can be navigated. Navigation starts from the planned route while the app is open, at the route knot nearest to the rider.
- The app keeps track of which knot comes next by sampling the rider's position: rarely far from a knot, more often close to one, and often while the screen is on.
- The most visible information is the number to follow at the next knot: "At 45 follow 12".
- Spoken notices in English: on passing a knot ("Next 45, then 12"), and about 200 m before the next knot ("45 ahead, then 12").
- Passing a later knot of the route skips ahead to it. Passing a knot that is not on the route, or getting far from the route's course, is announced as "Off route, next 45". There is no new route back.
- With the screen off, navigation keeps running: Android shows an ongoing notification, iOS a Live Activity on the lock screen and in the Dynamic Island. Both have a Stop button.
- With the screen on, the map shows the rider's position, the route and the next knot.
- After the last knot, the app says "You've arrived" and stops navigating.
- Navigation uses at most 5 % battery per hour with the screen off.

Out of scope:
- Planning a new route back to the route when off route
- Turn directions ("turn left to 12") and distances to the next knot
- Spoken notices in other languages than English, and translating the app
- A watch app (the Live Activity reaches the Apple Watch's Smart Stack without one)
- Recording or saving the ride, statistics
- Continuing navigation after the app was force-quit or the phone restarted
- Starting navigation in the background, e.g. from a widget or a shared link

## Capabilities

### New Capabilities
- `knot-navigation`: following a planned route knot by knot: progress along the route, off-route detection, spoken notices, the screen-off displays on both platforms, the position on the map and the battery budget

### Modified Capabilities

## Impact

- `modules/core`: a navigation state machine (position and route in, next knots, notices and the next sampling interval out), the texts of the notices and displays, and how knot numbers are spoken; all platform-free and tested on the JVM
- `apps/android`: location permission (while in use), a foreground service of type `location` with an ongoing notification, `TextToSpeech` with audio focus, a Navigate button, the rider's position on the map
- `apps/ios`: location permission (when in use) with background location updates and background audio for the spoken notices, a widget extension with the Live Activity and Dynamic Island views (new target in `project.yml`), `AVSpeechSynthesizer` with a voice prompt audio session, a Navigate button, the rider's position on the map
- Store review: both stores ask why the app uses location in the background; Google Play needs a foreground service declaration
- `README.md`: "What the apps can do" gains navigation
