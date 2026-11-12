import Combine
import CoreLocation
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
  /// The place's details once found, see `map-planning` "Contacts on the place card"; `nil` before
  /// or without them.
  var details: PlaceDetails?
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
  /// What the network syncs came to, kept across starts, see `NetworkSyncState`.
  @Published private(set) var syncState = NetworkSyncState.companion.parse(
    text: UserDefaults.standard.string(forKey: AppModel.syncStateKey) ?? "")
  /// Whether to ask the user to update the app, see `network-sync` "Unsupported file version".
  var needsAppUpdate: Bool { syncState.needsAppUpdate }
  @Published private(set) var online = true
  @Published private(set) var session: PlanningSession?
  @Published private(set) var layers: MapLayers?
  @Published private(set) var routeDescription: RouteDescription = RouteDescriptionEmpty.shared
  @Published private(set) var loading = false
  /// What the last tapped knot became, while the note about it shows, see `map-planning` "Changing
  /// what a tapped knot became".
  @Published private(set) var tapNote: TapNote?
  private var tapNotes = 0
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
  /// The data date of the installed place details.
  @Published private(set) var placeDetailsDate: String? = PlaceDetailsData.store.dataDate
  /// The space the installed place details take, in bytes, see `network-sync`.
  @Published private(set) var placeDetailsBytes: Int64 = PlaceDetailsData.store.installedBytes()
  /// The course of the route planned when the place details were deleted, until the route changes
  /// or navigation starts, see `network-sync` "Deleting place details with a route planned".
  private var placeDetailsDeletedFor: [GeoPoint]?
  private var placeDetailsLoad: (task: Task<Void, Never>, cancellation: PathDataCancellation, course: [GeoPoint], online: Bool)?
  /// Whether the route's base map is stored, see `network-sync` "Base map ready for the ride".
  @Published private(set) var baseMap: PathDataStatus?
  /// The space the stored base map takes, see "Base map in the info sheet".
  @Published private(set) var baseMapBytes: Int64 = 0
  /// The course the user deleted the base map for: not stored again until the route changes.
  private var baseMapDeletedFor: [GeoPoint]?
  private var baseMapLoad: (task: Task<Void, Never>, course: [GeoPoint], online: Bool)?
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
  /// The place search's text and hits, not published here, see `PlaceSearchModel`.
  let placeSearch = PlaceSearchModel()
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
  /// The sync waiting to download a corrupted file again, see "Retrying a corrupted download".
  private var retryTask: Task<Void, Never>?
  private static let syncStateKey = "networkSyncState"
  /// Whether the sync could not fetch the index yet, so it runs when network access is back.
  private var syncPending = false
  /// Whether the saved route was restored; reloads before that would replace it with an empty one.
  private var restored = false

  /// The plan as made while a route for one navigation is shown, see `knot-navigation` "The plan
  /// after navigation"; `nil` otherwise.
  private var plannedSession: PlanningSession?
  private var navigationEnd: AnyCancellable?

  init() {
    defer {
      // When navigation ends, the plan as made comes back, see "The plan after navigation".
      navigationEnd = Navigator.shared.$state.map { $0 != nil }.removeDuplicates().dropFirst()
        .sink { [weak self] navigating in
          if !navigating { self?.restorePlan() }
        }
      Task { [weak self] in
        let bytes = await BaseMapStore.shared.readInstalledBytes()
        self?.baseMapBytes = bytes
      }
      placeSearch.context = { [weak self] in
        guard let self, let locator = self.loaded?.locator, let graph = self.session?.graph,
          let area = GeoBounds.companion.of(graph: graph)
        else { return nil }
        return PlaceSearchModel.Context(locator: locator, area: area, center: self.mapCenter ?? self.initialPlace)
      }
    }
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
    Task { await sync() }
  }

  /// Whether a route is being navigated; the route is fixed meanwhile, see `knot-navigation` "Route
  /// is fixed while navigating".
  var navigating: Bool { Navigator.shared.state != nil }

  /// Whether the route can be navigated: it is complete, see `knot-navigation` "Start navigation".
  var canNavigate: Bool { !navigating && routeDescription is RouteDescriptionComplete }

  /// Starts navigating the planned route, asking for location access first.
  /// Navigate's question whether to ride to the start or start on the route nearby, see
  /// `knot-navigation` "Start on the route nearby".
  @Published var startChoice: StartChoice?
  /// Whether Navigate is finding the rider's position, see `knot-navigation` "Start on the route
  /// nearby".
  @Published private(set) var findingStart = false

  /// Starts navigating the planned route. Away from the start but close to the route, asks first,
  /// with one position given up after 10 seconds, then navigation starts without asking, see
  /// `knot-navigation` "Start on the route nearby".
  func startNavigation() {
    guard let session, let loaded, session.route != nil, !findingStart else { return }
    // Shown on Navigate until the question or navigation takes over, see "Waiting for the
    // position"; a recent rough fix is enough to decide, see "Recent position".
    findingStart = true
    positionFinder.find(timeout: .seconds(10), accuracy: kCLLocationAccuracyHundredMeters, maxAge: 60) {
      [weak self] result in
      guard let self else { return }
      findingStart = false
      var choice: StartChoice?
      if case .found(let fix) = result {
        choice = StartChoice.companion.of(
          session: session, geometry: loaded.hasGeometry ? loaded.geometry : nil,
          position: GeoPoint(latitude: fix.latitude, longitude: fix.longitude))
      }
      if case .found(let fix) = result {
        startPosition = GeoPoint(latitude: fix.latitude, longitude: fix.longitude)
      } else {
        startPosition = nil
      }
      if let choice { startChoice = choice } else { checkFerries() }
    }
  }

  /// Navigate's question whether to ride around a ferry closed by the time the rider gets there,
  /// see `knot-navigation` "Riding around a closed ferry".
  @Published var ferryDetour: FerryDetour?

  /// Where the rider was when Navigate was pressed, for the ferries' arrival.
  private var startPosition: GeoPoint?

  /// Asks about ferries closed by the time the rider gets there before navigating; navigates at
  /// once when none is.
  private func checkFerries(kept: Set<Ferry> = [], asked: Int32 = 0) {
    guard let session, let loaded, let startPosition else {
      navigate()
      return
    }
    keptFerries = kept
    ferriesAsked = asked
    let detour = FerryDetour.companion.of(
      session: session, geometry: loaded.hasGeometry ? loaded.geometry : nil, position: startPosition,
      nowMillis: Int64(Date().timeIntervalSince1970 * 1000),
      localTime: { millis in
        RouteFerries.ferryMoment(Date(timeIntervalSince1970: Double(millis.int64Value) / 1000))
      }, language: AppLanguage.current, kept: kept, asked: asked)
    if let detour { ferryDetour = detour } else { navigate() }
  }

  /// The ferries kept and the number of ferry questions asked since Navigate was pressed.
  private var keptFerries: Set<Ferry> = []
  private var ferriesAsked: Int32 = 0

  /// Rides around the ferry asked about, for this navigation only, then asks about the next closed
  /// ferry or navigates, see "Riding around it" and "Several closed ferries".
  func rideAroundFerries() {
    guard let around = ferryDetour?.around else { return }
    ferryDetour = nil
    Task {
      await showForNavigation(around)
      checkFerries(kept: keptFerries, asked: ferriesAsked + 1)
    }
  }

  /// Keeps the ferry asked about, then asks about the next closed ferry or navigates, see "Keeping
  /// the ferry".
  func keepFerries() {
    guard let ferry = ferryDetour?.ferry else { return }
    ferryDetour = nil
    checkFerries(kept: keptFerries.union([ferry]), asked: ferriesAsked + 1)
  }

  /// The question dismissed: nothing starts, and a start nearby is undone, see "Question dismissed".
  func dismissFerryDetour() {
    ferryDetour = nil
    restorePlan()
  }

  /// Rides to the plan's start, see `knot-navigation` "Riding to the start".
  func rideToStart() {
    startChoice = nil
    checkFerries()
  }

  /// Starts on the route nearby for this navigation only, then navigates, see "Starting nearby".
  func startNearby(_ choice: StartChoice) {
    startChoice = nil
    let nearby = choice.nearby
    let pending = changes
    Task {
      await pending?.value
      guard let session else { return }
      let cut = await Task.detached { session.startOnRouteAt(nearby: nearby) }.value
      await showForNavigation(cut)
      checkFerries()
    }
  }

  /// Shows `newSession`, a route for one navigation, without storing it.
  private func showForNavigation(_ newSession: PlanningSession) async {
    guard !navigating, let loaded else { return }
    let geometry = loaded.geometry
    let prepared = await Task.detached { PreparedSession(newSession, geometry: geometry) }.value
    if plannedSession == nil { plannedSession = session }
    show(prepared)
  }

  /// Shows the plan as made again after a route for one navigation, see "The plan after
  /// navigation".
  private func restorePlan() {
    guard let planned = plannedSession else { return }
    plannedSession = nil
    show(planned)
  }

  /// The question dismissed: nothing starts, see "Question dismissed".
  func dismissStartChoice() {
    startChoice = nil
  }

  private func navigate() {
    guard let session, let loaded,
      let knots = (session.route?.summary as? RouteSummaryComplete)?.knots,
      let description = routeDescription as? RouteDescriptionComplete
    else { return }
    // The route's squares are wanted again, see `network-sync` "Deleting with a route planned".
    if pathDataDeletedFor != nil {
      pathDataDeletedFor = nil
      loadPathData()
    }
    if placeDetailsDeletedFor != nil {
      placeDetailsDeletedFor = nil
      loadPlaceDetails()
    }
    if baseMapDeletedFor != nil {
      baseMapDeletedFor = nil
      loadBaseMap()
    }
    Navigator.shared.onDenied = { [weak self] in
      self?.message = String(localized: "Knoop by Knoop needs location access to navigate. You can allow it in Settings.", bundle: AppLanguage.bundle)
    }
    Navigator.shared.start(
      NavigationPlan(
        route: NavigationRoute(
          knots: knots, graph: session.routeGraph,
          geometry: loaded.hasGeometry ? loaded.geometry.withRoutePoints(routeGraph: session.routeGraph) : nil,
          stops: Set(description.stopPositions.map { KotlinInt(int: $0.int32Value) })),
        roles: description.knots.map { BadgeRole($0.role) }))
  }

  /// The course of the planned route along the network, or `nil` while it cannot be exported, see
  /// `map-planning` "Export the route as GPX": not while navigating, and not as straight lines.
  private var exportCourse: [GeoPoint]? {
    guard !navigating, let session, let route = session.route, let loaded, loaded.hasGeometry else { return nil }
    // Once per route and network: the waypoint sheet asks on every redraw, and building the course
    // took over 100 ms on the iPhone, which made the sheet stutter while it moved.
    if let cached = exportCourseCache, cached.session === session, cached.geometry === loaded.geometry {
      return cached.course
    }
    var course = loaded.geometry.withRoutePoints(routeGraph: session.routeGraph).courseOf(route: route)
    if let found = course, !Gpx.shared.canExport(route: route, course: found) { course = nil }
    exportCourseCache = (session, loaded.geometry, course)
    return course
  }

  /// The last `exportCourse`, with the session and the network's geometry it was built for.
  private var exportCourseCache: (session: PlanningSession, geometry: NetworkGeometry, course: [GeoPoint]?)?

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

  /// One of the other roles in the note, see `map-planning` "Changing what a tapped knot became".
  func chooseTap(_ choice: TapChoice) {
    guard let note = tapNote else { return }
    // Stays with the choice selected and its time started again, so a mistap can be put right.
    tapNotes += 1
    tapNote = TapNote(options: note.options.chosen(choice: choice), before: note.before, id: tapNotes)
    let session = choice.session
    change { _ in session }
  }

  /// "Undo" in the note: the route as it was before the tap.
  func undoTap() {
    guard let note = tapNote else { return }
    tapNote = nil
    change { _ in note.before }
  }

  /// The note's time is up, unless a newer note took its place.
  func dismissTapNote(id: Int) {
    if tapNote?.id == id { tapNote = nil }
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
      if let place {
        offer = nameOnly(place, at: point)
        lookUpDetails(place)
      }
      return
    }
    // Any badge of a knot, see `map-planning` "Knots signposted at several places".
    let knot = loaded.locator.nearestAtAnyNode(latitude: latitude, longitude: longitude, radiusMeters: radiusMeters)
    let tapped = session.tapAt(
      position: GeoPoint(latitude: latitude, longitude: longitude), radiusMeters: radiusMeters, knot: knot,
      geometry: loaded.geometry, place: place)
    switch tapped {
    case let placeOnly as MapTapPlaceOnly:
      offer = nameOnly(placeOnly.place, at: point)
      lookUpDetails(placeOnly.place)
    case let onKnot as MapTapOnKnot:
      let knot = onKnot.knot
      let before = session
      change { $0.tap(knot: knot) }
      let pending = changes
      tapNote = nil
      tapNotes += 1
      let id = tapNotes
      Task {
        await pending?.value
        // A note only for a knot added to a route, with the other roles' routes worked out off the
        // main thread, see "Changing what a tapped knot became".
        guard let after = self.session, after !== before else { return }
        let options = await Task.detached {
          TapOptions.companion.of(before: before, after: after, knot: knot)
        }.value
        guard let options, self.session === after, tapNotes == id else { return }
        tapNote = TapNote(options: options, before: before, id: id)
      }
    // A spot or a place offers "Add to route" first, see "Choose a route point on the map" and
    // "Tapping a place on the map".
    case let place as MapTapOffer:
      offer = PlaceOffer(offer: place, point: point)
      if let tappedPlace = place.place { lookUpDetails(tappedPlace) }
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

  /// Where a tapped spot or place is being added, for a spinner there until it is.
  @Published private(set) var addingAt: CGPoint?

  /// Adds a route point for `tapped` where it fits, as a tapped knot, off the main thread, then
  /// shows the note, see `map-planning` "Choose a route point on the map". A later tap cancels it;
  /// a route changed meanwhile, e.g. by the courses coming in, is worked out again.
  private func addPoint(_ session: PlanningSession, _ tapped: MapTapOffer, at point: CGPoint) {
    guard let geometry = loaded?.geometry else { return }
    tapNote = nil
    tapNotes += 1
    let id = tapNotes
    addingAt = point
    Task {
      let added = await Task.detached { () -> (Knot, PlanningSession)? in
        guard let routePoint = session.routePointFor(offer: tapped, geometry: geometry) else { return nil }
        return (routePoint, session.placed(point: routePoint))
      }.value
      guard tapNotes == id else { return }
      addingAt = nil
      guard let (routePoint, after) = added else { return }
      guard self.session === session else {
        if let current = self.session, !navigating { addPoint(current, tapped, at: point) }
        return
      }
      change { _ in after }
      findWays()
      let options = await Task.detached {
        TapOptions.companion.of(before: session, after: after, knot: routePoint)
      }.value
      guard let options, tapNotes == id else { return }
      tapNote = TapNote(options: options, before: session, id: id)
    }
  }

  /// "Add to route" for a spot or a place, see "Choose a route point on the map".
  func addOfferToRoute() {
    guard let opened = offer, let session else {
      offer = nil
      return
    }
    offer = nil
    addPoint(session, opened.offer, at: opened.point)
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
          // Squares arrived since are read again, see "While the path data loads".
          let ways = AppModel.wayFinder.routePointWays(geometry: geometry)
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

  /// One way finder for the planner's ways, used on `PathData.queue` only.
  nonisolated(unsafe) private static let wayFinder = WayFinder(store: PathData.store)

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
      // An edit makes the shown route the plan.
      plannedSession = nil
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
    loadPlaceDetails()
    loadBaseMap()
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
  /// Keeps the place details around the planned route on the device, for cycling and hiking, see
  /// `network-sync` "Place details around the route": like the path data, 2 seconds after the route
  /// last changed.
  private func loadPlaceDetails() {
    guard
      let wanted = PlaceSquares.shared.courseToLoad(course: plannedCourse(), deletedFor: placeDetailsDeletedFor)
    else {
      placeDetailsLoad?.cancellation.cancel()
      placeDetailsLoad?.task.cancel()
      placeDetailsLoad = nil
      return
    }
    if let running = placeDetailsLoad, running.course == wanted, running.online == online { return }
    placeDetailsLoad?.cancellation.cancel()
    placeDetailsLoad?.task.cancel()
    let cancellation = PathDataCancellation()
    let task = Task { [weak self] in
      try? await Task.sleep(for: .seconds(2))
      guard !Task.isCancelled else { return }
      nonisolated(unsafe) let course = wanted
      await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
        PathData.queue.async {
          _ = PathFile_iosKt.placeDownloader(store: PlaceDetailsData.store).loadAround(
            course: course,
            nowMillis: Int64(Date().timeIntervalSince1970 * 1000),
            isCancelled: { KotlinBoolean(bool: cancellation.isCancelled) })
          continuation.resume()
        }
      }
      guard !cancellation.isCancelled else { return }
      self?.placeDetailsChanged()
    }
    placeDetailsLoad = (task, cancellation, wanted, online)
  }

  /// Stores the base map around the planned route, for cycling and hiking, see `network-sync` "Base
  /// map around the route": like the path data, 2 seconds after the route last changed, again with
  /// network access coming back, and at navigation start.
  private func loadBaseMap() {
    guard
      let course = BaseMapSquares.shared.courseToLoad(course: plannedCourse(), deletedFor: baseMapDeletedFor),
      let area = BaseMapArea.companion.of(course: course)
    else {
      baseMapLoad?.task.cancel()
      baseMapLoad = nil
      baseMap = nil
      return
    }
    if let running = baseMapLoad, running.course == course, running.online == online { return }
    baseMapLoad?.task.cancel()
    let isOnline = online
    let task = Task { [weak self] in
      try? await Task.sleep(for: .seconds(2))
      guard !Task.isCancelled else { return }
      let status = await BaseMapStore.shared.store(area: area, online: isOnline) { self?.baseMap = $0 }
      guard !Task.isCancelled, let self else { return }
      baseMap = status
      baseMapBytes = BaseMapStore.shared.installedBytes
    }
    baseMapLoad = (task, course, online)
  }

  /// Deletes the stored base map; the planned route's comes again once it changes or navigation
  /// starts, see `network-sync` "Base map in the info sheet".
  func deleteBaseMap() {
    baseMapDeletedFor = plannedCourse() ?? []
    loadBaseMap()
    baseMapBytes = 0
    Task {
      await BaseMapStore.shared.clear()
      baseMapBytes = BaseMapStore.shared.installedBytes
    }
  }

  /// Shows what is installed now: the info sheet's entry and the open card's details.
  private func placeDetailsChanged() {
    placeDetailsDate = PlaceDetailsData.store.dataDate
    placeDetailsBytes = PlaceDetailsData.store.installedBytes()
    refreshOfferDetails()
  }

  /// Shows the details of the offered `place` from the installed squares; while planning, its
  /// square is downloaded if it's missing, see `network-sync` "Place details for a tapped place".
  private func lookUpDetails(_ place: MapPlace) {
    refreshOfferDetails()
    guard online,
      let square = PlaceSquares.shared.squareToLoad(
        position: place.position, navigating: navigating, store: PlaceDetailsData.store)
    else { return }
    nonisolated(unsafe) let course = plannedCourse()
    PathData.queue.async { [weak self] in
      _ = PathFile_iosKt.placeDownloader(store: PlaceDetailsData.store).loadSquare(
        square: square, course: course, nowMillis: Int64(Date().timeIntervalSince1970 * 1000))
      DispatchQueue.main.async { MainActor.assumeIsolated { self?.placeDetailsChanged() } }
    }
  }

  /// The offered place's details as installed now, see "Details arriving after the tap".
  private func refreshOfferDetails() {
    guard let place = offer?.offer.place else { return }
    let details = PlaceDetailsData.store.detailsOf(place: place)
    if offer?.offer.place == place, offer?.details != details { offer?.details = details }
  }

  /// Deletes the downloaded place details; they come again for a changed route, a place tapped while
  /// planning, or when navigation starts, see `network-sync` "Place details in the info sheet".
  func deletePlaceDetails() {
    placeDetailsDeletedFor = plannedCourse() ?? []
    loadPlaceDetails()
    placeDetailsDate = nil
    placeDetailsBytes = 0
    offer?.details = nil
    PathData.queue.async { PlaceDetailsData.store.clear() }
  }

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
    // The restored route is the plan as made, see `knot-navigation` "The plan after navigation".
    plannedSession = nil
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
      // Only the plan as made is stored, see `knot-navigation` "The plan after navigation".
      if let planned = plannedSession {
        let plannedReattached = planned.reattached(geometry: withCourses.geometry)
        plannedSession = plannedReattached
        if plannedReattached !== planned {
          storage.saveRoute(RouteSnapshot.companion.of(session: plannedReattached))
        }
      } else if reattached !== session {
        storage.saveRoute(RouteSnapshot.companion.of(session: reattached))
      }
      show(reattached)
      // A navigation started before the courses had loaded takes them now, see `knot-navigation`
      // "Courses loaded after the start".
      if navigating {
        Navigator.shared.takeCourses(withCourses.geometry.withRoutePoints(routeGraph: reattached.routeGraph))
      }
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

  /// Moves the map to `hit` and marks it; the route stays as it is.
  func choose(_ hit: PlaceHit) {
    placeSearch.chosen(hit.name)
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

  func clearSearch() {
    placeSearch.clear()
    marker = nil
  }

  private func refreshCatalog() {
    catalog = NetworkCatalog(
      index: nil, installed: storage.installed(), downloading: downloads.downloading(),
      failed: downloads.failed().union(syncState.failed))
  }

  /// Reloads the selected network when its files changed, keeping the route, see `network-sync`.
  private func reloadIfChanged() async {
    guard restored, let graphEntry = storage.graphEntry(network: network) else { return }
    if loaded?.graphSha256 != graphEntry.sha256 || loaded?.hasGeometry != status.hasGeometry {
      await loadNetwork(
        network, snapshot: (plannedSession ?? session).map { RouteSnapshot.companion.of(session: $0) })
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
    updateSyncState(syncState.started(toFetch: toFetch))
    downloads.start(toFetch)
    refreshCatalog()
  }

  private func networkAccessChanged(_ isOnline: Bool) {
    online = isOnline
    loadPathData()
    loadPlaceDetails()
    loadBaseMap()
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
          let check = NetworkInstaller.shared.check(
            entry: entry, bytes: Data_iosKt.byteArrayOf(data: data),
            installedGraphEntry: storage.graphEntry(network: entry.network),
            installedGraph: storage.graph(network: entry.network)?.graph)
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
          updateSyncState(syncState.rejected(entry: entry, reason: rejection))
        } else {
          updateSyncState(syncState.installed(entry: entry))
        }
      }
      // The run ends once nothing is left to download, see "Retrying a corrupted download".
      if downloads.downloading().isEmpty && downloads.finished().isEmpty {
        updateSyncState(syncState.finished())
        if syncState.retry { scheduleRetry() }
      }
      installing = false
      refreshCatalog()
      await reloadIfChanged()
      // Files that finished while installing are installed in the next round.
      if !downloads.finished().isEmpty { installFinished() }
    }
  }

  private func updateSyncState(_ newState: NetworkSyncState) {
    syncState = newState
    UserDefaults.standard.set(newState.toJson(), forKey: Self.syncStateKey)
  }

  /// Downloads a corrupted file again after the state's delay, while the app runs.
  private func scheduleRetry() {
    retryTask?.cancel()
    let delay = syncState.retryDelayMillis
    retryTask = Task { [weak self] in
      try? await Task.sleep(for: .milliseconds(delay))
      guard !Task.isCancelled else { return }
      await self?.sync()
    }
  }
}

/// The note after a tap: what the knot became and what else it can be, with the session `before`
/// the tap for Undo, see `map-planning` "Changing what a tapped knot became".
struct TapNote: @unchecked Sendable {
  let options: TapOptions
  let before: PlanningSession
  let id: Int
}
