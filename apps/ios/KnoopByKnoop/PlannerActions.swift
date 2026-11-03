import KnoopByKnoopCore
import SwiftUI

/// A filled action with icon and label: Navigate, or Stop in red, see `map-planning` "Clear actions
/// in the planner panel".
struct ProminentAction: View {

  let title: LocalizedStringKey
  let systemImage: String
  var color: Color = .actionGreen
  let action: () -> Void

  var body: some View {
    Button(action: action) { Label(title, systemImage: systemImage) }
      .buttonStyle(.borderedProminent)
      .buttonBorderShape(.capsule)
      .tint(color)
  }
}

/// A round action with only an icon: Back to start or Clear. VoiceOver reads the title.
struct IconAction: View {

  let title: LocalizedStringKey
  let systemImage: String
  let action: () -> Void

  var body: some View {
    Button(action: action) { Label(title, systemImage: systemImage) }
      .labelStyle(.iconOnly)
      .buttonStyle(.bordered)
      .buttonBorderShape(.circle)
      .tint(.actionIconGreen)
  }
}

/// The choices of the switch for new knots, each with its icon and word. Not a segmented `Picker`,
/// which shows only the word of a label, see `map-planning` "What new knots become".
struct NewKnotsChoices: View {

  @Binding var selection: NewKnots

  var body: some View {
    HStack(spacing: 2) {
      choice(.stop, word: String(localized: "Between", bundle: AppLanguage.bundle), icon: NewKnotsIcon.between)
      choice(.end, word: String(localized: "At end", bundle: AppLanguage.bundle), icon: NewKnotsIcon.end)
    }
    .padding(2)
    .background(Capsule().fill(Color(uiColor: .tertiarySystemFill)))
    .accessibilityElement(children: .contain)
  }

  private func choice(_ newKnots: NewKnots, word: String, icon: UIImage) -> some View {
    Button {
      selection = newKnots
    } label: {
      HStack(spacing: 6) {
        Image(uiImage: icon).accessibilityHidden(true)
        Text(word).font(.footnote.weight(.medium)).lineLimit(1).fixedSize()
      }
      .padding(.horizontal, 10)
      .padding(.vertical, 6)
      .frame(maxWidth: .infinity)
      .background(Capsule().fill(selection == newKnots ? Self.selected : .clear))
      .contentShape(Capsule())
    }
    .buttonStyle(.plain)
    .foregroundStyle(.primary)
    .accessibilityAddTraits(selection == newKnots ? .isSelected : [])
  }

  private static let selected = Color(
    uiColor: UIColor { $0.userInterfaceStyle == .dark ? UIColor(white: 0.4, alpha: 1) : .white })
}
