# Design

## Context

- The store texts live in `fastlane/metadata/{ios,android}/<locale>/` and the screenshots in `fastlane/screenshots/ios/<locale>/` and `fastlane/metadata/android/<locale>/images/phoneScreenshots/`, five per language. The iPhone screenshots are 1320×2868 and about 2 MB each.
- `PRIVACY.md` and `TERMS.md` are English only. They say "Last updated: <day> <month> <year>" at the top, and the privacy policy names the person responsible with a postal and mail address.
- The project builds with Gradle. `./gradlew check` runs the tests and the scenario check, where a scenario counts as covered by a test named after it or a `// Scenario:` / `# Scenario:` mark. `.github/scripts/store-listing-check.sh` with its `-test.sh` checks the listing in shell.
- The repository is public. GitHub Pages is not enabled yet. knoopbyknoop.com is registered at Hostinger, which also handles the mail (MX records); its address record points at a Hostinger server.
- Nothing in the apps links to the website yet, and this change keeps it that way.

## Goals / Non-Goals

**Goals:**
- One source for each text: the home page reads the store texts and screenshots, and the English legal pages read `PRIVACY.md` and `TERMS.md`, so the site never drifts from the stores or the repository.
- Pages that work without JavaScript, load fast on a phone, and follow the visitor's light or dark setting.
- Checks in `./gradlew check`, so a missing page or a stale translation fails before anything is published.

**Non-Goals:**
- A content management system, a theme framework or a JavaScript build. The site is a handful of pages.
- Choosing the visitor's language automatically. English is at the root and the language switch is one tap away.

## Decisions

### A small Kotlin generator in a Gradle module `website`
`website/` holds the page sources and a Kotlin JVM program that writes the finished site to `website/build/site/`, run by `./gradlew :website:site`. It reads:
- `fastlane/` for the home page: name, subtitle, promotional text and description from the iOS metadata, the screenshots from the iOS screenshots;
- `PRIVACY.md` and `TERMS.md` for the English legal pages;
- `website/content/<lang>/` for the help pages, the legal notice, and the German, Dutch and French legal texts, as Markdown;
- `website/static/` for the stylesheet, the icon, the store badges and the 404 page.

Markdown becomes HTML through commonmark-java with its tables extension, since the privacy policy has a table. One HTML template wraps every page with the header, the language switch, the footer and `hreflang` links between the four versions of a page.

The generator is tested like the rest of the project, with JUnit tests named after the scenarios. They run the generator on the real repository content, so `./gradlew check` fails on a missing page or a stale translation.

**Alternatives:**
- **Jekyll on GitHub Pages:** it builds Markdown without our own code. But reading the store texts from `fastlane/` would need copies or plugins GitHub Pages doesn't run, and its checks would live outside `./gradlew check`.
- **Hand-written HTML:** 20 pages with the store description copied into four of them would drift from the stores.
- **A shell or Python script:** the project's checks and tests are Kotlin and JUnit. Python is only used for the icon and inside the listing check.

### Paths and languages
English is at `/`, the others at `/de/`, `/nl/` and `/fr/`. Each language has the same pages: `index.html`, `help/`, `privacy/`, `terms/` and `legal-notice/`. Paths stay in English in every language, so the language switch only swaps the prefix. The store locales map to the site's languages: `en-US` to English, `de-DE` to `de`, `nl-NL` to `nl` and `fr-FR` to `fr`.

### Translated legal texts carry the date they translate
Each translated `privacy.md` and `terms.md` starts with front matter `translates: 2026-11-13`. The generator reads the English date from the "Last updated:" line, and the check compares the two. Below the title, each translation says that the English version is binding and links to it. The translations are drafted from the English text and reviewed by the maintainer before publishing. The German review is by the maintainer; for Dutch and French, the maintainer decides whether a reviewer is needed.

### The legal notice is its own page
The legal notice (Impressum) names the person running the site with the postal and mail address from the privacy policy's "Who is responsible". It is a Markdown page per language. The privacy policy's "Who is responsible" and the legal notice must then be changed together; the check compares their address lines.

### Nothing from other sites
- **Fonts:** the system font stack; no web fonts.
- **Store badges:** Apple's and Google's official badges per language, downloaded once and kept in `website/static/badges/`.
- **Screenshots:** scaled by the generator to 600 pixels wide with the JDK's own image code, so a page stays at a few hundred KB.
- **Pages:** no scripts at all.

The check reads every generated HTML and CSS file and fails on any `src`, `srcset`, `<link href>`, `@import` or `url()` that points to another host. Ordinary `<a href>` links to other sites are allowed.

### Publishing with GitHub Actions to GitHub Pages
A workflow `website.yml` runs on pushes to main that touch `website/`, `fastlane/metadata/`, `fastlane/screenshots/`, `PRIVACY.md` or `TERMS.md`, and can also be started by hand. It runs the website's tests and the generator, then publishes `website/build/site/` with `actions/upload-pages-artifact` and `actions/deploy-pages`. A failed test stops the workflow before the deploy, so the previous site stays online.

GitHub Pages is set to "GitHub Actions" as its source, with the custom domain knoopbyknoop.com and enforced HTTPS. At Hostinger, the maintainer:
- replaces the domain's A records with GitHub Pages' four addresses (185.199.108.153 to 185.199.111.153);
- points `www` with a CNAME to `mkutz.github.io`;
- leaves the MX and other mail records as they are.

### Store URLs
In each iOS locale:
- `privacy_url.txt` becomes `https://knoopbyknoop.com/<prefix>privacy/`
- `support_url.txt` becomes `https://knoopbyknoop.com/<prefix>help/`

The prefix is empty for English and `de/`, `nl/` or `fr/` otherwise. `store-listing-check.sh` checks both against the locale. The URLs are only switched once the site is reachable under the domain. Then one run of Release store listing brings them to the App Store. The Google Play privacy URL is not part of the fastlane metadata, so the maintainer changes it in the Play Console.

### The privacy policy covers the website
A new section in `PRIVACY.md` says:
- the website is hosted by GitHub Pages;
- GitHub receives the visitor's IP address and request data, under GitHub's privacy statement;
- the site sets no cookies and has no analytics.

The translations get the same section.

## Risks / Trade-offs

- **Translations of legal texts can be wrong or stale.** The binding English version is named on every translation, and the date check catches stale ones. A wrong translation is only caught by review.
- **DNS switch:** for up to a day after the A records change, some visitors still reach the old Hostinger address, and HTTPS on GitHub Pages only works once its certificate is issued. The store URLs only change after the site is reachable with HTTPS, so the stores never point at a broken page.
- **Hostinger may host something on the domain today.** The maintainer checks before the switch; mail is not affected.
- **The legal notice publishes the postal address on the website.** It is already public in `PRIVACY.md`, and German law requires it for the site.
- **Store badge guidelines:** Apple's and Google's badges may only be used unchanged and with their usage rules. The generator embeds them as provided.
- **The screenshots are taken from the iPhone set only,** to keep one set per language. The Android ones look alike.
