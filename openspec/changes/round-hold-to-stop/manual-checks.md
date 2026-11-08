# Manual checks

## Scenarios

Android checked 2026-11-08 on the emulator (`Medium_Phone_API_37.0`), debug build, navigating 06 → 02 → Alt Mülldorf, with a temporary PIN so the navigation shows over the lock screen (removed again afterwards), presses sent with `adb shell input`.

| Scenario | Android | Notes |
|---|---|---|
| Before touching the stop button | ✅ emulator | Round, outlined in red, the size of the Stop in the app; the search is hidden ([screenshot](screenshots/android-over-lock-screen.png)) |
| A touch on the stop button | ✅ emulator | "Hold to stop" above the button; navigation continues ([screenshot](screenshots/android-tap-hint.png), top) |
| Letting go early | ✅ emulator | A 600 ms press: the ring empties, navigation continues ([screenshot](screenshots/android-tap-hint.png), bottom, after it) |
| Holding the stop button | ✅ emulator | The ring grows clockwise from the top ([screenshot](screenshots/android-ring-filling.png)); after the hold navigation stopped and the lock screen showed as usual |
| Without animations | ✅ emulator | All three animation scales at 0: a tap leaves navigation running, a 1.5 s hold stops it |
| Stop with a screen reader | partly | The button's node reads "Hold to stop" and is clickable, so TalkBack's action stops; TalkBack itself not heard |
| Unlocking, the notification, the lock screen behind, another app, not navigating | unchanged | Not touched by this change |

## For the maintainer

On the Pixel on a ride: wake the phone while navigating, tap the button (the hint shows, navigation goes on), then hold it until the ring closes (navigation stops).
