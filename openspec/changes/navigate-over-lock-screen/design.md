# Design

## Context

See proposal.md for the motivation. `MainActivity` is the only activity. `PlannerScreen` keeps the display on while navigating. `Navigating.visible` follows `onStart` and `onStop`, and sets how often navigation samples. A test build has this change, accepted by the maintainer on their Pixel 7 with a real look at the lock screen, but not on a ride yet.

## Goals / Non-Goals

**Goals:**
- Keep what the maintainer accepted, without adding to it.
- No more location fixes with the screen off than today.

**Non-Goals:**
- A separate reduced screen for the lock screen: it's the normal navigation view.

## Decisions

**`setShowWhenLocked` follows navigating, set from the UI's state.** Not the manifest attribute, which would show planning over the lock screen too. It needs API 27; on API 26 the lock screen stays as usual. Alternative: a separate activity for the lock screen, more code and a second map, for no visible gain.

**Whether the phone is locked is checked once a second while the activity is started.** `KeyguardManager.isKeyguardLocked` in a loop with `repeatOnLifecycle(STARTED)`. The first version used `ACTION_USER_PRESENT` and missed an unlock on the emulator, leaving the search hidden: the broadcast can come before the receiver is registered. The listener for keyguard changes needs a permission apps can't get. It stops with the activity, so it doesn't run with the screen off.

**Hold to stop is an own gesture on a `Surface`,** as a Material button has no long press, and the system's long press is too short and shows no progress. The hold is timed with `withTimeoutOrNull`, separately from the fill's `Animatable`: with the phone's animations off the animation ends at once, and a touch would stop the ride if it decided. A tap fills to about a third before emptying, as a press that short would otherwise show nothing.

**The fill is the same content twice:** outlined with dark text below, white on red above, the upper one clipped to the progress. So the label stays readable where the fill's edge crosses it. A screen reader gets one button with the label, whose action stops navigation at once.

**Sampling stays as it is.** Over the lock screen with the display on, the activity is started, so `visible` is true and navigation samples live, as with the app open. With the display off, `onStop` makes it false, as today. So the battery cost only comes while the rider is looking.

**The display is kept on with the window's `FLAG_KEEP_SCREEN_ON`,** not `View.keepScreenOn`. Measured on the API 37 emulator: after leaving the app and coming back, the view's flag stayed in force for good, also over the lock screen and after navigation ended. The window's flag can be cleared in all of these.

## Risks / Trade-offs

- **The route and position show to anyone holding the locked phone.** → Accepted for the ride, and the notification already shows the next knots. Sharing, export and search stay out of reach.
- **A pocket touch can still move the map or scroll the panel.** → Harmless; a hold on the button is the only way to end the ride from there. Whether a long press happens in a pocket is for the ride to show.
- **The hold button moves when the header's text changes.** → Accepted by the maintainer as it is, listed under out of scope.
- **One unexplained stop on the emulator:** navigation had ended after a tap in a run with mistaken coordinates, not reproduced in later taps on the button. → The tap scenario gets checked again in the tasks, and the ride watches for it.
- **With TalkBack on, the navigation didn't show over the lock screen on the emulator:** the usual lock screen with the notification showed in both tries, whose Stop works by tapping. So "Stop with a screen reader" is unverified, and the cause unknown; enabling TalkBack by adb may play a part. → Check on a phone with TalkBack on.
- **Battery while looking:** the map renders and samples live only with the display on, and the display times out. → Measured on the ride, against "Battery use while navigating".
