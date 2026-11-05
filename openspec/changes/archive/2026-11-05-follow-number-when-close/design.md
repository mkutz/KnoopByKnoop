# Design

## Context

- **The header** (`NavigationHeader`):
  - **Android** (`PlannerScreen.kt`): a `FlowRow` with "At", the next knot's badge (scale 1.3), "follow", the badge of the number to follow (scale 1.8) and the distance in `titleMedium`.
  - **iOS** (`PlannerView.swift`): the same in a `ViewThatFits`, with the distance below where it does not fit.
  - Both take what to show from `Navigation.display`, a `NavigationDisplay` with `next`, `follow` and `offRoute`.
- **The reminder** (`Navigation.update` and `start`):
  - `remindedIndex` becomes `nextIndex` when the rider comes within `REMINDER_METERS` (200 m) straight of the next knot, at the start if they start that close, and right on passing when the new next knot is closer than 200 m to the one passed (`closeToPrevious`).
  - It is private, and it is exactly "close" as the spec now uses it.
- **Outside the app:** the Android notification and the iOS Live Activity use `display.text` and `display.follow`; they stay as they are.
- **Strings:** "At", "follow" and "is the end" are app strings (`values*/strings.xml`, `Localizable.xcstrings`) in English, German, Dutch and French.

## Goals / Non-Goals

**Goals:**
- The header switches when the reminder is spoken, so the screen and the voice agree.
- Both platforms take the switch from the core.

**Non-Goals:**
- Changing the reminder, the notification or the Live Activity.
- A distance setting.

## Decisions

**`NavigationDisplay.close: Boolean`**, set by `Navigation.display` from `remindedIndex == nextIndex`. It's a new constructor parameter with the default `true`, so the other callers (the notification, the Live Activity, tests) keep "At 45 follow 12" without changes.
- Alternative: the apps compare the distance with 200 m themselves. The header would switch at a slightly different moment than the reminder, which comes from the last precise fix, and knots close together would need `closeToPrevious` again.

**Far layout, two lines:**
- First line: "Next", the next knot's badge at scale 1.8 and the distance in `titleLarge`.
- Second line: "then" and the badge of the number to follow at scale 1.0, in `bodyLarge`.
- At the end of the route, `follow` is `null`, so the header stays "31 is the end" whether close or not.
- Off route, the off-route header, as now.

The first line keeps today's rule that the distance goes below where it does not fit (`FlowRow` on Android, `ViewThatFits` on iOS). The second line is short enough for the largest text size.

**Screen readers:** the far header is one element announcing "Next 83, 1.2 km, then 30".
- Android: `Modifier.semantics(mergeDescendants = true) { contentDescription = … }`.
- iOS: `.accessibilityElement(children: .ignore)` with the label, as the off-route header.

**Strings:**

| | Next | then |
|---|---|---|
| en | Next | then |
| de | Nächster | dann |
| nl | Volgende | dan |
| fr | Prochain | puis |

The German "Nächster" matches "Abseits der Route, nächster"; "then" follows the spoken notices ("dann", "dan", "puis"). The screen reader's label is built from the same two strings plus the distance.

## Risks / Trade-offs

- **The header changes height between far and close** (two lines, then one), and the panel's navigation height follows it, see "Panel height while navigating". Checked on the emulator: the panel got lower at the switch. So it keeps the largest navigation height until navigation ends:
  - Android: `navigationHeight` takes the maximum and goes back to 0 when navigation ends. A font size change recreates the activity, so it is measured anew.
  - iOS: the same, keyed by the text size it was measured at, which `WaypointSheet` reports with the height. Another text size starts anew, so a smaller text size makes the panel lower again. Taking the size from `PlannerView` instead left the panel low after a change, as the height can arrive before the new size.
- **Stop at accessibility sizes (iOS):** next to the far header, Stop was cut to "St…" (`screenshots/ios-stop-cut-off.png`). As with the off-route header, Stop goes above it at accessibility sizes.
- **The switch follows precise fixes only, like the reminder.** With coarse fixes far from a knot, the header changes at the first precise fix within 200 m, the moment the reminder is spoken.
