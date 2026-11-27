## Purpose

The public website of Knoop by Knoop at knoopbyknoop.com: what the app does and where to get it, help, and the legal texts, in every app language, without tracking, built from the repository so it stays in step with the store listings.

## ADDED Requirements

### Requirement: Pages in every app language
The website SHALL be published at https://knoopbyknoop.com and SHALL offer these pages in English, German, Dutch and French: home, help, privacy policy, terms of use and legal notice. English SHALL be at the site's root and each other language under its own path (`/de/`, `/nl/`, `/fr/`). Every page SHALL link to the same page in the other three languages and to the other pages in its own language.

A check in the build SHALL fail when a page is missing in a language, and SHALL name the missing page.

#### Scenario: Language switch
- **WHEN** a visitor on the German help page chooses Nederlands
- **THEN** the Dutch help page opens

#### Scenario: Missing page in a language
- **WHEN** the French terms of use are missing
- **THEN** the check fails and names the French terms of use

### Requirement: Home page from the store listing
The home page SHALL show, in its language, the app's name and subtitle, the promotional text and the description of the store listing, the App Store and Google Play badges linking to the apps, and the store screenshots of that language. When the store texts or screenshots in the repository change, the next published site SHALL show the changed ones.

#### Scenario: Store description changes
- **WHEN** the Dutch store description is changed in the repository and the change is pushed to main
- **THEN** the Dutch home page shows the changed description once the site is published again

#### Scenario: Links to the stores
- **WHEN** a visitor on the English home page taps the App Store badge
- **THEN** the app's page in the App Store opens; the Google Play badge opens the app's page in Google Play

### Requirement: Help page
The help page SHALL explain in its language how to plan a route, how to navigate it, how to share it, and what to do when the map shows no knots (the networks only exist in the Netherlands, Belgium and North Rhine-Westphalia). It SHALL say how to reach the maintainer: the mail address and the GitHub issues.

#### Scenario: No knots on the map
- **WHEN** a visitor reads the help page because the map shows no knots
- **THEN** the page tells them which areas have networks and how to move the map or search a place there

### Requirement: Legal texts in every language, English binding
The privacy policy and terms of use SHALL be the texts of the repository's English privacy policy and terms, and their German, Dutch and French translations. Each translation SHALL say that the English version is the binding one and link to it. A translation SHALL carry the same "last updated" date as the English text it translates; a check in the build SHALL fail when a translation's date differs from the English one, and SHALL name it.

#### Scenario: English text updated, translation not
- **WHEN** the English privacy policy gets a new "last updated" date and the German one keeps the old date
- **THEN** the check fails and names the German privacy policy

#### Scenario: Binding version named
- **WHEN** a visitor opens the French terms of use
- **THEN** the page says that the English version is binding and links to it

### Requirement: Legal notice
The legal notice SHALL name who runs the website, with a postal address and a mail address, in every language.

#### Scenario: Who runs the site
- **WHEN** a visitor opens the legal notice in any language
- **THEN** it shows the name, postal address and mail address of the person running the site

### Requirement: No tracking and nothing from other sites
The website SHALL set no cookies, use no analytics or tracking, and load no scripts, styles, fonts or images from other sites; everything a page loads SHALL come from knoopbyknoop.com. Links that open other sites (the stores, GitHub, OpenStreetMap) are allowed. The privacy policy SHALL describe what the website's host receives when a page is opened. A check in the build SHALL fail when a page loads anything from another site, and SHALL name the page and the address.

#### Scenario: Font from another site
- **WHEN** a page loads a font from fonts.googleapis.com
- **THEN** the check fails and names the page and that address

#### Scenario: No cookies
- **WHEN** a visitor opens any page of the site
- **THEN** no cookie is set

### Requirement: Published from main
The website SHALL be built from the repository's main branch and published when a push to main changes anything it is built from. A build that fails the website's checks SHALL NOT be published; the site then stays as it was.

#### Scenario: Push changes the help
- **WHEN** a change to the English help page is pushed to main
- **THEN** the published English help page shows it within minutes

#### Scenario: Failing check
- **WHEN** a push to main leaves the Dutch legal notice missing
- **THEN** the site is not published again and the previous version stays online
