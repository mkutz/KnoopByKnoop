# Tasks

## 1. Core

- [x] 1.1 Move the course-related setup of `Navigation.start` into one private function; `start` uses it. `NavigationTest` and `RealRideTest` pass unchanged.
- [x] 1.2 `Navigation.withCourses(geometry, fix)`: a new setup, the ride state kept, `nextIndex` moved up without notices when the rider's leg is beyond it, pace and ferry watch re-based. Tests in `NavigationSessionTest`, on the synthetic network:
  - "Courses arriving on the course": next knot unchanged, the course ahead drawn along the course, no notices.
  - "Knots passed before the courses": passed knots stay, none announced again.
  - "Off route when the courses arrive": off route with its notice on the next fix.
  - A ferry already warned about isn't warned again after the courses arrive.
- [x] 1.3 `NavigationSession.withCourses(geometry)`: before the first fix only the route's geometry; after it the navigation's; nothing after the end or a second time. A test for each.

## 2. Android

- [x] 2.1 `AppViewModel` puts the courses into `Navigating` when they finish loading while navigating; `NavigationService` takes them into the session and acts on the step.
- [x] 2.2 Check on the emulator: Navigate pressed right after starting the app, before "startup: courses", with the recorded ride `siegburg-going-on` replayed. A few seconds after the courses have loaded, the course ahead is drawn and leaving the route is told as off route ("Courses on a slow phone"). List the scenario in `openspec/checked-by-hand.md`.

## 3. iOS

- [x] 3.1 `AppModel` hands the courses to `Navigator.takeCourses` when they finish loading while navigating. The project builds. The check on the iPhone is the maintainer's; the courses load in 0.7 s there, so it is hard to hit by hand.

## 4. Wrap-up

- [x] 4.1 A changelog line under Unreleased: "Navigation started before the network's courses had loaded takes them as soon as they are there, instead of going on without them for the whole ride". `./gradlew check` and `npx @fission-ai/openspec validate --all` pass, and `./gradlew checkScenarios -Pchange=take-courses-during-navigation`.
