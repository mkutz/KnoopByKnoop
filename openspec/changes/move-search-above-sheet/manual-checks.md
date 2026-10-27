# Manual checks

iOS is checked by the maintainer on an iPhone (⏳ until then). In the simulator (iPhone 18 Pro, iOS 27) during apply, portrait showed the field right above the smallest sheet with the attribution above it and Liquid Glass on the floating views, in light and dark mode. Typing, dragging and landscape cannot be driven there in this setup. Android is unchanged by this change (no Android files touched).

| Scenario | iOS | Notes |
|---|---|---|
| Field above the sheet on iOS | ⏳ | |
| Sheet pulled up on iOS | ⏳ | |
| Typing on iOS | ⏳ | |
| Back above the sheet on iOS | ⏳ | |
| Searching in landscape on iOS | ⏳ | |
| Station | ⏳ | |
| No hits | ⏳ | |
| Tapping a hit | ⏳ | |
| Removing the marker | ⏳ | |
| Airplane mode | ⏳ | |
| Liquid Glass readable over a busy map | ⏳ | |

## Found and fixed

- In dark mode, the glass over the always light map turned dark and made the field's placeholder hard to read; the views over the map now keep the light appearance (simulator, during apply).
