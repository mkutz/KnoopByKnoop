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

/// One choice of `IconChoices`.
struct IconChoice<Value> {
  let value: Value
  let word: String
  let icon: Image
}

/// Choices with an icon next to each word, for the switches of `map-planning` "What new knots
/// become" and "Icons on the network switch". Not a segmented `Picker`, which shows only the word
/// of a label.
struct IconChoices<Value: Equatable>: View {

  @Binding var selection: Value
  let choices: [IconChoice<Value>]

  var body: some View {
    HStack(spacing: 2) {
      ForEach(choices.indices, id: \.self) { choice(choices[$0]) }
    }
    .padding(2)
    .background(Capsule().fill(Color(uiColor: .tertiarySystemFill)))
    .accessibilityElement(children: .contain)
  }

  private func choice(_ choice: IconChoice<Value>) -> some View {
    Button {
      selection = choice.value
    } label: {
      HStack(spacing: 6) {
        choice.icon.accessibilityHidden(true)
        Text(choice.word).lineLimit(1).fixedSize()
      }
      .font(.footnote.weight(.medium))
      .padding(.horizontal, 10)
      .padding(.vertical, 6)
      .frame(maxWidth: .infinity)
      .background(Capsule().fill(selection == choice.value ? Self.selected : .clear))
      .contentShape(Capsule())
    }
    .buttonStyle(.plain)
    .foregroundStyle(.primary)
    .accessibilityAddTraits(selection == choice.value ? .isSelected : [])
  }

  private static var selected: Color {
    Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? UIColor(white: 0.4, alpha: 1) : .white })
  }
}
