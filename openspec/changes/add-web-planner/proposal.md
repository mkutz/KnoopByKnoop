# Proposal

## Why

Planning a long ride on a phone means a lot of zooming and panning; a big screen with a mouse shows the whole tour at once. Riders often plan at home the evening before and ride with the phone the next day.
knoopbyknoop.com now exists, and the planning logic already lives in the Kotlin Multiplatform core, free of platform APIs, so a planner in the browser can reuse it instead of being written a second time.

## What Changes

- A route planner in the browser at https://knoopbyknoop.com/plan/, in English, German, Dutch and French like the rest of the site, linked from every page's navigation:
  - **The networks:** the cycling and the hiking network on a large map, knots and courses as in the apps, with the switch between the two.
  - **Planning:** clicking knots plans the route through the core as the apps do: waypoints, inserting and reordering stops, round trips, route points on the map, the route summary with its distance, undo.
  - **Places:** place search, the places useful on a ride on the map, and a place card with details when one is clicked; a place can become a route point.
  - **Ferries:** ferries on the route and whether they run, as in the apps.
  - **The current route is kept** in the browser, so a reload or a later visit finds it again.
- Handing the route to the phone:
  - **Route link:** `https://knoopbyknoop.com/route/#…` carries the route in the part after `#`, which browsers never send to the server. On a phone with the app, the link opens the route in the app; without the app, it opens the route in the web planner.
  - **QR code** of that link, to scan with the phone from the screen.
  - **Downloads:** the route as a `.knoop` file and as GPX, the same files the apps write.
  - **Opening a `.knoop` file** in the planner, by choosing it or dropping it on the map.
- **The apps open route links:** Android App Links and iOS Universal Links for `https://knoopbyknoop.com/route/`, loading the route as when a `.knoop` file is opened.
- **Network data from the website:** the site serves the published network data itself, since browsers cannot read the GitHub release downloads from another site. It is updated with every weekly data publication.
- **An exception to "nothing from other sites"** for the planner pages only: the base map from OpenFreeMap and the place search from Photon, the same services the apps use. The privacy policy says so for the planner.

## Out of scope

- Navigation, the rider's position and anything else that needs to be outdoors; the planner plans only.
- Accounts, saved route collections or syncing between devices; the route lives in the browser and travels by link or file.
- Offering route links from the apps' Share choices; the apps only open them for now.
- A layout for phone-sized browsers beyond being usable; on a phone, the app is the better planner.
- Downloading the networks for offline use in the browser.
- The base map in the planner's own style beyond what the apps already have (light, and the dark style in `map-styles/`).
- Changing the network data format or the apps' download source; the apps keep downloading from the GitHub releases.

## Capabilities

### New Capabilities
- `web-planner`: the route planner in the browser: what it shows, how a route is planned and kept there, how it is handed to the phone, where its data comes from, and what it loads from other sites.

### Modified Capabilities
- `project-website`: the planner page joins the pages in every language, and "No tracking and nothing from other sites" gets its named exception for the planner pages. This capability is still in the change `add-project-website`, which has to be archived before this change.
- `route-sharing`: the route link as a second form of a shared route, and the apps opening it like a `.knoop` file.

## Impact

- `modules/core`: a JavaScript target next to JVM and iOS; anything in `commonMain` that the target cannot compile gets fixed there.
- New Gradle module `web` (Kotlin/JS) for the planner's interface, with MapLibre GL JS bundled and served from the site; the `website` generator writes the planner pages and takes in the bundle.
- `.github/workflows/website.yml`: also runs after the weekly network data publication, and copies the published data into the site.
- `apps/android`: an App Links intent filter; `apps/ios`: the Associated Domains entitlement, which the App ID and the release signing need; `website/static/`: `assetlinks.json` and `apple-app-site-association`.
- `PRIVACY.md` and its translations: the planner and what OpenFreeMap and Photon receive from it.
- `openspec/checked-by-hand.md`: the checks only possible in a browser or on a phone.
