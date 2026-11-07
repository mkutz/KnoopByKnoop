import ActivityKit
import AVFoundation
import CoreLocation
import KnoopByKnoopCore
import UIKit
import os

/// What the navigation shows: the navigation, `nil` while waiting for the first fix, and the
/// rider's last position.
struct NavigationState {
  let navigation: Navigation?
  let position: Fix?
  /// The track ridden since navigation started, see `knot-navigation` "Ridden track".
  var track = RiddenTrack(points: [], network: .cycling)
  /// The riding direction from the most recent movement, or `nil` when standing.
  var direction: Double? = nil
  /// The way back to the route, see `knot-navigation` "Guided way back to the route".
  var guidedWay: GuidedWay? = nil
}

/// A route to navigate: what `Navigation.start` needs. Without courses, `geometry` is `nil`:
/// straight lines do not count as a course.
struct NavigationRoute {
  let knots: [Knot]
  /// How each of `knots` takes part in the route, for the badges of the Live Activity.
  let roles: [BadgeRole]
  let graph: NetworkGraph
  let geometry: NetworkGeometry?
  /// The positions of the stops in `knots`, see `knot-navigation` "Distances and times ahead".
  var stops: Set<Int> = []
}

/// Runs the navigation, also with the screen locked, see `knot-navigation`: it feeds the fixes to
/// the core's `Navigation`, speaks the notices and shows the number to follow in a Live Activity.
/// Location access "when in use" is enough, as navigation is started while the app is visible.
@MainActor
final class Navigator: NSObject, ObservableObject {

  static let shared = Navigator()

  /// The navigation, or `nil` when not navigating.
  @Published private(set) var state: NavigationState?

  /// Whether the app is on screen, so it samples live.
  var visible = true {
    didSet {
      if let navigation = state?.navigation { request(navigation.precision(visible: visible)) }
    }
  }

  /// Called when navigation cannot start because location access was denied.
  var onDenied: () -> Void = {}

  private let manager = CLLocationManager()
  private let speech = AVSpeechSynthesizer()
  /// Keeps location updates coming while the app is in the background.
  private var backgroundSession: CLBackgroundActivitySession?
  private var route: NavigationRoute?
  /// The network's courses, to tell whether the rider is on a path of the knot network off route.
  var geometry: NetworkGeometry? { route?.geometry }
  /// Kept here, not in the map, so it includes the stretches ridden with the screen off.
  private var track = RiddenTrack(points: [], network: .cycling)
  private var waitingForAccess = false
  private var precision: Precision?
  private var activity: Activity<NavigationActivityAttributes>?
  /// Set after arriving: navigation ends once "You've arrived" was spoken.
  private var stopping = false
  /// The way back to the route, see `knot-navigation` "Guided way back to the route".
  private let wayGuide = WayGuide()
  private var guidedWay: GuidedWay? { wayGuide.way }
  /// Finds ways one at a time, off the main thread; only used on this queue.
  private static let pathQueue = DispatchQueue(label: "com.knoopbyknoop.paths", qos: .userInitiated)
  nonisolated(unsafe) private static let pathRouter = PathRouter(file: PathData.store)
  nonisolated(unsafe) private static var pathRouterVersion: Int32 = -1

  /// Notices and precision changes are logged, to check navigation on a ride and to tune the
  /// precision for the battery. Read them in Console with the category `Navigation`.
  nonisolated private let logger = Logger(subsystem: "com.knoopbyknoop", category: "Navigation")

  private override init() {
    super.init()
    manager.delegate = self
    speech.delegate = self
    // A rider waiting at a ferry or a traffic light keeps getting notices afterwards, see spike.md.
    manager.pausesLocationUpdatesAutomatically = false
    manager.activityType = .otherNavigation
    StopNavigationIntent.stop = { [weak self] in self?.stop() }
  }

  /// Ends a background session left over from a navigation the app did not end, e.g. because it
  /// was closed while navigating: the system keeps such a session, and with it the location
  /// indicator, beyond the app's end, see `knot-navigation` "No location use after navigation".
  ///
  /// Only after a navigation the app did not end: creating a session on every start and ending it
  /// right away left a session of its own behind (found on the iPhone). The cleanup session is ended
  /// a little later, once the system has registered it.
  func endLeftoverSession() {
    guard state == nil, UserDefaults.standard.bool(forKey: Self.sessionActiveKey) else { return }
    UserDefaults.standard.removeObject(forKey: Self.sessionActiveKey)
    let status = manager.authorizationStatus
    guard status == .authorizedWhenInUse || status == .authorizedAlways else { return }
    let cleanup = CLBackgroundActivitySession()
    DispatchQueue.main.asyncAfter(deadline: .now() + 1) { cleanup.invalidate() }
  }

