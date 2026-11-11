# Manual checks

## How it was checked

Checked on 2026-11-10:

- **Data:** the place details built locally with `pipeline places` from the Geofabrik extracts of 2026-11-08 (NL, BE, NRW): 3.3 MB, 99,738 places in 3,375 squares, 95% of the opening hours readable. The minicamping Manege Grol (an OSM way), the Fletcher Strandhotel and Het Wapen van Zeeland (nodes) were found in the squares of their icons, by the tiles' feature ids.
- **Android:** the emulator (`Medium_Phone_API_37.0`) with the debug build. Nothing is published yet, so the 12 squares around Renesse were written into the app's store with a script (as if downloaded). Taps with `adb shell input tap`, the camera set in `camera.json`.
- **iOS:** the maintainer's iPhone with a debug build from main, after the workflow had published the place details (2026-11-11): cards with opening hours, websites and phone numbers, also at a large text size.

## Scenarios

| Scenario | Android | iOS | Notes |
|---|---|---|---|
| Campsites near Renesse | ✅ | build | Campsite and hotel icons on the light and the dark map ([light](screenshots/android-campsites-near-renesse.png), [dark](screenshots/android-campsites-near-renesse-dark.png)). `PlaceKindTest`, `BaseMapPlacesTest` on a stored tile |
| Motorhome sites and holiday homes | unit test | unit test | `PlaceKindTest` |
| Campsite with website and phone | ✅ | maintainer | Minicamping Manege Grol: "manegegrol.nl" and "+31 111 461 623" ([Android](screenshots/android-campsite-with-website-and-phone.png)). `PlaceStoreTest` |
| Opening the website | ✅ | maintainer | Chrome opened (its first-run screen on the emulator); back in the app, card and map as before |
| Calling a hotel | ✅ | maintainer | The dialler with "+31 111 461 623", nothing called ([Android](screenshots/android-calling.png)) |
| Website without a scheme | unit test | unit test | `PlaceDetailsTest` |
| Only a phone number, No contacts, Several numbers | unit test | unit test | `PlaceStoreTest`, `PlaceDetailsTest` |
| Contacts while navigating | unit test | unit test | The lookup reads only installed squares, the same while navigating; on a ride (below) |
| Details not installed | ✅ | maintainer | Before the squares were installed: icon and name at once, no rows ([Android](screenshots/android-card-details-not-installed.png)) |
| Details arriving after the tap | — | — | Needs the published file; once the workflow has published it, on both phones |
| Contacts with a screen reader | — | — | With TalkBack and VoiceOver; the rows are buttons labelled e.g. "manegegrol.nl, opens the website in the browser" |
| Café open now … Opening hours while navigating | ✅ | maintainer | De IJswinckel at 22:45: "Closed until tomorrow 12:00", "Mo-Su 12:00-20:00", website and phone ([Android](screenshots/android-card-opening-hours.png)). `PlaceDetailsTest` for all wordings |
| Closing while the card is shown | — | — | Updated every minute like the ferry list; keep a card open over a closing time |
| Opening hours with a screen reader | — | — | With TalkBack and VoiceOver |
| Published place details, Contact keys, Place drawn from a building, Café with opening hours only, Place without contacts, Places that are not highlighted | unit test | unit test | `PlaceExtractionTest`, and the local build (above) |
| Failed places build | — | — | `VerifyTest`; the workflow keeps the published file. On the first workflow run |
| Place details size budget | — | — | 3.3 MB against 10 MB; on the first workflow run |
| Index, format version | unit test | unit test | `NetworkIndexTest`, `PlaceFileTest`, `MainTest` |
| Place details around the route, for a tapped place, keeping | unit test | unit test | `PlaceStoreTest` |
| Opening the info sheet with place details | ✅ | maintainer | "Data of 2026-11-08 · 0.3 MB" with Delete ([Android](screenshots/android-info-sheet-place-details.png)) |
| No place details installed, Deleting the place details | ✅ | maintainer | After Delete: "None downloaded", only the list file left, networks and ways still there |
| Explaining place details | ✅ English | maintainer | German, Dutch and French texts in `strings.xml` and `Localizable.xcstrings` |
| Place details delete button with a screen reader | ✅ label | — | Labelled "Delete downloaded place details", like the ways' button |

## Found and fixed

- **iOS card ran off the screen with large text:** it was centred 90 points below the place and kept inside the screen only sideways, so the taller card with details covered the place and its lower rows ran past the screen. It now hangs below the place, stays between the switch and the sheet, scrolls when taller, and grows wider with the text size. Checked by the maintainer on the iPhone at a large text size ([iOS](screenshots/ios-card-large-text.png)). Android's card is a dropdown menu the system keeps inside the screen.
- **Android info sheet showed "None downloaded" with place details installed:** `loadPathData()` copied the state it had read before reading the ways' size, and so undid the place details' size set meanwhile. It now reads the size first.

## For the maintainer

1. **After the next weekly run:** the index has a `places` entry and `places.details` is in the release (about 3.3 MB).
2. **iPhone, planning near Renesse or anywhere:** tap a campsite and a restaurant: tent or fork icon, opening status in green or red with the hours, website and phone. Tap the website (Safari opens) and the number (iOS asks before calling). Info sheet: the "Place details" entry with size and Delete.
3. **Both phones, a place far from the route while planning, online:** the contacts appear on the open card after a moment.
4. **On a ride, both phones, offline:** tap a café near the route: its contacts and opening status are there. Compare the status with the door.
