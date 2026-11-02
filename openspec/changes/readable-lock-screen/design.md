# Design

## Context

- **Android** (`NavigationService.notification`): a `Notification.Builder` with title and text, `CATEGORY_NAVIGATION`, public on the lock screen, with a Stop action. It is renewed per fix with `display.text` or `offRouteText` and `progressLine`. `minSdk` is 26, `compileSdk` 37. The Pixel 7 and the emulator run API 37.
- **The SDK (API 36 and up):** `Notification.ProgressStyle` (segments, points, tracker, start and end icons), `Builder.setRequestPromotedOngoing`, `Builder.setShortCriticalText`, `NotificationManager.canPostPromotedNotifications`, and the permission `POST_PROMOTED_NOTIFICATIONS`.
- **iOS:** the Live Activity's `ContentState` has `next`, `follow`, `offRoute`, the roles, `text` and `progress`. `NextKnot` draws "At 45 follow 12" with badges, also in the Dynamic Island's expanded form. `Navigator.updateActivity` runs per fix and only sends a changed state.
- **The line** (`Navigation.progressLine`): renewed when a knot is passed, and off route per `LINE_METERS` (100 m) in `guide`.
- **The wording:** `display.close`, from `follow-number-when-close`. `Texts.next` already has "Next 29, then 25" in all four languages, for the notices.
- **The strip:** `Navigation.progress(fix).strip` has the marks with their fractions and the rider's place.

## Goals / Non-Goals

**Goals:**
- Readable at a glance on the handlebar, with the same words as the app.
- A distance that is about as fresh as the fixes.

**Non-Goals:**
- More fixes or more wakeups with the screen off.
- Our own layout on Android.

## Decisions

**`NavigationDisplay.text` gives the far wording** when `!close` and `follow != null`: `texts.next(next, follow)`. So `display.text` is the one string for outside the app. The German is the notices' "Nächster Knotenpunkt 29, dann 25", which fits the title on the API 37 emulator.

**`progressLine` per 100 m of the route.** In `update`, on route, the line is renewed when the place moved 100 m from where it was last renewed, as `guide` does off route. It uses the current pace from `arrivals-at-current-speed`. Precise fixes only.

**The Live Update.** From API 36, `notification` adds the bar; promotion and the chip need API 36.1 (Android 16 QPR2), behind their own version check (`SDK_INT_FULL`), as lint found:
- **`setRequestPromotedOngoing(true)`**, with `POST_PROMOTED_NOTIFICATIONS` in the manifest. It is a normal permission, granted at install, and the rider can turn it off per app. Without it, the system shows the same notification unpromoted, so no code path differs.
- **`ProgressStyle`** on a scale of 0–1000:
  - `setProgress` from the strip's rider place;
  - points from the strip's marks without the start and the end;
  - the rider's arrow as the tracker;
  - the end knot's badge as the end icon.
  - Off route, the bar stays at the last place on the route with its tracker in red, as the strip's dot.
- **`setShortCriticalText`:** the chip. The next knot while far, the number to follow when close, the next knot off route. So the chip and the large icon are the same knot as the rider's next decision.
- **`setLargeIcon`:** that knot's badge as a bitmap, in its route colors, from the drawing the map's badges use. On all API levels.

**Only notify on a change.** `apply` compares title, text, chip and the bar's place rounded to 1% with the last notification, instead of notifying per fix. At 1 Hz with the app visible, per-fix updates would be wasted work, and the system may rate-limit them.

**iOS: the bar.** `ContentState` gets `riderFraction`, `offRoute` as it has, and `marks`: the strip's marks without the start, each a fraction and whether it is a ferry with a warning, a ferry or a stop. A small SwiftUI `ProgressBar` draws them, the ridden part green, the marks as dots and the rider as a dot, red off route. It is sent when the line is renewed, so about every 100 m, and not per fix, to stay within what iOS allows. The marks stay few, by the strip's ranking, as a Live Activity's state must stay under 4 KB.

**iOS: `ContentState.close`.** `NextKnot` draws "Next (29)" and "then (25)" when far, with 29 as the prominent badge, and "At (45) follow (12)" when close, as the app's header. The card's height follows, and the system gives the Live Activity the room it needs. The compact Dynamic Island stays as it is.

**The app tells the Live Activity the system's mode.** Found by the maintainer on the iPhone: on the lock screen, the Live Activity's `colorScheme` is dark also with the system in light mode, and `activityBackgroundTint` still gave a light card, so the text was unreadable. So the card draws an opaque background and uses only opaque colors, chosen by `ContentState.dark`. The app sets it from the screen's trait collection, not its windows', which follow the appearance chosen in Info. It is compared with each position, so a change shows at the next fix, with the screen off about every 30 s. Rendering the views in the app did not show any of this, so the Live Activity's colors can only be judged on a device.

## Risks / Trade-offs

- **Android's bar shows at most 4 marks:** measured on API 37, where 12 offered still gave 4. So the most important four are sent: ferries with a warning, then stops, then other ferries, each nearest first. That's fewer than the mock-up the maintainer picked, which had every knot. iOS draws our own bar, so it can show up to 8.
- **Promotion rules are the system's:** it can refuse to promote a notification that breaks them, e.g. colorized ones. `canPostPromotedNotifications` and a screenshot on the Pixel show the result.
- **Older Android keeps text,** with the large badge. The maintainer accepted that.
- **The lock screen is only as fresh as the fixes:** with the screen off, a fix about every 30 s far from a knot. At 20 km/h that is about 170 m, so the distance moves in those steps.
- **The mode in the background:** whether iOS reports a changed mode to an app in the background has to show on the iPhone. If not, the card follows it when the app is next opened.
- **Live Activity updates can be throttled by iOS.** The state only changes per 100 m, which is well within what navigation apps send.
