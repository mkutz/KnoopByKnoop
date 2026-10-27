import KnoopByKnoopCore
import SwiftUI

@main
struct KnoopByKnoopApp: App {

  @UIApplicationDelegateAdaptor private var appDelegate: AppDelegate
  @StateObject private var model = AppModel()

  var body: some Scene {
    WindowGroup {
      Group {
        switch model.screen {
        case .regions: RegionsView(model: model)
        case .planner: PlannerView(model: model)
        }
      }
      .tint(.knoopGreen)
      .onAppear { appDelegate.downloads = model.downloads }
      .alert(
        model.message ?? "", isPresented: Binding(get: { model.message != nil }, set: { if !$0 { model.message = nil } })
      ) {
        Button("OK") { model.message = nil }
      }
    }
  }
}

/// Hands the system's background download events to the downloads, see `RegionDownloads`.
final class AppDelegate: NSObject, UIApplicationDelegate {

  var downloads: RegionDownloads?

  func application(
    _ application: UIApplication, handleEventsForBackgroundURLSession identifier: String,
    completionHandler: @escaping () -> Void
  ) {
    if identifier == RegionDownloads.sessionIdentifier {
      downloads?.backgroundCompletionHandler = completionHandler
      downloads?.reconnect()
    } else {
      completionHandler()
    }
  }
}
