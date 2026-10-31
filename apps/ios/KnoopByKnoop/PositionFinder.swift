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

  override init() {
    super.init()
    manager.delegate = self
    manager.desiredAccuracy = kCLLocationAccuracyBest
  }

  /// Asks for access "when in use" first if it was never asked for.
  func find(_ completion: @escaping (Result) -> Void) {
    guard self.completion == nil else { return }
    self.completion = completion
    switch manager.authorizationStatus {
    case .notDetermined: manager.requestWhenInUseAuthorization()
    case .denied, .restricted: finish(.denied)
    default: request()
    }
  }

  private func request() {
    manager.requestLocation()
    timeout = Task { [weak self] in
      try? await Task.sleep(for: .seconds(15))
      guard !Task.isCancelled else { return }
      self?.finish(.notFound)
    }
  }

  private func finish(_ result: Result) {
    timeout?.cancel()
    timeout = nil
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
      finish(
        .found(
          Fix(
            latitude: location.coordinate.latitude, longitude: location.coordinate.longitude,
            accuracyMeters: location.horizontalAccuracy,
            timeMillis: Int64(location.timestamp.timeIntervalSince1970 * 1000))))
    }
  }

  nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
    MainActor.assumeIsolated {
      // An unknown location is temporary: the timeout decides.
      if (error as? CLError)?.code == .locationUnknown { return }
      if (error as? CLError)?.code == .denied { finish(.denied) } else { finish(.notFound) }
    }
  }
}
