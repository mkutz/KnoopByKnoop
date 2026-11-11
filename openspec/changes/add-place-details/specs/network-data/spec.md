## ADDED Requirements

### Requirement: Published place details
The weekly publication SHALL include one file with the details of every highlighted place (see `map-planning` "Places useful on a ride") in the covered area: its website, its phone number and its opening hours as OpenStreetMap has them. It SHALL be built from the same extracts as the networks, identify each place by its OpenStreetMap object, so the app can find the details of a place tapped on the base map, and be split into squares that can be downloaded on their own. Places with none of them SHALL be left out. Details missing in OpenStreetMap SHALL be left out, not guessed.

#### Scenario: Campsite with contacts
- **WHEN** a campsite node has `website` and `phone` in OpenStreetMap
- **THEN** the file carries that website and that phone number for that node

#### Scenario: Contact keys
- **WHEN** a hotel has `contact:website` and `contact:phone` instead of `website` and `phone`
- **THEN** the file carries them as its website and phone number

#### Scenario: Place drawn from a building
- **WHEN** a restaurant is mapped as a building outline (a way) with its tags on the way
- **THEN** the file carries its details for that way, and the app finds them when its icon is tapped

#### Scenario: Café with opening hours only
- **WHEN** a café has `opening_hours` but no website and no phone number in OpenStreetMap
- **THEN** the file carries its opening hours as tagged

#### Scenario: Place without contacts
- **WHEN** a drinking water point has no website, no phone number and no opening hours
- **THEN** it is not in the file

#### Scenario: Places that are not highlighted
- **WHEN** a pharmacy or a holiday home has a website
- **THEN** it is not in the file

#### Scenario: Failed places build
- **WHEN** building the place details fails
- **THEN** the previously published place details and the index stay available unchanged, and the networks and the path data are still published

#### Scenario: Place details size budget
- **WHEN** the place details of the whole covered area are written
- **THEN** the file is smaller than 10 MB, or the place details are not published

### Requirement: Place details in the index
The index SHALL describe the place details file in an optional entry of its own, with the data date and what is needed to verify it. Apps that don't know this entry SHALL be able to read the index as before.

#### Scenario: An earlier app reads the index with place details
- **WHEN** a released app that doesn't know place details reads an index with this entry
- **THEN** it installs the graphs, the geometry and the path data as before

#### Scenario: An index without place details
- **WHEN** an app reads an index without this entry
- **THEN** it works as before, and place cards show no contacts

### Requirement: Versioned place details format
The place details file SHALL state its format version, so an app can tell that it can't read it.

#### Scenario: Unsupported place details format
- **WHEN** the place details file has a format version the app doesn't support
- **THEN** the app doesn't use it, place cards show no contacts, and the networks and the path data are not affected
