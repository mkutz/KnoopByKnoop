import KnoopByKnoopCore
import SwiftUI

/// The map and, on cycling routes, the ways as symbols with their status, at the end of the action
/// row, see `network-sync` "Base map ready for the ride" and "Path data ready for the ride".
struct DownloadSymbols: View {

  let baseMap: PathDataStatus?
  let pathData: PathDataStatus?

  var body: some View {
    HStack(spacing: 4) {
      if let baseMap {
        DownloadSymbol(systemImage: "map", status: baseMap, sentence: Self.mapSentence(baseMap))
      }
      if let pathData {
        DownloadSymbol(
          systemImage: "point.3.connected.trianglepath.dotted", status: pathData,
          sentence: Self.waysSentence(pathData))
      }
    }
  }

  private static func mapSentence(_ status: PathDataStatus) -> String {
    switch status {
    case .loading: String(localized: "Saving the map along the route…", bundle: AppLanguage.bundle)
    case .ready: String(localized: "Map along the route saved: it shows offline too", bundle: AppLanguage.bundle)
    default: String(localized: "Map along the route not saved: offline, only the knots and the route show", bundle: AppLanguage.bundle)
    }
  }

  private static func waysSentence(_ status: PathDataStatus) -> String {
    switch status {
    case .loading: String(localized: "Saving paths and streets around the route…", bundle: AppLanguage.bundle)
    case .ready: String(localized: "Paths and streets around the route saved: directions back to it work offline", bundle: AppLanguage.bundle)
    default: String(localized: "Paths and streets around the route not saved: off the route, only the knots guide you", bundle: AppLanguage.bundle)
    }
  }
}

/// One symbol with a mark at its corner: a spinner while loading, a checkmark when loaded, a warning
/// when it couldn't be loaded. Read as `sentence` by VoiceOver; a tap shows it.
private struct DownloadSymbol: View {

  let systemImage: String
  let status: PathDataStatus
  let sentence: String
  @State private var showing = false
  @Environment(\.accessibilityReduceMotion) private var reduceMotion

  var body: some View {
    Button {
      showing = true
    } label: {
      Image(systemName: systemImage)
        .font(.system(size: 18, weight: .medium))
        .foregroundStyle(.secondary)
        .frame(width: 32, height: 32)
        .overlay(alignment: .bottomTrailing) {
          mark
            .frame(width: 14, height: 14)
            // Ringed in the panel's colour, so it stands apart from the symbol.
            .background(Circle().fill(.background).padding(-1.5))
            .id(status)
            .transition(.opacity)
        }
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.25), value: status)
    }
    .buttonStyle(.plain)
    .popover(isPresented: $showing) {
      Text(sentence)
        .font(.footnote)
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .presentationCompactAdaptation(.popover)
    }
    .accessibilityElement(children: .ignore)
    .accessibilityLabel(sentence)
    .accessibilityRemoveTraits(.isButton)
  }

  @ViewBuilder private var mark: some View {
    switch status {
    case .loading:
      ProgressView()
        .controlSize(.mini)
        .tint(.actionIconGreen)
    case .ready:
      Image(systemName: "checkmark.circle.fill")
        .font(.system(size: 12, weight: .semibold))
        .foregroundStyle(Color.actionIconGreen)
    default:
      Image(systemName: "exclamationmark.triangle.fill")
        .font(.system(size: 11, weight: .semibold))
        .foregroundStyle(Color.downloadWarning)
    }
  }
}

extension Color {
  /// A download that couldn't be loaded, see `network-sync`: amber, as red means Stop.
  static let downloadWarning = Color(
    UIColor { traits in
      traits.userInterfaceStyle == .dark
        ? UIColor(red: 0xFF / 255, green: 0xB8 / 255, blue: 0x4D / 255, alpha: 1)
        : UIColor(red: 0xB2 / 255, green: 0x6A / 255, blue: 0x00 / 255, alpha: 1)
    })
}
