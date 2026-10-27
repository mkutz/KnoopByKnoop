import Foundation
import KnoopByKnoopCore
import Network

/// The map layers of the planner: the network as GeoJSON files (large, parsed by the map off the
/// main thread), the route and waypoints as GeoJSON text.
struct MapLayers {
  let knots: URL
  let connections: URL
  let route: String
  let routeKnots: String
  let routeKnotList: [RouteKnot]
  let refs: Set<String>
}

/// The installed data of one network, prepared for planning and drawing.
private struct LoadedNetwork {
  let graphSha256: String
  let hasGeometry: Bool
  let geometry: NetworkGeometry
  let locator: KnotLocator
  let knots: URL
  let connections: URL
  let refs: Set<String>
}

/// A planning session with everything the screens show of it, prepared off the main thread.
private struct PreparedSession: @unchecked Sendable {
  let session: PlanningSession
  let description: RouteDescription
  let route: String
  let routeKnots: String
  let routeKnotMarkers: [RouteKnot]

  init(_ session: PlanningSession, geometry: NetworkGeometry) {
    self.session = session
    description = MapFeatures.shared.describe(session: session)
    route = MapFeatures.shared.route(session: session, geometry: geometry)
    routeKnots = MapFeatures.shared.routeKnotsLayer(session: session)
    routeKnotMarkers = MapFeatures.shared.routeKnotMarkers(session: session)
  }
}

/// Connects the planner with storage, the network sync and the core's planning logic.
@MainActor
final class AppModel: ObservableObject {

  @Published private(set) var network: IndexedNetworkType = .cycling
  @Published private(set) var catalog = NetworkCatalog(index: nil, installed: [], downloading: Set(), failed: Set())
  @Published private(set) var needsAppUpdate = false
  @Published private(set) var online = true
  @Published private(set) var session: PlanningSession?
  @Published private(set) var layers: MapLayers?
  @Published private(set) var routeDescription: RouteDescription = RouteDescriptionEmpty.shared
  @Published private(set) var loading = false
  @Published var message: String?

  var status: NetworkStatus { catalog.status(network: network) }

  let downloads = NetworkDownloads()
  private let storage = NetworkStorage()
  private let pathMonitor = NWPathMonitor()
  private var loaded: LoadedNetwork?
  private var installing = false
  /// Networks with a downloaded file that was rejected; it is downloaded again on the next sync.
  private var rejected: Set<IndexedNetworkType> = []
  /// Whether the sync could not fetch the index yet, so it runs when network access is back.
  private var syncPending = false
  /// Whether the saved route was restored; reloads before that would replace it with an empty one.
  private var restored = false

  init() {
    downloads.onChange = { [weak self] in self?.installFinished() }
    downloads.reconnect()
    pathMonitor.pathUpdateHandler = { [weak self] path in
      Task { @MainActor in self?.networkAccessChanged(path.status == .satisfied) }
    }
    pathMonitor.start(queue: .main)
    Task {
      await installBundled()
      refreshCatalog()
      await restoreRoute()
      restored = true
      await reloadIfChanged()
      if isSyncDue() { await sync() }
    }
  }

  /// Retries failed downloads right away.
  func retrySync() {
    downloads.forgetFailed()
    rejected = []
    Task { await sync() }
  }

  func switchNetwork(_ network: IndexedNetworkType) {
    storage.saveRoute(nil)
    self.network = network
    // The map stays until the other network is loaded, so it keeps its place; without a loaded
    // network, taps are ignored meanwhile.
    loaded = nil
    Task { await loadNetwork(network, snapshot: nil) }
  }

  /// Handles a tap on the map; `radiusMeters` is the touch target at the current zoom level.
  func tap(latitude: Double, longitude: Double, radiusMeters: Double) {
    guard let locator = loaded?.locator else { return }
    change { session in
      locator.nearest(latitude: latitude, longitude: longitude, radiusMeters: radiusMeters)
        .map { session.tap(knot: $0) }
    }
  }

  func moveWaypoint(from: Int, to: Int) {
    change { $0.moveWaypoint(from: Int32(from), to: Int32(to)) }
  }

  func removeWaypoint(at position: Int) {
    change { $0.canRemoveWaypoints ? $0.removeWaypoint(position: Int32(position)) : nil }
  }

  func clear() {
    change { $0.clear() }
  }

