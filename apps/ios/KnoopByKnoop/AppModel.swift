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
  /// The ways of the route points, see `map-planning` "The line on the map".
  let routePointWays: String
  let refs: Set<String>
  /// Whether `connections` follow their course, or are straight lines without geometry.
  let hasCourses: Bool
  /// The installed geometry, to split lines at bridges; `nil` without courses.
  let geometry: NetworkGeometry?
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
  let routePointWays: String

  init(_ session: PlanningSession, geometry: NetworkGeometry) {
    self.session = session
    description = MapFeatures.shared.describe(session: session, language: AppLanguage.current)
    route = MapFeatures.shared.route(session: session, geometry: geometry)
    routeKnots = MapFeatures.shared.routeKnotsLayer(session: session)
    routeKnotMarkers = MapFeatures.shared.routeKnotMarkers(session: session)
    routePointWays = MapFeatures.shared.routePointWays(session: session)
  }
}

/// Where the nearest `knot` is and, if known, the `rider`, for the map to show, see `map-planning`
/// "Nearest knots".
final class NearestKnots {
  let knot: GeoPoint
  let rider: Fix?

  init(knot: GeoPoint, rider: Fix?) {
    self.knot = knot
    self.rider = rider
  }
}

/// A tapped place offered as a route point, see `map-planning` "Choose a route point on the map".
struct PlaceOffer: Identifiable {
  let id = UUID()
  let offer: MapTapOffer
  /// Where the place is on the map now, to show the choices there; it follows the map.
  var point: CGPoint
  /// Whether only the place's name is shown, without choices, see `map-planning` "Tapping a place
  /// on the map".
  var nameOnly = false
}

/// What to make of an offered place, see `map-planning` "What a chosen route point does".
enum PlaceChoice {
  case start, stop, end
}

/// A `knot` to show on the map; `request` counts the taps, so the same knot can be shown again.
struct ShownKnot {
  let knot: Knot
  let request: Int
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
  /// What tapped knots become, kept across starts of the app, see `map-planning` "What new knots become".
  @Published var newKnots: NewKnots =
    UserDefaults.standard.string(forKey: "newKnots") == "end" ? .end : .stop
  {
    didSet { UserDefaults.standard.set(newKnots == .end ? "end" : "stop", forKey: "newKnots") }
  }
  /// How far the path data for the planned route is ready, `nil` if none is wanted.
  @Published private(set) var pathData: PathDataStatus?
  /// The data date of the installed path data.
  @Published private(set) var pathDataDate: String? = PathData.store.dataDate
  /// The space the installed path data takes, in bytes, see `network-sync`.
  @Published private(set) var pathDataBytes: Int64 = PathData.store.installedBytes()
  /// The course of the route planned when the ways were deleted, until the route changes or
  /// navigation starts, see `network-sync` "Deleting with a route planned".
  private var pathDataDeletedFor: [GeoPoint]?
  private var pathDataLoad: (task: Task<Void, Never>, cancellation: PathDataCancellation, course: [GeoPoint], online: Bool)?
  @Published var message: String?
  /// Whether the `message` offers to open the app's settings, e.g. to allow location access.
  @Published var messageOpensSettings = false
  /// The rider's position as found on request, marked on the map without following them, see
  /// `map-planning` "Show my position".
  @Published private(set) var myPosition: Fix?
  /// Whether the position is being found.
  @Published private(set) var locating = false
  /// Whether the map shows no knots, see `map-planning` "Nearest knots".
  @Published private(set) var noKnotsInView = false
  /// Whether the nearest knots are being found.
  @Published private(set) var findingNearest = false
  /// Where the map moved for the nearest knots last.
  @Published private(set) var nearestKnots: NearestKnots?
  private let positionFinder = PositionFinder()
  @Published private(set) var searchQuery = ""
  @Published private(set) var searchResult: SearchResult = .none
  /// The chosen search hit, shown with a marker on the map.
  @Published private(set) var marker: PlaceHit?
  /// The coming knot last tapped, shown on the map, see `knot-navigation` "Showing a coming knot on
  /// the map".
  @Published private(set) var shownKnot: ShownKnot?

  var status: NetworkStatus { catalog.status(network: network) }

