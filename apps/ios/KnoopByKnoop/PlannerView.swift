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
  /// The height of the summary and the actions while planning, see `planningDetent`.
  @State private var summaryHeight: CGFloat = 0
  /// How far the sheet reaches up from the bottom of the screen, see `searchOverlay`.
  @State private var sheetHeight: CGFloat = 0
  /// The height of the number to follow and the progress strip while navigating, see
  /// `navigationDetent`.
  @State private var navigationHeight: CGFloat = 0
  /// The text size `navigationHeight` was measured at: another one gives the navigation part
  /// another height, also a lower one, so it is measured anew.
  @State private var navigationHeightTypeSize: DynamicTypeSize?
  /// The attribution's height above the search, see `searchOverlay`.
  @State private var attributionHeight: CGFloat = 0
  @FocusState private var searchFocused: Bool

  private static let smallestDetent = PresentationDetent.height(170)

  /// The smallest sheet while planning: at least the summary and the actions, which are below it at
  /// large text sizes, see `map-planning` "Route summary at large text sizes". The 24 points are the
  /// space above the summary.
  private var planningDetent: PresentationDetent {
    summaryHeight + 24 > 170 ? .height((summaryHeight + 24).rounded(.up)) : Self.smallestDetent
  }

  /// While navigating, the sheet's smallest height: the number to follow and the whole strip, see
  /// `knot-navigation` "Panel height while navigating". Rounded up, so a relayout does not move the
  /// sheet by fractions of a point.
  private var navigationDetent: PresentationDetent? {
    guard navigator.state != nil, navigationHeight > 0 else { return nil }
    return .height(navigationHeight.rounded(.up))
  }

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
          .presentationDetents(
            [navigationDetent ?? planningDetent, .medium, .large], selection: $detent
          )
          // The map stays usable and undimmed below the sheet up to half the screen.
          .presentationBackgroundInteraction(.enabled(upThrough: .medium))
          .interactiveDismissDisabled()
          // A sheet keeps the system's appearance unless told otherwise.
          .appAppearance()
      }
      // The keyboard covers the smallest sheet, but not a higher one, which would hide the field.
      .onChange(of: keyboardShown) { _, shown in
        if shown && !landscape { detent = planningDetent }
      }
      // Starting navigation brings the sheet to the navigation height whatever its height was, and
      // ending it brings it back to the smallest planning height.
      // A sheet the rider pulled up stays up when the navigation height changes, e.g. off route.
      .onChange(of: navigationDetent) { previous, navigation in
        if previous == nil || navigation == nil || detent == previous {
          detent = navigation ?? planningDetent
        }
      }
      // A sheet at its smallest stays there when that height changes.
      .onChange(of: planningDetent) { previous, planning in
        if detent == previous { detent = planning }
      }
    }
    // The display stays on while navigating with the app open, see `knot-navigation` "Display on
    // while navigating in the app"; iOS applies it only while the app is active.
    .onChange(of: navigator.state != nil, initial: true) { _, navigating in
      UIApplication.shared.isIdleTimerDisabled = navigating
      if !navigating { navigationHeight = 0 }
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
        WaypointSheet(
          // The highest the navigation part was while navigating, so the sheet does not jump when
          // the header switches to the number to follow near a knot, see `knot-navigation` "Number
          // to follow"; 0 again when navigation ends.
          model: model, session: session,
          onNavigationHeight: { height, typeSize in
            navigationHeight =
              navigationHeightTypeSize == typeSize ? max(navigationHeight, height) : height
            navigationHeightTypeSize = typeSize
          },
          onSummaryHeight: { summaryHeight = $0 },
          // The sheet comes down, so the knot is not hidden by it, see `knot-navigation` "Showing a
          // coming knot on the map".
          onShowKnot: { knot in
            model.showOnMap(knot)
            if let navigationDetent { detent = navigationDetent }
          })
      } else {
        ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
      }
    }
    .confirmationDialog(
      pendingNetwork == .hiking ? LocalizedStringKey("Switch to hiking?") : "Switch to cycling?", isPresented: Binding(
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
        nextKnot: navigator.state?.navigation?.nextKnot,
        wayToRoute: navigator.state.flatMap { state in
          state.position.flatMap { state.navigation?.wayToRoute(fix: $0) }
        },
        track: navigator.state?.track,
        direction: navigator.state?.direction,
        myPosition: navigator.state == nil ? model.myPosition : nil,
        locating: model.locating,
        onLocate: locateAction,
        shownKnot: model.shownKnot
      )
      .ignoresSafeArea()
      // While typing in landscape, the hits need the room up to the top, see `place-search`.
      // Not while navigating: the route is fixed then, see `knot-navigation` "Switching the network
      // while navigating", and the info sheet is not needed on the ride, see `network-sync` "Info
      // sheet".
      if !(landscape && keyboardShown) && navigator.state == nil { networkSwitch }
      if model.layers == nil {
        // Only without bundled graphs, e.g. a debug build that was built without internet access,
        // or for the moment it takes to read the graph.
        LoadingNote(
          text: model.status.dataDate == nil && !model.online
            ? LocalizedStringKey("The knot networks need an internet connection once")
            : model.network == .hiking ? "Loading the hiking knot network…" : "Loading the cycling knot network…"
        )
        .padding(.top, notesTop)
      } else if !model.status.hasGeometry {
        CoursesNote(online: model.online).padding(.top, notesTop)
      }
    }
    .overlay(alignment: .bottom) { searchOverlay(landscape: landscape, keyboardShown: keyboardShown, screen: screen) }
  }

  /// Just below the network switch.
  private let notesTop: CGFloat = 64

  /// The position on request, only while planning, see `map-planning` "Show my position".
  private var locateAction: (() -> Void)? {
    guard navigator.state == nil else { return nil }
    return { [model] in model.locate() }
  }

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

  /// A round button like the zoom buttons, see `network-sync` "Info sheet": a bare "i" could be
  /// taken for the base map's tourist information symbol.
  private var infoButton: some View {
    Button {
      showInfo = true
      detent = .medium
    } label: {
      Image(systemName: "info.circle")
        .font(.system(size: 20))
        .foregroundStyle(Color.knoopGreen)
        .frame(width: 40, height: 40)
        .background(Circle().fill(Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? UIColor(white: 0.17, alpha: 1) : .white })))
        .shadow(color: .black.opacity(0.2), radius: 2, y: 1)
    }
    .accessibilityLabel("Info")
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
  /// The exported GPX file while the share sheet shows it.
  @State private var exportedGpx: ExportedGpx?
  /// The height of the number to follow and the progress strip while navigating, 0 otherwise, and
  /// the text size it was measured at.
  let onNavigationHeight: (CGFloat, DynamicTypeSize) -> Void
  @Environment(\.dynamicTypeSize) private var dynamicTypeSize
  /// The height of the summary and the actions while planning.
  let onSummaryHeight: (CGFloat) -> Void
  /// Shows a coming knot on the map.
  let onShowKnot: (Knot) -> Void

  var body: some View {
    let complete = model.routeDescription as? RouteDescriptionComplete
    // While navigating, where the rider is on the route, see `knot-navigation` "Progress strip".
    let progress = navigator.state.flatMap { state in
      state.position.flatMap { state.navigation?.progress(fix: $0) }
    }
    VStack(alignment: .leading, spacing: 0) {
      // The header stays in place; below it, the ferries and the waypoints scroll together, see
      // `map-planning` "Long routes in the panel".
      VStack(alignment: .leading, spacing: 8) {
        if let navigation = navigator.state {
          NavigationHeader(
            state: navigation, roles: complete?.knots.map(\.role) ?? [],
            nextKnotDistance: progress?.nextKnotDistance
          ) { model.stopNavigation() }
        } else {
          // The distance and time stay in one line each, with the actions below them where they do
          // not fit next to them, see `map-planning` "Route summary at large text sizes".
          Group {
            if complete != nil {
              ViewThatFits(in: .horizontal) {
                HStack(alignment: .center) {
                  summary
                  Spacer()
                  actions
                }
                VStack(alignment: .trailing, spacing: 8) {
                  summary.frame(maxWidth: .infinity, alignment: .leading)
                  actions
                }
              }
            } else {
              HStack(alignment: .center) {
                summary
                Spacer()
                actions
              }
            }
          }
          // Measured for the sheet's smallest height while planning, see `PlannerView`.
          .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { onSummaryHeight($0) }
          // Presented from the planner sheet itself: presented from the map behind it, it would
          // dismiss the sheet, see `PlannerView`.
          .sheet(item: $exportedGpx) { exported in
            ShareSheet(url: exported.url)
              .presentationDetents([.medium, .large])
              .appAppearance()
          }
        }
        // While navigating, where the rider is on the route instead of the badges, see
        // `knot-navigation` "Progress strip".
        if let progress, let navigation = navigator.state?.navigation {
          ProgressPanel(progress: progress, ferries: complete?.ferries ?? [], navigation: navigation)
        } else if let complete {
          RouteKnots(description: complete)
        }
      }
      .padding([.horizontal, .top])
      .padding(.bottom, 8)
      .onGeometryChange(for: CGFloat.self) { $0.size.height } action: {
        onNavigationHeight(navigator.state == nil ? 0 : $0, dynamicTypeSize)
      }
      List {
        // While navigating, the ferries are on the progress strip, see `knot-navigation` "Ferries on
        // the strip".
        if let complete, !complete.ferries.isEmpty, navigator.state == nil {
          // Each ferry in its own row: a row sends taps anywhere in it to its first link, so with
          // one row for all, a ferry without a website opened the website of another.
          RouteFerries(ferries: complete.ferries, navigation: navigator.state?.navigation)
            .moveDisabled(true)
            .deleteDisabled(true)
        }
        // While navigating, the coming knots in place of the waypoints, as the route is fixed then,
        // see `knot-navigation` "Coming knots" and `map-planning` "Waypoint list with badges".
        ForEach(Array((progress?.coming ?? []).enumerated()), id: \.offset) { _, coming in
          comingRow(coming)
            .listRowInsets(EdgeInsets(top: 2, leading: 16, bottom: 2, trailing: 16))
            .listRowBackground(Color.clear)
        }
        ForEach(Array((navigator.state == nil ? rows : []).enumerated()), id: \.element.id) { index, row in
          HStack(spacing: 12) {
            KnotBadge(ref: row.knot.ref, role: role(index)).frame(width: 40)
            Text(Self.roleName(role(index))).font(.callout)
          }
          .deleteDisabled(!session.canRemoveWaypoints)
          .listRowInsets(EdgeInsets(top: 2, leading: 16, bottom: 2, trailing: 16))
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
      // One compact line per waypoint, as high as a touch target.
      .environment(\.defaultMinListRowHeight, 44)
      // Always editing while planning, so every waypoint shows the system's reorder grip and, where
      // allowed, its remove control, see `map-planning` "Edit the waypoint list".
      .environment(\.editMode, .constant(navigator.state == nil ? .active : .inactive))
      // So the sheet's own background shows, Liquid Glass on iOS 26 and later.
      .scrollContentBackground(.hidden)
    }
    .onAppear { rows = WaypointRow.rows(of: session) }
    .onChange(of: WaypointRow.rows(of: session)) { _, newRows in rows = newRows }
  }

  /// A coming knot or ferry with its distance and arrival; tapping a knot shows it on the map.
  @ViewBuilder private func comingRow(_ coming: ComingKnot) -> some View {
    if let knot = coming.knot {
      Button { onShowKnot(knot) } label: { comingContent(coming) }
        .buttonStyle(.plain)
    } else {
      comingContent(coming)
    }
  }

  private func comingContent(_ coming: ComingKnot) -> some View {
    let description: String = coming.description_
    return HStack(spacing: 12) {
      comingBadge(coming).frame(width: 40)
      Text(coming.distance).bold()
      Spacer()
      comingArrival(coming)
    }
    .contentShape(Rectangle())
    .accessibilityElement(children: .ignore)
    .accessibilityLabel(Text(description))
  }

  @ViewBuilder private func comingArrival(_ coming: ComingKnot) -> some View {
    if let arrival = coming.arrival {
      let color: Color = ProgressPanel.arrivalColor(coming.state)
      Text(arrival).fontWeight(coming.state == nil ? .regular : .medium).foregroundStyle(color)
    }
  }

  @ViewBuilder private func comingBadge(_ coming: ComingKnot) -> some View {
    if coming.knot == nil {
      FerryBadge(warning: coming.state == FerryArrivalState.warning)
    } else {
      KnotBadge(ref: coming.label, role: Self.role(of: coming.kind))
    }
  }

  /// The waypoint's role in the list, e.g. `Start`; keys of their own, as Stop also ends navigation.
  private static func roleName(_ role: KnotRole) -> String {
    switch role {
    case .start: String(localized: "waypoint.start", defaultValue: "Start", bundle: AppLanguage.bundle)
    case .end: String(localized: "waypoint.end", defaultValue: "End", bundle: AppLanguage.bundle)
    default: String(localized: "waypoint.stop", defaultValue: "Stop", bundle: AppLanguage.bundle)
    }
  }

  private static func role(of kind: ProgressMarkKind) -> KnotRole {
    switch kind {
    case .start: .start
    case .stop: .stop
    case .end: .end
    default: .passed
    }
  }

  /// Side by side with the summary where they fit, so the header stays one row and the badges fit
  /// into the smallest sheet; the less frequent actions in a menu, see `map-planning` "Clear actions
  /// in the planner panel".
  @ViewBuilder private var actions: some View {
    HStack(alignment: .center) {
      if session.canReturnToStart || model.canExportGpx {
        Menu {
          if session.canReturnToStart {
            Button("Back to start", systemImage: "arrow.uturn.backward") {
              model.returnToStart()
            }
          }
          if model.canExportGpx {
            Button("Export GPX", systemImage: "square.and.arrow.up") {
              exportedGpx = model.exportGpx()
            }
          }
        } label: {
          Label("More actions", systemImage: "ellipsis")
        }
        .labelStyle(.iconOnly)
        .buttonStyle(.bordered)
        .buttonBorderShape(.circle)
        .tint(.actionIconGreen)
      }
      if !session.waypoints.isEmpty {
        IconAction(title: "Clear", systemImage: "trash") { model.clear() }
      }
      if model.canNavigate {
        ProminentAction(title: "Navigate", systemImage: "location.north.fill") {
          model.startNavigation()
        }
      }
    }
    // Never squeezed: the summary and the spacer give way, or the actions move below them.
    .fixedSize(horizontal: true, vertical: false)
  }

  @ViewBuilder private var summary: some View {
    switch model.routeDescription {
    case let complete as RouteDescriptionComplete:
      VStack(alignment: .leading, spacing: 0) {
        Text(complete.distance).font(.title2.bold()).lineLimit(1)
        // The time at the assumed pace, see `map-planning` "Show the route".
        Text(complete.duration).font(.caption).foregroundStyle(.secondary).lineLimit(1)
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

/// The number to follow at the next knot as the most prominent information, with the knots as
/// badges and the distance to the next knot, and Stop, see `knot-navigation` "Number to follow".
private struct NavigationHeader: View {

  let state: NavigationState
  /// How each knot of the route takes part in it, for the badges.
  let roles: [KnotRole]
  let nextKnotDistance: String?
  let onStop: () -> Void
  @Environment(\.dynamicTypeSize) private var dynamicTypeSize

  var body: some View {
    if let navigation = state.navigation, dynamicTypeSize.isAccessibilitySize,
      navigation.display.offRoute || (navigation.display.follow != nil && !navigation.display.close)
    {
      // At accessibility sizes, Stop next to the off-route header or the next knot leaves them no
      // room: Stop goes above them, see `knot-navigation` "Number to follow at large text sizes".
      VStack(alignment: .leading, spacing: 8) {
        HStack {
          Spacer()
          stop
        }
        let display = navigation.display
        let next = Int(navigation.nextIndex)
        if let follow = display.follow, !display.offRoute {
          nextKnotHeader(display.next, at: next, follow: follow)
        } else {
          offRouteHeader(
            display.next, at: next, guidance: state.position.flatMap { navigation.guidance(fix: $0) })
        }
      }
    } else {
      header
    }
  }

  private var stop: some View {
    ProminentAction(title: "Stop", systemImage: "stop.fill", color: .actionRed, action: onStop)
  }

  private var header: some View {
    HStack(alignment: .center) {
      if let navigation = state.navigation {
        let display = navigation.display
        let next = Int(navigation.nextIndex)
        if display.offRoute {
          offRouteHeader(display.next, at: next, guidance: state.position.flatMap { navigation.guidance(fix: $0) })
        } else if let follow = display.follow, !display.close {
          nextKnotHeader(display.next, at: next, follow: follow)
        } else {
          // The distance moves below the number to follow where it does not fit next to it, see
          // `knot-navigation` "Number to follow at large text sizes".
          ViewThatFits(in: .horizontal) {
            HStack(alignment: .center, spacing: 8) {
              numberToFollow(display, at: next)
              distance
            }
            VStack(alignment: .leading, spacing: 2) {
              HStack(alignment: .center, spacing: 8) { numberToFollow(display, at: next) }
              distance
            }
          }
          .minimumScaleFactor(0.8)
        }
      } else {
        Text("Waiting for your position…").font(.headline)
      }
      Spacer()
      stop
    }
  }

  @ViewBuilder private func numberToFollow(_ display: NavigationDisplay, at next: Int) -> some View {
    if let follow = display.follow {
      Text("At").font(.title2)
      badge(display.next, at: next, scale: 1.3)
      Text("follow").font(.title2)
      badge(follow, at: next + 1, scale: 1.8)
    } else {
      badge(display.next, at: next, scale: 1.3)
      Text("is the end").font(.title2)
    }
  }

  /// Far from the next knot: the next knot and the distance to it as the most prominent
  /// information, and the number to follow there smaller below, see `knot-navigation` "Number to
  /// follow".
  private func nextKnotHeader(_ next: String, at index: Int, follow: String) -> some View {
    VStack(alignment: .leading, spacing: 2) {
      // The distance moves below the next knot where it does not fit next to it, see
      // `knot-navigation` "Number to follow at large text sizes".
      ViewThatFits(in: .horizontal) {
        HStack(alignment: .center, spacing: 8) {
          Text("Next").font(.title2)
          badge(next, at: index, scale: 1.8)
          nextDistance
        }
        VStack(alignment: .leading, spacing: 2) {
          HStack(alignment: .center, spacing: 8) {
            Text("Next").font(.title2)
            badge(next, at: index, scale: 1.8)
          }
          nextDistance
        }
      }
      HStack(alignment: .center, spacing: 6) {
        Text("then").font(.body).foregroundStyle(.secondary)
        badge(follow, at: index + 1, scale: 1.0)
      }
    }
    .minimumScaleFactor(0.8)
    .accessibilityElement(children: .ignore)
    .accessibilityLabel(
      [
        String(localized: "Next \(next)", bundle: AppLanguage.bundle), nextKnotDistance,
        String(localized: "then \(follow)", bundle: AppLanguage.bundle),
      ].compactMap { $0 }.joined(separator: ", "))
  }

  /// The distance to the next knot, as prominent as the knot while far from it.
  @ViewBuilder private var nextDistance: some View {
    if let nextKnotDistance {
      Text(nextKnotDistance).font(.title2).lineLimit(1)
    }
  }

  @ViewBuilder private var distance: some View {
    if let nextKnotDistance {
      Text(nextKnotDistance).font(.title3).foregroundStyle(.secondary).lineLimit(1)
    }
  }

  /// Off route: the next knot, and below it the distance, an arrow towards the knot relative to
  /// the riding direction (to north while standing, as the map) and the compass direction, see
  /// `knot-navigation` "Direction to the route".
  private func offRouteHeader(_ next: String, at index: Int, guidance: Guidance?) -> some View {
    VStack(alignment: .leading, spacing: 2) {
      // The badge goes below the text where both do not fit in one line, as the German text, see
      // `knot-navigation` "Number to follow at large text sizes".
      ViewThatFits(in: .horizontal) {
        HStack(alignment: .center, spacing: 8) {
          offRouteNext
          badge(next, at: index, scale: 1.3)
        }
        VStack(alignment: .leading, spacing: 2) {
          offRouteNext.fixedSize(horizontal: false, vertical: true)
          badge(next, at: index, scale: 1.3)
        }
      }
      if let guidance {
        // The direction goes below the distance where both do not fit in one line.
        ViewThatFits(in: .horizontal) {
          HStack(spacing: 6) {
            wayToNext(guidance)
            Text(guidance.direction).font(.title3).foregroundStyle(.secondary)
          }
          VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: 6) { wayToNext(guidance) }
            Text(guidance.direction).font(.title3).foregroundStyle(.secondary)
          }
        }
      }
    }
    .minimumScaleFactor(0.8)
    .accessibilityElement(children: .ignore)
    .accessibilityLabel([String(localized: "Off route, next \(next)", bundle: AppLanguage.bundle), guidance?.text].compactMap { $0 }.joined(separator: ", "))
  }

  /// The distance to the next knot and an arrow towards it.
  @ViewBuilder private func wayToNext(_ guidance: Guidance) -> some View {
    Text(guidance.distance).font(.title3)
    Image(systemName: "location.north.fill")
      .foregroundStyle(.red)
      .rotationEffect(.degrees(guidance.bearing - (state.direction ?? 0)))
  }

  private var offRouteNext: some View {
    Text("Off route, next").font(.title2.bold()).foregroundStyle(.red)
  }

  private func badge(_ ref: String, at index: Int, scale: CGFloat) -> some View {
    Badge(ref: ref, role: roles.indices.contains(index) ? BadgeRole(roles[index]) : nil, scale: scale)
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
