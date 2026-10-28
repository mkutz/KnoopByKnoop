import KnoopByKnoopCore
import SwiftUI

@main
struct KnoopByKnoopApp: App {

  @UIApplicationDelegateAdaptor private var appDelegate: AppDelegate
  @StateObject private var model = AppModel()
  @Environment(\.scenePhase) private var scenePhase

  var body: some Scene {
    WindowGroup {
      PlannerView(model: model)
      .tint(.knoopGreen)
      .onAppear { appDelegate.downloads = model.downloads }
      .alert(
        model.message ?? "", isPresented: Binding(get: { model.message != nil }, set: { if !$0 { model.message = nil } })
      ) {
        Button("OK") { model.message = nil }
      }
      // While the app is visible, the navigation samples live, see `knot-navigation`.
      .onChange(of: scenePhase) { _, phase in Navigator.shared.visible = phase == .active }
    }
  }
}

/// Hands the system's background download events to the downloads, see `NetworkDownloads`.
final class AppDelegate: NSObject, UIApplicationDelegate {

  var downloads: NetworkDownloads?

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
