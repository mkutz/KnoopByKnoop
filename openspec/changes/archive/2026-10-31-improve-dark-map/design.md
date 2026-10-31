# Design

## Context

See `proposal.md` for why. Both apps load OpenFreeMap's styles by URL: `liberty` in light, `dark` in dark (`NetworkMap.kt` `BASE_MAP_STYLE`/`DARK_BASE_MAP_STYLE`, `NetworkMapView.swift` `baseMapStyle(dark:)`), with an offline fallback style of a plain background each. The styles share the `openmaptiles` vector tiles, the `ne2_shaded` raster and the sprite and glyphs at `tiles.openfreemap.org`. The apps follow the system appearance (`isSystemInDarkTheme()`, `colorScheme`), and on a switch reload the base map without moving the camera (`map-planning` "Light and dark appearance").

## Goals / Non-Goals

**Goals:**
- A dark map with every detail of the light one, readable for finding the way.
- The appearance choosable, without breaking "all light or all dark".

**Non-Goals:**
- Hosting styles, tiles or sprites ourselves.

## Decisions

### A generated dark variant of liberty
`pipeline` gets a `DarkStyle` generator (a `main` and a Gradle task `:pipeline:darkStyle`) that reads OpenFreeMap's liberty style JSON and writes `liberty-dark.json`: same sources, layers, filters and layout, with every color in `paint` (plain values, `stops` and expressions) mapped to a dark counterpart:
- in HSL, the lightness is inverted into a dark range (light land and backgrounds become dark grey-green, dark text becomes light), hue kept, saturation slightly lowered
- water and waterways keep their blue at a medium-dark lightness, so they stay recognizably blue
- parks, woods and grass keep their green, darker
- text halos become dark, label text light
- the `ne2_shaded` raster gets a lower `raster-brightness-max`; POI icons from the shared sprite keep their colors at a slightly lower opacity
The file is checked in under `map-styles/` and copied into the Android assets and the iOS bundle by the builds; the generator is run by hand when OpenFreeMap's liberty changes. Tests check the mapping on liberty's actual colors (water blue and dark, background dark, labels light, halos dark) and that every layer of liberty is kept.
*Alternative:* OpenFreeMap's "dark" with fixes. It lacks most of liberty's layers; adding them by hand is more work and drifts from liberty.
*Alternative:* inverting the map with a color filter on the view. It turns water orange and inverts the knots and the route too.

### Loading the shipped style
Android: `Style.Builder().fromJson(assets "liberty-dark.json")` in place of the dark URL; iOS: `MLNMapView.styleURL` with the bundle's file URL. The light map keeps loading liberty by URL. The fallback to the offline style, the reload on reconnect and the camera keeping on a switch stay as they are.

### The appearance setting
Stored per device (Android `SharedPreferences`, iOS `UserDefaults`, key `appearance`: `system`, `light`, `dark`). Android: the theme, the system bar style and `NetworkMap`'s `dark` take `appearance` resolved against `isSystemInDarkTheme()` instead of the system alone; `AppCompatDelegate` is not used, so the activity is not recreated. iOS: `.preferredColorScheme(nil / .light / .dark)` on the root view, which also covers the sheets and the status bar. The info sheet shows a segmented choice "Appearance: System | Light | Dark".

## Risks / Trade-offs

- [OpenFreeMap changes liberty's layers or sprite version] → The shipped dark style keeps working as long as its sprite and tiles stay available; regenerating is one command, and the generator's tests fail on a liberty they cannot map.
- [Generated colors look off somewhere] → Checked on the emulator in towns, the countryside, along rivers and in woods, light and dark side by side; single colors can be overridden per layer in the generator.
- [POI icons are bright on the dark map] → Slightly lower opacity; they are small.
