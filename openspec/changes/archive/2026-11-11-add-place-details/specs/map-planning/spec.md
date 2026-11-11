## MODIFIED Requirements

### Requirement: Places useful on a ride
The map SHALL draw places from the base map that matter on a ride with their own icon on a coloured disc, one per kind:
- **food and drink:** restaurants, cafés, fast food, bars, pubs, beer gardens and ice cream
- **supermarkets**
- **bicycle shops,** including bicycle repair, and bicycle repair stations where the base map has them
- **railway stations** and halts
- **water and toilets:** drinking water and public toilets
- **campsites,** not those for motorhomes only
- **hotels and guest houses:** hotels, motels, hostels and guest houses, not holiday homes

The discs' colours SHALL differ from the knots' sign colours and from the route. The base map SHALL NOT draw these places a second time in its own style. The places SHALL be shown from zoom 14, and their names from zoom 16. The knots, the route, the waypoints and the rider's position SHALL be drawn above them, and where a place's icon would overlap a knot's badge, the badge SHALL be shown and the place left out. The places SHALL be shown on the light and the dark map, for both network types, while planning and while navigating. They come with the base map, so where the base map is missing they are missing too; nothing beyond the base map SHALL be loaded to draw them.

#### Scenario: Places in a town
- **WHEN** the map shows the centre of Troisdorf at zoom 15
- **THEN** cafés and restaurants, supermarkets, the bicycle shops and Troisdorf station each show their kind's icon on its coloured disc, without names

#### Scenario: Names close up
- **WHEN** the user zooms in to zoom 16 over a highlighted café
- **THEN** its name is shown next to its icon

#### Scenario: Zoomed out
- **WHEN** the map is at zoom 13
- **THEN** no highlighted places are shown

#### Scenario: Shown once
- **WHEN** a supermarket is highlighted
- **THEN** the base map's grey shop icon is not drawn at the same place

#### Scenario: Other places unchanged
- **WHEN** the map shows a pharmacy, a bank or a bike rental
- **THEN** they are drawn in the base map's own style, as before this change

#### Scenario: Knot next to a café
- **WHEN** a café is a few metres from a knot, so their icons would overlap
- **THEN** the knot's badge is shown in full and the café's icon is left out

#### Scenario: Under the route
- **WHEN** the route runs past a restaurant
- **THEN** the route line is drawn above the restaurant's icon

#### Scenario: Distinct from knots
- **WHEN** highlighted places are shown among Dutch, Belgian and German knots
- **THEN** no place disc has the colour of a knot's sign, and each kind can be told from the others by its icon and colour

#### Scenario: Places on the dark map
- **WHEN** the app is dark and the map shows highlighted places
- **THEN** their icons and names are as readable as on the light map

#### Scenario: Places while navigating
- **WHEN** the rider navigates through a town at zoom 15 or closer
- **THEN** the highlighted places are shown along the way, below the route and the rider's arrow

#### Scenario: Places on a hiking map
- **WHEN** the hiking network is selected
- **THEN** the same places are highlighted

#### Scenario: Without the base map
- **WHEN** the base map cannot be loaded, e.g. offline on first start
- **THEN** the network is shown as before and no places are shown

#### Scenario: Campsites near Renesse
- **WHEN** the map shows the dunes east of Renesse at zoom 15
- **THEN** the campsites show the campsite icon on its own coloured disc, and the hotels and guest houses show the hotel icon on another

#### Scenario: Motorhome sites and holiday homes
- **WHEN** the map shows a site for motorhomes only or a holiday home in a park
- **THEN** they are drawn in the base map's own style, as before this change

## ADDED Requirements

### Requirement: Contacts on the place card
The card of a tapped highlighted place SHALL show the place's website and phone number from OpenStreetMap below its name, each with an action: the website opens in the browser, the phone number opens the phone's dialler with the number, without calling. A detail missing in OpenStreetMap SHALL be left out, not guessed, and a place without both SHALL show the card as before. The contacts SHALL be shown for every highlighted kind, while planning and while navigating. They come from the downloaded place details (see `network-sync` "Place details around the route"); where those are not installed, the card SHALL show what it can without them and SHALL NOT wait for them before showing the name. Showing or using the contacts SHALL NOT change the route or the navigation.

#### Scenario: Campsite with website and phone
- **WHEN** the user taps a campsite that has a website and a phone number in OpenStreetMap, with its place details installed
- **THEN** the card shows the campsite's name, its website and its phone number, each with its action, and the route point choices below

