import ActivityKit
import AVFoundation
import CoreLocation
import KnoopByKnoopCore
import os

/// What the navigation shows: the navigation, `nil` while waiting for the first fix, and the
/// rider's last position.
struct NavigationState {
  let navigation: Navigation?
  let position: Fix?
  /// The track ridden since navigation started, see `knot-navigation` "Ridden track".
  var track = RiddenTrack(points: [])
  /// The riding direction from the most recent movement, or `nil` when standing.
  var direction: Double? = nil
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
  /// Kept here, not in the map, so it includes the stretches ridden with the screen off.
  private var track = RiddenTrack(points: [])
  private var waitingForAccess = false
  private var precision: Precision?
  private var activity: Activity<NavigationActivityAttributes>?
  /// Set after arriving: navigation ends once "You've arrived" was spoken.
  private var stopping = false

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
    state = nil
    let ending = activity
    activity = nil
    return ending
  }

  private func begin() {
    stopping = false
    track = RiddenTrack(points: [])
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
    for notice in update.notices {
      logger.notice("notice: \(notice.text, privacy: .public)")
      speak(notice.speech)
    }
    track = track.add(fix: fix)
    state = NavigationState(
      navigation: navigation, position: fix, track: track,
      direction: track.directionAt(fix: fix).map { $0.doubleValue })
    if navigation.arrived {
      // "You've arrived" is spoken before navigation ends, see `knot-navigation` "Arriving".
      stopping = true
      manager.stopUpdatingLocation()
      if !speech.isSpeaking { stop() }
      return
    }
    updateActivity(navigation.display, nextIndex: Int(navigation.nextIndex), progress: navigation.progressLine)
    request(update.precision)
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
        next: "", follow: nil, offRoute: false, nextRole: nil, followRole: nil, text: String(localized: "Starting navigation…", bundle: AppLanguage.bundle)),
      staleDate: nil)
    activity = try? Activity.request(attributes: NavigationActivityAttributes(), content: content)
  }

  private func updateActivity(_ display: NavigationDisplay, nextIndex: Int, progress: String? = nil) {
    let roles = route?.roles ?? []
    let state = NavigationActivityAttributes.ContentState(
      next: display.next, follow: display.follow, offRoute: display.offRoute,
      nextRole: roles.indices.contains(nextIndex) ? roles[nextIndex] : nil,
      followRole: roles.indices.contains(nextIndex + 1) ? roles[nextIndex + 1] : nil,
      text: display.offRoute ? display.offRouteText : display.text,
      progress: progress)
    guard let activity else {
      // Live Activities end after about 8 hours; opening the app starts a new one.
      if visible { startActivity() }
      return
    }
    guard activity.content.state != state else { return }
    Task { await activity.update(ActivityContent(state: state, staleDate: nil)) }
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
