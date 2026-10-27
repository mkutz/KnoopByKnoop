import SwiftUI

extension View {

  /// The background of a view floating over the map: Liquid Glass on iOS 26 and later, the blurred
  /// material before.
  @ViewBuilder func floatingBackground(in shape: some Shape) -> some View {
    if #available(iOS 26, *) {
      glassEffect(.regular, in: shape)
    } else {
      background(.regularMaterial, in: shape)
    }
  }
}
