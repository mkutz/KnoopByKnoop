import SwiftUI

/// The appearance chosen in the info sheet, by default like the system, see `map-planning` "Light
/// and dark appearance". Set on the windows, so every sheet and popover in them follows it. Not with
/// `preferredColorScheme`: an open sheet kept light when switching from Light back to System while
/// the system was dark, as `nil` does not undo an earlier choice there.
///
/// The language chosen there too, see `app-languages` "Language setting": SwiftUI looks up the
/// texts of every sheet and popover in it, as each presentation keeps the system's otherwise.
private struct AppAppearance: ViewModifier {

  @AppStorage("appearance") private var appearance = "system"
  @AppStorage(AppLanguage.storageKey) private var language = "system"

  func body(content: Content) -> some View {
    // `language` is read here so the body follows a new choice; `AppLanguage` reads it too.
    content
    .environment(\.locale, language == "system" ? AppLanguage.locale : Locale(identifier: language))
    .onChange(of: appearance, initial: true) { _, appearance in
      let style: UIUserInterfaceStyle =
        appearance == "light" ? .light : appearance == "dark" ? .dark : .unspecified
      for case let scene as UIWindowScene in UIApplication.shared.connectedScenes {
        for window in scene.windows { window.overrideUserInterfaceStyle = style }
      }
    }
  }
}

extension View {
  /// Light or dark, and the language, as chosen in the info sheet.
  func appAppearance() -> some View { modifier(AppAppearance()) }
}
