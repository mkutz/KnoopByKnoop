# Proposal

## Why

The maintainer usually rides with a podcast on. Measured on their Pixel 7 on 2026-11-05: with audio playing, Android puts the Live Update below the media player and collapses it to one line, without its progress bar and with cut-off text. With the player paused, the same notification is first and expanded. An app can't change that order or keep the card expanded.

So a look at a locked phone shows little, and the card can be second or third. The maintainer suggested handling it like Google Maps, with the app over the lock screen. They tried a test build and accepted it as it is: "the lockscreen does look great and the live action still shows, when I flick the screen up".

## What Changes

- **Android:** while navigating with the app on top, locking the phone and waking it shows the navigation over the lock screen, without unlocking.
- **Only the navigation is reachable there.** The place search is hidden; the info sheet and the planning controls are already gone while navigating.
- **Stopping needs a hold** over the lock screen, so a touch in a pocket doesn't end the ride. The button is outlined instead of solid and fills up while held, because it would otherwise look like the Stop button in the app, which works with a tap. The maintainer found a tap seemed to do nothing on the first try.
- **The display times out as usual** over the lock screen, instead of staying on as in the app. This changes "Display on while navigating in the app".
- **The Live Update stays** as it is: pulling the notifications down or flicking up to the lock screen behind shows it, and it is what the rider sees when another app was on top when locking.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `knot-navigation`: adds navigation over the lock screen, and the display rule for it.

## Impact

- Android app: `MainActivity`, `PlannerScreen`, `PlannerActions`, `Navigating`, and a new text in all app languages.
- Privacy: anyone holding the locked phone can see the route and position on the map during a ride. The notification already shows the next knots there. Nothing leaves the phone.

## Out of Scope

- iOS: an app can't show over the lock screen there. The Live Activity stays.
- A setting to turn this off. It can follow if riders ask for it.
- Starting navigation or showing the app over the lock screen when another app was on top when locking.
- The Live Update's content, and its collapsed text. Worth a change of its own if that case matters on rides.
- Keeping the hold button in one place as the header's text changes: accepted as it is for now.
