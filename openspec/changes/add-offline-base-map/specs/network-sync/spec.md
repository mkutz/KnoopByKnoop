## ADDED Requirements

### Requirement: Base map around the route
While a cycling or hiking route is complete, the app SHALL store the base map around its course without the user doing anything else: in detail close to the course and coarser farther away, so the map can be read along the route while riding and around it while looking ahead. The tiles requested SHALL depend only on the route, never on the rider's position.

#### Scenario: Route planned at home
- **WHEN** the user completes a route with network access and stops changing it for a moment
- **THEN** the base map around it is stored, and the map shows streets, villages, rivers and woods along it later without network access

#### Scenario: A hiking route
- **WHEN** the user completes a hiking route
- **THEN** its base map is stored as for a cycling route

#### Scenario: Changing the route
- **WHEN** the user keeps adding knots to a route
- **THEN** the base map is only requested after the route has stopped changing, and tiles already stored are not requested again

#### Scenario: Starting before the download is done
- **WHEN** the rider starts navigating while parts of the route's base map are still missing, with network access
- **THEN** the missing parts of the route's base map are downloaded, and no others

#### Scenario: Starting away from the route
- **WHEN** the rider starts navigating 6 km from the route, with network access
- **THEN** no base map is requested for storing around the rider or on the way to the route

#### Scenario: A long route
- **WHEN** the route's base map would need more than 3,000 tiles or 50 MB
- **THEN** it is stored without the most detailed level where that doesn't fit, and the map can still be read zoomed in

#### Scenario: Without network access
- **WHEN** the route's base map can't be downloaded
- **THEN** planning and navigating work as before, and the download is tried again when network access is back

### Requirement: Base map requests
Every request for the base map SHALL name the app in its User-Agent. Storing a route's base map SHALL send at most 4 requests at a time and request at most 3,000 tiles and 50 MB per route, as OpenFreeMap's terms for offline downloads ask.

#### Scenario: Requests name the app
- **WHEN** the map loads tiles, while planning or while storing a route's base map
- **THEN** each request's User-Agent names Knoop by Knoop

#### Scenario: Requests at a time
- **WHEN** a route's base map is being stored
- **THEN** no more than 4 of its requests are open at a time

### Requirement: Base map ready for the ride
While planning a complete route, the panel SHALL show whether the route's base map is stored, after the line about the ways.

#### Scenario: Map still loading
- **WHEN** parts of the route's base map are still being downloaded
- **THEN** the panel says that the map for this route is loading

#### Scenario: Map loaded
- **WHEN** the route's base map is stored
- **THEN** the panel says that the map for this route is loaded

#### Scenario: Map not available
- **WHEN** the route's base map can't be downloaded
- **THEN** the panel says that the map for this route couldn't be loaded, and planning and navigating work as before

#### Scenario: Map status in all languages
- **WHEN** the app's language is German, Dutch or French
- **THEN** the panel's map line is in that language, also for a screen reader

### Requirement: Keeping the base map
The stored base map SHALL stay usable without network access until the route changes. The app SHALL keep the base map of the current route only: a new route's map SHALL replace it once that is downloaded. Tiles the map caches while viewing it SHALL be handled as before.

#### Scenario: A new route
- **WHEN** the user plans a new route in another area and its base map is stored
- **THEN** the base map of the previous route is removed

#### Scenario: Restarting the app
- **WHEN** the app is closed and started again without network access, with the same route planned
- **THEN** the route's base map is shown

### Requirement: Base map in the info sheet
The info sheet SHALL show the base map after the place details: the space it takes, or that none is stored, with a short text that the map around the planned route is stored for use without a connection and comes from OpenFreeMap, which can see the rough area of a route but never where the rider is. While one is stored, the entry SHALL offer a button to delete it.

#### Scenario: Opening the info sheet with a base map
- **WHEN** the base map of the route, taking 18 MB, is stored and the user opens the info sheet
- **THEN** it shows the base map after the place details, with 18 MB and a delete button

#### Scenario: No base map stored
- **WHEN** no base map is stored and the user opens the info sheet
- **THEN** the entry says that none is stored, and shows no delete button

#### Scenario: Explaining the base map
- **WHEN** the user opens the info sheet, in English, German, Dutch or French
- **THEN** the entry explains what the stored base map is for and what OpenFreeMap can see, in the app's language

#### Scenario: Deleting the base map
- **WHEN** the user taps the entry's delete button
- **THEN** the stored base map is removed, the entry says that none is stored, and the networks, ways and place details stay installed

#### Scenario: Deleting the base map with a route planned
- **WHEN** a route is planned with its base map stored and the user deletes it
- **THEN** no base map is requested until the route is changed or navigation starts, and then the route's base map is downloaded

#### Scenario: Base map delete button with a screen reader
- **WHEN** a screen reader reaches the entry's delete button
- **THEN** it announces that it deletes the stored base map, as a button
