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

extension Color {
  /// The green and orange of the app icon, see `design/icon`.
  static let knoopGreen = Color(red: 0x2E / 255, green: 0x9E / 255, blue: 0x5B / 255)
  static let knoopOrange = Color(red: 0xF2 / 255, green: 0x8C / 255, blue: 0x28 / 255)
}
