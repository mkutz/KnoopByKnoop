# Design

## Context

**Android:** `NavigationService.kt` builds the notification from:
- a sentence title from `NavigationDisplay.text`
- a large icon from `badgeBitmap`
- `R.drawable.ic_navigation` as the small icon
- from Android 16, `setRequestPromotedOngoing` with `setShortCriticalText(knot)` and a `ProgressStyle` with one green segment and coloured points

**iOS:** `Shared/NavigationActivity.swift` holds `ContentState`:
- `next`, `follow`, `offRoute`, `wrongWay`, `close` and the roles
- `text` (the sentence)
- `progress`, `riderFraction` and `marks`
- `dark`, for the card's own light/dark choice

`NavigationLiveActivity.swift` draws the lock screen view and the island regions from it.

`sign-like-header` adds `HeaderRow` to `NavigationUpdate`, and `knot-colours-from-signs` adds `KnotPlate` and the corner symbols. Both come first.

**Platform limits found on 2026-11-07:**
- **Android Live Update** (developer.android.com, "Live Updates"):
  - It must not be colorized, must not use custom views, and must have a title.
  - The status chip always shows the small icon. It shows its text whole under 7 characters, and only partly or not at all beyond that.
  - The progress bar takes coloured segments and points, and a tracker icon.
- **iOS:** `activityBackgroundTint` is set in the view, but Apple's developer forums report that it fails to update dynamically and resolves to the wrong appearance on iOS 27.

## Goals / Non-Goals

**Goals:**
- One source for what is shown outside the app: `HeaderRow`, the same as the header.
- No text in the outside views that depends on the app language.

**Non-Goals:**
- Making Android's notification look like the iPhone's card. Android stays a system notification with text, a large icon and a progress bar.

## Decisions

### Outside texts derived from `HeaderRow`, not from `Texts`

**Choice:** `HeaderRow` gets `notificationTitle`, `notificationLine` and `chipText`. They're built from numbers, the distance formatter, arrow characters and ■, never from `Texts`.
- The direction arrow is the relative bearing rounded to eight characters (↑ ↗ → ↘ ↓ ↙ ← ↖).
- On a guided way, the turn arrows are ↰ ↱ ↑.
- Riding the wrong way uses ↩.

`NavigationDisplay.text` and `offRouteText` then have no users outside the app and are removed, along with their `Texts` entries. The speech texts stay.

**Why:** the title is the only place Android shows text prominently. Building it in the core makes it testable once for both platforms, and identical in every language.

**As built (2026-11-07):**
- **The outside texts are an `OutsideView`** from `Navigation.outside(fix, …)`, built from the header row plus the end arrival (`Navigation.endArrival`). The iOS minimal element is chosen in the app from the row's kind.
- **The titles change with the rounded distance and the arrow's 45° sector,** so they update at least as often as the spec asks.
- **`NavigationDisplay.text` and its `Texts` stay in the core,** see task 1.3.

### iOS: the content state carries the row, the view draws it

**Choice:** `ContentState` replaces `text` with:
- a compact `row`: kind, badges (ref, plate, filled, onPlate, role), distance, relative bearing, small next knot, and turn
- `inverted: Bool` for off route and wrong way

Its size stays far below ActivityKit's 4 KB limit.

**The light-or-dark card:**
- The card's colours come from `dark != inverted`, drawn as the view's own full-size background.
- `activityBackgroundTint` is set once to a neutral that matches, only so the system's own chrome isn't the default colour.

This avoids depending on a per-state tint, which is reported unreliable.

**Badges:** `Shared/Badges.swift` switches to the plate scheme from `knot-colours-from-signs`. The widget no longer keeps the old role colours.

### Dynamic Island regions

- **Compact leading:** the miniature row, sized to the island's height (about 22–26 pt badges).
- **Compact trailing:** the distance.
- **Minimal:** a single element, chosen in the core as `HeaderRow.minimal`.
- **Expanded:** the lock screen row and bar.

The light tile off route is a rounded rectangle in the card's light colour behind the arrow, the same signal as the inverted card.

### Android: icons per state and coloured points

- **Large icon:** drawn with the app's badge code: the plate (close), the next knot's badge (far), the to-the-route symbol on a dark disc (off route) or a light disc (on the way), or the U-turn on a dark disc (wrong way).
- **Small icon:** monochrome vector drawables for the to-the-route symbol and the U-turn join `ic_navigation`, and are used in those states. The chip shows them.
- **ProgressStyle:**
  - the segment colour stays the app's green
  - the points take the stop's plate colour
  - a ferry with a warning gets a dark point (`#1B1F1C`)
  - the tracker icon becomes the rider arrow

### Chip text

- Close: "→" plus the number to follow ("→12").
- Far: the next knot ("83").
- Off route or on the way: the arrow character plus the distance, without a space when it would exceed 7 characters ("↖1.7 km", "→800 m").
- Wrong way: "↩" plus the next knot.

The distance is left out when it is far from a knot, because "83 · 1.2 km" is over 7 characters and the chip would cut it.

## Risks / Trade-offs

- **[TalkBack reads arrow characters oddly in the notification title]** → The notification's `tickerText` is left out, and the progress bar's content description keeps the words. To be checked with TalkBack; if it's poor, a spoken-only title variant can follow in its own change.
- **[■ for the end may not read as "end"]** → It is the end's corner symbol on the map and in the app, so riders learn it there.
- **[iOS 27 tint bug affects the system chrome around the card]** → Our own background covers the card. Only the thin system edge may differ.
- **[OEMs may add criteria for Live Updates]** → The notification still works as a plain ongoing one ("Live Update turned off").

## Migration Plan

App-only. The Live Activity's `ContentState` changes shape. An activity running across an app update ends with the update anyway, so no compatibility code is needed.
