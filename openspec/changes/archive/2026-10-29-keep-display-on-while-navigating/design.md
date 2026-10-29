# Design

## Context

See `proposal.md` for why. Neither app keeps the display on today: there is no `FLAG_KEEP_SCREEN_ON` on Android and no `isIdleTimerDisabled` on iOS. Both apps know whether they are navigating: on Android from the state's `navigating` (from `Navigating.state`), on iOS from `Navigator.shared.state != nil`.

## Goals / Non-Goals

**Goals:**
- The display stays on exactly while navigating with the app in the foreground.

**Non-Goals:**
- Wake locks or keeping the display on in the background, which the systems don't allow for this anyway.
- Dimming or brightness.

## Decisions

### Android: `View.keepScreenOn` from the UI while navigating
A `DisposableEffect(state.navigating)` in `PlannerScreen` sets `LocalView.current.keepScreenOn` to `state.navigating`, and to `false` when it leaves the composition. `keepScreenOn` works through the window's `FLAG_KEEP_SCREEN_ON`, which only applies while the window is visible. Leaving the app or pressing the power button therefore works as usual without extra code ("Leaving the app", "Turning the display off").
*Alternative:* a `WakeLock` in the navigation service. It needs a permission, keeps the display on in the background too, and costs battery the rider didn't ask for.

### iOS: `isIdleTimerDisabled` while navigating
`UIApplication.shared.isIdleTimerDisabled` is set from `Navigator.shared.state != nil` in the app, on every change of the navigation state. iOS only applies it while the app is active, so leaving the app restores the normal Auto-Lock.
*Alternative:* tying it to `scenePhase` as well. Not needed, since the system already ignores it in the background, and resetting it on every phase change would add code without changing behavior.

## Risks / Trade-offs

- [More battery with the display on] → It only happens with the app open; the rider can turn the display off with the power button, and navigation is built for that. The battery requirement measures the display-off case and stays as it is.
- [Hard to test automatically] → Checked manually: on the emulator with a short display timeout (`settings put system screen_off_timeout 15000`, restored afterwards), on the iPhone by the maintainer.
