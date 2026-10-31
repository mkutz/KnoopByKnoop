import SwiftUI

/// The appearance chosen in the info sheet, by default like the system, see `map-planning` "Light
/// and dark appearance". Applied to the root and to every sheet and popover, as each presentation
/// keeps the system's appearance otherwise.
private struct AppAppearance: ViewModifier {

  @AppStorage("appearance") private var appearance = "system"

  func body(content: Content) -> some View {
    content.preferredColorScheme(appearance == "light" ? .light : appearance == "dark" ? .dark : nil)
  }
}

extension View {
  /// Light or dark as chosen in the info sheet.
  func appAppearance() -> some View { modifier(AppAppearance()) }
}
