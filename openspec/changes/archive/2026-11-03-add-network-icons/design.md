# Design

## Context

- **Android:** the switch is a row of two `FilterChip`s and the info button in `PlannerScreen`, over the map. The chips have no leading icon today. The app's other icons are vector drawables copied from Material Symbols (rounded), with the source named in a comment, and not a dependency on an icon library.
- **iOS:** the switch is a segmented `Picker` with `frame(width: 200)` and the info button in `PlannerView.networkSwitch`. `networkBinding` asks first when a route exists. The minimum version is iOS 17. The app's other icons are SF Symbols.
- **What `add-new-knots-switch` found:** on iOS, the segmented `Picker` shows only the word of a `Label`, so that change built `NewKnotsChoices`, a small control with an icon and a word on each choice, the selected trait for VoiceOver, and a measured contrast.

## Goals / Non-Goals

**Goals:**
- The same control and the same behavior as today on Android, with an icon added.
- One control for icons with words on iOS, so that the two switches look the same.

**Non-Goals:**
- Making the switch look alike on both platforms, which already differ.

## Decisions

**Icons of each platform's family.**
- Android: Material Symbols "directions_bike" and "hiking" (rounded, Apache 2.0), as vector drawables like the other icons, in the chips' `leadingIcon`. It stays at the default 18 dp, which the chip's slot is made for, so the problem of the new knots switch, with its wide icon in a small slot, does not come up.
- iOS: the SF Symbols `bicycle` and `figure.hiking`. They are templates, so they follow the color of the word, and they scale with the text.
- Alternative, one drawn pair on both platforms: this would look the same everywhere, but it would be two more icons to draw and keep, and they would fit neither platform as well.

**On iOS, a shared control instead of the `Picker`.** `NewKnotsChoices` becomes a generic control over a selected value and a list of choices, each with a word and an icon, and the new knots switch and the network switch both use it. `networkBinding` stays, so asking first works as today.
- Alternative, an image of icon and word inside the `Picker`: this keeps the native look, but the word would no longer be text. It would not follow Dynamic Type by itself, and it would need its own label for VoiceOver.
- Alternative, keeping the `Picker` on iOS without icons: the platforms would differ for no reason the user could see.

**The accessibility label "Knot network" stays** on the iOS control, as on the `Picker`, and the icons are hidden from VoiceOver. On Android, the icon has no content description, as the chip's word is what TalkBack announces.

**The fixed width on iOS goes.** `frame(width: 200)` leaves too little room for icons with "Radfahren" or "Randonnée", so the control takes the width of its content. The longest case is checked on the phones.

**If the row is too wide on a narrow phone or at large text:** measured, both platforms fit at 360 dp and 130% in German and French. On iOS, where the fixed width is gone, the row stops growing at the text size xxxLarge, as it was 385 of 402 points wide at accessibility medium and would have pushed the info button off a narrower phone. On Android, at 150% and 200% in German at 360 dp, the info button was squeezed and "Wandern" broke over two lines, so the row scrolls there and the words stay on one line.

**On Android, the icon has the color of the word,** set on the chips. By default, an unselected chip's icon is the primary color, which is green here, and green means start in this app.

## Risks / Trade-offs

- **Less map:** the switch gets wider, by about two icons, and covers a little more of the top of the map. It is judged on the phones.
- **A changed look on iOS:** the native control goes, and it had a sliding animation between choices. The new control has none. If this feels worse, an animation can be added to the shared control.
- **`add-new-knots-switch` is not archived yet,** and this change edits its `NewKnotsChoices`. The new knots switch is therefore checked again here, to see that it looks and works as before.
- **`figure.hiking` not available as assumed:** a missing symbol shows no image and no error, so the check looks at screenshots.
