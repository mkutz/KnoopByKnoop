## Context

- **Android:** `NetworkMap` draws one centred column of `SmallFloatingActionButton`s at the right: search, zoom +/−, position, nearest knots, and recenter while navigating. `PlannerScreen` uses a `BottomSheetScaffold`; the summary row holds More actions, Clear and an extended `NavigateButton`. `InfoButton` sits inside the network switch's card.
- **iOS:** `NetworkMapView` adds UIKit `UIButton`s in vertical `UIStackView`s; `PlannerView` knows the sheet's resting height (`restingSheetHeight`) and the live height (`sheet.live`). `floatingBackground` gives Liquid Glass on iOS 26.

## Goals / Non-Goals

**Goals:**
- One column of grouped buttons that never collides with the panel or Navigate.
- Navigate as an icon on both platforms, with the actions visible without a menu.

**Non-Goals:**
- Changing what any button does.

## Decisions

### The column follows the panel

The map stops placing its own buttons and gets a `bottomInset` (the panel's live top edge, plus the Navigate FAB on Android). The column is aligned to the bottom end above that inset, so it moves with the sheet.

- **Android:** read from `bottomSheetState.requireOffset()`, as the tap note does.
- **iOS:** read from `sheet.live`, not only from the resting height, so it follows the drag.

The column measures its height against the room between the top controls and the inset. It drops the zoom group first, then the position group.

*Alternative:* fixed positions with the panel overlapping. That was rejected, because it's the collision the maintainer found in the mockup.

### Groups

- **Android:** each group is a `Surface` card (16 dp corners, shadow) holding `IconButton`s, separated by a hairline.
- **iOS:** each group is a `UIVisualEffectView` with `UIGlassEffect` on iOS 26 (blur material before), with buttons inside a vertical stack, and a capsule mask. The nearest knots button keeps showing only when no knots are in view; the position group shrinks to one button then.

### Navigate

- **Android:** a `LargeFloatingActionButton`-sized FAB (navigation arrow icon, content description "Navigate"), drawn in the planner's root `Box` at the sheet's top edge, right aligned. Stop stays in the navigation header, where it already is a round icon button: moving it would change the header's measured heights.
- **iOS:** a 56 pt round `.borderedProminent` (`.glassProminent` on iOS 26) button in the header, where the capsule was.
- **Landscape:** the same round button at the end of the side panel's header on both.

### The action row

Below the knots strip: Back to start as a tonal button with icon and label (only if not a round trip), then Export GPX and Clear as tonal icon buttons. Everything that was in More actions is now here, and the menu goes.

### The gear

- **Android:** a 56 dp round surface button with a gear vector at the top right, aligned with the network switch card.
- **iOS:** a glass circle with `gearshape`.

Both have the content description and label "Settings".

### The note on the panel's edge

Mockup: the row "The note on the panel's edge, glass everywhere" of https://claude.ai/artifact/NPjcSYcmLEZyvwu3CFSjne.

The tap note becomes part of the stack on the panel's edge, bottom to top: panel, note, Navigate (Android), column. The inset the column reads is the panel's edge plus the note's measured height while it shows, plus the FAB on Android. The note no longer computes its own offset. When it goes, everything drops back without its own animation, like a sheet drag.

### Glass

- **iOS:** everything over the map goes through `floatingBackground`: the network switch already does, and now the gear, the button groups (UIKit: `UIGlassEffect` in a `UIVisualEffectView`), the note, the place card and the loading notes do too.
- **Android:** MapLibre draws into its own surface, so Compose can't blur what's behind it (`Modifier.blur` blurs the content, not the backdrop). One `MapGlass` surface, `surface` at 88 % opacity with a 1 dp white border at 60 % and a 6 dp shadow, is used for all of them. Contrast is checked against a light and a dark map.

## Risks / Trade-offs

- **Navigate loses its word** → The arrow and the Stop symmetry carry it. The spec keeps "Navigate" for screen readers, and Android shows it on long press.
- **The column jumping while the sheet animates** → Read the live offset, as the note does. No animation of its own.
- **Small phones with a high sheet** → The zoom group, then the position group drop out. Pinch zoom always works.
- **Translucent surfaces on Android lose contrast over busy map parts** → The opacity stays high (88 %), and the contrast scenario is measured on a light and a dark map.
