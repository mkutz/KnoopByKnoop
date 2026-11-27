# Spec Delta

## ADDED Requirements

### Requirement: Route link
A route SHALL also be shareable as a link: `https://knoopbyknoop.com/route/#` followed by the text of its route file (see "Route file"), percent-encoded. The route SHALL be only in the part after `#`, which browsers do not send to the server. A link SHALL be read as its route file is (see "Reading a route file"). Parameters added before `#`, as messengers add `?fbclid=…`, SHALL be ignored.

#### Scenario: Link of a route
- **WHEN** the link of the cycling route 02 → 83 → 73 is made
- **THEN** it starts with `https://knoopbyknoop.com/route/#`, and the rest decodes to the text of that route's file

#### Scenario: Link with added parameters
- **WHEN** a messenger has added `?fbclid=IwAR0x` before the `#` of a route link
- **THEN** the link is read as without it

#### Scenario: Route not sent to the server
- **WHEN** a browser opens a route link
- **THEN** the request to knoopbyknoop.com is for `/route/` only, without the route

### Requirement: Open a route link in the app
On a phone with the app, opening a route link SHALL open the app and load the link's route as opening a route file does (see "Open a route file"), with the same handling of the other network and of navigation, also when the app was not running and showed the other network last. The route SHALL be shown whole on the map, clear of the buttons over it. A link that holds no route SHALL be told as a link that holds no route the app can open. Without the app, the link SHALL open in the browser.

#### Scenario: Link opened on Android
- **WHEN** the user taps a link of the route 02 → 83 → 73 in a messenger on an Android phone with the app
- **THEN** the app opens with the route 02 → 83 → 73, shown whole on the map

#### Scenario: Link opened on iOS
- **WHEN** the user scans the QR code of a route link with the iPhone's camera and opens it
- **THEN** the app opens with the link's route, shown whole on the map

#### Scenario: Link for the other network while the app is closed
- **WHEN** the app is not running, showed hiking last, and the user opens a link of the cycling route 02 → 83 → 73
- **THEN** the app opens on cycling with the route 02 → 83 → 73

#### Scenario: Link without a route
- **WHEN** the user opens `https://knoopbyknoop.com/route/` without a route after `#`
- **THEN** the app says the link holds no route it can open, and the route stays as it was

#### Scenario: Link opened while navigating
- **WHEN** the rider is navigating and opens a route link
- **THEN** the app says to stop navigating to open it, and navigation goes on with its route
