# Tasks

## 1. Android

- [x] 1.1 `PlannerScreen.kt`: keep the display on with `LocalView.current.keepScreenOn` while `state.navigating`, off otherwise and when leaving the composition; checked on the emulator with a 15 s display timeout ("Navigating with the app open", "Navigation ends", "Planning", "Leaving the app", "Turning the display off")

## 2. iOS

- [x] 2.1 Set `UIApplication.shared.isIdleTimerDisabled` from whether `Navigator.shared.state` is set; both targets build

## 3. Checks

- [x] 3.1 Manual checks in `manual-checks.md`: all five scenarios on the Android emulator (display timeout set to 15 s and restored afterwards); on the iPhone by the maintainer, with Auto-Lock at 30 s: "Navigating with the app open", "Navigation ends", "Leaving the app"
- [x] 3.2 `./gradlew check` passes, CI passes

## Workflow follow-up

- Archive `add-knot-navigation` before this change: the requirement here is added to `knot-navigation`, whose main spec that change creates
