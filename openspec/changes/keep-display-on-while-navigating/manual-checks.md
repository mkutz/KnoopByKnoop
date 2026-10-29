# Manual checks

## Android

Emulator `Medium_Phone_API_37.0`, 2026-10-29, display timeout set to 15 s (`settings put system screen_off_timeout 15000`) and restored to its previous value afterwards. Awake or asleep read from `dumpsys power` (`mWakefulness`), the window's flag from `dumpsys window`.

| Scenario | Android | Notes |
|---|---|---|
| Planning | ✅ | App open without navigating: asleep after 25 s; the window has no `KEEP_SCREEN_ON` |
| Navigating with the app open | ✅ | Awake after 28 s; the window has `KEEP_SCREEN_ON` |
| Turning the display off | ✅ | Power button while navigating: asleep, and the navigation service keeps running |
| Leaving the app | ✅ | Home while navigating: asleep after 25 s |
| Navigation ends | ✅ | After Stop, the window's `KEEP_SCREEN_ON` is gone again, so the display goes off after the timeout as when planning |

## iOS

Both targets build. Checks by the maintainer on the iPhone, with Auto-Lock at 30 s: ⏳.

| Scenario | iPhone | Notes |
|---|---|---|
| Navigating with the app open | ⏳ | The display stays on longer than 30 s |
| Navigation ends | ⏳ | After Stop, it locks after 30 s again |
| Leaving the app | ⏳ | After switching to another app, it locks after 30 s |