  /// Set while a background session of a navigation is active, so a start after the app ended
  /// without ending navigation knows there may be one left.
  private static let sessionActiveKey = "navigationSessionActive"

  /// Starts navigating `route`, asking for location access first if needed.
  func start(_ route: NavigationRoute) {
    guard state == nil else { return }
    self.route = route
    switch manager.authorizationStatus {
    case .notDetermined:
      waitingForAccess = true
      manager.requestWhenInUseAuthorization()
    case .authorizedWhenInUse, .authorizedAlways:
      begin()
    default:
      self.route = nil
      onDenied()
    }
  }

  func stop() {
    let ending = endNavigation()
    Task { await ending?.end(nil, dismissalPolicy: .immediate) }
  }

  /// Ends navigation when the app is closed, see `knot-navigation` "Closing the app while
  /// navigating": waits up to a second for the Live Activity to go away, as the app ends right after.
  func stopBeforeTermination() {
    guard state != nil else { return }
    let ending = endNavigation()
    let ended = DispatchSemaphore(value: 0)
    Task.detached {
      await ending?.end(nil, dismissalPolicy: .immediate)
      ended.signal()
    }
    _ = ended.wait(timeout: .now() + 1)
  }

  /// Stops speech, location and the background session; returns the Live Activity, for the caller to
  /// end it.
  private func endNavigation() -> Activity<NavigationActivityAttributes>? {
    if speech.isSpeaking { speech.stopSpeaking(at: .immediate) }
    manager.stopUpdatingLocation()
    manager.allowsBackgroundLocationUpdates = false
    backgroundSession?.invalidate()
    backgroundSession = nil
    UserDefaults.standard.removeObject(forKey: Self.sessionActiveKey)
    route = nil
    precision = nil
    stopping = false
    wayGuide.reset()
    state = nil
    let ending = activity
    activity = nil
    return ending
  }

  private func begin() {
    stopping = false
    track = RiddenTrack(points: [], network: .cycling)
    state = NavigationState(navigation: nil, position: nil)
    manager.allowsBackgroundLocationUpdates = true
    manager.showsBackgroundLocationIndicator = true
    backgroundSession = CLBackgroundActivitySession()
    UserDefaults.standard.set(true, forKey: Self.sessionActiveKey)
    // The first fix decides where navigation begins, so it is sampled precisely.
    request(.fine)
    manager.startUpdatingLocation()
    startActivity()
  }

  private func onLocation(_ location: CLLocation) {
    guard let route, !stopping, location.horizontalAccuracy >= 0 else { return }
    let fix = Fix(
      latitude: location.coordinate.latitude, longitude: location.coordinate.longitude,
      accuracyMeters: location.horizontalAccuracy, timeMillis: Int64(location.timestamp.timeIntervalSince1970 * 1000))
    let update: NavigationUpdate
    if let navigation = state?.navigation {
      update = navigation.update(fix: fix, visible: visible)
    } else if let started = Navigation.companion.start(
      knots: route.knots, graph: route.graph, geometry: route.geometry, fix: fix, visible: visible,
      stops: Set(route.stops.map { KotlinInt(int: Int32($0)) }),
      localTime: { millis in
        RouteFerries.ferryMoment(Date(timeIntervalSince1970: Double(millis.int64Value) / 1000))
      }, language: AppLanguage.current, speechLanguage: AppLanguage.speech)
    {
      update = started
    } else {
      stop()
      return
    }
    apply(update, fix: fix)
  }

