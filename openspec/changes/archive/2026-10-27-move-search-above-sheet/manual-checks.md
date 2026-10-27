# Manual checks

iOS was checked by the maintainer on an iPhone on 2026-10-27, after the fixes listed below: "Works on iPhone now". In the simulator (iPhone 18 Pro, iOS 27) during apply, portrait showed the field right above the smallest sheet with the attribution above it and Liquid Glass on the floating views, in light and dark mode. Typing, dragging and landscape cannot be driven there in this setup. Android is unchanged by this change (no Android files touched).

| Scenario | iOS | Notes |
|---|---|---|
| Field above the sheet on iOS | ✅ | |
| Sheet pulled up on iOS | ✅ | |
| Typing on iOS | ✅ | |
| Back above the sheet on iOS | ✅ | |
| Searching in landscape on iOS | ✅ | |
| Station | ✅ | |
| No hits | ✅ | |
| Tapping a hit | ✅ | |
| Removing the marker | ✅ | |
| Airplane mode | ✅ | |
| Liquid Glass readable over a busy map | ✅ | |

## Found and fixed

- In dark mode, the glass over the always light map turned dark and made the field's placeholder hard to read; the views over the map now keep the light appearance (simulator, during apply).
- On the iPhone, the field stayed high above the sheet after a search, and the top of the sheet's content was cut off: the keyboard was counted into the sheet's height. The sheet's content now ignores the keyboard (iPhone, first round).
- The hits card always took its full 320 points, even for three hits; it now fits the hits and scrolls only when the room is smaller (iPhone, first round).
- The hits came back after a hit was chosen, because the field passes its text again when it loses the focus, which started a new search; a search now only starts when the text changed. Checked in the simulator with a temporary scripted search: the hits are gone, the marker is shown and the field is right above the smallest sheet.
