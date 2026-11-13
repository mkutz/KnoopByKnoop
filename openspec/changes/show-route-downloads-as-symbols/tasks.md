## 1. Android

- [x] 1.1 `DownloadStatus` chip composable: subject drawable, corner mark (progress, check, warning), accessibility label from the existing strings, live region, tooltip on tap; drawables for the map and the network.
- [x] 1.2 `RouteActions`: chips at the end of the row (map, then ways when `pathData` is set), row shown for every complete route; Back to start icon-only when the row doesn't fit 360 dp. Remove `PathDataNote` and `BaseMapNote`.

## 2. iOS

- [x] 2.1 The same chip in `PlannerView` with SF Symbols, `.accessibilityLabel`, a popover on tap; `routeActions` with the chips, `ViewThatFits` for Back to start without its label; remove `pathDataNote` and `baseMapNote`.

## 3. Checks

- [x] 3.1 `openspec/checked-by-hand.md` entries for the new scenarios, CHANGELOG line; `./gradlew check`, validate, `checkScenarios -Pchange=show-route-downloads-as-symbols`.
- [ ] 3.2 On the emulator at 360 dp and in the simulator: loading, loaded and failed (airplane mode), cycling and hiking, TalkBack and VoiceOver, dark mode, the sheet keeps its height. Install on both phones.
