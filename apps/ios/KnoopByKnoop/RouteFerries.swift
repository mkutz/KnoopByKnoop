import KnoopByKnoopCore
import SwiftUI

/// Marks a ferry between two knots of the route summary, see `map-planning`.
struct FerryBadge: View {

  var body: some View {
    Text("⛴")
      .font(.system(size: 14))
      .frame(width: 28, height: 28)
      .background(Circle().fill(Color.accentColor.opacity(0.2)))
  }
}

/// The route's ferries with their operating times, fee and website, see `map-planning`.
struct RouteFerries: View {

  let ferries: [RouteFerry]

  var body: some View {
    VStack(alignment: .leading, spacing: 6) {
      Text("Ferries on this route").font(.subheadline.bold())
      ForEach(Array(ferries.enumerated()), id: \.offset) { _, ferry in
        VStack(alignment: .leading, spacing: 2) {
          Text("⛴ \(ferry.title)").font(.callout)
          Text(ferry.times).font(.caption).foregroundStyle(ferry.timesKnown ? Color.secondary : Color.red)
          if let fee = ferry.fee { Text(fee).font(.caption).foregroundStyle(.secondary) }
          if let website = ferry.ferry.website,
            let url = URL(string: website.contains("://") ? website : "https://\(website)")
          {
            Link("Website", destination: url).font(.caption)
          }
        }
      }
    }
    .padding(.top, 4)
  }
}
