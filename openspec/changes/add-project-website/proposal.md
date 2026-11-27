# Proposal

## Why

Knoop by Knoop is in both stores now, but it has no home of its own: the stores link to the privacy policy as a Markdown file on GitHub and to the GitHub issues for support, which looks unfinished and is hard to read for riders who are not developers.
The domain knoopbyknoop.com is already registered and used for mail. A small website there gives the apps a proper address and is the place a web planner can grow into later.

## What Changes

- A static website at https://knoopbyknoop.com, hosted on GitHub Pages, in English, German, Dutch and French, with a language switch on every page:
  - **Home:** what the app does, taken from the store description and promotional text, the App Store and Google Play badges linking to the apps, and the store screenshots in the page's language.
  - **Help:** how to plan, navigate and share a route, what to do when no knots show, and how to reach the maintainer (mail and GitHub issues).
  - **Privacy policy** and **Terms of use:** the texts of `PRIVACY.md` and `TERMS.md`, translated into German, Dutch and French, with the English version stated as the binding one.
  - **Legal notice (Impressum):** who runs the site and how to reach them, as German law requires for a site run from Germany.
- The website sets no cookies, has no tracking or analytics, and loads nothing from other sites: fonts, badges and images are served from the site itself.
- The site is built from the repository: the store texts and screenshots in `fastlane/`, the legal texts and the help pages. Every push to main that touches them publishes the site again.
- A check in the build fails when a page is missing in a language, when a translated legal text is older than the English one, or when a page loads anything from another site.
- The privacy policy gains a section on the website itself.
- The App Store privacy and support URLs point to the website's privacy and help pages in the listing's language; the Google Play privacy URL is changed by hand in the Play Console.

## Out of scope

- A web planner, or any map or routing in the browser; this change only makes the place it can live in later.
- Serving the network data from the website; the apps keep downloading it from the GitHub releases.
- Links from the apps to the website (e.g. from the info sheet); a later change can add them.
- Moving mail or anything else away from the current host; only the web address records of knoopbyknoop.com change.
- Any blog, news, newsletter, contact form or comments.

## Capabilities

### New Capabilities
- `project-website`: the public website at knoopbyknoop.com: its pages and languages, what it may and may not load, how it is built from the repository and published, and the check that keeps it complete.

### Modified Capabilities
- `store-listing`: the App Store support and privacy URLs point to the website's help and privacy pages in the listing's language.

## Impact

- New `website/` directory: page templates, help pages and the translated legal texts, plus a small generator that builds the site from them and from `fastlane/`.
- New GitHub Actions workflow publishing to GitHub Pages; GitHub Pages enabled for the repository with the custom domain.
- DNS for knoopbyknoop.com at Hostinger: the address records change to GitHub Pages (by the maintainer); the mail (MX) records stay.
- `PRIVACY.md`: a section on the website; `fastlane/metadata/ios/*/support_url.txt` and `privacy_url.txt`: the new addresses; then one run of Release store listing.
- `openspec/checked-by-hand.md`: the checks only possible on the published site.
