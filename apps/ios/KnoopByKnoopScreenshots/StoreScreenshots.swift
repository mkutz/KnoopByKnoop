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
    // The system's questions, e.g. for notifications on the first Navigate: allowed, in any
    // language, by their default button.
    addUIInterruptionMonitor(withDescription: "System alert") { alert in
      let allow = alert.buttons.element(boundBy: alert.buttons.count > 1 ? 1 : 0)
      guard allow.exists else { return false }
      allow.tap()
      return true
    }
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
    length.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: -1))
      .press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.1)))
    pause(2)
    shot(app, "3-waypoints")

    // 2: navigating between 03 and 21, started on the route nearby.
    for position in positions {
      XCUIDevice.shared.location = XCUILocation(location: position)
      pause(1)
    }
    app.buttons["navigate"].firstMatch.tap()
    let nearby = app.buttons["startNearby"].firstMatch
    XCTAssertTrue(nearby.waitForExistence(timeout: 30), "no question to start nearby")
    nearby.tap()
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

    // 4: a highlighted place, found by search so the map centres on it, then tapped.
    openRoute(app)
    app.buttons["search"].firstMatch.tap()
    let field = app.textFields["searchField"].firstMatch
    XCTAssertTrue(field.waitForExistence(timeout: 10))
    field.tap()
    // Searched once with Return, so the public search service gets few requests.
    field.typeText(placeSearch + "\n")
    let result = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH %@", placeName))
      .element(boundBy: 0)
    XCTAssertTrue(result.waitForExistence(timeout: 30), "\(placeName) not found by search")
    result.tap()
    pause(3)
    let map = app.otherElements.matching(identifier: "map").firstMatch
    let centre = map.exists ? map : app
    centre.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: map.exists ? 0.5 : 0.3)).tap()
    XCTAssertTrue(app.buttons["addToRoute"].firstMatch.waitForExistence(timeout: 10), "no place card")
    shot(app, "4-place")

    // 5: the planned route in dark mode.
    app.terminate()
    let dark = launch(appearance: "dark")
    openRoute(dark)
    shot(dark, "5-dark")
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
