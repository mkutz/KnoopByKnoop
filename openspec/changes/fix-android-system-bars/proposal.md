# Proposal

## Why

On Android, the status bar seems to be missing: its clock and icons are white and disappear on the light map. The search field reaches up under the status bar, so its upper part can't be tapped; those touches go to the system. The maintainer found both in exploratory testing of 0.1.2. Both have existed since Android started drawing apps behind the system bars (edge-to-edge, enforced from Android 15). The app draws behind them but never accounts for them.

## What Changes

- The status bar's and the navigation bar's icons are dark on the light map and light in dark mode, also after switching the appearance while the app is open.
- The controls over the map stay below the status bar and clear of the display cutout, in portrait and in landscape:
  - the search field and its hits
  - the network switch
  - the notes
  - the zoom buttons
  - the attribution
- The landscape side panel does the same.
- The map itself keeps drawing behind the bars, so it fills the screen.

Out of scope: iOS, which already keeps its controls in the safe area; hiding the bars (immersive mode); the bottom sheet in portrait, which already handles its insets.

## Capabilities

### New Capabilities

### Modified Capabilities
- `map-planning`: a new requirement that the system bars stay readable and the controls stay clear of them

## Impact

- Android only:
  - `MainActivity.kt`: edge-to-edge with system bar icons by appearance, again on a `uiMode` change
  - `PlannerScreen.kt`: insets for the controls over the map and for the landscape panel
  - `NetworkMap.kt`: insets for the zoom buttons and the attribution
- No new dependencies: `enableEdgeToEdge` and `WindowInsets` come with `androidx.activity` and Compose