  private func apply(_ update: NavigationUpdate, fix: Fix) {
    let navigation = update.navigation
    let step = wayGuide.onFix(
      navigation: navigation,
      notices: update.notices,
      fix: fix,
      network: route?.graph.network,
      heading: track.directionAt(fix: fix),
      nowMillis: Int64(Date().timeIntervalSince1970 * 1000),
      speechLanguage: AppLanguage.speech)
    if let search = step.search { findWay(search) }
    for notice in step.notices {
      logger.notice("notice: \(notice.text, privacy: .public)")
      speak(notice.speech)
    }
    // The route's network sets the speed a fix may not exceed, see `knot-navigation` "Ridden track".
    if let network = route?.graph.network, network != track.network {
      track = track.doCopy(points: track.points, network: network)
    }
    track = track.add(fix: fix)
    publish(navigation, fix: fix)
    if navigation.arrived {
      // "You've arrived" is spoken before navigation ends, see `knot-navigation` "Arriving".
      stopping = true
      manager.stopUpdatingLocation()
      if !speech.isSpeaking { stop() }
      return
    }
    showActivity(navigation, fix: fix)
    request(update.precision)
  }

  private func publish(_ navigation: Navigation, fix: Fix) {
    state = NavigationState(
      navigation: navigation, position: fix, track: track,
      direction: track.directionAt(fix: fix).map { $0.doubleValue }, guidedWay: guidedWay)
  }

  private func showActivity(_ navigation: Navigation, fix: Fix) {
    let row = navigation.header(
      fix: fix, ridingDirection: track.directionAt(fix: fix), guidedWay: guidedWay,
      network: route?.geometry)
    updateActivity(
      navigation.display, nextIndex: Int(navigation.nextIndex),
      progress: guidedWay?.view(language: navigation.language).line ?? navigation.progressLine,
      strip: navigation.progress(fix: fix, guidedWay: guidedWay).strip,
      row: Self.rowState(row, endArrival: navigation.endArrival(fix: fix, guidedWay: guidedWay)))
  }

  /// Looks for a way off the main thread, see `WayGuide`.
  private func findWay(_ search: WaySearch) {
    let logger = logger
    nonisolated(unsafe) let search = search
    Self.pathQueue.async {
      // Squares may have arrived or gone, see `knot-navigation` "Path data arrives on the way".
      if Self.pathRouterVersion != PathData.store.version {
        Self.pathRouter.forgetSquares()
        Self.pathRouterVersion = PathData.store.version
      }
      let started = Date()
      let way = Self.pathRouter.way(from: search.from, to: search.to, heading: search.heading)
      let millis = Int(Date().timeIntervalSince(started) * 1000)
      logger.notice(
        "guided way: \(way.map { Int($0.lengthMeters) } ?? -1, privacy: .public) m, \(way?.turns.count ?? -1, privacy: .public) turns, \(millis, privacy: .public) ms, \(Self.pathRouter.visitedSteps, privacy: .public) steps, \(Self.pathRouter.loadedSquares, privacy: .public) squares")
      #if DEBUG
        // For a mocked ride to follow the way; this is where the rider goes.
        if let way {
          let points = way.points.map { "\($0.longitude),\($0.latitude)" }.joined(separator: ";")
          logger.debug("way points: \(points, privacy: .public)")
        }
      #endif
      nonisolated(unsafe) let found = way
      DispatchQueue.main.async {
        MainActor.assumeIsolated { self.foundWay(found) }
      }
    }
  }

  private func foundWay(_ way: PathWay?) {
    wayGuide.found(found: way, navigation: stopping ? nil : state?.navigation)
    guard !stopping, let navigation = state?.navigation, let position = state?.position,
      navigation.offRoute, !navigation.arrived
    else { return }
    publish(navigation, fix: position)
    showActivity(navigation, fix: position)
  }

  /// Samples with `newPrecision`, mapped as in the design's precision table.
  private func request(_ newPrecision: Precision) {
    guard newPrecision != precision else { return }
    precision = newPrecision
    logger.notice("precision: \(newPrecision.name, privacy: .public)")
    switch newPrecision {
    case .coarse:
      manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
      manager.distanceFilter = 100
    case .fine:
      manager.desiredAccuracy = kCLLocationAccuracyBest
      manager.distanceFilter = 10
    default:
      manager.desiredAccuracy = kCLLocationAccuracyBestForNavigation
      manager.distanceFilter = kCLDistanceFilterNone
    }
  }

  // MARK: Speaking, see `knot-navigation` "Spoken notices"

  /// Notices handed to the synthesizer and not yet finished: other audio is lowered until none is
  /// left, see `knot-navigation` "Music playing". Asking the synthesizer instead is unreliable: in
  /// its `didFinish`, it may still report that it is speaking.
  private var pendingNotices = 0