  func returnToStart() {
    change { $0.canReturnToStart ? $0.returnToStart() : nil }
  }

  private var changes: Task<Void, Never>?

  /// Applies a planning change and prepares its map layers off the main thread: routing on a large
  /// network takes long enough to make the screen stutter. Changes run one after the other.
  private func change(_ apply: @escaping @Sendable (PlanningSession) -> PlanningSession?) {
    let previous = changes
    changes = Task {
      await previous?.value
      guard let current = session, let loaded else { return }
      let geometry = loaded.geometry
      let prepared = await Task.detached { () -> PreparedSession? in
        apply(current).map { PreparedSession($0, geometry: geometry) }
      }.value
      // A network switch while preparing replaces the session; the change no longer applies.
      guard let prepared, session === current else { return }
      storage.saveRoute(RouteSnapshot.companion.of(session: prepared.session))
      show(prepared)
    }
  }

  private func show(_ newSession: PlanningSession) {
    guard let loaded else { return }
    show(PreparedSession(newSession, geometry: loaded.geometry))
  }

  private func show(_ prepared: PreparedSession) {
    session = prepared.session
    routeDescription = prepared.description
    if let loaded {
      layers = MapLayers(
        knots: loaded.knots,
        connections: loaded.connections,
        route: prepared.route,
        routeKnots: prepared.routeKnots,
        routeKnotList: prepared.routeKnotMarkers,
        refs: loaded.refs)
    }
  }

  private func restoreRoute() async {
    let snapshot = storage.route()
    network = snapshot?.network ?? .cycling
    await loadNetwork(network, snapshot: snapshot)
  }

  /// Loads a network off the main thread: reading it and preparing its map layers takes time.
  /// `snapshot` is the route to continue with, e.g. after a restart or a data update.
  private func loadNetwork(_ network: IndexedNetworkType, snapshot: RouteSnapshot?) async {
    loading = session == nil
    let storage = self.storage
    let result: (LoadedNetwork, PlanningSession, [String])? = await Task.detached {
      guard let graphEntry = storage.graphEntry(network: network), let graphFile = storage.graph(network: network)
      else { return nil }
      let graph = graphFile.graph
      let installedGeometry = storage.geometry(network: network, graphFile: graphFile)
      let geometry = installedGeometry ?? NetworkGeometry.companion.straightLines(graph: graph)
      let knots = Array(graph.knots.values)
      let loaded = LoadedNetwork(
        graphSha256: graphEntry.sha256,
        hasGeometry: installedGeometry != nil,
        geometry: geometry,
        locator: KnotLocator(knots: knots),
        knots: storage.mapLayer(network: network, name: "knots") {
          MapFeatures.shared.knots(graph: graph)
        },
        connections: storage.mapLayer(network: network, name: "connections") {
          MapFeatures.shared.connections(geometry: geometry)
        },
        refs: Set(knots.map(\.ref)))
      if let snapshot, snapshot.network == network {
        let restored = snapshot.restore(graph: graph)
        return (loaded, restored.session, restored.removedWaypoints)
      }
      return (loaded, PlanningSession.companion.start(graph: graph, waypoints: []), [])
    }.value
    loading = false
    guard let (loadedNetwork, newSession, removedWaypoints) = result, network == self.network else { return }
    loaded = loadedNetwork
    show(newSession)
    if !removedWaypoints.isEmpty {
      message = "Waypoint \(removedWaypoints.joined(separator: ", ")) no longer exists and was removed from the route."
    }
  }

  private func refreshCatalog() {
    catalog = NetworkCatalog(
      index: nil, installed: storage.installed(), downloading: downloads.downloading(),
      failed: downloads.failed().union(rejected))
  }

  /// Reloads the selected network when its files changed, keeping the route, see `network-sync`.
  private func reloadIfChanged() async {
    guard restored, let graphEntry = storage.graphEntry(network: network) else { return }
    if loaded?.graphSha256 != graphEntry.sha256 || loaded?.hasGeometry != status.hasGeometry {
      await loadNetwork(network, snapshot: session.map { RouteSnapshot.companion.of(session: $0) })
    }
  }

  // MARK: Network sync, see `network-sync`

  private static let lastCheckKey = "network-sync-last-check"

