# Manual checks

## Scenarios

Android checked 2026-10-31 on the emulator (`Medium_Phone_API_37.0`), freshly installed, position mocked with `adb emu geo fix` near Renkum.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| First start | ✅ | ⏳ | Fresh install: the system asks for location access right away ([screenshot](screenshots/android-first-start.png)); after "While using the app" the map centers on the position near Renkum with the dot ([screenshot](screenshots/android-centered.png)) |
| Centering on my position | ✅ | ⏳ | The button below the zoom buttons centers at zoom 14 or closer; the dot marks the position |
| No tracking while planning | ✅ | ⏳ | `dumpsys location`: one request of the app per tap, the GPS provider `OFF` again afterwards |
| Later starts | ✅ | ⏳ | With `camera.json`, a restart does not ask, also with access revoked |
| Access denied | ✅ | ⏳ | Access denied permanently: "Knoop by Knoop needs location access to show your position." with "Settings" ([screenshot](screenshots/android-denied.png)); "Settings" opens the app's settings |
| No position found | ✅ | ⏳ | Location turned off on the device: "Your position could not be found." after about 2 s (Android answers at once without a provider; the 15 s timeout covers a provider without a fix) |
| Not while navigating | ✅ | ⏳ | The button is shown while planning and hidden while navigating |
| First tap asks | ✅ | ⏳ | With access not yet granted, the button opens the system dialog (same request as on the first start) |

### iOS steps for the maintainer

1. Delete the app and install it again (or reset location permissions): on the first start, the system asks for location access with the new text ("…to show where you are on the map and, while navigating, …"); allow it: the map goes to your position with a blue dot.
2. Move the map away, tap the location button below the zoom buttons: back to your position; the location arrow in the status bar disappears again after a few seconds.
3. Restart the app: no question, the map opens where it was.
4. In Settings, set location access to "Never", tap the button: a message with "Settings" opens the app's settings.
5. Start navigating: the location button is gone, the recenter button works as before.
