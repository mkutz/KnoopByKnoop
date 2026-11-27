# Tasks

## 1. Generator

- [x] 1.1 Gradle module `website` (Kotlin JVM, commonmark-java with tables) included in `settings.gradle.kts`, with the task `:website:site` writing `website/build/site/`. Verify: `./gradlew :website:site` produces `index.html` and `de/index.html`.
- [x] 1.2 Page template: header with icon and name, the language switch, the navigation (home, help), a footer (privacy, terms, legal notice, OpenStreetMap credit), `hreflang` links, and a stylesheet with system fonts and light/dark via `prefers-color-scheme`, without scripts. Tests "Language switch" (the German help page links to `/nl/help/`) and "Missing page in a language" (a content directory without the French terms fails naming them).
- [x] 1.3 Home page from `fastlane/`: name, subtitle, promotional text, description and the five iPhone screenshots scaled to 600 px wide; App Store and Google Play badges per language in `website/static/badges/`, linking to `https://apps.apple.com/app/id6831624583` and `https://play.google.com/store/apps/details?id=com.knoopbyknoop`. Tests "Store description changes" (a changed Dutch description shows on the Dutch home page) and "Links to the stores".
- [x] 1.4 Check for other sites: every generated HTML and CSS file is read, and any `src`, `srcset`, `<link href>`, `@import` or `url()` pointing to another host fails, naming the page and address. Test "Font from another site".

## 2. Content

- [x] 2.1 Help pages in `website/content/{en,de,nl,fr}/help.md`: planning, navigating, sharing, "no knots on the map" (the network areas, search or move the map), and contact (mail and GitHub issues). Test "No knots on the map" (each help page has the section, naming the three areas).
- [x] 2.2 `PRIVACY.md` gets the section on the website (GitHub Pages, IP address and request data, no cookies, no analytics). English legal pages built from `PRIVACY.md` and `TERMS.md`.
- [ ] 2.3 German, Dutch and French privacy policy and terms in `website/content/<lang>/`, each with front matter `translates:` and the note that the English version is binding, with a link to it. Tests "English text updated, translation not" and "Binding version named". The maintainer reviews the German texts, and decides on a review of Dutch and French.
  - Done 2026-11-27: the translations and tests; the maintainer reviewed and approved the German texts. Left: the decision on Dutch and French.
- [x] 2.4 Legal notice per language with name, postal and mail address, matching the privacy policy's "Who is responsible". Test "Who runs the site", which fails when the legal notice and privacy policy disagree.
- [x] 2.5 A 404 page and `robots.txt`. Verify: both exist in `website/build/site/`.

## 3. Publishing

- [x] 3.1 Workflow `.github/workflows/website.yml`: on pushes to main touching `website/`, `fastlane/metadata/`, `fastlane/screenshots/`, `PRIVACY.md` or `TERMS.md`, and by hand. It runs `./gradlew :website:test :website:site`, then `actions/upload-pages-artifact` and `actions/deploy-pages`. Verify: `actionlint`, if available, and the first run.
  - Done 2026-11-27: the workflow (actionlint runs in Build on push; the local run was not allowed). The first run after the push (cf70cf0) built, checked and published the site.
- [x] 3.2 Maintainer: enable GitHub Pages with source "GitHub Actions" and push the change. Verify: the site is reachable at https://mkutz.github.io/knoopbyknoop/.
  - Done 2026-11-27: Pages enabled and pushed. The custom domain was already set, so the github.io address redirects to knoopbyknoop.com; the site itself is checked there in 3.3.
- [ ] 3.3 Maintainer, guided: in the Pages settings, set the custom domain knoopbyknoop.com. At Hostinger, replace the A records with GitHub Pages' four addresses, add `www` CNAME `mkutz.github.io`, and leave the MX records. Then turn on "Enforce HTTPS". Verify: https://knoopbyknoop.com and https://www.knoopbyknoop.com show the site with a valid certificate, and mail still arrives.

## 4. Store URLs

- [x] 4.1 `store-listing-check.sh` checks that each locale's `privacy_url.txt` and `support_url.txt` are the website's privacy and help pages in its language; `-test.sh` gets `# Scenario: Privacy URL in the listing's language`. Then set the eight URL files. Verify: `./gradlew check`.
- [ ] 4.2 After 3.3, one run of Release store listing brings the URLs to the App Store. The maintainer sets the Google Play privacy URL in the Play Console to https://knoopbyknoop.com/privacy/. Verify: App Store Connect shows the new URLs in all four languages.

## 5. Finish

- [x] 5.1 `openspec/checked-by-hand.md`, under "## project-website":
  - "No cookies": browser developer tools on the published site show no cookie on any page.
  - "Push changes the help": a change to a help page is live within minutes of the push.
  - "Failing check": a deliberately broken push in a test commit is not published.
- [x] 5.2 README links to the website; CHANGELOG line under "Unreleased". `./gradlew check` and `npx @fission-ai/openspec validate --all` pass.
- [x] 5.3 `./gradlew checkScenarios -Pchange=add-project-website` passes.
