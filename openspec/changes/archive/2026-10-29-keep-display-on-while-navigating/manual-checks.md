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

Both targets build. Checks by the maintainer on the iPhone, 2026-10-29, with Auto-Lock at 30 s, the app started from the home screen and the phone lying on the table.

| Scenario | iPhone | Notes |
|---|---|---|
| Navigating with the app open | ✅ | Stays on beyond 40 s |
| Navigation ends | ✅ | After Stop, it locks after about 30 s |
| Leaving the app | ✅ | Locks with the app in the background |
| Planning | ✅ | Locks after about 30 s |

## Found and fixed

- **iPhone: display stayed on in planning mode** (maintainer). Not the app: the app ran under the Xcode debugger, which keeps an iPhone from locking; it had also kept it on while navigating before this change. Started from the home screen, it locks as expected. Display checks on the iPhone count only with the app started from the home screen, and with nobody looking at it (Face ID's Attention Aware Features).
