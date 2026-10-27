import KnoopByKnoopCore
import SwiftUI

/// The map with the network and the network switch, next to the panel with the waypoints, which
/// also shows the info, see `map-planning` and `network-sync`. In portrait, the panel is a sheet over
/// the map; in landscape, it is shown next to the map.
struct PlannerView: View {

  @ObservedObject var model: AppModel
  @State private var pendingNetwork: IndexedNetworkType?
  @State private var showInfo = false
  @State private var detent: PresentationDetent = .height(170)

  var body: some View {
    GeometryReader { geometry in
      let landscape = geometry.size.width > geometry.size.height
      // The map stays the first view of this stack in both orientations, so rotating keeps it,
      // with its position, instead of creating a new one.
      HStack(spacing: 0) {
        content(landscape: landscape)
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
          .presentationDetents([.height(170), .medium, .large], selection: $detent)
          .presentationBackgroundInteraction(.enabled(upThrough: .medium))
          .interactiveDismissDisabled()
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

  @ViewBuilder private func content(landscape: Bool) -> some View {
    if let session = model.session, let layers = model.layers, !model.loading {
      ZStack(alignment: .top) {
        NetworkMapView(
          graph: session.graph, layers: layers, focus: focus(session),
          onTap: model.tap(latitude:longitude:radiusMeters:),
          online: model.online
        )
        .ignoresSafeArea()
        HStack {
          Picker("Knot network", selection: networkBinding(session)) {
            Text("Cycling").tag(IndexedNetworkType.cycling)
            Text("Hiking").tag(IndexedNetworkType.hiking)
          }
          .pickerStyle(.segmented)
          .frame(width: 200)
          infoButton
        }
        .padding(8)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 12))
        .padding(.top, 8)
        if !model.status.hasGeometry {
          CoursesNote(online: model.online).padding(.top, 64)
        }
      }
      .overlay(alignment: .bottomTrailing) {
        Text("© OpenStreetMap contributors")
          .font(.caption2)
          .padding(.horizontal, 4)
          .background(.white.opacity(0.7))
          // Above the sheet in portrait.
          .padding(.bottom, landscape ? 4 : 132)
          .padding(.trailing, 4)
      }
    } else {
      // Only without bundled graphs, e.g. a debug build that was built without internet access.
      VStack(spacing: 16) {
        if model.status.dataDate == nil && !model.online {
          Text("The knot networks need an internet connection once.").multilineTextAlignment(.center)
        } else {
          ProgressView()
          Text("Loading the \(model.network.name.lowercased()) knot network…")
        }
        infoButton
      }
      .padding(32)
      .frame(maxWidth: .infinity, maxHeight: .infinity)
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

  private func networkBinding(_ session: PlanningSession) -> Binding<IndexedNetworkType> {
    Binding(
      get: { IndexedNetworkType.companion.of(network: session.network) },
      set: { network in
        if network == IndexedNetworkType.companion.of(network: session.network) { return }
        if session.waypoints.isEmpty { model.switchNetwork(network) } else { pendingNetwork = network }
      })
  }
}

/// The route summary and the waypoints: drag to move one, swipe to remove one.
private struct WaypointSheet: View {

  @ObservedObject var model: AppModel
  let session: PlanningSession
  @State private var rows: [WaypointRow] = []

  var body: some View {
    VStack(alignment: .leading, spacing: 0) {
      HStack(alignment: .top) {
        summary
        Spacer()
        VStack(alignment: .trailing, spacing: 12) {
          if !session.waypoints.isEmpty { Button("Clear") { model.clear() } }
          if session.canReturnToStart { Button("Back to start") { model.returnToStart() } }
        }
      }
      .padding()
      List {
        ForEach(Array(rows.enumerated()), id: \.element.id) { index, row in
          HStack(spacing: 12) {
            KnotBadge(ref: row.knot.ref, role: role(index)).frame(width: 40)
            Text(role(index).name.lowercased().capitalized).font(.body)
          }
          .deleteDisabled(!session.canRemoveWaypoints)
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
    }
    .onAppear { rows = WaypointRow.rows(of: session) }
    .onChange(of: WaypointRow.rows(of: session)) { _, newRows in rows = newRows }
  }

  @ViewBuilder private var summary: some View {
    switch model.routeDescription {
    case let complete as RouteDescriptionComplete:
      VStack(alignment: .leading, spacing: 6) {
        Text(complete.distance).font(.title2.bold())
        // The route's knots like on the map; the chosen waypoints stand out.
        WrappingRow {
          ForEach(Array(complete.knots.enumerated()), id: \.offset) { _, routeKnot in
            KnotBadge(ref: routeKnot.knot.ref, role: routeKnot.role)
          }
        }
      }
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
