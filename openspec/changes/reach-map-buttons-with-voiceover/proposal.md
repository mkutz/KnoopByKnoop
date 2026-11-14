# Proposal

## Why

On iOS, VoiceOver cannot reach the map's own buttons: search, My position, nearest knots, zoom in and out, and Recenter. They are subviews of the MapLibre map view, which acts as its own accessibility container and lists only its Compass and "About this map".

This was found on 2026-11-14, when the store screenshot UI test (`automate-store-screenshots`, paused for this) couldn't find the search button. A dump of the accessibility tree confirmed it. The test meets the same wall a VoiceOver user does.

## What Changes

- **iOS:** the map's button column moves out of the MapLibre map view, into a container view that holds the map and the column side by side. The buttons look and sit where they are now, and VoiceOver reaches them with their labels.
- **Android:** no change. TalkBack already reaches the map's buttons; the `uiautomator` dump lists them with their localised descriptions.

## Out of Scope

- **Other VoiceOver issues** seen in the tree, e.g. the "Share" button nested in a second unlabelled button, and the route-download symbols read as "Three Connected Points With A Dotted Triangular Path". These are noted for a later change.
- **The map's knots and places** as accessibility elements.
- **Resuming `automate-store-screenshots`.** It continues after this change is archived.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`: a new requirement that a screen reader reaches every button on the map, read with its label.

## Impact

- **iOS:** `apps/ios/KnoopByKnoop/NetworkMapView.swift`. `makeUIView` returns a container holding the `MLNMapView` and the button column. `updateUIView` reaches the map view through the coordinator.
- **iOS UI test target:** a test asserting the buttons are in the accessibility tree.
- **Android, core:** none.
