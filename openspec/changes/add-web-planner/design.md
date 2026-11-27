# Design

## Context

- The core (`modules/core`) holds everything the planner needs and builds for the JVM and iOS: the graph and course files (`GraphFile`, `NetworkGeometry`), routing (`Router`, `PathRouter`), the planning rules (`PlanningSession`, `RouteHistory`), route files and GPX (`RouteFile`, `Gpx`), place search requests (`PlaceSearch`, Photon), place details and opening hours (`PlaceDetails`, `OpeningHours`), ferries (`FerryWatch`), and reading squares of a file by range (`SquareFile`). It depends only on kotlinx-serialization-json, which also builds for JavaScript.
- The apps download the data from `https://github.com/mkutz/knoopbyknoop/releases/download/network-data/`: `index.json` (2 KB), `cycling.graph` (0.7 MB), `cycling.geometry` (3.3 MB), the hiking pair (1.3 + 2.6 MB), `places.details` (3.3 MB) and `cycling.paths` (103 MB, read in squares by HTTP range). That address answers browsers from other sites with a redirect without `Access-Control-Allow-Origin`, so a browser cannot read it.
- knoopbyknoop.com is built by the `website` generator and published by `.github/workflows/website.yml` to GitHub Pages, which answers range requests (checked: `206` with the requested bytes).
- The apps draw the base map from OpenFreeMap's Liberty style, with their own dark style in `map-styles/liberty-dark.json`; the places useful on a ride come from the base map's tiles. OpenFreeMap and Photon both answer browsers from other sites (`Access-Control-Allow-Origin: *`).
- Neither app handles links yet: no App Links intent filter, no Associated Domains.
- `add-project-website` is still open, waiting for the next release; its `project-website` capability has to be archived before this change, whose delta modifies it.

## Goals / Non-Goals

**Goals:**
- The planner plans with the core's code, so a route is the same in the browser and in the apps, and fixes to the planning rules reach all three.
- Everything the planner runs is served from knoopbyknoop.com and built in the repository; only map tiles and search answers come from the two named services.
- The planner's behaviour is tested in `./gradlew check` where it doesn't need a browser.

**Non-Goals:**
- A shared user interface with the apps; each app stays native, and so does the web planner (plain HTML and CSS around the map).
- Offline use in the browser, a service worker, or installing the planner as an app.

## Decisions

### The core gets a JavaScript target; the planner is a Kotlin/JS module `web`
`modules/core` adds `js { browser() }` beside `jvm()` and the iOS targets, with `commonTest` also running on it, so the core's tests show where JavaScript behaves differently (for example `Long` arithmetic and number formatting). A new module `web` (Kotlin/JS, browser, IR) holds the planner's interface: it builds the page's DOM with `kotlinx-browser`, talks to MapLibre GL JS through `external` declarations for the few calls it needs, and keeps the planner's state in one class that the tests drive without a browser. Webpack bundles it, with MapLibre and a QR code library from npm, into `planner.js` and `planner.css`, which the `website` generator copies into the site.

**Alternatives:**
- **Kotlin/Wasm (`wasmJs`):** faster routing, but still Beta, with more friction calling MapLibre's JavaScript API and a bigger first download. The JS target can be swapped for Wasm later without changing the specs.
- **TypeScript interface calling the core compiled to JS:** two languages and two test setups for one planner, and the exported Kotlin API would need `@JsExport` throughout the core.
- **Compose Multiplatform for web:** draws into a canvas, which makes text selection, browser zoom and screen readers worse, and still needs MapLibre beside it.

