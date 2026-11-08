import KnoopByKnoopCore
import SwiftUI

/// Data dates, download state and attribution of the knot networks, see `network-sync` "Info
/// sheet".
struct InfoSheet: View {

  @ObservedObject var model: AppModel
  let onDone: () -> Void
  /// The appearance of the whole app, see `map-planning` "Light and dark appearance".
  @AppStorage("appearance") private var appearance = "system"
  /// Whether places useful on a ride are highlighted, see `map-planning` "Switch for places on the
  /// map".
  @AppStorage("places") private var places = true
  /// The language of the app, see `app-languages` "Language setting".
  @AppStorage(AppLanguage.storageKey) private var language = "system"

  var body: some View {
    NavigationStack {
      List {
        Section("Appearance") {
          Picker("Appearance", selection: $appearance) {
            Text("System").tag("system")
            Text("Light").tag("light")
            Text("Dark").tag("dark")
          }
          .pickerStyle(.segmented)
        }
        Section {
          Toggle(isOn: $places) {
            VStack(alignment: .leading) {
              Text("Places on the map")
              Text("Food, supermarkets, bike shops, stations, water and toilets")
                .font(.footnote)
                .foregroundStyle(.secondary)
            }
          }
        }
        Section("Language") {
          Picker("Language", selection: $language) {
            Text("System").tag("system")
            // Each language in itself, so it can be found whatever the app's language.
            Text(verbatim: "English").tag("en")
            Text(verbatim: "Deutsch").tag("de")
            Text(verbatim: "Nederlands").tag("nl")
            Text(verbatim: "Français").tag("fr")
          }
          .pickerStyle(.menu)
        }
        Section {
          ForEach(model.catalog.networks, id: \.network) { status in
            HStack {
              VStack(alignment: .leading) {
                Text(status.network == .cycling ? "Cycling" : "Hiking").font(.headline)
                Text(describe(status)).font(.subheadline).foregroundStyle(.secondary)
              }
              Spacer()
              if status.lastAttemptFailed { Button("Retry") { model.retrySync() } }
            }
          }
          // The downloaded ways, see `network-sync` "Path data in the info sheet".
          HStack {
            VStack(alignment: .leading) {
              Text("Ways for guidance").font(.headline)
              Text(waysDescription).font(.subheadline).foregroundStyle(.secondary)
            }
            Spacer()
            if model.pathDataDate != nil, model.pathDataBytes > 0 {
              Button("Delete") { model.deletePathData() }
                .accessibilityLabel(Text("Delete downloaded ways"))
            }
          }
          if model.needsAppUpdate {
            Text("Newer network data needs a newer version of the app. Please update it.")
              .foregroundStyle(.red)
          }
        } header: {
          Text("Downloaded data")
        } footer: {
          Text(
            "Guides you back to the route along suitable paths, also without a connection. The app downloads the ways around planned cycling routes from GitHub, so GitHub can see the rough area of a route, but never where you are."
          )
        }
        Section {
          Text(
            "Knot network data © OpenStreetMap contributors, available under the Open Database License (ODbL 1.0), openstreetmap.org/copyright"
          )
          .font(.footnote)
          Text(
            "Search by Photon (komoot), data © OpenStreetMap contributors. What you type and the map's center are sent to photon.komoot.io."
          )
          .font(.footnote)
        }
      }
      .navigationTitle("Info")
      .navigationBarTitleDisplayMode(.inline)
      .toolbar { Button("Done", action: onDone) }
    }
    // The navigation bar keeps its title until rebuilt, so it follows a new language too.
    .id(language)
  }

  /// The ways' data date and size in MB with one decimal, or that none are downloaded.
  private var waysDescription: String {
    guard let date = model.pathDataDate, model.pathDataBytes > 0 else {
      return String(localized: "None downloaded", bundle: AppLanguage.bundle)
    }
    let megabytes = (Double(model.pathDataBytes) / 1_000_000).formatted(
      .number.precision(.fractionLength(1)).locale(AppLanguage.locale))
    return String(localized: "Data of \(date)", bundle: AppLanguage.bundle) + " · \(megabytes) MB"
  }

  private func describe(_ status: NetworkStatus) -> String {
    let data = status.dataDate.map { String(localized: "Data of \($0)", bundle: AppLanguage.bundle) } ?? String(localized: "Not installed yet", bundle: AppLanguage.bundle)
    let state: String? =
      if status.lastAttemptFailed { String(localized: "the last download failed", bundle: AppLanguage.bundle) }
      else if status.state == .downloading { String(localized: "downloading", bundle: AppLanguage.bundle) }
      else if !status.hasGeometry && !model.online { String(localized: "courses need an internet connection", bundle: AppLanguage.bundle) }
      else if !status.hasGeometry { String(localized: "courses are loading", bundle: AppLanguage.bundle) }
      else if status.state == .updatable { String(localized: "update pending", bundle: AppLanguage.bundle) }
      else { nil }
    return [data, state].compactMap { $0 }.joined(separator: ", ")
  }
}

/// Shown while the courses of the selected network are missing, see `network-sync`.
struct CoursesNote: View {

  let online: Bool

  var body: some View {
    LoadingNote(text: online ? LocalizedStringKey("Loading the courses…") : "The courses need an internet connection once")
  }
}

/// A short note over the map about something that is still loading.
struct LoadingNote: View {

  let text: LocalizedStringKey

  var body: some View {
    Text(text)
      .font(.callout)
      .padding(.horizontal, 12)
      .padding(.vertical, 6)
      .floatingBackground(in: Capsule())
  }
}
