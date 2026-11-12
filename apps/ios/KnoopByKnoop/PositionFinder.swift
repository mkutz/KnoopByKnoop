import CoreLocation
import KnoopByKnoopCore

/// Finds the rider's position once on request while planning, see `map-planning` "Show my
/// position": a single fix with its own location manager, separate from navigation's, given up
/// after 15 seconds.
@MainActor
final class PositionFinder: NSObject, CLLocationManagerDelegate {

  enum Result {
    case found(Fix)
    case notFound
    case denied
  }

  private let manager = CLLocationManager()
  private var completion: ((Result) -> Void)?
  private var timeout: Task<Void, Never>?
  private var timeoutDuration: Duration = .seconds(15)

  override init() {
    super.init()
    manager.delegate = self
    manager.desiredAccuracy = kCLLocationAccuracyBest
  }

  /// Whether location access is allowed, without asking.
  var hasAccess: Bool {
    manager.authorizationStatus == .authorizedWhenInUse || manager.authorizationStatus == .authorizedAlways
  }

  /// Asks for access "when in use" first if it was never asked for; gives up after [timeout].
  /// With [maxAge], a fix the phone found within that time and at least as accurate as [accuracy]
  /// is taken at once; a new one is sought at [accuracy], see `knot-navigation` "Start on the
  /// route nearby".
  func find(
    timeout: Duration = .seconds(15), accuracy: CLLocationAccuracy = kCLLocationAccuracyBest,
    maxAge: TimeInterval? = nil, _ completion: @escaping (Result) -> Void
  ) {
    // Busy with another request: no position for this one, so nobody waits for nothing.
    guard self.completion == nil else {
      completion(.notFound)
      return
    }
    if hasAccess, let maxAge, let recent = manager.location,
      -recent.timestamp.timeIntervalSinceNow <= maxAge,
      recent.horizontalAccuracy >= 0, recent.horizontalAccuracy <= max(accuracy, 0)
    {
      completion(.found(Self.fix(recent)))
      return
    }
    self.completion = completion
    timeoutDuration = timeout
    manager.desiredAccuracy = accuracy
    switch manager.authorizationStatus {
    case .notDetermined: manager.requestWhenInUseAuthorization()
    case .denied, .restricted: finish(.denied)
    default: request()
    }
  }

  private func request() {
    manager.requestLocation()
    timeout = Task { [weak self] in
      try? await Task.sleep(for: self?.timeoutDuration ?? .seconds(15))
      guard !Task.isCancelled else { return }
      self?.finish(.notFound)
    }
  }

  private func finish(_ result: Result) {
    timeout?.cancel()
    timeout = nil
    manager.desiredAccuracy = kCLLocationAccuracyBest
    let completion = completion
    self.completion = nil
    completion?(result)
  }

  nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
    MainActor.assumeIsolated {
      guard completion != nil, timeout == nil else { return }
      switch manager.authorizationStatus {
      case .notDetermined: break
      case .denied, .restricted: finish(.denied)
      default: request()
      }
    }
  }

  nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
    MainActor.assumeIsolated {
      guard let location = locations.last, completion != nil else { return }
      finish(.found(Self.fix(location)))
    }
  }

  nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
    MainActor.assumeIsolated {
      // An unknown location is temporary: the timeout decides.
      if (error as? CLError)?.code == .locationUnknown { return }
      if (error as? CLError)?.code == .denied { finish(.denied) } else { finish(.notFound) }
    }
  }

  private static func fix(_ location: CLLocation) -> Fix {
    Fix(
      latitude: location.coordinate.latitude, longitude: location.coordinate.longitude,
      accuracyMeters: location.horizontalAccuracy,
      timeMillis: Int64(location.timestamp.timeIntervalSince1970 * 1000))
  }
}
