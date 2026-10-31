import AVFoundation
import Foundation
import KnoopByKnoopCore

/// The app's language, see `app-languages` "Language of the texts": the per-app language set in
/// the system settings, or else the system's, as far as the app has it, otherwise English.
enum AppLanguage {

  /// The language the bundle resolved from the user's choices and the app's localizations.
  static var code: String { Bundle.main.preferredLocalizations.first ?? "en" }

  /// The language for the core's texts.
  static var current: Language { Language.companion.of(code: code) }

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
