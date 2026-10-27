import Foundation
import KnoopByKnoopCore

enum Screen {
  case regions
  case planner
}

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

/// The installed data of one region and network, prepared for planning and drawing.
private struct LoadedNetwork {
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

/// Connects the screens with storage, downloads and the core's planning logic.
@MainActor
final class AppModel: ObservableObject {

  @Published private(set) var screen: Screen = .regions
  @Published private(set) var catalog = RegionCatalog(index: nil, installed: [], downloading: Set())
  @Published private(set) var progress: [String: Double] = [:]
  @Published private(set) var session: PlanningSession?
  @Published private(set) var layers: MapLayers?
  @Published private(set) var routeDescription: RouteDescription = RouteDescriptionEmpty.shared
  @Published private(set) var loading = false
  @Published var message: String?

  let downloads = RegionDownloads()
  private let storage = RegionStorage()
  private var index: NetworkIndex?
  private var loaded: LoadedNetwork?
  private var installing: Set<String> = []

  init() {
    downloads.onChange = { [weak self] in self?.checkDownloads() }
    downloads.reconnect()
    refreshCatalog()
    Task { await restoreRoute() }
    Task { await fetchIndex() }
  }

  func showRegions() {
    screen = .regions
    // Without the index the app was offline; network access may be back by now.
    if index == nil { Task { await fetchIndex() } }
  }

  func download(_ region: RegionStatus) {
    downloads.start(region)
    refreshCatalog()
  }

  func remove(region: String) {
    downloads.cancel(region: region)
    storage.remove(region: region)
    if session?.region == region {
      loaded = nil
      session = nil
      layers = nil
      routeDescription = RouteDescriptionEmpty.shared
    }
    refreshCatalog()
  }

  func openPlanner(region: String) {
    screen = .planner
    if session?.region != region {
      Task { await startSession(region: region, network: .cycling, snapshot: nil) }
    }
  }

