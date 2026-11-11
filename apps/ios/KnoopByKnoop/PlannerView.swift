import KnoopByKnoopCore
import SwiftUI
import os

private let searchLog = Logger(subsystem: "com.knoopbyknoop", category: "Search")

/// The map with the network, the network switch and the place search, next to the panel with the
/// waypoints, which also shows the info, see `map-planning`, `network-sync` and `place-search`. In
/// portrait, the panel is a sheet over the map, with the search right above it; in landscape, it is
/// shown next to the map, with the search at the bottom of the map.
struct PlannerView: View {

  @ObservedObject var model: AppModel
  @ObservedObject private var navigator = Navigator.shared
  @State private var pendingNetwork: IndexedNetworkType?
  @State private var showInfo = false
  /// Whether places useful on a ride are highlighted, see `map-planning` "Switch for places on the map".
  @AppStorage("places") private var places = true
  @State private var detent: PresentationDetent = smallestDetent
  /// The height of the summary and the actions, the knots and the switch while planning, with their
  /// padding, see `planningDetent`.
  @State private var summaryHeight: CGFloat = 0
  /// How far the sheet reaches up from the bottom of the screen, on every frame of its move. Not
  /// observed here, only by the attribution: as this view's state, every frame redrew the planner,
  /// the map and the waypoints, which made the sheet stutter on the iPhone.
  @State private var sheet = SheetHeight()
  /// The sheet's height once it has come to rest, for the map's buttons.
  @State private var restingSheetHeight: CGFloat = 0
  /// The height of the number to follow and the progress strip while navigating, see
  /// `navigationDetent`.
  @State private var navigationHeight: CGFloat = 0
  /// The text size `navigationHeight` was measured at, and whether a junction picture was shown:
  /// another one gives the navigation part another height, also a lower one, so it is measured anew.
  @State private var navigationHeightKey: NavigationHeightKey?
  /// The attribution's height above the search, see `searchOverlay`.
  @State private var attributionHeight: CGFloat = 0
  /// Whether the place search is open in the panel, see `place-search` "Search while typing".
  @State private var searching = false
  /// The sheet's height before the search opened, to go back to when it closes.
  @State private var detentBeforeSearch: PresentationDetent?

  private static let smallestDetent = PresentationDetent.height(170)

  /// The smallest sheet while planning: at least the summary and the actions, which are below it at
  /// large text sizes, see `map-planning` "Route summary at large text sizes". The 24 points are the
  /// space above the summary.
  /// Whether the planning height is taller than the sheet's medium height, half the screen.
  private func planningAboveMedium(_ screen: GeometryProxy) -> Bool {
    let fullHeight = screen.size.height + screen.safeAreaInsets.top + screen.safeAreaInsets.bottom
    return navigationDetent == nil && summaryHeight + 34 > fullHeight / 2
  }

