# Privacy Policy

Last updated: 5 November 2026

This policy covers the Knoop by Knoop apps for Android and iOS.


## In short

Knoop by Knoop has no accounts, no ads, no analytics and no crash reporting.
Your location and your routes stay on your device.
The app only talks to three services to download network data, show the base map and search for places.
If you turn on guidance away from the route, which is off until you do, GitHub can also see the rough area of the routes you plan, but never where you are.


## Who is responsible

Michael Kutz
Wellenstr. 52, 53721 Siegburg
mail@knoopbyknoop.com


## Data that stays on your device

- **Location.** While you navigate, the app uses your location to tell you the next knot. It is only used on the device and never sent anywhere. The app only accesses your location while it is in use, or while navigation runs, shown by a notification.
- **Routes and settings.** Your current route, the downloaded networks and ways, and your settings are stored on the device. They are removed when you uninstall the app.
- **Shared routes.** When you share a route as a link or QR code, the link contains the route's knots. It is only passed to whoever you share it with.


## Services the app contacts

All connections are encrypted (HTTPS).
Like any internet connection, each request reveals your IP address and basic device information (such as the app's user agent) to the service.
The app sends no identifiers of its own.

| Service | Purpose | What it receives |
|---|---|---|
| [GitHub](https://github.com) (GitHub, Inc., USA) | Downloading the knooppunt networks, on first start and at most once a day, and, only with guidance away from the route turned on, the ways around a planned route | IP address, with that feature on also the rough area of planned routes |
| [OpenFreeMap](https://openfreemap.org) | Base map tiles while the map is shown | IP address, the map area being shown |
| [Photon](https://photon.komoot.io) by komoot (komoot GmbH, Germany) | Place search | IP address, the text you type into the search field, the centre of the network area |

**Guidance away from the route.** This feature is off until you turn it on in the info sheet. While it is on, the app downloads the ways within about 5 km of a cycling route you planned, in squares of about 7 by 4 km, from GitHub. The squares requested show GitHub the rough area of that route, along with your IP address, and may be downloaded while you navigate if they weren't complete before. They are chosen from the route alone: your position is never used to choose them and never sent. The way back to the route is worked out on your device. Turning the feature off stops the downloads and removes the downloaded ways.

These services process the data under their own privacy policies:
[GitHub](https://docs.github.com/site-policy/privacy-policies/github-general-privacy-statement),
[OpenFreeMap](https://openfreemap.org/privacy/),
[komoot](https://www.komoot.com/privacy).

The legal basis is Art. 6(1)(b) GDPR (providing the features you use) and Art. 6(1)(f) GDPR (our legitimate interest in running the app).


## Permissions

- **Location** to navigate. Planning routes works without it.
- **Notifications** to show the next knot on the lock screen while navigating.
- **Internet** for the services above.


## Your rights

Since we store no personal data about you, there is usually nothing to access, correct or delete.
For data the services above process, contact them directly.
You have the right to lodge a complaint with a data protection supervisory authority.


## Changes

Changes to this policy are published here and tracked in the [repository history](https://github.com/mkutz/knoopbyknoop/commits/main/PRIVACY.md).
