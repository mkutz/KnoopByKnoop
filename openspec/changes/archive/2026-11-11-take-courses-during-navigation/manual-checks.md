# Manual checks

## How it was checked

On 2026-11-11 on the emulator (API 37) with the debug build and the published data of 2026-11-08, the route 01 → 02 → 28 near Siegburg, replaying the recorded ride `siegburg-going-on` with `adb emu geo fix`.

## Scenarios

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Courses arriving on the course, Knots passed before the courses, Off route when the courses arrive | unit test | unit test | `NavigationSessionTest`; the ferry warning kept in `FerryWarningTest` "warned once also after the courses arrive" |
| Courses on a slow phone | ✅ | build | The app started, Navigate tapped before "startup: courses" (22.5 s on the emulator). Navigation started at 11:57:33, the courses came at 11:57:38, and leaving the route at the Sieg was told: "Off route, next 28, 1.7 km to the north-west", then the guided way back ([screenshot](screenshots/android-courses-after-early-navigate.png)). Before the change the same press gave a ride without "Off route" (seen 2026-11-11 earlier the same day). The first position reached the navigation only after the courses, as the app is busy while loading them, so here the start took them; taking them after the first position is covered by the unit tests. On the iPhone the courses load in about 0.7 s, too fast to hit by hand |