  /// The audio session's calls can block for a while, so they run off the main thread; one after
  /// the other, so taking and giving back the audio keep their order.
  private static let audioQueue = DispatchQueue(label: "com.knoopbyknoop.audio")

  private func speak(_ text: String) {
    pendingNotices += 1
    Self.audioQueue.async { [weak self] in
      self?.takeAudio()
      // Spoken once the audio is taken, so the notice is not cut off at its start.
      Task { @MainActor in
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AppLanguage.voice
        self?.speech.speak(utterance)
      }
    }
  }

  nonisolated private func takeAudio() {
    let audio = AVAudioSession.sharedInstance()
    do {
      // Other audio is lowered while a notice is spoken, and comes back after it.
      try audio.setCategory(.playback, mode: .voicePrompt, options: [.duckOthers])
      try audio.setActive(true)
    } catch {
      logger.error("audio session: \(error.localizedDescription, privacy: .public)")
    }
  }

  private func finishedSpeaking() {
    pendingNotices = max(0, pendingNotices - 1)
    guard pendingNotices == 0 else { return }
    releaseAudio(retrying: true)
    if stopping { stop() }
  }

  /// Gives the audio back, so other audio plays at its volume again; retried once if the audio is
  /// still busy.
  private func releaseAudio(retrying: Bool) {
    Self.audioQueue.async { [weak self] in
      guard let self else { return }
      do {
        try AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
      } catch {
        logger.error("releasing audio: \(error.localizedDescription, privacy: .public)")
        guard retrying else { return }
        Task { @MainActor [weak self] in
          try? await Task.sleep(for: .milliseconds(500))
          guard let self, self.pendingNotices == 0 else { return }
          self.releaseAudio(retrying: false)
        }
      }
    }
  }

  // MARK: Live Activity, see `knot-navigation` "Navigation with the screen off"

  private func startActivity() {
    guard ActivityAuthorizationInfo().areActivitiesEnabled else { return }
    let content = ActivityContent(
      state: NavigationActivityAttributes.ContentState(
        next: "", follow: nil, offRoute: false, nextRole: nil, followRole: nil, text: String(localized: "Starting navigation…", bundle: AppLanguage.bundle),
        dark: Self.systemIsDark, language: AppLanguage.code),
      staleDate: nil)
    activity = try? Activity.request(attributes: NavigationActivityAttributes(), content: content)
  }

  private func updateActivity(
    _ display: NavigationDisplay, nextIndex: Int, progress: String? = nil, strip: ProgressStrip,
    row: NavigationActivityAttributes.HeaderRowState? = nil
  ) {
    let roles = route?.roles ?? []
    // The bar moves with the line, about every 100 m, not with every fix.
    let current = activity?.content.state
    let renewBar = current?.progress != progress || current?.offRoute != display.offRoute
      || current?.wrongWay != display.wrongWay || current?.row?.kind != row?.kind
    let state = NavigationActivityAttributes.ContentState(
      next: display.next, follow: display.follow, offRoute: display.offRoute,
      nextRole: roles.indices.contains(nextIndex) ? roles[nextIndex] : nil,
      followRole: roles.indices.contains(nextIndex + 1) ? roles[nextIndex + 1] : nil,
      text: display.offRoute ? display.offRouteText : display.text,
      progress: progress,
      // A route point has no signs to follow, see `knot-navigation` "Navigating with route points".
      close: display.close && display.nextRole == nil && display.followRole == nil,
      riderFraction: renewBar ? strip.rider : current?.riderFraction,
      marks: renewBar ? Self.barMarks(of: strip) : current?.marks ?? [],
      dark: Self.systemIsDark,
      wrongWay: display.wrongWay,
      language: AppLanguage.code,
      row: row)
    guard let activity else {
      // Live Activities end after about 8 hours; opening the app starts a new one.
      if visible { startActivity() }
      return
    }
    guard activity.content.state != state else { return }
    Task { await activity.update(ActivityContent(state: state, staleDate: nil)) }
  }
}

extension Navigator {

