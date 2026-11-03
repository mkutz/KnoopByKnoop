# Manual checks

## Scenarios

Checked 2026-11-03 with the route 02, 28 at Siegburg and the installed cycling data. Android on the emulator, iOS in the simulator (iPhone 18 Pro), tapping knot 27. The route, camera, choice, text size, rotation and appearance of both were restored.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Adding a stop | ✅ | ✅ | 02, 27, 28: the route shows 02, 28, 27, 28 and 16 km, a there-and-back as before. `PlanningTest` |
| Extending the route | ✅ | ✅ | 02, 28, 27, 9.4 km ([Android](screenshots/android-new-knots.png), [iOS](screenshots/ios-new-knots.png)). `PlanningTest` |
| Looking around, A knot beside the route, First two taps | unit test | unit test | `PlanningTest` |
| Round trip on End | ✅ | ✅ | 02, 28, 27, 02: the route shows 02, 28, 27, 28, 02, 19 km. `PlanningTest` |
| A knot that cannot be reached | unit test | unit test | `PlanningTest` |
| Default | ✅ | ✅ | Stop, with the choice removed first |
| Remembered | ✅ | ✅ | After restarting the app with End chosen. Across networks: same setting by construction, not tried |
| Switching changes nothing yet | ✅ | not tried | The route stayed 02, 28 |
| In the smallest panel | ✅ at 360 dp | ⚠️ at 402 pt | The whole row shows on both. The iPhone 18 Pro is wider than 360 |
| In landscape | ✅ | ⚠️ not tried | [Android](screenshots/android-new-knots-landscape.png). `simctl` cannot rotate; iOS uses the same view |
| While navigating | ✅ | ✅ | No switch |
| Larger text | ✅ German 130% at 360 dp | ✅ German, accessibility medium | The label goes above the choices, also in German at normal size on both |
| Screen reader | ⚠️ UI tree only | ⚠️ not tried | Android: the label as text and the choices as checkable. TalkBack and VoiceOver not run |
| Readable in both appearances | ⚠️ by eye | ⚠️ by eye | The platforms' own controls in light and dark. Contrast not measured |

Labels checked: English, German, Dutch and French on Android at 360 dp, and English, German and French on iOS.

### Steps for the maintainer

1. On the Pixel and the iPhone: plan 12 to 45 on Stop, and tap a knot beside it. It works as before.
2. Switch to End and tap several knots one after the other. Each becomes the new end, and the old one a stop.
3. On End, use "Back to start", then tap a knot. The route stays a round trip.
4. iPhone: rotate to landscape, and check the switch at the top of the side panel.
5. With TalkBack and VoiceOver: "New knots", both choices and which one is selected are announced.
6. Judge: is the remembered choice right, or does it catch you out? Is the map lost to the row worth it?