### The data is published into the site, in a folder per publication
The Website workflow downloads the files of the latest `network-data` release with `gh release download` and puts them into `data/<publication>/` in the site, with `data/index.json` naming the current folder. The planner first reads `data/index.json` (sent with GitHub Pages' short cache time), then only files under that publication's folder, so a cached older file can never be mixed with a newer index. The checksums in the index are checked as the apps check them.

The workflow runs, besides on pushes, after the weekly `Update network data` workflow finishes successfully (`workflow_run`), so new data is online within the run's time.

**Alternatives:**
- **Serve the data through a proxy (Cloudflare Worker or similar):** another host, account and privacy policy entry, for files that fit on GitHub Pages.
- **Move the publication from GitHub releases to the site for the apps too:** changes the apps' download source and `network-sync`; possible later, not needed now.

### Planner pages from the generator, texts from properties files
The generator writes `plan/index.html` per language with the header and footer of the other pages, an empty map element, the texts from `website/content/<lang>/planner.properties` as JSON in the page, and the `<noscript>` text with the store badges. The check for missing pages and texts covers the planner's keys in every language. `route/index.html` is a small page that sends the visitor to the planner in the browser's language, keeping the part after `#`.

### Hand-off: route link in the core, QR code in the bundle
The link's encoding and decoding live in the core next to `RouteFile`, so the planner and both apps share it. The QR code is drawn by a small QR library bundled from npm (`qrcode-generator`, MIT), with the lowest error correction level that keeps a 30-knot route in a code a phone reads from a laptop screen.

**Alternative:** a QR encoder written in Kotlin in the core: no dependency, but the error-correction maths is work that doesn't belong to this project.

### The apps open `https://knoopbyknoop.com/route/`
- **Android:** an intent filter with `android:autoVerify="true"` for `https://knoopbyknoop.com/route/`, handled like a `.knoop` file. The site serves `/.well-known/assetlinks.json` with the SHA-256 fingerprints of the Play app signing key and the upload key.
- **iOS:** the Associated Domains entitlement `applinks:knoopbyknoop.com`, handled in `onOpenURL` like a `.knoop` file. The site serves `/.well-known/apple-app-site-association` with the app's team and bundle id for `/route/*`. The App ID needs the Associated Domains capability, and the release workflow's signing has to include it.

### The planner exception in the check for other sites
`foreignResources` gets a list of hosts allowed per page: none for every page, `tiles.openfreemap.org` and `photon.komoot.io` for the planner pages. The bundle is checked too, since the base map style and the search address are in it. A script loaded from another host stays forbidden on every page.

### What is kept in the browser
The route and its undo history are stored in `localStorage` under one key, as the text of a route file plus the history, read in a `try`, so blocked storage only loses the route on reload. Nothing is stored in cookies, and nothing of the route goes to knoopbyknoop.com.

## Risks / Trade-offs

- [Routing in JavaScript is slower than on the JVM, mostly where the core uses `Long`] → Run the core's routing tests on JS and time a long route; move hot spots to `Int` or `Double` in `commonMain` if needed, which the apps don't mind.
- [`cycling.paths` is 103 MB, near GitHub Pages' limits for a single file and a site of 1 GB] → The site stays far below 1 GB; if the deployment refuses the file, the workflow fails before publishing, and the paths can be split by the pipeline in its own change.
- [GitHub Pages serves `apple-app-site-association` without a JSON content type] → Apple's CDN accepts it; checked by hand with Apple's validation and a link on the iPhone.
- [Long routes make long links and dense QR codes] → A 30-knot route's file is well under a kilobyte; beyond what a QR code holds, the planner offers the link and the file only and says why.
- [The planner pages contact OpenFreeMap and Photon] → Only on the planner pages, named in the privacy policy, and the same services and data the apps already send.
- [The route link's knots only resolve against the network the phone has] → The link carries node ids as the route file does, and reading falls back to numbers and towns as for files.

## Migration Plan

1. Archive `add-project-website` (after its store links reach the stores), then this change can be archived after it.
2. The planner goes online with the next push; the apps open links once a release with the link handling is out. Until then, links open the web planner on phones too, which still shows the route.
3. Rollback: remove the planner from the navigation; the rest of the site is unchanged.

## Open Questions

- The fingerprint of the Play app signing key for `assetlinks.json`; the maintainer reads it from the Play Console (App integrity).
- Whether the iOS release signing picks up the new entitlement by itself or needs a new provisioning profile; seen on the first build with it.
