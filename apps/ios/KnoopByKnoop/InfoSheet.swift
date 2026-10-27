import KnoopByKnoopCore
import SwiftUI

/// Data dates, download state and attribution of the knot networks, see `network-sync` "Info
/// sheet".
struct InfoSheet: View {

  @ObservedObject var model: AppModel
  let onDone: () -> Void

  var body: some View {
    NavigationStack {
      List {
        Section("Knot network data") {
          ForEach(model.catalog.networks, id: \.network) { status in
            HStack {
              VStack(alignment: .leading) {
                Text(status.network.name.lowercased().capitalized).font(.headline)
                Text(describe(status)).font(.subheadline).foregroundStyle(.secondary)
              }
              Spacer()
              if status.lastAttemptFailed { Button("Retry") { model.retrySync() } }
            }
          }
          if model.needsAppUpdate {
            Text("Newer network data needs a newer version of the app. Please update it.")
              .foregroundStyle(.red)
          }
        }
        Section {
          Text(
            "Knot network data © OpenStreetMap contributors, available under the Open Database License "
              + "(ODbL 1.0), openstreetmap.org/copyright"
          )
          .font(.footnote)
          Text(
            "Search by Photon (komoot), data © OpenStreetMap contributors. What you type and the map's "
              + "center are sent to photon.komoot.io."
          )
          .font(.footnote)
        }
      }
      .navigationTitle("Info")
      .navigationBarTitleDisplayMode(.inline)
      .toolbar { Button("Done", action: onDone) }
    }
  }

  private func describe(_ status: NetworkStatus) -> String {
    let data = status.dataDate.map { "Data of \($0)" } ?? "Not installed yet"
    let state: String? =
      if status.lastAttemptFailed { "the last download failed" }
      else if status.state == .downloading { "downloading" }
      else if !status.hasGeometry && !model.online { "courses need an internet connection" }
      else if !status.hasGeometry { "courses are loading" }
      else if status.state == .updatable { "update pending" }
      else { nil }
    return [data, state].compactMap { $0 }.joined(separator: ", ")
  }
}

/// Shown while the courses of the selected network are missing, see `network-sync`.
struct CoursesNote: View {

  let online: Bool

  var body: some View {
    LoadingNote(text: online ? "Loading the courses…" : "The courses need an internet connection once")
  }
}

/// A short note over the map about something that is still loading.
struct LoadingNote: View {

  let text: String

  var body: some View {
    Text(text)
      .font(.callout)
      .padding(.horizontal, 12)
      .padding(.vertical, 6)
      .floatingBackground(in: Capsule())
  }
}
