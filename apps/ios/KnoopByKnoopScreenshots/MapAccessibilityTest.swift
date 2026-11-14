import XCTest

/// Covers `map-planning` "Map buttons for screen readers" on the iPhone: what XCUITest finds is
/// what VoiceOver reaches.
final class MapAccessibilityTest: XCTestCase {

  private let routeFile = URL(fileURLWithPath: #filePath)
    .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
    .deletingLastPathComponent().appendingPathComponent("fastlane/screenshots/demo-route.knoop")

  // Scenario: Map buttons with VoiceOver
  func testMapButtonsWithVoiceOver() {
    let labels = [
      "en": ["Search places", "My position", "Zoom in", "Zoom out"],
      "de": ["Orte suchen", "Meine Position", "Vergrößern", "Verkleinern"],
    ]
    for (language, buttons) in labels {
      let app = XCUIApplication()
      app.launchArguments = ["-language", language, "-AppleLanguages", "(\(language))"]
      app.launch()
      app.open(routeFile)
      XCTAssertTrue(app.buttons["navigate"].firstMatch.waitForExistence(timeout: 180), "no route")
      for label in buttons {
        let button = app.buttons[label].firstMatch
        XCTAssertTrue(button.waitForExistence(timeout: 10), "VoiceOver does not reach \(label)")
        XCTAssertTrue(button.isHittable, "\(label) cannot be activated")
      }
      app.terminate()
    }
  }
}
