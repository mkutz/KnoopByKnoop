# Proposal

## Why

On the maintainer's ride of 2026-11-08, the Android navigation over the lock screen still showed the long outlined "Hold to stop" pill, while the app unlocked has a small round Stop button since `sign-like-header`. The pill takes room from the header's row and looks out of place next to it. The maintainer would prefer a button as small as the one in the app, with a ring that fills while it is held, and a hint when it is only tapped.

## What Changes

- Over the lock screen, the stop button is round and the size of the round Stop button in the app, outlined in red with the stop icon, so it still looks different from the solid red Stop.
- Holding it fills a red ring around the button over about a second; when the ring is closed, navigation stops, as now.
- A short tap shows the hint "Hold to stop" above the button for a few seconds, and the ring fills a little and empties again; navigation continues.
- Screen readers still announce "Hold to stop", and its action stops navigation.

## Out of Scope

- **iOS:** the Live Activity's Stop button is the system's; there is no navigation over the lock screen on iOS.
- **The Stop button in the app unlocked**, and the notification's Stop action: unchanged.
- **How long to hold:** stays about a second.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: "Navigation over the lock screen" gets the round button with the ring and the hint on a tap.

## Impact

- **Android:** the hold-to-stop button in `PlannerActions.kt` and its use in the navigation header in `PlannerScreen.kt`. The string "Hold to stop" exists in all four languages.
- **Core and iOS:** none.
