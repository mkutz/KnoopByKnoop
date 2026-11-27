# Spec Delta

## MODIFIED Requirements

### Requirement: Pages in every app language
The website SHALL be published at https://knoopbyknoop.com and SHALL offer these pages in English, German, Dutch and French: home, planner, help, privacy policy, terms of use and legal notice. English SHALL be at the site's root and each other language under its own path (`/de/`, `/nl/`, `/fr/`). Every page SHALL link to the same page in the other three languages and to the other pages in its own language.

A check in the build SHALL fail when a page is missing in a language, and SHALL name the missing page.

#### Scenario: Language switch
- **WHEN** a visitor on the German help page chooses Nederlands
- **THEN** the Dutch help page opens

#### Scenario: Missing page in a language
- **WHEN** the French terms of use are missing
- **THEN** the check fails and names the French terms of use

#### Scenario: Planner in the navigation
- **WHEN** a visitor opens the French home page
- **THEN** its navigation links to the French planner at `/fr/plan/`

### Requirement: No tracking and nothing from other sites
The website SHALL set no cookies, use no analytics or tracking, and load no scripts, styles, fonts or images from other sites; everything a page loads SHALL come from knoopbyknoop.com. Links that open other sites (the stores, GitHub, OpenStreetMap) are allowed. The privacy policy SHALL describe what the website's host receives when a page is opened. A check in the build SHALL fail when a page loads anything from another site, and SHALL name the page and the address.

The planner pages SHALL be the only exception: they MAY load the base map's style, tiles, fonts and symbols from tiles.openfreemap.org and place search results from photon.komoot.io, and nothing else from other sites; their scripts SHALL come from knoopbyknoop.com. The privacy policy SHALL name both services for the planner and what they receive.

#### Scenario: Font from another site
- **WHEN** a page loads a font from fonts.googleapis.com
- **THEN** the check fails and names the page and that address

#### Scenario: No cookies
- **WHEN** a visitor opens any page of the site
- **THEN** no cookie is set

#### Scenario: Base map on the planner only
- **WHEN** the help page refers to tiles.openfreemap.org, and the planner page does too
- **THEN** the check fails naming the help page and that address, and accepts the planner page

#### Scenario: Script from another site on the planner
- **WHEN** the planner page loads a script from cdn.jsdelivr.net
- **THEN** the check fails and names the planner page and that address