  private var planningDetent: PresentationDetent {
    // Plus the home indicator's area, which the sheet's content ends above.
    summaryHeight + 34 > 170 ? .height((summaryHeight + 34).rounded(.up)) : Self.smallestDetent
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
      // Without the sheet, the messages are shown from here.
      .modifier(MessageAlert(model: model, active: landscape))
      // Presented from here, outside the switch between map and loading view: a sheet attached to
      // a view that is replaced, e.g. while the other knot network loads, is dismissed for good.
      .sheet(isPresented: .constant(!landscape)) {
        panel
          // The keyboard covers the sheet while typing in the search above it; without this, the
          // sheet's content moved up and reported the keyboard as part of the sheet's height.
          .ignoresSafeArea(.keyboard)
          // The sheet's content ends above the home indicator; the sheet itself does not.
          .onGeometryChange(for: CGFloat.self) { $0.size.height + $0.safeAreaInsets.bottom } action: {
            height in
            sheet.live = height
            sheet.settle?.cancel()
            sheet.settle = Task {
              try? await Task.sleep(for: .milliseconds(120))
              if !Task.isCancelled, restingSheetHeight != height { restingSheetHeight = height }
            }
          }
          .presentationDetents(
            [navigationDetent ?? planningDetent, .medium, .large], selection: $detent
          )
          // While searching, a touch on the places found that moves a little scrolls them instead
          // of dragging the sheet, which swallowed taps on the phone.
          .presentationContentInteraction(searching ? .scrolls : .automatic)
          // The map stays usable and undimmed below the sheet up to half the screen, or up to the
          // planning height if that is taller, as at the largest text sizes: above the heights
          // given here, the system swallowed the first tap on the map and its buttons and pulled
          // the sheet down, see `place-search` "Search at the largest text size".
          .presentationBackgroundInteraction(
            .enabled(upThrough: planningAboveMedium(geometry) ? planningDetent : .medium))
          .interactiveDismissDisabled()
          // A sheet keeps the system's appearance unless told otherwise.
          .appAppearance()
          // From the sheet while it is shown: presented from below it, the dialog dismissed the
          // sheet for good, as the sheet is never presented again.
          .modifier(MessageAlert(model: model, active: !landscape))
      }
      // The keyboard covers the smallest sheet, but not a higher one, which would hide the field.
      .onChange(of: keyboardShown) { _, shown in
        if shown && !landscape && !searching { detent = planningDetent }
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
    .onAppear { DebugTiming.start() }
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
    // Pinned to the top: at the largest text sizes the content is taller than the sheet, and
    // centred it lost its top, the search field and the route's distance, under the grabber, see
    // `place-search` "Search at the largest text size".
    GeometryReader { space in
      panelStack.frame(width: space.size.width, height: space.size.height, alignment: .top)
    }
  }

  private var panelStack: some View {
    ZStack(alignment: .top) {
      panelContent
        // Kept under the search rather than rebuilt when it closes, which held up the main thread
        // for a quarter of a second on the phone.
        .opacity(searching ? 0 : 1)
        .allowsHitTesting(!searching)
        .accessibilityHidden(searching)
      if searching {
        PlaceSearchView(model: model, search: model.placeSearch, onClose: closeSearch, onPick: pick)
          .padding(.horizontal, 16)
          .padding(.top, 20)
      }
    }
  }

  private var panelContent: some View {
    Group {
      if showInfo {
        InfoSheet(model: model, onDone: { showInfo = false })
      } else if let session = model.session, model.layers != nil, !model.loading {
        WaypointSheet(
          // The highest the navigation part was while navigating, so the sheet does not jump when
          // the header switches to the number to follow near a knot, see `knot-navigation` "Number
          // to follow"; 0 again when navigation ends.
          model: model, session: session,
          onNavigationHeight: { height, key in
            navigationHeight = navigationHeightKey == key ? max(navigationHeight, height) : height
            navigationHeightKey = key
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
      "Where do you want to start?",
      isPresented: Binding(
        get: { model.startChoice != nil }, set: { if !$0 { model.dismissStartChoice() } }),
      titleVisibility: .visible, presenting: model.startChoice
    ) { choice in
      // See `knot-navigation` "Start on the route nearby".
      let language = AppLanguage.current
      let nearby = MapFeatures.shared.formatDistance(meters: choice.nearbyMeters, language: language)
      let start = MapFeatures.shared.formatDistance(meters: choice.startMeters, language: language)
      Button("Start on the route nearby · \(nearby)") { model.startNearby(choice) }
      if choice.start.isRoutePoint {
        Button("Ride to the start · \(start)") { model.rideToStart() }
      } else {
        Button("Ride to \(choice.start.ref) · \(start)") { model.rideToStart() }
      }
      Button("Cancel", role: .cancel) { model.dismissStartChoice() }
    } message: { _ in
      Text("You are closer to the route than to its start.")
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
        places: places,
        graph: model.session?.graph, layers: model.layers, focus: model.session.map(focus) ?? [],
        // A tap on the map closes the search, see `place-search` "Back above the sheet on iOS".
        onTap: { latitude, longitude, radius, point, place in
          if searching {
            closeSearch()
          } else {
            model.tap(latitude: latitude, longitude: longitude, radiusMeters: radius, at: point, place: place)
          }
        },
        online: model.online,
        initialPlace: model.initialPlace,
        onLeave: model.saveCamera,
        marker: model.marker,
        onMove: model.mapMoved,
        position: navigator.state?.position,
        nextKnot: navigator.state?.navigation?.nextKnot,
        navigating: navigator.state?.navigation != nil,
        courseAhead: navigator.state?.navigation?.courseAhead,
        wayToRoute: navigator.state.flatMap { state in
          state.position.flatMap { state.navigation?.wayToRoute(fix: $0) }
        },
        mapFocus: navigator.state.flatMap { state in
          state.position.flatMap { state.navigation?.mapFocus(fix: $0, guidedWay: state.guidedWay) }
        },
        guidedWay: navigator.state.flatMap { state in
          state.navigation?.guidedLines(guidedWay: state.guidedWay)
        },
        track: navigator.state?.track,
        direction: navigator.state?.direction?.doubleValue,
        myPosition: navigator.state == nil ? model.myPosition : nil,
        locating: model.locating,
        onLocate: locateAction,
        findingNearest: model.findingNearest,
        onShowNearest: showNearestAction,
        nearestKnots: model.nearestKnots,
        onSearch: navigator.state == nil && !searching ? openSearch : nil,
        // Above the sheet, or the bottom safe area in landscape, and the attribution right above it.
        bottomInset: (landscape ? screen.safeAreaInsets.bottom + 8 : restingSheetHeight) + attributionHeight + 8,
        onIdle: model.mapIdle,
        shownKnot: model.shownKnot,
        offerPlace: model.offer?.offer.position,
        offerFlag: model.offer?.nameOnly != true,
        onOfferMoved: { point in DispatchQueue.main.async { model.offerMoved(to: point) } }
      )
      .ignoresSafeArea()
      // Also while typing in landscape: the hits are in the side panel, see `map-planning`
      // "Landscape layout". Not while navigating: the route is fixed then, see `knot-navigation` "Switching the network
      // while navigating", and the info sheet is not needed on the ride, see `network-sync` "Info
      // sheet".
      if navigator.state == nil { networkSwitch }
      if model.layers == nil {
        // Only without bundled graphs, e.g. a debug build that was built without internet access,
        // or for the moment it takes to read the graph.
        LoadingNote(
          text: model.status.dataDate == nil && !model.online
            ? LocalizedStringKey("The knot networks need an internet connection once")
            : model.network == .hiking ? "Loading the hiking knot network…" : "Loading the cycling knot network…"
        )
        .padding(.top, notesTop)
      } else if let showing = model.showingHit {
        // Right after a hit is picked, so the app does not look frozen while the map gets there,
        // see `place-search` "Showing a far place on iOS".
        HStack(spacing: 8) {
          ProgressView()
          Text("Showing \(showing)…").font(.callout).lineLimit(1)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .floatingBackground(in: Capsule())
        .padding(.top, notesTop)
      } else if !model.status.hasGeometry {
        CoursesNote(online: model.online).padding(.top, notesTop)
      }
      // The choices for a tapped place, at its flag and above the controls, see `map-planning`
      // "Choose a route point on the map". In the map's coordinates, which ignore the safe area.
      if let offer = model.offer {
        // Below the place, kept between the controls at the top and the sheet, and scrolled when
        // taller than that, e.g. with opening hours and contacts in a large text size. Above the
        // attribution, like the map controls.
        OfferPlacement(
          anchor: offer.point,
          top: screen.safeAreaInsets.top + notesTop,
          bottom: (landscape ? screen.safeAreaInsets.bottom + 8 : restingSheetHeight) + attributionHeight + 8
        ) {
          PlaceOfferMenu(
            canAddStop: offer.offer.canAddStop, place: offer.offer.place, nameOnly: offer.nameOnly,
            details: offer.details, choose: model.choose)
        }
        .ignoresSafeArea()
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

  /// The nearest knots, while planning with no knots in view, see `map-planning` "Nearest knots".
  private var showNearestAction: (() -> Void)? {
    guard navigator.state == nil, model.noKnotsInView else { return nil }
    return { [model] in model.showNearestKnots() }
  }

  /// The network switch and the info button, at the top of the map.
  private var networkSwitch: some View {
    HStack {
      IconChoices(
        selection: networkBinding,
        choices: [
          IconChoice(
            value: IndexedNetworkType.cycling, word: String(localized: "Cycling", bundle: AppLanguage.bundle),
            icon: Image(systemName: "bicycle")),
          IconChoice(
            value: IndexedNetworkType.hiking, word: String(localized: "Hiking", bundle: AppLanguage.bundle),
            icon: Image(systemName: "figure.hiking")),
        ]
      )
      .fixedSize()
      .accessibilityLabel(String(localized: "Knot network", bundle: AppLanguage.bundle))
      infoButton
    }
    // Without a fixed width, larger text would push the info button off a narrow phone.
    .dynamicTypeSize(...DynamicTypeSize.xxxLarge)
    .padding(8)
    .floatingBackground(in: RoundedRectangle(cornerRadius: 12))
    .padding(.horizontal, 12)
    .padding(.top, 8)
  }

  /// The attribution at the bottom of the map, see `AttributionOverlay`.
  @ViewBuilder private func searchOverlay(landscape: Bool, keyboardShown: Bool, screen: GeometryProxy) -> some View {
    AttributionOverlay(
      sheet: sheet, landscape: landscape,
      visible: landscape || (!searching && !keyboardShown),
      atSmallestWhilePlanning: navigator.state == nil && detent == planningDetent,
      fullHeight: screen.size.height + screen.safeAreaInsets.top + screen.safeAreaInsets.bottom,
      bottomSafeArea: screen.safeAreaInsets.bottom,
      onHeight: { attributionHeight = $0 })
  }

  /// Opens the place search in the panel, the sheet at its full height, see `place-search` "Typing
  /// on iOS".
  private func openSearch() {
    detentBeforeSearch = detent
    searching = true
    detent = .large
  }

  /// Picks a hit: the search closes at once, and the map moves to the hit once the sheet has come
  /// down, as the sheet's move would cut the map's short, see `place-search` "Back above the sheet
  /// on iOS".
  private func pick(_ hit: PlaceHit) {
    searchLog.notice("pick")
    DebugTiming.note("pick \(hit.name)")
    model.willShow(hit)
    closeSearch()
    DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) { model.choose(hit) }
  }

  /// Closes the place search, the sheet back at the height it had, see `place-search` "Back above
  /// the sheet on iOS".
  private func closeSearch() {
    searching = false
    if let detentBeforeSearch { detent = detentBeforeSearch }
    detentBeforeSearch = nil
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
  let onNavigationHeight: (CGFloat, NavigationHeightKey) -> Void
  @Environment(\.dynamicTypeSize) private var dynamicTypeSize
  /// The height of the summary and the actions, the knots and the switch while planning.
  let onSummaryHeight: (CGFloat) -> Void
  /// Shows a coming knot on the map.
  let onShowKnot: (Knot) -> Void

  /// Whether the ways for the route are in, see `network-sync` "Path data ready for the ride".
  private func pathDataNote(_ status: PathDataStatus) -> some View {
    Group {
      switch status {
      case .loading: Text("Loading ways for this route…")
      case .ready: Text("Ways for this route are loaded")
      default: Text("Ways for this route couldn't be loaded")
      }
    }
    .font(.caption)
    .foregroundStyle(.secondary)
  }

  var body: some View {
    let complete = model.routeDescription as? RouteDescriptionComplete
    // While navigating, where the rider is on the route, see `knot-navigation` "Progress strip".
    let progress = navigator.state.flatMap { state in
      state.position.flatMap { state.navigation?.progress(fix: $0, guidedWay: state.guidedWay) }
    }
    VStack(alignment: .leading, spacing: 0) {
      // The header stays in place; below it, the ferries and the waypoints scroll together, see
      // `map-planning` "Long routes in the panel".
      VStack(alignment: .leading, spacing: 8) {
        if let navigation = navigator.state {
          NavigationHeader(state: navigation, geometry: navigator.geometry) { model.stopNavigation() }
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
                summaryAboveStackedActions
              }
            } else if dynamicTypeSize.isAccessibilitySize {
              summaryAboveStackedActions
            } else {
              HStack(alignment: .center) {
                summary
                Spacer()
                actions
              }
            }
          }
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
          if let status = model.pathData {
            pathDataNote(status)
          }
          // See `map-planning` "No path data".
          if session.hasStraightLines {
            Text("The way to a chosen place is only a straight line so far, and so is its length")
              .font(.caption)
              .foregroundStyle(.secondary)
          }
        }
        if navigator.state == nil, session.route != nil {
          newKnotsSwitch
        }
      }
      .padding([.horizontal, .top])
      .padding(.bottom, 8)
      // Measured for the sheet's smallest height, see `PlannerView`.
      .onGeometryChange(for: CGFloat.self) { $0.size.height } action: {
        onNavigationHeight(
          navigator.state == nil ? 0 : $0,
          NavigationHeightKey(
            typeSize: dynamicTypeSize, picture: navigator.state?.guidedWay?.junctionAhead() != nil))
        if navigator.state == nil { onSummaryHeight($0) }
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
            KnotBadge(
              ref: row.knot.ref, role: role(index), plate: .of(row.knot.signRegion),
              place: row.knot.routePoint?.placeKind
            ).frame(width: 40)
            // A route point made from a place shows its name, see `map-planning` "Route points from a
            // place"; read with a comma, not the dot.
            if let placeName = Self.placeName(of: row.knot) {
              Text(verbatim: "\(Self.roleName(role(index))) · \(placeName)").font(.callout).lineLimit(1)
                .accessibilityLabel(Text(verbatim: "\(Self.roleName(role(index))), \(placeName)"))
            } else {
              Text(Self.roleName(role(index))).font(.callout)
            }
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
      Text(arrival)
        .fontWeight(coming.state == .warning ? .bold : coming.state == nil ? .regular : .medium)
        .foregroundStyle(color)
    }
  }

  @ViewBuilder private func comingBadge(_ coming: ComingKnot) -> some View {
    if coming.knot == nil {
      FerryBadge(warning: coming.state == FerryArrivalState.warning)
    } else {
      KnotBadge(
        ref: coming.label, role: Self.role(of: coming.kind), plate: .of(coming.knot?.signRegion),
        place: coming.knot?.routePoint?.placeKind)
    }
  }

  /// The waypoint's role in the list, e.g. `Start`; keys of their own, as Stop also ends navigation.
  /// What tapped knots become, with the label above the choices where they do not fit next to it,
  /// see `map-planning` "What new knots become".
  private var newKnotsSwitch: some View {
    let label = String(localized: "Add knots", bundle: AppLanguage.bundle)
    let choices = IconChoices(
      selection: $model.newKnots,
      choices: [
        IconChoice(
          value: NewKnots.stop, word: String(localized: "Between", bundle: AppLanguage.bundle),
          icon: Image(uiImage: NewKnotsIcon.between)),
        IconChoice(
          value: NewKnots.end, word: String(localized: "At end", bundle: AppLanguage.bundle),
          icon: Image(uiImage: NewKnotsIcon.end)),
      ]
    )
    .accessibilityLabel(label)
    return ViewThatFits(in: .horizontal) {
      HStack {
        Text(label).font(.callout).fixedSize()
        Spacer()
        choices.fixedSize()
      }
      VStack(alignment: .leading, spacing: 4) {
        Text(label).font(.callout).fixedSize(horizontal: false, vertical: true)
        // Sideways where even alone it is wider than the panel, like the route's knots, rather than
        // widening the panel beyond the screen.
        ScrollView(.horizontal, showsIndicators: false) { choices }
          .scrollBounceBehavior(.basedOnSize, axes: .horizontal)
      }
    }
  }

  /// The name of the place a route point was made from, or its kind's name, `nil` for others.
  private static func placeName(of knot: Knot) -> String? {
    guard let kind = knot.routePoint?.placeKind else { return nil }
    return knot.routePoint?.placeName ?? PlaceIcons.kindName(kind)
  }

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
  private var actions: some View {
    HStack(alignment: .center) {
      lesserActions
      mainAction
    }
    // Never squeezed: the summary and the spacer give way, or the actions move below them.
    .fixedSize(horizontal: true, vertical: false)
  }

  /// Where even the actions alone are wider than the panel, the main action gets its own line, and
  /// wraps its words if it has to, so nothing widens the panel beyond the screen, see `map-planning`
  /// "Route summary at large text sizes".
  private var summaryAboveStackedActions: some View {
    VStack(alignment: .trailing, spacing: 8) {
      summary.frame(maxWidth: .infinity, alignment: .leading)
      if !session.waypoints.isEmpty || session.canReturnToStart || model.canExportGpx {
        HStack(alignment: .center) { lesserActions }
      }
      mainAction
    }
  }

  @ViewBuilder private var lesserActions: some View {
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
  }

  @ViewBuilder private var mainAction: some View {
    if model.canNavigate {
      ProminentAction(title: "Navigate", systemImage: "location.north.fill") {
        model.startNavigation()
      }
    }
    // In place of asking to tap a knot, see `map-planning` "Nearest knots".
    if model.noKnotsInView && session.waypoints.isEmpty {
      ProminentAction(title: "Show the nearest knots", systemImage: "signpost.right") {
        model.showNearestKnots()
      }
    }
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
      if startOnly.endFirst {
        Text("End set, tap a knot to set the start").font(.headline)
      } else if startOnly.routePoint {
        Text("Start set, tap a knot to set the end").font(.headline)
      } else {
        Text("Start at \(startOnly.start), tap a knot to set the end").font(.headline)
      }
    case let unreachable as RouteDescriptionUnreachable:
      Text("No route for \(unreachable.legs.joined(separator: ", "))").font(.headline).foregroundStyle(.red)
    default:
      if model.noKnotsInView {
        Text("No knots in view").font(.headline)
      } else {
        Text("Tap a knot to set the start").font(.headline)
      }
    }
  }

  private func role(_ index: Int) -> KnotRole {
    if session.endFirst { return .end }
    return switch index {
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
          KnotBadge(
            ref: routeKnot.knot.ref, role: routeKnot.role, plate: .of(routeKnot.knot.signRegion),
            place: routeKnot.knot.routePoint?.placeKind)
          if ferryPositions.contains(position) { FerryBadge() }
        }
        // Room for the waypoints' corner symbols, which reach beyond their badges.
        .padding(.vertical, 6)
        .padding(.trailing, 6)
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
  /// The network's courses, to tell whether the rider is on a path of the knot network off route.
  let geometry: NetworkGeometry?
  let onStop: () -> Void
  @Environment(\.colorScheme) private var colorScheme

  /// What to look for on the signs, see `knot-navigation` "Number to follow".
  private var row: HeaderRow? {
    guard let navigation = state.navigation, let position = state.position else { return nil }
    return navigation.header(
      fix: position, ridingDirection: state.direction,
      guidedWay: state.guidedWay, network: geometry)
  }

  var body: some View {
    let row = row
    // Off route and riding the wrong way, the header turns dark, light in dark mode, instead of red,
    // see `map-planning` "Light and dark appearance".
    let inverted = row is HeaderRow.WrongWay || (row as? HeaderRow.ToRoute)?.offRoute == true
    let content = HStack(alignment: .center) {
      if let row {
        HeaderRowView(row: row)
      } else {
        Text("Waiting for your position…").font(.headline)
      }
      Spacer(minLength: 8)
      StopIconAction(action: onStop)
    }
    VStack(alignment: .leading, spacing: 8) {
      header(content, inverted: inverted)
      // The picture of a doubtful junction close ahead, below the row, so the row keeps its height,
      // see `knot-navigation` "Picture in the app".
      if let guided = (row as? HeaderRow.ToRoute)?.guided, let picture = guided.picture {
        HStack(alignment: .center, spacing: 12) {
          JunctionPictureView(encoded: picture.encoded())
            .frame(width: 104, height: 104)
            .accessibilityHidden(true)
          if let sentence = guided.pictureSentence {
            Text(sentence).font(.headline).fixedSize(horizontal: false, vertical: true)
          }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(guided.pictureSentence ?? "")
      }
    }
  }

  @ViewBuilder private func header(_ content: some View, inverted: Bool) -> some View {
    if inverted {
      content
        .foregroundStyle(colorScheme == .dark ? Color.knoopInk : Color.white)
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background(
          RoundedRectangle(cornerRadius: 12)
            .fill(colorScheme == .dark ? Color(red: 0xE6 / 255, green: 0xE8 / 255, blue: 0xE6 / 255) : Color.knoopInk))
        .environment(\.colorScheme, colorScheme == .dark ? .light : .dark)
    } else {
      content
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

/// The choices for a tapped place, shown below its flag on the map and following it while the map
/// moves. Dismissed by tapping the map elsewhere.
/// Places the tapped place's card in the map's coordinates: its top 40 points below the place (below
/// its flag), moved up when it would reach `bottom`, never above `top`, and kept 10 points inside
/// the sides, at most as wide as the map. A card taller than the space scrolls.
private struct OfferPlacement<Card: View>: View {
  let anchor: CGPoint
  let top: CGFloat
  let bottom: CGFloat
  @ViewBuilder let card: Card

  var body: some View {
    CardLayout(anchor: anchor, top: top, bottom: bottom) {
      ViewThatFits(in: .vertical) {
        card
        ScrollView { card }.scrollBounceBehavior(.basedOnSize)
          .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 14))
          .clipShape(RoundedRectangle(cornerRadius: 14))
      }
    }
  }

  private struct CardLayout: Layout {
    let anchor: CGPoint
    let top: CGFloat
    let bottom: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
      proposal.replacingUnspecifiedDimensions()
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
      guard let card = subviews.first else { return }
      let lowest = bounds.maxY - bottom
      let highest = bounds.minY + top
      let size = card.sizeThatFits(
        ProposedViewSize(width: bounds.width - 20, height: max(lowest - highest, 88)))
      let x = min(max(anchor.x - size.width / 2, bounds.minX + 10), bounds.maxX - size.width - 10)
      let y = max(min(bounds.minY + anchor.y + 40, lowest - size.height), highest)
      card.place(at: CGPoint(x: x, y: y), anchor: .topLeading, proposal: ProposedViewSize(size))
    }
  }
}

private struct PlaceOfferMenu: View {
  let canAddStop: Bool
  /// The tapped place, shown with its icon and name, see `map-planning` "Tapping a place on the map".
  let place: MapPlace?
  /// Whether only the place's name is shown, without choices.
  let nameOnly: Bool
  /// What OpenStreetMap tells about the place, see `map-planning` "Contacts on the place card".
  let details: PlaceDetails?
  let choose: (PlaceChoice) -> Void
  @Environment(\.openURL) private var openURL
  /// 240 points in the default text size, wider for larger text, up to the map's width.
  @ScaledMetric(relativeTo: .body) private var width: CGFloat = 240

  var body: some View {
    VStack(alignment: .leading, spacing: 0) {
      if let place {
        HStack(spacing: 12) {
          Image(uiImage: PlaceIcons.image(place.kind)).accessibilityHidden(true)
          Group {
            if let name = place.name { Text(verbatim: name) } else { Text(kindName(place.kindKey)) }
          }
          .font(.headline)
        }
        .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
        .padding(.horizontal, 16)
        if let details { detailRows(details) }
        if !nameOnly { Divider() }
      }
      if !nameOnly { choices }
    }
    .frame(maxWidth: width)
    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 14))
    .shadow(radius: 8)
  }

  /// Whether the place is open now with its opening hours, its website and its phone number, each
  /// left out when OpenStreetMap doesn't have it.
  @ViewBuilder private func detailRows(_ details: PlaceDetails) -> some View {
    if let hours = details.openingHours {
      // Kept current every minute like the ferries, see "Closing while the card is shown".
      TimelineView(.everyMinute) { context in
        let status = details.openingStatusAt(
          now: RouteFerries.ferryMoment(context.date), language: AppLanguage.current)
        HStack(alignment: .top, spacing: 12) {
          Image(systemName: "clock").foregroundStyle(.secondary).accessibilityHidden(true)
          VStack(alignment: .leading, spacing: 2) {
            if let status {
              Text(verbatim: status.text).font(.subheadline).foregroundStyle(status.open ? .green : .red)
            }
            Text(verbatim: hours).font(.footnote).foregroundStyle(.secondary)
              .fixedSize(horizontal: false, vertical: true)
          }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 6)
        .accessibilityElement(children: .combine)
      }
    }
    if let website = details.displayWebsite, let uri = details.websiteUri, let url = URL(string: uri) {
      detailButton("globe", website) { openURL(url) }
        .accessibilityLabel(Text("\(website), opens the website in the browser", bundle: AppLanguage.bundle))
    }
    if let phone = details.displayPhone, let uri = details.dialUri, let url = URL(string: uri) {
      // iOS asks before calling, see "Calling a hotel".
      detailButton("phone", phone) { openURL(url) }
        .accessibilityLabel(Text("\(phone), opens the phone app to call", bundle: AppLanguage.bundle))
    }
  }

  private func detailButton(_ symbol: String, _ text: String, action: @escaping () -> Void) -> some View {
    Button(action: action) {
      HStack(spacing: 12) {
        Image(systemName: symbol).accessibilityHidden(true)
        Text(verbatim: text).lineLimit(1).truncationMode(.middle)
      }
      .font(.subheadline)
      .frame(maxWidth: .infinity, minHeight: 36, alignment: .leading)
      .padding(.horizontal, 16)
    }
  }

  /// The name of a kind of place, for a place without a name of its own.
  private func kindName(_ kindKey: String) -> LocalizedStringKey {
    switch kindKey {
    case "food": return "Food and drink"
    case "supermarket": return "Supermarket"
    case "bicycle": return "Bicycle shop"
    case "railway": return "Railway station"
    case "toilets": return "Toilets"
    case "campsite": return "Campsite"
    case "lodging": return "Hotel or guest house"
    default: return "Drinking water"
    }
  }

  @ViewBuilder private var choices: some View {
    VStack(alignment: .leading, spacing: 0) {
      item("Start here", .start)
      if canAddStop {
        Divider()
        item("Add stop here", .stop)
      }
      Divider()
      item("End here", .end)
    }
  }

  private func item(_ title: LocalizedStringKey, _ choice: PlaceChoice) -> some View {
    Button { choose(choice) } label: {
      Text(title).frame(maxWidth: .infinity, minHeight: 44, alignment: .leading).padding(.horizontal, 16)
    }
  }
}

/// The app's messages, e.g. "Your position could not be found", as a dialog shown from the view on
/// top when `active`: a dialog presented from below the planner's sheet dismisses the sheet.
private struct MessageAlert: ViewModifier {
  @ObservedObject var model: AppModel
  let active: Bool

  func body(content: Content) -> some View {
    content.alert(
      model.message ?? "",
      isPresented: Binding(get: { active && model.message != nil }, set: { if !$0 { model.message = nil } })
    ) {
      if model.messageOpensSettings {
        Button("Settings") {
          model.message = nil
          model.messageOpensSettings = false
          if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
        }
      }
      Button("OK") {
        model.message = nil
        model.messageOpensSettings = false
      }
    }
  }
}

/// The sheet's height on every frame of its move, observed only where it must be followed live.
@MainActor
private final class SheetHeight: ObservableObject {
  @Published var live: CGFloat = 0
  /// Sets the planner's resting height once the sheet stopped moving.
  var settle: Task<Void, Never>?
}

/// The attribution at the bottom of the map: right above the sheet in portrait, following it when
/// it is dragged, and hidden while the sheet is pulled up over the map. Its own view, so following
/// the sheet redraws only this, see `SheetHeight`. The place search is in the panel, see
/// `place-search` "Search while typing".
private struct AttributionOverlay: View {

  @ObservedObject var sheet: SheetHeight
  let landscape: Bool
  /// In portrait the sheet covers the map while searching; in landscape the map stays as it is.
  let visible: Bool
  let atSmallestWhilePlanning: Bool
  let fullHeight: CGFloat
  let bottomSafeArea: CGFloat
  let onHeight: (CGFloat) -> Void

  var body: some View {
    let sheetUp = !landscape && sheet.live > fullHeight * 0.6 && !atSmallestWhilePlanning
    // This view ends at the bottom safe area, the sheet's height is measured from the screen's
    // bottom.
    let bottom = landscape ? 8 : max(sheet.live - bottomSafeArea, 0) + 8
    if !sheetUp && visible {
      HStack {
        Spacer(minLength: 0)
        Text("© OpenStreetMap contributors")
          .font(.caption2)
          // Like the network switch, so large text leaves the credit in full, on one line.
          .dynamicTypeSize(...DynamicTypeSize.xxxLarge)
          .padding(.horizontal, 6)
          .padding(.vertical, 2)
          .floatingBackground(in: Capsule())
          .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { onHeight($0) }
      }
      .padding(.horizontal, 12)
      .padding(.bottom, bottom)
    }
  }
}

/// What the navigation part's height depends on besides the route: the text size, and whether a
/// junction picture shows, which makes it taller only while it does, see `knot-navigation`
/// "Picture in the app".
struct NavigationHeightKey: Equatable {
  let typeSize: DynamicTypeSize
  let picture: Bool
}
