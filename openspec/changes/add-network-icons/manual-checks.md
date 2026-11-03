# Manual checks

## Scenarios

Checked 2026-11-03 with the route 02, 28 at Siegburg and the installed cycling data. Android on the emulator, iOS in the simulator (iPhone 18 Pro). The route, camera, language, text size, rotation and appearance of both were restored.

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| The icons | ✅ | ✅ | A bike and a hiker ([Android](screenshots/android-network-icons.png), [iOS](screenshots/ios-network-icons.png)). In the color of the word, after setting this on the Android chips, whose icons were green by default |
| Selected network | ✅ | ⚠️ by launch flag | Android: tapped Hiking without a route. iOS: a temporary flag switched the network in the model, so **tapping has not been tried** |
| Switching with a route | ✅ | ⚠️ not tried | Android: asks first, and Cycling stays selected after Cancel. iOS: `networkBinding` is unchanged, but this needs a tap |
| On a narrow phone | ✅ at 360 dp | ⚠️ at 402 pt | One row in all four languages. There was no narrower iPhone to check |
| Larger text | ✅ 130%, German and French | ✅ accessibility medium, and again at the largest size | iOS stops growing at xxxLarge, so the row fits at the largest size in German |
| Very large text | ✅ German at 200% | ✅ German at the largest size | [Android](screenshots/android-network-icons-200.png): the row scrolls to the info button, and the words stay on one line. iOS does not need to scroll on this phone |
| In landscape | ✅ before the color fix | ⚠️ not tried | Android: the layout did not change after it. `simctl` cannot rotate |
| Screen reader | ⚠️ UI tree only | ⚠️ not tried | Android: the words as text, a checked state, and no text on the icons. iOS: the icons are hidden from VoiceOver and the selected trait is set in code. TalkBack and VoiceOver not run |
| Readable in both appearances | ✅ measured | ✅ measured | The lowest contrast is 8.3:1 on Android and 5.7:1 on iOS, both in dark ([Android dark](screenshots/android-network-icons-dark.png)) |

The new knots switch on iOS, which now shares its control with this switch, looks as before in light and dark. Tapping it has still not been tried.

### Steps for the maintainer

1. iPhone: tap both choices of the network switch and of the new knots switch. Neither has been tapped yet.
2. On both phones: with a route, tap the other network. The app asks first, and Cancel keeps the selection.
3. iPhone: rotate to landscape, and check that the switch shows the icons and covers nothing.
4. With TalkBack and VoiceOver: both words and which one is selected are announced, and not the icons.
5. Judge: is the map lost to the wider switch worth it? Does the iOS control feel worse without the native sliding animation?
