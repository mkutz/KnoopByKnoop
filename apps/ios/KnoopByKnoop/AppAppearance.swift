import SwiftUI

/// The appearance chosen in the info sheet, by default like the system, see `map-planning` "Light
/// and dark appearance". Set on the windows, so every sheet and popover in them follows it. Not with
/// `preferredColorScheme`: an open sheet kept light when switching from Light back to System while
/// the system was dark, as `nil` does not undo an earlier choice there.
private struct AppAppearance: ViewModifier {

  @AppStorage("appearance") private var appearance = "system"

  func body(content: Content) -> some View {
    content.onChange(of: appearance, initial: true) { _, appearance in
      let style: UIUserInterfaceStyle =
        appearance == "light" ? .light : appearance == "dark" ? .dark : .unspecified
      for case let scene as UIWindowScene in UIApplication.shared.connectedScenes {
        for window in scene.windows { window.overrideUserInterfaceStyle = style }
      }
    }
  }
}

extension View {
  /// Light or dark as chosen in the info sheet.
  func appAppearance() -> some View { modifier(AppAppearance()) }
}