  private func isSyncDue() -> Bool {
    let lastCheck = UserDefaults.standard.object(forKey: Self.lastCheckKey) as? Int64
    let now = Int64(Date().timeIntervalSince1970 * 1000)
    return UpdateCheck.shared.isDue(lastCheckEpochMillis: lastCheck.map { KotlinLong(value: $0) }, nowEpochMillis: now)
      || catalog.networks.contains { !$0.hasGeometry }
  }

  /// Installs the bundled graphs of networks that have no installed graph or only an older one, and
  /// removes stale data, e.g. networks installed per region by earlier app versions.
  private func installBundled() async {
    defer { storage.removeStaleData() }
    guard let directory = Bundle.main.url(forResource: "bundled-networks", withExtension: nil),
      let text = try? String(contentsOf: directory.appendingPathComponent(NetworkIndex.companion.BUNDLED_INDEX_FILE), encoding: .utf8),
      let bundled = try? NetworkIndex.companion.parse(text: text)
    else { return }  // A debug build without network access bundles nothing.
    let storage = self.storage
    await Task.detached {
      let toInstall = NetworkCatalog(index: nil, installed: storage.installed(), downloading: Set(), failed: Set())
        .bundledToInstall(bundled: bundled)
      for entry in toInstall {
        guard let data = try? Data(contentsOf: directory.appendingPathComponent(entry.file)),
          NetworkInstaller.shared.checkGraph(entry: entry, bytes: Data_iosKt.byteArrayOf(data: data)) is InstallCheckValid<GraphFile>
        else { continue }
        try? storage.install(entry, data: data)
      }
    }.value
  }

  /// Fetches the published index and starts downloading every file the device still needs.
  private func sync() async {
    var request = URLRequest(url: URL(string: NetworkIndex.companion.PUBLISHED_BASE_URL + "index.json")!)
    request.timeoutInterval = 15
    guard let (data, response) = try? await URLSession.shared.data(for: request),
      (response as? HTTPURLResponse)?.statusCode == 200,
      let index = try? NetworkIndex.companion.parse(text: String(decoding: data, as: UTF8.self))
    else {
      syncPending = true
      return
    }
    syncPending = false
    UserDefaults.standard.set(Int64(Date().timeIntervalSince1970 * 1000), forKey: Self.lastCheckKey)
    let toFetch = NetworkCatalog(index: index, installed: storage.installed(), downloading: Set(), failed: Set())
      .filesToFetch
    downloads.start(toFetch)
    refreshCatalog()
  }

  private func networkAccessChanged(_ isOnline: Bool) {
    online = isOnline
    if isOnline && syncPending { Task { await sync() } }
  }

  /// Verifies and installs finished downloads one by one: a graph on its own, a geometry against the
  /// graph installed before it.
  private func installFinished() {
    refreshCatalog()
    guard !installing else { return }
    let finished = downloads.finished().sorted {
      ($0.entry.network.ordinal, $0.entry.kind.ordinal) < ($1.entry.network.ordinal, $1.entry.kind.ordinal)
    }
    guard !finished.isEmpty else { return }
    installing = true
    let storage = self.storage
    Task {
      for (entry, file) in finished {
        let rejection: RejectionReason? = await Task.detached {
          guard let data = try? Data(contentsOf: file) else { return RejectionReason.corrupted }
          let check: InstallCheck
          if entry.kind == .graph {
            check = NetworkInstaller.shared.checkGraph(entry: entry, bytes: Data_iosKt.byteArrayOf(data: data))
          } else if let graphEntry = storage.graphEntry(network: entry.network),
            let graphFile = storage.graph(network: entry.network)
          {
            check = NetworkInstaller.shared.checkGeometry(
              entry: entry, bytes: Data_iosKt.byteArrayOf(data: data), graphEntry: graphEntry, graph: graphFile.graph)
          } else {
            return RejectionReason.mismatchingGeometry
          }
          if let rejected = check as? InstallCheckRejected { return rejected.reason }
          do {
            try storage.install(entry, data: data)
            storage.removeStaleData()
            return nil
          } catch {
            return RejectionReason.corrupted
          }
        }.value
        downloads.remove(entry)
        if let rejection {
          rejected.insert(entry.network)
          if rejection == .unsupportedVersion { needsAppUpdate = true }
        } else {
          rejected.remove(entry.network)
        }
      }
      installing = false
      refreshCatalog()
      await reloadIfChanged()
      // Files that finished while installing are installed in the next round.
      if !downloads.finished().isEmpty { installFinished() }
    }
  }
}
