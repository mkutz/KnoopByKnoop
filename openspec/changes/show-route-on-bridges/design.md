# Design

## Context

- **Geometry file:** `NetworkGeometry` stores one simplified course per connection, followed by a section with the courses to each signpost. Readers since 0.7.0 read that section only if the file has more bytes, and ignore anything after it. Simplification (Douglas-Peucker, 5 m) runs in `Main.kt` after `NetworkExtraction` has built each course from the OSM nodes of the route's ways. Way tags other than ferry tags are not kept.
- **Drawing:** `MapFeatures` turns courses into GeoJSON for the sources `connections`, `route` and `route-ahead`. Both apps draw their own layers on top of the base map style:
  - online: OpenFreeMap Liberty, or the dark style `DarkStyle` derives from it with the same layer ids;
  - offline: a plain background style.
- **Base map tiles:** the `transportation` layer has `brunnel` and `layer`. Liberty draws bridges in 20 line layers whose ids start with `bridge_`, outlines included.
- **Prototype:** the throwaway build (scratchpad `bridge-proto`, Android only) copied those 20 layers at runtime at 60 % opacity and put them below `knots`. It worked on the Pixel with the light map.
- **Patents:** fading or otherwise restyling the route where it is hidden is close to Apple's US 12098930. Prominence that depends on the distance from the route is close to US 12281912. The decisions below avoid both.

## Goals / Non-Goals

**Goals:**
- Bridge stretches that survive simplification and reversal, and joining courses into a route or the line ahead.
- One layer order on both platforms, light and dark.
- No drawing change at all until geometry with bridge stretches is installed.

**Non-Goals:**
- Using OSM `layer` numbers.
- Changing the route's colour, width or opacity anywhere.
- Restyling the base map's own bridge layers. They stay as they are; the copies sit above our lines.

## Decisions

### Stretches as point index ranges, in two sections after the signpost courses
- **Format:** for each course with bridges, a list of (first point index, last point index) into its simplified points.
- **Connection section:** the delta-coded course index, the number of ranges, then the ranges as delta-coded varints.
- **Signpost section:** the course index and the signpost course's position in that course's list, then the ranges.
- **Compatibility:** both sections always follow the signpost section, so 0.7.0 to 0.9.0 read the file as before. The format version stays 1. Readers of this change read the sections when more bytes follow.
- **Alternatives:**
  - A flag per segment costs a bit for every segment of every course; the ranges are a few bytes per bridge.
  - A new format version would break released apps.
  - A separate file would need a new index entry and more download handling.

### Bridge boundaries are kept points of the simplification
- **In the pipeline:** `NetworkExtraction` keeps the `bridge` tag of the route's ways and marks, per course point, whether the segment after it is on a bridge. Consecutive bridge ways merge into one stretch.
- **Simplification:** before simplifying, the course is cut at every stretch boundary. Each piece is simplified on its own and the pieces are joined, so the boundary points survive exactly.
- **Alternative:** mapping boundaries to the nearest kept point after simplifying can move a boundary by up to a whole long segment, beyond 5 m.

### The core hands out lines already split, with a property
- `NetworkGeometry` carries the stretches through `courseOf` (reversal, joining courses, signpost courses).
- `MapFeatures` emits each course as alternating LineString features with `"bridge": true/false`. That applies to `connections`, `route` and the line ahead.
- Each app adds one more layer per line kind, with the same paint and a filter on `bridge`, above the bridge copies. The existing layers filter to `bridge: false`.
- `MapFeatures` exposes whether the installed geometry has any bridge stretches. Without them, no copies are added and the existing layers draw all lines, so the map looks as it does today.
- **Alternative:** computing occlusion in the app, by checking which route segments cross rendered bridge features. That needs feature queries per frame, gets confused by simplified tiles, and is what the patents describe.

### Bridge copies are made from the loaded style at runtime
- After a style has loaded, each app copies every line layer whose id starts with `bridge_`:
  - source, source layer, filter, zoom range, colour, width, gap width, offset, dash pattern, cap and join, all from the original;
  - opacity set to 0.6 everywhere.
- The copies go above the ridden track and below the bridge-stretch layers and `knots`.
- Copying the dark style's layers gives dark copies without a second list of colours.
- **Android:** `PropertyValue` with the expression or plain value, as in the prototype.
- **iOS:** the `MLNLineStyleLayer` NSExpression properties.
- **Alternatives:**
  - A hand-written bridge style in the app would drift from Liberty and from the dark style.
  - Changing the base style's own layer order is not possible: our sources are added after it.

### Final layer order (bottom to top)
1. Base map
2. `connections`, `marker`
3. `route`, `route-unreachable`, `route-direction`
4. `route-ahead`, `route-ahead-direction`
5. `ridden-track`
6. **Bridge copies**
7. **`connections-bridge`, `route-bridge`, `route-ahead-bridge`**
8. `knots`, `way-to-route`, `guided-way`, `my-position`, `next-knot`, `route-knots`, `position`, `position-arrow`

## Risks / Trade-offs

- **[Liberty renames its bridge layers]** → No copies, so the route is drawn as before. An app test against the bundled dark style (same ids) checks that bridge layers are found. The ride check covers the light style.
- **[A separately mapped path beside a bridge has no `bridge` tag]** → It is drawn below the copy. This was rare on the prototype; it's left to OSM data.
- **[Base map tiles and our data come from different weeks]** → A bridge stretch without a bridge in the tiles is just drawn as usual. A new bridge in the tiles without a stretch covers the route until the next weekly data.
- **[Direction arrows split at bridge ends]** → The arrows stay on the lower layers. Under a bridge they are covered like the line. Because the line is split, their spacing may restart at a stretch boundary, which is acceptable.
- **[Bridge over a bridge]** → A route's bridge stretch is drawn above all bridge copies, even under a higher bridge. This happens about 0.1 to 0.2 times per 100 km.
- **[Battery]** → 20 more line layers on tiles that are already loaded. Check on the Pixel while navigating, as for other map changes.

## Migration Plan

1. Ship pipeline and core first. Published geometry gains the sections; released apps ignore them.
2. Ship the apps. Until a geometry file with stretches is installed, they draw nothing new.
3. **Rollback:** an app without this change, or geometry without the sections, falls back to today's map.
