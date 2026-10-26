import KnoopByKnoopCore
import SwiftUI

@main
struct KnoopByKnoopApp: App {
  var body: some Scene {
    WindowGroup {
      ContentView()
    }
  }
}

struct ContentView: View {
  private let nextKnot = Knot(
    id: 0, ref: "12", network: .cycling, latitude: 52.09, longitude: 5.12)

  var body: some View {
    VStack {
      Text("Next knot").font(.headline)
      Text(nextKnot.ref).font(.system(size: 96, weight: .bold))
    }
  }
}
