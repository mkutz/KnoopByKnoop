# Proposal

## Why

The network switch on the map shows only words, "Cycling" and "Hiking". A bike and a hiker can be recognized at a glance, which is what this control is for: seeing which network is on the map and changing it. The maintainer suggested this after the arrow icons on the new knots switch.

## What Changes

- **An icon next to each word on the network switch:** a bike for cycling, a hiker for hiking.
- **The words stay,** for clarity and for screen readers.
- **Both platforms use icons of their own icon family,** so they suit the app's other icons on each platform.
- **iOS:** the segmented `Picker` shows no icons next to words, as `add-new-knots-switch` found, so the switch moves to the small control made there. Its look changes slightly on iOS.

## Out of Scope

- **Icons without words:** this would save a little map, but the words are clearer.
- **Icons in other places** that name a network, such as the info sheet, the dialog for switching the network or the loading notes.
- **Different behavior of the switch:** switching with a route still asks first, and the switch is still hidden while navigating and in the same situations as today.
- **A change of the words** or their translations.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `map-planning`: a new requirement "Icons on the network switch".

## Impact

- **Android:** two vector drawables from Material Symbols, in the `FilterChip`s of `PlannerScreen`.
- **iOS:** `networkSwitch` in `PlannerView` uses a shared control, generalized from `NewKnotsChoices` in `PlannerActions`, with SF Symbols.
- **Core and data:** no change, and no data run.
- **Strings:** no new strings.
- **The map:** the switch gets wider and covers a little more of the map.
