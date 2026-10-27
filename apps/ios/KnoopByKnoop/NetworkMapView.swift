import KnoopByKnoopCore
import MapLibre
import SwiftUI

/// OpenStreetMap-based base map without API key, see the design of `add-planning-screens`.
private let baseMapStyle = URL(string: "https://tiles.openfreemap.org/styles/liberty")!

/// Used when the base map cannot be loaded, e.g. offline on first start: the network layers still
/// show. Offline, MapLibre takes long to fail, so it is used after a timeout.
private let offlineStyle = """
  {"version": 8, "sources": {}, "layers": [{"id": "background", "type": "background", \
  "paint": {"background-color": "#EEF1EA"}}]}
  """

/// How long the base map may take to load before the offline style is used.
private let baseMapTimeout: TimeInterval = 3

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
  /// Whether the device has internet access; the base map is loaded again once it is back.
  let online: Bool

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
    addZoomButtons(to: mapView, coordinator: context.coordinator)
    DispatchQueue.main.asyncAfter(deadline: .now() + baseMapTimeout) { [weak mapView] in
      if let mapView, context.coordinator.style == nil { context.coordinator.useOfflineStyle(on: mapView) }
    }
    return mapView
  }

  /// Buttons to zoom in and out, in addition to the gestures.
  private func addZoomButtons(to mapView: MLNMapView, coordinator: Coordinator) {
    let buttons = [("plus", #selector(Coordinator.zoomIn)), ("minus", #selector(Coordinator.zoomOut))].map {
      symbol, action in
      let button = UIButton(type: .system)
      button.setImage(UIImage(systemName: symbol), for: .normal)
      button.tintColor = UIColor(Color.knoopGreen)
      button.backgroundColor = .white
      button.layer.cornerRadius = 20
      button.layer.shadowOpacity = 0.2
      button.layer.shadowRadius = 2
      button.layer.shadowOffset = CGSize(width: 0, height: 1)
      button.widthAnchor.constraint(equalToConstant: 40).isActive = true
      button.heightAnchor.constraint(equalToConstant: 40).isActive = true
      button.addTarget(coordinator, action: action, for: .touchUpInside)
      return button
    }
    let stack = UIStackView(arrangedSubviews: buttons)
    stack.axis = .vertical
    stack.spacing = 8
    stack.translatesAutoresizingMaskIntoConstraints = false
    mapView.addSubview(stack)
    NSLayoutConstraint.activate([
      stack.trailingAnchor.constraint(equalTo: mapView.safeAreaLayoutGuide.trailingAnchor, constant: -12),
      stack.centerYAnchor.constraint(equalTo: mapView.centerYAnchor),
    ])
  }

  func updateUIView(_ mapView: MLNMapView, context: Context) {
    context.coordinator.onTap = onTap
    if online && context.coordinator.usesOfflineStyle {
      context.coordinator.usesOfflineStyle = false
      mapView.styleURL = baseMapStyle
    }
    context.coordinator.show(graph: graph, layers: layers, focus: focus, on: mapView)
  }

  /// Keeps the style and what is shown, so updates only touch what changed.
  final class Coordinator: NSObject, MLNMapViewDelegate {

    var onTap: (Double, Double, Double) -> Void = { _, _, _ in }
    private weak var mapView: MLNMapView?
    private(set) var style: MLNStyle?
    private var graph: NetworkGraph?
    private var layers: MapLayers?
    private var focus: [Knot] = []
    private var shownGraph: NetworkGraph?
    private var shownNetwork: URL?
    private var shownRoute: String?

    func show(graph: NetworkGraph, layers: MapLayers, focus: [Knot], on mapView: MLNMapView) {
      self.mapView = mapView
      self.graph = graph
      self.layers = layers
      self.focus = focus
      update()
    }

    @objc func zoomIn() { zoom(by: 1) }

    @objc func zoomOut() { zoom(by: -1) }

    private func zoom(by delta: Double) {
      guard let mapView else { return }
      mapView.setZoomLevel(mapView.zoomLevel + delta, animated: true)
    }

    @objc func tapped(_ recognizer: UITapGestureRecognizer) {
      guard let mapView else { return }
      let point = recognizer.location(in: mapView)
      if mapView.zoomLevel < knotMinimumZoom {
        // Zoomed out, knots are not shown: a tap on the route zooms to it.
        let area = CGRect(x: point.x - touchTargetPoints, y: point.y - touchTargetPoints, width: 2 * touchTargetPoints, height: 2 * touchTargetPoints)
        if !mapView.visibleFeatures(in: area, styleLayerIdentifiers: ["route"]).isEmpty { fit(to: focus, animated: true) }
        return
      }
      let coordinate = mapView.convert(point, toCoordinateFrom: mapView)
      let metersPerPoint = mapView.metersPerPoint(atLatitude: coordinate.latitude)
      onTap(coordinate.latitude, coordinate.longitude, metersPerPoint * touchTargetPoints)
    }

    func mapView(_ mapView: MLNMapView, didFinishLoading style: MLNStyle) {
      self.style = style
      shownNetwork = nil
      shownRoute = nil
      addLayers(to: style)
      update()
    }

    func mapViewDidFailLoadingMap(_ mapView: MLNMapView, withError error: Error) {
      useOfflineStyle(on: mapView)
    }

    /// Whether the offline style is shown, so the base map is loaded once internet access is back.
    var usesOfflineStyle = false

    func useOfflineStyle(on mapView: MLNMapView) {
      guard !usesOfflineStyle else { return }
      usesOfflineStyle = true
      let file = FileManager.default.temporaryDirectory.appendingPathComponent("offline-style.json")
      try? Data(offlineStyle.utf8).write(to: file)
      mapView.styleURL = file
    }

    private func update() {
      guard let style, let layers else { return }
      if shownNetwork != layers.connections {
        for ref in layers.refs where style.image(forName: KnotBadges.name(nil, ref)) == nil {
          style.setImage(KnotBadges.image(nil, ref), forName: KnotBadges.name(nil, ref))
        }
        (style.source(withIdentifier: "knots") as? MLNShapeSource)?.url = layers.knots
        (style.source(withIdentifier: "connections") as? MLNShapeSource)?.url = layers.connections
        shownNetwork = layers.connections
      }
      if shownRoute != layers.route + layers.routeKnots {
        // Route knots change without the route, e.g. when only the start is set.
        for routeKnot in layers.routeKnotList {
          let name = KnotBadges.name(routeKnot.role, routeKnot.knot.ref)
          if style.image(forName: name) == nil { style.setImage(KnotBadges.image(routeKnot.role, routeKnot.knot.ref), forName: name) }
        }
        (style.source(withIdentifier: "route") as? MLNShapeSource)?.shape = shape(layers.route)
        (style.source(withIdentifier: "route-knots") as? MLNShapeSource)?.shape = shape(layers.routeKnots)
        shownRoute = layers.route + layers.routeKnots
      }
      if shownGraph !== graph, let graph {
        // A new network shows the restored route if there is one, the whole network otherwise.
        fit(to: focus.count >= 2 ? focus : Array(graph.knots.values), animated: false)
        shownGraph = graph
      }
    }

    private func fit(to knots: [Knot], animated: Bool) {
      guard knots.count >= 2, let mapView else { return }
      let coordinates = knots.map { CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude) }
      let bounds = MLNCoordinateBounds(
        sw: CLLocationCoordinate2D(
          latitude: coordinates.map(\.latitude).min()!, longitude: coordinates.map(\.longitude).min()!),
        ne: CLLocationCoordinate2D(
          latitude: coordinates.map(\.latitude).max()!, longitude: coordinates.map(\.longitude).max()!))
      mapView.setVisibleCoordinateBounds(
        bounds, edgePadding: UIEdgeInsets(top: 140, left: 40, bottom: 220, right: 60), animated: animated,
        completionHandler: nil)
    }

    private func shape(_ geoJson: String) -> MLNShape? {
      try? MLNShape(data: Data(geoJson.utf8), encoding: String.Encoding.utf8.rawValue)
    }

    private func addLayers(to style: MLNStyle) {
      // Large network layers come from files, so the map parses them off the main thread.
      let sources = ["connections", "route", "route-knots", "knots"].map { MLNShapeSource(identifier: $0, shape: nil) }
      // Not "arrow": the base map's sprite has an image of that name for one-way streets.
      style.setImage(KnotBadges.arrow(), forName: "route-arrow")
      sources.forEach(style.addSource)

      let connections = MLNLineStyleLayer(identifier: "connections", source: sources[0])
      connections.lineColor = NSExpression(forConstantValue: UIColor(Color.knoopGreen))
      connections.lineWidth = NSExpression(forConstantValue: 2)
      connections.lineOpacity = NSExpression(forConstantValue: 0.7)
      style.addLayer(connections)

      let route = MLNLineStyleLayer(identifier: "route", source: sources[1])
      route.predicate = NSPredicate(format: "reachable == YES")
      route.lineColor = NSExpression(forConstantValue: UIColor(Color.knoopOrange))
      route.lineWidth = NSExpression(forConstantValue: 8)
      route.lineCap = NSExpression(forConstantValue: "round")
      route.lineJoin = NSExpression(forConstantValue: "round")
      style.addLayer(route)

      let unreachable = MLNLineStyleLayer(identifier: "route-unreachable", source: sources[1])
      unreachable.predicate = NSPredicate(format: "reachable == NO")
      unreachable.lineColor = NSExpression(forConstantValue: UIColor.systemRed)
      unreachable.lineWidth = NSExpression(forConstantValue: 4)
      unreachable.lineDashPattern = NSExpression(forConstantValue: [2, 2])
      style.addLayer(unreachable)

      // Arrows along the route show the riding direction: each leg runs from its start to its end.
      let direction = MLNSymbolStyleLayer(identifier: "route-direction", source: sources[1])
      direction.predicate = NSPredicate(format: "reachable == YES")
      direction.symbolPlacement = NSExpression(forConstantValue: "line")
      direction.symbolSpacing = NSExpression(forConstantValue: 48)
      direction.iconImageName = NSExpression(forConstantValue: "route-arrow")
      direction.iconAllowsOverlap = NSExpression(forConstantValue: true)
      direction.iconIgnoresPlacement = NSExpression(forConstantValue: true)
      style.addLayer(direction)

      let knots = MLNSymbolStyleLayer(identifier: "knots", source: sources[3])
      knots.iconImageName = NSExpression(format: "mgl_join({'knot-', ref})")
      knots.iconAllowsOverlap = NSExpression(forConstantValue: true)
      knots.iconIgnoresPlacement = NSExpression(forConstantValue: true)
      knots.minimumZoomLevel = Float(knotMinimumZoom)
      style.addLayer(knots)

      // Route knots are drawn above the network's knots, with a badge per role.
      let routeKnots = MLNSymbolStyleLayer(identifier: "route-knots", source: sources[2])
      routeKnots.iconImageName = NSExpression(format: "mgl_join({'route-', role, '-', ref})")
      routeKnots.iconAllowsOverlap = NSExpression(forConstantValue: true)
      routeKnots.iconIgnoresPlacement = NSExpression(forConstantValue: true)
      routeKnots.minimumZoomLevel = Float(knotMinimumZoom)
      style.addLayer(routeKnots)
    }

  }
}
