# Manual checks

## Scenarios

Checked 2026-11-03 with the route 02, 28 at Siegburg and the installed cycling data. Android on the emulator, iOS in the simulator (iPhone 18 Pro), tapping knot 27. The route, camera, choice, text size, rotation and appearance of both were restored.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Adding a stop | ✅ | ✅ | On Between, 02, 27, 28: the route shows 02, 28, 27, 28, a there-and-back as before. `PlanningTest` |
| Extending the route | ✅ | ✅ | On At end, 02, 28, 27, 9.4 km ([Android](screenshots/android-new-knots.png), [iOS](screenshots/ios-new-knots.png)). `PlanningTest` |
| Looking around, A knot beside the route, First two taps | unit test | unit test | `PlanningTest` |
| Round trip at the end | ✅ | not retried | 02, 28, 27, 02: the route shows 02, 28, 27, 28, 02. `PlanningTest` |
| A knot that cannot be reached | unit test | unit test | `PlanningTest` |
| Default | ✅ | ✅ | Between, with the choice removed first |
| Remembered | ✅ | ✅ | After restarting the app with At end chosen. Across networks: same setting by construction, not tried |
| Switching changes nothing yet | ✅ | ⚠️ not tried | The route stayed 02, 28. iOS: the check sets the choice in the model, so **tapping the new control has not been tried** |
| The icons | ✅ | ✅ | An arrow between two knots and an arrow past the last one, in the color of the word |
| In the smallest panel | ✅ at 360 dp | ⚠️ at 402 pt | The whole row shows on both. The iPhone 18 Pro is wider than 360 |
| In landscape | ✅ | ⚠️ not tried | [Android](screenshots/android-new-knots-landscape.png). `simctl` cannot rotate; iOS uses the same view |
| While navigating | ✅ | ✅ | No switch |
| Larger text | ✅ German 130% at 360 dp | ✅ German, accessibility medium | The label goes above the choices, also in German and French at normal size. The icons keep their size on both |
| Screen reader | ⚠️ UI tree only | ⚠️ not tried | Android: the label as text and the choices by their words. iOS is now a control of its own, with the selected trait set in code. TalkBack and VoiceOver not run |
| Readable in both appearances | ✅ measured | ✅ measured | Words and icons have one color, the lowest contrast being 5.7:1 (iOS dark, selected). The others are 9.1 to 21:1 |

Labels checked: English, German, Dutch and French on Android at 360 dp, and English, German and French on iOS. Found and fixed while checking: on Android, the icon overlapped the word in the button's icon slot, and "Entre deux" was cut off; on iOS, the segmented `Picker` showed no icons.

### Steps for the maintainer

1. On the Pixel and the iPhone: plan 12 to 45 on Between, and tap a knot beside it. It works as before.
2. Switch to At end and tap several knots one after the other. Each becomes the new end, and the old one a stop.
3. On At end, use "Back to start", then tap a knot. The route stays a round trip.
4. iPhone: rotate to landscape, and check the switch at the top of the side panel.
5. With TalkBack and VoiceOver: "Add knots", both words and which one is selected are announced, and not the icons.
6. iPhone: tap both choices of the new control. Tapping has only been tried on Android.
7. Judge: do the icons and words explain themselves? Is the remembered choice right, or does it catch you out? Is the map lost to the row worth it?
