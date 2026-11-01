# Tasks

## 1. Android

- [x] 1.1 `NetworkMap.addLayers`: the track after the route direction arrows, opacity 1; the rider's dot radius 12 and the arrow 44 dp. On the emulator, a mocked ride along a route: a screenshot shows the ridden part blue with orange edges and the part ahead orange with arrows ("Riding along the route"), and the arrow and the dot at the new size ("Seeing the position at a glance"), in light and dark mode

## 2. iOS

- [x] 2.1 `NetworkMapView.addLayers` and `KnotBadges.riderArrow` the same way; the project builds, and a simulator screenshot shows the map while navigating with the new arrow; the ridden track on top, which needs fixes the simulator does not deliver precisely enough here, goes to the maintainer's steps in `manual-checks.md`

## 3. Wrap-up

- [x] 3.1 `./gradlew check` and `npx @fission-ai/openspec validate --all` pass; a CHANGELOG entry under Unreleased

## Workflow follow-up

- The maintainer checks both on a ride, on the Pixel and the iPhone, before `/opsx:archive show-rider-on-top`.
