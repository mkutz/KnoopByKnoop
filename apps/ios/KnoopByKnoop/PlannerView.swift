import KnoopByKnoopCore
import SwiftUI

/// The map with the network, the network switch and the place search, next to the panel with the
/// waypoints, which also shows the info, see `map-planning`, `network-sync` and `place-search`. In
/// portrait, the panel is a sheet over the map, with the search right above it; in landscape, it is
/// shown next to the map, with the search at the bottom of the map.
struct PlannerView: View {

  @ObservedObject var model: AppModel
  @ObservedObject private var navigator = Navigator.shared
  @State private var pendingNetwork: IndexedNetworkType?
  @State private var showInfo = false
  @State private var detent: PresentationDetent = smallestDetent
  /// How far the sheet reaches up from the bottom of the screen, see `searchOverlay`.
  @State private var sheetHeight: CGFloat = 0
  /// The attribution's height above the search, see `searchOverlay`.
  @State private var attributionHeight: CGFloat = 0
  @FocusState private var searchFocused: Bool

  private static let smallestDetent = PresentationDetent.height(170)

  /// Whether an on-screen keyboard covers the bottom of the screen. Its safe area counts as the
  /// bottom inset, which is at most the home indicator's 34 points without one. A hardware keyboard
  /// brings up no on-screen keyboard, or only a small bar, so the search stays above the sheet.
  private static func keyboardShown(_ screen: GeometryProxy) -> Bool {
    screen.safeAreaInsets.bottom > 100
  }

  var body: some View {
    GeometryReader { geometry in
      // The keyboard's safe area counts as height, so the keyboard does not turn portrait into
      // landscape on small phones.
      let landscape = geometry.size.width > geometry.size.height + geometry.safeAreaInsets.bottom
      let keyboardShown = Self.keyboardShown(geometry)
      // The map stays the first view of this stack in both orientations, so rotating keeps it,
      // with its position, instead of creating a new one.
      HStack(spacing: 0) {
        content(landscape: landscape, keyboardShown: keyboardShown, screen: geometry)
        if landscape {
          Divider()
          panel
            .frame(width: min(380, geometry.size.width * 0.4))
            .background(Color(.systemBackground))
        }
      }
      // Presented from here, outside the switch between map and loading view: a sheet attached to
      // a view that is replaced, e.g. while the other knot network loads, is dismissed for good.
      .sheet(isPresented: .constant(!landscape)) {
        panel
          // The keyboard covers the sheet while typing in the search above it; without this, the
          // sheet's content moved up and reported the keyboard as part of the sheet's height.
          .ignoresSafeArea(.keyboard)
          // The sheet's content ends above the home indicator; the sheet itself does not.
          .onGeometryChange(for: CGFloat.self) { $0.size.height + $0.safeAreaInsets.bottom } action: {
            sheetHeight = $0
          }
          .presentationDetents([Self.smallestDetent, .medium, .large], selection: $detent)
          .presentationBackgroundInteraction(.enabled(upThrough: .medium))
          .interactiveDismissDisabled()
      }
      // The keyboard covers the smallest sheet, but not a higher one, which would hide the field.
      .onChange(of: keyboardShown) { _, shown in
        if shown && !landscape { detent = Self.smallestDetent }
      }
    }
  }

  /// The waypoints, or the info. The info and the confirmation are shown in the panel itself: while
  /// the panel is a sheet, anything presented from the view behind it is not reliably shown and
  /// dismisses the sheet.
  private var panel: some View {
    Group {
      if showInfo {
        InfoSheet(model: model, onDone: { showInfo = false })
      } else if let session = model.session, model.layers != nil, !model.loading {
        WaypointSheet(model: model, session: session)
      } else {
        ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
      }
    }
    .confirmationDialog(
      "Switch to \(pendingNetwork?.name.lowercased() ?? "")?", isPresented: Binding(
        get: { pendingNetwork != nil }, set: { if !$0 { pendingNetwork = nil } }), titleVisibility: .visible
    ) {
      Button("Switch", role: .destructive) {
        if let network = pendingNetwork { model.switchNetwork(network) }
        pendingNetwork = nil
      }
    } message: {
      Text("The current route will be discarded.")
    }
  }

