import KnoopByKnoopCore
import SwiftUI

/// What the place search shows below its field, see `place-search`.
enum SearchResult {
  /// Nothing to show: no query, or a hit was chosen.
  case none
  case searching
  case hits([PlaceHit])
  case noHits
  /// Photon did not answer or answered with an error.
  case unavailable
}

/// Sends the place search's requests to Photon.
enum PlaceSearchClient {

  /// Photon asks to be told who is calling.
  private static let userAgent = "knoopbyknoop-ios (https://github.com/mkutz/knoopbyknoop)"

  /// The response, or `nil` when Photon did not answer or answered with an error.
  static func fetch(_ url: String) async -> String? {
    guard let url = URL(string: url) else { return nil }
    var request = URLRequest(url: url)
    request.timeoutInterval = 10
    request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
    guard let (data, response) = try? await URLSession.shared.data(for: request),
      (response as? HTTPURLResponse)?.statusCode == 200
    else { return nil }
    return String(decoding: data, as: UTF8.self)
  }
}

/// The search field with its hits in the panel: the field at the top with Cancel, the hits below it
/// on the panel's own background, see `place-search` "Search while typing".
struct PlaceSearchView: View {

  @ObservedObject var model: AppModel
  /// Closes the search: after a hit is picked, or with Cancel.
  var onClose: () -> Void
  /// Its own, as a focus set from the view behind the sheet does not reach into it.
  @FocusState private var focused: Bool

  var body: some View {
    VStack(alignment: .leading, spacing: 12) {
      HStack(spacing: 12) {
        field
          .padding(10)
          .background(Color(.tertiarySystemFill), in: RoundedRectangle(cornerRadius: 12))
        Button("Cancel") {
          model.clearSearch()
          onClose()
        }
      }
      if model.online {
        switch model.searchResult {
        case .none: EmptyView()
        case .searching: Text("Searching…").foregroundStyle(.secondary)
        case .noHits: Text("Nothing found").foregroundStyle(.secondary)
        case .unavailable: Text("Search is not available right now").foregroundStyle(.secondary)
        case .hits(let hits): ScrollView { hitRows(hits) }
        }
      }
      Spacer(minLength: 0)
    }
    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    // Once the sheet has risen, so the keyboard comes up with the field in view, see `place-search`
    // "Typing on iOS".
    .task {
      try? await Task.sleep(for: .milliseconds(400))
      focused = true
    }
  }

  private var field: some View {
    HStack {
      Image(systemName: "magnifyingglass").foregroundStyle(.secondary)
      TextField(
        model.online ? LocalizedStringKey("Search places") : "Search needs an internet connection",
        text: Binding(get: { model.online ? model.searchQuery : "" }, set: { model.search($0) })
      )
      .focused($focused)
      .submitLabel(.search)
      .disabled(!model.online)
      // Empties the field to type again; Cancel closes the search.
      if !model.searchQuery.isEmpty && model.online {
        Button {
          model.clearSearch()
        } label: {
          Image(systemName: "xmark.circle.fill").foregroundStyle(.secondary)
        }
        .accessibilityLabel("Clear")
      }
    }
  }

  private func hitRows(_ hits: [PlaceHit]) -> some View {
    VStack(alignment: .leading, spacing: 0) {
      ForEach(Array(hits.enumerated()), id: \.offset) { index, hit in
        if index > 0 { Divider() }
        Button {
          model.choose(hit)
          focused = false
          onClose()
        } label: {
          VStack(alignment: .leading, spacing: 2) {
            Text(hit.name).foregroundStyle(.primary)
            if !hit.description_.isEmpty {
              Text(hit.description_).font(.caption).foregroundStyle(.secondary)
            }
          }
          .frame(maxWidth: .infinity, alignment: .leading)
          .padding(.vertical, 10)
          .contentShape(Rectangle())
        }
        // The default style tints the whole label with the accent color.
        .buttonStyle(.plain)
      }
    }
  }
}
