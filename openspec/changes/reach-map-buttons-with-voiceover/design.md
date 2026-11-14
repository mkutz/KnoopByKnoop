# Design

## Context

`NetworkMapView` is a `UIViewRepresentable` whose `makeUIView` returns the `MLNMapView`. The button column (`UIStackView` of `MapButtonGroup`s) is added with `mapView.addSubview(column)` and pinned to the map view's edges.

`MLNMapView` implements the accessibility container methods (`accessibilityElementCount`, `accessibilityElement(at:)`) for its own elements: annotations, the compass and the attribution button. UIKit then doesn't look at its other subviews, so the column is invisible to VoiceOver and XCUITest.

## Goals / Non-Goals

**Goals:** VoiceOver reaches the map's buttons, in the order top to bottom; the layout stays as it is.

**Non-Goals:** knots and places as accessibility elements.

## Decisions

### A container with the map and the column as siblings

`makeUIView` returns a plain `UIView` holding:
- the `MLNMapView`, pinned to all edges
- the button column above it

The column's constraints point at the container instead of the map view; the edges are the same. `updateUIView` gets the container and reaches the map view through `context.coordinator.mapView`, which the coordinator already keeps or gets.

The container is no accessibility element itself. Its order is the map first, then the column, so VoiceOver reads the map, then search, position, nearest knots, zoom and Recenter.

**Rejected:** subclassing `MLNMapView` to append the column to its accessibility elements. It depends on MapLibre's private container implementation, which can change with any MapLibre update.

## Risks / Trade-offs

- **Gestures:** a container view between SwiftUI and the map could swallow touches. → The container doesn't handle touches; the map keeps its gesture recognisers. The column's buttons still get their taps first, as now.
- **Other overlays added to the map view** (spinners in buttons, etc.) move with their buttons; nothing else is added to the map view by the app.
