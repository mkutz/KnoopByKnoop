# Manual checks

## Scenarios

Checked 2026-10-28 by the maintainer on the iPhone with the on-screen keyboard, searching for "Amsterdam".

| Scenario | iPhone | Notes |
|---|---|---|
| Typing in landscape on iOS | ✅ | The field is fully visible right above the keyboard, the network switch and the attribution are hidden, and two hits fit completely, where there was about one and a half before ([screenshot](screenshots/ios-landscape-typing.png)) |
| Done typing in landscape on iOS | ✅ | Checked with the keyboard's search key, which dismisses the keyboard the same way as tapping a hit or clearing: the switch and the attribution are back, and the hits stay above the field until one is chosen ([screenshot](screenshots/ios-landscape-after-search-key.png)) |
| Map controls in landscape | ✅ | Without the keyboard, switch, info button, zoom buttons and attribution are on the map (same screenshot) |
| Attribution | ✅ | Shown in landscape without the keyboard and in portrait while typing |
| Typing on iOS | ✅ | Portrait: the field is right above the keyboard, with six hits above it ([screenshot](screenshots/ios-portrait-typing.png)) |
| Back above the sheet on iOS | ✅ | Portrait, after the search key: the field is right above the sheet again, with the hits above it ([screenshot](screenshots/ios-portrait-after-search-key.png)) |

## Observations

- The hits get two full entries in landscape, not the three the proposal estimated: the field and the top padding take more than estimated. The maintainer found it "much better".