  /// The header's row for the Live Activity, see `knot-navigation` "Navigation with the screen off".
  fileprivate static func rowState(_ row: HeaderRow, endArrival: String?)
    -> NavigationActivityAttributes.HeaderRowState
  {
    func knot(_ badge: HeaderBadge?) -> NavigationActivityAttributes.HeaderKnotState? {
      guard let badge else { return nil }
      return .init(
        ref: badge.ref, colour: UInt32(truncatingIfNeeded: badge.plate.colour) & 0xFFFFFF,
        innerRing: badge.plate.innerRing, filled: badge.filled,
        role: badge.role == .passed ? nil : BadgeRole(badge.role))
    }
    if let close = row as? HeaderRow.Close {
      return .init(
        kind: .close, next: knot(close.next), follow: knot(close.follow), distance: close.distance,
        turnDegrees: close.turn.map { Self.turnDegrees($0) }, endArrival: endArrival)
    }
    if let far = row as? HeaderRow.Far {
      return .init(
        kind: .far, next: knot(far.next), follow: knot(far.follow), distance: far.distance,
        endArrival: endArrival)
    }
    if let end = row as? HeaderRow.End {
      return .init(kind: .end, next: knot(end.next), distance: end.distance, endArrival: endArrival)
    }
    if let wrongWay = row as? HeaderRow.WrongWay {
      return .init(
        kind: .wrongWay, next: knot(wrongWay.next), distance: wrongWay.distance, inverted: true,
        endArrival: endArrival)
    }
    let toRoute = row as! HeaderRow.ToRoute
    return .init(
      kind: .toRoute, next: knot(toRoute.next), distance: toRoute.distance,
      arrowDegrees: toRoute.arrowDegrees,
      turnDegrees: toRoute.guided.map { Self.turnDegrees($0.turn) },
      turnDistance: toRoute.guided?.turnDistance, remaining: toRoute.guided?.remaining,
      inverted: toRoute.offRoute, offRoute: toRoute.offRoute, endArrival: endArrival)
  }

  /// The angle a turn on the guided way turns the arrow, from straight ahead.
  private static func turnDegrees(_ turn: TurnDirection?) -> Double {
    switch turn {
    case .slightLeft?: -45
    case .left?: -90
    case .sharpLeft?: -135
    case .slightRight?: 45
    case .right?: 90
    case .sharpRight?: 135
    case .back?: 180
    default: 0
    }
  }

  /// Whether the system is in dark mode, from the screen, as the app's windows follow the appearance
  /// chosen in Info, see `map-planning` "Light and dark appearance".
  fileprivate static var systemIsDark: Bool {
    let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
    return scene?.screen.traitCollection.userInterfaceStyle == .dark
  }

  /// The most marks on the bar in the Live Activity, whose state has to stay small.
  private static let maximumBarMarks = 8

  /// The stops and ferries ahead on `strip`, the most important first, as on the strip's labels.
  fileprivate static func barMarks(of strip: ProgressStrip) -> [NavigationActivityAttributes.BarMark] {
    let ahead = strip.marks.filter { $0.fraction > strip.rider }
    let closed = ahead.filter { $0.kind == .ferry && $0.state == .warning }
      .map { NavigationActivityAttributes.BarMark(fraction: $0.fraction, kind: .closedFerry) }
    let stops = ahead.filter { $0.kind == .stop }
      .map { NavigationActivityAttributes.BarMark(fraction: $0.fraction, kind: .stop) }
    let ferries = ahead.filter { $0.kind == .ferry && $0.state != .warning }
      .map { NavigationActivityAttributes.BarMark(fraction: $0.fraction, kind: .ferry) }
    return Array((closed + stops + ferries).prefix(maximumBarMarks))
  }
}

extension Navigator: CLLocationManagerDelegate {

  nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
    MainActor.assumeIsolated { locations.forEach(onLocation) }
  }

  nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
    MainActor.assumeIsolated {
      guard waitingForAccess else { return }
      switch manager.authorizationStatus {
      case .notDetermined:
        return
      case .authorizedWhenInUse, .authorizedAlways:
        waitingForAccess = false
        begin()
      default:
        waitingForAccess = false
        route = nil
        onDenied()
      }
    }
  }
}

extension Navigator: AVSpeechSynthesizerDelegate {

  nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
    MainActor.assumeIsolated { finishedSpeaking() }
  }

  nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
    MainActor.assumeIsolated { finishedSpeaking() }
  }
}
