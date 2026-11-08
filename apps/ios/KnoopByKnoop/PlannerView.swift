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
  /// Whether places useful on a ride are highlighted, see `map-planning` "Switch for places on the map".
  @AppStorage("places") private var places = true
  @State private var detent: PresentationDetent = smallestDetent
  /// The height of the summary and the actions, the knots and the switch while planning, with their
  /// padding, see `planningDetent`.
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
          // From the sheet while it is shown: presented from below it, the dialog dismissed the
          // sheet for good, as the sheet is never presented again.
          .modifier(MessageAlert(model: model, active: !landscape))
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
        places: places,
        graph: model.session?.graph, layers: model.layers, focus: model.session.map(focus) ?? [],
        onTap: model.tap(latitude:longitude:radiusMeters:at:),
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
        guidedWay: navigator.state?.guidedWay?.pointsAhead(),
        track: navigator.state?.track,
        direction: navigator.state?.direction,
        myPosition: navigator.state == nil ? model.myPosition : nil,
        locating: model.locating,
        onLocate: locateAction,
        findingNearest: model.findingNearest,
        onShowNearest: showNearestAction,
        nearestKnots: model.nearestKnots,
        shownKnot: model.shownKnot,
        offerPlace: model.offer?.offer.position,
        onOfferMoved: { point in DispatchQueue.main.async { model.offerMoved(to: point) } }
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
      // The choices for a tapped place, at its flag and above the controls, see `map-planning`
      // "Choose a route point on the map". In the map's coordinates, which ignore the safe area.
      if let offer = model.offer {
        GeometryReader { map in
          PlaceOfferMenu(canAddStop: offer.offer.canAddStop, choose: model.choose)
            .fixedSize()
            .position(
              x: min(max(offer.point.x, 110), map.size.width - 110),
              y: offer.point.y + 90)
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

  /// The place search and the attribution, at the bottom of the map: right above the sheet in
  /// portrait, following it when it is dragged, and right above the on-screen keyboard while typing.
  /// Hidden while the sheet is pulled up over the map. While typing in landscape, the attribution and
  /// the network switch give way, so the hits can reach the top of the map.
  @ViewBuilder private func searchOverlay(landscape: Bool, keyboardShown: Bool, screen: GeometryProxy) -> some View {
    let fullHeight = screen.size.height + screen.safeAreaInsets.top + screen.safeAreaInsets.bottom
    // At its smallest while planning, the sheet is not pulled up, however high large text makes it:
    // it cannot go lower, and the search must stay reachable.
    let atSmallestWhilePlanning = navigator.state == nil && detent == planningDetent
    let sheetUp = !landscape && !keyboardShown && sheetHeight > fullHeight * 0.6 && !atSmallestWhilePlanning
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
              // Like the network switch, so large text leaves the credit in full, on one line.
              .dynamicTypeSize(...DynamicTypeSize.xxxLarge)
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
        onNavigationHeight(navigator.state == nil ? 0 : $0, dynamicTypeSize)
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
            KnotBadge(ref: row.knot.ref, role: role(index), plate: .of(row.knot.signRegion)).frame(width: 40)
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
      Text(arrival)
        .fontWeight(coming.state == .warning ? .bold : coming.state == nil ? .regular : .medium)
        .foregroundStyle(color)
    }
  }

  @ViewBuilder private func comingBadge(_ coming: ComingKnot) -> some View {
    if coming.knot == nil {
      FerryBadge(warning: coming.state == FerryArrivalState.warning)
    } else {
      KnotBadge(ref: coming.label, role: Self.role(of: coming.kind), plate: .of(coming.knot?.signRegion))
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
          KnotBadge(ref: routeKnot.knot.ref, role: routeKnot.role, plate: .of(routeKnot.knot.signRegion))
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
      fix: position, ridingDirection: state.direction.map { KotlinDouble(value: $0) },
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
private struct PlaceOfferMenu: View {
  let canAddStop: Bool
  let choose: (PlaceChoice) -> Void

  var body: some View {
    VStack(alignment: .leading, spacing: 0) {
      item("Start here", .start)
      if canAddStop {
        Divider()
        item("Add stop here", .stop)
      }
      Divider()
      item("End here", .end)
    }
    .frame(width: 200)
    .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 14))
    .shadow(radius: 8)
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
