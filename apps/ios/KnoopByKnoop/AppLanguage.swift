import AVFoundation
import Foundation
import KnoopByKnoopCore

/// The app's language, see `app-languages` "Language of the texts" and "Language setting": the one
/// chosen in the info sheet, or for System the per-app language set in the system settings, or
/// else the system's, as far as the app has it, otherwise English.
enum AppLanguage {

  /// The choice in the info sheet: `system`, `en`, `de`, `nl` or `fr`.
  static let storageKey = "language"

  static var choice: String { UserDefaults.standard.string(forKey: storageKey) ?? "system" }

  /// The language code of the texts, e.g. `de`.
  static var code: String {
    if choice != "system" { return choice }
    // Read now rather than from `Bundle.main.preferredLocalizations`, which keeps the languages of
    // the app's start, so going back to System applies without a restart.
    let localizations = Bundle.main.localizations.filter { $0 != "Base" }
    return Bundle.preferredLocalizations(
      from: localizations, forPreferences: UserDefaults.standard.stringArray(forKey: "AppleLanguages")
    ).first ?? "en"
  }

  /// For SwiftUI's lookup of its texts.
  static var locale: Locale { Locale(identifier: code) }

  /// The texts in that language, for those built in code with `String(localized:bundle:)`.
  static var bundle: Bundle {
    Bundle.main.path(forResource: code, ofType: "lproj").flatMap(Bundle.init(path:)) ?? .main
  }

  /// The language for the core's texts.
  static var current: Language { Language.companion.of(code: code) }

  /// Keeps the choice where the system's per-app language is, so what the system draws, e.g. the
  /// location prompt, follows from the next start; System leaves it to the system again.
  static func store() {
    if choice == "system" {
      UserDefaults.standard.removeObject(forKey: "AppleLanguages")
    } else {
      UserDefaults.standard.set([choice], forKey: "AppleLanguages")
    }
  }

  /// The language the notices are spoken in: the app's where the phone has a voice for it, else
  /// English, so an English voice never reads another language, see `app-languages` "Spoken
  /// notices in the language".
  static var speech: Language {
    AVSpeechSynthesisVoice(language: voiceCode(current)) == nil ? .en : current
  }

  /// The voice for `speech`.
  static var voice: AVSpeechSynthesisVoice? {
    AVSpeechSynthesisVoice(language: voiceCode(speech)) ?? AVSpeechSynthesisVoice(language: "en-GB")
  }

  private static func voiceCode(_ language: Language) -> String {
    switch language {
    case .de: "de-DE"
    case .nl: "nl-NL"
    case .fr: "fr-FR"
    default: "en-GB"
    }
  }
}
