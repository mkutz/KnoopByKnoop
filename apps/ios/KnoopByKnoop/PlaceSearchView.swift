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

/// The search field over the map and the hits right below it, so the keyboard does not hide them.
struct PlaceSearchView: View {

  @ObservedObject var model: AppModel
  @FocusState private var focused: Bool

  var body: some View {
    VStack(spacing: 4) {
      HStack {
        Image(systemName: "magnifyingglass").foregroundStyle(.secondary)
        TextField(
          model.online ? "Search places" : "Search needs an internet connection",
          text: Binding(get: { model.online ? model.searchQuery : "" }, set: { model.search($0) })
        )
        .focused($focused)
        .submitLabel(.search)
        .disabled(!model.online)
        if !model.searchQuery.isEmpty && model.online {
          Button {
            model.clearSearch()
            focused = false
          } label: {
            Image(systemName: "xmark.circle.fill").foregroundStyle(.secondary)
          }
        }
      }
      .padding(10)
      .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
      if model.online { results }
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
      ScrollView {
        VStack(alignment: .leading, spacing: 0) {
          ForEach(Array(hits.enumerated()), id: \.offset) { _, hit in
            Button {
              model.choose(hit)
              focused = false
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
            }
            Divider()
          }
        }
      }
      .frame(maxHeight: 320)
      .fixedSize(horizontal: false, vertical: true)
      .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
    }
  }

  private func message(_ text: String) -> some View {
    Text(text)
      .frame(maxWidth: .infinity, alignment: .leading)
      .padding(12)
      .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
  }
}