  /// The map is shown right away; the network follows when it is loaded, see `map-planning`.
  private func content(landscape: Bool, keyboardShown: Bool, screen: GeometryProxy) -> some View {
    ZStack(alignment: .top) {
      NetworkMapView(
        graph: model.session?.graph, layers: model.layers, focus: model.session.map(focus) ?? [],
        onTap: model.tap(latitude:longitude:radiusMeters:),
        online: model.online,
        initialPlace: model.initialPlace,
        onLeave: model.saveCamera,
        marker: model.marker,
        onMove: model.mapMoved,
        position: navigator.state?.position,
        nextKnot: navigator.state?.navigation?.nextKnot
      )
      .ignoresSafeArea()
      // While typing in landscape, the hits need the room up to the top, see `place-search`.
      if !(landscape && keyboardShown) { networkSwitch }
      if model.layers == nil {
        // Only without bundled graphs, e.g. a debug build that was built without internet access,
        // or for the moment it takes to read the graph.
        LoadingNote(
          text: model.status.dataDate == nil && !model.online
            ? "The knot networks need an internet connection once"
            : "Loading the \(model.network.name.lowercased()) knot network…"
        )
        .padding(.top, notesTop)
      } else if !model.status.hasGeometry {
        CoursesNote(online: model.online).padding(.top, notesTop)
      }
    }
    .overlay(alignment: .bottom) { searchOverlay(landscape: landscape, keyboardShown: keyboardShown, screen: screen) }
    // The map is light in dark mode too, so the views floating over it stay light to be readable.
    .environment(\.colorScheme, .light)
  }

  /// Just below the network switch.
  private let notesTop: CGFloat = 64

  /// The network picker and the info button, at the top of the map.
  private var networkSwitch: some View {
    HStack {
      Picker("Knot network", selection: networkBinding) {
        Text("Cycling").tag(IndexedNetworkType.cycling)
        Text("Hiking").tag(IndexedNetworkType.hiking)
      }
      .pickerStyle(.segmented)
      .frame(width: 200)
      infoButton
    }
    .padding(8)
    .floatingBackground(in: RoundedRectangle(cornerRadius: 12))
    .padding(.horizontal, 12)
    .padding(.top, 8)
  }

  /// The place search and the attribution, at the bottom of the map: right above the sheet in
  /// portrait, following it when it is dragged, and right above the on-screen keyboard while typing.
  /// Hidden while the sheet is pulled up over the map. While typing in landscape, the attribution and
  /// the network switch give way, so the hits can reach the top of the map.
  @ViewBuilder private func searchOverlay(landscape: Bool, keyboardShown: Bool, screen: GeometryProxy) -> some View {
    let fullHeight = screen.size.height + screen.safeAreaInsets.top + screen.safeAreaInsets.bottom
    let sheetUp = !landscape && !keyboardShown && sheetHeight > fullHeight * 0.6
    let typingInLandscape = landscape && keyboardShown
    // This view ends at the bottom safe area, the sheet's height is measured from the screen's
    // bottom. While typing, the keyboard's safe area already lifts this view above it; with a
    // hardware keyboard, there is none, and the field stays above the sheet.
    let bottom = landscape || keyboardShown ? 8 : max(sheetHeight - screen.safeAreaInsets.bottom, 0) + 8
    // Below the network switch, or below the top safe area while it is hidden.
    let top = typingInLandscape ? 8 : notesTop
    if !sheetUp {
      GeometryReader { area in
        VStack(alignment: .trailing, spacing: 4) {
          Spacer(minLength: 0)
          if !typingInLandscape {
            Text("© OpenStreetMap contributors")
              .font(.caption2)
              .padding(.horizontal, 6)
              .padding(.vertical, 2)
              .floatingBackground(in: Capsule())
              .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { attributionHeight = $0 }
          }
          PlaceSearchView(
            model: model, focused: $searchFocused,
            // The hits get what the field leaves; never more than fits, so the field is not
            // pushed under the keyboard.
            maxHeight: area.size.height - top - bottom - (typingInLandscape ? 0 : attributionHeight + 4)
          )
          .frame(maxWidth: .infinity)
        }
        .padding(.top, top)
        .padding(.horizontal, 12)
        .padding(.bottom, bottom)
      }
    }
  }

  private var infoButton: some View {
    Button("Info") {
      showInfo = true
      detent = .medium
    }
  }

  /// The knots the map shows when it opens: the whole route if there is one.
  private func focus(_ session: PlanningSession) -> [Knot] {
    (session.route?.summary as? RouteSummaryComplete)?.knots ?? session.waypoints
  }

  private var networkBinding: Binding<IndexedNetworkType> {
    Binding(
      get: { model.network },
      set: { network in
        if network == model.network { return }
        if model.session?.waypoints.isEmpty ?? true { model.switchNetwork(network) } else { pendingNetwork = network }
      })
  }
}

/// The route summary and the waypoints: drag to move one, swipe to remove one.
private struct WaypointSheet: View {

  @ObservedObject var model: AppModel
  let session: PlanningSession
  @ObservedObject private var navigator = Navigator.shared
  @State private var rows: [WaypointRow] = []

