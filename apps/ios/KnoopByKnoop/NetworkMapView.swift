import KnoopByKnoopCore
import MapLibre
import SwiftUI

/// OpenStreetMap-based base map without API key, see the design of `add-planning-screens`.
private let baseMapStyle = URL(string: "https://tiles.openfreemap.org/styles/liberty")!

/// Used when the base map cannot be loaded, e.g. offline: the network layers still show.
private let offlineStyle = """
  {"version": 8, "sources": {}, "layers": [{"id": "background", "type": "background", \
  "paint": {"background-color": "#EEF1EA"}}]}
  """

/// Knots are drawn and can be tapped from this zoom level on.
private let knotMinimumZoom = 10.0

/// The touch target around a tap within which the nearest knot is picked.
private let touchTargetPoints = 24.0

/// The map with the network and the route. Taps are reported with the touch target in meters at
/// the current zoom level, so the core's knot locator can pick the knot.
struct NetworkMapView: UIViewRepresentable {

  let graph: NetworkGraph
  let layers: MapLayers
  /// The knots to show when a network is opened, e.g. those of a restored route.
  let focus: [Knot]
  let onTap: (Double, Double, Double) -> Void

  func makeCoordinator() -> Coordinator { Coordinator() }

  func makeUIView(context: Context) -> MLNMapView {
    let mapView = MLNMapView(frame: .zero, styleURL: baseMapStyle)
    mapView.delegate = context.coordinator
    mapView.compassView.isHidden = true
    let tap = UITapGestureRecognizer(target: context.coordinator, action: #selector(Coordinator.tapped(_:)))
    // Double taps zoom the map and must not select knots.
    for recognizer in mapView.gestureRecognizers ?? [] {
      if let other = recognizer as? UITapGestureRecognizer, other.numberOfTapsRequired == 2 {
        tap.require(toFail: other)
      }
    }
    mapView.addGestureRecognizer(tap)
    return mapView
  }

  func updateUIView(_ mapView: MLNMapView, context: Context) {
    context.coordinator.onTap = onTap
    context.coordinator.show(graph: graph, layers: layers, focus: focus, on: mapView)
  }

  /// Keeps the style and what is shown, so updates only touch what changed.
  final class Coordinator: NSObject, MLNMapViewDelegate {

    var onTap: (Double, Double, Double) -> Void = { _, _, _ in }
    private weak var mapView: MLNMapView?
    private var style: MLNStyle?
    private var graph: NetworkGraph?
    private var layers: MapLayers?
    private var focus: [Knot] = []
    private var shownGraph: NetworkGraph?
    private var shownNetwork: URL?
    private var shownRoute: String?
    private var shownWaypoints: String?

    func show(graph: NetworkGraph, layers: MapLayers, focus: [Knot], on mapView: MLNMapView) {
      self.mapView = mapView
      self.graph = graph
      self.layers = layers
      self.focus = focus
      update()
    }

    @objc func tapped(_ recognizer: UITapGestureRecognizer) {
      guard let mapView, mapView.zoomLevel >= knotMinimumZoom else { return }
      let coordinate = mapView.convert(recognizer.location(in: mapView), toCoordinateFrom: mapView)
      let metersPerPoint = mapView.metersPerPoint(atLatitude: coordinate.latitude)
      onTap(coordinate.latitude, coordinate.longitude, metersPerPoint * touchTargetPoints)
    }

    func mapView(_ mapView: MLNMapView, didFinishLoading style: MLNStyle) {
      self.style = style
      shownNetwork = nil
      shownRoute = nil
      shownWaypoints = nil
      addLayers(to: style)
      update()
    }

    func mapViewDidFailLoadingMap(_ mapView: MLNMapView, withError error: Error) {
      let file = FileManager.default.temporaryDirectory.appendingPathComponent("offline-style.json")
      try? Data(offlineStyle.utf8).write(to: file)
      mapView.styleURL = file
    }

    private func update() {
      guard let style, let layers else { return }
      if shownNetwork != layers.connections {
        for ref in layers.refs where style.image(forName: "knot-\(ref)") == nil {
          style.setImage(badge(ref), forName: "knot-\(ref)")
        }
        (style.source(withIdentifier: "knots") as? MLNShapeSource)?.url = layers.knots
        (style.source(withIdentifier: "connections") as? MLNShapeSource)?.url = layers.connections
        shownNetwork = layers.connections
      }
      if shownRoute != layers.route {
        (style.source(withIdentifier: "route") as? MLNShapeSource)?.shape = shape(layers.route)
        shownRoute = layers.route
      }
      // Waypoints change without the route, e.g. when only the start is set.
      if shownWaypoints != layers.waypoints {
        (style.source(withIdentifier: "waypoints") as? MLNShapeSource)?.shape = shape(layers.waypoints)
        shownWaypoints = layers.waypoints
      }
      if shownGraph !== graph, let graph {
        // A new network shows the restored route if there is one, the whole network otherwise.
        fit(to: focus.count >= 2 ? focus : Array(graph.knots.values))
        shownGraph = graph
      }
    }

    private func fit(to knots: [Knot]) {
      guard knots.count >= 2, let mapView else { return }
      let coordinates = knots.map { CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude) }
      let bounds = MLNCoordinateBounds(
        sw: CLLocationCoordinate2D(
          latitude: coordinates.map(\.latitude).min()!, longitude: coordinates.map(\.longitude).min()!),
        ne: CLLocationCoordinate2D(
          latitude: coordinates.map(\.latitude).max()!, longitude: coordinates.map(\.longitude).max()!))
      mapView.setVisibleCoordinateBounds(
        bounds, edgePadding: UIEdgeInsets(top: 140, left: 40, bottom: 220, right: 40), animated: false,
        completionHandler: nil)
    }

    private func shape(_ geoJson: String) -> MLNShape? {
      try? MLNShape(data: Data(geoJson.utf8), encoding: String.Encoding.utf8.rawValue)
    }

    private func addLayers(to style: MLNStyle) {
      // Large network layers come from files, so the map parses them off the main thread.
      let sources = ["connections", "route", "waypoints", "knots"].map { MLNShapeSource(identifier: $0, shape: nil) }
      sources.forEach(style.addSource)

      let connections = MLNLineStyleLayer(identifier: "connections", source: sources[0])
      connections.lineColor = NSExpression(forConstantValue: UIColor(Color.knoopGreen))
      connections.lineWidth = NSExpression(forConstantValue: 2)
      connections.lineOpacity = NSExpression(forConstantValue: 0.7)
      style.addLayer(connections)

      let route = MLNLineStyleLayer(identifier: "route", source: sources[1])
      route.predicate = NSPredicate(format: "reachable == YES")
      route.lineColor = NSExpression(forConstantValue: UIColor(Color.knoopOrange))
      route.lineWidth = NSExpression(forConstantValue: 6)
      route.lineCap = NSExpression(forConstantValue: "round")
      route.lineJoin = NSExpression(forConstantValue: "round")
      style.addLayer(route)

      let unreachable = MLNLineStyleLayer(identifier: "route-unreachable", source: sources[1])
      unreachable.predicate = NSPredicate(format: "reachable == NO")
      unreachable.lineColor = NSExpression(forConstantValue: UIColor.systemRed)
      unreachable.lineWidth = NSExpression(forConstantValue: 4)
      unreachable.lineDashPattern = NSExpression(forConstantValue: [2, 2])
      style.addLayer(unreachable)

      let waypoints = MLNCircleStyleLayer(identifier: "waypoints", source: sources[2])
      waypoints.circleRadius = NSExpression(forConstantValue: 16)
      waypoints.circleColor = NSExpression(forConstantValue: UIColor(Color.knoopOrange))
      waypoints.circleStrokeColor = NSExpression(forConstantValue: UIColor.white)
      waypoints.circleStrokeWidth = NSExpression(forConstantValue: 3)
      style.addLayer(waypoints)

      let knots = MLNSymbolStyleLayer(identifier: "knots", source: sources[3])
      knots.iconImageName = NSExpression(format: "mgl_join({'knot-', ref})")
      knots.iconAllowsOverlap = NSExpression(forConstantValue: true)
      knots.iconIgnoresPlacement = NSExpression(forConstantValue: true)
      knots.minimumZoomLevel = Float(knotMinimumZoom)
      style.addLayer(knots)
    }

    /// A round badge with the knot's number, drawn locally so numbers show without network access.
    private func badge(_ ref: String) -> UIImage {
      let size = CGSize(width: 22, height: 22)
      return UIGraphicsImageRenderer(size: size).image { context in
        let circle = CGRect(origin: .zero, size: size).insetBy(dx: 1.5, dy: 1.5)
        UIColor.white.setFill()
        context.cgContext.fillEllipse(in: circle)
        UIColor(Color.knoopGreen).setStroke()
        context.cgContext.setLineWidth(2)
        context.cgContext.strokeEllipse(in: circle)
        let attributes: [NSAttributedString.Key: Any] = [
          .font: UIFont.boldSystemFont(ofSize: ref.count > 2 ? 7 : 9),
          .foregroundColor: UIColor(Color.knoopGreen),
        ]
        let text = ref as NSString
        let textSize = text.size(withAttributes: attributes)
        text.draw(
          at: CGPoint(x: (size.width - textSize.width) / 2, y: (size.height - textSize.height) / 2),
          withAttributes: attributes)
      }
    }
  }
}