#### Scenario: Opening the website
- **WHEN** the user taps the website on the card
- **THEN** the website opens in the browser, and the route and the map stay as they are when the user comes back

#### Scenario: Calling a hotel
- **WHEN** the user taps the phone number of a hotel on the card
- **THEN** the phone's dialler opens with the number, and nothing is called until the user confirms there

#### Scenario: Website without a scheme
- **WHEN** a place's website is tagged as `www.example.nl`, without `https://`
- **THEN** the card shows it and opens `https://www.example.nl`

#### Scenario: Only a phone number
- **WHEN** a café has a phone number but no website in OpenStreetMap
- **THEN** the card shows the phone number with its action and no website row

#### Scenario: No contacts
- **WHEN** a drinking water point has neither a website nor a phone number
- **THEN** the card shows the water icon and its name as before, without empty rows

#### Scenario: Contacts while navigating
- **WHEN** the rider is navigating and taps a restaurant next to the route that has a website
- **THEN** the card shows the name and the website with its action, without the route point choices, and the navigation goes on

#### Scenario: Details not installed
- **WHEN** the user taps a place whose place details are not installed and cannot be downloaded
- **THEN** the card shows the icon and the name at once, as before this change, without contacts

#### Scenario: Details arriving after the tap
- **WHEN** the user taps a place while planning and its place details are downloaded while the card is open
- **THEN** the contacts appear on the open card without tapping again

#### Scenario: Several numbers
- **WHEN** a hotel's phone is tagged with two numbers separated by `;`
- **THEN** the card shows the first number

#### Scenario: Contacts with a screen reader
- **WHEN** a screen reader reaches the website or the phone number on the card
- **THEN** it reads the website or the number and announces that it opens the website or the dialler, as a button

### Requirement: Opening hours on the place card
The card of a tapped highlighted place with opening hours in its place details SHALL show whether the place is open now according to OpenStreetMap and, where that can be told too, when that changes next, worded as for ferries (see "Ferry open now"), in the local time of the covered area, with the opening hours as tagged below it. When the opening hours don't allow a sure answer, the card SHALL show only the tagged hours. The status SHALL stay current while the card is shown. A place without opening hours SHALL show neither.

#### Scenario: Café open now
- **WHEN** the user taps a café with "Mo-Su 08:00-18:00" on Wednesday, 16 June 2027, 14:00
- **THEN** the card shows "Open until 18:00" and "Mo-Su 08:00-18:00" below it

#### Scenario: Restaurant opening later
- **WHEN** the user taps a restaurant with "Tu-Su 17:00-22:00" on Wednesday, 16 June 2027, 14:00
- **THEN** the card shows "Closed until 17:00"

#### Scenario: Restaurant closed on its rest day
- **WHEN** the user taps the same restaurant on Monday, 14 June 2027, 14:00
- **THEN** the card shows "Closed until tomorrow 17:00"

#### Scenario: Lunch and dinner
- **WHEN** the user taps a restaurant with "Mo-Su 11:30-14:00,17:30-22:00" on Wednesday, 16 June 2027, 15:00
- **THEN** the card shows "Closed until 17:30"

#### Scenario: Hours the app can't be sure of
- **WHEN** the user taps a café with "Mo-Fr 08:00-18:00; SH off" on Wednesday, 16 June 2027, 10:00
- **THEN** the card shows no status, only "Mo-Fr 08:00-18:00; SH off"

#### Scenario: Unreadable hours
- **WHEN** a café's opening hours are tagged as "summer only, ask inside"
- **THEN** the card shows no status, only the tagged text

#### Scenario: Closing while the card is shown
- **WHEN** the card of a café that closes at 18:00 is shown at 17:59
- **THEN** its status changes to closed within a minute after 18:00, without any action

#### Scenario: Place without opening hours
- **WHEN** the user taps a campsite with a website but no opening hours
- **THEN** the card shows the website and no opening status or hours

#### Scenario: Opening hours while navigating
- **WHEN** the rider is navigating and taps a café next to the route whose place details are installed
- **THEN** the card shows its opening status and hours, and the navigation goes on

#### Scenario: Opening hours with a screen reader
- **WHEN** a screen reader reaches the opening status on the card
- **THEN** it reads the status followed by the tagged hours
