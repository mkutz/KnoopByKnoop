# Design

## Context

See `proposal.md` for why. `MainActivity` sets its Compose content without `enableEdgeToEdge()`. With target SDK 37, Android draws the app behind the status and navigation bar anyway, but the bar icons follow the window theme: `Theme.Material.Light` has light icons, meant for its dark-green status bar that is no longer drawn. Nothing in the UI uses `WindowInsets`:
- the search field and network switch sit at `padding(12.dp)` from the top
- the notes sit at 140 dp
- the zoom buttons are centered at the end, the attribution at the bottom end
- in landscape, the side panel starts 16 dp from the top

In portrait, the `BottomSheetScaffold` sheet already keeps clear of the navigation bar. The activity handles `uiMode` changes itself (`follow-system-appearance`), so it is not recreated when the appearance changes.

## Goals / Non-Goals

**Goals:**
- Readable bar icons in both appearances, including after a switch.
- Every control fully tappable, in portrait and landscape, with gesture and three-button navigation.

**Non-Goals:**
- Changes to the iOS app.
- Colored or translucent bar backgrounds: the map shows through, as in other map apps.

## Decisions

### `enableEdgeToEdge` with `SystemBarStyle.auto`, again on `uiMode` changes
`ComponentActivity.enableEdgeToEdge()` makes the bars transparent and sets their icons dark or light from the current appearance. Because the activity is not recreated on a `uiMode` change, it is called again in `onConfigurationChanged`.
*Alternative:* setting `windowLightStatusBar` in the light and night themes. That would cover the start, but not a switch while the app is open, which keeps the old window theme.

### Insets on the overlays, not on the map
The map keeps `fillMaxSize` behind the bars. The overlays get the insets:
- **Controls at the top** (search field, network switch, notes): `windowInsetsPadding(WindowInsets.safeDrawing.only(Top + Horizontal))` on their column, in addition to the existing 12 dp. The notes move down by the same top inset.
- **Zoom buttons and attribution:** `safeDrawing` horizontal insets, and the bottom inset in landscape.
- **Landscape side panel:** `safeDrawing` insets for top, end and bottom.

`safeDrawing` combines the system bars, the display cutout and the keyboard area. The keyboard part only matters at the bottom, which the top controls don't use.
*Alternative:* `statusBarsPadding()` only. It would miss the cutout and the navigation bar on the side in landscape with three-button navigation.

### Checked on the emulator
There are no UI tests in the app, so the scenarios are checked on the emulator, like in earlier changes, with screenshots:
- gesture and three-button navigation
- a display cutout in landscape (developer option "Display cutout": corner or punch hole)
- tapping near the search field's upper edge with `adb shell input tap`

## Risks / Trade-offs

- [The top controls move down by the status bar's height (about 24–48 dp)] → They no longer overlap the bar. The map area below them shrinks by the same amount, which matches other map apps.
- [Different devices place cutouts differently] → `safeDrawing` follows what each device reports; the emulator's cutout options cover the usual shapes.
