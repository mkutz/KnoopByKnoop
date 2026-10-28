import SwiftUI

/// A filled action with icon and label: Navigate, or Stop in red, see `map-planning` "Clear actions
/// in the planner panel".
struct ProminentAction: View {

  let title: String
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

  let title: String
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
