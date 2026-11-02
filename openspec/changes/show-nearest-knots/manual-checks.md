# Manual checks

## Scenarios

Android checked 2026-11-02 on the emulator (`Medium_Phone_API_37.0`) with the debug build and the published data. iOS checked the same day in the simulator (iPhone 18 Pro), the tap made by a temporary launch flag, removed afterwards. The map was opened over Berlin through the saved map position, positions mocked. Everything was restored.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| No knots in the area | ✅ | ✅ | [Android](screenshots/android-zoomed-rider-route.png), [iOS](screenshots/ios.png), third. `PlanningTest` |
| Zoomed out too far | ✅ | ⏳ | First shot of the Android strip, with the zoom button. `PlanningTest` |
| Knots in view | ✅ | ✅ | No button, "Tap a knot to set the start". `PlanningTest` |
| With waypoints set | ✅ | ⏳ | A saved route with the map over Berlin: the button, and the route's summary in the panel (third shot) |
| From the rider's position | ✅ | ✅ | Rider east of Siegburg: the dot and knot 11 in view, second shot on Android and on iOS |
| Without location access | ✅ | ✅ | No prompt; from Berlin the map goes to the network near Bückeburg |
| No position found | ⏳ | ⏳ | Not run, needs a phone without a fix. Same fallback as above in the code |
| Far from the network | ✅ | ✅ | Rider in Berlin: the knot at the zoom where knots show, without the rider |
| Other network | ✅ partly | ⏳ | Hiking over Berlin: the button, the spinner, then "Tap a knot to set the start". Which knot is covered by `PlanningTest`, and each network has its own locator |
| Not while navigating | ✅ | ⏳ | Zoomed out to the whole of Europe while navigating: no button ([screenshot](screenshots/android-navigating-hiking.png)). The same check in the code on iOS |
| Nearest knots with a screen reader | ✅ | ⏳ | Android's description "Show the nearest knots" read from `uiautomator`. iOS sets the same text as its accessibility label |

Also checked on Android: German at the largest font in dark mode ([screenshot](screenshots/android-german-large-dark.png)): the text and the button take their own lines. iOS in light and dark.

### Steps for the maintainer

1. **Both phones:** search for a place far from the network, e.g. Berlin, or zoom far out: the button appears below the to-me button, and the panel says "No knots in view". Tap either: you and the nearest knot are in view, or the knot alone if it is far.
2. **iPhone, screen reader:** VoiceOver reads "Show the nearest knots" on the button.
3. **iPhone:** zoom far out while navigating: no button.
4. **German and Dutch:** do the texts read well to you?
