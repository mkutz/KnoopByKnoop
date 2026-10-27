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

/// The search field floating at the bottom of the map, with the hits above it, so they grow over
/// the map and the keyboard does not hide them.
struct PlaceSearchView: View {

  @ObservedObject var model: AppModel
  var focused: FocusState<Bool>.Binding

  var body: some View {
    VStack(spacing: 4) {
      if model.online { results }
      HStack {
        Image(systemName: "magnifyingglass").foregroundStyle(.secondary)
        TextField(
          model.online ? "Search places" : "Search needs an internet connection",
          text: Binding(get: { model.online ? model.searchQuery : "" }, set: { model.search($0) })
        )
        .focused(focused)
        .submitLabel(.search)
        .disabled(!model.online)
        if !model.searchQuery.isEmpty && model.online {
          Button {
            model.clearSearch()
            focused.wrappedValue = false
          } label: {
            Image(systemName: "xmark.circle.fill").foregroundStyle(.secondary)
          }
        }
      }
      .padding(10)
      .floatingBackground(in: RoundedRectangle(cornerRadius: 12))
    }
    .frame(maxWidth: 480)
  }

  @ViewBuilder private var results: some View {
    switch model.searchResult {
    case .none: EmptyView()
    case .searching: message("Searching…")
    case .noHits: message("Nothing found")
    case .unavailable: message("Search is not available right now")
    case .hits(let hits):
      // As high as the hits where they fit, scrolling in the room there is where they do not.
      ViewThatFits(in: .vertical) {
        hitRows(hits)
        ScrollView { hitRows(hits) }
      }
      .frame(maxHeight: 320)
      .floatingBackground(in: RoundedRectangle(cornerRadius: 12))
    }
  }

  private func hitRows(_ hits: [PlaceHit]) -> some View {
    VStack(alignment: .leading, spacing: 0) {
      ForEach(Array(hits.enumerated()), id: \.offset) { index, hit in
        if index > 0 { Divider() }
        Button {
          model.choose(hit)
          focused.wrappedValue = false
        } label: {
          VStack(alignment: .leading, spacing: 2) {
            Text(hit.name).foregroundStyle(.primary)
            if !hit.description_.isEmpty {
              Text(hit.description_).font(.caption).foregroundStyle(.secondary)
            }
          }
          .frame(maxWidth: .infinity, alignment: .leading)
          .padding(.horizontal, 12)
          .padding(.vertical, 8)
          .contentShape(Rectangle())
        }
        // The default style tints the whole label with the accent color.
        .buttonStyle(.plain)
      }
    }
  }

  private func message(_ text: String) -> some View {
    Text(text)
      .frame(maxWidth: .infinity, alignment: .leading)
      .padding(12)
      .floatingBackground(in: RoundedRectangle(cornerRadius: 12))
  }
}
