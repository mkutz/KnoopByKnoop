import CoreLocation
import XCTest

/// Takes the five store screenshots of the iPhone app in one language, see `store-screenshots`:
/// the demo route planned, navigating, its waypoint list, a tapped place and dark mode. The
/// language comes from `SCREENSHOT_LANGUAGE` (e.g. `de`), passed by the workflow as
/// `TEST_RUNNER_SCREENSHOT_LANGUAGE`; the clock is set by the workflow before the run. The
/// screenshots are attachments named `<n>-<name>`, copied out of the result bundle.
final class StoreScreenshots: XCTestCase {

  private let repository = URL(fileURLWithPath: #filePath)
    .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
    .deletingLastPathComponent()

  private var language: String {
    ProcessInfo.processInfo.environment["SCREENSHOT_LANGUAGE"] ?? "en"
  }

  /// The values of `fastlane/screenshots/demo.env`.
  private lazy var demo: [String: String] = {
    let text = (try? String(
      contentsOf: repository.appendingPathComponent("fastlane/screenshots/demo.env"), encoding: .utf8))
      ?? ""
    var values: [String: String] = [:]
    for line in text.split(separator: "\n") where !line.hasPrefix("#") && line.contains("=") {
      let parts = line.split(separator: "=", maxSplits: 1)
      values[String(parts[0])] = parts[1].trimmingCharacters(in: CharacterSet(charactersIn: "\""))
    }
    return values
  }()

  private var routeFile: URL {
    repository.appendingPathComponent("fastlane/screenshots/demo-route.knoop")
  }

  override func setUp() {
    continueAfterFailure = false
  }

  func testScreenshots() throws {
    let positions = try XCTUnwrap(demo["RIDE_POSITIONS"]).split(separator: " ").map(location)
    let placeSearch = try XCTUnwrap(demo["PLACE_SEARCH"])
    let placeName = try XCTUnwrap(demo["PLACE_NAME"])
    XCUIDevice.shared.location = XCUILocation(location: positions[0])

    let app = launch(appearance: "light")

    // 1: the planned route with the panel.
    openRoute(app)
    shot(app, "1-route")

    // 3: the waypoint list, with the panel pulled up.
    let length = app.staticTexts["11 km"].firstMatch
    XCTAssertTrue(length.waitForExistence(timeout: 30))
    // Dragged from the route's length, which is on the panel; above it is the map.
    length.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
      .press(
        forDuration: 0.2,
        thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.1)))
    pause(2)
    shot(app, "3-waypoints")

    // 2: navigating between 03 and 21, started on the route nearby.
    for position in positions {
      XCUIDevice.shared.location = XCUILocation(location: position)
      pause(1)
    }
    // The question needs the courses and a fresh position, which a fresh install may not have yet:
    // navigation then starts towards the start, and is stopped to ask again.
    var asked = false
    for _ in 0..<5 {
      for position in positions {
        XCUIDevice.shared.location = XCUILocation(location: position)
        pause(1)
      }
      app.buttons["navigate"].firstMatch.tap()
      allowSystemAlert()
      let nearby = app.buttons["startNearby"].firstMatch
      if nearby.waitForExistence(timeout: 15) {
        nearby.tap()
        asked = true
        break
      }
      let stop = app.buttons["stop"].firstMatch
      if stop.exists { stop.tap() }
      pause(20)
    }
    XCTAssertTrue(asked, "no question to start nearby")
    pause(3)
    noClosedFerry(app)
    XCTAssertTrue(app.buttons["stop"].firstMatch.waitForExistence(timeout: 30))
    for position in positions {
      XCUIDevice.shared.location = XCUILocation(location: position)
      pause(2)
    }
    shot(app, "2-navigate")
    app.buttons["stop"].firstMatch.tap()
    pause(2)

