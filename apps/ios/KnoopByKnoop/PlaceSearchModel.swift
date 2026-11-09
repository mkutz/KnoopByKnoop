import Foundation
import KnoopByKnoopCore

/// The place search's text and hits, observed by the search view alone: published on `AppModel`,
/// every keystroke drew the planner, the map and the waypoints again, see `place-search` "Typing
/// without delay".
@MainActor
final class PlaceSearchModel: ObservableObject {

  /// What a search needs from the rest of the app when it starts.
  struct Context {
    let locator: KnotLocator
    let area: GeoBounds
    let center: CameraPlace?
  }

  @Published private(set) var query = ""
  @Published private(set) var result: SearchResult = .none
  private var task: Task<Void, Never>?

  /// The locator, the network's area and the map's center now; `nil` while no network is loaded.
  var context: () -> Context? = { nil }

  /// Searches `query` once the user stopped typing for a moment; newer queries replace older ones.
  func search(_ newQuery: String) {
    // The field passes its text again when it loses the focus, e.g. after a hit was chosen, which
    // must not bring the hits back.
    guard newQuery != query else { return }
    task?.cancel()
    query = newQuery
    guard newQuery.trimmingCharacters(in: .whitespaces).count >= Int(PlaceSearch.shared.MIN_QUERY_LENGTH),
      let context = context()
    else {
      result = .none
      return
    }
    task = Task {
      try? await Task.sleep(for: .milliseconds(300))
      guard !Task.isCancelled else { return }
      result = .searching
      let area = context.area
      // The app's language rather than the system's, see `place-search` "Hits in the app's language".
      let requests = PlaceSearch.shared.requests(
        query: newQuery,
        centerLatitude: context.center?.latitude ?? (area.minLatitude + area.maxLatitude) / 2,
        centerLongitude: context.center?.longitude ?? (area.minLongitude + area.maxLongitude) / 2,
        area: area, language: AppLanguage.code)
      async let prominent = PlaceSearchClient.fetch(requests.prominent)
      async let nearby = PlaceSearchClient.fetch(requests.nearby)
      let responses = await (prominent, nearby)
      guard !Task.isCancelled else { return }
      // Off the main thread: on the phone, reading the answers takes long enough to hold up taps.
      nonisolated(unsafe) let network = context.locator
      let started = Date()
      let merged: [PlaceHit]? = await Task.detached {
        try? PlaceSearch.shared.merge(prominent: responses.0, nearby: responses.1, network: network)
      }.value
      DebugTiming.note("search \"\(newQuery)\" merged in \(Int(Date().timeIntervalSince(started) * 1000)) ms")
      guard !Task.isCancelled else { return }
      // Neither request was answered when merging fails.
      if let hits = merged {
        result = hits.isEmpty ? .noHits : .hits(hits)
      } else {
        result = .unavailable
      }
    }
  }

  /// Searches the text in the field again, e.g. when the search opens with it or on the keyboard's
  /// search key, see `place-search` "Typing on iOS".
  func searchAgain() {
    let current = query
    query = ""
    search(current)
  }

  /// A hit was chosen: its name stays in the field, the hits go.
  func chosen(_ name: String) {
    task?.cancel()
    query = name
    result = .none
  }

  func clear() {
    task?.cancel()
    query = ""
    result = .none
  }
}
