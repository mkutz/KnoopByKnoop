# Design

## Context

How badges are drawn today:
- **Android:** `KnotBadges.kt` keys colours by `KnotRole` (start green, stop orange, passed white with an orange ring, end dark, round trip half green and half dark, not on the route white with a green ring). It draws one bitmap per map image name: `knot-<ref>` or `route-<role>-<ref>`.
- **iOS:** `KnotBadges.swift` does the same. `apps/ios/Shared/Badges.swift` holds the `BadgeStyle` shared with the Live Activity widget.

Red (`#D32F2F`, `#D93025`, `colorScheme.error`) marks these problems:
- on the map: the way-to-route line, the guided way, the unreachable leg and the search-hit marker
- in the panel: the off-route header text and the off-route dot on the strip
- in the ferry badges: the ring of a ferry with a warning

The pipeline reads relations, then ways, then nodes from the Geofabrik extracts of the Netherlands, Belgium and North Rhine-Westphalia (`update-network-data.yml`). The graph file is at format version 2. Readers stop after the ferries, so sections can be appended after the merged knots without a new version.

## Goals / Non-Goals

**Goals:**
- One plate table in the core, so both apps draw the same colours.
- No more map images than today for knots that are not on the route.

**Non-Goals:**
- Changing the Live Activity, Dynamic Island or Android notification badges. They keep today's style until `sign-like-lock-screen`.
- Any change to the header layout beyond its off-route colours.

## Decisions

### Sign region from administrative boundaries, not from the extract a knot came from

**Choice:** the pipeline collects boundary relations:
- `boundary=administrative` with `admin_level=2` and `ISO3166-1` `NL` or `BE`
- `admin_level=4` with `ISO3166-2=DE-NW`

It builds their outer rings from the member ways and nodes, and tests each cycling knot's own position against them with a bounding box first, then ray casting. A knot inside NL is `NETHERLANDS`, inside BE is `BELGIUM`, inside DE-NW is `NORTH_RHINE_WESTPHALIA`, and anything else gets no region. A ring that does not close fails the build with the boundary's name.

**Why:** Geofabrik extracts overlap by a buffer. A knot near a border is in two extracts, so "the extract it came from" is ambiguous. Each extract contains its own region's boundary whole, because the clipping polygon is that region plus a buffer. Only OSM data is used, as the project requires.

**Alternatives:**
- Use the extract file name: wrong near borders, and Rhineland-Palatinate knots in the NRW buffer would come out red.
- Ship a Natural Earth border file: not OSM, and too coarse at the border.
- Read the `network` tag of the route relations: there is no reliable tag for sign style (see the 2026-11-03 findings, no `colour` or `osmc:symbol` on 217 relations).

### `SignRegion` on `Knot`, as a section appended to the graph file

**Core changes:**
- `enum class SignRegion { NETHERLANDS, BELGIUM, NORTH_RHINE_WESTPHALIA }`
- `Knot.signRegion: SignRegion? = null`
- `GraphFile` appends, after the merged knots: a count, then for each knot with a region its index and a one-byte code. The format version stays 2.

Older apps stop reading before this section. Older files end before it, and the reader treats a missing section as "no regions". Hiking graphs write an empty section. A merged knot takes the region of its own position.

**Alternative:** a format version 3, which would make older apps reject the new files. The appended-section pattern exists for exactly this case.

### Plate table in the core

**Choice:** `KnotPlate.of(region: SignRegion?): KnotPlate(colour: Long /* ARGB */, innerRing: Boolean)`:
- NL `#12663A`, no inner ring
- BE `#1A5FB4`, inner ring
- NRW `#C8102E`, inner ring
- `null` the app green `#2E9E5B`, inner ring

The apps turn it into their own colour types. Fill by role is drawn in the apps:
- **Waypoint:** filled plate and a white number, plus a white inner ring when `innerRing`.
- **Passed knot:** white disc with one ring and the number in the plate colour.
- **Not on the route:** grey ring and number (`#9AA19C` / `#6B736D`) on white, or on `#2A2C2A` in dark mode.

The colours are values from the maintainer's photos and the mockups. Keeping them in one place makes later corrections a one-line change with a test.

### Map image names carry the plate only where needed

- Route markers become `route-<role>-<plate>-<ref>`, where `<plate>` is `nl`, `be`, `nrw` or `none`.
- `MapFeatures` adds a `plate` property to the route marker points, and the symbol layers build the name from `role`, `plate` and `ref`.
- Knots not on the route stay `knot-<ref>`, since they are grey everywhere. The cached knots layer and its image count don't change, only its colours. Bump the layer version in the cached knots file name, so old green images are not reused.

### Corner symbols drawn into the badge image

- **Look:** a dark disc (`#1B1F1C`) about 45 % of the badge diameter, at the top right, with a white glyph: ▶ for start, a flag for stop, ■ for end, a circular arrow for round trip.
- **Canvas:** the image grows by the overhang, and the anchor stays at the plate's centre, so the plate sits exactly where today's badge does.
- **Route points:** they keep their flag inside the plate (from the open change `add-route-points`) and take the app green plate, since they have no region. A route point stop therefore shows a flag inside and a flag at the corner. That is accepted, because the corner says the role and the inside says it's a route point.

### Red replaced by dark, never by another hue

**Map:**
- the way-to-route dashed line, the guided way and the unreachable leg: `#263238` with a white casing on the light map, `#ECEFF1` with a dark casing on the dark map
- the search-hit marker: a `#263238` circle with a white stroke

**Panel:**
- **Off route:** the header area takes `#1B1F1C` with white text and arrow in light mode, and `#E6E8E6` with dark text in dark mode. Knot badges in it keep their plates.
- **Strip, off route:** the rider dot becomes a white disc with a dark ring and ✕.
- **Strip, ferry warning:** a dark clock badge at the top-right corner of the ferry mark, with the arrival in bold.
- Popover and ferry detail texts stay red, since they are words, not marks.

**Why:** the maintainer chose the inverted header and the clock badge on 2026-11-07 (Warnings board). Amber alone was judged not recognisable as a problem by newcomers.

### Shared iOS `BadgeStyle` split

`apps/ios/Shared/Badges.swift` keeps today's style for the widget. The app's map and panel use a new `PlateBadge` in `KnotBadges.swift`, so the Live Activity is unchanged until `sign-like-lock-screen`.

## Risks / Trade-offs

- **[Regional sign styles]** The maintainer remembers Dutch-style signs in Belgium near the border, and other German states differ. → Knots outside the three regions keep the app's green, and BE stays one colour. A sub-region table can follow when photos confirm it.
- **[Wrong colour values]** The colours are taken from photos. → One table, one test per region, and a check on the next ride.
- **[Users used to today's colours]** The green start, orange stops and dark end disappear. → The corner symbols keep the roles readable, and the CHANGELOG entry explains it.
- **[Boundary assembly cost]** The NL border has many nodes. → Bounding box first, then ray casting on cached rings. The tests measure the Köln-sized fixture, and the CI run time is compared before and after.
- **[Red base map features]** Red roads or POIs on the base map may still sit near red knots. → Accepted. Badges have the white ring and are drawn on top.

## Migration Plan

1. Ship the app with the reader that accepts the new section. Data without regions shows every route knot in the app's green, with the new fill scheme.
2. The next "Update Network Data" run writes regions. Installed apps pick them up with the next data download.
3. Rollback: the pipeline can stop writing the section. Apps then show the app's green again, with no app release needed.

## Open Questions

- The exact grey for knots that are not on the route on the dark map may need tuning on the device. It can change without changing the specs.
