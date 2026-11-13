import SwiftUI
import UIKit

/// A file written for sharing: the GPX export or a route file, see `map-planning` "Export the route as
/// GPX" and `route-sharing` "Share the route".
struct SharedFile: Identifiable {
  let url: URL
  var id: URL { url }

  /// Writes [content] to the temporary directory as [fileName]; only the latest export is kept.
  static func write(fileName: String, content: String) -> SharedFile? {
    let directory = FileManager.default.temporaryDirectory.appendingPathComponent("export")
    try? FileManager.default.removeItem(at: directory)
    do {
      try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
      let url = directory.appendingPathComponent(fileName)
      try content.write(to: url, atomically: true, encoding: .utf8)
      return SharedFile(url: url)
    } catch {
      print("Writing \(fileName) failed: \(error)")
      return nil
    }
  }
}

/// The system share sheet for a file: Save to Files, AirDrop, messengers and apps that open it.
struct ShareSheet: UIViewControllerRepresentable {

  let url: URL

  func makeUIViewController(context: Context) -> UIActivityViewController {
    UIActivityViewController(activityItems: [url], applicationActivities: nil)
  }

  func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}