  func switchNetwork(_ network: IndexedNetworkType) {
    guard let region = session?.region else { return }
    storage.saveRoute(nil)
    Task { await startSession(region: region, network: network, snapshot: nil) }
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

  private func fetchIndex() async {
    index = await NetworkIndexClient.fetch()
    refreshCatalog()
  }

  private func restoreRoute() async {
    let snapshot = storage.route()
    guard let region = snapshot?.region ?? storage.installed().first?.region else { return }
    screen = .planner
    await startSession(region: region, network: snapshot?.network ?? .cycling, snapshot: snapshot)
    if session == nil { screen = .regions }
  }

  /// Loads a network off the main thread: reading it and preparing its map layers takes time.
  private func startSession(region: String, network: IndexedNetworkType, snapshot: RouteSnapshot?) async {
    loading = true
    let storage = self.storage
    let result: (LoadedNetwork, PlanningSession, [String])? = await Task.detached {
      guard let graphFile = storage.graph(region: region, network: network) else { return nil }
      let graph = graphFile.graph
      let geometry =
        storage.geometry(region: region, network: network, graphFile: graphFile)
        ?? NetworkGeometry.companion.straightLines(graph: graph)
      let knots = Array(graph.knots.values)
      let loaded = LoadedNetwork(
        geometry: geometry,
        locator: KnotLocator(knots: knots),
        knots: storage.mapLayer(region: region, network: network, name: "knots") {
          MapFeatures.shared.knots(graph: graph)
        },
        connections: storage.mapLayer(region: region, network: network, name: "connections") {
          MapFeatures.shared.connections(geometry: geometry)
        },
        refs: Set(knots.map(\.ref)))
      if let snapshot, snapshot.network == IndexedNetworkType.companion.of(network: graphFile.network) {
        let restored = snapshot.restore(graph: graph)
        return (loaded, restored.session, restored.removedWaypoints)
      }
      return (loaded, PlanningSession.companion.start(region: region, graph: graph, waypoints: []), [])
    }.value
    loading = false
    guard let (network, newSession, removedWaypoints) = result else { return }
    loaded = network
    show(newSession)
    if !removedWaypoints.isEmpty {
      message = "Waypoint \(removedWaypoints.joined(separator: ", ")) no longer exists and was removed from the route."
    }
  }

  private func refreshCatalog() {
    catalog = RegionCatalog(index: index, installed: storage.installed(), downloading: downloads.regions())
    if session == nil && !catalog.regions.contains(where: { $0.state != .available }) { screen = .regions }
  }

  /// Updates the download progress and installs finished downloads.
  private func checkDownloads() {
    var newProgress: [String: Double] = [:]
    for region in downloads.regions() {
      switch downloads.progress(region: region) {
      case .running(let fraction): newProgress[region] = fraction
      case .finished(let files): install(region: region, files: files)
      case .failed: fail(region: region, "The download of \(RegionCatalog.companion.nameOf(region: region)) failed. Please try again.")
      case nil: break
      }
    }
    if newProgress != progress { progress = newProgress }
  }

  private func install(region: String, files: [(entry: NetworkIndexEntry, file: URL)]) {
    guard !installing.contains(region) else { return }
    installing.insert(region)
    let storage = self.storage
    Task {
      let rejection: RejectionReason? = await Task.detached {
        let data = files.compactMap { file in (try? Data(contentsOf: file.file)).map { (file.entry, $0) } }
        guard data.count == files.count else { return RejectionReason.corrupted }
        switch Self.verified(data) {
        case .rejected(let reason): return reason
        case .valid(let verified):
          do {
            try storage.install(verified)
            return nil
          } catch {
            return RejectionReason.corrupted
          }
        }
      }.value
      installing.remove(region)
      downloads.cancel(region: region)
      let name = RegionCatalog.companion.nameOf(region: region)
      switch rejection {
      case .unsupportedVersion?: fail(region: region, "\(name) needs a newer version of the app. Please update it.")
      case .some: fail(region: region, "The download of \(name) was damaged. Please try again.")
      case nil:
        refreshCatalog()
        if session == nil || session?.region == region { openPlanner(region: region) }
      }
    }
  }

  private enum Verification {
    case valid([(entry: NetworkIndexEntry, data: Data)])
    case rejected(RejectionReason)
  }

  /// Checks all files of a region. A damaged graph rejects the whole region; a geometry that cannot
  /// be installed is left out, so the network is drawn with straight lines until an update.
  private nonisolated static func verified(_ files: [(NetworkIndexEntry, Data)]) -> Verification {
    var verified: [(entry: NetworkIndexEntry, data: Data)] = []
    for (graphEntry, graphData) in files where graphEntry.kind == .graph {
      let check = RegionInstaller.shared.checkGraph(entry: graphEntry, bytes: Data_iosKt.byteArrayOf(data: graphData))
      guard let valid = check as? InstallCheckValid<GraphFile>, let graphFile = valid.content else {
        return .rejected((check as? InstallCheckRejected)?.reason ?? .corrupted)
      }
      verified.append((graphEntry, graphData))
      if let (geometryEntry, geometryData) = files.first(where: { $0.0.kind == .geometry && $0.0.network == graphEntry.network }),
        RegionInstaller.shared.checkGeometry(
          entry: geometryEntry, bytes: Data_iosKt.byteArrayOf(data: geometryData), graphEntry: graphEntry,
          graph: graphFile.graph) is InstallCheckValid<NetworkGeometry>
      {
        verified.append((geometryEntry, geometryData))
      }
    }
    return .valid(verified)
  }

  private func fail(region: String, _ text: String) {
    downloads.cancel(region: region)
    message = text
    refreshCatalog()
  }
}

/// Fetches the published index of regions.
enum NetworkIndexClient {

  /// The published index, or `nil` without network access.
  static func fetch() async -> NetworkIndex? {
    var request = URLRequest(url: RegionDownloads.baseURL.appendingPathComponent("index.json"))
    request.timeoutInterval = 10
    guard let (data, response) = try? await URLSession.shared.data(for: request),
      (response as? HTTPURLResponse)?.statusCode == 200
    else { return nil }
    return try? NetworkIndex.companion.parse(text: String(decoding: data, as: UTF8.self))
  }
}
