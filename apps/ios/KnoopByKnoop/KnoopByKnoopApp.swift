import KnoopByKnoopCore
import SwiftUI

@main
struct KnoopByKnoopApp: App {

  @UIApplicationDelegateAdaptor private var appDelegate: AppDelegate
  @StateObject private var model = AppModel()
  @Environment(\.scenePhase) private var scenePhase
  @AppStorage(AppLanguage.storageKey) private var language = "system"

  init() {
    Navigator.shared.endLeftoverSession()
  }

  var body: some Scene {
    WindowGroup {
      PlannerView(model: model)
      .tint(.knoopGreen)
      // Light or dark as chosen in the info sheet, also the status bar.
      .appAppearance()
      .onAppear { appDelegate.downloads = model.downloads }
      .alert(
        model.message ?? "", isPresented: Binding(get: { model.message != nil }, set: { if !$0 { model.message = nil } })
      ) {
        if model.messageOpensSettings {
          Button("Settings") {
            model.message = nil
            model.messageOpensSettings = false
            if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
          }
        }
        Button("OK") {
          model.message = nil
          model.messageOpensSettings = false
        }
      }
      // The language chosen in the info sheet applies right away, see `app-languages` "Language
      // setting".
      .onChange(of: language) {
        AppLanguage.store()
        model.languageChanged()
      }
      // While the app is visible, the navigation samples live, see `knot-navigation`.
      .onChange(of: scenePhase) { _, phase in Navigator.shared.visible = phase == .active }
    }
  }
}

/// Hands the system's background download events to the downloads, see `NetworkDownloads`.
final class AppDelegate: NSObject, UIApplicationDelegate {

  var downloads: NetworkDownloads?

  /// Closing the app while navigating ends navigation, so the location indicator and the Live
  /// Activity go away right then, see `knot-navigation` "Closing the app while navigating".
  func applicationWillTerminate(_ application: UIApplication) {
    MainActor.assumeIsolated { Navigator.shared.stopBeforeTermination() }
  }

  func application(
    _ application: UIApplication, handleEventsForBackgroundURLSession identifier: String,
    completionHandler: @escaping () -> Void
  ) {
    if identifier == NetworkDownloads.sessionIdentifier {
      downloads?.backgroundCompletionHandler = completionHandler
      downloads?.reconnect()
    } else {
      completionHandler()
    }
  }
}