  let downloads = NetworkDownloads()
  private let storage = NetworkStorage()
  private let pathMonitor = NWPathMonitor()
  private var loaded: LoadedNetwork? {
    didSet { updateKnotsInView() }
  }
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
      // The first start, with nothing else to show: ask for the position right away.
      let firstStart = storage.camera() == nil && (storage.route()?.waypoints.isEmpty ?? true)
      await restoreRoute()
      restored = true
      if firstStart { locate() }
      await reloadIfChanged()
      // At every start, see `network-sync` "Keep the networks current".
      await sync()
    }
  }

  /// Retries failed downloads right away.
  func retrySync() {
    downloads.forgetFailed()
    rejected = []
    Task { await sync() }
  }

  /// Whether a route is being navigated; the route is fixed meanwhile, see `knot-navigation` "Route
  /// is fixed while navigating".
  var navigating: Bool { Navigator.shared.state != nil }

  /// Whether the route can be navigated: it is complete, see `knot-navigation` "Start navigation".
  var canNavigate: Bool { !navigating && routeDescription is RouteDescriptionComplete }

  /// Starts navigating the planned route, asking for location access first.
  func startNavigation() {
    guard let session, let loaded,
      let knots = (session.route?.summary as? RouteSummaryComplete)?.knots,
      let description = routeDescription as? RouteDescriptionComplete
    else { return }
    // The route's squares are wanted again, see `network-sync` "Deleting with a route planned".
    if pathDataDeletedFor != nil {
      pathDataDeletedFor = nil
      loadPathData()
    }
    Navigator.shared.onDenied = { [weak self] in
      self?.message = String(localized: "Knoop by Knoop needs location access to navigate. You can allow it in Settings.", bundle: AppLanguage.bundle)
    }
    Navigator.shared.start(
      NavigationRoute(
        knots: knots, roles: description.knots.map { BadgeRole($0.role) }, graph: session.routeGraph,
        geometry: loaded.hasGeometry ? loaded.geometry.withRoutePoints(routeGraph: session.routeGraph) : nil,
        stops: Set(description.stopPositions.map { Int(truncating: $0) })))
  }

  /// The course of the planned route along the network, or `nil` while it cannot be exported, see
  /// `map-planning` "Export the route as GPX": not while navigating, and not as straight lines.
  private var exportCourse: [GeoPoint]? {
    guard !navigating, let session, let route = session.route, let loaded, loaded.hasGeometry,
      let course = loaded.geometry.withRoutePoints(routeGraph: session.routeGraph).courseOf(route: route),
      Gpx.shared.canExport(route: route, course: course)
    else { return nil }
    return course
  }

  /// Whether the planned route can be exported as GPX.
  var canExportGpx: Bool { exportCourse != nil }

  /// Writes the planned route's GPX file for sharing, see [canExportGpx].
  func exportGpx() -> ExportedGpx? {
    guard let route = session?.route, let course = exportCourse else { return nil }
    return ExportedGpx.write(
      fileName: Gpx.shared.fileName(route: route), content: Gpx.shared.write(route: route, course: course))
  }

  /// Writes the core's texts in the language just chosen, see `app-languages` "Language setting".
  func languageChanged() {
    guard let session else { return }
    routeDescription = MapFeatures.shared.describe(session: session, language: AppLanguage.current)
  }

  func stopNavigation() {
    Navigator.shared.stop()
  }

  /// Shows the coming `knot` on the map, see `knot-navigation` "Showing a coming knot on the map".
  func showOnMap(_ knot: Knot) {
    shownKnot = ShownKnot(knot: knot, request: (shownKnot?.request ?? 0) + 1)
  }

  func switchNetwork(_ network: IndexedNetworkType) {
    guard !navigating else { return }
    storage.saveRoute(nil)
    self.network = network
    // The map stays until the other network is loaded, so it keeps its place; without a loaded
    // network, taps are ignored meanwhile.
    loaded = nil
    Task { await loadNetwork(network, snapshot: nil) }
  }

  /// Handles a tap on the map; `radiusMeters` is the touch target at the current zoom level.
  func tap(latitude: Double, longitude: Double, radiusMeters: Double, at point: CGPoint, place: MapPlace?) {
    // A tap beside the choices closes them, see `map-planning` "Offer and dismiss".
    if offer != nil {
      offer = nil
      return
    }
    guard let loaded, let session else { return }
    // While navigating, a place shows its name only, see `map-planning` "Place while navigating".
    if navigating {
      if let place { offer = nameOnly(place, at: point) }
      return
    }
    let newKnots = newKnots
    // Any badge of a knot, see `map-planning` "Knots signposted at several places".
    let knot = loaded.locator.nearestAtAnyNode(latitude: latitude, longitude: longitude, radiusMeters: radiusMeters)
    let tapped = session.tapAt(
      position: GeoPoint(latitude: latitude, longitude: longitude), radiusMeters: radiusMeters, knot: knot,
      geometry: loaded.geometry, place: place)
    switch tapped {
    case let placeOnly as MapTapPlaceOnly: offer = nameOnly(placeOnly.place, at: point)
    case let onKnot as MapTapOnKnot: change { $0.tap(knot: onKnot.knot, newKnots: newKnots) }
    case let place as MapTapOffer:
      offer = PlaceOffer(offer: place, point: point)
    // See `map-planning` "Too far from the network".
    case let tooFar as MapTapTooFar:
      let kilometers = Int(tooFar.limitMeters / 1000)
      message =
        session.network == .hiking
        ? String(localized: "No hiking knots within \(kilometers) km", bundle: AppLanguage.bundle)
        : String(localized: "No cycling knots within \(kilometers) km", bundle: AppLanguage.bundle)
    default: break
    }
  }

  /// A tapped place whose choices are shown, see `map-planning` "Choose a route point on the map".
  @Published var offer: PlaceOffer?

  /// The card with only the name of `place`.
  private func nameOnly(_ place: MapPlace, at point: CGPoint) -> PlaceOffer {
    PlaceOffer(
      offer: MapTapOffer(position: place.position, onCourse: nil, canAddStop: false, place: place), point: point,
      nameOnly: true)
  }

  /// Keeps the offered place's choices at its flag while the map moves.
  func offerMoved(to point: CGPoint) {
    guard offer != nil, offer?.point != point else { return }
    offer?.point = point
  }

  /// Makes the offered place a route point, see `map-planning` "What a chosen route point does".
  func choose(_ choice: PlaceChoice) {
    guard let offered = offer?.offer, let session, let loaded,
      let point = session.routePointFor(offer: offered, geometry: loaded.geometry)
    else {
      offer = nil
      return
    }
    offer = nil
    change { session in
      switch choice {
      case .start: return session.startAt(point: point)
      case .stop: return session.route != nil ? session.addStopAt(point: point) : nil
      case .end: return session.endAt(point: point)
      }
    }
    findWays()
  }

  /// Replaces the straight lines of the route points by ways along paths where the installed path
  /// data has them, on the path data's queue, see `map-planning` "A route point away from the network".
  private func findWays() {
    // After the change that added the point.
    let previous = changes
    Task {
      await previous?.value
      guard let session, session.hasStraightLines, !navigating, let loaded, loaded.hasGeometry else { return }
      nonisolated(unsafe) let points = session.routePoints.filter { $0.routePoint?.straight == true }
      nonisolated(unsafe) let geometry = loaded.geometry
      let found: [Knot] = await withCheckedContinuation { continuation in
        PathData.queue.async {
          // Squares arrived since: the router read them as missing, see "While the path data loads".
          if AppModel.pathRouterVersion != PathData.store.version {
            AppModel.pathRouter.forgetSquares()
            AppModel.pathRouterVersion = PathData.store.version
          }
          let ways = RoutePointWays(router: AppModel.pathRouter, geometry: geometry)
          nonisolated(unsafe) let found = points.compactMap { ways.wayFor(point: $0) }
          continuation.resume(returning: found)
        }
      }
      guard !found.isEmpty, !navigating else { return }
      // Onto the session as it is now: the route may have changed meanwhile.
      change { session in
        let updated = found.reduce(session) { $0.withRoutePoint(point: $1) }
        return updated === session ? nil : updated
      }
    }
  }

  /// One router for the planner's ways, used on `PathData.queue` only.
  nonisolated(unsafe) private static let pathRouter = PathRouter(file: PathData.store)
  nonisolated(unsafe) private static var pathRouterVersion: Int32 = -1

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
    guard !navigating else { return }
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
        routePointWays: prepared.routePointWays,
        refs: loaded.refs,
        hasCourses: loaded.hasGeometry,
        geometry: loaded.hasGeometry ? loaded.geometry : nil)
    }
    loadPathData()
  }

  /// Keeps the squares around the planned route on the device, see `network-sync` "Path data around
  /// the route": requests start 2 seconds after the route last changed, and start over with a new
  /// route, after deleting the ways, or with network access coming back. A download running when
  /// navigation starts goes on, as only the route decides what is requested.
  private func loadPathData() {
    guard
      let wanted = PathSquares.shared.courseToLoad(
        network: session?.network, course: plannedCourse(), deletedFor: pathDataDeletedFor)
    else {
      pathDataLoad?.cancellation.cancel()
      pathDataLoad?.task.cancel()
      pathDataLoad = nil
      pathData = nil
      return
    }
    if let running = pathDataLoad, running.course == wanted, running.online == online { return }
    pathDataLoad?.cancellation.cancel()
    pathDataLoad?.task.cancel()
    let cancellation = PathDataCancellation()
    let task = Task { [weak self] in
      try? await Task.sleep(for: .seconds(2))
      guard !Task.isCancelled else { return }
      nonisolated(unsafe) let course = wanted
      let status: PathDataStatus = await withCheckedContinuation { continuation in
        PathData.queue.async {
          let status = PathFile_iosKt.pathDownloader(store: PathData.store).load(
            course: course,
            nowMillis: Int64(Date().timeIntervalSince1970 * 1000),
            isCancelled: { KotlinBoolean(bool: cancellation.isCancelled) },
            onStatus: { status in
              nonisolated(unsafe) let status = status
              DispatchQueue.main.async {
                MainActor.assumeIsolated {
                  if !cancellation.isCancelled { self?.pathData = status }
                }
              }
            })
          nonisolated(unsafe) let result = status
          continuation.resume(returning: result)
        }
      }
      guard !cancellation.isCancelled else { return }
      self?.pathData = status
      self?.pathDataDate = PathData.store.dataDate
      self?.pathDataBytes = PathData.store.installedBytes()
      // The squares around a straight line may have arrived, see "While the path data loads".
      if status == .ready { self?.findWays() }
    }
    pathDataLoad = (task, cancellation, wanted, online)
  }

  /// The course of the complete planned route, `nil` without one or without courses.
  private func plannedCourse() -> [GeoPoint]? {
    guard let session, let route = session.route, route.summary is RouteSummaryComplete, let loaded,
      loaded.hasGeometry
    else { return nil }
    return loaded.geometry.withRoutePoints(routeGraph: session.routeGraph).courseOf(route: route)
  }

  /// Deletes the downloaded ways; the planned route's squares come again once it changes or
  /// navigation starts, see `network-sync` "Path data in the info sheet".
  func deletePathData() {
    pathDataDeletedFor = plannedCourse() ?? []
    loadPathData()
    pathDataDate = nil
    pathDataBytes = 0
    PathData.queue.async { PathData.store.clear() }
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
    // Step 1, the graph: knots, straight connections and the route, so planning can start.
    let prepared: (GraphFile, LoadedNetwork)? = await Task.detached {
      guard let graphEntry = storage.graphEntry(network: network), let graphFile = storage.graph(network: network)
      else { return nil }
      let graph = graphFile.graph
      let lines = NetworkGeometry.companion.straightLines(graph: graph)
      let knots = Array(graph.knots.values)
      return (
        graphFile,
        LoadedNetwork(
          graphSha256: graphEntry.sha256,
          hasGeometry: false,
          geometry: lines,
          locator: KnotLocator(knots: knots),
          knots: storage.mapLayer(network: network, name: "knot-badges", withGeometry: false) {
            MapFeatures.shared.knots(graph: graph)
          },
          connections: storage.mapLayer(network: network, name: "lines", withGeometry: false) {
            MapFeatures.shared.connections(geometry: lines)
          },
          refs: Set(knots.map(\.ref)))
      )
    }.value
    guard let (graphFile, withoutCourses) = prepared else {
      loading = false
      return
    }
    let graph = graphFile.graph
    let restored: (PlanningSession, [String]) = await Task.detached {
      if let snapshot, snapshot.network == network {
        let restored = snapshot.restore(graph: graph)
        return (restored.session, restored.removedWaypoints)
      }
      return (PlanningSession.companion.start(graph: graph, waypoints: []), [])
    }.value
    loading = false
    guard network == self.network else { return }
    loaded = withoutCourses
    show(restored.0)
    if !restored.1.isEmpty {
      message = String(
        localized: "Waypoint \(restored.1.joined(separator: ", ")) no longer exists and was removed from the route.")
    }
    await addCourses(network, graphFile: graphFile, to: withoutCourses)
  }

  /// Step 2 of `loadNetwork`, the geometry: the connections and the route are redrawn along their
  /// course. The session stays, so planning goes on, and so does the map's place.
  private func addCourses(_ network: IndexedNetworkType, graphFile: GraphFile, to withoutCourses: LoadedNetwork) async {
    let storage = self.storage
    let withCourses: LoadedNetwork? = await Task.detached {
      guard let geometry = storage.geometry(network: network, graphFile: graphFile) else { return nil }
      return LoadedNetwork(
        graphSha256: withoutCourses.graphSha256,
        hasGeometry: true,
        geometry: geometry,
        locator: withoutCourses.locator,
        knots: withoutCourses.knots,
        connections: storage.mapLayer(network: network, name: "courses", withGeometry: true) {
          MapFeatures.shared.connections(geometry: geometry)
        },
        refs: withoutCourses.refs)
    }.value
    // Another network or newer data may have been loaded meanwhile.
    guard let withCourses, network == self.network, loaded?.connections == withoutCourses.connections else { return }
    loaded = withCourses
    if let session {
      // Route points whose connection went away join the courses anew, see "Connection gone".
      let reattached = session.reattached(geometry: withCourses.geometry)
      if reattached !== session {
        storage.saveRoute(RouteSnapshot.companion.of(session: reattached))
      }
      show(reattached)
    }
    findWays()
  }

  /// Where the map opens: where it was when the app was left last.
  lazy var initialPlace: CameraPlace? = storage.camera()

  /// Finds the rider's position once, asking for access first if needed, see `map-planning` "Show
  /// my position".
  func locate() {
    guard !locating, !findingNearest else { return }
    locating = true
    positionFinder.find { [weak self] result in
      guard let self else { return }
      locating = false
      switch result {
      case .found(let fix): myPosition = fix
      case .notFound: message = String(localized: "Your position could not be found.", bundle: AppLanguage.bundle)
      case .denied:
        message = String(
          localized: "Knoop by Knoop needs location access to show your position. You can allow it in Settings.")
        messageOpensSettings = true
      }
    }
  }

  /// Moves the map to the nearest knots, from the rider's position where location access is
  /// allowed, otherwise from the middle of the map, without asking, see `map-planning` "Nearest
  /// knots".
  func showNearestKnots() {
    guard !locating, !findingNearest else { return }
    guard positionFinder.hasAccess else {
      moveToNearestKnots(rider: nil)
      return
    }
    findingNearest = true
    positionFinder.find { [weak self] result in
      guard let self else { return }
      findingNearest = false
      if case .found(let fix) = result {
        moveToNearestKnots(rider: fix)
      } else {
        moveToNearestKnots(rider: nil)
      }
    }
  }

  private func moveToNearestKnots(rider: Fix?) {
    guard let center = mapCenter ?? initialPlace else { return }
    guard
      let knot = loaded?.locator.nearestPosition(
        latitude: rider?.latitude ?? center.latitude, longitude: rider?.longitude ?? center.longitude)
    else { return }
    if let rider { myPosition = rider }
    nearestKnots = NearestKnots(knot: knot, rider: rider)
  }

  func saveCamera(_ place: CameraPlace) {
    // The app is left: the position found on request is forgotten.
    myPosition = nil
    storage.saveCamera(place)
  }

  // MARK: Place search, see `place-search`

  private var searchTask: Task<Void, Never>?
  private var mapCenter: CameraPlace?
  private var mapArea: GeoBounds?

  /// The map's current place, to rank search hits near it.
  func mapMoved(_ place: CameraPlace, area: GeoBounds?) {
    mapCenter = place
    mapArea = area
    // The map can report a move while the view updates, which must not change what it observes.
    DispatchQueue.main.async { [weak self] in self?.updateKnotsInView() }
  }

  private func updateKnotsInView() {
    let none = mapCenter != nil && loaded?.locator.noKnotsIn(area: mapArea) == true
    if none != noKnotsInView { noKnotsInView = none }
  }

  /// Searches `query` once the user stopped typing for a moment; newer queries replace older ones.
  func search(_ query: String) {
    // The field passes its text again when it loses the focus, e.g. after a hit was chosen, which
    // must not bring the hits back.
    guard query != searchQuery else { return }
    searchTask?.cancel()
    searchQuery = query
    guard query.trimmingCharacters(in: .whitespaces).count >= Int(PlaceSearch.shared.MIN_QUERY_LENGTH),
      let locator = loaded?.locator, let graph = session?.graph, let area = GeoBounds.companion.of(graph: graph)
    else {
      searchResult = .none
      return
    }
    let center = mapCenter ?? initialPlace
    searchTask = Task {
      try? await Task.sleep(for: .milliseconds(300))
      guard !Task.isCancelled else { return }
      searchResult = .searching
      let requests = PlaceSearch.shared.requests(
        query: query,
        centerLatitude: center?.latitude ?? (area.minLatitude + area.maxLatitude) / 2,
        centerLongitude: center?.longitude ?? (area.minLongitude + area.maxLongitude) / 2,
        area: area, language: Locale.current.language.languageCode?.identifier ?? "en")
      async let prominent = PlaceSearchClient.fetch(requests.prominent)
      async let nearby = PlaceSearchClient.fetch(requests.nearby)
      let responses = await (prominent, nearby)
      guard !Task.isCancelled else { return }
      // Off the main thread: on the phone, reading the answers takes long enough to hold up taps.
      nonisolated(unsafe) let network = locator
      let started = Date()
      let merged: [PlaceHit]? = await Task.detached {
        try? PlaceSearch.shared.merge(prominent: responses.0, nearby: responses.1, network: network)
      }.value
      DebugTiming.note("search \"\(query)\" merged in \(Int(Date().timeIntervalSince(started) * 1000)) ms")
      guard !Task.isCancelled else { return }
      // Neither request was answered when merging fails.
      if let hits = merged {
        searchResult = hits.isEmpty ? .noHits : .hits(hits)
      } else {
        searchResult = .unavailable
      }
    }
  }

  /// Moves the map to `hit` and marks it; the route stays as it is.
  func choose(_ hit: PlaceHit) {
    searchTask?.cancel()
    searchQuery = hit.name
    searchResult = .none
    marker = hit
  }

  /// Notes on the map that it is about to show `hit`, at once on the tap, until the map is idle.
  func willShow(_ hit: PlaceHit) {
    showingHit = hit.name
    // At the latest, should the map never report being done.
    let name = hit.name
    Task {
      try? await Task.sleep(for: .seconds(10))
      if showingHit == name { showingHit = nil }
    }
  }

  /// While the map moves to a chosen hit and draws it, its name, for a note on the map, see
  /// `place-search` "Showing a far place on iOS".
  @Published private(set) var showingHit: String?

  /// The map has come to rest after moving to a hit.
  func mapIdle() {
    // Not the idle right after the tap, before the map has started to move.
    if showingHit != nil, let marker, let center = mapCenter,
      abs(center.latitude - marker.latitude) < 0.01, abs(center.longitude - marker.longitude) < 0.01
    {
      DebugTiming.note("map idle")
      showingHit = nil
    }
  }

  /// Searches the text in the field again, e.g. when the search opens with it or on the keyboard's
  /// search key, see `place-search` "Typing on iOS".
  func searchAgain() {
    let query = searchQuery
    searchQuery = ""
    search(query)
  }

  func clearSearch() {
    searchTask?.cancel()
    searchQuery = ""
    searchResult = .none
    marker = nil
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

  /// Installs the bundled graphs of networks that have no installed graph, only an older one, or the
  /// same data in an older format, and removes stale data, e.g. networks installed per region by
  /// earlier app versions.
  private func installBundled() async {
    defer { storage.removeStaleData() }
    guard let directory = Bundle.main.url(forResource: "bundled-networks", withExtension: nil),
      let text = try? String(contentsOf: directory.appendingPathComponent(NetworkIndex.companion.BUNDLED_INDEX_FILE), encoding: .utf8),
      let bundled = try? NetworkIndex.companion.parse(text: text)
    else { return }  // A debug build without network access bundles nothing.
    let storage = self.storage
    await Task.detached {
      let graphs = bundled.files.filter { $0.kind == .graph }
      var installedVersions: [IndexedNetworkType: KotlinInt] = [:]
      var bundledVersions: [IndexedNetworkType: KotlinInt] = [:]
      for entry in graphs {
        installedVersions[entry.network] = storage.graphFormatVersion(network: entry.network).map { KotlinInt(value: Int32($0)) }
        bundledVersions[entry.network] = NetworkStorage.graphFormatVersion(of: directory.appendingPathComponent(entry.file))
          .map { KotlinInt(value: Int32($0)) }
      }
      let toInstall = NetworkCatalog(index: nil, installed: storage.installed(), downloading: Set(), failed: Set())
        .bundledToInstall(bundled: bundled, installedFormatVersions: installedVersions, bundledFormatVersions: bundledVersions)
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
    let toFetch = NetworkCatalog(index: index, installed: storage.installed(), downloading: Set(), failed: Set())
      .filesToFetch
    downloads.start(toFetch)
    refreshCatalog()
  }

  private func networkAccessChanged(_ isOnline: Bool) {
    online = isOnline
    loadPathData()
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