  var body: some View {
    let complete = model.routeDescription as? RouteDescriptionComplete
    VStack(alignment: .leading, spacing: 0) {
      // The header stays in place; below it, the ferries and the waypoints scroll together, see
      // `map-planning` "Long routes in the panel".
      VStack(alignment: .leading, spacing: 8) {
        if let navigation = navigator.state {
          NavigationHeader(state: navigation) { model.stopNavigation() }
        } else {
          HStack(alignment: .firstTextBaseline) {
            summary
            Spacer()
            // Side by side, so the header stays one row and the badges fit into the smallest sheet.
            if session.canReturnToStart { Button("Back to start") { model.returnToStart() } }
            if !session.waypoints.isEmpty { Button("Clear") { model.clear() } }
            if model.canNavigate { Button("Navigate") { model.startNavigation() } }
          }
        }
        if let complete { RouteKnots(description: complete) }
      }
      .padding([.horizontal, .top])
      .padding(.bottom, 8)
      List {
        if let complete, !complete.ferries.isEmpty {
          RouteFerries(ferries: complete.ferries)
            .listRowSeparator(.hidden)
            .listRowBackground(Color.clear)
            .moveDisabled(true)
            .deleteDisabled(true)
        }
        ForEach(Array(rows.enumerated()), id: \.element.id) { index, row in
          HStack(spacing: 12) {
            KnotBadge(ref: row.knot.ref, role: role(index)).frame(width: 40)
            Text(role(index).name.lowercased().capitalized).font(.body)
          }
          .deleteDisabled(!session.canRemoveWaypoints || navigator.state != nil)
          .moveDisabled(navigator.state != nil)
          .listRowBackground(Color.clear)
        }
        .onMove { source, destination in
          guard let from = source.first else { return }
          // Show the new order at once; the route follows as soon as it is computed.
          rows.move(fromOffsets: source, toOffset: destination)
          // List counts the destination before removing the moved row.
          model.moveWaypoint(from: from, to: destination > from ? destination - 1 : destination)
        }
        .onDelete { positions in
          guard let position = positions.first else { return }
          rows.remove(atOffsets: positions)
          model.removeWaypoint(at: position)
        }
      }
      .listStyle(.plain)
      // So the sheet's own background shows, Liquid Glass on iOS 26 and later.
      .scrollContentBackground(.hidden)
    }
    .onAppear { rows = WaypointRow.rows(of: session) }
    .onChange(of: WaypointRow.rows(of: session)) { _, newRows in rows = newRows }
  }

  @ViewBuilder private var summary: some View {
    switch model.routeDescription {
    case let complete as RouteDescriptionComplete:
      Text(complete.distance).font(.title2.bold())
    case let startOnly as RouteDescriptionStartOnly:
      Text("Start at \(startOnly.start), tap a knot to set the end").font(.headline)
    case let unreachable as RouteDescriptionUnreachable:
      Text("No route for \(unreachable.legs.joined(separator: ", "))").font(.headline).foregroundStyle(.red)
    default:
      Text("Tap a knot to set the start").font(.headline)
    }
  }

  private func role(_ index: Int) -> KnotRole {
    switch index {
    case 0: .start
    case rows.count - 1: .end
    default: .stop
    }
  }
}

/// The route's knots like on the map, with the chosen waypoints standing out, in one line that
/// scrolls sideways.
private struct RouteKnots: View {

  let description: RouteDescriptionComplete

  var body: some View {
    let ferryPositions = Set(description.ferries.map { Int($0.fromPosition) })
    ScrollView(.horizontal, showsIndicators: false) {
      HStack(spacing: 4) {
        ForEach(Array(description.knots.enumerated()), id: \.offset) { position, routeKnot in
          KnotBadge(ref: routeKnot.knot.ref, role: routeKnot.role)
          if ferryPositions.contains(position) { FerryBadge() }
        }
      }
      // Room for the larger badges of the waypoints and their rings.
      .padding(.vertical, 2)
    }
  }
}

/// The number to follow at the next knot as the most prominent information, and Stop, see
/// `knot-navigation` "Number to follow".
private struct NavigationHeader: View {

  let state: NavigationState
  let onStop: () -> Void

  var body: some View {
    HStack(alignment: .center) {
      if let display = state.navigation?.display {
        if display.offRoute {
          Text(display.offRouteText).font(.title2.bold()).foregroundStyle(.red)
        } else if let follow = display.follow {
          HStack(alignment: .firstTextBaseline, spacing: 10) {
            Text("At \(display.next) follow").font(.title2)
            Text(follow).font(.system(size: 44, weight: .bold)).foregroundStyle(Color.knoopGreen)
          }
        } else {
          Text(display.text).font(.title2.bold())
        }
      } else {
        Text("Waiting for your position…").font(.headline)
      }
      Spacer()
      Button("Stop", action: onStop)
    }
  }
}

/// A waypoint with an identity that survives moving it, so the list animates moves correctly.
private struct WaypointRow: Equatable {
  let id: String
  let knot: Knot

  static func == (left: WaypointRow, right: WaypointRow) -> Bool { left.id == right.id }

  /// Waypoints are distinct knots, except the end of a round trip, which repeats the start.
  static func rows(of session: PlanningSession) -> [WaypointRow] {
    var seen = Set<Int64>()
    return session.waypoints.map { knot in
      let repeated = !seen.insert(knot.id).inserted
      return WaypointRow(id: repeated ? "\(knot.id)-again" : "\(knot.id)", knot: knot)
    }
  }
}