    // 4: a highlighted place, found by search so the map centres on it, then tapped. Started
    // again, so the panel is down and the map's buttons, the search among them, show.
    app.terminate()
    let planner = launch(appearance: "light")
    openRoute(planner)
    // The public search service may be slow or busy; searched again up to three times.
    let result = planner.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", placeName))
      .element(boundBy: 0)
    for _ in 0..<3 {
      let field = planner.textFields["searchField"].firstMatch
      if !field.exists {
        // A UIKit button in the map's glass group, which XCUITest does not list as a button.
        planner.descendants(matching: .any).matching(identifier: "search").firstMatch.tap()
        XCTAssertTrue(field.waitForExistence(timeout: 10), "no search field")
      }
      field.tap()
      if let typed = field.value as? String, !typed.isEmpty, typed != field.placeholderValue {
        field.typeText(String(repeating: XCUIKeyboardKey.delete.rawValue, count: typed.count))
      }
      // Searched once with Return, so the public search service gets few requests.
      field.typeText(placeSearch + "\n")
      if result.waitForExistence(timeout: 30) { break }
      pause(10)
    }
    XCTAssertTrue(result.exists, "\(placeName) not found by search")
    result.tap()
    pause(3)
    // The map centres the hit in the part of the screen above the panel, about halfway down.
    planner.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.52)).tap()
    // The place's card, not a spot's: both offer "Add to route".
    let card = planner.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", placeName))
      .element(boundBy: 0)
    XCTAssertTrue(card.waitForExistence(timeout: 10), "no card for \(placeName)")
    XCTAssertTrue(planner.buttons["addToRoute"].firstMatch.exists, "no place card")
    shot(planner, "4-place")

    // 5: the planned route in dark mode.
    planner.terminate()
    let dark = launch(appearance: "dark")
    openRoute(dark)
    shot(dark, "5-dark")
  }

  /// Allows the system's question shown now, e.g. for notifications on the first Navigate, by its
  /// second button, Allow, in any language.
  private func allowSystemAlert() {
    let alert = XCUIApplication(bundleIdentifier: "com.apple.springboard").alerts.firstMatch
    guard alert.waitForExistence(timeout: 5), alert.buttons.count > 1 else { return }
    alert.buttons.element(boundBy: 1).tap()
  }

  private func launch(appearance: String) -> XCUIApplication {
    let app = XCUIApplication()
    app.launchArguments = [
      "-language", language, "-AppleLanguages", "(\(language))", "-appearance", appearance,
    ]
    app.launch()
    return app
  }

  /// Opens the demo route as from a file, with the map fitted to it and the panel down.
  private func openRoute(_ app: XCUIApplication) {
    app.open(routeFile)
    XCTAssertTrue(app.buttons["navigate"].firstMatch.waitForExistence(timeout: 180), "no route")
    // The map along the route is loaded after the route shows.
    pause(10)
  }

  /// Fails with the question's text when Navigate asks about a closed ferry, see
  /// `store-screenshots` "Closed ferry".
  private func noClosedFerry(_ app: XCUIApplication) {
    if app.staticTexts["closedFerry"].exists {
      let texts = app.staticTexts.allElementsBoundByIndex.map(\.label).joined(separator: " ")
      XCTFail("a ferry of the demo route is closed at the screenshots' moment: \(texts)")
    }
  }

  /// Waits until the screen stops changing (map tiles loaded): three equal screenshots 3 s apart,
  /// at most 90 seconds, then attaches it.
  private func shot(_ app: XCUIApplication, _ name: String) {
    var previous = Data()
    var same = 0
    var screenshot = XCUIScreen.main.screenshot()
    for _ in 0..<30 {
      pause(3)
      screenshot = XCUIScreen.main.screenshot()
      let current = screenshot.pngRepresentation
      same = current == previous ? same + 1 : 0
      if same >= 2 { break }
      previous = current
    }
    let attachment = XCTAttachment(screenshot: screenshot)
    attachment.name = name
    attachment.lifetime = .keepAlways
    add(attachment)
  }

  private func location(_ text: Substring) -> CLLocation {
    let parts = text.split(separator: ",").compactMap { Double($0) }
    return CLLocation(latitude: parts[0], longitude: parts[1])
  }

  private func pause(_ seconds: Double) {
    Thread.sleep(forTimeInterval: seconds)
  }
}
